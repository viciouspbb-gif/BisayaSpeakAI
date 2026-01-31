package com.bisayaspeak.ai.ui.roleplay

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.PromptProvider
import com.bisayaspeak.ai.data.repository.RoleplayHistoryRepository
import com.bisayaspeak.ai.data.repository.ScenarioRepository
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.domain.honor.HonorLevelManager
import com.bisayaspeak.ai.ui.roleplay.RoleplayThemeManager
import com.bisayaspeak.ai.util.LocaleUtils
import com.bisayaspeak.ai.utils.MistakeManager
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val isEndingSession: Boolean = false,
    val finalFarewellMessageId: String? = null,
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
    val totalLessonsCompleted: Int = 0,
    val tutorialMessage: String = "",
    val tutorialHint: String = "",
    val translationDirective: String = "",
    val translationLanguage: TranslationLanguage = TranslationLanguage.JAPANESE
)

enum class TranslationLanguage { JAPANESE, ENGLISH }

class RoleplayChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository(promptProvider = PromptProvider(application))
    private val whisperRepository: WhisperRepository = WhisperRepository()
    private val historyRepository: RoleplayHistoryRepository = RoleplayHistoryRepository(application)
    private val voiceRecorder: VoiceInputRecorder = VoiceInputRecorder(application.applicationContext)
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application)
    private val usageRepository: UsageRepository = UsageRepository(application)
    private val scenarioRepository: ScenarioRepository = ScenarioRepository(application)
    private val promptProvider: PromptProvider = PromptProvider(application)
    private val random = Random(System.currentTimeMillis())
    private val themeManager = RoleplayThemeManager(random)

    private companion object {
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val TARI_SCENARIO_ID = "tari_infinite_mode"
        private const val COMPLETION_SCORE = 90
        private const val COMPLETION_THRESHOLD = 80
        private const val LOCKED_OPTION_HOLD_MS = 500L
        private const val POST_CLEAR_SILENCE_MS = 1000L
        private const val CASUAL_MIN_TURN = 9
        private const val CASUAL_MAX_TURN = 11
        private const val SCENARIO_MIN_TURN = 11
        private const val SCENARIO_MAX_TURN = 13
        private val levelPrefixRegex = Regex("^LV\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE)
        private const val OPTION_TUTORIAL_VERSION = 2
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
            "ampingi",
            "bye",
            "bai",
            "babay na",
            "adto sa ko",
            "balik ko unya",
            "ばいばい",
            "バイバイ",
            "またね",
            "じゃあね",
            "もう行くね",
            "またあとで",
            "おやすみ"
        ).map { it.lowercase() }.toSet()
        private val GENERIC_FALLBACK_HINTS = listOf(
            HintPhrase("Oo, maayo kaayo na!", "いいね、やってみよう！"),
            HintPhrase("Pwede nimo ikuwento gamay pa?", "もう少し教えて！"),
            HintPhrase("Salamat kaayo, unsay sunod natong buhaton?", "ありがとう、次はどうする？"),
            HintPhrase("Gusto ko mosuway ana.", "それを試してみたいな"),
            HintPhrase("Pwede ko mangayo og tabang gamay?", "少し助けてもらえる？")
        )
        private val SCENE_LOCATIONS = listOf(
            "オスメニャ・サークル周辺",
            "ITパーク屋台通り",
            "カーボン市場の路地",
            "マクタン島のビーチ",
            "アヤラセンター屋上庭園",
            "サントニーニョ教会付近",
            "山の上の夜景スポット",
            "港の跳ね橋のそば"
        )
        private val SCENE_TIMES = listOf(
            "夜明け前の薄暗い時間",
            "午前9時の爽やかな風が吹く時間",
            "正午の蒸し暑い時間",
            "スコール直後の涼しい夕方",
            "ネオンが点き始める黄昏",
            "真夜中に近い静かな時間"
        )
        private val SCENE_EVENTS = listOf(
            "突然のスコール",
            "タクシー運転手との料金交渉",
            "屋台のくじ引き当選",
            "友人との偶然の再会",
            "忘れ物を取りに戻る騒動",
            "即席ライブ演奏が始まる",
            "停電で街灯が一瞬消える",
            "お祭りの打ち上げ花火"
        )
        private val SCENE_SENSORY_DETAILS = listOf(
            "潮の匂いと冷たい風",
            "カフェから漂う甘い香り",
            "ジプニーのクラクション",
            "遠くで流れるアコースティックギター",
            "焼きバナナの香り",
            "濡れた石畳の反射光"
        )
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
    private var userNickname: String? = null
    private var userCallSign: String = "パートナー（Tari）"
    private var calloutBisaya: String = "Friend"
    private var calloutEnglish: String = "Friend"
    private var pendingAutoExitHistory: List<MissionHistoryMessage>? = null
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
    private var lastSceneSeed: String? = null
    private var optionTutorialVisible: Boolean = true
    private var isJapaneseLocale: Boolean = true

    private val localeState: StateFlow<Locale> = LocaleUtils.localeState
    private val translationLanguageState: StateFlow<TranslationLanguage> = localeState
        .map { locale ->
            if (locale.language.equals("ja", ignoreCase = true)) TranslationLanguage.JAPANESE else TranslationLanguage.ENGLISH
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, if (LocaleUtils.currentLocale().language.equals("ja", true)) TranslationLanguage.JAPANESE else TranslationLanguage.ENGLISH)

    val translationDirective: StateFlow<String> = translationLanguageState
        .map { lang ->
            val directive = if (lang == TranslationLanguage.JAPANESE) {
                "翻訳対象: 日本語"
            } else {
                "Translation Target: English"
            }
            Log.d("RoleplayChatViewModel", "Translation directive updated: ${'$'}directive")
            directive
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "翻訳対象: 日本語")

    init {
        isJapaneseLocale = LocaleUtils.isJapanese(application)
        viewModelScope.launch {
            translationLanguageState.collect { lang ->
                val wasJapanese = isJapaneseLocale
                isJapaneseLocale = lang == TranslationLanguage.JAPANESE
                if (wasJapanese != isJapaneseLocale) {
                    Log.d("RoleplayChatViewModel", "Locale changed -> isJapanese=${'$'}isJapaneseLocale")
                }
                _uiState.update { it.copy(translationLanguage = lang) }
            }
        }
        viewModelScope.launch {
            translationDirective.collect { directive ->
                _uiState.update { it.copy(translationDirective = directive) }
            }
        }
        observeUserProfile()
        observeUserProgress()
        observeOptionTutorialState()
    }

    private fun observeOptionTutorialState() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.roleplayTutorialSeen,
                userPreferencesRepository.roleplayTutorialVersion,
                userPreferencesRepository.roleplayTutorialLocale
            ) { seen, version, storedLocale ->
                Triple(seen, version, storedLocale)
            }.collect { (seen, version, storedLocale) ->
                val currentLocaleTag = currentLocaleTag()
                optionTutorialVisible = when {
                    version < OPTION_TUTORIAL_VERSION -> {
                        userPreferencesRepository.setRoleplayTutorialVersion(OPTION_TUTORIAL_VERSION)
                        userPreferencesRepository.setRoleplayTutorialSeen(false)
                        userPreferencesRepository.setRoleplayTutorialLocale(currentLocaleTag)
                        true
                    }
                    storedLocale != currentLocaleTag -> {
                        userPreferencesRepository.setRoleplayTutorialSeen(false)
                        userPreferencesRepository.setRoleplayTutorialLocale(currentLocaleTag)
                        true
                    }
                    else -> !seen
                }

                _uiState.update {
                    it.copy(showOptionTutorial = optionTutorialVisible)
                }
            }
        }
    }

    private fun selectActiveTheme(scenario: RoleplayScenarioDefinition, forcedTurnLimit: Int? = null) {
        val flavor = if (scenario.level <= 3) {
            RoleplayThemeFlavor.CASUAL
        } else {
            if (random.nextBoolean()) RoleplayThemeFlavor.CASUAL else RoleplayThemeFlavor.SCENARIO
        }
        val theme = themeManager.drawTheme(currentUserLevel, flavor)
        activeTheme = theme
        activeFlavor = flavor
        isCasualTheme = flavor == RoleplayThemeFlavor.CASUAL
        endingTurnTarget = forcedTurnLimit ?: if (isCasualTheme) {
            random.nextInt(CASUAL_MIN_TURN, CASUAL_MAX_TURN + 1)
        } else {
            random.nextInt(SCENARIO_MIN_TURN, SCENARIO_MAX_TURN + 1)
        }
        scriptedTurnsRemaining = forcedTurnLimit ?: endingTurnTarget
        inClosingPhase = false
    }

    private fun observeUserProgress() {
        viewModelScope.launch {
            usageRepository.getTotalLessonsCompleted().collect { lessons ->
                val progress = HonorLevelManager.getProgress(lessons)
                currentUserLevel = progress.level
                _uiState.update {
                    it.copy(
                        userLevel = progress.level,
                        totalLessonsCompleted = lessons
                    )
                }
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
        optionTutorialVisible = false
        _uiState.update { it.copy(showOptionTutorial = false) }
        viewModelScope.launch {
            userPreferencesRepository.setRoleplayTutorialSeen(true)
            userPreferencesRepository.setRoleplayTutorialVersion(OPTION_TUTORIAL_VERSION)
            userPreferencesRepository.setRoleplayTutorialLocale(currentLocaleTag())
        }
    }

    private fun currentLocaleTag(): String = LocaleUtils.resolveAppLocale(getApplication()).toLanguageTag()

    private fun observeUserProfile() {
        viewModelScope.launch {
            userPreferencesRepository.userProfile.collect { profile ->
                currentUserGender = profile.gender
                _uiState.update { it.copy(userGender = profile.gender) }

                val (baseBisaya, baseEnglish, callSignLabel) = when (profile.gender) {
                    UserGender.MALE -> Triple("Guapo", "Guapo", "タリ")
                    UserGender.FEMALE -> Triple("Gwapa", "Gwapa", "タリ")
                    UserGender.OTHER -> Triple("Bestie", "Bestie", "タリ")
                }

                userCallSign = callSignLabel
                calloutBisaya = baseBisaya
                calloutEnglish = baseEnglish
                userNickname = null
            }
        }
    }

    fun loadScenario(scenarioId: String, isProUser: Boolean = isProVersion) {
        val scenario = scenarioRepository.getScenarioById(scenarioId)
        if (scenario == null) {
            Log.e("RoleplayChatViewModel", "Scenario not found: $scenarioId")
            _uiState.update { it.copy(isLoading = false, systemPrompt = "", options = emptyList()) }
            return
        }
        Log.d("RoleplayChatViewModel", "Loading scenario=${scenario.id} title=${scenario.title}")
        
        val definition = convertToRoleplayScenarioDefinition(scenario)
        history.clear()
        branchFacts.clear()
        pendingAutoExitHistory = null
        scriptedRuntime = scriptedScenarioDefinitions[scenarioId]?.let { ScriptedRuntime(it) }
        isProVersion = isProUser
        val forcedTurnLimit = if (scenario.id == TARI_SCENARIO_ID) 12 else null
        selectActiveTheme(definition, forcedTurnLimit)
        scenarioClosingGuidance = definition.closingGuidance
        inClosingPhase = false
        refreshFarewellSignals(definition)

        val cleanThemeTitle = levelPrefixRegex.replace(activeTheme.title, "").trim().ifBlank { activeTheme.title }
        val closingCue = activeTheme.closingCue
        val localizedThemeTitle = if (isJapaneseLocale) cleanThemeTitle else definition.title
        val localizedThemeDescription = if (isJapaneseLocale) activeTheme.description else definition.situation
        val localizedThemePersona = if (isJapaneseLocale) activeTheme.persona else definition.aiRole
        val localizedThemeGoal = if (isJapaneseLocale) activeTheme.goalStatement else definition.goal
        val localizedIntroLine = if (isJapaneseLocale) activeTheme.introLine else ""
        val localizedFarewellTranslation = if (isJapaneseLocale) closingCue.translation else definition.description
        val localizedFarewellExplanation = if (isJapaneseLocale) closingCue.explanation else definition.goal

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = promptProvider.getRoleplaySystemPrompt(scenarioId),
            messages = emptyList(),
            isLoading = scriptedRuntime == null,
            isProUser = isProVersion,
            userGender = currentUserGender,
            activeThemeTitle = localizedThemeTitle,
            activeThemeDescription = localizedThemeDescription,
            activeThemePersona = localizedThemePersona,
            activeThemeGoal = localizedThemeGoal,
            activeThemeFlavor = activeTheme.flavor,
            activeThemeIntroLine = localizedIntroLine,
            activeThemeFarewellBisaya = closingCue.bisaya,
            activeThemeFarewellTranslation = localizedFarewellTranslation,
            activeThemeFarewellExplanation = localizedFarewellExplanation,
            turnsTarget = endingTurnTarget,
            isClosingPhase = inClosingPhase,
            scenarioClosingGuidance = scenarioClosingGuidance,
            showOptionTutorial = optionTutorialVisible,
            translationDirective = translationDirective.value
        )

        if (scriptedRuntime == null) {
            if (isJapaneseLocale) {
                injectThemeIntroLine()
            }
            requestAiTurn(
                scenario = definition,
                userMessage = START_TOKEN
            )
        } else {
            scriptedRuntime?.let {
                deliverScriptedTurn()
            }
        }
    }
    
    private fun convertToRoleplayScenarioDefinition(scenario: MissionScenario): RoleplayScenarioDefinition {
        return RoleplayScenarioDefinition(
            id = scenario.id,
            level = when (scenario.context.level) {
                com.bisayaspeak.ai.data.model.LearningLevel.BEGINNER -> 1
                com.bisayaspeak.ai.data.model.LearningLevel.INTERMEDIATE -> 2
                com.bisayaspeak.ai.data.model.LearningLevel.ADVANCED -> 3
            },
            title = scenario.title,
            description = scenario.subtitle,
            situation = scenario.context.situation,
            aiRole = scenario.context.role,
            goal = scenario.context.goal,
            iconEmoji = "🎭",
            initialMessage = scenario.openingMessage,
            systemPrompt = scenario.systemPrompt,
            hintPhrases = scenario.context.hints.map { HintPhrase(it, it) },
            closingGuidance = null
        )
    }

    private fun injectThemeIntroLine() {
        val introLine = activeTheme.introLine.takeIf { it.isNotBlank() } ?: return
        val introMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = introLine,
            isUser = false,
            translation = null,
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
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
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = scriptedRuntime == null,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                completedTurns = it.completedTurns + 1,
                successfulTurns = it.successfulTurns + 1,
                lockedOption = option,
                isEndingSession = false,
                finalFarewellMessageId = null,
                pendingExitHistory = null
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

        val farewellDetected = containsFarewellCue(trimmed)

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
                lockedOption = null,
                isClosingPhase = it.isClosingPhase || farewellDetected,
                pendingExitHistory = null,
                isEndingSession = false,
                finalFarewellMessageId = null
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

    fun prepareImmediateExit(): List<MissionHistoryMessage> {
        val snapshot = history.toList()
        pendingAutoExitHistory = null
        _uiState.update {
            it.copy(
                pendingExitHistory = snapshot,
                options = emptyList(),
                isLoading = false,
                lockedOption = null,
                showCompletionDialog = false,
                isEndingSession = true,
                finalFarewellMessageId = null
            )
        }
        return snapshot
    }

    fun notifyVoicePlaybackStarted(messageId: String) {
        _speakingMessageId.value = messageId
    }

    fun notifyVoicePlaybackFinished(messageId: String) {
        if (_speakingMessageId.value == messageId) {
            _speakingMessageId.value = null
        }
        val state = _uiState.value
        val autoExitHistory = pendingAutoExitHistory
        if (state.finalFarewellMessageId == messageId && autoExitHistory != null) {
            triggerAutoExit(autoExitHistory)
            pendingAutoExitHistory = null
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
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
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

    private fun triggerAutoExit(historySnapshot: List<MissionHistoryMessage>) {
        _uiState.update {
            it.copy(
                pendingExitHistory = historySnapshot,
                options = emptyList(),
                isLoading = false,
                lockedOption = null,
                finalFarewellMessageId = null
            )
        }
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
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
        )
        val isTariScenario = _uiState.value.currentScenario?.id == TARI_SCENARIO_ID
        val options = payload.options
            .filter { it.text.isNotBlank() }
            .map {
                RoleplayOption(
                    text = it.text,
                    hint = if (isProVersion) it.translation else null,
                    tone = it.tone
                )
            }.filterForAccess()
        val ensuredOptions = if (options.isNotEmpty()) options else buildFallbackOptions()
        val farewellDetected = containsFarewellCue(aiSpeech) || containsFarewellCue(payload.aiTranslation)

        scriptedTurnsRemaining = (scriptedTurnsRemaining - 1).coerceAtLeast(0)
        if (!inClosingPhase && scriptedTurnsRemaining <= 2) {
            inClosingPhase = true
        }

        val sanitizedOptions = when {
            farewellDetected -> emptyList()
            scriptedTurnsRemaining <= 0 -> emptyList()
            inClosingPhase && scriptedTurnsRemaining <= 0 -> emptyList()
            isTariScenario && scriptedTurnsRemaining <= 0 -> emptyList()
            else -> ensuredOptions
        }

        val aiClosedConversation = sanitizedOptions.isEmpty() || farewellDetected

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = sanitizedOptions,
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                turnsTarget = scriptedTurnsRemaining.coerceAtLeast(0),
                isClosingPhase = inClosingPhase,
                isEndingSession = aiClosedConversation && sanitizedOptions.isEmpty(),
                finalFarewellMessageId = if (aiClosedConversation && sanitizedOptions.isEmpty()) aiMsg.id else null,
                pendingExitHistory = if (aiClosedConversation && sanitizedOptions.isEmpty()) it.pendingExitHistory else null
            )
        }

        val shouldComplete = (inClosingPhase && aiClosedConversation) || (scriptedTurnsRemaining <= 0 && aiClosedConversation)
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

    private fun buildFallbackOptions(): List<RoleplayOption> {
        val picks = GENERIC_FALLBACK_HINTS.shuffled(random).take(3)
        return picks.map { phrase ->
            RoleplayOption(
                text = phrase.nativeText,
                hint = if (isProVersion) phrase.translation else null
            )
        }
    }

    private fun drawSceneSeed(): String {
        val candidate = buildString {
            append("場所: ${SCENE_LOCATIONS.random(random)} / ")
            append("時間帯: ${SCENE_TIMES.random(random)} / ")
            append("出来事: ${SCENE_EVENTS.random(random)} / ")
            append("ディテール: ${SCENE_SENSORY_DETAILS.random(random)}")
        }
        if (candidate == lastSceneSeed && SCENE_LOCATIONS.size > 1) {
            return drawSceneSeed()
        }
        lastSceneSeed = candidate
        return candidate
    }

    private fun queueCompletion(score: Int) {
        val farewell = forcedTariFarewell() ?: selectCompletionFarewell()
        inClosingPhase = false
        val farewellMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = farewell.bisaya,
            isUser = false,
            translation = farewell.translation,
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
        )
        history.add(MissionHistoryMessage(farewell.bisaya, isUser = false))
        val historySnapshot = history.toList()
        pendingAutoExitHistory = historySnapshot
        _uiState.update {
            it.copy(
                showCompletionDialog = false,
                completionScore = score.coerceIn(0, 100),
                messages = it.messages + farewellMessage,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                activeThemeFarewellBisaya = farewell.bisaya,
                activeThemeFarewellTranslation = farewell.translation,
                activeThemeFarewellExplanation = farewell.explanation,
                isClosingPhase = false,
                isEndingSession = true,
                finalFarewellMessageId = farewellMessage.id,
                pendingExitHistory = historySnapshot
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
        val translationLanguage = translationLanguageState.value
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
        val nicknameLine = userNickname?.let {
            "- The learner’s preferred nickname is \"$it\". Blend it with the affectionate titles when you call out to them."
        } ?: "- The learner has not set a nickname. Use the affectionate titles as-is."
        val translationInstructionLine = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> "- Provide a separate Japanese translation ONLY inside the dedicated translation field."
            TranslationLanguage.ENGLISH -> "- Provide a separate English translation ONLY inside the dedicated translation field."
        }

        val genderInstruction = """
            SYSTEM DIRECTIVE:
            - You are Tari, a real Cebuana woman living in Cebu.
            - You must always speak Bisaya (Cebuano) in your dialogue. Never mix Japanese or English into your speech field.
            $translationInstructionLine
            - Tagalog / Filipino words (e.g., "masarap", "mahal", "po", "opo") are strictly forbidden. Use pure Cebuano alternatives such as "lami" (おいしい), "mahal kaayo" (高い), "salamat" (ありがとう) with NO "po" suffix.
            - ABSOLUTE RULE: 100% Cebuano vocabulary in every aiSpeech line and suggested option. If the learner uses Tagalog, interpret it but respond with Bisaya expressions only.
            - If you notice a Tagalog syllable sneaking into your output, immediately restate that sentence in Cebuano within the same response and continue in Bisaya only.
            - The learner is $learnerGenderLabel. Address them affectionately as "$calloutBisaya" (Bisaya) or "$calloutEnglish" (English) during greetings/farewells only; avoid repeating names every line.
            $nicknameLine
            - Mention these affectionate titles only during greetings, farewells, when praising the learner, or when double-checking their understanding/feelings. Avoid repeating the name on every line so the flow feels natural.
            - These affectionate callouts are for Tari's speech bubbles only. Never include them inside the suggested learner reply options or translations.
            - Stay affectionate, supportive, and slightly mischievous, but never break character.
            - The learner can submit free-form voice transcriptions or typed input beyond the suggested options. Interpret any Japanese/English phrases they use, convert their intent into Bisaya context, and continue the mission naturally.
            - Never reject or scold the learner for ad-lib input. Absorb it into the story, react with empathy, and keep the pacing lively.
            - If the learner introduces new plot ideas or feelings, weave them into the narrative and adjust your guidance accordingly while staying aligned with the scenario goal.
            - Double-check every word you output to ensure it is Cebuano only. If you accidentally think of a Tagalog term, immediately replace it with its Cebuano equivalent.
            - Before finalizing each JSON payload, run an internal "LANGUAGE CHECK" and confirm there are zero Tagalog terms. If any are detected, regenerate the line entirely in Cebuano.
        """.trimIndent()

        val basePrompt = if (scenario.systemPrompt.isBlank()) {
            """
            You are ${scenario.aiRole}.
            Situation: ${scenario.situation}
            Goal: ${scenario.goal}
            """.trimIndent()
        } else scenario.systemPrompt

        val closingDirective = buildClosingDirective()
        val sceneSeed = drawSceneSeed()

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
            append("\nランダム設定: $sceneSeed")
            append("\n\n毎回シチュエーションが少しずつ変化するように、場所・時間帯・突発的な出来事（天気変化、サプライズ、ちょっとしたトラブル）を即興で1-2個追加。")
            append("\n例: ビーチ沿い、通学路、ナイトマーケット、突然のスコール、タクシーの料金交渉、忘れ物、友人とのバッタリ遭遇など。")
            append("\n同じテーマでもディテールを必ず変え、季節感や混み具合、音・匂いなど感覚情報を織り込みなさい。描写文にもタガログ語を入れないこと。")
            append("\nLANGUAGE FILTER: 設定描写・状況説明・メタ情報も必ずビサヤ語で書き、日本語訳以外にタガログ語を含めないこと。")
            append("\n\nあなたは${calloutBisaya}と呼ぶ相手に優しく寄り添いながら会話するタリです。")
            append("\n相手の呼び方: ${calloutBisaya} / ${calloutEnglish}。ユーザー性別: ${currentUserGender.name}。")
            append("\n\n")
            append(genderInstruction)
            if (closingDirective.isNotBlank()) {
                append("\n\n")
                append(closingDirective)
            }
        }.trim()

        val localizedPrompt = """
            $systemPromptWithGender

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $userMessage

            Respond strictly in JSON with exactly these fields and no extras:
            {
              "aiSpeech": "Assistant reply in Bisaya ONLY. Do not include Japanese, English, or Tagalog.",
              "aiTranslation": "Japanese translation of aiSpeech ONLY.",
              "options": [
                {
                  "text": "Suggested learner reply in Bisaya ONLY (no Tagalog words).",
                  "translation": "Japanese translation of that option ONLY.",
                  "tone": "Short descriptor in Japanese (optional)."
                }
              ]
            }
            Output 2-3 concise options that build upon the learner's most recent intent. Never include markdown or explanations outside JSON.
        """.trimIndent()

        val prompt = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> localizedPrompt
            TranslationLanguage.ENGLISH -> "Translate to English: \n$localizedPrompt"
        }

        Log.d(
            "RoleplayChatViewModel",
            "Prompt generated lang=${'$'}translationLanguage userMsg='${'$'}{userMessage.take(40)}'"
        )
        return prompt
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
        builder.append("- Even while closing, never switch to Tagalog; maintain 100% Cebuano vocabulary.\n")
        scenario?.let {
            builder.append("- Reference the current situation (e.g., ${it.description}) and tie loose ends before ending.\n")
            builder.append("- Make the learner feel the mission \"${it.goal}\" was accomplished.\n")
        }
        builder.append("- Guide the conversation to a natural goodbye within the next ${scriptedTurnsRemaining.coerceAtLeast(1)} AI turns while allowing the learner to reply.\n")
        builder.append("- Mention that Tari enjoyed the interaction and appreciates the learner's effort before the farewell.\n")
        if (scenario?.id == TARI_SCENARIO_ID) {
            builder.append("- Casually mention errands or curfew to hint that the walk is ending soon.\n")
            builder.append("- If this is the final turn, promise clearly that you'll meet here again tomorrow and thank the learner warmly.\n")
        }
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

    private fun forcedTariFarewell(): FarewellLine? {
        if (_uiState.value.currentScenario?.id != TARI_SCENARIO_ID) return null
        val translation = if (translationLanguageState.value == TranslationLanguage.JAPANESE) {
            "今日は本当に楽しかった！ありがとう。また明日ここで会おうね！"
        } else {
            "I had such a wonderful time with you today! Thank you, and let's meet right here again tomorrow!"
        }
        return FarewellLine(
            bisaya = "Salamat kaayo sa imong kuyog karon. Lingaw kaayo ko nimo. Kita ta balik ugma diri ha!",
            translation = translation,
            explanation = "Tari Walk専用のフィナーレ：心からの感謝と『また明日ここで会おう』の約束を伝える。"
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
    val voiceCue: GeminiVoiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE,
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
