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
    val systemPrompt: String
)

// ─────────────────────────────────────────────────────────────────────────────
// シナリオ定義（LV1～LV3）
// ─────────────────────────────────────────────────────────────────────────────

private val lv1AirportPrompt = """
    You are an immigration officer at Cebu Airport. The user is a Japanese tourist. Ask short questions in simple English and Bisaya (Cebuano). Ask for passport, purpose of visit, and length of stay. Correct the user if they make a mistake. Goal: Approve entry.
""".trimIndent()

val roleplayScenarioDefinitions: List<RoleplayScenarioDefinition> = listOf(
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
        systemPrompt = lv1AirportPrompt
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
        systemPrompt = ""
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
        systemPrompt = ""
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
