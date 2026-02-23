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
 * 描画の第一フレームから正しい状態を保証するため、
 * オブジェクト生成時にBuildConfigを参照して即座に初期値を決定する。
 */
object PremiumStatusProvider {

    // 描画の第一フレームからtrueである状態を保証
    // CEO直命：リリースビルドでのisProDebugを物理的に無効化
    private val initialValue: Boolean = if (BuildConfig.DEBUG) {
        // デバッグビルド時のみ、Proフレーバーを参照可能
        BuildConfig.FLAVOR == "pro"
    } else {
        // リリースビルドでは、物理的にfalse固定
        false
    }
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
