package com.bisayaspeak.ai.data.model

data class LessonResult(
    val correctCount: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val leveledUp: Boolean
)
