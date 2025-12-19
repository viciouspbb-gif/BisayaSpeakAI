package com.bisayaspeak.ai

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.data.PurchaseStore
import com.bisayaspeak.ai.ui.ads.AdMobManager
import com.bisayaspeak.ai.ui.navigation.AppNavGraph
import com.bisayaspeak.ai.ui.theme.BisayaSpeakAITheme
import com.bisayaspeak.ai.update.UpdateCheckResult
import com.bisayaspeak.ai.update.UpdateManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var billingManager: BillingManager
    private lateinit var purchaseStore: PurchaseStore
    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AdMob初期化（テストデバイスID設定含む）
        AdMobManager.initialize(this)
        
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
        
        // アップデートマネージャー初期化
        updateManager = UpdateManager(this)
        setupUpdateCallbacks()
        
        // アプリ起動時にアップデートチェック
        lifecycleScope.launch {
            checkForAppUpdate()
        }

        setContent {
            BisayaSpeakAITheme {

                val navController = rememberNavController()

                // Debug時のみ Premiumトグル有効
                var isPremium by remember { mutableStateOf(false) }

                AppNavGraph(
                    navController = navController,
                    isPremium = isPremium,
                    showPremiumTestToggle = BuildConfig.DEBUG,
                    onTogglePremiumTest = { isPremium = !isPremium }
                )
            }
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
        lifecycleScope.launch {
            updateManager.checkUpdateInProgress(this@MainActivity)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
        updateManager.cleanup()
    }
    
    /**
     * アップデートコールバック設定
     */
    private fun setupUpdateCallbacks() {
        updateManager.onDownloadProgress = { progress ->
            android.util.Log.d("MainActivity", "Update download progress: $progress%")
        }
        
        updateManager.onDownloadCompleted = {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Update downloaded. Restart to install.",
                    Toast.LENGTH_LONG
                ).show()
                // ユーザーに再起動を促す
                updateManager.completeUpdate()
            }
        }
        
        updateManager.onUpdateFailed = { error ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Update failed: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * アップデートチェック
     */
    private suspend fun checkForAppUpdate() {
        when (val result = updateManager.checkForUpdate()) {
            is UpdateCheckResult.NoUpdateAvailable -> {
                android.util.Log.d("MainActivity", "No update available")
            }
            is UpdateCheckResult.FlexibleUpdateAvailable -> {
                android.util.Log.d("MainActivity", "Flexible update available")
                updateManager.startFlexibleUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.ImmediateUpdateAvailable -> {
                android.util.Log.d("MainActivity", "Immediate update available")
                updateManager.startImmediateUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.ImmediateUpdateRequired -> {
                android.util.Log.d("MainActivity", "Immediate update REQUIRED (force update)")
                updateManager.startImmediateUpdate(this, result.appUpdateInfo)
            }
            is UpdateCheckResult.UpdateNotAllowed -> {
                android.util.Log.w("MainActivity", "Update not allowed: ${result.reason}")
            }
            is UpdateCheckResult.Error -> {
                android.util.Log.e("MainActivity", "Update check error: ${result.message}")
            }
        }
    }
}
