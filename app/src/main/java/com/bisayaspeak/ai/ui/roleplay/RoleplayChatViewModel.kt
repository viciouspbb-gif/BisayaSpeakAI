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
import kotlin.random.Random
import kotlinx.coroutines.Job
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
    val targetGoal: String = "",
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
    val completionScore: Int = 100,
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
    val goalAchieved: Boolean = false,
    val userLevel: Int = 1,
    val totalLessonsCompleted: Int = 0,
    val tutorialMessage: String = "",
    val tutorialHint: String = "",
    val translationDirective: String = "",
    val translationLanguage: TranslationLanguage = TranslationLanguage.JAPANESE,
    val activeSceneLabel: String = "",
    val activeSceneDescription: String = "",
    val activeSceneIntroLine: String = "",
    val autoExitArmed: Boolean = false,
    val autoExitCountdownMs: Long = 0L,
    val isSessionEnded: Boolean = false,
    val finalMessage: String = "",
    val showFeedbackCard: Boolean = true,
    val roleplayMode: RoleplayMode = RoleplayMode.SANPO
)

enum class TranslationLanguage { JAPANESE, ENGLISH }

private enum class RelationshipMode {
    FORMAL_UNKNOWN,
    INTIMATE_KNOWN
}

class RoleplayChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private companion object {
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val TARI_SCENARIO_ID = "tari_infinite_mode"
        private const val COMPLETION_SCORE = 90
        private const val COMPLETION_THRESHOLD = 80
        private const val LOCKED_OPTION_HOLD_MS = 500L
        private const val POST_CLEAR_SILENCE_MS = 1000L
        private const val SCENARIO_LOG_TAG = "ScenarioGenerator"
        private const val FORCED_EXIT_TURN_THRESHOLD = 6
        private const val FORCED_FAREWELL_OPTION_ID = "forced-farewell-option"
        private val levelPrefixRegex = Regex("^LV\\s*\\d+\\s*: \\s*", RegexOption.IGNORE_CASE)
        private const val OPTION_TUTORIAL_VERSION = 2
        private val SITUATION_TAG_REGEX = Regex("\\[Situation:[^]]*]")
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
            "おやすみ",
            "see you",
            "see ya",
            "see you soon",
            "see you later",
            "goodbye",
            "good bye",
            "gotta go",
            "g2g",
            "time to go",
            "catch you later",
            "talk to you later",
            "また今度",
            "じゃあまた",
            "おつかれ",
            "お疲れ",
            "そろそろ行く",
            "帰るね"
        ).map { it.lowercase() }.toSet()
        private val INTIMATE_KEYWORDS = listOf(
            "tari",
            "walk",
            "friend",
            "friends",
            "buddy",
            "barkada",
            "lover",
            "girlfriend",
            "boyfriend",
            "partner",
            "family",
            "hangout",
            "date",
            "talk",
            "雑談",
            "友達",
            "恋人",
            "親友",
            "散歩",
            "デート",
            "タリ"
        )
        private val GENERIC_FALLBACK_HINTS = listOf(
            HintPhrase("Oo, maayo kaayo na!", "いいね、やってみよう！"),
            HintPhrase("Pwede nimo ikuwento gamay pa?", "もう少し教えて！"),
            HintPhrase("Salamat kaayo, unsay sunod natong buhaton?", "ありがとう、次はどうする？"),
            HintPhrase("Gusto ko mosuway ana.", "それを試してみたいな"),
            HintPhrase("Pwede ko mangayo og tabang gamay?", "少し助けてもらえる？")
        )
        private val NAME_UNLOCK_PATTERNS = listOf(
            Regex("(?i)(?:my name is|i am|i'm|this is|call me)\\s+([A-Za-z][A-Za-z\\-'\\s]{1,40})"),
            Regex("(?i)(?:ako\\s+si|ako\\s+kay|ako\\s+ni|ang pangalan ko kay|ang pangalan nako kay|pangalan ko si|pangalan ko kay|pangalan nako)\\s+([A-Za-z][A-Za-z\\-'\\s]{1,40})"),
            Regex("私の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
            Regex("僕の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
            Regex("俺の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
            Regex("私は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})です")
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
    private val scenarioGenerator = ScenarioGenerator(random)
    private val logicProcessor = RoleplayLogicProcessor()
    private val promptManager = RoleplayPromptManager()
    private var currentMode: RoleplayMode = RoleplayMode.SANPO
    private var turnCount = 0


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
    private var lastSceneSeed: String? = null
    private var optionTutorialVisible: Boolean = true
    private var isJapaneseLocale: Boolean = true
    private var activeDynamicScenario: DynamicScenarioTemplate? = null
    private var autoExitJob: Job? = null
    private var currentScenarioClosingGuidance: ScenarioClosingGuidance? = null
    private var farewellSignals: Set<String> = DEFAULT_FAREWELL_KEYWORDS
    private var currentRelationshipMode: RelationshipMode = RelationshipMode.FORMAL_UNKNOWN
    private var knownLearnerName: String? = null
    private var isLearnerIdentified: Boolean = false

    private fun resolveUserDisplayName(): String {
        return knownLearnerName?.takeIf { it.isNotBlank() }
            ?: userNickname?.takeIf { it.isNotBlank() }
            ?: calloutEnglish.takeIf { it.isNotBlank() }
            ?: "User"
    }

    private fun buildModeAwareSystemPrompt(details: String = ""): String {
        val userNameForPrompt = if (currentMode == RoleplayMode.DOJO) {
            "stranger"
        } else {
            resolveUserDisplayName()
        }
        val scenarioDetails = when {
            currentMode == RoleplayMode.DOJO -> {
                val base = details.takeIf { it.isNotBlank() } ?: "指定シチュエーション"
                "$base / 相手を個人として認識せず、与えられた役に徹してミッション処理だけを行うこと"
            }
            details.isNotBlank() -> details
            else -> "Cebu daily life"
        }
        return promptManager.getSystemPrompt(
            mode = currentMode,
            userName = userNameForPrompt,
            details = scenarioDetails
        )
    }

    private fun switchMode(newMode: RoleplayMode) {
        currentMode = newMode
        turnCount = 0
        history.clear()
        branchFacts.clear()
        _uiState.update { it.copy(messages = emptyList()) }
    }

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

    fun handleSessionEnd(isMissionComplete: Boolean = false) {
        val finalLine = if (currentMode == RoleplayMode.DOJO) {
            if (isMissionComplete) {
                "ごゆっくりお過ごしください（ミッション達成）"
            } else {
                "また練習しよう！"
            }
        } else {
            "今日はよくしゃべったね、また明日！"
        }
        _uiState.update {
            it.copy(
                isSessionEnded = true,
                finalMessage = finalLine,
                showFeedbackCard = false,
                isEndingSession = true,
                options = emptyList(),
                isLoading = false
            )
        }
    }

    private fun loadInfiniteTariMode(isProUser: Boolean) {
        val fallbackScenario = scenarioRepository.getScenarioById(TARI_SCENARIO_ID)
        if (fallbackScenario == null) {
            Log.e("RoleplayChatViewModel", "Infinite mode base scenario missing")
            _uiState.update { it.copy(isLoading = false, systemPrompt = "", options = emptyList()) }
            return
        }

        resetAutoExitState()
        switchMode(RoleplayMode.SANPO)
        val definition = convertToRoleplayScenarioDefinition(fallbackScenario)
        pendingAutoExitHistory = null
        scriptedRuntime = null
        activeDynamicScenario = null
        isProVersion = isProUser
        currentScenarioClosingGuidance = definition.closingGuidance
        refreshFarewellSignals(definition)
        selectActiveTheme(definition)
        currentRelationshipMode = RelationshipMode.INTIMATE_KNOWN
        knownLearnerName = userNickname?.takeIf { it.isNotBlank() }
        isLearnerIdentified = knownLearnerName != null
        val scenarioDetails = definition.description.ifBlank { definition.situation }
        val systemPrompt = buildModeAwareSystemPrompt(scenarioDetails)

        _uiState.value = RoleplayUiState(
            currentScenario = definition.copy(id = TARI_SCENARIO_ID),
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = systemPrompt,
            messages = emptyList(),
            isLoading = true,
            isProUser = isProVersion,
            userGender = currentUserGender,
            activeThemeTitle = definition.title,
            activeThemeDescription = definition.description,
            activeThemePersona = definition.aiRole,
            activeThemeGoal = definition.goal,
            activeThemeFlavor = activeTheme.flavor,
            activeThemeIntroLine = "",
            activeThemeFarewellBisaya = activeTheme.closingCue.bisaya,
            activeThemeFarewellTranslation = activeTheme.closingCue.translation,
            activeThemeFarewellExplanation = activeTheme.closingCue.explanation,
            activeSceneLabel = "",
            activeSceneDescription = "",
            activeSceneIntroLine = "",
            showOptionTutorial = optionTutorialVisible,
            translationDirective = translationDirective.value,
            goalAchieved = false,
            isEndingSession = false,
            roleplayMode = RoleplayMode.SANPO
        )

        if (isJapaneseLocale) {
            injectThemeIntroLine()
        }
        requestAiTurn(
            scenario = definition.copy(id = TARI_SCENARIO_ID),
            userMessage = START_TOKEN
        )
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
                userNickname = profile.nickname.takeIf { it.isNotBlank() }
            }
        }
    }

    fun loadScenario(scenarioId: String, isProUser: Boolean = isProVersion) {
        if (scenarioId == TARI_SCENARIO_ID) {
            loadInfiniteTariMode(isProUser)
            return
        }

        val scenario = scenarioRepository.getScenarioById(scenarioId)
        if (scenario == null) {
            Log.e("RoleplayChatViewModel", "Scenario not found: $scenarioId")
            _uiState.update { it.copy(isLoading = false, systemPrompt = "", options = emptyList()) }
            return
        }
        Log.d("RoleplayChatViewModel", "Loading scenario=${scenario.id} title=${scenario.title}")
        
        switchMode(RoleplayMode.DOJO)
        val definition = convertToRoleplayScenarioDefinition(scenario)
        pendingAutoExitHistory = null
        scriptedRuntime = scriptedScenarioDefinitions[scenarioId]?.let { ScriptedRuntime(it) }
        activeDynamicScenario = null
        isProVersion = isProUser
        currentScenarioClosingGuidance = definition.closingGuidance
        refreshFarewellSignals(definition)
        selectActiveTheme(definition)
        val relationshipMode = determineRelationshipMode(definition, null)
        currentRelationshipMode = relationshipMode
        if (relationshipMode == RelationshipMode.INTIMATE_KNOWN) {
            knownLearnerName = userNickname?.takeIf { it.isNotBlank() }
            isLearnerIdentified = knownLearnerName != null
        } else {
            knownLearnerName = null
            isLearnerIdentified = false
        }

        val cleanThemeTitle = levelPrefixRegex.replace(activeTheme.title, "").trim().ifBlank { activeTheme.title }
        val closingCue = activeTheme.closingCue
        val localizedThemeTitle = if (isJapaneseLocale) cleanThemeTitle else definition.title
        val localizedThemeDescription = if (isJapaneseLocale) activeTheme.description else definition.situation
        val localizedThemePersona = if (isJapaneseLocale) activeTheme.persona else definition.aiRole
        val localizedThemeGoal = if (isJapaneseLocale) activeTheme.goalStatement else definition.goal
        val localizedIntroLine = if (isJapaneseLocale) activeTheme.introLine else ""
        val localizedFarewellTranslation = if (isJapaneseLocale) closingCue.translation else definition.description
        val localizedFarewellExplanation = if (isJapaneseLocale) closingCue.explanation else definition.goal
        val scenarioDetails = definition.situation.ifBlank { definition.description }
        val systemPrompt = buildModeAwareSystemPrompt(scenarioDetails)

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = systemPrompt,
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
            activeSceneLabel = localizedThemeTitle,
            activeSceneDescription = localizedThemeDescription,
            activeSceneIntroLine = localizedIntroLine.ifBlank { definition.initialMessage },
            showOptionTutorial = optionTutorialVisible,
            translationDirective = translationDirective.value,
            goalAchieved = false,
            isEndingSession = false,
            roleplayMode = RoleplayMode.DOJO
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
        if (_uiState.value.currentScenario?.id == TARI_SCENARIO_ID) return
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
        if (_uiState.value.isEndingSession) return
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
        processUserUtteranceForNameUnlock(option.text)
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
        if (_uiState.value.isEndingSession) return
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isLoading) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )
        history.add(MissionHistoryMessage(trimmed, isUser = true))
        processUserUtteranceForNameUnlock(trimmed)
        scriptedRuntime = null

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                pendingExitHistory = null,
                isEndingSession = false,
                finalFarewellMessageId = null,
                completedTurns = it.completedTurns + 1,
                successfulTurns = it.successfulTurns + 1
            )
        }

        viewModelScope.launch {
            requestAiTurn(scenario, trimmed)
        }
    }

    fun startVoiceRecording() {
        if (_uiState.value.isEndingSession) return
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
        if (_uiState.value.isEndingSession) return
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
        _uiState.update { it.copy(pendingExitHistory = null, autoExitArmed = false) }
    }

    fun prepareImmediateExit(autoTriggered: Boolean = false): List<MissionHistoryMessage> {
        if (!autoTriggered) {
            cancelAutoExitCountdown()
        }
        val snapshot = history.toList()
        pendingAutoExitHistory = null
        _uiState.update {
            val keepAutoExit = autoTriggered && it.autoExitArmed
            it.copy(
                pendingExitHistory = snapshot,
                options = emptyList(),
                isLoading = false,
                lockedOption = null,
                showCompletionDialog = true,
                isEndingSession = true,
                finalFarewellMessageId = null,
                autoExitCountdownMs = 0L,
                autoExitArmed = keepAutoExit
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
        queueCompletion()
    }

    private fun finalizeScriptedScenario() {
        scriptedRuntime = null
        queueCompletion()
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

    private fun scheduleAutoExit() {
        autoExitJob?.cancel()
        autoExitJob = viewModelScope.launch {
            val delayMs = 3000L
            _uiState.update { it.copy(autoExitCountdownMs = delayMs, autoExitArmed = true) }
            var remaining = delayMs
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1000L
                val snapshot = remaining.coerceAtLeast(0L)
                _uiState.update { it.copy(autoExitCountdownMs = snapshot) }
            }
            prepareImmediateExit(autoTriggered = true)
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
        val rawAiSpeech = payload.aiSpeech.ifBlank { "..." }
        val completionTagDetected = rawAiSpeech.contains("[COMPLETE]")
        val sanitizedAiSpeech = sanitizeSystemTags(
            rawAiSpeech
                .replace("[FINISH]", "")
                .replace("[COMPLETE]", "")
                .trim()
        ).ifBlank { "..." }
        val aiTranslation = sanitizeSystemTags(
            payload.aiTranslation
                .replace("[FINISH]", "")
                .replace("[COMPLETE]", "")
                .trim()
        )
        turnCount++
        val shouldForceEndByTurnLimit = currentMode == RoleplayMode.SANPO && turnCount >= 12
        val finishDetected = rawAiSpeech.contains("[FINISH]") || completionTagDetected || shouldForceEndByTurnLimit
        val hasCompleteTag = completionTagDetected || shouldForceEndByTurnLimit
        val finalAiSpeech = if (shouldForceEndByTurnLimit) {
            "今日はたくさん話したね、また明日！"
        } else {
            sanitizedAiSpeech
        }
        val finalAiTranslation = if (shouldForceEndByTurnLimit) {
            "今日はたくさん話したね、また明日！"
        } else {
            aiTranslation
        }

        history.add(MissionHistoryMessage(finalAiSpeech, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = finalAiSpeech,
            isUser = false,
            translation = finalAiTranslation.takeIf { it.isNotBlank() },
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
        )
        val isTariScenario = _uiState.value.currentScenario?.id == TARI_SCENARIO_ID
        val options = payload.options
            .filter { it.text.isNotBlank() }
            .map {
                val optionText = maybeStripLearnerNames(sanitizeSystemTags(it.text))
                val optionHint = if (isProVersion) maybeStripLearnerNames(sanitizeSystemTags(it.translation)) else null
                RoleplayOption(
                    text = optionText,
                    hint = optionHint,
                    tone = it.tone
                )
            }.filterForAccess()
        val ensuredOptions = if (options.isNotEmpty()) options else buildFallbackOptions()
        val optionsWithForcedFarewell = maybeInjectFarewellOption(ensuredOptions, _uiState.value.completedTurns)
        val farewellDetected = containsFarewellCue(finalAiSpeech) || containsFarewellCue(finalAiTranslation)
        val shouldForceEnd = finishDetected || farewellDetected

        val sanitizedOptions = if (shouldForceEnd) emptyList() else optionsWithForcedFarewell

        val aiClosedConversation = shouldForceEnd || sanitizedOptions.isEmpty()

        if (finishDetected) {
            cancelVoiceRecording()
        }

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = sanitizedOptions,
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                isEndingSession = shouldForceEnd || (aiClosedConversation && sanitizedOptions.isEmpty()),
                finalFarewellMessageId = if (aiClosedConversation) aiMsg.id else null,
                pendingExitHistory = if (aiClosedConversation) it.pendingExitHistory else null
            )
        }

        if (aiClosedConversation) {
            val emitFarewellMessage = !(finishDetected || farewellDetected)
            val shouldComplete = finishDetected || aiClosedConversation
            if (shouldComplete) {
                queueCompletion(
                    emitFarewellMessage = !finishDetected,
                    closingReference = if (finishDetected) aiMsg else null
                )
            }
        }

        if (hasCompleteTag) {
            handleSessionEnd(isMissionComplete = currentMode == RoleplayMode.DOJO)
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

    private fun maybeInjectFarewellOption(
        options: List<RoleplayOption>,
        completedTurns: Int
    ): List<RoleplayOption> {
        if (completedTurns < FORCED_EXIT_TURN_THRESHOLD) return options
        val alreadyHasFarewell = options.any { option ->
            option.id == FORCED_FAREWELL_OPTION_ID || containsFarewellCue(option.text)
        }
        if (alreadyHasFarewell) return options

        val translation = when (translationLanguageState.value) {
            TranslationLanguage.JAPANESE -> "じゃあ、もう行くね！また話そうね。"
            TranslationLanguage.ENGLISH -> "Alright, I'll head out now—talk soon!"
        }
        val farewellOption = RoleplayOption(
            id = FORCED_FAREWELL_OPTION_ID,
            text = "Sige sa ko ha, mularga na ko. Kita ta napud!",
            hint = if (isProVersion) translation else null,
            tone = "farewell"
        )
        return options + farewellOption
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

    private fun ensureTariScenarioLocked(userMessage: String?): DynamicScenarioTemplate? {
        if (activeDynamicScenario != null) {
            return activeDynamicScenario
        }
        val selected = scenarioGenerator.selectScenario(userMessage, null)
        activeDynamicScenario = selected
        selected?.let { template ->
            val label = template.label.resolve(isJapaneseLocale)
            Log.d(SCENARIO_LOG_TAG, "Chosen Scenario: $label (id=${template.id})")
            _uiState.update { state ->
                state.copy(
                    activeSceneLabel = label,
                    activeSceneDescription = template.description.resolve(isJapaneseLocale),
                    activeSceneIntroLine = buildDynamicSceneIntroLine(template)
                )
            }
        }
        return selected
    }

    private fun sanitizeSystemTags(text: String?): String {
        if (text.isNullOrBlank()) return text.orEmpty()
        return SITUATION_TAG_REGEX.replace(text, "").replace("  ", " ").trim()
    }

    private fun stripLearnerNames(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val names = buildList {
            knownLearnerName?.takeIf { it.isNotBlank() }?.let { add(it) }
            userNickname?.takeIf { it.isNotBlank() }?.let { add(it) }
            calloutBisaya.takeIf { it.isNotBlank() }?.let { add(it) }
            calloutEnglish.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        if (names.isEmpty()) return text
        var result: String = text
        names.forEach { name ->
            val pattern = Regex("(?i)\\b${Regex.escape(name.trim())}\\b[,:\\-\\s]*")
            result = result.replace(pattern, "")
        }
        return result.replace("  ", " ").trim()
    }

    private fun maybeStripLearnerNames(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return if (currentMode == RoleplayMode.SANPO) {
            stripLearnerNames(text)
        } else {
            text.trim()
        }
    }

    private fun processUserUtteranceForNameUnlock(message: String) {
        if (currentRelationshipMode == RelationshipMode.INTIMATE_KNOWN || isLearnerIdentified) return
        val extracted = extractLearnerName(message)
        if (!extracted.isNullOrBlank()) {
            knownLearnerName = extracted
            isLearnerIdentified = true
            Log.d("RoleplayChatViewModel", "Learner name unlocked: $extracted")
        }
    }

    private fun extractLearnerName(message: String): String? {
        if (message.isBlank()) return null
        NAME_UNLOCK_PATTERNS.forEach { pattern ->
            val match = pattern.find(message)
            if (match != null && match.groupValues.size > 1) {
                sanitizeLearnerName(match.groupValues[1])?.let { return it }
            }
        }
        val profileName = userNickname
        if (!profileName.isNullOrBlank() && message.contains(profileName, ignoreCase = true)) {
            return profileName
        }
        return null
    }

    private fun sanitizeLearnerName(raw: String?): String? {
        val trimmed = raw?.trim()?.trimStart(',', ':', '、')?.trimEnd('.', ',', '!', '?', '。', '！', '？')
        val primary = trimmed?.split(Regex("[、,。！!？?]")).orEmpty().firstOrNull()?.trim()
        return primary?.takeIf { it.length in 2..32 }
    }

    private fun buildDynamicSceneIntroLine(template: DynamicScenarioTemplate): String {
        val label = template.label.resolve(isJapaneseLocale)
        val description = template.description.resolve(isJapaneseLocale)
        return if (isJapaneseLocale) {
            "今日は『$label』のシチュエーションで練習しよう。$description"
        } else {
            "Let's practice the '$label' scenario today. $description"
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

    private fun determineRelationshipMode(
        scenario: RoleplayScenarioDefinition,
        dynamicScenario: DynamicScenarioTemplate?
    ): RelationshipMode {
        if (scenario.id == TARI_SCENARIO_ID) {
            return RelationshipMode.INTIMATE_KNOWN
        }
        val locale = LocaleUtils.resolveAppLocale(getApplication())
        val aggregateText = listOfNotNull(
            scenario.situation,
            scenario.description,
            scenario.aiRole,
            dynamicScenario?.label?.resolve(isJapaneseLocale),
            dynamicScenario?.description?.resolve(isJapaneseLocale),
            dynamicScenario?.tariRole?.resolve(isJapaneseLocale)
        ).joinToString(" ").lowercase(locale)
        return if (INTIMATE_KEYWORDS.any { aggregateText.contains(it) }) {
            RelationshipMode.INTIMATE_KNOWN
        } else {
            RelationshipMode.FORMAL_UNKNOWN
        }
    }

    private fun queueCompletion(
        emitFarewellMessage: Boolean = true,
        closingReference: ChatMessage? = null
    ) {
        val farewellMessage = if (emitFarewellMessage) {
            val farewell = forcedTariFarewell() ?: selectCompletionFarewell()
            history.add(MissionHistoryMessage(farewell.bisaya, isUser = false))
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = farewell.bisaya,
                isUser = false,
                translation = farewell.translation,
                voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
            ) to farewell
        } else null

        val historySnapshot = history.toList()
        pendingAutoExitHistory = historySnapshot
        val (messageToAppend, farewellLine) = farewellMessage ?: (null to null)
        val referenceMessage = closingReference ?: messageToAppend

        _uiState.update {
            it.copy(
                showCompletionDialog = true,
                completionScore = 100,
                messages = messageToAppend?.let { msg -> it.messages + msg } ?: it.messages,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                activeThemeFarewellBisaya = referenceMessage?.text ?: farewellLine?.bisaya ?: "",
                activeThemeFarewellTranslation = referenceMessage?.translation
                    ?: farewellLine?.translation ?: "",
                activeThemeFarewellExplanation = farewellLine?.explanation ?: it.activeThemeFarewellExplanation,
                isEndingSession = true,
                finalFarewellMessageId = referenceMessage?.id ?: it.finalFarewellMessageId,
                pendingExitHistory = historySnapshot
            )
        }
        scheduleAutoExit()
    }

    private fun buildRoleplayPrompt(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ): String {
        val isTariScenario = scenario.id == TARI_SCENARIO_ID
        val sanitizedUserMessage = userMessage.takeUnless { it == START_TOKEN }
        val dynamicScenario = if (isTariScenario) {
            ensureTariScenarioLocked(sanitizedUserMessage)
        } else null
        if (!isTariScenario) {
            activeDynamicScenario = null
        }
        val relationshipMode = determineRelationshipMode(scenario, dynamicScenario)
        currentRelationshipMode = relationshipMode

        val sceneLabelForPrompt = (dynamicScenario?.label?.resolve(isJapaneseLocale)
            ?: scenario.title).ifBlank { activeTheme.title }.ifBlank { "Cebu Daily Life" }
        val sceneDescriptionForPrompt = (dynamicScenario?.description?.resolve(isJapaneseLocale)
            ?: scenario.description).ifBlank { activeTheme.description }
        val tariRoleForPrompt = (dynamicScenario?.tariRole?.resolve(isJapaneseLocale)
            ?: scenario.aiRole).ifBlank { activeTheme.persona }
        val learnerRoleForPrompt = (dynamicScenario?.userRole?.resolve(isJapaneseLocale)
            ?: "Learner" )

        val translationLanguage = translationLanguageState.value
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }

        val latestLearnerMessage = if (userMessage == START_TOKEN) {
            "SYSTEM: Conversation is starting. Open with a warm Cebuano greeting that anchors the scene."
        } else userMessage

        val hints = scenario.hintPhrases.joinToString(separator = "\n") {
            "- ${it.nativeText} (${it.translation})"
        }.ifBlank { "- (none)" }

        val missionGoal = scenario.goal.ifBlank { "Guide the learner through real Cebu life." }
        val targetGoal = scenario.targetGoal.ifBlank { missionGoal }
        val learnerGenderLabel = when (currentUserGender) {
            UserGender.MALE -> "male (男性)"
            UserGender.FEMALE -> "female (女性)"
            UserGender.OTHER -> "non-binary / undisclosed"
        }
        val honorificFallback = when (currentUserGender) {
            UserGender.MALE -> "Sir for formal tone, Kuya for warm-yet-polite tone"
            UserGender.FEMALE -> "Ma'am for formal tone, Ate for warm-yet-polite tone"
            UserGender.OTHER -> "Friend / Amigo / Amiga"
        }
        val unlockedLearnerName = when {
            relationshipMode == RelationshipMode.INTIMATE_KNOWN -> {
                if (!isLearnerIdentified) {
                    knownLearnerName = knownLearnerName ?: userNickname?.takeIf { it.isNotBlank() }
                    isLearnerIdentified = knownLearnerName != null
                }
                knownLearnerName ?: userNickname
            }
            isLearnerIdentified -> knownLearnerName
            else -> null
        }?.takeIf { it.isNotBlank() }

        val nicknameLine = when (relationshipMode) {
            RelationshipMode.INTIMATE_KNOWN -> {
                unlockedLearnerName?.let {
                    "- Known nickname: \"$it\". It is already unlocked for this intimate scenario—use it warmly from the very first aiSpeech line."
                } ?: "- No saved nickname, so treat '$calloutBisaya'/'$calloutEnglish' as the affectionate default and use it from line one because this is a close relationship."
            }
            RelationshipMode.FORMAL_UNKNOWN -> "- Nickname data: even if the system stores a nickname, it is LOCKED until the learner self-introduces. Do not reveal or guess any name until they say it aloud."
        }
        val directNameInstruction = when (relationshipMode) {
            RelationshipMode.FORMAL_UNKNOWN -> "- Recognition event: the instant the learner states their name (or booking reference), acknowledge it and combine the provided name with Sir/Ma'am/Kuya/Ate for the rest of the scene."
            RelationshipMode.INTIMATE_KNOWN -> "- If the learner offers a new nickname or corrects how they want to be called, adopt it immediately and keep using it affectionately—never revert to Sir/Ma'am."
        }
        val honorificGuidanceLine = when (relationshipMode) {
            RelationshipMode.FORMAL_UNKNOWN -> "- Name etiquette: until the learner explicitly says their name in this scene, assume you don't know it and address them using $honorificFallback based on how formal the situation feels."
            RelationshipMode.INTIMATE_KNOWN -> "- Name etiquette: this is a personal scenario—skip Sir/Ma'am entirely and rely on the unlocked nickname or affectionate callouts even when giving instructions."
        }
        val nameRecognitionProfile = when (relationshipMode) {
            RelationshipMode.FORMAL_UNKNOWN -> "- Relationship profile: Public/first-time. Start with nameKnown=FALSE and call the learner only Sir/Ma'am/Kuya/Ate. Flip nameKnown=TRUE only after they state their name, then use 'Sir [Name]' or 'Ma'am [Name]' in Tari's dialogue."
            RelationshipMode.INTIMATE_KNOWN -> "- Relationship profile: Close friend/lover/family. Start with nameKnown=TRUE and speak to them by nickname right away—no honorific buffer needed."
        }
        val nameAmnesiaLine = when (relationshipMode) {
            RelationshipMode.FORMAL_UNKNOWN -> "- Memory wall: You do NOT know the learner's name yet. Treat them like a stranger until they personally reveal it."
            RelationshipMode.INTIMATE_KNOWN -> "- Memory wall: This scenario is intimate—the nickname is already known and should stay active unless the learner corrects it."
        }
        val learnerNameUsageLine = unlockedLearnerName?.let {
            if (relationshipMode == RelationshipMode.FORMAL_UNKNOWN) {
                "- Learner provided name: \"$it\". Pair it with Sir/Ma'am/Kuya/Ate every time (e.g., 'Sir $it, complete ni nato ha?')."
            } else {
                "- Learner nickname active: \"$it\". Use it lovingly in Tari's own dialogue but never inside learner option text."
            }
        } ?: "- Learner name remains UNKNOWN. Never invent or reuse profile data until the learner explicitly introduces themselves in-scene."
        val translationInstructionLine = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> "- Provide a separate Japanese translation ONLY inside the dedicated translation field."
            TranslationLanguage.ENGLISH -> "- Provide a separate English translation ONLY inside the dedicated translation field."
        }
        val particleGuidanceLine = "- Express politeness using Bisaya particles like 'man', 'na', 'ba', 'lagi', not Tagalog 'po/opo'."

        val genderToneInstruction = when (currentUserGender) {
            UserGender.MALE -> "- Lean into confident, slightly adventurous banter (sports, business wins, protective gestures) and praise him with Cebuano compliments such as \"gwapo kaayo ka karon\" when it feels natural."
            UserGender.FEMALE -> "- Highlight warm, caring, or stylish angles (cafés, self-care, celebrations) and praise her with Cebuano compliments such as \"gwapa kaayo ka\"."
            UserGender.OTHER -> "- Keep the tone inclusive and gender-neutral. Use friendly Cebuano like \"bestie\", \"amiga\", or \"partner\" instead of gendered compliments."
        }

        val sceneSeed = drawSceneSeed()
        val dynamicScenarioGuidance = dynamicScenario?.let {
            "- Dynamic guidance today: ${it.guidance.resolve(isJapaneseLocale)}"
        }

        val systemPromptWithIdentity = buildString {
            appendLine("[ROLELOCK]")
            appendLine("- You are Tari, a real Cebuana woman living in Cebu and the learner's closest Filipino friend/partner.")
            appendLine("- Speak 100% Cebuano in every aiSpeech line. No Tagalog words, suffixes, or mixed languages.")
            appendLine(particleGuidanceLine)
            appendLine(translationInstructionLine)
            appendLine("- If the learner uses Tagalog/Japanese/English, interpret it and respond in Cebuano while keeping the story moving.")
            appendLine("- Lead every exchange proactively—never toss decisions back with 'What do you want to do?'")
            appendLine("- The learner is $learnerGenderLabel. Address them affectionately as '$calloutBisaya' (Bisaya) or '$calloutEnglish' (English) only inside Tari's dialogue when greeting, praising, or saying goodbye.")
            appendLine(nameAmnesiaLine)
            appendLine(honorificGuidanceLine)
            appendLine("- Whatever the learner says, NEVER drop the assigned in-world role (clerk, driver, receptionist, barista, etc.). React as that person every second.")
            appendLine(nicknameLine)
            appendLine(directNameInstruction)
            appendLine(nameRecognitionProfile)
            appendLine(learnerNameUsageLine)
            appendLine(genderToneInstruction)
            appendLine("- These affectionate callouts never appear inside the suggested learner reply options or translations.")
            appendLine("- Stay loving, supportive, a little mischievous, yet anchored in reality—and keep every exchange tight, high-energy, and purposeful.")
            appendLine()

            appendLine("[SCENARIO SNAPSHOT]")
            appendLine("- Location label: $sceneLabelForPrompt")
            appendLine("- Situation detail: $sceneDescriptionForPrompt")
            appendLine("- Tari role: $tariRoleForPrompt")
            appendLine("- Learner POV: $learnerRoleForPrompt")
            appendLine("- Mission statement: $missionGoal")
            appendLine("- Target goal (must be physically achieved before ending): $targetGoal")
            dynamicScenarioGuidance?.let { appendLine(it) }
            appendLine("- Theme persona flavor: ${activeTheme.persona}")
            appendLine("- Theme goal cue: ${activeTheme.goalStatement}")
            appendLine("- Filipino sensory cue today: $sceneSeed")
            appendLine()

            appendLine("[GOAL PLAYBOOK]")
            appendLine("- Warm up with at most 2 short Cebuano lines of small talk tied to Filipino life, then immediately guide the learner back toward the mission.")
            appendLine("- Every turn must push the learner toward '$targetGoal' with concrete, real-world actions (approach the guard, give the booking name, pay the jeepney fare, etc.).")
            appendLine("- Apply the 'one step forward' rule: each response must introduce a NEW forward action or detail. Repeating the same explanation counts as failure.")
            appendLine("- Phase irreversibility: once a detail is confirmed (name stated, symptoms given, payment done), never circle back to that topic unless the learner explicitly undoes it.")
            appendLine("- Respect the 3–5 exchange contract: wrap the mission in 3 to 5 total back-and-forths (AI+Learner pairs). If you reach the third exchange with momentum, finish immediately on the next decisive action.")
            appendLine("- Termination mandate: the moment the learner's goal is satisfied or they hint that they must leave, stop offering new tasks, deliver one warm goodbye, and end the scene.")
            appendLine("- Explicitly reference progress (e.g., 'naa na ta sa counter', 'hapit na nato makompleto ang papeles').")
            appendLine("- If the learner hesitates or derails, gently redirect them with a precise next step and cultural context (traffic, barangay rules, Filipino etiquette) without adding extra small talk.")
            appendLine("- Never introduce brand-new side quests or random topics. Everything must reinforce the mission.")
            appendLine("- Use randomness only as a spice—tiny sensory observations or playful lines that never slow the mission clock.")
            appendLine()

            appendLine("[FINISH PROTOCOL]")
            appendLine("- Output '[FINISH]' ONLY after '$targetGoal' is undeniably accomplished in-world (documents handed over, destination reached, promise fulfilled).")
            appendLine("- On that final turn describe the physical resolution, celebrate warmly, append ' [FINISH]' to aiSpeech, and return an empty options array.")
            appendLine("- If the learner has already completed the required actions yet the scene risks stalling, Tari must decisively wrap up: confirm completion ('ok human na ang admission, lingkod sa waiting area ha?') and immediately emit '[FINISH]'.")
            appendLine("- Never let a promised action linger for more than one additional turn. If you say you'll hand over a ticket, do it in the very next message and then close.")
            appendLine("- Before completion, do NOT hint at ending, do NOT say goodbye, and keep options actionable—but once the mission locks or the learner says goodbye, end briskly so the learner feels satisfied yet eager for another run.")
            appendLine("- Farewell discipline: if the learner initiates a goodbye or shows farewell cues, reciprocate once, append '[FINISH]', and do not generate any further options.")
            val closingHookInstruction = when (relationshipMode) {
                RelationshipMode.FORMAL_UNKNOWN -> "- Closure hook: after confirming success, thank them in a courteous service tone and invite them back soon (e.g., 'Daghang salamat, balik lang anytime ha', '本日はありがとうございました。また何かあればいつでもどうぞ。')."
                RelationshipMode.INTIMATE_KNOWN -> "- Closure hook: after celebrating success, drop a playful future promise (e.g., 'Kita-kits ta napud', 'Sige, puhon! Sunod napud ha?') so they crave the next hangout."
            }
            appendLine(closingHookInstruction)
            appendLine("- The moment you deliver that hook line, append '[FINISH]' immediately and end the turn—never wait for another learner reply.")
            appendLine()

            appendLine("[OPTION RULES]")
            appendLine("- Provide 2-3 Bisaya-only options that keep momentum (confirming next step, reacting with confidence, asking for clarification about the current action).")
            appendLine("- Each option must be an imperative or decisive statement—not vague questions like 'unsa man sunod?'.")
            appendLine("- Options must be anchored to the immediate physical situation (serve the coffee, pay the fare, sign the clipboard) and never fall back to generic chit-chat.")
            appendLine("- Never include the learner's nickname/call sign inside options. Use the translation field (${translationLanguage.name}) separately.")
            appendLine()

            appendLine("[REALISM & LOOP GUARD]")
            appendLine("- Pepper sensory details of Cebu life: humidity, jeepney bells, sari-sari smells, security guards, traffic, fiesta music, etc.")
            appendLine("- Absolutely forbid filler like '他に聞きたいことは？' , 'Anything else?', or 'What do you want to do next?'.")
            appendLine("- If you drift into irrelevant banter for more than 2 lines, snap back to the mission with a decisive action cue.")
            appendLine("- Emulate Filipino closure etiquette: once the task is done, cheerfully cut the chat ('Sige ha, naa ra ko dri') instead of lingering questions.")
        }.trim()

        val localizedPrompt = """
            $systemPromptWithIdentity

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $latestLearnerMessage

            Respond strictly in JSON with exactly these fields and no extras:
            {
              "aiSpeech": "Assistant reply in Bisaya ONLY. Do not include Japanese, English, or Tagalog. Append ' [FINISH]' only when the target goal is achieved.",
              "aiTranslation": "${if (translationLanguage == TranslationLanguage.JAPANESE) "Japanese" else "English"} translation of aiSpeech ONLY.",
              "options": [
                {
                  "text": "Suggested learner reply in Bisaya ONLY (no Tagalog words, no nickname).",
                  "translation": "${if (translationLanguage == TranslationLanguage.JAPANESE) "Japanese" else "English"} translation of that option ONLY.",
                  "tone": "Short descriptor in Japanese (optional)."
                }
              ]
            }
            - Output 2-3 concise options unless you already ended with [FINISH], in which case options must be an empty array.
            - Never include markdown, commentary, or explanations outside the JSON object.
        """.trimIndent()

        val prompt = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> localizedPrompt
            TranslationLanguage.ENGLISH -> "Translate to English:\n$localizedPrompt"
        }

        Log.d(
            "RoleplayChatViewModel",
            "Prompt generated lang=$translationLanguage userMsg='${userMessage.take(40)}'"
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

    private fun selectCompletionFarewell(): FarewellLine {
        currentScenarioClosingGuidance?.farewellExamples?.takeIf { it.isNotEmpty() }?.let { examples ->
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

    private fun cancelAutoExitCountdown() {
        autoExitJob?.cancel()
        autoExitJob = null
    }

    private fun resetAutoExitState() {
        cancelAutoExitCountdown()
        _uiState.update { it.copy(autoExitCountdownMs = 0L, autoExitArmed = false, pendingExitHistory = null) }
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
