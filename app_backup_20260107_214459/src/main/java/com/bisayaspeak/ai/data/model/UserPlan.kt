package com.bisayaspeak.ai.data.model

/**
 * アプリ内で提供するプランを統一的に扱うための列挙。
 *
 * - LITE: 無料ユーザー。広告表示・機能制限あり。
 * - STANDARD: Pro Unlock 相当。広告非表示、標準機能が開放。
 * - PREMIUM: Premium AI サブスク。すべての機能が開放。
 */
enum class UserPlan {
    LITE,
    STANDARD,
    PREMIUM;

    companion object {
        fun fromFlags(isProUnlocked: Boolean, hasPremiumAI: Boolean): UserPlan {
            return when {
                hasPremiumAI -> PREMIUM
                isProUnlocked -> STANDARD
                else -> LITE
            }
        }
    }

    val isPaidPlan: Boolean
        get() = this != LITE

    val isPremiumPlan: Boolean
        get() = this == PREMIUM
}
