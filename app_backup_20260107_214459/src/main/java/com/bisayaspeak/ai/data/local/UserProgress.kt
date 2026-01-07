package com.bisayaspeak.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val level: Int,
    val stars: Int = 0,
    val isUnlocked: Boolean = false
)
