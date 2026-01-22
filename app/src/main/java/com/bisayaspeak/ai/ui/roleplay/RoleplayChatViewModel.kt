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

data class FarewellLine(
    val bisaya: String,
    val translation: String,
    val explanation: String
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
    val showOptionTutorial: Boolean = true,
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
    val speakingRequestId: String? = null,
    val userGender: UserGender = UserGender.OTHER,
    val activeThemeTitle: String = "",
    val activeThemeDescription: String = "",
    val activeThemePersona: String = "",
    val activeThemeGoal: String = "",
    val activeThemeFlavor: RoleplayThemeFlavor = RoleplayThemeFlavor.CASUAL,
    val activeThemeIntroLine: String = "",
    val activeThemeFarewellBisaya: String = "",
    val activeThemeFarewellTranslation: String = "",
    val activeThemeFarewellExplanation: String = "",
    val turnsTarget: Int = 0,
    val isClosingPhase: Boolean = false,
    val scenarioClosingGuidance: ScenarioClosingGuidance? = null,
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
        private const val SCENARIO_MIN_TURN = 10
        private const val SCENARIO_MAX_TURN = 12
        private val levelPrefixRegex = Regex("^LV\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE)
        private val DEFAULT_FAREWELL_KEYWORDS = setOf(
            "babay",
            "sige, una",
            "sige una",
            "sige una ko",
            "sige una sa ko",
            "una sa ko",
            "una na ko",
            "kita ta napud",
            "kita ta puhon",
            "ampingi"
        ).map { it.lowercase() }.toSet()
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
    private var scriptedTurnsRemaining: Int = SCENARIO_MAX_TURN
    private var scenarioClosingGuidance: ScenarioClosingGuidance? = null
    private var inClosingPhase: Boolean = false
    private var farewellSignals: Set<String> = DEFAULT_FAREWELL_KEYWORDS

    init {
        observeUserGender()
        observeUserProgress()
        observeOptionTutorialState()
    }

    private fun observeOptionTutorialState() {
        viewModelScope.launch {
            userPreferencesRepository.roleplayTutorialSeen.collect { seen ->
                _uiState.update { it.copy(showOptionTutorial = !seen) }
            }
        }
    }

    private fun selectActiveTheme(scenario: RoleplayScenarioDefinition) {
        val flavor = if (scenario.level <= 3) {
            RoleplayThemeFlavor.CASUAL
        } else {
            if (random.nextBoolean()) RoleplayThemeFlavor.CASUAL else RoleplayThemeFlavor.SCENARIO
        }
        val theme = themeManager.drawTheme(currentUserLevel, flavor)
        activeTheme = theme
        activeFlavor = flavor
        isCasualTheme = flavor == RoleplayThemeFlavor.CASUAL
        endingTurnTarget = if (isCasualTheme) {
            random.nextInt(CASUAL_MIN_TURN, CASUAL_MAX_TURN + 1)
        } else {
            random.nextInt(SCENARIO_MIN_TURN, SCENARIO_MAX_TURN + 1)
        }
        scriptedTurnsRemaining = endingTurnTarget
        inClosingPhase = false
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

    fun dismissOptionTutorial() {
        if (!_uiState.value.showOptionTutorial) return
        _uiState.update { it.copy(showOptionTutorial = false) }
        viewModelScope.launch {
            userPreferencesRepository.setRoleplayTutorialSeen(true)
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
        scenarioClosingGuidance = definition.closingGuidance
        inClosingPhase = false
        refreshFarewellSignals(definition)

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
            activeThemeFarewellExplanation = closingCue.explanation,
            turnsTarget = endingTurnTarget,
            isClosingPhase = inClosingPhase,
            scenarioClosingGuidance = scenarioClosingGuidance
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
            voiceCue = GeminiVoiceCue.TALK_HIGH
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
            val result = whisperRepository.transcribe(
                file,
                WhisperRepository.RecognitionHint.BISAYA_AND_JAPANESE
            )
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
            voiceCue = GeminiVoiceCue.TALK_HIGH
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
        val farewellDetected = containsFarewellCue(aiSpeech) || containsFarewellCue(payload.aiTranslation)
        val sanitizedOptions = if (farewellDetected) emptyList() else options

        scriptedTurnsRemaining = (scriptedTurnsRemaining - 1).coerceAtLeast(0)
        if (!inClosingPhase && scriptedTurnsRemaining <= 2) {
            inClosingPhase = true
        }
        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = sanitizedOptions,
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                turnsTarget = scriptedTurnsRemaining.coerceAtLeast(0),
                isClosingPhase = inClosingPhase
            )
        }

        val aiClosedConversation = sanitizedOptions.isEmpty() || farewellDetected
        val shouldComplete = inClosingPhase && aiClosedConversation
        if (shouldComplete) {
            queueCompletion(calculateScore())
        }
    }

    private fun refreshFarewellSignals(definition: RoleplayScenarioDefinition) {
        val signals = mutableSetOf<String>()
        signals += DEFAULT_FAREWELL_KEYWORDS
        definition.closingGuidance?.farewellExamples?.forEach { example ->
            example.bisaya.takeIf { it.isNotBlank() }?.let { signals += it.lowercase() }
        }
        activeTheme.closingCue.bisaya.takeIf { it.isNotBlank() }?.let { signals += it.lowercase() }
        farewellSignals = signals
    }

    private fun containsFarewellCue(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val normalized = text.lowercase()
        return farewellSignals.any { signal ->
            signal.isNotBlank() && normalized.contains(signal)
        }
    }

    private fun queueCompletion(score: Int) {
        val farewell = selectCompletionFarewell()
        inClosingPhase = false
        _uiState.update {
            it.copy(
                showCompletionDialog = true,
                completionScore = score.coerceIn(0, 100),
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                activeThemeFarewellBisaya = farewell.bisaya,
                activeThemeFarewellTranslation = farewell.translation,
                activeThemeFarewellExplanation = farewell.explanation,
                isClosingPhase = false
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

        val closingDirective = buildClosingDirective()

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
            if (closingDirective.isNotBlank()) {
                append("\n\n")
                append(closingDirective)
            }
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

    private fun buildClosingDirective(): String {
        if (!inClosingPhase) return ""
        val scenario = _uiState.value.currentScenario
        val guidance = scenarioClosingGuidance
        val builder = StringBuilder()
        builder.append("CLOSING PHASE DIRECTIVES:\n")
        builder.append("- ABSOLUTE RULE: Do NOT introduce brand-new questions or tasks.\n")
        builder.append("- 絶対に新しい質問をしないで、これまでの会話をまとめてください。\n")
        builder.append("- Deliver one clear farewell sentence in Bisaya (e.g., \"Sige, una sa ko\", \"Babay\", \"Kita ta napud\") that explicitly ends the chat.\n")
        builder.append("- 別れのビサヤ語フレーズ（Sige, una na ko / Babay / Kita ta napud 等）を必ず述べ、会話の終わりを明確にしてください。\n")
        builder.append("- After the farewell, do not ask or imply any follow-up question. Keep learner options empty or limited to a single acknowledgement if absolutely necessary.\n")
        builder.append("- フェアウェル後は新規の問いかけをせず、必要なら『Salamat kaayo, mag-uban ta napud.』のような余韻のみ提示してください。\n")
        scenario?.let {
            builder.append("- Reference the current situation (e.g., ${it.description}) and tie loose ends before ending.\n")
            builder.append("- Make the learner feel the mission \"${it.goal}\" was accomplished.\n")
        }
        builder.append("- Guide the conversation to a natural goodbye within the next ${scriptedTurnsRemaining.coerceAtLeast(1)} AI turns while allowing the learner to reply.\n")
        builder.append("- Mention that Tari enjoyed the interaction and appreciates the learner's effort before the farewell.\n")
        guidance?.resolutionReminders?.forEach { reminder ->
            builder.append("- Resolve: $reminder\n")
        }
        guidance?.appreciationPhrases?.takeIf { it.isNotEmpty() }?.let {
            builder.append("- Consider lines like ${it.joinToString()} when praising the learner.\n")
        }
        guidance?.followUpSuggestions?.forEach { suggestion ->
            builder.append("- Prompt: $suggestion\n")
        }
        guidance?.farewellExamples?.takeIf { it.isNotEmpty() }?.let {
            builder.append("- Sample farewells: ${it.joinToString { sample -> sample.bisaya }}\n")
        }
        builder.append("- Offer 1-2 gentle follow-up options (e.g., a reflective reply or a final thank-you) rather than new mission tasks.\n")
        builder.append("- Keep tone warm and scenario-appropriate (hospital, hotel, taxi, etc.).\n")
        return builder.toString()
    }

    private fun selectCompletionFarewell(): FarewellLine {
        scenarioClosingGuidance?.farewellExamples?.takeIf { it.isNotEmpty() }?.let { examples ->
            val pick = examples[random.nextInt(examples.size)]
            return FarewellLine(
                bisaya = pick.bisaya,
                translation = pick.translation,
                explanation = pick.explanation
            )
        }
        val cue = activeTheme.closingCue
        return FarewellLine(
            bisaya = cue.bisaya,
            translation = cue.translation,
            explanation = cue.explanation
        )
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
    val voiceCue: GeminiVoiceCue = GeminiVoiceCue.TALK_HIGH,
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
