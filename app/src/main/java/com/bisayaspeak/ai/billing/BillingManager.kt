package com.bisayaspeak.ai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
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

        const val PREMIUM_AI_MONTHLY_BASE_PLAN_ID = "premium_ai_monthly_sub"
        const val PREMIUM_AI_YEARLY_BASE_PLAN_ID = "premium_ai_yearly_sub"

        const val MONTHLY_TRIAL_TAG = "monthly-trial"
        const val YEARLY_TRIAL_TAG = "yearly-trial"
        
        // 旧商品ID（互換性のため）
        const val PREMIUM_MONTHLY_SKU = "premium_monthly"
        const val PREMIUM_YEARLY_SKU = "premium_yearly"
        
        fun basePlanIdFor(productId: String): String? = when (productId) {
            PREMIUM_AI_MONTHLY_SKU -> PREMIUM_AI_MONTHLY_BASE_PLAN_ID
            PREMIUM_AI_YEARLY_SKU -> PREMIUM_AI_YEARLY_BASE_PLAN_ID
            else -> null
        }
        
        // 開発者アカウント（常にプレミアム扱い）
        private val DEVELOPER_EMAILS = setOf(
            "vicious.pbb@gmail.com"
        )
    }
    
    private var billingClient: BillingClient? = null
    private var isClientReady = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase canceled by user")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }
    
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
     * 開発者の実機かどうかを判定
     */
    private fun isMyDevice(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.orEmpty()
        val brand = android.os.Build.BRAND.orEmpty()
        val model = android.os.Build.MODEL.orEmpty()

        Log.d(
            TAG,
            "Device check => manufacturer: $manufacturer / brand: $brand / model: $model"
        )

        return manufacturer.equals("SHARP", ignoreCase = true) ||
            brand.equals("SHARP", ignoreCase = true) ||
            model.contains("sense9", ignoreCase = true)
    }
    
    /**
     * Billing Clientを初期化
     */
    fun initialize(onReady: () -> Unit = {}) {
        if (billingClient?.isReady == true && isClientReady) {
            onReady()
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient is ready")
                    isClientReady = true
                    queryProducts()
                    checkPremiumStatus()
                    onReady()
                } else {
                    Log.e(TAG, "Failed to set up BillingClient: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                isClientReady = false
            }
        })
    }
    
    /**
     * 商品情報を取得
     */
    private fun queryProducts() {
        val client = billingClient ?: return
        val combinedProducts = mutableListOf<ProductDetails>()

        fun queryByType(
            products: List<QueryProductDetailsParams.Product>,
            typeLabel: String,
            onComplete: () -> Unit
        ) {
            if (products.isEmpty()) {
                onComplete()
                return
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()

            client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    combinedProducts.addAll(productDetailsList)
                    Log.d(TAG, "$typeLabel products loaded: ${productDetailsList.size}")
                } else {
                    Log.e(TAG, "Failed to query $typeLabel products: ${billingResult.debugMessage}")
                }
                onComplete()
            }
        }

        val subsProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_AI_MONTHLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_AI_YEARLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_UNLOCK_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        queryByType(subsProducts, "SUBS") {
            queryByType(inAppProducts, "INAPP") {
                _products.value = combinedProducts
                Log.d(TAG, "Total products loaded: ${combinedProducts.size}")
            }
        }
    }
    
    /**
     * プレミアムステータスを確認
     */
    fun checkPremiumStatus() {
        val client = billingClient ?: return

        // サブスクリプションを確認
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        client.queryPurchasesAsync(subsParams) { billingResult, purchases ->
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
        
        client.queryPurchasesAsync(inAppParams) { billingResult, purchases ->
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
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        basePlanId: String? = null,
        offerTag: String? = null
    ) {
        val client = billingClient ?: return
        if (!isClientReady) {
            Log.w(TAG, "BillingClient not ready. Ignoring purchase flow launch")
            return
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        val targetOffer = productDetails.findOffer(basePlanId, offerTag)

        val offerToken = targetOffer?.offerToken
        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        } else {
            Log.w(
                TAG,
                "Offer token not found for product=${productDetails.productId}, basePlanId=$basePlanId, offerTag=$offerTag"
            )
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val result = client.launchBillingFlow(activity, billingFlowParams)
        Log.d(TAG, "Launch billing flow result: ${result?.responseCode}")
    }
    
    /**
     * 商品IDで購入フローを開始
     */
    fun launchPurchaseFlowByProductId(activity: Activity, productId: String, offerTag: String? = null) {
        val product = _products.value.find { it.productId == productId }
        if (product != null) {
            launchPurchaseFlow(activity, product, basePlanIdFor(product.productId), offerTag)
        } else {
            Log.e(TAG, "Product not found: $productId")
            // まだ商品情報が取得できていない場合は再取得を試みる
            queryProducts()
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
    
    fun reloadProducts() {
        queryProducts()
    }

    /**
     * リソースを解放
     */
    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
        isClientReady = false
    }
}
