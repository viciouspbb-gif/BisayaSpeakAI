package com.bisayaspeak.ai.ui.roleplay

/**
 * ユーザーの発言から「名前」を抜き出したり、「別れの挨拶」を判定したりする専用クラス。
 * ViewModelから重い正規表現を追い出して、ここだけで完結させます。
 */
class RoleplayLogicProcessor {

    private val nameUnlockPatterns = listOf(
        Regex("(?i)(?:my name is|i am|i'm|this is|call me)\\s+([A-Za-z][A-Za-z\\-'\\s]{1,40})"),
        Regex("(?i)(?:ako\\s+si|ako\\s+kay|ako\\s+ni|ang pangalan ko kay|ang pangalan nako kay|pangalan ko si|pangalan ko kay|pangalan nako)\\s+([A-Za-z][A-Za-z\\-'\\s]{1,40})"),
        Regex("私の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
        Regex("僕の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
        Regex("俺の名前は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})"),
        Regex("私は\\s*([\\p{InHiragana}\\p{InKatakana}\\p{IsHan}A-Za-zー\\s]{1,20})です")
    )

    private val farewellKeywords = setOf(
        "babay", "sige, una", "bye", "またね", "じゃあね", "おやすみ", "see you",
        "goodbye", "gotta go", "また今度", "お疲れ", "そろそろ行く", "あきらめる", "やめる"
    ).map { it.lowercase() }.toSet()

    /** 名前を抽出 */
    fun extractLearnerName(text: String): String? {
        for (pattern in nameUnlockPatterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return null
    }

    /** 別れ際か判定 */
    fun isFarewellSignal(text: String): Boolean {
        val lowerText = text.lowercase()
        return farewellKeywords.any { lowerText.contains(it) }
    }
}
