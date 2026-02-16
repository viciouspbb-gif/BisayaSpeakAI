package com.bisayaspeak.ai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.data.PurchaseStore
import com.bisayaspeak.ai.ui.navigation.AppNavGraph
import com.bisayaspeak.ai.ui.theme.BisayaSpeakAITheme
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModelFactory
import com.bisayaspeak.ai.update.UpdateCheckResult
import com.bisayaspeak.ai.update.UpdateManager
import com.bisayaspeak.ai.R
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {
    
    private lateinit var billingManager: BillingManager
    private lateinit var purchaseStore: PurchaseStore
    private var updateManager: UpdateManager? = null
    private val isInAppUpdateSupported: Boolean by lazy { isInstalledFromPlayStore() }
    private var adsInitializationTriggered = false

    companion object {
        private const val TAG = "MainActivity"
        private const val CONSENT_TEST_DEVICE_HASH = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        requestUserConsentAndInitializeAds()
        
        // Billing初期化と購入リストア
        billingManager = BillingManager(this)
        purchaseStore = PurchaseStore(this)
        
        billingManager.initialize {
            // 初期化完了後、購入状態をリストア
            lifecycleScope.launch {
                billingManager.checkPremiumStatus()
                syncPurchaseStatus()
            }
        }
        lifecycleScope.launch {
            combine(
                billingManager.hasPremiumAI,
                billingManager.isProUnlocked,
                billingManager.isPremium
            ) { hasPremiumAI, isProUnlocked, subscriptionActive ->
                PremiumStatusProvider.updateStatus(
                    hasPremiumAI = hasPremiumAI,
                    isProUnlocked = isProUnlocked,
                    subscriptionActive = subscriptionActive
                )
            }.collect { }
        }
        
        if (isInAppUpdateSupported) {
            updateManager = UpdateManager(this).also { manager ->
                setupUpdateCallbacks(manager)
                lifecycleScope.launch { checkForAppUpdate(manager) }
            }
        } else {
            android.util.Log.d("MainActivity", "Skip in-app update: not installed from Play Store")
        }

        val app = application as BisayaSpeakApp
        val listeningViewModelFactory = ListeningViewModelFactory(
            application = app,
            repository = app.questionRepository,
            userProgressRepository = app.userProgressRepository,
            dbSeedStateRepository = app.dbSeedStateRepository
        )

        setContentView(R.layout.activity_main)
        setupComposeContent(app, listeningViewModelFactory)
    }

    private fun setupComposeContent(
        app: BisayaSpeakApp,
        listeningViewModelFactory: ListeningViewModelFactory
    ) {
        val composeView = findViewById<ComposeView>(R.id.main_compose_view)
        if (composeView == null) {
            Log.e(TAG, "ComposeView not found; finishing activity to avoid blank screen")
            finish()
            return
        }

        val content: @Composable () -> Unit = {
            BisayaSpeakAITheme {

                val navController = rememberNavController()
                val isPremiumUser by PremiumStatusProvider.isPremiumUser.collectAsState()
                val observedPro by app.proVersionState.collectAsState()

                val debugAuth = remember { FirebaseAuth.getInstance() }
                val currentUser = remember { mutableStateOf(debugAuth.currentUser) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser.value = auth.currentUser
                    }
                    debugAuth.addAuthStateListener(listener)
                    onDispose { debugAuth.removeAuthStateListener(listener) }
                }

                val isDebugWhitelistedUser = BuildConfig.DEBUG &&
                    currentUser.value?.email?.equals("vicious.pbb@gmail.com", ignoreCase = true) == true
                val isProDebugBuild = BuildConfig.DEBUG && BuildConfig.FLAVOR.equals("pro", ignoreCase = true)
                val effectivePro = isPremiumUser || isDebugWhitelistedUser || isProDebugBuild

                androidx.compose.runtime.LaunchedEffect(effectivePro) {
                    app.isProVersion = effectivePro
                }

                AppNavGraph(
                    navController = navController,
                    isProVersion = observedPro,
                    showPremiumTestToggle = false,
                    onTogglePremiumTest = {},
                    listeningViewModelFactory = listeningViewModelFactory,
                    onRestorePurchase = {
                        billingManager.restorePurchases {
                            lifecycleScope.launch { syncPurchaseStatus() }
                        }
                    }
                )
            }
        }

        applyComposeContentWithRetry(composeView, content)
    }

    private fun applyComposeContentWithRetry(
        composeView: ComposeView,
        content: @Composable () -> Unit,
        remainingAttempts: Int = 3
    ) {
        try {
            composeView.setContent(content)
        } catch (npe: NullPointerException) {
            if (remainingAttempts > 1) {
                Log.w(TAG, "ComposeView.setContent NPE; retrying (${remainingAttempts - 1} attempts left)", npe)
                composeView.post { applyComposeContentWithRetry(composeView, content, remainingAttempts - 1) }
            } else {
                Log.e(TAG, "ComposeView.setContent failed after retries", npe)
                throw npe
            }
        }
    }

    private fun requestUserConsentAndInitializeAds() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val consentParamsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            val debugSettingsBuilder = ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            if (CONSENT_TEST_DEVICE_HASH.isNotBlank()) {
                debugSettingsBuilder.addTestDeviceHashedId(CONSENT_TEST_DEVICE_HASH)
            }
            consentParamsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
        }

        val consentParams = consentParamsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            this,
            consentParams,
            {
                loadAndShowConsentFormIfNeeded(consentInformation)
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                startAdsAfterConsentIfAllowed(consentInformation)
            }
        )
    }

    private fun loadAndShowConsentFormIfNeeded(consentInformation: ConsentInformation) {
        if (consentInformation.canRequestAds()) {
            startAdsAfterConsentIfAllowed(consentInformation)
            return
        }

        UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError: FormError? ->
            formError?.let {
                Log.w(TAG, "Consent form error: ${it.message}")
            }
            startAdsAfterConsentIfAllowed(consentInformation)
        }
    }

    private fun startAdsAfterConsentIfAllowed(consentInformation: ConsentInformation) {
        if (!consentInformation.canRequestAds()) {
            Log.w(TAG, "Consent not granted yet. Skipping AdMob initialization.")
            return
        }
        startAdsAfterConsent()
    }

    private fun startAdsAfterConsent() {
        if (adsInitializationTriggered) return
        adsInitializationTriggered = true
        MobileAds.initialize(this) { initializationStatus ->
            AdManager.initialize(this, initializationStatus)
        }
    }
    
    /**
     * 購入状態をDataStoreに同期
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
    
    override fun onResume() {
        super.onResume()
        // 中断された更新をチェック
        updateManager?.let { manager ->
            lifecycleScope.launch {
                manager.checkUpdateInProgress(this@MainActivity)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
        updateManager?.cleanup()
    }
    
    /**
     * アップデートコールバック設定
     */
    private fun setupUpdateCallbacks(manager: UpdateManager) {
        manager.onDownloadProgress = { progress ->
            android.util.Log.d("MainActivity", "Update download progress: $progress%")
        }
        
        manager.onDownloadCompleted = {
            runOnUiThread {
                android.util.Log.d("MainActivity", "Update downloaded. Restart to install.")
                // ユーザーに再起動を促す
                manager.completeUpdate()
            }
        }
        
        manager.onUpdateFailed = { error ->
            runOnUiThread {
                android.util.Log.d("MainActivity", "Update failed: $error")
            }
        }
    }
    
    /**
     * アップデートチェック
     */
    private suspend fun checkForAppUpdate(manager: UpdateManager) {
        when (val result = manager.checkForUpdate()) {
            is UpdateCheckResult.NoUpdateAvailable -> {
                android.util.Log.d("MainActivity", "No update available")
            }
            is UpdateCheckResult.FlexibleUpdateAvailable -> {
                android.util.Log.d("MainActivity", "Flexible update available")
                manager.startFlexibleUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.ImmediateUpdateAvailable -> {
                android.util.Log.d("MainActivity", "Immediate update available")
                manager.startImmediateUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.ImmediateUpdateRequired -> {
                android.util.Log.d("MainActivity", "Immediate update REQUIRED (force update)")
                manager.startImmediateUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.UpdateNotAllowed -> {
                android.util.Log.w("MainActivity", "Update not allowed: ${result.reason}")
            }
            is UpdateCheckResult.Error -> {
                android.util.Log.e("MainActivity", "Update check error: ${result.message}")
            }
        }
    }

    private fun isInstalledFromPlayStore(): Boolean {
        return try {
            val installer = packageManager.getInstallerPackageName(packageName)
            installer == "com.android.vending" || installer == "com.google.android.feedback"
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to determine installer package", e)
            false
        }
    }
}
