package com.bisayaspeak.ai.ui.upgrade

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.data.PurchaseStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * UpgradeScreen用ViewModel
 */
class UpgradeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val billingManager = BillingManager(application)
    private val purchaseStore = PurchaseStore(application)
    
    private val _uiState = MutableStateFlow(UpgradeUiState())
    val uiState: StateFlow<UpgradeUiState> = _uiState.asStateFlow()
    
    private val _showPurchaseSuccess = MutableStateFlow<String?>(null)
    val showPurchaseSuccess: StateFlow<String?> = _showPurchaseSuccess.asStateFlow()
    
    init {
        // BillingManager初期化
        billingManager.initialize {
            // 初期化完了後、購入状態を確認
            viewModelScope.launch {
                syncPurchaseStatus()
            }
        }
        
        // 購入成功コールバック
        billingManager.onPurchaseSuccess = { productId ->
            viewModelScope.launch {
                when (productId) {
                    BillingManager.PRO_UNLOCK_SKU -> {
                        purchaseStore.setProUnlocked(true)
                        _showPurchaseSuccess.value = "Pro Unlock"
                    }
                    BillingManager.PREMIUM_AI_MONTHLY_SKU,
                    BillingManager.PREMIUM_AI_YEARLY_SKU -> {
                        purchaseStore.setPremiumAI(true)
                        _showPurchaseSuccess.value = "Premium AI"
                    }
                }
                syncPurchaseStatus()
            }
        }
        
        // 購入状態を監視
        viewModelScope.launch {
            combine(
                billingManager.isProUnlocked,
                billingManager.hasPremiumAI,
                purchaseStore.isProUnlocked,
                purchaseStore.hasPremiumAI
            ) { billingPro, billingPremium, storePro, storePremium ->
                UpgradeUiState(
                    isProUnlocked = billingPro || storePro,
                    hasPremiumAI = billingPremium || storePremium,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * 購入状態を同期
     */
    private suspend fun syncPurchaseStatus() {
        val isProUnlocked = billingManager.isProUnlocked.value
        val hasPremiumAI = billingManager.hasPremiumAI.value
        
        if (isProUnlocked) {
            purchaseStore.setProUnlocked(true)
        }
        if (hasPremiumAI) {
            purchaseStore.setPremiumAI(true)
        }
    }
    
    /**
     * Pro Unlockを購入
     */
    fun purchaseProUnlock(activity: Activity) {
        billingManager.launchPurchaseFlowByProductId(activity, BillingManager.PRO_UNLOCK_SKU)
    }
    
    /**
     * Premium AI (月額)を購入
     */
    fun purchasePremiumAIMonthly(activity: Activity) {
        billingManager.launchPurchaseFlowByProductId(activity, BillingManager.PREMIUM_AI_MONTHLY_SKU)
    }
    
    /**
     * Premium AI (年額)を購入
     */
    fun purchasePremiumAIYearly(activity: Activity) {
        billingManager.launchPurchaseFlowByProductId(activity, BillingManager.PREMIUM_AI_YEARLY_SKU)
    }
    
    /**
     * 購入を復元
     */
    fun restorePurchases() {
        viewModelScope.launch {
            billingManager.checkPremiumStatus()
            syncPurchaseStatus()
        }
    }
    
    /**
     * 購入成功メッセージをクリア
     */
    fun clearPurchaseSuccessMessage() {
        _showPurchaseSuccess.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}

/**
 * UpgradeScreen UI状態
 */
data class UpgradeUiState(
    val isProUnlocked: Boolean = false,
    val hasPremiumAI: Boolean = false,
    val isLoading: Boolean = true
)
