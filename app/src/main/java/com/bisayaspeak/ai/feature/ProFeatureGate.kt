package com.bisayaspeak.ai.feature

import com.bisayaspeak.ai.BuildConfig

/**
 * Pro機能の判定を一元化するヘルパー。
 * CEO直命：リリースビルドでのisProDebugを物理的に無効化
 */
object ProFeatureGate {
    
    /**
     * 共通のisProDebug判定ロジック
     */
    private fun isProDebug(): Boolean {
        return if (BuildConfig.DEBUG) {
            // デバッグビルド時のみ、Proフレーバーを参照可能
            BuildConfig.FLAVOR == "pro"
        } else {
            // リリースビルドでは、物理的にfalse固定
            false
        }
    }
    
    /**
     * フレーバー対応のPro機能判定
     * @param baseCondition 基本となる課金状態
     * @return Pro版デバッグならtrue、その他はbaseCondition
     */
    fun isProFeatureEnabled(baseCondition: Boolean): Boolean {
        return if (isProDebug()) {
            true  // Proデバッグ：強制的に有効化
        } else {
            baseCondition  // その他：基本条件を使用
        }
    }
    
    /**
     * AAB65同期確定値と同じロジック
     * @param isPremiumUser 課金状態
     * @return フレーバーとデバッグモードを考慮した最終判定
     */
    fun getEffectiveProStatus(isPremiumUser: Boolean): Boolean {
        return if (isProDebug()) {
            true  // Proデバッグ：描画の第一フレームからtrue
        } else {
            isPremiumUser  // その他：課金状態を使用
        }
    }
}
