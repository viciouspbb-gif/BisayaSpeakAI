package com.bisayaspeak.ai.feature

import com.bisayaspeak.ai.BuildConfig

/**
 * Pro機能の判定を一元化するヘルパー。
 * 常にbaseConditionをそのまま返す（デバッグ用の強制有効化は無効化）
 */
object ProFeatureGate {
    fun isProFeatureEnabled(baseCondition: Boolean): Boolean {
        return baseCondition
    }
}
