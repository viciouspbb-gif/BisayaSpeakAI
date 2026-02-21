package com.bisayaspeak.ai.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * アプリ全体のプレミアム判定を一元管理するシングルトン。
 * hasPremiumAI / isProUnlocked / subscriptionActive のいずれかが true であれば
 * isPremiumUser も true になる。
 */
object PremiumStatusProvider {

    private val _isPremiumUser = MutableStateFlow(false)
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
