package com.bisayaspeak.ai.ui.roleplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.RoleplayHistoryRepository
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.utils.MistakeManager
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class RoleplayOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val hint: String? = null,
    val tone: String? = null,
    val nextTurnId: String? = null,
    val branchKey: String? = null,
    val branchValue: String? = null,
    val requiresPro: Boolean = false
)

data class RoleplayResultPayload(
    val correctCount: Int,
    val totalQuestions: Int,
    val earnedXp: Int,
    val clearedLevel: Int,
    val leveledUp: Boolean
)

data class RoleplayUiState(
    val currentScenario: RoleplayScenarioDefinition? = null,
    val missionGoal: String = "",
    val aiCharacterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = "",
    val isLoading: Boolean = false,
    val options: List<RoleplayOption> = emptyList(),
    val peekedHintOptionIds: Set<String> = emptySet(),
    val completedTurns: Int = 0,
    val successfulTurns: Int = 0,
    val showCompletionDialog: Boolean = false,
    val completionScore: Int = 0,
    val pendingUnlockLevel: Int? = null,
    val pendingResult: RoleplayResultPayload? = null,
    val lockedOption: RoleplayOption? = null,
    val isProUser: Boolean = false,
    val isVoiceRecording: Boolean = false,
    val isVoiceTranscribing: Boolean = false,
    val voiceErrorMessage: String? = null,
    val lastTranscribedText: String? = null,
    val isSavingHistory: Boolean = false,
    val saveHistoryError: String? = null,
    val pendingExitHistory: List<MissionHistoryMessage>? = null
)

class RoleplayChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository()
    private val whisperRepository: WhisperRepository = WhisperRepository()
    private val historyRepository: RoleplayHistoryRepository = RoleplayHistoryRepository(application)
    private val voiceRecorder: VoiceInputRecorder = VoiceInputRecorder(application.applicationContext)
    private val userPreferencesRepository = UserPreferencesRepository(application)

    private companion object {
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val COMPLETION_SCORE = 90
        private const val COMPLETION_THRESHOLD = 80
        private const val LOCKED_OPTION_HOLD_MS = 500L
        private const val POST_CLEAR_SILENCE_MS = 1000L
    }

    private val _uiState = MutableStateFlow(RoleplayUiState())
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val history = mutableListOf<MissionHistoryMessage>()
    private var scriptedRuntime: ScriptedRuntime? = null
    private var isProVersion: Boolean = false
    private val branchFacts = mutableMapOf<String, String>()
    private var currentUserGender: UserGender = UserGender.OTHER
    private var userCallSign: String = "パートナー（Friend）"
    private var calloutBisaya: String = "Friend"
    private var calloutEnglish: String = "Friend"
    private var currentRecordingFile: File? = null

    init {
        observeUserGender()
    }

    fun setProAccess(enabled: Boolean) {
        if (isProVersion == enabled) return
        isProVersion = enabled
        _uiState.update { it.copy(isProUser = enabled) }
    }

    fun saveUserGender(gender: UserGender) {
        viewModelScope.launch {
            userPreferencesRepository.saveUserGender(gender)
        }
    }

    private fun observeUserGender() {
        viewModelScope.launch {
            userPreferencesRepository.userGender.collect { gender ->
                currentUserGender = gender
                when (gender) {
                    UserGender.MALE -> {
                        userCallSign = "彼氏（Gwapo/Handsome）"
                        calloutBisaya = "Gwapo"
                        calloutEnglish = "Handsome"
                    }
                    UserGender.FEMALE -> {
                        userCallSign = "彼女（Gwapa/Beautiful）"
                        calloutBisaya = "Gwapa"
                        calloutEnglish = "Beautiful"
                    }
                    UserGender.OTHER -> {
                        userCallSign = "パートナー（Friend）"
                        calloutBisaya = "Bestie"
                        calloutEnglish = "My friend"
                    }
                }
            }
        }
    }

    fun loadScenario(scenarioId: String, isProUser: Boolean = isProVersion) {
        val definition = getRoleplayScenarioDefinition(scenarioId)
        history.clear()
        branchFacts.clear()
        scriptedRuntime = scriptedScenarioDefinitions[scenarioId]?.let { ScriptedRuntime(it) }
        isProVersion = isProUser

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = definition.systemPrompt,
            messages = emptyList(),
            isLoading = scriptedRuntime == null,
            isProUser = isProVersion
        )

        scriptedRuntime?.let {
            deliverScriptedTurn()
        } ?: requestAiTurn(
            scenario = definition,
            userMessage = START_TOKEN
        )
    }

    fun selectOption(optionId: String) {
        val option = _uiState.value.options.find { it.id == optionId } ?: return
        if (!_uiState.value.isProUser && option.requiresPro) return
        if (_uiState.value.isLoading && scriptedRuntime == null) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = option.text,
            isUser = true,
            translation = option.hint
        )
        history.add(MissionHistoryMessage(option.text, isUser = true))

        val usedHint = option.id in _uiState.value.peekedHintOptionIds

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = scriptedRuntime == null,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                completedTurns = it.completedTurns + 1,
                successfulTurns = it.successfulTurns + 1,
                lockedOption = option
            )
        }

        option.branchKey?.let { key ->
            option.branchValue?.let { value ->
                branchFacts[key] = value
            }
        }

        viewModelScope.launch {
            delay(LOCKED_OPTION_HOLD_MS)
            _uiState.update { it.copy(lockedOption = null) }
            delay(POST_CLEAR_SILENCE_MS)
            proceedToNextTurn(option)
        }
    }

    fun submitFreeFormMessage(inputText: String) {
        val scenario = _uiState.value.currentScenario ?: return
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isLoading) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )
        history.add(MissionHistoryMessage(trimmed, isUser = true))
        scriptedRuntime = null

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                completedTurns = it.completedTurns + 1,
                successfulTurns = it.successfulTurns + 1,
                lockedOption = null
            )
        }

        viewModelScope.launch {
            requestAiTurn(scenario, trimmed)
        }
    }

    fun startVoiceRecording() {
        if (_uiState.value.isVoiceRecording || _uiState.value.isVoiceTranscribing || _uiState.value.isLoading) return
        try {
            val file = voiceRecorder.startRecording()
            currentRecordingFile = file
            _uiState.update {
                it.copy(
                    isVoiceRecording = true,
                    voiceErrorMessage = null,
                    lastTranscribedText = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isVoiceRecording = false,
                    voiceErrorMessage = "録音開始に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
        currentRecordingFile?.delete()
        currentRecordingFile = null
        _uiState.update {
            it.copy(
                isVoiceRecording = false,
                isVoiceTranscribing = false,
                voiceErrorMessage = null
            )
        }
    }

    fun stopVoiceRecordingAndSend() {
        if (!_uiState.value.isVoiceRecording) return
        val recordedFile = try {
            voiceRecorder.stopRecording()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isVoiceRecording = false,
                    voiceErrorMessage = "録音停止に失敗しました: ${e.message}"
                )
            }
            null
        }

        _uiState.update { it.copy(isVoiceRecording = false) }

        if (recordedFile == null || !recordedFile.exists() || recordedFile.length() == 0L) {
            recordedFile?.delete()
            currentRecordingFile = null
            _uiState.update {
                it.copy(
                    voiceErrorMessage = "音声が記録されませんでした。もう一度お試しください。"
                )
            }
            return
        }

        currentRecordingFile = recordedFile
        transcribeAndSend(recordedFile)
    }

    private fun transcribeAndSend(file: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVoiceTranscribing = true,
                    voiceErrorMessage = null
                )
            }
            val result = whisperRepository.transcribe(file)
            file.delete()
            if (currentRecordingFile == file) {
                currentRecordingFile = null
            }
            result.fold(
                onSuccess = { text ->
                    val trimmed = text.trim()
                    if (trimmed.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isVoiceTranscribing = false,
                                voiceErrorMessage = "音声の内容を聞き取れませんでした。"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isVoiceTranscribing = false,
                                lastTranscribedText = trimmed
                            )
                        }
                        submitFreeFormMessage(trimmed)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isVoiceTranscribing = false,
                            voiceErrorMessage = "音声解析に失敗しました: ${error.message ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    private suspend fun proceedToNextTurn(option: RoleplayOption) {
        scriptedRuntime?.let {
            val nextTurnId = resolveNextTurnId(option, it)
            if (nextTurnId == null) {
                finalizeScriptedScenario()
            } else {
                it.currentTurnId = nextTurnId
                deliverScriptedTurn()
            }
            return
        }

        val scenario = _uiState.value.currentScenario ?: return
        requestAiTurn(scenario, option.text)
    }

    fun markHintPeeked(optionId: String) {
        val option = _uiState.value.options.find { it.id == optionId } ?: return
        MistakeManager.addMistake(option.text)
        _uiState.update {
            it.copy(peekedHintOptionIds = it.peekedHintOptionIds + optionId)
        }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
    }

    fun consumePendingResult() {
        _uiState.update { it.copy(pendingResult = null) }
    }

    fun markUnlockHandled() {
        _uiState.update { it.copy(pendingUnlockLevel = null) }
    }

    fun consumePendingExitHistory() {
        _uiState.update { it.copy(pendingExitHistory = null) }
    }

    fun notifyVoicePlaybackStarted(messageId: String) {
        _speakingMessageId.value = messageId
    }

    fun notifyVoicePlaybackFinished(messageId: String) {
        if (_speakingMessageId.value == messageId) {
            _speakingMessageId.value = null
        }
    }

    private fun deliverScriptedTurn() {
        val runtime = scriptedRuntime ?: return
        val nextTurnId = runtime.currentTurnId
        if (nextTurnId == null) {
            finalizeScriptedScenario()
            return
        }
        val turn = runtime.scenario.turns[nextTurnId]
        if (turn == null) {
            finalizeScriptedScenario()
            return
        }
        runtime.awaitingTurnId = turn.id
        runtime.currentTurnId = null

        history.add(MissionHistoryMessage(turn.aiText, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = turn.aiText,
            isUser = false,
            translation = turn.translation,
            voiceCue = turn.voiceCue
        )
        val options = turn.options.map {
            RoleplayOption(
                text = it.text,
                hint = if (isProVersion) it.translation else null,
                tone = null,
                nextTurnId = it.nextTurnId,
                branchKey = it.branchKey,
                branchValue = it.branchValue,
                requiresPro = it.requiresPro
            )
        }.filterForAccess()

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = options,
                peekedHintOptionIds = emptySet(),
                lockedOption = null
            )
        }
    }

    fun forceCompleteScenario() {
        queueCompletion(calculateScore())
    }

    private fun finalizeScriptedScenario() {
        scriptedRuntime = null
        queueCompletion(calculateScore())
    }

    private fun requestAiTurn(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = buildRoleplayPrompt(scenario, userMessage)
                val rawResponse = chatRepository.generateJsonResponse(prompt)
                val payload = parseRoleplayPayload(rawResponse)
                applyAiPayload(payload)
            } catch (e: Exception) {
                val fallbackText = "AIの応答取得に失敗しました: ${e.message ?: "Unknown error"}"
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = fallbackText,
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMsg,
                        isLoading = false,
                        options = emptyList(),
                        peekedHintOptionIds = emptySet(),
                        lockedOption = null
                    )
                }
            }
        }
    }

    private fun applyAiPayload(payload: RoleplayAiResponsePayload) {
        val aiSpeech = payload.aiSpeech.ifBlank { "..." }
        history.add(MissionHistoryMessage(aiSpeech, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = aiSpeech,
            isUser = false,
            translation = payload.aiTranslation.takeIf { it.isNotBlank() },
            voiceCue = GeminiVoiceCue.HIGH_PITCH
        )
        val options = payload.options
            .filter { it.text.isNotBlank() }
            .map {
                RoleplayOption(
                    text = it.text,
                    hint = if (isProVersion) it.translation else null,
                    tone = it.tone
                )
            }.filterForAccess()

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = options,
                peekedHintOptionIds = emptySet(),
                lockedOption = null
            )
        }

        if (options.isEmpty()) {
            queueCompletion(calculateScore())
        }
    }

    private fun buildResultPayload(score: Int): RoleplayResultPayload {
        val totalTurns = _uiState.value.completedTurns.coerceAtLeast(1)
        val successful = _uiState.value.successfulTurns.coerceAtMost(totalTurns)
        val xp = (successful * 20).coerceAtLeast(10)
        val clearedLevel = _uiState.value.currentScenario?.level ?: 1
        val leveledUp = score >= COMPLETION_THRESHOLD
        return RoleplayResultPayload(
            correctCount = successful,
            totalQuestions = totalTurns,
            earnedXp = xp,
            clearedLevel = clearedLevel,
            leveledUp = leveledUp
        )
    }

    private fun queueCompletion(score: Int) {
        if (_uiState.value.pendingResult != null) return
        val scenarioLevel = _uiState.value.currentScenario?.level ?: 1
        val payload = buildResultPayload(score)
        _uiState.update {
            it.copy(
                isLoading = false,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                showCompletionDialog = false,
                completionScore = score,
                pendingUnlockLevel = if (score >= COMPLETION_THRESHOLD) scenarioLevel + 1 else null,
                pendingResult = payload,
                lockedOption = null
            )
        }
    }

    private fun calculateScore(): Int {
        val turns = _uiState.value.completedTurns
        return if (turns > 0) {
            ((_uiState.value.successfulTurns.toFloat() / turns.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            COMPLETION_SCORE
        }
    }

    fun saveAndExit() {
        if (_uiState.value.isSavingHistory) return
        val snapshot = history.map { MissionHistoryMessage(it.text, it.isUser) }
        if (snapshot.isEmpty()) {
            _uiState.update {
                it.copy(saveHistoryError = "会話履歴がありません。", isSavingHistory = false)
            }
            return
        }
        val scenarioId = _uiState.value.currentScenario?.id
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingHistory = true, saveHistoryError = null) }
            val result = historyRepository.saveSession(scenarioId, snapshot)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingHistory = false,
                            pendingExitHistory = snapshot,
                            saveHistoryError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingHistory = false,
                            saveHistoryError = error.message ?: "保存に失敗しました。"
                        )
                    }
                }
            )
        }
    }

    private fun buildRoleplayPrompt(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ): String {
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }

        val hints = scenario.hintPhrases.joinToString(separator = "\n") {
            "- ${it.nativeText} (${it.translation})"
        }.ifBlank { "- (none)" }

        val learnerGenderLabel = when (currentUserGender) {
            UserGender.MALE -> "male (男性)"
            UserGender.FEMALE -> "female (女性)"
            UserGender.OTHER -> "non-binary / undisclosed"
        }
        val genderInstruction = """
            SYSTEM DIRECTIVE:
            - You are Tari, a real Cebuana woman living in Cebu.
            - You must always speak Bisaya (Cebuano) in your dialogue. Never mix Japanese or English into your speech field.
            - Provide a separate Japanese translation ONLY inside the dedicated translation field.
            - The learner is $learnerGenderLabel. Address them affectionately as "$calloutBisaya" (Bisaya) or "$calloutEnglish" (English) and never use other titles.
            - Open every greeting or turn by referring to the learner with one of those approved names.
            - Throughout the entire conversation, keep using only those approved callouts whenever you refer to the learner.
            - Stay affectionate, supportive, and slightly mischievous, but never break character.
            - The learner can submit free-form voice transcriptions or typed input beyond the suggested options. Interpret any Japanese/English phrases they use, convert their intent into Bisaya context, and continue the mission naturally.
            - Never reject or scold the learner for ad-lib input. Absorb it into the story, react with empathy, and keep the pacing lively.
            - If the learner introduces new plot ideas or feelings, weave them into the narrative and adjust your guidance accordingly while staying aligned with the scenario goal.
        """.trimIndent()

        val basePrompt = if (scenario.systemPrompt.isBlank()) {
            """
            You are ${scenario.aiRole}.
            Situation: ${scenario.situation}
            Goal: ${scenario.goal}
            """.trimIndent()
        } else scenario.systemPrompt

        val systemPromptWithGender = buildString {
            append(basePrompt)
            append("\n\n")
            append(genderInstruction)
        }.trim()

        return """
            $systemPromptWithGender

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $userMessage

            Respond strictly in JSON with exactly these fields and no extras:
            {
              "aiSpeech": "Assistant reply in Bisaya ONLY. Do not include Japanese or English.",
              "aiTranslation": "Japanese translation of aiSpeech ONLY.",
              "options": [
                {
                  "text": "Suggested learner reply in Bisaya ONLY.",
                  "translation": "Japanese translation of that option ONLY.",
                  "tone": "Short descriptor in Japanese (optional)."
                }
              ]
            }
            Output 2-3 concise options that build upon the learner's most recent intent. Never include markdown or explanations outside JSON.
        """.trimIndent()
    }

    private fun parseRoleplayPayload(raw: String): RoleplayAiResponsePayload {
        return try {
            val cleaned = raw
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()
            val normalized = extractJsonObject(cleaned)
            val json = JSONObject(normalized)
            val aiSpeech = json.optString("aiSpeech", json.optString("aiResponse", raw))
            val aiTranslation = json.optString("aiTranslation", "")
            val optionsArray = json.optJSONArray("options") ?: JSONArray()
            val options = mutableListOf<RoleplayAiOption>()
            for (i in 0 until optionsArray.length()) {
                val item = optionsArray.optJSONObject(i) ?: continue
                options += RoleplayAiOption(
                    text = item.optString("text"),
                    translation = item.optString("translation"),
                    tone = item.optString("tone")
                )
            }
            RoleplayAiResponsePayload(
                aiSpeech = aiSpeech,
                aiTranslation = aiTranslation,
                options = options
            )
        } catch (_: Exception) {
            RoleplayAiResponsePayload(
                aiSpeech = raw,
                aiTranslation = "",
                options = emptyList()
            )
        }
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    private data class RoleplayAiResponsePayload(
        val aiSpeech: String,
        val aiTranslation: String,
        val options: List<RoleplayAiOption>
    )

    private data class RoleplayAiOption(
        val text: String,
        val translation: String?,
        val tone: String?
    )

    private fun List<RoleplayOption>.filterForAccess(): List<RoleplayOption> {
        if (isProVersion) return this
        return this.filterNot { it.requiresPro }
    }

    private fun resolveNextTurnId(option: RoleplayOption, runtime: ScriptedRuntime): String? {
        option.nextTurnId?.let { return it }
        val lastTurnId = runtime.awaitingTurnId ?: return null
        val turn = runtime.scenario.turns[lastTurnId] ?: return null
        return turn.defaultNextId
    }

    override fun onCleared() {
        super.onCleared()
        cancelVoiceRecording()
    }
}

private data class ScriptedScenario(
    val startTurnId: String,
    val turns: Map<String, ScriptedTurn>
)

private data class ScriptedTurn(
    val id: String,
    val aiText: String,
    val translation: String,
    val voiceCue: GeminiVoiceCue = GeminiVoiceCue.HIGH_PITCH,
    val options: List<ScriptedOption> = emptyList(),
    val defaultNextId: String? = null
)

private data class ScriptedOption(
    val text: String,
    val translation: String,
    val nextTurnId: String? = null,
    val branchKey: String? = null,
    val branchValue: String? = null,
    val requiresPro: Boolean = false
)

private class ScriptedRuntime(val scenario: ScriptedScenario) {
    var currentTurnId: String? = scenario.startTurnId
    var awaitingTurnId: String? = null
}

private val scriptedScenarioDefinitions: Map<String, ScriptedScenario> = mapOf(
    "rp_tarsier_morning" to ScriptedScenario(
        startTurnId = "intro",
        turns = mapOf(
            "intro" to ScriptedTurn(
                id = "intro",
                aiText = "Maayong buntag! Ako si Tarsier Master Tali. Handom ko nga ma-init imong adlaw.",
                translation = "おはよう！タルシエ先生タリだよ。今日も良い朝にしよう。",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Maayong buntag, Tali! Andam ko sa leksyon.",
                        translation = "おはようタリ！レッスンの準備できてるよ。",
                        nextTurnId = "mood_check",
                        branchKey = "greeting",
                        branchValue = "energetic"
                    ),
                    ScriptedOption(
                        text = "Maayong buntag! Nalipay ko makakita nimo.",
                        translation = "おはよう！会えてうれしいよ。",
                        nextTurnId = "mood_check",
                        branchKey = "greeting",
                        branchValue = "tender"
                    )
                ),
                defaultNextId = "mood_check"
            ),
            "mood_check" to ScriptedTurn(
                id = "mood_check",
                aiText = "Kumusta ka karon? Murag daghan ka'g enerhiya ron!",
                translation = "調子はどう？すごく元気そうだね！",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Maayo ra ko, salamat! Motivated ko kaayo.",
                        translation = "元気だよ、ありがとう！やる気満々。",
                        nextTurnId = "react_positive",
                        branchKey = "mood",
                        branchValue = "positive"
                    ),
                    ScriptedOption(
                        text = "Gikapoy gamay pero padayon gihapon.",
                        translation = "ちょっと疲れてるけど頑張るよ。",
                        nextTurnId = "react_tired",
                        branchKey = "mood",
                        branchValue = "tired"
                    )
                ),
                defaultNextId = "react_positive"
            ),
            "react_positive" to ScriptedTurn(
                id = "react_positive",
                aiText = "Nice kaayo! Ang imong energy makahawa. Sugdan nato ang papel-play adventure!",
                translation = "いいね！そのエネルギーが伝わってくるよ。さあ紙芝居アドベンチャーを始めよう！",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Ready ko! Asa ta maglakaw?",
                        translation = "準備完了！どこに行こうか？",
                        nextTurnId = "wrap_confident"
                    ),
                    ScriptedOption(
                        text = "Game! Dad-a ko sa imong secret shortcut.",
                        translation = "ゲーム開始！秘密の近道を見せて。",
                        nextTurnId = "wrap_confident",
                        requiresPro = true
                    )
                ),
                defaultNextId = "wrap_confident"
            ),
            "react_tired" to ScriptedTurn(
                id = "react_tired",
                aiText = "Sige lang, naa ra ko diri motabang nimo. Dali ra ni, promise!",
                translation = "大丈夫、私がそばで支えるから。あっという間だよ！",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Salamat, Tali! Hinay-hinay lang ta.",
                        translation = "ありがとうタリ！ゆっくり進もう。",
                        nextTurnId = "wrap_encourage"
                    ),
                    ScriptedOption(
                        text = "Okay ra, kay kung naa ka diha kusgan ko.",
                        translation = "君がいるなら頑張れるよ。",
                        nextTurnId = "wrap_encourage"
                    )
                ),
                defaultNextId = "wrap_encourage"
            ),
            "wrap_confident" to ScriptedTurn(
                id = "wrap_confident",
                aiText = "Tan-awa! Sa imong pagpili, mas ni-bright ang kagabhion. Balik ta unya para sa sunod nga page!",
                translation = "ほら！君の選択で夜道も明るくなったよ。また次のページで会おうね！",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Magkita ta unya! Dal-i ko sunod nga lakaw.",
                        translation = "またね！次の散歩も早くしたい。",
                        nextTurnId = null
                    ),
                    ScriptedOption(
                        text = "Mo-report ko kay Sensei sunod ha!",
                        translation = "次は先生に報告するね！",
                        nextTurnId = null
                    )
                )
            ),
            "wrap_encourage" to ScriptedTurn(
                id = "wrap_encourage",
                aiText = "Nakaya ra nimo bisan kapoy! Proud kaayo ko nimo. Pahuway gamay, unya balik para sa chapter 2.",
                translation = "疲れててもやり遂げたね！とっても誇らしいよ。少し休んでから第2章で会おう！",
                voiceCue = GeminiVoiceCue.HIGH_PITCH,
                options = listOf(
                    ScriptedOption(
                        text = "Sige, pahulay sa ko. Kita ta usab!",
                        translation = "うん、ちょっと休むね。また会おう！",
                        nextTurnId = null
                    ),
                    ScriptedOption(
                        text = "Thanks sa encouragement! Mag practice ko.",
                        translation = "励ましありがとう！練習しておくね。",
                        nextTurnId = null
                    )
                )
            )
        )
    )
)
