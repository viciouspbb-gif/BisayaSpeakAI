package com.bisayaspeak.ai.data.model

/**
 * Practice用の統一データクラス
 */
data class PracticeItem(
    val id: String,
    val category: String,
    val bisaya: String,
    val japanese: String,
    val english: String,
    val pronunciation: String,
    val difficulty: Int,
    val isPremium: Boolean,
    val description: String? = null  // 補足説明（使用場面や注意点など）
)
