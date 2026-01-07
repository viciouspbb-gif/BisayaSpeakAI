package com.bisayaspeak.ai.data.model

enum class LearningLevel(val key: String) {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    val apiValue: String
        get() = key

    fun apiValue(): String = key

    val displayName: String
        get() = when (this) {
            BEGINNER -> "初級"
            INTERMEDIATE -> "中級"
            ADVANCED -> "上級"
        }
    
    val displayNameRes: String
        get() = displayName

    fun routeKey(): String = key

    companion object {
        fun fromApiValue(value: String): LearningLevel {
            return entries.find { it.key == value } ?: BEGINNER
        }
    }
}

enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    val apiValue: String
        get() = name.lowercase()

    companion object {
        fun fromApiValue(value: String): DifficultyLevel {
            return values().find { it.apiValue == value } ?: BEGINNER
        }
    }
}

data class LearningContent(
    val id: String,
    val bisaya: String,
    val japanese: String,
    val english: String,
    val category: String,
    val level: LearningLevel
) {
    val bisayaText: String get() = bisaya
    val japaneseTranslation: String get() = japanese
    val englishTranslation: String get() = english
    val pronunciation: String get() = bisaya
    val difficulty: Int get() = when (level) {
        LearningLevel.BEGINNER -> 1
        LearningLevel.INTERMEDIATE -> 3
        LearningLevel.ADVANCED -> 5
    }
}

data class PronunciationData(
    val score: Int,
    val feedback: String = "",
    val detailedFeedback: List<FeedbackDetail> = emptyList(),
    val tips: List<String> = emptyList()
)

data class FeedbackDetail(
    val aspect: String,
    val score: Int,
    val comment: String
)

data class PronunciationResponse(
    val score: Int,
    val feedback: String?,
    val detailedFeedback: List<FeedbackDetail>?,
    val tips: List<String>?
) {
    val pronunciationScore: Int get() = score
    val rating: String get() = when {
        score >= 90 -> "優秀"
        score >= 70 -> "良好"
        score >= 50 -> "普通"
        else -> "要改善"
    }
    val word: String get() = ""
    val overall: String get() = feedback ?: ""
    val details: List<FeedbackDetail> get() = detailedFeedback ?: emptyList()
    val comparisonDetails: String get() = ""
}

enum class ConversationMode {
    SCENARIO,
    TOPIC,
    FREE_TALK,
    ROLEPLAY,
    ROLE_PLAY,
    SHADOWING,
    WORD_DRILL,
    ROLEPLAY_SCENE // 新しい実践ロールプレイモード
}

data class RolePlayScenario(
    val id: String,
    val titleJa: String,
    val titleEn: String,
    val descriptionJa: String,
    val descriptionEn: String,
    val userRoleJa: String,
    val userRoleEn: String,
    val aiRoleJa: String,
    val aiRoleEn: String,
    val level: LearningLevel,
    val icon: String,
    val isFree: Boolean = false  // 無料で利用可能かどうか（デフォルト: false）
)

data class ConversationTurn(
    val speaker: Speaker,
    val text: String,
    val translation: String = ""
)

enum class Speaker(val label: String) {
    USER("You"),
    AI("AI")
}

data class ConversationSummary(
    val totalTurns: Int,
    val userTurns: Int,
    val aiTurns: Int,
    val duration: Long,
    val phrasesLearned: List<String>,
    val feedback: String,
    val durationSeconds: Int = (duration / 1000).toInt(),
    val averagePronunciationScore: Int = 0,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
)

data class ConversationSession(
    val turns: MutableList<ConversationTurn> = mutableListOf(),
    var sceneId: String? = null
)

sealed class ConversationUiState {
    object Idle : ConversationUiState()
    object Loading : ConversationUiState()
    object Ready : ConversationUiState()
    object Chatting : ConversationUiState()
    object Recording : ConversationUiState()
    object ProcessingAudio : ConversationUiState()
    object WaitingForAI : ConversationUiState()
    object Success : ConversationUiState()
    data class ShowingSummary(val summary: ConversationSummary) : ConversationUiState()
    data class Error(val message: String) : ConversationUiState()
}
