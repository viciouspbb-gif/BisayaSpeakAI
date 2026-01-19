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
import com.bisayaspeak.ai.data.repository.UsageRepository
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
import kotlin.random.Random
import kotlin.text.RegexOption

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
    val pendingExitHistory: List<MissionHistoryMessage>? = null,
    val userGender: UserGender = UserGender.OTHER,
    val activeThemeTitle: String = "",
    val activeThemeDescription: String = "",
    val turnsTarget: Int = 0,
    val activeThemePersona: String = "",
    val activeThemeGoal: String = "",
    val activeThemeFlavor: RoleplayThemeFlavor = RoleplayThemeFlavor.CASUAL,
    val activeThemeIntroLine: String = "",
    val activeThemeFarewellBisaya: String = "",
    val activeThemeFarewellTranslation: String = "",
    val userLevel: Int = 1,
    val totalXp: Int = 0
)

class RoleplayChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository()
    private val whisperRepository: WhisperRepository = WhisperRepository()
    private val historyRepository: RoleplayHistoryRepository = RoleplayHistoryRepository(application)
    private val voiceRecorder: VoiceInputRecorder = VoiceInputRecorder(application.applicationContext)
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val usageRepository = UsageRepository(application)
    private val random = Random(System.currentTimeMillis())
    private val themeManager = RoleplayThemeManager(random)

    private companion object {
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val COMPLETION_SCORE = 90
        private const val COMPLETION_THRESHOLD = 80
        private const val LOCKED_OPTION_HOLD_MS = 500L
        private const val POST_CLEAR_SILENCE_MS = 1000L
        private const val CASUAL_MIN_TURN = 8
        private const val CASUAL_MAX_TURN = 10
        private const val SCENARIO_MAX_TURN = 12
        private val levelPrefixRegex = Regex("^LV\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE)
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
    private var activeTheme: RoleplayThemeDefinition = themeManager.drawTheme(1, RoleplayThemeFlavor.CASUAL)
    private var activeFlavor: RoleplayThemeFlavor = RoleplayThemeFlavor.CASUAL
    private var currentUserLevel: Int = 1
    private var lastRandomThemeId: String? = null
    private var endingTurnTarget: Int = SCENARIO_MAX_TURN
    private var isCasualTheme: Boolean = true
    private var endingTriggered: Boolean = false
    private var scriptedTurnsRemaining: Int = SCENARIO_MAX_TURN

    init {
        observeUserGender()
        observeUserProgress()
    }

    private fun observeUserProgress() {
        viewModelScope.launch {
            usageRepository.getCurrentLevel().collect { level ->
                currentUserLevel = level
                _uiState.update { it.copy(userLevel = level) }
            }
        }
        viewModelScope.launch {
            usageRepository.getTotalXP().collect { xp ->
                _uiState.update { it.copy(totalXp = xp) }
            }
        }
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
                _uiState.update { it.copy(userGender = gender) }
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
        selectActiveTheme(definition)

        val cleanThemeTitle = levelPrefixRegex.replace(activeTheme.title, "").trim().ifBlank { activeTheme.title }
        val closingCue = activeTheme.closingCue

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = definition.systemPrompt,
            messages = emptyList(),
            isLoading = scriptedRuntime == null,
            isProUser = isProVersion,
            userGender = currentUserGender,
            activeThemeTitle = cleanThemeTitle,
            activeThemeDescription = activeTheme.description,
            activeThemePersona = activeTheme.persona,
            activeThemeGoal = activeTheme.goalStatement,
            activeThemeFlavor = activeTheme.flavor,
            activeThemeIntroLine = activeTheme.introLine,
            activeThemeFarewellBisaya = closingCue.bisaya,
            activeThemeFarewellTranslation = closingCue.translation,
            turnsTarget = endingTurnTarget
        )

        if (scriptedRuntime == null) {
            injectThemeIntroLine()
        }

        scriptedRuntime?.let {
            deliverScriptedTurn()
        } ?: requestAiTurn(
            scenario = definition,
            userMessage = START_TOKEN
        )
    }

    private fun injectThemeIntroLine() {
        val introLine = activeTheme.introLine.takeIf { it.isNotBlank() } ?: return
        val introMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = introLine,
            isUser = false,
            translation = null,
            voiceCue = GeminiVoiceCue.HIGH_PITCH
        )
        history.add(MissionHistoryMessage(introLine, isUser = false))
        _uiState.update {
            it.copy(
                messages = it.messages + introMessage
            )
        }
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

        if (maybeTriggerEnding(aiMsg)) {
            return
        }

        scriptedTurnsRemaining -= 1
        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = options,
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                turnsTarget = scriptedTurnsRemaining.coerceAtLeast(0)
            )
        }

        if (options.isEmpty() || scriptedTurnsRemaining <= 0) {
            queueCompletion(calculateScore())
        }
    }

    private fun queueCompletion(score: Int) {
        _uiState.update {
            it.copy(
                showCompletionDialog = true,
                completionScore = score.coerceIn(0, 100),
                options = emptyList(),
                peekedHintOptionIds = emptySet()
            )
        }
    }

    private fun calculateScore(): Int {
        val state = _uiState.value
        if (state.completedTurns == 0) return 100
        val ratio = state.successfulTurns.toFloat() / state.completedTurns.toFloat()
        return (ratio * 100).toInt().coerceIn(0, 100)
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
            append("\n\n現在のテーマ: ${activeTheme.title} — ${activeTheme.description}。")
            append("\nタリの役柄: ${activeTheme.persona}")
            append("\n今回の目的: ${activeTheme.goalStatement}")
            append("\nテーマ演技ノート: ${activeTheme.instruction}")
            append("\n属性パネル例:")
            activeTheme.attributePanels.forEach { panel ->
                append("\n- $panel")
            }
            append("\n\nあなたは${calloutBisaya}と呼ぶ相手に優しく寄り添いながら会話するタリです。")
            append("\n相手の呼び方: ${calloutBisaya} / ${calloutEnglish}。ユーザー性別: ${currentUserGender.name}。")
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

    private fun selectActiveTheme(definition: RoleplayScenarioDefinition) {
        val isScriptedScenario = scriptedRuntime != null
        val level = currentUserLevel
        val selectedFlavor = when {
            isScriptedScenario -> RoleplayThemeFlavor.SCENARIO
            random.nextBoolean() -> RoleplayThemeFlavor.CASUAL
            else -> RoleplayThemeFlavor.SCENARIO
        }
        activeFlavor = selectedFlavor
        isCasualTheme = selectedFlavor == RoleplayThemeFlavor.CASUAL
        endingTriggered = false
        scriptedTurnsRemaining = SCENARIO_MAX_TURN

        activeTheme = if (isScriptedScenario) {
            scriptedTheme(definition)
        } else {
            themeManager.drawTheme(level, selectedFlavor)
        }

        endingTurnTarget = if (isCasualTheme) {
            random.nextInt(CASUAL_MIN_TURN, CASUAL_MAX_TURN + 1)
        } else {
            SCENARIO_MAX_TURN
        }
    }

    private fun scriptedTheme(definition: RoleplayScenarioDefinition): RoleplayThemeDefinition {
        val displayTitle = levelPrefixRegex.replace(definition.title, "").ifBlank { definition.title }
        return RoleplayThemeDefinition(
            id = definition.id,
            title = displayTitle.trim(),
            description = definition.description,
            instruction = "シナリオのゴール(${definition.goal})を12ターン以内に達成するよう導いて。",
            attributePanels = listOf("Deep dive: ${definition.goal}", "Scenery: ${definition.situation}", "Joke: タリの茶目っ気"),
            flavor = RoleplayThemeFlavor.SCENARIO,
            persona = definition.aiRole,
            goalStatement = definition.goal,
            introLine = definition.initialMessage ?: "さぁ、ミッション始めよう",
            closingCue = EndingCue("Balik ta para sa sunod nga mission!", "また次のミッションで会おうね！")
        )
    }

    private fun maybeTriggerEnding(aiMessage: ChatMessage): Boolean {
        if (endingTriggered) return false
        val turns = _uiState.value.completedTurns
        val shouldEnd = when {
            scriptedRuntime != null -> false
            isCasualTheme -> turns >= endingTurnTarget
            else -> turns >= endingTurnTarget
        }

        if (!shouldEnd) return false
        endingTriggered = true
        val cue = activeTheme.closingCue ?: EndingCue("Balik ta sunod, ha?", "また遊ぼうね！")
        val endingMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = cue.bisaya,
            isUser = false,
            translation = cue.translation,
            voiceCue = GeminiVoiceCue.HIGH_PITCH
        )
        history.add(MissionHistoryMessage(cue.bisaya, isUser = false))
        _uiState.update {
            it.copy(
                messages = it.messages + aiMessage + endingMessage,
                isLoading = false,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                turnsTarget = 0
            )
        }
        queueCompletion(calculateScore())
        return true
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

private val scriptedScenarioDefinitions: Map<String, ScriptedScenario> = emptyMap()
