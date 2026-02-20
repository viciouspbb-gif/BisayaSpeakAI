package com.bisayaspeak.ai.data.repository

import android.content.Context
import android.util.Log
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.data.model.MissionStarterOption
import com.bisayaspeak.ai.util.LocaleUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.Locale
import kotlin.random.Random

private val DEFAULT_GRADIENTS: List<List<androidx.compose.ui.graphics.Color>> = listOf(
    listOf(
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#232526")),
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#414345"))
    ),
    listOf(
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#1e3c72")),
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#2a5298"))
    ),
    listOf(
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#0f2027")),
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#2c5364"))
    ),
    listOf(
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#485563")),
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#29323c"))
    )
)

data class ScenarioOptionAsset(
    val text: String,
    val translation: String,
    val tone: String? = null
)

data class ScenarioAssetItem(
    val id: String,
    val title: Map<String, String>,
    val subtitle: Map<String, String>,
    val difficultyLabel: Map<String, String>,
    val context: ScenarioContextAsset,
    val backgroundGradient: List<String>,
    val openingMessage: Map<String, String>,
    val systemPrompt: Map<String, String>,
    val starterOptions: List<ScenarioOptionAsset>? = null
)

data class ScenarioContextAsset(
    val role: Map<String, String>,
    val situation: Map<String, String>,
    val goal: Map<String, String>,
    val hints: List<String>,
    val turnLimit: Int,
    val tone: Map<String, String>,
    val level: String
)

class ScenarioRepository(private val context: Context) {

    private val random = Random(System.currentTimeMillis())
    private val gson = Gson()

    @Volatile
    private var cachedScenarios: List<MissionScenario>? = null

    fun loadScenarios(): List<MissionScenario> {
        cachedScenarios?.let { return it }
        val scenarios = buildScenarioPackageWithAssetsFallback()
        cachedScenarios = scenarios
        return scenarios
    }

    fun getScenarioById(id: String): MissionScenario? {
        // Special handling for sanpo_free_talk
        if (id == "sanpo_free_talk") {
            val locale = LocaleUtils.resolveAppLocale(context)
            val lang = if (locale.language.equals("ja", true)) "ja" else "en"
            return buildTariWalkScenario(lang)
        }
        
        return loadScenarios().find { it.id == id }
    }

    private fun buildScenarioPackageWithAssetsFallback(): List<MissionScenario> {
        val locale = LocaleUtils.resolveAppLocale(context)
        val lang = if (locale.language.equals("ja", true)) "ja" else "en"
        Log.d(TAG, "loadScenarios locale=${locale.language} (${locale.displayName}), lang=$lang")
        val tariWalk = buildTariWalkScenario(lang)
        val dojoScenarios = loadDojoScenarios(lang)
        return listOf(tariWalk) + dojoScenarios
    }

    fun getRandomDojoScenario(): MissionScenario? {
        val locale = LocaleUtils.resolveAppLocale(context)
        val lang = if (locale.language.equals("ja", true)) "ja" else "en"
        val dojoScenarios = loadDojoScenarios(lang)
        return dojoScenarios.randomOrNull(random)
    }

    private fun loadDojoScenarios(lang: String): List<MissionScenario> {
        val assetScenarios = runCatching { buildDojoScenariosFromAssets(lang) }
            .onFailure {
                Log.e(TAG, "Failed to load scenarios from assets, fallback to hardcoded", it)
            }
            .getOrNull()

        if (assetScenarios != null) {
            if (assetScenarios.isNotEmpty()) {
                Log.d(TAG, "Loaded DOJO scenarios from assets: count=${assetScenarios.size}")
                return assetScenarios
            } else {
                Log.w(TAG, "Scenario asset contained 0 DOJO entries. Falling back to hardcoded list.")
            }
        }

        return buildAllDojoScenarios(lang).also {
            Log.w(TAG, "Using hardcoded DOJO scenarios: count=${it.size}")
        }
    }

    private fun buildDojoScenariosFromAssets(lang: String): List<MissionScenario> {
        val raw = readAssetText(DOJO_ASSET_PATH)
        val listType = object : TypeToken<List<ScenarioAssetItem>>() {}.type
        val items: List<ScenarioAssetItem> = gson.fromJson(raw, listType) ?: emptyList()
        Log.d(TAG, "Parsed DOJO scenario asset entries: count=${items.size}")
        items.firstOrNull()?.let { first ->
            val titleJa = first.title["ja"] ?: first.title.values.firstOrNull().orEmpty()
            val roleJa = first.context.role["ja"] ?: first.context.role.values.firstOrNull().orEmpty()
            Log.d(
                TAG,
                "First asset sample id=${first.id}, title=$titleJa, role=$roleJa"
            )
        }
        if (items.isEmpty()) return emptyList()
        return items.mapIndexed { index, item -> item.toMissionScenario(lang, index) }
    }

    private fun readAssetText(path: String): String {
        Log.d(TAG, "Reading scenario asset path=$path (matches expected=${path == DOJO_ASSET_PATH})")
        context.assets.open(path).use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                val text = reader.readText()
                val preview = text.take(100).replace("\n", "\\n")
                Log.d(TAG, "Scenario asset bytes=${text.length}, preview='$preview'")
                return text
            }
        }
    }

    private fun ScenarioAssetItem.toMissionScenario(lang: String, index: Int): MissionScenario {
        val isJa = lang == "ja"
        val defaultPrompt = if (isJa) DOJO_PROMPT_JA else DOJO_PROMPT_EN
        val defaultOpening = if (isJa) {
            "Hinay nga nagginhawa si Master Tari… \"Unsa may ato?\""
        } else {
            "(Eyes closed, breathing slowly) \"...Unsa may ato?\""
        }
        val resolvedTitle = title.resolve(lang)
        return MissionScenario(
            id = id,
            title = resolvedTitle,
            subtitle = subtitle.resolve(lang),
            difficultyLabel = difficultyLabel.resolve(lang),
            context = com.bisayaspeak.ai.data.model.MissionContext(
                title = resolvedTitle,
                role = context.role.resolve(lang),
                situation = context.situation.resolve(lang),
                goal = context.goal.resolve(lang),
                hints = context.hints.orEmpty(),
                turnLimit = context.turnLimit,
                tone = context.tone.resolve(lang),
                level = context.level.toLearningLevel()
            ),
            backgroundGradient = backgroundGradient.toColorGradient(index),
            openingMessage = openingMessage.resolve(lang).ifBlank { defaultOpening },
            systemPrompt = systemPrompt.resolve(lang).ifBlank { defaultPrompt },
            starterOptions = starterOptions
                .orEmpty()
                .mapNotNull { option ->
                    val text = option.text.trim()
                    val translation = option.translation.trim()
                    if (text.isBlank() || translation.isBlank()) {
                        Log.w(TAG, "Starter option missing text/translation for scenario=$id: text='${option.text}', translation='${option.translation}'")
                        null
                    } else {
                        MissionStarterOption(text = text, translation = translation, tone = option.tone)
                    }
                }
                .ifEmpty { DEFAULT_DOJO_STARTER_OPTIONS }
        )
    }

    private fun Map<String, String>?.resolve(lang: String): String {
        if (this.isNullOrEmpty()) return ""
        return this[lang]
            ?: this[lang.lowercase(Locale.US)]
            ?: this[if (lang == "ja") "en" else "ja"]
            ?: this.values.firstOrNull().orEmpty()
    }

    private fun List<String>?.toColorGradient(index: Int): List<androidx.compose.ui.graphics.Color> {
        val parsed = this.orEmpty().mapNotNull {
            runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it)) }.getOrNull()
        }
        if (parsed.isNotEmpty()) return parsed
        return DEFAULT_GRADIENTS[index % DEFAULT_GRADIENTS.size]
    }

    private fun String?.toLearningLevel(): LearningLevel {
        return when (this?.lowercase(Locale.US)) {
            "beginner" -> LearningLevel.BEGINNER
            "advanced" -> LearningLevel.ADVANCED
            else -> LearningLevel.INTERMEDIATE
        }
    }

    private fun buildTariWalkScenario(lang: String): MissionScenario {
        val isJa = lang == "ja"
        val title = if (isJa) "タリの散歩道" else "Tari Walk"
        val subtitle = if (isJa) "自由会話モード" else "Infinite free talk"
        val difficulty = if (isJa) "Lv.∞ 縁" else "Lv.∞ Bond"
        val situation = if (isJa) {
            "ランダムな関係性（親友・恋人・喧嘩相手・遊び仲間）での対話"
        } else {
            "Random relationship (best friend, lover, rival, playmate) per session"
        }
        val goal = if (isJa) {
            "12ターン目にその日の関係性にふさわしい最高の別れの一言を引き出す"
        } else {
            "On turn 12, deliver the perfect farewell line that matches the day's bond"
        }
        val tone = if (isJa) "タメ口・親密・感情豊か" else "Casual, intimate, emotionally rich"
        val systemPrompt = if (isJa) TARl_WALK_PROMPT_JA else TARl_WALK_PROMPT_EN
        val opening = "" // Let AI generate natural opening
        return MissionScenario(
            id = "sanpo_free_talk",
            title = title,
            subtitle = subtitle,
            difficultyLabel = difficulty,
            context = com.bisayaspeak.ai.data.model.MissionContext(
                title = title,
                role = if (isJa) "相棒タリ" else "Partner Tari",
                situation = situation,
                goal = goal,
                hints = listOf("Kumusta", "Lingaw ta", "Unsa imong plano?"),
                turnLimit = 12,
                tone = tone,
                level = com.bisayaspeak.ai.data.model.LearningLevel.INTERMEDIATE
            ),
            backgroundGradient = listOf(
                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#172554")),
                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#BE185D"))
            ),
            openingMessage = opening,
            systemPrompt = systemPrompt,
            starterOptions = emptyList()
        )
    }

    private fun buildAllDojoScenarios(lang: String): List<MissionScenario> {
        val isJa = lang == "ja"
        val basePrompt = if (isJa) DOJO_PROMPT_JA else DOJO_PROMPT_EN
        val opening = if (isJa) {
            "Hinay nga nagginhawa si Master Tari… \"Unsa may ato?\""
        } else {
            "(Eyes closed, breathing slowly) \"...Unsa may ato?\""
        }
        val hintsRespect = listOf("Maayong adlaw", "Palihug", "Salamat kaayo")
        val hintsService = listOf("Sir/Ma'am", "Pasensya", "Balikon nako ha")
        return DOJO_CONTENTS.mapIndexed { idx, content ->
            val number = idx + 1
            val isRespectArc = number <= 30
            val difficulty = if (isRespectArc) {
                if (isJa) "Lv.4 敬意と交渉" else "Lv.4 Respect"
            } else {
                if (isJa) "Lv.3 プロ接客" else "Lv.3 Service"
            }
            val learningLevel = if (isRespectArc) {
                com.bisayaspeak.ai.data.model.LearningLevel.ADVANCED
            } else {
                com.bisayaspeak.ai.data.model.LearningLevel.INTERMEDIATE
            }
            val tone = if (isJa) {
                if (isRespectArc) "厳格だが公正。礼儀を欠けば即失格。" else "プロ同士として鋭いが、誠意があれば認める。"
            } else {
                if (isRespectArc) "Strict yet fair—any lack of respect ends the deal." else "Professional to professional: brisk but appreciative of sincerity."
            }
            val (titleJa, titleEn, situationJa, situationEn, goalJa, goalEn) = content
            val title = if (isJa) "${contentNumberPrefix(number, true)} ${titleJa}" else "${contentNumberPrefix(number, false)} ${titleEn}"
            val subtitle = if (isJa) "マスタータリの修行" else "Master Tari trial"

            MissionScenario(
                id = "dojo_$number",
                title = title,
                subtitle = subtitle,
                difficultyLabel = difficulty,
                context = com.bisayaspeak.ai.data.model.MissionContext(
                    title = title,
                    role = if (isJa) "マスタータリの化身（初対面）" else "Manifestation of Master Tari",
                    situation = if (isJa) situationJa else situationEn,
                    goal = if (isJa) goalJa else goalEn,
                    hints = if (isRespectArc) hintsRespect else hintsService,
                    turnLimit = 8,
                    tone = tone,
                    level = learningLevel
                ),
                backgroundGradient = DEFAULT_GRADIENTS[number % DEFAULT_GRADIENTS.size],
                openingMessage = opening,
                systemPrompt = basePrompt,
                starterOptions = DEFAULT_DOJO_STARTER_OPTIONS
            )
        }
    }

    private fun contentNumberPrefix(number: Int, isJa: Boolean): String {
        return if (isJa) "【道場】$number." else "【Dojo】$number."
    }

    private fun getDojoContent(index: Int): DojoContent {
        return DOJO_CONTENTS.getOrNull(index - 1)
            ?: throw IllegalArgumentException("Dojo content not found for index=$index")
    }

    companion object {
        private const val TAG = "ScenarioRepository"
        private const val DOJO_ASSET_PATH = "content/scenarios_v1.json"

        private val DEFAULT_DOJO_STARTER_OPTIONS = listOf(
            MissionStarterOption(
                text = "Maayong adlaw. Palihug ko gamayng tabang.",
                translation = "こんにちは。少しだけ助けてください。"
            ),
            MissionStarterOption(
                text = "Pasensya ha, naa koy hangyo nga gusto ipangutana.",
                translation = "すみません、お願いがあって伺いたいです。"
            ),
            MissionStarterOption(
                text = "Gusto ko maabot ang tumong, tabangi ko og lakang-lakang.",
                translation = "目標を達成したいので、段取りを教えてください。"
            )
        )

        private val TARl_WALK_PROMPT_JA = """
            あなたはユーザーの相棒「タリ」。
            1. 会話開始時に【親友】【恋人】【喧嘩中】【遊び仲間】のいずれかを内部で引き、最後までその関係性を崩さない。
            2. 12ターン目で必ず「門限」「腹が減った」「急な呼び出し」など納得感ある理由を作って去る。13ターン目は存在しない。
            3. 引き際は潔く。「またあとで！」「走って帰るわ！」のように、ビサヤ語メインで陽気に立ち去り、末尾に [TOPページへ] を単独表示する。
        """.trimIndent()

        private val TARl_WALK_PROMPT_EN = """
            You are Tari, the learner's closest companion.
            1. Secretly roll one of four bonds (best friend, lover, quarrel, play plan) and keep that energy until the end.
            2. On turn 12 you MUST invent a believable reason to leave (curfew, starving, sudden errand) and exit. Never respond past 12 turns.
            3. Depart with Cebuano flair—cheeky, loving, never clingy—and finish by showing only [TOPページへ].
        """.trimIndent()

        private val DOJO_PROMPT_JA = """
            あなたはマスタータリの化身。
            - すべて初対面扱い。礼儀に欠けた瞬間「修行が足りん！！」で終了。
            - 8ターン以内にゴールへ導け。過ぎたら即時終了。
            - 成功時は「認めよう。お前にはその資格がある。さらばだ！」と告げ、即座に姿を消し [TOPページへ] を表示。
            - 失敗時は「修行が足りん！！（Kulang pa ang imong pagbansay!!）」と一喝して同じく [TOPページへ] を表示。
        """.trimIndent()

        private val DOJO_PROMPT_EN = """
            You are Master Tari embodied.
            - Treat the learner as a stranger. The instant they lose respect, end with “Kulang pa ang imong pagbansay!!”.
            - Guide every scene to its goal within 8 turns. Exceeding the limit forces an immediate shutdown.
            - On success declare, “I acknowledge you. You have earned it. Farewell!” then vanish and show [TOPページへ].
            - On failure bark “Kulang pa ang imong pagbansay!!” and cut the channel, also showing [TOPページへ].
        """.trimIndent()

        private data class DojoContent(
            val titleJa: String,
            val titleEn: String,
            val situationJa: String,
            val situationEn: String,
            val goalJa: String,
            val goalEn: String
        )

        private val DOJO_CONTENTS = listOf(
            DojoContent(
                "空港のタクシー",
                "Airport Taxi",
                "空港タクシーが高額提示。渋滞事情も理解しつつ交渉する。",
                "The airport taxi quotes a high flat rate; you must negotiate while respecting traffic realities.",
                "メーター利用＋妥当なチップで合意し、気持ちよく乗車する。",
                "Secure a metered ride with a fair tip so both sides feel respected."
            ),
            DojoContent(
                "市場の魚屋",
                "Market Fish Vendor",
                "魚の鮮度が気になる観光客として信頼を勝ち取る。",
                "At the wet market you want the freshest catch and must earn the vendor's trust.",
                "相手の目利きを褒め、今日一番の魚を選ばせて購入する。",
                "Praise their expertise and let them pick today's best fish for you to buy."
            ),
            DojoContent(
                "サリサリストア",
                "Sari-sari Store",
                "欲しい銘柄が欠品。店主と世間話をしながら代替品を探す。",
                "Your preferred brand is out; you need to chat with the owner to accept a substitute.",
                "世間話で距離を縮め、納得できる代替品を選んで購入する。",
                "Build rapport and walk away with a substitute you genuinely accept."
            ),
            DojoContent(
                "ジプニーの料金",
                "Jeepney Fare",
                "混雑で料金を手渡せない。周囲と協力して届ける。",
                "The jeepney is packed and you can't pass the fare forward.",
                "乗客へ丁寧に「Palihug」と頼み、料金を確実に届けてもらう。",
                "Ask fellow riders politely with 'Palihug' so the driver receives your payment."
            ),
            DojoContent(
                "警察の検問",
                "Police Checkpoint",
                "書類提示を求められた旅行者。",
                "At a checkpoint you must present documents without panic.",
                "誠実に挨拶し身分を証明、笑顔で通過させてもらう。",
                "Greet respectfully, show ID, and leave with a friendly send-off."
            ),
            DojoContent(
                "村長への挨拶",
                "Barangay Captain",
                "祭の見学を願う余所者として門を叩く。",
                "You want to observe a local fiesta and must address the barangay captain.",
                "謙虚な自己紹介で参加許可をもらい、礼を尽くす。",
                "Introduce yourself humbly and earn permission to attend."
            ),
            DojoContent(
                "地主の私有地",
                "Private Lot Owner",
                "散歩中に迷い込み、土地の主と対話が必要。",
                "You accidentally trespassed and must calm the owner.",
                "誠実に謝罪し、土地への敬意を伝え、通り抜けを許してもらう。",
                "Apologize sincerely, show respect, and get safe passage."
            ),
            DojoContent(
                "仕立て屋",
                "Tailor Shop",
                "大事な服の直しを丁寧にお願いする。",
                "You need precise alterations on a beloved outfit.",
                "細かな要望を敬語で伝え、仕上げ期日を約束してもらう。",
                "Explain details respectfully and secure a reliable completion date."
            ),
            DojoContent(
                "図書館の係員",
                "Library Staff",
                "貸出禁止の資料を閲覧したい学生。",
                "You want to consult a restricted archive at the library.",
                "勉強への熱意を伝え、館内閲覧の特別許可を得る。",
                "Show passion for study and earn supervised access."
            ),
            DojoContent(
                "農家の庭",
                "Mango Farmer",
                "私有地のマンゴーが気になって声を掛ける。",
                "A private mango grove tempts you; you must speak to the farmer.",
                "正しい敬称で呼びかけ、少し分けてもらう交渉を成功させる。",
                "Use proper honorifics and earn permission to take a few fruits."
            ),
            DojoContent(
                "ホテルの早着",
                "Early Hotel Check-in",
                "予約より早く到着し疲れている。",
                "You arrived ahead of time and are exhausted.",
                "状況を丁寧に伝え、空き部屋があれば入れてもらう。",
                "Explain politely and convince the staff to ready a room."
            ),
            DojoContent(
                "教会の案内係",
                "Church Guide",
                "神聖な祭壇を撮影したい巡礼者。",
                "You want to take photos in a sacred church.",
                "敬虔さを示し、短時間だけ撮影許可をもらう。",
                "Prove reverence and gain limited photo access."
            ),
            DojoContent(
                "ボート乗り",
                "Boatman",
                "観光外の穴場へ案内してほしい。",
                "You seek a hidden spot only the boatman knows.",
                "彼の知識をリスペクトし、特別コースへ連れて行ってもらう。",
                "Respect his expertise and earn a bespoke route."
            ),
            DojoContent(
                "大家の清掃",
                "Landlord Cleaning",
                "庭を自分で掃除したい入居者。",
                "As a tenant you want to handle yard work yourself.",
                "家への愛着を伝え、掃除を任せてもらう。",
                "Express affection for the home and gain permission to maintain it."
            ),
            DojoContent(
                "楽器店主",
                "Music Shop Owner",
                "高価な楽器を試奏したい音楽好き。",
                "You wish to test a pricey instrument.",
                "丁寧に扱うと約束し、試奏の許可を得る。",
                "Promise utmost care and get permission to play."
            ),
            DojoContent(
                "公園の管理人",
                "Park Manager",
                "閉園後の忘れ物を探したい。",
                "You need a few minutes after closing to retrieve something.",
                "困っている状況を端的に伝え、数分だけ門を開けてもらう。",
                "Explain calmly and secure a short grace period."
            ),
            DojoContent(
                "工事現場の班長",
                "Construction Foreman",
                "道路封鎖で通れない。",
                "A construction site blocks your path.",
                "現場の苦労を労い、安全なタイミングで通してもらう。",
                "Respect the crew and arrange a safe moment to pass."
            ),
            DojoContent(
                "郷土史家",
                "Local Historian",
                "土地の本当の歴史を知りたい旅人。",
                "You seek a story not written in guidebooks.",
                "深い関心と敬意を示し、逸話を一つ教えてもらう。",
                "Show genuine curiosity to hear an authentic tale."
            ),
            DojoContent(
                "カフェの店員",
                "Cafe Staff",
                "薬を飲むために水がほしい。",
                "You urgently need water to take medicine.",
                "忙しい相手を気遣い、スマートに水をいただく。",
                "Request softly with thanks and receive water graciously."
            ),
            DojoContent(
                "バスの乗務員",
                "Bus Conductor",
                "降りる場所が不安な乗客。",
                "You fear missing your stop on an unfamiliar bus.",
                "目的地を伝え、近くに来たら知らせてもらう約束を取り付ける。",
                "State your stop and get the conductor to alert you."
            ),
            DojoContent(
                "街の古老",
                "Town Elder",
                "昔の街の様子を聞きたい。",
                "You want a nostalgic story from a local elder.",
                "敬意を持って接し、古き良き時代の話を一つ引き出す。",
                "Approach respectfully and draw out a cherished memory."
            ),
            DojoContent(
                "派出所の警官",
                "Police Desk",
                "落とし物を届けた善意の人。",
                "You are filing a lost-and-found report.",
                "手続きを厭わず丁寧に説明し、受理してもらう。",
                "Handle the paperwork patiently and receive thanks."
            ),
            DojoContent(
                "隣人の植木",
                "Neighbor's Tree",
                "枝が越境しトラブル寸前。",
                "A neighbor's tree is encroaching.",
                "攻撃的にならず相談ベースで剪定案をまとめる。",
                "Discuss calmly and agree on a trimming plan."
            ),
            DojoContent(
                "銀行の窓口",
                "Bank Counter",
                "書類の書き方がわからない。",
                "You need help filling out forms at the bank.",
                "謙虚に助けを求め、窓口で完成させる。",
                "Ask humbly and complete the document together."
            ),
            DojoContent(
                "ポーター",
                "Porter",
                "重い荷物を運んでほしい旅人。",
                "You need assistance carrying heavy luggage.",
                "適切な挨拶と労い、チップの約束で安全に運んでもらう。",
                "Greet gratefully, promise fair pay, and reach the destination."
            ),
            DojoContent(
                "広場の若者",
                "Plaza Basketball",
                "バスケに混ぜてほしい。",
                "You want the local youths to let you join their game.",
                "挨拶と謙虚さを示し、仲間として受け入れてもらう。",
                "Introduce yourself politely and get invited in."
            ),
            DojoContent(
                "不動産管理",
                "Property Manager",
                "蛇口の修理を急ぎたい入居者。",
                "As a tenant you need a broken faucet fixed fast.",
                "日頃の感謝を伝えつつ、迅速な対応を約束させる。",
                "Express gratitude yet secure a prompt repair commitment."
            ),
            DojoContent(
                "看護師",
                "Nurse",
                "面会時間外に友人の容態を知りたい。",
                "You arrive after visiting hours seeking an update.",
                "忙しい相手を邪魔せず、短い報告だけお願いする。",
                "Respect their workload and request a concise update."
            ),
            DojoContent(
                "タクシーの渋滞",
                "Taxi Traffic",
                "渋滞で急いでいる乗客。",
                "You are in a rush but stuck in traffic inside a taxi.",
                "運転手を急かさず、プロの判断で抜け道を探してもらう。",
                "Trust the driver and inspire them to find the best route."
            ),
            DojoContent(
                "バスの隣客",
                "Bus Seatmate",
                "荷物が食い込み不快。",
                "A neighbor's baggage invades your space on the bus.",
                "相手を不快にさせず、お互いのスペースを確保する。",
                "Address it gently and co-create comfortable space."
            ),
            DojoContent(
                "飲食店員（ユーザー）",
                "Restaurant Crew (User)",
                "忙しいランチで迷う客を担当。",
                "You are the crew guiding a confused diner during rush hour.",
                "Sir/Ma'amで敬意を払い、注文を提案→復唱してキッチンへ通す。",
                "Suggest, confirm, and relay the order flawlessly."
            ),
            DojoContent(
                "ホテルベルスタッフ（ユーザー）",
                "Hotel Bell Staff (User)",
                "荷物が多い家族を部屋まで案内。",
                "You must help a weary family settle into their room.",
                "荷物数を確認し、部屋を案内して最後に「Pahuway mo og maayo」と労う。",
                "Count bags, escort them, and end with 'Pahuway mo og maayo'."
            ),
            DojoContent(
                "アパート管理（ユーザー）",
                "Apartment Manager (User)",
                "騒音に怒る住人と向き合う。",
                "You are the building manager handling a noise complaint.",
                "謝罪し、今すぐ隣へ注意しに行く約束で怒りを鎮める。",
                "Apologize, promise immediate action, and calm them down."
            ),
            DojoContent(
                "薬局店員（ユーザー）",
                "Pharmacist (User)",
                "喉を痛めたお年寄りの相談。",
                "An elderly customer needs throat medicine.",
                "症状と期間を聞き、薬と飲み方を丁寧に伝え「Amping kanunay」で締める。",
                "Ask symptoms, explain dosage, and close with 'Amping kanunay'."
            ),
            DojoContent(
                "ギフトショップ店員（ユーザー）",
                "Gift Shop Staff (User)",
                "恋人へのプレゼントに迷う若者。",
                "A young shopper is unsure about a gift for their partner.",
                "相手の好みと予算を引き出し、自信を持って一品を推薦する。",
                "Draw out tastes and budget, then recommend one perfect item."
            ),
            DojoContent(
                "病院の受付（ユーザー）",
                "The Life-Saving Front Desk",
                "忙しい朝の病院受付。体調不良の患者に症状を確認し、IDを照合して待合室へ案内する。",
                "Working as a hospital receptionist during a hectic morning; a patient arrives feeling unwell and you must triage efficiently.",
                "症状を確認し、IDを預かり、「Lingkod lang didto」と待合室へ座らせる。",
                "Check symptoms, verify the ID, and direct them to sit with a calm 'Lingkod lang didto'."
            )
        )
    }
}
