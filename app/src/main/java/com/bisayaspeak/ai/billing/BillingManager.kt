package com.bisayaspeak.ai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.bisayaspeak.ai.data.model.UserPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing管理クラス
 */
class BillingManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BillingManager"
        
        // 商品ID
        const val PRO_UNLOCK_SKU = "pro_unlock"
        const val PREMIUM_AI_MONTHLY_SKU = "premium_ai_monthly"
        const val PREMIUM_AI_YEARLY_SKU = "premium_ai_yearly"
        
        // 旧商品ID（互換性のため）
        const val PREMIUM_MONTHLY_SKU = "premium_monthly"
        const val PREMIUM_YEARLY_SKU = "premium_yearly"
        
        // 開発者アカウント（常にプレミアム扱い）
        private val DEVELOPER_EMAILS = setOf(
            "vicious.pbb@gmail.com"
        )
    }
    
    private var billingClient: BillingClient? = null
    
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    
    private val _isProUnlocked = MutableStateFlow(false)
    val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()
    
    private val _hasPremiumAI = MutableStateFlow(false)
    val hasPremiumAI: StateFlow<Boolean> = _hasPremiumAI.asStateFlow()
    
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()
    private val _userPlan = MutableStateFlow(UserPlan.LITE)
    val userPlan: StateFlow<UserPlan> = _userPlan.asStateFlow()
    
    // 購入成功コールバック
    var onPurchaseSuccess: ((String) -> Unit)? = null
    
    /**
     * 開発者アカウントかチェック
     */
    private fun isDeveloperAccount(): Boolean {
        try {
            val accountManager = android.accounts.AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            
            Log.d(TAG, "Checking ${accounts.size} Google accounts")
            for (account in accounts) {
                Log.d(TAG, "Found account: ${account.name}")
                if (account.name in DEVELOPER_EMAILS) {
                    Log.d(TAG, "✅ Developer account detected: ${account.name}")
                    return true
                }
            }
            Log.d(TAG, "❌ No developer account found")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check developer account", e)
        }
        return false
    }
    
    /**
     * Billing Clientを初期化
     */
    fun initialize(onReady: () -> Unit = {}) {
        // デバッグビルドまたは開発者アカウントの場合は即座にプレミアム扱い
        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        
        if (isDebug || isDeveloperAccount()) {
            _isPremium.value = true
            _isProUnlocked.value = true
            _hasPremiumAI.value = true
            refreshUserPlan()
            Log.d(TAG, "Premium enabled - isDebug=$isDebug, isDeveloper=${isDeveloperAccount()}")
            onReady()
            return
        }
        
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully")
                    queryProducts()
                    checkPremiumStatus()
                    onReady()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // 再接続を試みる
            }
        })
    }
    
    /**
     * 商品情報を取得
     */
    private fun queryProducts() {
        val productList = listOf(
            // Pro Unlock (買い切り)
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_UNLOCK_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            // Premium AI (サブスク)
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_AI_MONTHLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_AI_YEARLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            // 旧商品ID（互換性）
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList
                Log.d(TAG, "Products loaded: ${productDetailsList.size}")
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * プレミアムステータスを確認
     */
    fun checkPremiumStatus() {
        // サブスクリプションを確認
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient?.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremiumAI = purchases.any { purchase ->
                    (purchase.products.contains(PREMIUM_AI_MONTHLY_SKU) || 
                     purchase.products.contains(PREMIUM_AI_YEARLY_SKU) ||
                     purchase.products.contains(PREMIUM_MONTHLY_SKU) || 
                     purchase.products.contains(PREMIUM_YEARLY_SKU)) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                _hasPremiumAI.value = hasPremiumAI
                _isPremium.value = hasPremiumAI // Premium AIは全機能含む
                Log.d(TAG, "Premium AI status: $hasPremiumAI (${purchases.size} subs)")
                refreshUserPlan()
            }
        }
        
        // 買い切り商品を確認
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        
        billingClient?.queryPurchasesAsync(inAppParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasProUnlock = purchases.any { purchase ->
                    purchase.products.contains(PRO_UNLOCK_SKU) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                _isProUnlocked.value = hasProUnlock
                if (hasProUnlock && !_isPremium.value) {
                    _isPremium.value = true // ProもPremium扱い
                }
                Log.d(TAG, "Pro Unlock status: $hasProUnlock (${purchases.size} in-app)")
                refreshUserPlan()
            }
        }
    }
    
    /**
     * 購入フローを開始
     */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        
        // サブスクリプションの場合はofferTokenが必要
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }
        
        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
        Log.d(TAG, "Launch billing flow result: ${result?.responseCode}")
    }
    
    /**
     * 商品IDで購入フローを開始
     */
    fun launchPurchaseFlowByProductId(activity: Activity, productId: String) {
        val product = _products.value.find { it.productId == productId }
        if (product != null) {
            launchPurchaseFlow(activity, product)
        } else {
            Log.e(TAG, "Product not found: $productId")
        }
    }
    
    /**
     * 購入を処理
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                
                // 購入した商品に応じてフラグを更新
                for (productId in purchase.products) {
                    when (productId) {
                        PRO_UNLOCK_SKU -> {
                            _isProUnlocked.value = true
                            _isPremium.value = true
                            onPurchaseSuccess?.invoke(productId)
                            Log.d(TAG, "Pro Unlock purchased")
                        }
                        PREMIUM_AI_MONTHLY_SKU, PREMIUM_AI_YEARLY_SKU,
                        PREMIUM_MONTHLY_SKU, PREMIUM_YEARLY_SKU -> {
                            _hasPremiumAI.value = true
                            _isPremium.value = true
                            onPurchaseSuccess?.invoke(productId)
                            Log.d(TAG, "Premium AI purchased")
                        }
                    }
                    refreshUserPlan()
                }
            }
        }
    }

    private fun refreshUserPlan() {
        _userPlan.value = UserPlan.fromFlags(
            isProUnlocked = _isProUnlocked.value,
            hasPremiumAI = _hasPremiumAI.value
        )
    }
    
    /**
     * 購入を承認
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }
    
    /**
     * 購入を復元
     */
    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        checkPremiumStatus()
        refreshUserPlan()
        onComplete(_isPremium.value)
    }
    
    /**
     * リソースを解放
     */
    fun destroy() {
        billingClient?.endConnection()
    }
}
