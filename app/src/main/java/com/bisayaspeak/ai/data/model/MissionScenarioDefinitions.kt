package com.bisayaspeak.ai.data.model
import androidx.compose.ui.graphics.Color

data class MissionScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val difficultyLabel: String,
    val context: MissionContext,
    val backgroundGradient: List<Color>
)

val missionScenarios: List<MissionScenario> = listOf(
    MissionScenario(
        id = "market_bargain",
        title = "Market Bargain Takedown",
        subtitle = "市場で値切り交渉を完勝せよ",
        difficultyLabel = "Lv.2 日常交渉",
        context = MissionContext(
            title = "市場で値切り",
            role = "露店の店主（計算に厳しいがフレンドリー）",
            situation = "セブのカルボン市場。観光客に人気の民芸雑貨ブース前。",
            goal = "ビサヤ語で値切り交渉を成功させ、200ペソ以内で購入完了する",
            hints = listOf("Hangyo (お願い)", "Barato (安い)", "Palihug tawon (お願い)"),
            turnLimit = 8,
            tone = "陽気で少し早口。時々ジョークを挟む。",
            level = LearningLevel.INTERMEDIATE
        ),
        backgroundGradient = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    ),
    MissionScenario(
        id = "staff_instruction",
        title = "Staff Instruction Blitz",
        subtitle = "新人スタッフに正しく指示を伝えよ",
        difficultyLabel = "Lv.3 ビジネス指示",
        context = MissionContext(
            title = "新人指示トレーニング",
            role = "カフェのシフトリーダー（冷静で丁寧）",
            situation = "モダンカフェの開店30分前。新人スタッフと最終確認。",
            goal = "新人に3つのタスクをビサヤ語で明確に伝え、復唱させる",
            hints = listOf("Siguroha (確実に)", "I-check (確認する)", "Sunod nga buhat (次の仕事)"),
            turnLimit = 7,
            tone = "冷静だが優しい指導者。丁寧に確認質問を挟む。",
            level = LearningLevel.ADVANCED
        ),
        backgroundGradient = listOf(Color(0xFF1D2671), Color(0xFFC33764))
    ),
    MissionScenario(
        id = "jealousy",
        title = "Jealousy Radar",
        subtitle = "恋人の嫉妬を溶かし信頼を勝ち取れ",
        difficultyLabel = "Lv.4 恋愛心理",
        context = MissionContext(
            title = "嫉妬の弁明ミッション",
            role = "恋人（少し拗ねているがおちゃめ）",
            situation = "ナイトマーケットの帰り道。友達と話していた件で勘違い。",
            goal = "丁寧に事情を説明し、相手の不安を解消して仲直りする",
            hints = listOf("Pasayloa ko (ごめん)", "Walay lain (ほかに誰もいない)", "Salig (信頼)"),
            turnLimit = 9,
            tone = "少し拗ねた恋人。すねた声調とユーモア混じり。",
            level = LearningLevel.ADVANCED
        ),
        backgroundGradient = listOf(Color(0xFF360033), Color(0xFF0B8793))
    ),
    MissionScenario(
        id = "making_up",
        title = "Making Up Quest",
        subtitle = "親友と本気の仲直りを果たせ",
        difficultyLabel = "Lv.3 エモーショナルケア",
        context = MissionContext(
            title = "親友への謝罪作戦",
            role = "幼馴染の親友（率直だが感情豊か）",
            situation = "カフェのテラス席。ドタキャン続きで怒っている。",
            goal = "ビサヤ語で謝罪し、次の約束を決めたうえで笑顔で終わる",
            hints = listOf("Tinood (本当)", "Pasalig (約束)", "Sunod nga higayon (次の機会)"),
            turnLimit = 8,
            tone = "感情的だが心底信頼している親友。",
            level = LearningLevel.INTERMEDIATE
        ),
        backgroundGradient = listOf(Color(0xFF41295A), Color(0xFF2F0743))
    )
)

fun getMissionScenario(id: String): MissionScenario? =
    missionScenarios.find { it.id == id }
