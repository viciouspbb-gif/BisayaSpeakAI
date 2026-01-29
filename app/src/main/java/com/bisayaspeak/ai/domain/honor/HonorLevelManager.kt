package com.bisayaspeak.ai.domain.honor

import kotlin.math.max

/**
 * Central place to calculate honor level progress based on total lessons completed.
 */
object HonorLevelManager {

    private val levelThresholds: LinkedHashMap<Int, Int> = linkedMapOf(
        1 to 0,
        2 to 3,
        3 to 7,
        4 to 12,
        5 to 18,
        6 to 25,
        7 to 33,
        8 to 42,
        9 to 52,
        10 to 63,
        11 to 75,
        12 to 88,
        13 to 102,
        14 to 117,
        15 to 133,
        16 to 150,
        17 to 168,
        18 to 187,
        19 to 207,
        20 to 228,
        21 to 250,
        22 to 273,
        23 to 297,
        24 to 322,
        25 to 348,
        26 to 375,
        27 to 403,
        28 to 432,
        29 to 462,
        30 to 493
    )

    private val orderedLevels = levelThresholds.entries.toList()

    fun getProgress(totalLessonsCompleted: Int): HonorLevelProgress {
        val sanitizedLessons = max(0, totalLessonsCompleted)
        var currentEntry = orderedLevels.first()
        orderedLevels.forEach { entry ->
            if (sanitizedLessons >= entry.value) {
                currentEntry = entry
            }
        }
        val currentIndex = orderedLevels.indexOf(currentEntry)
        val nextEntry = if (currentIndex + 1 < orderedLevels.size) {
            orderedLevels[currentIndex + 1]
        } else {
            currentEntry
        }

        val span = (nextEntry.value - currentEntry.value).coerceAtLeast(1)
        val lessonsIntoLevel = sanitizedLessons - currentEntry.value
        val fraction = if (nextEntry == currentEntry) 1f else (lessonsIntoLevel / span.toFloat()).coerceIn(0f, 1f)
        val lessonsRemaining = if (nextEntry == currentEntry) 0 else (nextEntry.value - sanitizedLessons).coerceAtLeast(0)

        return HonorLevelProgress(
            level = currentEntry.key,
            lessonsForCurrentLevel = currentEntry.value,
            lessonsForNextLevel = nextEntry.value,
            lessonsRemainingToNext = lessonsRemaining,
            progressToNextLevel = fraction
        )
    }

    fun levelForLessons(totalLessonsCompleted: Int): Int {
        return getProgress(totalLessonsCompleted).level
    }

    fun requiredLessonsForLevel(level: Int): Int {
        return levelThresholds[level] ?: levelThresholds.values.last()
    }
}

data class HonorLevelProgress(
    val level: Int,
    val lessonsForCurrentLevel: Int,
    val lessonsForNextLevel: Int,
    val lessonsRemainingToNext: Int,
    val progressToNextLevel: Float
)
