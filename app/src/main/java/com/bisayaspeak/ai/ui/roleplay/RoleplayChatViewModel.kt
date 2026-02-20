package com.bisayaspeak.ai.ui.roleplay

import android.app.Application
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.data.repository.FreeUsageManager
import com.bisayaspeak.ai.data.repository.FreeUsageRepository
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.RoleplayHistoryRepository
import com.bisayaspeak.ai.data.repository.ScenarioRepository
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.domain.honor.HonorLevelManager
import com.bisayaspeak.ai.ui.roleplay.RoleplayThemeManager
import com.bisayaspeak.ai.util.LocaleUtils
import com.bisayaspeak.ai.utils.MistakeManager
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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

sealed interface RoleplayEvent {
    data object ShowSanpoUpsell : RoleplayEvent
    data class RequestSanpoInterstitial(val phase: SanpoAdGatePhase) : RoleplayEvent
    data class RequireSanpoAd(val placement: String) : RoleplayEvent
    data class ShowSanpoUpsellDialog(val placement: String) : RoleplayEvent
}

enum class SanpoMonetPlacement { ENTER, TURN_COMPLETE, EXIT_TO_TOP }

private enum class OptionSource {
    STARTER,
    TEMPLATE,
    AI,
    UNKNOWN
}

data class FarewellLine(
    val bisaya: String,
    val translation: String,
    val explanation: String
)

data class SanpoFarewellLine(
    val bisaya: String,
    val translationJa: String,
    val translationEn: String
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
    val roleplayMode: RoleplayMode = RoleplayMode.SANPO,
    val learnerName: String = "",
    val isSanpoEnding: Boolean = false,
    val sanpoEndingFarewell: String = "",
    val sanpoEndingFarewellTranslation: String = "",
    val sanpoAdGateRemainingCount: Int = 0,
    val pendingSanpoStart: Boolean = false,
    val sanpoAdGatePhase: SanpoAdGatePhase = SanpoAdGatePhase.IDLE,
    val pendingSecondAdReason: SanpoSecondAdReason = SanpoSecondAdReason.NONE,
    val sanpoTurnCount: Int = 0,
    val hasShownTurnLimitAdThisSession: Boolean = false
)

enum class TranslationLanguage { JAPANESE, ENGLISH }

enum class SanpoAdGatePhase {
    IDLE,
    FIRST_GATE_PENDING,
    FIRST_GATE_SHOWING,
    SECOND_GATE_PENDING,
    SECOND_GATE_SHOWING,
    TURN_LIMIT_PENDING,
    TURN_LIMIT_SHOWING
}

enum class SanpoSecondAdReason {
    NONE,
    USER_CANCEL_ONLY,
    REENTRY_AFTER_DISMISS,
    SESSION_EXIT
}

private enum class RelationshipMode {
    FORMAL_UNKNOWN,
    INTIMATE_KNOWN
}

class RoleplayChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val scenarioRepository = ScenarioRepository(application)
    private val chatRepository = OpenAiChatRepository()
    private val promptManager = RoleplayPromptManager()
    private val usageRepository = UsageRepository(application)
    private val whisperRepository = WhisperRepository()
    private val roleplayHistoryRepository = RoleplayHistoryRepository(application)
    private val sanpoPromptProvider = SanpoPromptProvider()
    private val themeManager = RoleplayThemeManager()
    private val voiceRecorder = VoiceInputRecorder(application)
    private val random: Random = Random(System.currentTimeMillis())
    private val scenarioGenerator = ScenarioGenerator(random)
    private val guestNicknameFallback = application.getString(R.string.account_guest_nickname_default)

    init {
        viewModelScope.launch {
            userPreferencesRepository.sanpoCycleState.collect { sanpoCycleState = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.sanpoTurnCount.collect { sanpoTurnCount = it }
        }
        viewModelScope.launch {
            userPreferencesRepository.sanpoMonetCount.collect { sanpoMonetCount = it }
        }
    }

    private val _uiState = MutableStateFlow(RoleplayUiState())
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()
    private val _events = Channel<RoleplayEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private var currentMode: RoleplayMode = RoleplayMode.SANPO
    private var turnCount: Int = 0
    private val history = mutableListOf<MissionHistoryMessage>()
    private val branchFacts = mutableMapOf<String, String>()
    private val optionSourceById = mutableMapOf<String, OptionSource>()
    private var currentUserLevel: Int = 1
    private var isProVersion: Boolean = false
    private var optionTutorialVisible: Boolean = true
    private var isJapaneseLocale: Boolean = true
    private var currentRelationshipMode: RelationshipMode = RelationshipMode.FORMAL_UNKNOWN
    private var knownLearnerName: String? = null
    private var isLearnerIdentified: Boolean = false
    private var userCallSign: String = ""
    private var calloutBisaya: String = "Friend"
    private var calloutEnglish: String = "Friend"
    private var userNickname: String? = null
    private var pendingAutoExitHistory: List<MissionHistoryMessage>? = null
    private var scriptedRuntime: ScriptedRuntime? = null
    private var activeDynamicScenario: DynamicScenarioTemplate? = null
    private var currentScenarioClosingGuidance: ScenarioClosingGuidance? = null
    private var activeTheme: RoleplayThemeDefinition = themeManager.drawTheme(1, RoleplayThemeFlavor.CASUAL)
    private var activeFlavor: RoleplayThemeFlavor = RoleplayThemeFlavor.CASUAL
    private var lastRandomThemeId: String? = null
    private var lastSceneSeed: String? = null
    private var currentRecordingFile: File? = null
    private var autoExitJob: Job? = null
    private var farewellSignals: Set<String> = DEFAULT_FAREWELL_KEYWORDS
    private var currentUserGender: UserGender = UserGender.OTHER
    private var calloutJapanese: String = "Friend"
    private var sanpoAdGateConsumed: Boolean = false
    private var sanpoUpsellConsumed: Boolean = false
    private var sanpoSessionId: String = ""
    private var lastSanpoEntryAtMs: Long = 0L
    private var lastSanpoExitAtMs: Long = 0L
    private var lastSanpoSessionTurnCount: Int = 0

    // Sanpo monetization cycle state
    private var sanpoCycleState: String = "NEW"
    private var sanpoTurnCount: Int = 0
    private var sanpoMonetCount: Int = 0

    private companion object {
        private const val LOG_TAG = "LearnBisaya"
        private const val SANPO_REENTRY_DEBOUNCE_MS = 10_000L
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val TARI_SCENARIO_ID = "sanpo_free_talk"
        private const val SANPO_TURN_LIMIT_AD_THRESHOLD = 3
        const val SANPO_QUOTA_ENFORCEMENT_ENABLED = false
        private const val LOCKED_OPTION_HOLD_MS = 500L
        private const val POST_CLEAR_SILENCE_MS = 1000L
        private const val SCENARIO_LOG_TAG = "ScenarioGenerator"
        private const val OPTION_LOG_TAG = "RoleplayOptionLogger"
        private const val FORCED_EXIT_TURN_THRESHOLD = 6
        private const val FORCED_FAREWELL_OPTION_ID = "forced-farewell-option"
        private const val SANPO_AD_GATE_IMPRESSIONS = 2
        private val SANPO_FAREWELL_LINES = listOf(
            SanpoFarewellLine(
                bisaya = "Sige ha, naa pa koy buhaton. Magkita ta sunod.",
                translationJa = "ちょっと用事があるから行ってくるね。また会おう。",
                translationEn = "Okay, I still have something to do. Let's meet again later."
            ),
            SanpoFarewellLine(
                bisaya = "Amping sa imong adlaw. Mag-istorya ta puhon.",
                translationJa = "今日はこのへんで。続きはまた今度話そう。",
                translationEn = "Take care with the rest of your day. We'll chat again soon."
            ),
            SanpoFarewellLine(
                bisaya = "Sige una ko ha. Tawga lang ko kung kinahanglan nimo ko.",
                translationJa = "じゃあ先に行くね。必要なときはいつでも呼んで。",
                translationEn = "I'll head out first. Call me whenever you need me."
            ),
            SanpoFarewellLine(
                bisaya = "Pahuway usa ko gamay. Balik lang og chat kung andam naka.",
                translationJa = "ちょっと休むね。準備できたらまた話そう。",
                translationEn = "Let me rest a bit. Chat me again when you're ready."
            )
        )
        private val levelPrefixRegex = Regex("^LV\\s*\\d+\\s*: \\s*", RegexOption.IGNORE_CASE)
        private const val OPTION_TUTORIAL_VERSION = 2
        private const val MIN_VISIBLE_OPTIONS = 2
        private const val MAX_VISIBLE_OPTIONS = 3
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
            Regex("私の名前は\\s*([\\u3040-\\u309F\\u30A0-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFFーA-Za-z\\s]{1,20})"),
            Regex("僕の名前は\\s*([\\u3040-\\u309F\\u30A0-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFFーA-Za-z\\s]{1,20})"),
            Regex("俺の名前は\\s*([\\u3040-\\u309F\\u30A0-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFFーA-Za-z\\s]{1,20})"),
            Regex("私は\\s*([\\u3040-\\u309F\\u30A0-\\u30FF\\u3400-\\u4DBF\\u4E00-\\u9FFFーA-Za-z\\s]{1,20})です")
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
        private val DOJO_CONTENTS = listOf(
            "突然のスコール",
            "タクシー運転手との料金交渉",
            "屋台のくじ引き当選",
            "友人との偶然の再会",
            "忘れ物を取りに戻る騒動",
            "即席ライブ演奏が始まる",
            "停電で街灯が一瞬消える",
            "お祭りの打ち上げ花火"
        )

        private val DEFAULT_DOJO_STARTER_FALLBACKS = listOf(
            RoleplayStarterOption(
                text = "Maayong adlaw. Palihug ko gamayng tabang.",
                translation = "こんにちは。少しだけ助けてください。"
            ),
            RoleplayStarterOption(
                text = "Pasensya ha, naa koy hangyo nga gusto ipangutana.",
                translation = "すみません、お願いがあって伺いたいです。"
            ),
            RoleplayStarterOption(
                text = "Gusto ko maabot ang tumong, tabangi ko og lakang-lakang.",
                translation = "目標を達成したいので、段取りを教えてください。"
            )
        )

        private data class DojoProgressOptionTemplate(
            val text: String,
            val translation: String
        )

        private val DOJO_PROGRESS_OPTION_SETS: List<List<DojoProgressOptionTemplate>> = listOf(
            listOf(
                DojoProgressOptionTemplate(
                    text = "Masabtan nako. Unsa may sunod nakong lakang?",
                    translation = "わかりました。次に私がするべきことは何ですか？"
                ),
                DojoProgressOptionTemplate(
                    text = "Pwede ko mangayo nimo og klaro nga instruksyon?",
                    translation = "具体的な指示をいただけますか？"
                ),
                DojoProgressOptionTemplate(
                    text = "Salamat sa pagsulti. Sugdan nato ning proseso karon dayon.",
                    translation = "伝えてくれてありがとう。今すぐこの手続きを始めます。"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Nag-andam na ko sa papeles. Palihug ko'g tan-aw kung sakto na.",
                    translation = "書類を準備しました。これで合っているか確認してください。"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung naa kay gustong usbon, sulti lang para ma-adjust nako.",
                    translation = "修正したい点があれば、すぐ調整するので教えてください。"
                ),
                DojoProgressOptionTemplate(
                    text = "Akoa ning i-file human nimo i-approve. OK ra?",
                    translation = "承認いただけたら提出します。よろしいですか？"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Andam ko maminaw sa imong obserbasyon sa akong gihimo.",
                    translation = "私の対応についてご意見を伺いたいです。"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung kinahanglan pa kog ebidensya, hatagan tika dayon.",
                    translation = "さらに証拠が必要ならすぐにお渡しします。"
                ),
                DojoProgressOptionTemplate(
                    text = "Basig naa kay laing paagi nga mas hapsay, tudlui ko.",
                    translation = "もっと良い進め方があれば教えてください。"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Gibuhat na nako ang imong sugo. Palihug tan-awa ang resulta.",
                    translation = "ご指示通り進めました。結果をご確認ください。"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung kulang pa, ingna lang ko para madungagan nako.",
                    translation = "不足があればすぐに補います。"
                ),
                DojoProgressOptionTemplate(
                    text = "Maghulat ko sa imong go signal para mosunod sa sunod nga lakang.",
                    translation = "次のステップに進む合図を待っています。"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Nisunod ko sa timeline. Asa pa ko dapat mupaspas?",
                    translation = "スケジュール通り進めていますが、急ぐべき箇所はありますか？"
                ),
                DojoProgressOptionTemplate(
                    text = "Gisiguro nako nga respetado gihapon ang imong mga kondisyon.",
                    translation = "ご条件を尊重して調整しています。"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung naa kay feedback, dawaton ko og bukas nga hunahuna.",
                    translation = "フィードバックがあれば素直に受け止めます。"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Nalamdagan nako ang kahimtang. Andam kong mosalo sa bisan unsang kulang.",
                    translation = "状況が掴めました。不足があれば補います。"
                ),
                DojoProgressOptionTemplate(
                    text = "Pwede na ba nato i-finalize ang kasabotan?",
                    translation = "合意内容をそろそろ確定してよろしいですか？"
                ),
                DojoProgressOptionTemplate(
                    text = "Gusto ko masiguro nga komportable ka sa akong gihimo.",
                    translation = "私の対応にご安心いただけていますか？"
                )
            ),
            listOf(
                DojoProgressOptionTemplate(
                    text = "Salamat sa imong pasensya. Adu na bay final check nga kinahanglan?",
                    translation = "ご辛抱ありがとうございます。最後の確認はありますか？"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung satisfied na ka, akong ipadayag nga human ang buluhaton.",
                    translation = "ご満足いただけたら、任務完了と宣言します。"
                ),
                DojoProgressOptionTemplate(
                    text = "Kung naa pa kay mando, sugu-a lang ko karon mismo.",
                    translation = "まだ指示があれば今すぐ承ります。"
                )
            )
        )
    }

    private fun buildModeAwareSystemPrompt(details: String = ""): String {
        return when (currentMode) {
            RoleplayMode.SANPO -> buildSanpoSystemPrompt()
            RoleplayMode.DOJO -> {
                val scenarioDetails = details.ifBlank { "指定シチュエーション" }
                promptManager.getSystemPrompt(
                    mode = RoleplayMode.DOJO,
                    userName = "stranger",
                    details = scenarioDetails
                )
            }
        }
    }

    fun onSanpoAdStarted(phase: SanpoAdGatePhase) {
        if (phase == SanpoAdGatePhase.IDLE) return
        _uiState.update {
            it.copy(
                sanpoAdGatePhase = when (phase) {
                    SanpoAdGatePhase.FIRST_GATE_PENDING, SanpoAdGatePhase.FIRST_GATE_SHOWING -> SanpoAdGatePhase.FIRST_GATE_SHOWING
                    SanpoAdGatePhase.SECOND_GATE_PENDING, SanpoAdGatePhase.SECOND_GATE_SHOWING -> SanpoAdGatePhase.SECOND_GATE_SHOWING
                    SanpoAdGatePhase.TURN_LIMIT_PENDING, SanpoAdGatePhase.TURN_LIMIT_SHOWING -> SanpoAdGatePhase.TURN_LIMIT_SHOWING
                    SanpoAdGatePhase.IDLE -> SanpoAdGatePhase.IDLE
                }
            )
        }
    }

    fun onSanpoAdDismissed(phase: SanpoAdGatePhase) {
        Log.d("SanpoAdFlow", "AD_DISMISSED gate=$phase session=$sanpoSessionId")
        when (phase) {
            SanpoAdGatePhase.FIRST_GATE_PENDING, SanpoAdGatePhase.FIRST_GATE_SHOWING -> {
                _uiState.update {
                    it.copy(
                        sanpoAdGatePhase = SanpoAdGatePhase.IDLE,
                        pendingSecondAdReason = it.pendingSecondAdReason.takeIf { reason -> reason != SanpoSecondAdReason.NONE }
                            ?: SanpoSecondAdReason.USER_CANCEL_ONLY
                    )
                }
                completeSanpoAdGate()
            }
            SanpoAdGatePhase.SECOND_GATE_PENDING, SanpoAdGatePhase.SECOND_GATE_SHOWING -> {
                _uiState.update {
                    it.copy(
                        sanpoAdGatePhase = SanpoAdGatePhase.IDLE,
                        pendingSecondAdReason = SanpoSecondAdReason.NONE
                    )
                }
                completeSanpoAdGate()
            }
            SanpoAdGatePhase.TURN_LIMIT_PENDING, SanpoAdGatePhase.TURN_LIMIT_SHOWING -> {
                _uiState.update { it.copy(sanpoAdGatePhase = SanpoAdGatePhase.IDLE) }
                viewModelScope.launch { requestUpgradeForSanpo() }
            }
            SanpoAdGatePhase.IDLE -> Unit
        }
    }

    private fun registerSanpoTurn() {
        if (currentMode != RoleplayMode.SANPO) return
        if (isProVersion) {
            _uiState.update { it.copy(sanpoTurnCount = it.sanpoTurnCount + 1) }
            return
        }

        var shouldTriggerTurnLimitAd = false
        _uiState.update {
            val nextCount = it.sanpoTurnCount + 1
            val trigger = nextCount >= SANPO_TURN_LIMIT_AD_THRESHOLD && !it.hasShownTurnLimitAdThisSession
            if (trigger) {
                shouldTriggerTurnLimitAd = true
            }
            it.copy(
                sanpoTurnCount = nextCount,
                hasShownTurnLimitAdThisSession = it.hasShownTurnLimitAdThisSession || trigger
            )
        }
        Log.d(
            LOG_TAG,
            "registerSanpoTurn mode=$currentMode count=${_uiState.value.sanpoTurnCount} trigger=$shouldTriggerTurnLimitAd pro=$isProVersion"
        )
        Log.d("SanpoAdFlow", "TURN turn=${_uiState.value.sanpoTurnCount} session=$sanpoSessionId")

        // Update monetization cycle state and trigger TURN_COMPLETE
        sanpoTurnCount = _uiState.value.sanpoTurnCount
        viewModelScope.launch { userPreferencesRepository.setSanpoTurnCount(sanpoTurnCount) }
        if (sanpoCycleState == "NEW") {
            sanpoCycleState = "IN_PROGRESS"
            viewModelScope.launch { userPreferencesRepository.setSanpoCycleState("IN_PROGRESS") }
        }
        onSanpoMonetEvent(SanpoMonetPlacement.TURN_COMPLETE)

        if (!shouldTriggerTurnLimitAd) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(sanpoAdGatePhase = SanpoAdGatePhase.TURN_LIMIT_PENDING)
            }
            Log.d(LOG_TAG, "Sanpo turn limit ad requested phase=${SanpoAdGatePhase.TURN_LIMIT_PENDING}")
            Log.d("SanpoAdFlow", "REQUEST_AD gate=${SanpoAdGatePhase.TURN_LIMIT_PENDING} session=$sanpoSessionId turn=${_uiState.value.sanpoTurnCount}")
            _events.send(RoleplayEvent.RequestSanpoInterstitial(SanpoAdGatePhase.TURN_LIMIT_PENDING))
        }
    }

    fun onSanpoAdFailed(phase: SanpoAdGatePhase) {
        when (phase) {
            SanpoAdGatePhase.FIRST_GATE_PENDING, SanpoAdGatePhase.FIRST_GATE_SHOWING -> {
                _uiState.update { it.copy(sanpoAdGatePhase = SanpoAdGatePhase.IDLE) }
                completeSanpoAdGate()
            }
            SanpoAdGatePhase.SECOND_GATE_PENDING, SanpoAdGatePhase.SECOND_GATE_SHOWING -> {
                _uiState.update {
                    it.copy(
                        sanpoAdGatePhase = SanpoAdGatePhase.IDLE,
                        pendingSecondAdReason = SanpoSecondAdReason.NONE
                    )
                }
                completeSanpoAdGate()
            }
            SanpoAdGatePhase.TURN_LIMIT_PENDING, SanpoAdGatePhase.TURN_LIMIT_SHOWING -> {
                _uiState.update { it.copy(sanpoAdGatePhase = SanpoAdGatePhase.IDLE) }
                viewModelScope.launch { requestUpgradeForSanpo() }
            }
            SanpoAdGatePhase.IDLE -> Unit
        }
    }

    private suspend fun requestUpgradeForSanpo(endSession: Boolean = true) {
        val currentTurns = _uiState.value.sanpoTurnCount
        if (currentTurns < SANPO_TURN_LIMIT_AD_THRESHOLD) {
            Log.d(LOG_TAG, "Sanpo upsell suppressed turns=$currentTurns sessionId=$sanpoSessionId")
            return
        }
        if (sanpoUpsellConsumed) {
            Log.d(LOG_TAG, "Sanpo upsell already consumed sessionId=$sanpoSessionId")
            return
        }
        if (shouldDebounceUpsell()) {
            Log.d(LOG_TAG, "Sanpo upsell debounced due to quick re-entry sessionId=$sanpoSessionId")
            return
        }
        sanpoUpsellConsumed = true
        lastSanpoSessionTurnCount = currentTurns
        lastSanpoExitAtMs = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                messages = it.messages,
                options = emptyList(),
                lockedOption = null,
                isLoading = false,
                finalFarewellMessageId = null,
                pendingExitHistory = null,
                tutorialMessage = "",
                tutorialHint = "",
                sanpoAdGateRemainingCount = 0,
                pendingSanpoStart = false,
                isEndingSession = if (endSession) true else it.isEndingSession
            )
        }
        if (endSession) {
            val snapshot = history.toList()
            if (snapshot.isNotEmpty()) {
                _uiState.update { it.copy(pendingExitHistory = snapshot) }
            }
        }
        _events.send(RoleplayEvent.ShowSanpoUpsell)
    }

    private fun shouldDebounceUpsell(): Boolean {
        if (lastSanpoExitAtMs == 0L) return false
        if (lastSanpoSessionTurnCount >= SANPO_TURN_LIMIT_AD_THRESHOLD) return false
        val elapsed = System.currentTimeMillis() - lastSanpoExitAtMs
        return elapsed in 1 until SANPO_REENTRY_DEBOUNCE_MS
    }

    fun completeSanpoAdGate() {
        val state = _uiState.value
        val shouldStartSanpo = state.pendingSanpoStart
        _uiState.update { it.copy(sanpoAdGateRemainingCount = 0, pendingSanpoStart = false) }
        if (shouldStartSanpo) {
            sanpoAdGateConsumed = true
            startSanpoSession(isProVersion)
        }
    }

    private fun shouldEnforceSanpoAdGate(): Boolean {
        val reasons = mutableListOf<String>()
        if (!BuildConfig.IS_LITE_BUILD) reasons += "not_lite"
        if (isProVersion) reasons += "pro_user"
        if (currentMode != RoleplayMode.SANPO) reasons += "not_sanpo"
        if (sanpoAdGateConsumed) reasons += "consumed"
        val result = BuildConfig.IS_LITE_BUILD && !isProVersion && currentMode == RoleplayMode.SANPO && !sanpoAdGateConsumed
        val reason = if (result) "eligible" else reasons.joinToString(",").ifBlank { "other" }
        Log.d(
            LOG_TAG,
            "shouldEnforceSanpoAdGate=$result lite=${BuildConfig.IS_LITE_BUILD} pro=$isProVersion mode=$currentMode consumed=$sanpoAdGateConsumed"
        )
        Log.d(
            "SanpoAdFlow",
            "SHOULD_ENFORCE result=$result reason=$reason consumed=$sanpoAdGateConsumed mode=$currentMode turn=${_uiState.value.sanpoTurnCount}"
        )
        return result
    }

    private fun onSanpoMonetEvent(placement: SanpoMonetPlacement) {
        if (isProVersion) {
            Log.i("SANPO_MONET", "SKIP placement=$placement reason=pro_user")
            return
        }
        when (placement) {
            SanpoMonetPlacement.ENTER -> {
                Log.i("SANPO_MONET", "ENTER state=$sanpoCycleState turn=$sanpoTurnCount will_show_upsell=${sanpoCycleState == "READY_FOR_UPSELL"}")
                handleSanpoMonetRequest(if (sanpoCycleState == "READY_FOR_UPSELL") "ENTER_READY" else "ENTER")
            }
            SanpoMonetPlacement.TURN_COMPLETE -> {
                val nextTurn = sanpoTurnCount + 1
                val becameReady = nextTurn >= 3 && sanpoCycleState == "IN_PROGRESS"
                Log.i("SANPO_MONET", "TURN_COMPLETE turn=$nextTurn became_ready=$becameReady")
                if (becameReady) {
                    sanpoCycleState = "READY_FOR_UPSELL"
                    viewModelScope.launch { userPreferencesRepository.setSanpoCycleState("READY_FOR_UPSELL") }
                }
            }
            SanpoMonetPlacement.EXIT_TO_TOP -> {
                val becameReady = sanpoCycleState == "IN_PROGRESS"
                Log.i("SANPO_MONET", "EXIT_TO_TOP became_ready=$becameReady")
                if (becameReady) {
                    sanpoCycleState = "READY_FOR_UPSELL"
                    viewModelScope.launch { userPreferencesRepository.setSanpoCycleState("READY_FOR_UPSELL") }
                }
            }
        }
    }

    private fun handleSanpoMonetRequest(placement: String) {
        Log.i("SANPO_MONET", "REQUEST placement=$placement isPro=$isProVersion lite=${BuildConfig.IS_LITE_BUILD}")
        
        if (isProVersion) {
            Log.i("SANPO_MONET", "SKIP placement=$placement reason=pro_user")
            return
        }
        viewModelScope.launch {
            val countBefore = sanpoMonetCount
            sanpoMonetCount++
            userPreferencesRepository.setSanpoMonetCount(sanpoMonetCount)
            val countAfter = sanpoMonetCount
            val shouldShowAd = countAfter in listOf(2, 4)
            val shouldShowUpsell = countAfter >= 5

            Log.i(
                "SANPO_MONET",
                "CHECK placement=$placement count_before=$countBefore count_after=$countAfter decision=${when {
                    shouldShowUpsell -> "SHOW_UPSELL"
                    shouldShowAd -> "SHOW_AD"
                    else -> "NONE"
                }} skip_reason=${when {
                    shouldShowUpsell -> null
                    shouldShowAd -> null
                    else -> "not_eligible"
                }}"
            )

            when {
                shouldShowUpsell -> {
                    Log.i("SANPO_MONET", "SHOW_UPSELL placement=$placement")
                    _events.send(RoleplayEvent.ShowSanpoUpsellDialog(placement))
                    resetSanpoMonetCycle()
                }
                shouldShowAd -> {
                    Log.i("SANPO_MONET", "SHOW_AD placement=$placement")
                    _events.send(RoleplayEvent.RequestSanpoInterstitial(SanpoAdGatePhase.FIRST_GATE_PENDING))
                }
                else -> {
                    Log.i("SANPO_MONET", "NO_ACTION placement=$placement")
                }
            }
        }
    }

    private fun resetSanpoMonetCycle() {
        Log.i("SANPO_MONET", "RESET cycle")
        sanpoCycleState = "NEW"
        sanpoTurnCount = 0
        sanpoMonetCount = 0
        viewModelScope.launch {
            userPreferencesRepository.setSanpoCycleState("NEW")
            userPreferencesRepository.setSanpoTurnCount(0)
            userPreferencesRepository.setSanpoMonetCount(0)
        }
    }

    private fun switchMode(newMode: RoleplayMode) {
        currentMode = newMode
        turnCount = 0
        history.clear()
        branchFacts.clear()
        optionSourceById.clear()
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
        if (currentMode == RoleplayMode.SANPO) {
            lastSanpoSessionTurnCount = _uiState.value.sanpoTurnCount
            lastSanpoExitAtMs = System.currentTimeMillis()
            Log.d(
                LOG_TAG,
                "Sanpo session end sessionId=$sanpoSessionId turns=$lastSanpoSessionTurnCount exitAt=$lastSanpoExitAtMs"
            )
            // Trigger EXIT_TO_TOP monetization event
            onSanpoMonetEvent(SanpoMonetPlacement.EXIT_TO_TOP)
        }
    }

    fun onSanpoEnter(isProUser: Boolean) {
        isProVersion = isProUser
        currentMode = RoleplayMode.SANPO
        sanpoSessionId = generateSanpoSessionId()
        val now = System.currentTimeMillis()
        lastSanpoEntryAtMs = now
        sanpoAdGateConsumed = false
        sanpoUpsellConsumed = false
        _uiState.update {
            it.copy(
                sanpoTurnCount = 0,
                hasShownTurnLimitAdThisSession = false
            )
        }
        val pendingReason = _uiState.value.pendingSecondAdReason
        Log.d(
            LOG_TAG,
            "onSanpoEnter sessionId=$sanpoSessionId pro=$isProUser lite=${BuildConfig.IS_LITE_BUILD} gateConsumed=$sanpoAdGateConsumed pendingSecond=$pendingReason entryAt=$now"
        )
        val firstGatePending = shouldEnforceSanpoAdGate()
        val secondGatePending = pendingReason != SanpoSecondAdReason.NONE
        Log.d(
            "SanpoAdFlow",
            "ENTER pro=$isProUser lite=${BuildConfig.IS_LITE_BUILD} session=$sanpoSessionId first=$firstGatePending second=$secondGatePending turn=${_uiState.value.sanpoTurnCount}"
        )

        // Sanpo monetization trigger
        onSanpoMonetEvent(SanpoMonetPlacement.ENTER)

        if (isProUser || !BuildConfig.IS_LITE_BUILD) {
            startSanpoSession(isProUser)
            return
        }

        viewModelScope.launch {
            val hasFreeExperience = if (SANPO_QUOTA_ENFORCEMENT_ENABLED) {
                runCatching { FreeUsageManager.canStartSanpo() }.getOrDefault(false)
            } else {
                Log.w(LOG_TAG, "Sanpo quota temporarily bypassed for re-entry sessionId=$sanpoSessionId")
                true
            }
            if (!hasFreeExperience) {
                Log.w(LOG_TAG, "Sanpo start skipped due to exhausted quota sessionId=$sanpoSessionId")
                return@launch
            }

            val shouldShowSecondGate = _uiState.value.pendingSecondAdReason != SanpoSecondAdReason.NONE
            when {
                shouldShowSecondGate -> {
                    _uiState.update {
                        it.copy(
                            pendingSanpoStart = true,
                            sanpoAdGatePhase = SanpoAdGatePhase.SECOND_GATE_PENDING
                        )
                    }
                    Log.d(
                        "SanpoAdFlow",
                        "REQUEST_AD gate=${SanpoAdGatePhase.SECOND_GATE_PENDING} session=$sanpoSessionId turn=${_uiState.value.sanpoTurnCount}"
                    )
                    _events.send(RoleplayEvent.RequestSanpoInterstitial(SanpoAdGatePhase.SECOND_GATE_PENDING))
                }
                shouldEnforceSanpoAdGate() -> {
                    _uiState.update {
                        it.copy(
                            pendingSanpoStart = true,
                            sanpoAdGatePhase = SanpoAdGatePhase.FIRST_GATE_PENDING
                        )
                    }
                    Log.d(
                        "SanpoAdFlow",
                        "REQUEST_AD gate=${SanpoAdGatePhase.FIRST_GATE_PENDING} session=$sanpoSessionId turn=${_uiState.value.sanpoTurnCount}"
                    )
                    _events.send(RoleplayEvent.RequestSanpoInterstitial(SanpoAdGatePhase.FIRST_GATE_PENDING))
                }
                else -> startSanpoSession(isProUser)
            }
        }
    }

    private fun loadInfiniteTariMode(isProUser: Boolean) {
        onSanpoEnter(isProUser)
    }

    private fun startSanpoSession(isProUser: Boolean) {
        if (!isProUser) {
            viewModelScope.launch {
                val canUse = if (SANPO_QUOTA_ENFORCEMENT_ENABLED) {
                    runCatching { FreeUsageManager.canStartSanpo() }.getOrDefault(false)
                } else {
                    true
                }
                if (!canUse) {
                    requestUpgradeForSanpo()
                    return@launch
                }
                proceedLoadSanpo(isProUser)
            }
        } else {
            proceedLoadSanpo(isProUser)
        }
    }

    private fun proceedLoadSanpo(isProUser: Boolean) {
        resetAutoExitState()
        switchMode(RoleplayMode.SANPO)
        turnCount = 0
        _uiState.update {
            it.copy(
                sanpoTurnCount = 0,
                hasShownTurnLimitAdThisSession = false
            )
        }
        val definition = buildSanpoScenarioDefinition()
        pendingAutoExitHistory = null
        scriptedRuntime = null
        activeDynamicScenario = null
        isProVersion = isProUser
        currentScenarioClosingGuidance = null
        refreshFarewellSignals(definition)
        selectActiveTheme(definition)
        currentRelationshipMode = RelationshipMode.FORMAL_UNKNOWN
        knownLearnerName = null
        isLearnerIdentified = false
        val systemPrompt = buildModeAwareSystemPrompt()
        if (!isProUser) {
            markSanpoUsageIfNeeded()
        }

        val initialSanpoOptions = listOf(
            RoleplayOption(
                text = "Maayong adlaw!",
                hint = "こんにちは！"
            ),
            RoleplayOption(
                text = "Asa ta paingon?",
                hint = "どこに向かう？"
            ),
            RoleplayOption(
                text = "Unsa imong ginabuhat?",
                hint = "何してるの？"
            )
        )

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = systemPrompt,
            messages = emptyList(),
            isLoading = false,
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
            activeSceneLabel = definition.title,
            activeSceneDescription = definition.description,
            activeSceneIntroLine = "",
            showOptionTutorial = optionTutorialVisible,
            translationDirective = translationDirective.value,
            goalAchieved = false,
            isEndingSession = false,
            roleplayMode = RoleplayMode.SANPO,
            learnerName = resolveUserDisplayName(),
            options = initialSanpoOptions,
            sanpoAdGateRemainingCount = 0,
            pendingSanpoStart = false
        )

        requestAiTurn(
            scenario = definition,
            userMessage = START_TOKEN
        )
    }

    private fun markSanpoUsageIfNeeded() {
        if (isProVersion) return
        if (!SANPO_QUOTA_ENFORCEMENT_ENABLED) {
            Log.d(LOG_TAG, "Sanpo quota enforcement disabled; skipping consumption sessionId=$sanpoSessionId")
            return
        }
        viewModelScope.launch {
            runCatching {
                FreeUsageManager.resetIfNewDay()
                FreeUsageManager.consumeSanpoStart()
                val day = FreeUsageManager.dayKey() ?: FreeUsageManager.currentDayKey()
                val count = FreeUsageManager.sanpoCount()
                FreeUsageManager.logUsage(
                    "free_limit_check feature=sanpo event=start day=${'$'}day count=${'$'}count premium=${isProVersion}"
                )
                Log.d(LOG_TAG, "sanpo consumed day=$day count=$count premium=$isProVersion")
                sanpoAdGateConsumed = true
            }.onFailure { error ->
                Log.e(LOG_TAG, "sanpo consume failed", error)
            }
        }
    }

    private fun buildSanpoScenarioDefinition(): RoleplayScenarioDefinition {
        Log.d("SanpoScenario", "Building Sanpo scenario with ID: $TARI_SCENARIO_ID")
        Log.d("SanpoScenario", "BuildConfig.FLAVOR: ${BuildConfig.FLAVOR}")
        Log.d("SanpoScenario", "BuildConfig.IS_LITE_BUILD: ${BuildConfig.IS_LITE_BUILD}")
        
        // Always use scenarioRepository - no fallback
        val result = scenarioRepository.getScenarioById(TARI_SCENARIO_ID)
        Log.d("SanpoScenario", "ScenarioRepository returned: ${result?.id ?: "null"}")
        
        val missionScenario = result ?: throw IllegalStateException("Sanpo scenario '$TARI_SCENARIO_ID' not found in ScenarioRepository")
        
        return convertToRoleplayScenarioDefinition(missionScenario)
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
                val resolvedNickname = profile.nickname
                    .takeIf { it.isNotBlank() && it != guestNicknameFallback }
                userNickname = resolvedNickname
                refreshLearnerNameState()
            }
        }
    }

    private fun resolveUserDisplayName(): String {
        return when {
            !userNickname.isNullOrBlank() -> userNickname!!
            knownLearnerName?.isNotBlank() == true -> knownLearnerName!!
            isJapaneseLocale -> calloutJapanese
            else -> calloutEnglish
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
        GeminiVoiceService.switchMode(RoleplayMode.DOJO)
        val definition = convertToRoleplayScenarioDefinition(scenario)
        pendingAutoExitHistory = null
        scriptedRuntime = scriptedScenarioDefinitions[scenarioId]?.let { ScriptedRuntime(it) }
        activeDynamicScenario = null
        isProVersion = isProUser
        currentScenarioClosingGuidance = definition.closingGuidance
        refreshFarewellSignals(definition)
        selectActiveTheme(definition)
        currentRelationshipMode = RelationshipMode.FORMAL_UNKNOWN
        knownLearnerName = null
        isLearnerIdentified = false

        val cleanThemeTitle = levelPrefixRegex.replace(activeTheme.title, "").trim().ifBlank { activeTheme.title }
        val closingCue = activeTheme.closingCue
        val forceScenarioCopy = true
        val localizedThemeTitle = if (forceScenarioCopy) definition.title else if (isJapaneseLocale) cleanThemeTitle else definition.title
        val localizedThemeDescription = if (forceScenarioCopy) definition.situation else if (isJapaneseLocale) activeTheme.description else definition.situation
        val localizedThemePersona = if (forceScenarioCopy) definition.aiRole else if (isJapaneseLocale) activeTheme.persona else definition.aiRole
        val localizedThemeGoal = if (forceScenarioCopy) definition.goal else if (isJapaneseLocale) activeTheme.goalStatement else definition.goal
        val localizedIntroLine = if (forceScenarioCopy) definition.initialMessage else if (isJapaneseLocale) activeTheme.introLine else ""
        val localizedFarewellTranslation = if (forceScenarioCopy) definition.description else if (isJapaneseLocale) closingCue.translation else definition.description
        val localizedFarewellExplanation = if (forceScenarioCopy) definition.goal else if (isJapaneseLocale) closingCue.explanation else definition.goal
        val scenarioDetails = "場所: ${definition.situation} / 役割: ${definition.aiRole} / 目的: ${definition.goal}"
        val systemPrompt = buildModeAwareSystemPrompt(scenarioDetails)
        val strippedSceneLabel = when (currentMode) {
            RoleplayMode.DOJO -> localizedThemeTitle.substringAfter(' ').substringAfter('、').substringAfter('。').takeIf { it.isNotBlank() }
                ?: localizedThemeTitle.replace("[道場]", "").trim().substringAfter(' ').trim()
            else -> localizedThemeTitle
        }

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = systemPrompt,
            messages = emptyList(),
            isLoading = scriptedRuntime == null,
            isProUser = isProVersion,
            userGender = currentUserGender,
            activeThemeTitle = strippedSceneLabel,
            activeThemeDescription = localizedThemeDescription,
            activeThemePersona = localizedThemePersona,
            activeThemeGoal = localizedThemeGoal,
            activeThemeFlavor = activeTheme.flavor,
            activeThemeIntroLine = localizedIntroLine,
            activeThemeFarewellBisaya = closingCue.bisaya,
            activeThemeFarewellTranslation = localizedFarewellTranslation,
            activeThemeFarewellExplanation = localizedFarewellExplanation,
            activeSceneLabel = strippedSceneLabel,
            activeSceneDescription = localizedThemeDescription,
            activeSceneIntroLine = localizedIntroLine.ifBlank { definition.initialMessage },
            showOptionTutorial = optionTutorialVisible,
            translationDirective = translationDirective.value,
            goalAchieved = false,
            isEndingSession = false,
            roleplayMode = RoleplayMode.DOJO,
            learnerName = resolveUserDisplayName()
        )

        if (scriptedRuntime == null) {
            if (isJapaneseLocale && currentMode != RoleplayMode.DOJO) {
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
            closingGuidance = null,
            starterOptions = scenario.starterOptions.map {
                RoleplayStarterOption(
                    text = it.text,
                    translation = it.translation,
                    tone = it.tone
                )
            }
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
        val currentTurnIndex = _uiState.value.completedTurns
        val option = _uiState.value.options.find { it.id == optionId } ?: run {
            Log.e(
                OPTION_LOG_TAG,
                "selectOption: optionId=$optionId not found. turnIndex=$currentTurnIndex available=${_uiState.value.options.map { it.id }}"
            )
            return
        }
        if (!_uiState.value.isProUser && option.requiresPro) return
        if (_uiState.value.isLoading && scriptedRuntime == null) return

        val optionSource = optionSourceById[option.id] ?: OptionSource.UNKNOWN
        val resolvedTranslation = option.hint.orEmpty()
        Log.d(
            OPTION_LOG_TAG,
            "selectOption id=${option.id}, source=$optionSource, turnIndex=$currentTurnIndex, text='${option.text}', translation='${resolvedTranslation}'"
        )

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = option.text,
            isUser = true,
            translation = option.hint
        )
        history.add(MissionHistoryMessage(option.text, isUser = true))
        processUserUtteranceForNameUnlock(option.text)
        registerSanpoTurn()
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = false,
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

        optionSourceById.clear()
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
        registerSanpoTurn()

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
        registerSanpoTurn()
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

        val turnIndex = _uiState.value.completedTurns
        registerOptionSources(options, OptionSource.TEMPLATE, turnIndex)

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
        val sanitizedAiTranslation = sanitizeSystemTags(
            payload.aiTranslation
                .replace("[FINISH]", "")
                .replace("[COMPLETE]", "")
                .trim()
        )
        turnCount++
        val isSanpoMode = currentMode == RoleplayMode.SANPO
        val isDojoMode = currentMode == RoleplayMode.DOJO
        val goalCleared = isDojoMode && payload.goalAchieved
        val sanpoTurnLimitReached = isSanpoMode && turnCount >= 12
        val finishTagFound = rawAiSpeech.contains("[FINISH]") || completionTagDetected

        var finalAiSpeech = if (sanpoTurnLimitReached) {
            "今日はたくさん話したね、また明日！"
        } else {
            sanitizedAiSpeech
        }
        var finalAiTranslation = if (sanpoTurnLimitReached) {
            "今日はたくさん話したね、また明日！"
        } else {
            sanitizedAiTranslation
        }

        val learnerNameForStrip = _uiState.value.learnerName.orEmpty()
        val shouldStripNames = isDojoMode && learnerNameForStrip.isNotBlank()

        val cleanedPayloadOptions = payload.options.map { opt ->
            val sanitizedText = sanitizeSystemTags(opt.text)
            val sanitizedTranslation = sanitizeSystemTags(opt.translation)
            if (shouldStripNames) {
                opt.copy(
                    text = stripName(sanitizedText, learnerNameForStrip),
                    translation = stripName(sanitizedTranslation, learnerNameForStrip)
                )
            } else {
                opt.copy(text = sanitizedText, translation = sanitizedTranslation)
            }
        }

        if (goalCleared) {
            val forcedEnding = buildDojoForcedEndingLines(_uiState.value.currentScenario)
            finalAiSpeech = forcedEnding.first
            finalAiTranslation = forcedEnding.second
        }
        if (shouldStripNames) {
            finalAiSpeech = stripName(finalAiSpeech, learnerNameForStrip)
            finalAiTranslation = stripName(finalAiTranslation, learnerNameForStrip)
        }

        history.add(MissionHistoryMessage(finalAiSpeech, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = finalAiSpeech,
            isUser = false,
            translation = finalAiTranslation.takeIf { it.isNotBlank() },
            voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
        )

        val userTurnIndex = _uiState.value.completedTurns
        val (computedOptions, computedSource) = if (isDojoMode) {
            val dojoOptions = enforceVisibleOptionCount(
                buildDojoOptionsForTurn(userTurnIndex),
                allowFallback = false
            )
            val source = if (userTurnIndex == 0) OptionSource.STARTER else OptionSource.TEMPLATE
            dojoOptions to source
        } else {
            val exposeHintForLiteDebug = BuildConfig.DEBUG && BuildConfig.IS_LITE_BUILD
            val sanpoOptions = cleanedPayloadOptions
                .filter { it.text.isNotBlank() }
                .map {
                    RoleplayOption(
                        text = it.text,
                        hint = if (isProVersion || exposeHintForLiteDebug) it.translation else null,
                        tone = it.tone
                    )
                }
                .filterForAccess()
                .let { ensured -> if (ensured.isNotEmpty()) ensured else buildFallbackOptions() }
                .let { maybeInjectFarewellOption(it, _uiState.value.completedTurns) }
                .let { enforceVisibleOptionCount(it) }
            sanpoOptions to OptionSource.AI
        }
        val sanpoFarewellDetected = isSanpoMode && (containsFarewellCue(finalAiSpeech) || containsFarewellCue(finalAiTranslation))
        val sanpoTopLinkEnding = isSanpoMode && rawAiSpeech.contains("[TOPページへ]")
        val sanpoFinishDetected = isSanpoMode && (finishTagFound || sanpoTurnLimitReached)

        val forcedTopToken = isDojoMode && finalAiSpeech.contains("[TOPページへ]")
        val shouldEndByMode = when {
            isSanpoMode -> sanpoFinishDetected || sanpoFarewellDetected || sanpoTopLinkEnding
            else -> goalCleared || forcedTopToken
        }

        val sanitizedOptions = when {
            goalCleared -> emptyList()
            shouldEndByMode -> emptyList()
            else -> computedOptions
        }

        val aiClosedConversation = shouldEndByMode || sanitizedOptions.isEmpty()

        if (sanpoFinishDetected) {
            cancelVoiceRecording()
        }

        registerOptionSources(
            options = sanitizedOptions,
            source = if (sanitizedOptions.isNotEmpty()) computedSource else null,
            turnIndex = userTurnIndex
        )

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = sanitizedOptions,
                peekedHintOptionIds = emptySet(),
                lockedOption = null,
                goalAchieved = goalCleared,
                isEndingSession = shouldEndByMode || (isSanpoMode && sanitizedOptions.isEmpty()),
                finalFarewellMessageId = if (aiClosedConversation) aiMsg.id else null,
                pendingExitHistory = if (aiClosedConversation) it.pendingExitHistory else null
            )
        }

        if (isDojoMode) {
            when {
                goalCleared -> handleSessionEnd(isMissionComplete = true)
                forcedTopToken && !goalCleared -> handleSessionEnd(isMissionComplete = false)
            }
        }

        if (aiClosedConversation) {
            val emitFarewellMessage = when {
                isSanpoMode -> !(sanpoFinishDetected || sanpoFarewellDetected || sanpoTopLinkEnding)
                else -> false
            }
            val shouldComplete = when {
                isSanpoMode -> shouldEndByMode || sanitizedOptions.isEmpty()
                else -> shouldEndByMode
            }
            if (shouldComplete) {
                queueCompletion(
                    emitFarewellMessage = emitFarewellMessage,
                    closingReference = if (sanpoFinishDetected) aiMsg else null
                )
            }
        }
    }

    private fun buildFallbackOptions(): List<RoleplayOption> {
        val picks = GENERIC_FALLBACK_HINTS.shuffled(random).take(3)
        return picks.map { phrase ->
            RoleplayOption(
                text = phrase.nativeText,
                hint = phrase.translation
            )
        }
    }

    private fun enforceVisibleOptionCount(
        options: List<RoleplayOption>,
        allowFallback: Boolean = true
    ): List<RoleplayOption> {
        val trimmed = options.take(MAX_VISIBLE_OPTIONS)
        if (trimmed.size >= MIN_VISIBLE_OPTIONS) {
            return trimmed
        }
        if (!allowFallback) {
            return trimmed
        }
        val needed = MIN_VISIBLE_OPTIONS - trimmed.size
        val fallback = buildFallbackOptions()
            .filterNot { candidate -> trimmed.any { it.text == candidate.text } }
            .take(needed)
        return (trimmed + fallback).take(MAX_VISIBLE_OPTIONS)
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

    private fun buildDojoOptionsForTurn(turnIndex: Int): List<RoleplayOption> {
        val currentScenario = _uiState.value.currentScenario
        val learnerName = _uiState.value.learnerName.orEmpty()
        val shouldStripNames = learnerName.isNotBlank()

        val starterOptions = if (turnIndex == 0) {
            (currentScenario?.starterOptions?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_DOJO_STARTER_FALLBACKS)
        } else emptyList()

        val progressIndex = (turnIndex - 1).coerceIn(0, DOJO_PROGRESS_OPTION_SETS.lastIndex)
        return when {
            turnIndex == 0 -> starterOptions.mapNotNull { starter ->
                val rawText = sanitizeSystemTags(starter.text)
                val rawTranslation = sanitizeSystemTags(starter.translation)
                if (rawText.isBlank()) {
                    null
                } else {
                    val finalText = if (shouldStripNames) stripName(rawText, learnerName) else rawText
                    val finalTranslation = if (shouldStripNames) stripName(rawTranslation, learnerName) else rawTranslation
                    RoleplayOption(text = finalText, hint = finalTranslation)
                }
            }

            turnIndex in 1..DOJO_PROGRESS_OPTION_SETS.size -> {
                DOJO_PROGRESS_OPTION_SETS[progressIndex].mapNotNull { template ->
                    val rawText = sanitizeSystemTags(template.text)
                    val rawTranslation = sanitizeSystemTags(template.translation)
                    if (rawText.isBlank()) {
                        null
                    } else {
                        val finalText = if (shouldStripNames) stripName(rawText, learnerName) else rawText
                        val finalTranslation = if (shouldStripNames) stripName(rawTranslation, learnerName) else rawTranslation
                        RoleplayOption(text = finalText, hint = finalTranslation)
                    }
                }
            }

            else -> emptyList()
        }
    }

    private fun registerOptionSources(
        options: List<RoleplayOption>,
        source: OptionSource?,
        turnIndex: Int
    ) {
        optionSourceById.clear()
        if (source == null) return
        options.forEach { option ->
            optionSourceById[option.id] = source
        }
        Log.d(
            OPTION_LOG_TAG,
            "registerOptionSources turnIndex=$turnIndex source=$source ids=${options.map { it.id }}"
        )
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
        if (currentMode != RoleplayMode.DOJO) return null
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

    private fun stripName(text: String?, name: String): String {
        if (text.isNullOrBlank() || name.isBlank()) return text.orEmpty()
        return text
            .replace(name, "", ignoreCase = true)
            .replace("$name!", "", ignoreCase = true)
            .replace("$name,", "", ignoreCase = true)
            .replace(" $name", "", ignoreCase = true)
            .replace("$name?", "", ignoreCase = true)
            .trim()
    }

    private fun processUserUtteranceForNameUnlock(message: String) {
        if (currentRelationshipMode == RelationshipMode.INTIMATE_KNOWN || isLearnerIdentified) return
        val extracted = extractLearnerName(message)
        if (!extracted.isNullOrBlank()) {
            knownLearnerName = extracted
            isLearnerIdentified = true
            refreshLearnerNameState()
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
        val primary = trimmed?.split(Regex("[、,。！!？?]"))?.orEmpty()?.firstOrNull()?.trim()
        return primary?.takeIf { it.length in 2..32 }
    }

    private fun refreshLearnerNameState() {
        _uiState.update { it.copy(learnerName = resolveUserDisplayName()) }
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
        if (currentMode == RoleplayMode.SANPO) {
            val farewellLine = SANPO_FAREWELL_LINES.random(random)
            val farewellText = farewellLine.bisaya
            val farewellTranslation = when (translationLanguageState.value) {
                TranslationLanguage.JAPANESE -> farewellLine.translationJa
                TranslationLanguage.ENGLISH -> farewellLine.translationEn
            }
            val farewellMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = farewellText,
                isUser = false,
                translation = farewellTranslation,
                voiceCue = GeminiVoiceCue.ROLEPLAY_NOVA_CUTE
            )
            history.add(MissionHistoryMessage(farewellText, isUser = false))
            val historySnapshot = history.toList()
            pendingAutoExitHistory = historySnapshot
            _uiState.update {
                it.copy(
                    showCompletionDialog = false,
                    messages = it.messages + farewellMessage,
                    options = emptyList(),
                    peekedHintOptionIds = emptySet(),
                    isEndingSession = true,
                    isSanpoEnding = true,
                    sanpoEndingFarewell = farewellText,
                    sanpoEndingFarewellTranslation = farewellTranslation,
                    finalFarewellMessageId = farewellMessage.id,
                    pendingExitHistory = historySnapshot,
                    finalMessage = farewellText,
                    showFeedbackCard = false
                )
            }
            return
        }

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
                pendingExitHistory = historySnapshot,
                isSanpoEnding = false,
                sanpoEndingFarewell = "",
                sanpoEndingFarewellTranslation = ""
            )
        }
        scheduleAutoExit()
    }

    private fun buildRoleplayPrompt(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ): String {
        return when (currentMode) {
            RoleplayMode.SANPO -> buildSanpoPrompt(userMessage)
            RoleplayMode.DOJO -> buildDojoPrompt(scenario, userMessage)
        }
    }

    private fun buildSanpoSystemPrompt(): String {
        val base = sanpoPromptProvider.baseSystemPrompt(resolveUserDisplayName())
        val nicknameInstruction = buildSanpoNicknameInstruction()
        return if (nicknameInstruction.isNullOrBlank()) base else "$base\n\n$nicknameInstruction"
    }

    private fun buildSanpoNicknameInstruction(): String? {
        if (!isProVersion) return null
        val nickname = userNickname?.takeIf { it.isNotBlank() } ?: return null
        val sanitizedNickname = sanitizeNicknameForPrompt(nickname) ?: return null
        if (!shouldNudgeNicknameThisTurn()) return null
        return "- 約25%の頻度で、そのターンに限りビサヤ語のセリフ内に一度だけニックネーム「$sanitizedNickname」を自然に入れてもよい。ただし連続使用や不自然な挿入は禁止。"
    }

    private fun shouldNudgeNicknameThisTurn(): Boolean = random.nextInt(4) == 0

    private fun sanitizeNicknameForPrompt(raw: String): String? {
        val cleaned = raw.replace(Regex("[\n\r]+"), " ").trim()
        return cleaned.takeIf { it.isNotBlank() }?.take(24)
    }

    private fun generateSanpoSessionId(): String = UUID.randomUUID().toString()

    private fun buildSanpoPrompt(userMessage: String): String {
        val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
        val translationLocaleLabel = if (deviceLanguage == "ja") "Japanese" else "English"
        val translationRules = OpenAiChatRepository.TRANSLATION_FIELD_RULES
            .replace("Japanese", translationLocaleLabel)
            .replace("English", translationLocaleLabel)
            .prependIndent("            ")
        val translationLanguage = translationLanguageState.value
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }
        val latestLearnerMessage = if (userMessage == START_TOKEN) {
            "SYSTEM: Conversation is starting. Open with a playful Cebuano greeting."
        } else userMessage
        val hints = (_uiState.value.currentScenario?.hintPhrases ?: emptyList()).joinToString(separator = "\n") {
            "- ${it.nativeText} (${it.translation})"
        }.ifBlank { "- (none)" }
        val systemPrompt = buildModeAwareSystemPrompt()
        val localizedPrompt = """
            $systemPrompt

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $latestLearnerMessage

            Respond strictly in JSON with exactly these fields and no extras:
            {
              "aiSpeech": "Assistant reply in Bisaya ONLY. Keep it under two lines. Append '[TOPページへ]' only on the forced final turn.",
              "aiTranslation": "$translationLocaleLabel translation of aiSpeech ONLY. NEVER leave this blank.",
              "options": [
                {
                  "text": "Suggested learner reply in Bisaya ONLY (casual tone).",
                  "translation": "$translationLocaleLabel translation of that option ONLY. Must be natural $translationLocaleLabel and non-empty.",
                  "tone": "Short descriptor in $translationLocaleLabel (optional)."
                }
              ]
            }
            - Output 2-3 concise options unless you already ended with [TOPページへ], in which case options must be an empty array.
            - Never include markdown, commentary, or explanations outside the JSON object.
            Translation requirements:
$translationRules
        """.trimIndent()
        val prompt = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> localizedPrompt
            TranslationLanguage.ENGLISH -> "Translate to English:\n$localizedPrompt"
        }
        Log.d("RoleplayChatViewModel", "Prompt generated lang=$translationLanguage userMsg='${userMessage.take(40)}'")
        if (BuildConfig.DEBUG) {
            Log.d("RoleplayChatViewModel", "Final prompt JSON for SANPO:\n$prompt")
        }
        return prompt
    }

    private fun buildDojoPrompt(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ): String {
        val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
        val translationLocaleLabel = if (deviceLanguage == "ja") "Japanese" else "English"
        val translationRules = OpenAiChatRepository.TRANSLATION_FIELD_RULES
            .replace("Japanese", translationLocaleLabel)
            .replace("English", translationLocaleLabel)
            .prependIndent("            ")
        val translationLanguage = translationLanguageState.value
        val missionGoal = scenario.goal.ifBlank { scenario.description.ifBlank { "Learner must complete the assigned task." } }
        val scenarioDetails = "状況: ${scenario.situation.ifBlank { scenario.description }} / 役割: ${scenario.aiRole} / 目的: $missionGoal"
        val systemPrompt = buildModeAwareSystemPrompt(scenarioDetails)
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }
        val latestLearnerMessage = if (userMessage == START_TOKEN) {
            "SYSTEM: Conversation is starting. Initialize the mission briefing in Cebuano."
        } else userMessage
        val hints = scenario.hintPhrases.joinToString(separator = "\n") {
            "- ${it.nativeText} (${it.translation})"
        }.ifBlank { "- (none)" }
        val localizedPrompt = """
            $systemPrompt

            【任務データ】
            - シチュエーション: ${scenario.situation.ifBlank { scenario.description }}
            - Tariの役割: ${scenario.aiRole}
            - ゴール: $missionGoal
            - Learnerは初対面の修行者。名前やニックネームを絶対に呼ぶな。
            - 会話は100%ビサヤ語。翻訳は別フィールドのみ。

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $latestLearnerMessage

            Respond strictly in JSON with exactly these fields and no extras:
            {
              "aiSpeech": "Assistant reply in Bisaya ONLY. Append ' [FINISH]' only after the goal is undeniably complete.",
              "aiTranslation": "$translationLocaleLabel translation of aiSpeech ONLY. This must be non-empty $translationLocaleLabel text.",
              "options": [
                {
                  "text": "Concrete next action in Bisaya ONLY (no learner names).",
                  "translation": "$translationLocaleLabel translation of that option ONLY. Never leave this blank and keep it natural.",
                  "tone": "Short descriptor in $translationLocaleLabel (optional)."
                }
              ]
            }
            - Output 2-3 decisive options unless you already ended with [FINISH], in which case options must be an empty array.
            - Never include markdown, commentary, or explanations outside the JSON object.
            Translation requirements:
$translationRules
        """.trimIndent()
        val prompt = when (translationLanguage) {
            TranslationLanguage.JAPANESE -> localizedPrompt
            TranslationLanguage.ENGLISH -> "Translate to English:\n$localizedPrompt"
        }
        Log.d("RoleplayChatViewModel", "Prompt generated lang=$translationLanguage userMsg='${userMessage.take(40)}'")
        if (BuildConfig.DEBUG) {
            Log.d("RoleplayChatViewModel", "Final prompt JSON for DOJO:\n$prompt")
        }
        return prompt
    }

    private fun parseRoleplayPayload(raw: String): RoleplayAiResponsePayload {
        return try {
            val json = JSONObject(extractJsonObject(raw))
            val aiSpeech = json.optString("aiSpeech").ifBlank { json.optString("aiResponse", raw) }
            val aiTranslation = json.optString("aiTranslation").ifBlank { json.optString("translation", "") }
            val goalAchieved = json.optBoolean("goalAchieved", false)
            val rawOptions = json.optJSONArray("options")
            val options = buildList<RoleplayAiOption>(rawOptions?.length() ?: 0) {
                if (rawOptions != null) {
                    for (i in 0 until rawOptions.length()) {
                        val option = rawOptions.optJSONObject(i) ?: continue
                        add(
                            RoleplayAiOption(
                                text = option.optString("text"),
                                translation = option.optString("translation"),
                                tone = option.optString("tone")
                            )
                        )
                    }
                }
            }
            RoleplayAiResponsePayload(
                aiSpeech = aiSpeech,
                aiTranslation = aiTranslation,
                goalAchieved = goalAchieved,
                options = options
            )
        } catch (_: Exception) {
            RoleplayAiResponsePayload(
                aiSpeech = raw,
                aiTranslation = "",
                goalAchieved = false,
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
        val goalAchieved: Boolean,
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

    private fun buildDojoForcedEndingLines(scenario: RoleplayScenarioDefinition?): Pair<String, String> {
        val role = scenario?.aiRole.orEmpty()
        val line1Bisaya = when {
            role.contains("地主") || role.contains("land", ignoreCase = true) ->
                "Sige, pasaylo na tika—makaagi na ka dinhi."
            role.contains("仕立") || role.contains("tailor", ignoreCase = true) ->
                "Sige, dawaton nako imong hangyo—humanon nako sa gisabot nga petsa."
            else ->
                "Maayo—nahuman nimo ang buluhaton."
        }
        val line2Bisaya = "Klaro na ang disiplina—padayon ta sunod higayon."
        val bisayaSpeech = listOf(line1Bisaya, line2Bisaya, "[TOPページへ]").joinToString("\n")

        val translation = when (translationLanguageState.value) {
            TranslationLanguage.JAPANESE -> {
                val line1Ja = when {
                    role.contains("地主") || role.contains("land", ignoreCase = true) ->
                        "よし、許そう。ここを通ってよい。"
                    role.contains("仕立") || role.contains("tailor", ignoreCase = true) ->
                        "よし、要望は受けた。約束の期日で仕上げる。"
                    else ->
                        "よし、任務完了だ。"
                }
                val line2Ja = "修行は形になった。また挑戦せよ。"
                listOf(line1Ja, line2Ja, "[TOPページへ]").joinToString("\n")
            }
            TranslationLanguage.ENGLISH -> {
                val line1En = when {
                    role.contains("land", ignoreCase = true) || role.contains("地主") ->
                        "Alright, I grant it—you may pass."
                    role.contains("tailor", ignoreCase = true) || role.contains("仕立") ->
                        "Deal accepted—I’ll finish it by the promised date."
                    else ->
                        "Good—you completed the mission."
                }
                val line2En = "Discipline is in you; continue next time."
                listOf(line1En, line2En, "[TOPページへ]").joinToString("\n")
            }
        }

        return bisayaSpeech to translation
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
