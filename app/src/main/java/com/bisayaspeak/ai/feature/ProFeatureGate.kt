package com.bisayaspeak.ai.feature

import com.bisayaspeak.ai.BuildConfig

/**
 * Pro機能の判定を一元化するヘルパー。
 * AAB65の同期確定値ロジックに統一し、全ての画面遷移のガード条件を一貫させる。
 * Pro版デバッグ：強制的に有効化、Lite版デバッグ：従来ロジックを維持
 */
object ProFeatureGate {
    
    /**
     * フレーバー対応のPro機能判定
     * @param baseCondition 基本となる課金状態
     * @return Pro版デバッグならtrue、その他はbaseCondition
     */
    fun isProFeatureEnabled(baseCondition: Boolean): Boolean {
        val isProFlavor = BuildConfig.FLAVOR == "pro"
        return if (isProFlavor && BuildConfig.DEBUG) {
            true  // Pro版デバッグ：強制的に有効化
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
        val isProFlavor = BuildConfig.FLAVOR == "pro"
        return if (isProFlavor && BuildConfig.DEBUG) {
            true  // Pro版デバッグ：描画の第一フレームからtrue
        } else {
            isPremiumUser  // その他：課金状態を使用
        }
    }
}
