package com.bisayaspeak.ai.domain.xp

import kotlin.math.max

object XpProgressManager {

    fun requiredXp(level: Int): Int {
        return if (level <= 1) 100 else 100 + (level * 50)
    }

    fun applyXpGain(currentXp: Int, currentLevel: Int, gain: Int): XpUpdateResult {
        var xp = max(0, currentXp) + gain
        var level = max(1, currentLevel)
        var levelsGained = 0
        var leveledUp = false

        while (xp >= requiredXp(level)) {
            xp -= requiredXp(level)
            level += 1
            levelsGained += 1
            leveledUp = true
        }

        return XpUpdateResult(
            newLevel = level,
            xpIntoLevel = xp,
            leveledUp = leveledUp,
            levelsGained = levelsGained,
            requiredXpForLevel = requiredXp(level)
        )
    }
}

data class XpUpdateResult(
    val newLevel: Int,
    val xpIntoLevel: Int,
    val leveledUp: Boolean,
    val levelsGained: Int,
    val requiredXpForLevel: Int
)
