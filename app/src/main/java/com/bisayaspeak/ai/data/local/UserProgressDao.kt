package com.bisayaspeak.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress ORDER BY level ASC")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Query("SELECT * FROM user_progress WHERE level = :level LIMIT 1")
    suspend fun getProgress(level: Int): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgress)

    @Query("UPDATE user_progress SET stars = :stars, isUnlocked = :isUnlocked WHERE level = :level")
    suspend fun updateProgress(level: Int, stars: Int, isUnlocked: Boolean)

    @Query("SELECT COUNT(*) FROM user_progress")
    suspend fun countProgress(): Int
}
