package com.bisayaspeak.ai.data.model

/**
 * 発音判定結果（APIレスポンス用）
 */
data class PronunciationResult(
    val score: Int,
    val feedback: String,
    val details: String
)
