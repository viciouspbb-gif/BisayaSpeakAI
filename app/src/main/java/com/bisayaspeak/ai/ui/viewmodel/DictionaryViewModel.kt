package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.PromptProvider
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import org.json.JSONArray
import org.json.JSONObject

enum class DictionaryLanguage { JAPANESE, BISAYA, ENGLISH, UNKNOWN }

data class TranslationCandidate(
    val bisaya: String,
    val japanese: String,
    val english: String,
    val politeness: String,
    val situation: String,
    val nuance: String,
    val tip: String
)

data class AiExplanation(
    val summary: String,
    val usage: String,
    val relatedPhrases: List<String>
)

data class DictionaryUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val candidates: List<TranslationCandidate> = emptyList(),
    val explanation: AiExplanation? = null,
    val errorMessage: String? = null,
    val dictionaryTtsState: DictionaryTtsState = DictionaryTtsState.Idle
)

sealed interface DictionaryTtsState {
    object Idle : DictionaryTtsState
    object Playing : DictionaryTtsState
    data class Error(val message: String) : DictionaryTtsState
}

class DictionaryViewModel(
    application: Application,
    private val repository: OpenAiChatRepository = OpenAiChatRepository(),
    private val promptProvider: PromptProvider = PromptProvider(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()
    private val dictionaryVoiceService by lazy { GeminiVoiceService(application.applicationContext) }
    private val hasOpenAiKey = BuildConfig.OPENAI_API_KEY.isNotBlank()
    private val prefersJapaneseOutput: Boolean = run {
        val locale = application.resources.configuration.locales[0]
        (locale?.language ?: Locale.getDefault().language).equals("ja", ignoreCase = true)
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
    }

    fun submitExploration() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "調べたい単語やフレーズを入力してください。") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val payload = requestExploration(query)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        candidates = payload.first,
                        explanation = payload.second,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "探索に失敗しました")
                }
            }
        }
    }

    fun speakBisaya(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (!hasOpenAiKey) {
            Log.w("DictionaryViewModel", "OPENAI_API_KEY is blank; skipping TTS in dictionary mode.")
            _uiState.update { it.copy(dictionaryTtsState = DictionaryTtsState.Error("OPENAIキーが設定されていません。")) }
            return
        }
        _uiState.update { it.copy(dictionaryTtsState = DictionaryTtsState.Playing) }
        dictionaryVoiceService.speak(
            text = trimmed,
            cue = GeminiVoiceCue.TALK_LOW,
            onStart = {
                _uiState.update { it.copy(dictionaryTtsState = DictionaryTtsState.Playing) }
            },
            onComplete = {
                _uiState.update { it.copy(dictionaryTtsState = DictionaryTtsState.Idle) }
            },
            onError = { error ->
                Log.e("DictionaryViewModel", "Gemini TTS failed", error)
                dictionaryVoiceService.stop()
                _uiState.update {
                    it.copy(
                        dictionaryTtsState = DictionaryTtsState.Error(
                            error.message ?: "Gemini音声の取得に失敗しました"
                        )
                    )
                }
            }
        )
    }

    private suspend fun requestExploration(query: String): Pair<List<TranslationCandidate>, AiExplanation?> {
        val prompt = buildString {
            appendLine(promptProvider.getSystemPrompt())
            appendLine("You are an AI exploration dictionary that bridges Japanese and Cebuano (Bisaya) learners.")
            appendLine("Analyze the provided query and output rich translation candidates with nuance.")
            appendLine("Respond ONLY in JSON with the following schema:")
            appendLine("{")
            appendLine("  \"query\": string,")
            appendLine("  \"detectedLanguage\": \"ja\"|\"ceb\"|\"en\",")
            appendLine("  \"candidates\": [")
            appendLine("    {")
            appendLine("      \"bisaya\": string,")
            appendLine("      \"japanese\": string,")
            appendLine("      \"english\": string,")
            appendLine("      \"politeness\": string,")
            appendLine("      \"situation\": string,")
            appendLine("      \"nuance\": string,")
            appendLine("      \"tip\": string")
            appendLine("    }...")
            appendLine("  ],")
            appendLine("  \"explanation\": {")
            appendLine("      \"summary\": string,")
            appendLine("      \"usage\": string,")
            appendLine("      \"related\": [string]")
            appendLine("  }")
            appendLine("}")
            appendLine("Keep answers concise, 2-4 candidates maximum. Use plain text (no markdown).")
            val explanationLanguage = if (promptProvider.getSystemPrompt().contains("Do not output Japanese")) {
                "English"
            } else {
                "Japanese"
            }
            appendLine("IMPORTANT: explanation.summary・explanation.usage・explanation.related must be in natural $explanationLanguage with concise sentences.")
            appendLine("User query: \"$query\"")
        }
        val raw = repository.generateJsonResponse(prompt, temperature = 0.35)
        return parseExplorationPayload(raw)
    }

    private fun parseExplorationPayload(raw: String): Pair<List<TranslationCandidate>, AiExplanation?> {
        val cleaned = raw.trim().removePrefix("```json").removeSuffix("```")
        val json = JSONObject(cleaned)
        val candidatesArray = json.optJSONArray("candidates") ?: JSONArray()
        val candidates = buildList {
            for (i in 0 until candidatesArray.length()) {
                val item = candidatesArray.optJSONObject(i) ?: continue
                add(
                    TranslationCandidate(
                        bisaya = item.optString("bisaya"),
                        japanese = item.optString("japanese"),
                        english = item.optString("english"),
                        politeness = item.optString("politeness"),
                        situation = item.optString("situation"),
                        nuance = item.optString("nuance"),
                        tip = item.optString("tip")
                    )
                )
            }
        }
        val explanationObj = json.optJSONObject("explanation")
        val explanation = explanationObj?.let {
            AiExplanation(
                summary = it.optString("summary"),
                usage = it.optString("usage"),
                relatedPhrases = it.optJSONArray("related")?.let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            add(arr.optString(i))
                        }
                    }
                } ?: emptyList()
            )
        }
        return candidates to explanation
    }

    private fun detectLanguage(text: String): DictionaryLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DictionaryLanguage.UNKNOWN
        var japaneseScore = 0
        var bisayaScore = 0
        var englishScore = 0
        val lower = trimmed.lowercase()

        val japaneseTokens = listOf("です", "ます", "ました", "ありがとう", "こんにちは", "すみません", "ましたか", "でしょう")
        japaneseTokens.forEach { token -> if (lower.contains(token)) japaneseScore += 2 }

        val bisayaTokens = listOf(
            "ako", "ikaw", "siya", "palihug", "salamat", "maayong", "unsa", "kaayo", "kana",
            "gani", "nimo", "kita", "balik", "laag", "pila", "gikan", "tawo", "nato", "adto",
            "puhon", "bai", "unsaon", "sige", "wala", "naa", "ganiha", "gihapon"
        )
        bisayaTokens.forEach { token -> if (lower.contains(token)) bisayaScore += 2 }

        val englishTokens = listOf(
            "hello", "hi", "please", "thank", "thanks", "sorry", "good", "morning", "evening",
            "night", "how", "are", "you", "today", "tomorrow", "yesterday", "where", "what",
            "why", "when", "who", "the", "and", "but", "because", "help", "need", "want",
            "love", "friend", "meet", "nice", "great"
        )
        englishTokens.forEach { token -> if (lower.contains(token)) englishScore += 2 }

        val words = lower.replace("[^a-z']".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
        val englishWordSet = setOf(
            "i", "you", "we", "they", "it", "hello", "hey", "please", "thanks", "thank",
            "help", "need", "want", "can", "could", "would", "should", "maybe", "sure",
            "okay", "ok", "alright", "right", "left"
        )
        val bisayaWordSet = setOf(
            "ako", "ikaw", "siya", "kami", "kita", "sila", "palihug", "salamat", "maayong",
            "buntag", "gabi", "gabii", "unsa", "asa", "ngano", "kanus-a", "pwede", "pwdi",
            "gusto", "pasensya", "laag", "adto", "unya", "karun", "karon"
        )
        words.forEach { word ->
            if (word in englishWordSet) englishScore += 3
            if (word in bisayaWordSet) bisayaScore += 3
        }

        if (words.any { it.contains("'m") || it.contains("n't") || it.contains("'re") }) {
            englishScore += 2
        }

        val hasKana = trimmed.any {
            val block = Character.UnicodeBlock.of(it)
            block == Character.UnicodeBlock.HIRAGANA || block == Character.UnicodeBlock.KATAKANA
        }
        val hasKanji = trimmed.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS }
        if (hasKana || hasKanji) japaneseScore += 4

        val latinOnly = trimmed.none {
            val block = Character.UnicodeBlock.of(it)
            block == Character.UnicodeBlock.HIRAGANA ||
                block == Character.UnicodeBlock.KATAKANA ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        }

        return when {
            hasKana || hasKanji -> DictionaryLanguage.JAPANESE
            englishScore >= max(2, bisayaScore + 1) -> DictionaryLanguage.ENGLISH
            bisayaScore >= max(2, englishScore + 1) -> DictionaryLanguage.BISAYA
            latinOnly && englishScore >= bisayaScore -> DictionaryLanguage.ENGLISH
            latinOnly && bisayaScore > englishScore -> DictionaryLanguage.BISAYA
            japaneseScore > 0 -> DictionaryLanguage.JAPANESE
            else -> DictionaryLanguage.UNKNOWN
        }
    }

    override fun onCleared() {
        super.onCleared()
        dictionaryVoiceService.stop()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DictionaryViewModel(application)
            }
        }
    }
}
