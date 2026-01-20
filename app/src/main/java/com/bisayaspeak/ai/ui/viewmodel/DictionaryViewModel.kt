package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

enum class DictionaryMode { EXPLORE, TALK }

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

data class TalkResponse(
    val id: String = UUID.randomUUID().toString(),
    val detectedLanguage: DictionaryLanguage,
    val sourceText: String,
    val translatedText: String,
    val spokenOutput: String,
    val romanized: String,
    val explanation: String
)

sealed interface TalkStatus {
    object Idle : TalkStatus
    object Listening : TalkStatus
    object Processing : TalkStatus
    object Speaking : TalkStatus
    data class Error(val message: String) : TalkStatus
}

data class DictionaryUiState(
    val mode: DictionaryMode = DictionaryMode.EXPLORE,
    val query: String = "",
    val isLoading: Boolean = false,
    val candidates: List<TranslationCandidate> = emptyList(),
    val explanation: AiExplanation? = null,
    val errorMessage: String? = null,
    val talkStatus: TalkStatus = TalkStatus.Idle,
    val talkResponse: TalkResponse? = null,
    val talkHistory: List<TalkResponse> = emptyList(),
    val isManualRecording: Boolean = false,
    val lastTranscript: String = "",
    val detectedLanguage: DictionaryLanguage = DictionaryLanguage.UNKNOWN
)

class DictionaryViewModel(
    application: Application,
    private val repository: OpenAiChatRepository = OpenAiChatRepository(),
    private val whisperRepository: WhisperRepository = WhisperRepository(),
    private val random: Random = Random(System.currentTimeMillis())
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val voiceRecorder = VoiceInputRecorder(application.applicationContext)
    private var currentRecording: File? = null

    private val voiceService by lazy { GeminiVoiceService(application.applicationContext) }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
    }

    fun setMode(mode: DictionaryMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update {
            it.copy(
                mode = mode,
                errorMessage = null,
                talkStatus = TalkStatus.Idle,
                talkResponse = null,
                talkHistory = if (mode == DictionaryMode.TALK) it.talkHistory else emptyList()
            )
        }
        stopCurrentRecording(clearStatus = true)
        voiceService.stop()
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

    fun startPushToTalk() {
        val state = _uiState.value
        if (state.isManualRecording) return
        when (state.talkStatus) {
            TalkStatus.Speaking -> {
                voiceService.stop()
                _uiState.update { it.copy(talkStatus = TalkStatus.Idle) }
            }
            TalkStatus.Processing, TalkStatus.Listening -> return
            else -> Unit
        }
        try {
            val file = voiceRecorder.startRecording()
            currentRecording = file
            _uiState.update { it.copy(isManualRecording = true, talkStatus = TalkStatus.Listening, errorMessage = null) }
        } catch (e: Exception) {
            val message = e.message?.ifBlank { null } ?: "録音の準備中です。もう一度タップしてください。"
            _uiState.update {
                it.copy(
                    isManualRecording = false,
                    talkStatus = TalkStatus.Error(message)
                )
            }
        }
    }

    fun stopPushToTalk(shouldTranslate: Boolean) {
        if (!_uiState.value.isManualRecording) return
        val recorded = try {
            voiceRecorder.stopRecording()
        } catch (e: Exception) {
            val message = e.message?.ifBlank { null } ?: "録音が中断されました。もう一度タップしてください。"
            _uiState.update { it.copy(isManualRecording = false, talkStatus = TalkStatus.Error(message)) }
            null
        }
        _uiState.update { it.copy(isManualRecording = false) }
        if (!shouldTranslate) {
            recorded?.delete()
            currentRecording = null
            _uiState.update { it.copy(talkStatus = TalkStatus.Idle) }
            return
        }
        if (recorded == null || !recorded.exists() || recorded.length() == 0L) {
            currentRecording = null
            _uiState.update {
                it.copy(
                    talkStatus = TalkStatus.Error("音声が検出できませんでした。もう一度タップしてください。")
                )
            }
            return
        }
        currentRecording = recorded
        _uiState.update { it.copy(talkStatus = TalkStatus.Processing) }
        viewModelScope.launch {
            processRecordedFile(recorded)
        }
    }
    private fun stopCurrentRecording(clearStatus: Boolean = false) {
        try {
            voiceRecorder.cancelRecording()
        } catch (_: Exception) {
        }
        currentRecording?.delete()
        currentRecording = null
        if (clearStatus) {
            _uiState.update { it.copy(isManualRecording = false, talkStatus = TalkStatus.Idle) }
        } else {
            _uiState.update { it.copy(isManualRecording = false) }
        }
    }

    private suspend fun processRecordedFile(file: File) {
        try {
            val transcript = whisperRepository.transcribe(file).getOrNull().orEmpty().trim()
            file.delete()
            currentRecording = null
            if (transcript.isBlank()) {
                _uiState.update { it.copy(talkStatus = TalkStatus.Error("音声が検出できませんでした。もう一度タップしてください。")) }
            } else {
                val language = detectLanguage(transcript)
                val talkResponse = requestTalkTranslation(transcript, language)
                deliverTalkResponse(talkResponse, transcript, language)
            }
        } catch (e: Exception) {
            val message = e.message?.ifBlank { null } ?: "音声解析に失敗しました。もう一度タップしてください。"
            _uiState.update { it.copy(talkStatus = TalkStatus.Error(message)) }
        }
    }

    fun replayLastTranslation() {
        val last = _uiState.value.lastTranscript
        if (last.isBlank()) return
        val state = _uiState.value
        if (state.talkStatus !is TalkStatus.Idle && state.talkStatus !is TalkStatus.Error) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(talkStatus = TalkStatus.Processing, errorMessage = null) }
                val language = detectLanguage(last)
                val talkResponse = requestTalkTranslation(last, language)
                deliverTalkResponse(talkResponse, last, language)
            } catch (e: Exception) {
                _uiState.update { it.copy(talkStatus = TalkStatus.Error(e.message ?: "再翻訳に失敗しました")) }
            }
        }
    }

    private fun deliverTalkResponse(
        talkResponse: TalkResponse,
        transcript: String,
        language: DictionaryLanguage
    ) {
        _uiState.update {
            it.copy(
                talkStatus = TalkStatus.Processing,
                talkResponse = talkResponse,
                talkHistory = (listOf(talkResponse) + it.talkHistory).take(10),
                lastTranscript = transcript,
                detectedLanguage = language
            )
        }
        val speechText = talkResponse.translatedText.ifBlank { talkResponse.spokenOutput }
        voiceService.speak(
            text = speechText,
            cue = if (language == DictionaryLanguage.JAPANESE) GeminiVoiceCue.DEFAULT else GeminiVoiceCue.HIGH_PITCH,
            onStart = {
                _uiState.update { it.copy(talkStatus = TalkStatus.Speaking) }
            },
            onComplete = {
                _uiState.update { it.copy(talkStatus = TalkStatus.Idle) }
            },
            onError = { error ->
                voiceService.stop()
                _uiState.update { it.copy(talkStatus = TalkStatus.Error(error.message ?: "音声出力に失敗しました")) }
            }
        )
    }

    private suspend fun requestExploration(query: String): Pair<List<TranslationCandidate>, AiExplanation?> {
        val prompt = buildString {
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
        val lower = trimmed.lowercase()

        val japaneseTokens = listOf("です", "ます", "ました", "ありがとう", "こんにちは", "すみません", "ましたか", "でしょう")
        japaneseTokens.forEach { token -> if (lower.contains(token)) japaneseScore += 2 }

        val bisayaTokens = listOf(
            "ako", "ikaw", "siya", "palihug", "salamat", "maayong", "unsa", "kaayo", "kana",
            "gani", "nimo", "kita", "balik", "laag", "pila", "gikan", "tawo", "nimo", "nato"
        )
        bisayaTokens.forEach { token -> if (lower.contains(token)) bisayaScore += 2 }

        val hasKana = trimmed.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.HIRAGANA || Character.UnicodeBlock.of(it) == Character.UnicodeBlock.KATAKANA }
        val hasKanji = trimmed.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS }
        if (hasKana || hasKanji) japaneseScore += 4

        val latinClusters = listOf("ng", "ka", "pa", "sa", "ta", "gi", "ma")
        latinClusters.forEach { cluster -> if (lower.contains(cluster)) bisayaScore += 1 }

        val latinOnly = trimmed.none {
            val block = Character.UnicodeBlock.of(it)
            block == Character.UnicodeBlock.HIRAGANA ||
                block == Character.UnicodeBlock.KATAKANA ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        }

        return when {
            hasKana || hasKanji -> DictionaryLanguage.JAPANESE
            bisayaScore >= max(2, japaneseScore) -> DictionaryLanguage.BISAYA
            japaneseScore >= max(3, bisayaScore + 2) -> DictionaryLanguage.JAPANESE
            latinOnly -> DictionaryLanguage.BISAYA
            else -> DictionaryLanguage.UNKNOWN
        }
    }

    private suspend fun requestTalkTranslation(text: String, language: DictionaryLanguage): TalkResponse {
        val direction = when (language) {
            DictionaryLanguage.JAPANESE -> "ja_to_ceb"
            DictionaryLanguage.BISAYA -> "ceb_to_ja"
            else -> if (random.nextBoolean()) "ja_to_ceb" else "ceb_to_ja"
        }
        val (sourceLabel, targetLabel) = when (direction) {
            "ja_to_ceb" -> "Japanese" to "Cebuano"
            else -> "Cebuano" to "Japanese"
        }
        val prompt = buildString {
            appendLine("You are a hands-free interpreter shuttling between Japanese and Cebuano.")
            appendLine("Return JSON only with fields: detectedLanguage('ja'|'ceb'), sourceText, translatedText, spokenOutput, romanized, explanation.")
            appendLine("Source language: $sourceLabel. Target language: $targetLabel.")
            appendLine("translatedText MUST always be written in the target language and must be a faithful translation of the input. Never echo the source language verbatim.")
            appendLine("spokenOutput must also be in the target language (natural, short, ready for TTS). If the input already matches the target language, respond with a natural acknowledgement sentence in that target language.")
            appendLine("Explanation should be <= 60 Japanese characters and describe the translation choice.")
            appendLine("Input: $text")
        }
        val raw = repository.generateJsonResponse(prompt, temperature = 0.4)
        val cleaned = raw.trim().removePrefix("```json").removeSuffix("```")
        val json = JSONObject(cleaned)
        val detectedCode = json.optString("detectedLanguage", "ja")
        val detected = when (detectedCode.lowercase()) {
            "ja" -> DictionaryLanguage.JAPANESE
            "ceb", "bisaya", "cebuano" -> DictionaryLanguage.BISAYA
            else -> language
        }
        return TalkResponse(
            detectedLanguage = detected,
            sourceText = json.optString("sourceText", text),
            translatedText = json.optString("translatedText"),
            spokenOutput = json.optString("spokenOutput"),
            romanized = json.optString("romanized"),
            explanation = json.optString("explanation")
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentRecording(clearStatus = true)
        voiceService.stop()
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
