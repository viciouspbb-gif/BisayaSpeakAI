package com.bisayaspeak.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sentence: String,
    val meaning: String,
    val level: Int,
    val type: String
)
