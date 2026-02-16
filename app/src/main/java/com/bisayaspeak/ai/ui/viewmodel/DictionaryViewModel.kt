package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.data.repository.FreeUsageManager
import com.bisayaspeak.ai.data.repository.FreeUsageRepository
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.PromptProvider
import com.bisayaspeak.ai.data.repository.WhisperRepository
import android.util.Log
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val explanation: String,
    val targetLanguage: DictionaryLanguage = DictionaryLanguage.BISAYA
)

sealed interface TalkStatus {
    object Idle : TalkStatus
    object Listening : TalkStatus
    object Processing : TalkStatus
    object Speaking : TalkStatus
    data class Error(val message: String) : TalkStatus
}

sealed interface TalkUsageStatus {
    val dayKey: String
    val usedCount: Int
    val maxCount: Int
    val remaining: Int get() = (maxCount - usedCount).coerceAtLeast(0)

    data class Allowed(
        override val dayKey: String,
        override val usedCount: Int,
        override val maxCount: Int
    ) : TalkUsageStatus

    data class NeedsAdBeforeUse(
        override val dayKey: String,
        override val usedCount: Int,
        override val maxCount: Int
    ) : TalkUsageStatus

    data class Blocked(
        override val dayKey: String,
        override val usedCount: Int,
        override val maxCount: Int
    ) : TalkUsageStatus
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
    val detectedLanguage: DictionaryLanguage = DictionaryLanguage.UNKNOWN,
    val dictionaryTtsState: DictionaryTtsState = DictionaryTtsState.Idle,
    val talkUsageStatus: TalkUsageStatus? = null
)

sealed interface DictionaryTtsState {
    object Idle : DictionaryTtsState
    object Playing : DictionaryTtsState
    data class Error(val message: String) : DictionaryTtsState
}

enum class TalkUsageReason(val logTag: String) {
    START_RECORDING("start"),
    SEND("send"),
    CANCEL("cancel")
}

sealed interface DictionaryTalkEvent {
    data class RequireAd(val status: TalkUsageStatus.NeedsAdBeforeUse, val reason: TalkUsageReason) : DictionaryTalkEvent
    data class ReachedLimit(val status: TalkUsageStatus.Blocked, val reason: TalkUsageReason) : DictionaryTalkEvent
}

private sealed interface TalkGateResult {
    data object Allowed : TalkGateResult
    data class RequiresAd(val status: TalkUsageStatus.NeedsAdBeforeUse) : TalkGateResult
    data class Blocked(val status: TalkUsageStatus.Blocked) : TalkGateResult
}

private enum class TalkPendingAction {
    START_RECORDING
}

class DictionaryViewModel(
    application: Application,
    private val repository: OpenAiChatRepository = OpenAiChatRepository(),
    private val promptProvider: PromptProvider = PromptProvider(application),
    private val whisperRepository: WhisperRepository = WhisperRepository(),
    private val random: Random = Random(System.currentTimeMillis())
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val _talkEvents = MutableSharedFlow<DictionaryTalkEvent>(extraBufferCapacity = 1)
    val talkEvents: SharedFlow<DictionaryTalkEvent> = _talkEvents.asSharedFlow()

    private val voiceRecorder = VoiceInputRecorder(application.applicationContext)
    private var currentRecording: File? = null
    private var latestPremiumStatus: Boolean = false
    private var pendingAction: TalkPendingAction? = null

    private val voiceService by lazy { GeminiVoiceService(application.applicationContext) }
    private val dictionaryVoiceService by lazy { GeminiVoiceService(application.applicationContext) }
    private val hasOpenAiKey = BuildConfig.OPENAI_API_KEY.isNotBlank()
    private val prefersJapaneseOutput: Boolean = run {
        val locale = application.resources.configuration.locales[0]
        (locale?.language ?: Locale.getDefault().language).equals("ja", ignoreCase = true)
    }

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
        dictionaryVoiceService.stop()
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

    init {
        viewModelScope.launch {
            PremiumStatusProvider.isPremiumUser.collectLatest { isPremium ->
                latestPremiumStatus = isPremium
                if (isPremium) {
                    _uiState.update { it.copy(talkUsageStatus = null) }
                } else {
                    FreeUsageManager.resetIfNewDay()
                    refreshTalkUsageStatus()
                }
            }
        }
    }

    fun startPushToTalk() {
        startPushToTalkInternal(bypassAdGate = false)
    }

    private fun startPushToTalkInternal(bypassAdGate: Boolean) {
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
        viewModelScope.launch {
            when (val result = checkAndConsumeTalkTurn(TalkUsageReason.START_RECORDING, bypassAdGate)) {
                TalkGateResult.Allowed -> startRecordingInternal()
                is TalkGateResult.RequiresAd -> {
                    pendingAction = TalkPendingAction.START_RECORDING
                    _talkEvents.emit(DictionaryTalkEvent.RequireAd(result.status, TalkUsageReason.START_RECORDING))
                }
                is TalkGateResult.Blocked -> {
                    pendingAction = null
                    _talkEvents.emit(DictionaryTalkEvent.ReachedLimit(result.status, TalkUsageReason.START_RECORDING))
                }
            }
        }
    }

    private fun startRecordingInternal() {
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
            processRecordedFile(recorded, latestPremiumStatus)
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

    private suspend fun processRecordedFile(file: File, isPremiumUser: Boolean) {
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

    private suspend fun refreshTalkUsageStatus() {
        if (latestPremiumStatus) {
            _uiState.update { it.copy(talkUsageStatus = null) }
            return
        }
        val day = FreeUsageManager.dayKey() ?: FreeUsageManager.currentDayKey()
        val used = FreeUsageManager.talkTurnCount()
        val max = FreeUsageRepository.MAX_TALK_TURNS_PER_DAY
        val status = when {
            used >= max -> TalkUsageStatus.Blocked(day, used, max)
            used == max - 1 -> TalkUsageStatus.NeedsAdBeforeUse(day, used, max)
            else -> TalkUsageStatus.Allowed(day, used, max)
        }
        _uiState.update { it.copy(talkUsageStatus = status) }
    }

    fun onAdResult(granted: Boolean) {
        val pending = pendingAction ?: return
        pendingAction = null
        if (!granted) return
        when (pending) {
            TalkPendingAction.START_RECORDING -> startPushToTalkInternal(bypassAdGate = true)
        }
    }

    private suspend fun checkAndConsumeTalkTurn(
        reason: TalkUsageReason,
        bypassAdGate: Boolean
    ): TalkGateResult {
        if (latestPremiumStatus) {
            FreeUsageManager.logUsage("free_limit_check feature=dict_talk result=allow_premium premium=true reason=${reason.logTag}")
            return TalkGateResult.Allowed
        }

        FreeUsageManager.resetIfNewDay()
        val status = fetchLatestTalkStatus()
        _uiState.update { it.copy(talkUsageStatus = status) }

        return when (status) {
            is TalkUsageStatus.Allowed -> {
                FreeUsageManager.consumeTalkTurn()
                val next = fetchLatestTalkStatus()
                _uiState.update { it.copy(talkUsageStatus = next) }
                logUsage(reason, "allow", next)
                TalkGateResult.Allowed
            }

            is TalkUsageStatus.NeedsAdBeforeUse -> {
                if (bypassAdGate) {
                    FreeUsageManager.consumeTalkTurn()
                    val next = fetchLatestTalkStatus()
                    _uiState.update { it.copy(talkUsageStatus = next) }
                    logUsage(reason, "allow_after_ad", next)
                    TalkGateResult.Allowed
                } else {
                    logUsage(reason, "ad", status)
                    TalkGateResult.RequiresAd(status)
                }
            }

            is TalkUsageStatus.Blocked -> {
                logUsage(reason, "cta", status)
                TalkGateResult.Blocked(status)
            }
        }
    }

    private suspend fun fetchLatestTalkStatus(): TalkUsageStatus {
        val day = FreeUsageManager.dayKey() ?: FreeUsageManager.currentDayKey()
        val used = FreeUsageManager.talkTurnCount()
        val max = FreeUsageRepository.MAX_TALK_TURNS_PER_DAY
        return when {
            used >= max -> TalkUsageStatus.Blocked(day, used, max)
            used == max - 1 -> TalkUsageStatus.NeedsAdBeforeUse(day, used, max)
            else -> TalkUsageStatus.Allowed(day, used, max)
        }
    }

    private suspend fun logUsage(reason: TalkUsageReason, outcome: String, status: TalkUsageStatus) {
        val installId = runCatching { FreeUsageManager.installId() }.getOrNull()
        FreeUsageManager.logUsage(
            "free_limit_check feature=dict_talk result=$outcome reason=${reason.logTag} day=${status.dayKey} " +
                "count=${status.usedCount}/${status.maxCount} premium=$latestPremiumStatus install=${installId ?: "n/a"}"
        )
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
        val cue = if (language == DictionaryLanguage.JAPANESE) {
            GeminiVoiceCue.TALK_HIGH
        } else {
            GeminiVoiceCue.TALK_CONVERSATION
        }
        voiceService.speak(
            text = speechText,
            cue = cue,
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

    private data class TalkDirectionSpec(
        val sourceLabel: String,
        val targetLabel: String,
        val targetLanguage: DictionaryLanguage
    )

    private fun resolveTalkDirection(language: DictionaryLanguage): TalkDirectionSpec {
        return when (language) {
            DictionaryLanguage.ENGLISH -> TalkDirectionSpec("English", "Cebuano", DictionaryLanguage.BISAYA)
            DictionaryLanguage.BISAYA -> {
                if (prefersJapaneseOutput) {
                    TalkDirectionSpec("Cebuano", "Japanese", DictionaryLanguage.JAPANESE)
                } else {
                    TalkDirectionSpec("Cebuano", "English", DictionaryLanguage.ENGLISH)
                }
            }
            DictionaryLanguage.JAPANESE -> TalkDirectionSpec("Japanese", "Cebuano", DictionaryLanguage.BISAYA)
            else -> TalkDirectionSpec("English", "Cebuano", DictionaryLanguage.BISAYA)
        }
    }

    private suspend fun requestTalkTranslation(text: String, language: DictionaryLanguage): TalkResponse {
        val direction = resolveTalkDirection(language)
        val prompt = buildString {
            appendLine(promptProvider.getSystemPrompt())
            appendLine("You are a hands-free interpreter shuttling between ${direction.sourceLabel} and ${direction.targetLabel}.")
            appendLine("Return JSON only with fields: detectedLanguage('en'|'ceb'), sourceText, translatedText, spokenOutput, romanized, explanation.")
            appendLine("Source language: ${direction.sourceLabel}. Target language: ${direction.targetLabel}.")
            appendLine("translatedText MUST always be written in ${direction.targetLabel} and must be a faithful translation of the input. Never echo the source language verbatim.")
            appendLine("spokenOutput must also be in ${direction.targetLabel} (natural, short, ready for TTS). If the input already matches the target language, respond with a natural acknowledgement sentence in that target language.")
            val explanationLanguage = when {
                promptProvider.getSystemPrompt().contains("Do not output Japanese") -> "English"
                direction.targetLabel == "Japanese" -> "Japanese"
                prefersJapaneseOutput -> "Japanese"
                else -> "English"
            }
            appendLine("Explanation should be <= 60 $explanationLanguage characters and describe the translation choice.")
            appendLine("Input: $text")
        }
        val raw = repository.generateJsonResponse(prompt, temperature = 0.4)
        val cleaned = raw.trim().removePrefix("```json").removeSuffix("```")
        val json = JSONObject(cleaned)
        val detectedCode = json.optString("detectedLanguage", "en")
        val detected = when (detectedCode.lowercase()) {
            "ja", "jp" -> DictionaryLanguage.JAPANESE
            "en", "eng", "english" -> DictionaryLanguage.ENGLISH
            "ceb", "bisaya", "cebuano" -> DictionaryLanguage.BISAYA
            else -> language
        }
        return TalkResponse(
            detectedLanguage = detected,
            sourceText = json.optString("sourceText", text),
            translatedText = json.optString("translatedText"),
            spokenOutput = json.optString("spokenOutput"),
            romanized = json.optString("romanized"),
            explanation = json.optString("explanation"),
            targetLanguage = direction.targetLanguage
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentRecording(clearStatus = true)
        voiceService.stop()
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
