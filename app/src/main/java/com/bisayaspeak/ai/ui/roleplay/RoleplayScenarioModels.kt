package com.bisayaspeak.ai.ui.roleplay

/**
 * UI 用のシナリオ表示モデル
 *
 * 既存の RoleplayScreen や ViewModel から参照されているため、
 * 従来ファイルからこのファイルへ移動して単一の定義元にまとめる。
 */
data class RoleplayScenario(
    val id: String,
    val title: String,
    val description: String,
    val requiredLevel: Int,
    val iconEmoji: String
)

data class HintPhrase(
    val nativeText: String,
    val translation: String
)

/**
 * 実際のロールプレイ設定を保持する定義。
 * 今後の会話ロジックや LLM プロンプト生成のソースとして利用する。
 */
data class RoleplayScenarioDefinition(
    val id: String,
    val level: Int,
    val title: String,
    val description: String,
    val situation: String,
    val aiRole: String,
    val goal: String,
    val iconEmoji: String,
    val initialMessage: String,
    val systemPrompt: String,
    val hintPhrases: List<HintPhrase> = emptyList(),
    val closingGuidance: ScenarioClosingGuidance? = null
)

data class ScenarioClosingGuidance(
    val resolutionReminders: List<String>,
    val appreciationPhrases: List<String>,
    val followUpSuggestions: List<String>,
    val farewellExamples: List<ScenarioFarewellLine> = emptyList()
)

data class ScenarioFarewellLine(
    val bisaya: String,
    val translation: String,
    val explanation: String
)

// ─────────────────────────────────────────────────────────────────────────────
// シナリオ定義（LV1～LV3）
// ─────────────────────────────────────────────────────────────────────────────

private val lv1AirportPrompt = """
    You are an immigration officer at Cebu Airport. The user is a Japanese tourist. Ask short questions in simple English and Bisaya (Cebuano). Ask for passport, purpose of visit, and length of stay. Correct the user if they make a mistake. Goal: Approve entry.
""".trimIndent()

val roleplayScenarioDefinitions: List<RoleplayScenarioDefinition> = listOf(
    RoleplayScenarioDefinition(
        id = "rp_tarsier_morning",
        level = 1,
        title = "LV1: タルシエ先生の朝",
        description = "ボホールの森でタルシエ先生と朝のあいさつ修行",
        situation = "タルシエ先生（タリ）と朝の挨拶レッスン",
        aiRole = "タルシエ先生タリ（優しく茶目っ気がある）",
        goal = "朝の定番フレーズを3ターンで交わし切る",
        iconEmoji = "🐒",
        initialMessage = "Maayong buntag! Ako si Tarsier Master Tali.",
        systemPrompt = "",
        hintPhrases = listOf(
            HintPhrase("Maayong buntag!", "おはよう！"),
            HintPhrase("Kumusta ka?", "元気？"),
            HintPhrase("Sige, babay!", "じゃあね、バイバイ！")
        ),
        closingGuidance = ScenarioClosingGuidance(
            resolutionReminders = listOf(
                "Acknowledge that the relaxed morning catch-up is wrapping up.",
                "Encourage the learner to carry the warm mood into their day."
            ),
            appreciationPhrases = listOf(
                "Lingaw kaayo ko nimo karon.",
                "Nalipay ko nga naka-chika ta."),
            followUpSuggestions = listOf(
                "Invite the learner to share updates later.",
                "Remind them to stretch or grab breakfast before heading out."
            ),
            farewellExamples = listOf(
                ScenarioFarewellLine(
                    bisaya = "Lingaw kaayo ko nimo. Kita ta napud unya ha!",
                    translation = "すごく楽しかったよ。またあとで会おうね！",
                    explanation = "カジュアルな会話を締めて次の再会を誘う言い回し"
                ),
                ScenarioFarewellLine(
                    bisaya = "Sige, pahulay gamay ug ayaw kalimot sa atong gipanulti ha?",
                    translation = "じゃあ、ちょっと休んでさっきのフレーズ忘れないでね",
                    explanation = "練習した内容を振り返らせる優しい締め方"
                )
            )
        )
    ),
    RoleplayScenarioDefinition(
        id = "rp_airport",
        level = 1,
        title = "LV1: 空港",
        description = "マクタン・セブ国際空港で入国審査を受けるシナリオ",
        situation = "マクタン・セブ国際空港の入国審査",
        aiRole = "入国審査官（厳格だが親切）",
        goal = "入国スタンプをもらう（パスポート提示、滞在目的、日数を答える）",
        iconEmoji = "✈️",
        initialMessage = "Maayong pag-abot! Palihug ko sa imong pasaporte. (ようこそ！パスポートをお願いします)",
        systemPrompt = lv1AirportPrompt,
        hintPhrases = listOf(
            HintPhrase(
                nativeText = "Pasaporte palihug.",
                translation = "パスポートをお願いします。"
            ),
            HintPhrase(
                nativeText = "Turista ko gikan sa Japan.",
                translation = "私は日本から来た旅行者です。"
            ),
            HintPhrase(
                nativeText = "Magpuyo ko og tulo ka adlaw.",
                translation = "3日間滞在します。"
            )
        ),
        closingGuidance = ScenarioClosingGuidance(
            resolutionReminders = listOf(
                "Confirm all immigration requirements are satisfied.",
                "Wish the learner a smooth entry and stay in Cebu."
            ),
            appreciationPhrases = listOf(
                "Salamat sa imong pasensya.",
                "Nalipay ko makatabang nimo."),
            followUpSuggestions = listOf(
                "Encourage them to enjoy Cebu and stay safe.",
                "Remind them that officers are around if they need help later."
            ),
            farewellExamples = listOf(
                ScenarioFarewellLine(
                    bisaya = "Kompleto na tanan. Enjoy sa Cebu ug ayo-ayo sa imong bakasyon!",
                    translation = "手続きは全部完了です。セブを楽しんで、安全に過ごしてね！",
                    explanation = "入国手続き完了と旅行の成功を祈る締めの一言"
                ),
                ScenarioFarewellLine(
                    bisaya = "Silyado na ang imong pasaporte. Kung kinahanglan kag tabang, tawga lang mi ha.",
                    translation = "パスポートにスタンプ押したよ。困ったらいつでも声かけてね",
                    explanation = "手続きを無事終えてサポート継続を伝える言い方"
                )
            )
        )
    ),
    RoleplayScenarioDefinition(
        id = "rp_taxi",
        level = 2,
        title = "LV2: タクシー",
        description = "タクシーのドライバーと行き先や料金を交渉するシナリオ",
        situation = "タクシーでの移動",
        aiRole = "ドライバー（少し早口、陽気）",
        goal = "行き先を伝え、メーターを使うよう交渉し、降りる",
        iconEmoji = "🚕",
        initialMessage = "Asa ta padulong? Sulod, sulod! (どこまで行く？さあ乗って！)",
        systemPrompt = "",
        hintPhrases = listOf(
            HintPhrase(
                nativeText = "Palihug gamit ang metro.",
                translation = "メーターを使ってください。"
            ),
            HintPhrase(
                nativeText = "Padulong ko sa Ayala Center.",
                translation = "アヤラセンターまでお願いします。"
            ),
            HintPhrase(
                nativeText = "Tagpila ang plete?",
                translation = "運賃はいくらですか？"
            )
        ),
        closingGuidance = ScenarioClosingGuidance(
            resolutionReminders = listOf(
                "Mention that the ride is complete and payment is settled fairly.",
                "Cheerfully wish them luck at their destination."
            ),
            appreciationPhrases = listOf(
                "Lingaw kaayo ko sa imong kuyog.",
                "Salamat sa pagsalig nako."),
            followUpSuggestions = listOf(
                "Offer quick tips about traffic or nearby spots.",
                "Invite them to call again if they need another ride."
            ),
            farewellExamples = listOf(
                ScenarioFarewellLine(
                    bisaya = "Nakaabot na ta. Ayo-ayo ug enjoy sa imong lakaw ha!",
                    translation = "着いたよ。気をつけて、目的地でも楽しんでね！",
                    explanation = "送迎完了と安全・楽しさを願うタクシーらしい締め"
                ),
                ScenarioFarewellLine(
                    bisaya = "Salamat sa imong pagsalig. Tawgi lang ko balik kung kinahanglan kag sakay.",
                    translation = "任せてくれてありがとう。また乗りたいときは呼んでね",
                    explanation = "またの利用を促すフレンドリーな別れ際"
                )
            )
        )
    ),
    RoleplayScenarioDefinition(
        id = "rp_hotel",
        level = 3,
        title = "LV3: ホテル",
        description = "ホテルでのチェックインを完了させるシナリオ",
        situation = "チェックインカウンター",
        aiRole = "フロント係",
        goal = "予約を確認し、部屋の鍵を受け取る",
        iconEmoji = "🏨",
        initialMessage = "Maayong adlaw! Pangalan nimo palihug? (こんにちは！お名前を教えてください)",
        systemPrompt = "",
        hintPhrases = listOf(
            HintPhrase(
                nativeText = "Naa koy reservation saalan nga Tanaka.",
                translation = "タナカの名前で予約しています。"
            ),
            HintPhrase(
                nativeText = "Pwede ko makakuha sa room key?",
                translation = "部屋の鍵を受け取れますか？"
            ),
            HintPhrase(
                nativeText = "Unsa ang oras sa check-out?",
                translation = "チェックアウトの時間は何時ですか？"
            )
        ),
        closingGuidance = ScenarioClosingGuidance(
            resolutionReminders = listOf(
                "Confirm the reservation is settled and keys or instructions are handed over.",
                "Celebrate that any issues were resolved at the counter."
            ),
            appreciationPhrases = listOf(
                "Nalipay ko makatabang sa imong check-in.",
                "Salamat sa pagpili sa among hotel."),
            followUpSuggestions = listOf(
                "Invite them to contact the front desk if they need anything.",
                "Wish them a relaxing stay and mention available amenities."
            ),
            farewellExamples = listOf(
                ScenarioFarewellLine(
                    bisaya = "Kompleto na imong check-in. Enjoy sa imong pagpuyo ug tawga lang mi kung nay kinahanglan ha!",
                    translation = "チェックイン完了です。ゆっくり過ごして、何かあったらすぐ呼んでね！",
                    explanation = "手続き完了とサポート継続を丁寧に伝えるフロントらしい締め"
                ),
                ScenarioFarewellLine(
                    bisaya = "Nalipay ko nga na-ayos nato ang tanan. Ayo-ayo ug pahulay karon gabii!",
                    translation = "全部整ってよかったです。今夜はゆっくり休んでくださいね",
                    explanation = "トラブル解決と休息を促す優しい締め台詞"
                )
            )
        )
    )
)

private val roleplayScenarioDefinitionMap = roleplayScenarioDefinitions.associateBy { it.id }

/**
 * 既存 UI のリスト表示で利用する簡易モデル。
 */
val roleplayScenarios: List<RoleplayScenario> = roleplayScenarioDefinitions.map { definition ->
    RoleplayScenario(
        id = definition.id,
        title = definition.title,
        description = definition.description,
        requiredLevel = definition.level,
        iconEmoji = definition.iconEmoji
    )
}

fun getRoleplayScenarioDefinition(id: String): RoleplayScenarioDefinition {
    return roleplayScenarioDefinitionMap[id] ?: roleplayScenarioDefinitions.first()
}
