package com.bisayaspeak.ai.feature

import com.bisayaspeak.ai.BuildConfig

/**
 * Pro機能の判定を一元化するヘルパー。
 * Debugビルドではプロ機能を常に有効化し、リリースビルドでは従来ロジックをそのまま使用する。
 */
object ProFeatureGate {
    fun isProFeatureEnabled(baseCondition: Boolean): Boolean {
        if (BuildConfig.DEBUG) return true
        return baseCondition
    }
}
