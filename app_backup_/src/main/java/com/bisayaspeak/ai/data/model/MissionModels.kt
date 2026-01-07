package com.bisayaspeak.ai.data.model

import java.util.UUID

data class MissionContext(
    val missionId: String = UUID.randomUUID().toString(),
    val title: String,
    val role: String,
    val situation: String,
    val goal: String,
    val hints: List<String> = emptyList(),
    val turnLimit: Int = 8,
    val tone: String? = null,
    val level: LearningLevel = LearningLevel.INTERMEDIATE
)

data class MissionChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val primaryText: String,
    val secondaryText: String? = null,
    val isUser: Boolean,
    val isGoalFlagged: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String = primaryText
)

data class MissionHistoryMessage(
    val text: String,
    val isUser: Boolean
)

enum class TranslationDirection {
    JA_TO_CEB,
    CEB_TO_JA
}
