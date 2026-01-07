package com.bisayaspeak.ai.data.model.mock

import com.bisayaspeak.ai.data.model.LearningLevel

data class MockQuizQuestion(
    val id: String,
    val level: LearningLevel,
    val questionJa: String,
    val questionVisayan: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationJa: String? = null
)
