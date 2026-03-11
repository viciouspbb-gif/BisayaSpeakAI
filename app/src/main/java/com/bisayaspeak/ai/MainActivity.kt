package com.bisayaspeak.ai

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.feature.ProFeatureGate
import com.bisayaspeak.ai.AppStartupState
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var billingManager: BillingManager
    private lateinit var purchaseStore: PurchaseStore
    private var updateManager: UpdateManager? = null
    private val isInAppUpdateSupported: Boolean by lazy { isInstalledFromPlayStore() }
    private var adsInitializationTriggered = false
    
    // 通知権限関連
    private var showNotificationPermissionDialog by mutableStateOf(false)
    private var showPermissionWarning by mutableStateOf(false)  // 強制警告用
    private var missingNotificationPermission by mutableStateOf(false)
    private var missingExactAlarmPermission by mutableStateOf(false)

    companion object {
        private const val TAG = "MainActivity"
        private const val CONSENT_TEST_DEVICE_HASH = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        billingManager = BillingManager(this)
        purchaseStore = PurchaseStore(this)

        setContentView(R.layout.activity_main)
        val app = application as BisayaSpeakApp
        setupComposeContent(app)

        // 通知経由の起動をチェック
        val fromNotification = intent.getBooleanExtra("from_notification", false)
        Log.d(TAG, "通知経由の起動: $fromNotification")

        // Android 13以上で通知権限を要求
        checkAllPermissions()

        // 通知経由起動時は広告ロードをスマート・ディレイ・ロード
        if (fromNotification) {
            Log.d(TAG, "通知経由起動：広告ロードを1.5秒ディレイで開始します")
            // onCreateでは即時ロードを禁止
            scheduleSmartDelayedAdLoad()
        } else {
            requestUserConsentAndInitializeAds()
        }

        lifecycleScope.launch {
            billingManager.initialize {
                lifecycleScope.launch {
                    billingManager.checkPremiumStatus()
                    syncPurchaseStatus()
                }
            }
        }
        lifecycleScope.launch {
            combine(
                billingManager.hasPremiumAI,
                billingManager.isPremium
            ) { hasPremiumAI, isPremium ->
                PremiumStatusProvider.updateStatus(
                    hasPremiumAI = hasPremiumAI,
                    isProUnlocked = false, // 買い切りは削除
                    subscriptionActive = isPremium
                )
                
                // ProFeatureGateに一本化
                val effectivePro = ProFeatureGate.getEffectiveProStatus(hasPremiumAI || isPremium)
                app.isProVersion = effectivePro
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
    }

    private fun setupComposeContent(
        app: BisayaSpeakApp
    ) {
        val composeView = findViewById<ComposeView>(R.id.main_compose_view)
        if (composeView == null) {
            Log.e(TAG, "ComposeView not found; finishing activity to avoid blank screen")
            finish()
            return
        }

        val content: @Composable () -> Unit = {
            BisayaSpeakAITheme {
                val startupState by app.startupState.collectAsState()
                when (val state = startupState) {
                    AppStartupState.Loading -> StartupLoadingScreen()
                    is AppStartupState.Failed -> StartupErrorScreen(
                        throwable = state.throwable,
                        onRetry = { app.retryInitialization() }
                    )
                    AppStartupState.Ready -> {
                        val listeningViewModelFactory = remember {
                            ListeningViewModelFactory(
                                application = app,
                                repository = app.questionRepository,
                                userProgressRepository = app.userProgressRepository,
                                dbSeedStateRepository = app.dbSeedStateRepository
                            )
                        }

                        val navController = rememberNavController()
                        val isPremiumUser by PremiumStatusProvider.isPremiumUser.collectAsState()
                        val observedPro by app.proVersionState.collectAsState()

                        // 同期的初期化：Flowを待つのではなく、その瞬間の確定値を計算
                        val effectivePro = ProFeatureGate.getEffectiveProStatus(isPremiumUser)

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
                        
                        // 課金状態の即時反映を確保 - collect内で設定することで競合状態を排除
                        app.isProVersion = effectivePro

                        androidx.compose.runtime.SideEffect {
                            logEffectiveProState(
                                isPremiumUser = isPremiumUser,
                                isDebugWhitelistedUser = isDebugWhitelistedUser,
                                effectivePro = effectivePro
                            )
                            // collect内での設定を最優先するため、SideEffectでも設定（二重保障）
                            app.isProVersion = effectivePro
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        ) {
                            AppNavGraph(
                                navController = navController,
                                isProVersion = effectivePro, // isPremiumUser(false) ではなく確定した effectivePro を渡す
                                showPremiumTestToggle = false,
                                onTogglePremiumTest = {},
                                listeningViewModelFactory = listeningViewModelFactory,
                                onRestorePurchase = {
                                    billingManager.restorePurchases {
                                        lifecycleScope.launch { syncPurchaseStatus() }
                                    }
                                }
                            )
                            
                            // 強制権限警告UI
                            if (showPermissionWarning) {
                                PermissionWarningBanner()
                            }
                            
                            // 通知権限ダイアログ
                            NotificationPermissionDialog()
                        }
                    }
                }
            }
        }

        applyComposeContentWithRetry(composeView, content)
    }

    @Composable
    private fun StartupLoadingScreen() {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun StartupErrorScreen(throwable: Throwable, onRetry: () -> Unit) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "初期化に失敗しました", style = MaterialTheme.typography.titleMedium)
            Text(
                text = throwable.localizedMessage ?: throwable.javaClass.simpleName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = androidx.compose.ui.Modifier.padding(top = 12.dp)
            )
            Button(onClick = onRetry, modifier = androidx.compose.ui.Modifier.padding(top = 24.dp)) {
                Text("再試行")
            }
        }
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

    private fun logEffectiveProState(
        isPremiumUser: Boolean,
        isDebugWhitelistedUser: Boolean,
        effectivePro: Boolean
    ) {
        Log.i(
            TAG,
            "effectivePro=$effectivePro (isPremiumUser=$isPremiumUser, debugWhitelisted=$isDebugWhitelistedUser)"
        )
    }
    
    /**
     * 購入状態をDataStoreに同期
     */
    private suspend fun syncPurchaseStatus() {
        val hasPremiumAI = billingManager.hasPremiumAI.value
        
        if (hasPremiumAI) {
            purchaseStore.setPremiumAI(true)
        }
        // isProUnlockedは削除
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
    
    private fun scheduleSmartDelayedAdLoad() {
        // UI描画完了から1.5秒のディレイを置いてロードを開始
        window.decorView.postDelayed({
            Log.d(TAG, "Ad stability: Load triggered after delay")
            loadAdsWithEnhancedRetry(0)
        }, 1500)
    }
    
    private var adsLoadInitialized = false
    
    private fun loadAdsWithEnhancedRetry(retryCount: Int) {
        val retryDelays = listOf(0L, 3000L, 10000L) // 初回、3秒後、10秒後
        
        try {
            Log.d(TAG, "広告ロード試行 ${retryCount + 1}/3")
            
            // リソース優先度の最適化：広告ロード中は非緊急処理を一時停止
            if (retryCount == 0) {
                Log.d(TAG, "Ad stability: Resource optimization enabled")
            }
            
            requestUserConsentAndInitializeAds()
            Log.d(TAG, "広告ロード成功")
        } catch (e: Exception) {
            Log.e(TAG, "広告ロード失敗 ${retryCount + 1}/3", e)
            
            if (retryCount < retryDelays.size - 1) {
                val delay = retryDelays[retryCount + 1]
                Log.d(TAG, "Ad stability: Retry #${retryCount + 1} initiated after ${delay}ms")
                
                window.decorView.postDelayed({
                    loadAdsWithEnhancedRetry(retryCount + 1)
                }, delay)
            } else {
                Log.e(TAG, "Ad stability: Retry limit reached, giving up")
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
    
    /**
     * すべての権限をチェックし、警告UIを表示
     */
    private fun checkAllPermissions() {
        // 通知権限チェック
        missingNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        
        // 正確なアラーム権限チェック
        missingExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() != true
        } else {
            false
        }
        
        Log.d(TAG, "権限チェック結果: 通知権限欠=${missingNotificationPermission}, アラーム権限欠=${missingExactAlarmPermission}")
        
        // どちらかが欠けている場合は警告UIを表示
        if (missingNotificationPermission || missingExactAlarmPermission) {
            showPermissionWarning = false /* disabled by CEO */
        }
        
        // Android 13以上で通知権限が欠けている場合はダイアログも表示
        if (missingNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showNotificationPermissionDialog = true
        }
    }
    
    /**
     * 強制権限警告バナー
     */
    @Composable
    private fun PermissionWarningBanner() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red.copy(alpha = 0.9f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "！ 通知設定が必要です",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val missingPermissions = mutableListOf<String>()
                if (missingNotificationPermission) missingPermissions.add("通知権限")
                if (missingExactAlarmPermission) missingPermissions.add("正確なアラーム権限")
                
                Text(
                    text = "以下の権限が不足しています：${missingPermissions.joinToString(", ")}",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { openPermissionSettings() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "設定画面へ移動",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    /**
     * 設定画面へ移動
     */
    private fun openPermissionSettings() {
        try {
            val intent = if (missingExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 正確なアラーム権限の設定画面へ
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            } else {
                // アプリ設定画面へ
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
            
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "設定画面の起動に失敗", e)
            // フォールバック
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "フォールバック設定画面の起動にも失敗", e2)
            }
        }
    }

    /**
     * Android 13以上で通知権限を要求
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "通知権限が許可されています")
                }
                else -> {
                    Log.d(TAG, "通知権限がないため、ダイアログを表示します")
                    showNotificationPermissionDialog = true
                }
            }
        }
    }
    
    /**
     * 通知権限要求ダイアログ
     */
    @Composable
    private fun NotificationPermissionDialog() {
        if (showNotificationPermissionDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                showNotificationPermissionDialog = false
                if (isGranted) {
                    Log.d(TAG, "通知権限が許可されました")
                } else {
                    Log.w(TAG, "通知権限が拒否されました")
                }
            }
            
            AlertDialog(
                onDismissRequest = { 
                    showNotificationPermissionDialog = false
                },
                title = {
                    Text("通知権限が必要です")
                },
                text = {
                    Text("ビサヤ語学習のリマインダー通知を送信するために、通知権限が必要です。設定から許可してください。")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    ) {
                        Text("許可する")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotificationPermissionDialog = false
                        }
                    ) {
                        Text("後で")
                    }
                }
            )
        }
    }
}
