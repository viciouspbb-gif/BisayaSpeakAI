package com.bisayaspeak.ai.ui.upgrade

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.data.PurchaseStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    
    val products: StateFlow<List<ProductDetails>> = billingManager.products
    
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
                    BillingManager.PREMIUM_AI_MONTHLY_SKU,
                    BillingManager.PREMIUM_AI_YEARLY_SKU -> {
                        purchaseStore.setPremiumAI(true)
                        _showPurchaseSuccess.value = "Premium AI"
                    }
                    // PRO_UNLOCK_SKUは削除
                }
                syncPurchaseStatus()
            }
        }
        
        // 購入状態を監視
        viewModelScope.launch {
            combine(
                billingManager.hasPremiumAI,
                purchaseStore.hasPremiumAI
            ) { billingPremium, storePremium ->
                UpgradeUiState(
                    isProUnlocked = false, // 買い切りは削除
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
        val hasPremiumAI = billingManager.hasPremiumAI.value
        
        if (hasPremiumAI) {
            purchaseStore.setPremiumAI(true)
        }
        // isProUnlockedは削除
    }
    
    /**
     * Premium AIを購入
     */
    fun purchasePremiumAI(activity: Activity, productId: String) {
        billingManager.launchPurchaseFlowByProductId(activity, productId)
    }
    
    /**
     * Premium AI (月額)を購入
     */
    fun purchasePremiumAIMonthly(activity: Activity) {
        billingManager.launchPurchaseFlowByProductId(
            activity,
            BillingManager.PREMIUM_AI_MONTHLY_SKU,
            BillingManager.MONTHLY_TRIAL_TAG
        )
    }
    
    /**
     * Premium AI (年額)を購入
     */
    fun purchasePremiumAIYearly(activity: Activity) {
        billingManager.launchPurchaseFlowByProductId(
            activity,
            BillingManager.PREMIUM_AI_YEARLY_SKU,
            BillingManager.YEARLY_TRIAL_TAG
        )
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

    fun reloadProducts() {
        billingManager.reloadProducts()
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
