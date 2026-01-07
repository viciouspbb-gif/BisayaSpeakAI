package com.bisayaspeak.ai.data.repository

import com.bisayaspeak.ai.data.local.UserProgress
import com.bisayaspeak.ai.data.local.UserProgressDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProgressRepository(
    private val userProgressDao: UserProgressDao
) {
    fun getUnlockedLevels(): Flow<Set<Int>> {
        return userProgressDao.getAllProgress().map { progressList ->
            progressList.filter { it.isUnlocked }.map { it.level }.toSet()
        }
    }

    suspend fun markLevelCompleted(level: Int, stars: Int) {
        val current = userProgressDao.getProgress(level)
        val newStars = maxOf(current?.stars ?: 0, stars)
        userProgressDao.upsert(
            UserProgress(
                level = level,
                stars = newStars,
                isUnlocked = current?.isUnlocked ?: (level == 1)
            )
        )
    }

    suspend fun unlockLevel(level: Int) {
        val current = userProgressDao.getProgress(level)
        if (current == null) {
            userProgressDao.upsert(UserProgress(level = level, stars = 0, isUnlocked = true))
        } else if (!current.isUnlocked) {
            userProgressDao.upsert(current.copy(isUnlocked = true))
        }
    }
}
