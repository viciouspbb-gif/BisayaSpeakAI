package com.bisayaspeak.ai.data.repository

import java.time.LocalDate

/**
 * Controls scheduled releases for listening levels. CEO can update the schedule
 * when announcing new drop dates without touching the UI layer.
 */
object LevelReleasePolicy {

    private data class ReleaseEntry(val date: LocalDate, val maxLevel: Int)

    private val releaseSchedule = listOf(
        // 13th: unlock Level 32 (Chapter 7 finale)
        ReleaseEntry(LocalDate.of(2026, 3, 13), 32),
        // 20th: unlock up to Level 35 (next Tari drop)
        ReleaseEntry(LocalDate.of(2026, 3, 20), 35)
    )

    private const val BASELINE_UNLOCKED = 31

    fun currentReleasedMax(today: LocalDate = LocalDate.now()): Int {
        val unlockedEntry = releaseSchedule
            .filter { !it.date.isAfter(today) }
            .maxByOrNull { it.maxLevel }
        return unlockedEntry?.maxLevel ?: BASELINE_UNLOCKED
    }
}
