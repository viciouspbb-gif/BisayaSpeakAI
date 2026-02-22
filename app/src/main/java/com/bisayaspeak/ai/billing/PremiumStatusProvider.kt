package com.bisayaspeak.ai.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.bisayaspeak.ai.BuildConfig

/**
 * アプリ全体のプレミアム判定を一元管理するシングルトン。
 * hasPremiumAI / isProUnlocked / subscriptionActive のいずれかが true であれば
 * isPremiumUser も true になる。
 * 
 * 初期値はフレーバーで決定：
 * - Pro版デバッグ：最初からtrue（テスト用）
 * - その他：false（実際の課金状態を待つ）
 */
object PremiumStatusProvider {

    // フレーバーに応じた初期値を設定
    private val initialValue = BuildConfig.FLAVOR == "pro" && BuildConfig.DEBUG
    private val _isPremiumUser = MutableStateFlow(initialValue)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    fun updateStatus(
        hasPremiumAI: Boolean,
        isProUnlocked: Boolean,
        subscriptionActive: Boolean
    ) {
        val next = hasPremiumAI || isProUnlocked || subscriptionActive
        if (_isPremiumUser.value != next) {
            _isPremiumUser.value = next
        }
    }

    fun currentValue(): Boolean = _isPremiumUser.value
}
