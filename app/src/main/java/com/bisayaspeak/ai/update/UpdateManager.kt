package com.bisayaspeak.ai.update

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import kotlinx.coroutines.tasks.await

/**
 * In-App Updates管理クラス
 */
class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val UPDATE_REQUEST_CODE = 1001
        
        // 最小サポートバージョン（これ未満は強制更新）
        const val MIN_SUPPORTED_VERSION = 1
    }
    
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    
    // コールバック
    var onDownloadProgress: ((Int) -> Unit)? = null
    var onDownloadCompleted: (() -> Unit)? = null
    var onUpdateFailed: ((String) -> Unit)? = null
    
    // Flexible更新の進捗リスナー
    private val installStateUpdatedListener = object : InstallStateUpdatedListener {
        override fun onStateUpdate(state: com.google.android.play.core.install.InstallState) {
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    val progress = (bytesDownloaded * 100 / totalBytesToDownload).toInt()
                    Log.d(TAG, "Downloading: $progress%")
                    onDownloadProgress?.invoke(progress)
                }
                InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Download completed, ready to install")
                    onDownloadCompleted?.invoke()
                }
                InstallStatus.INSTALLED -> {
                    Log.d(TAG, "Update installed")
                    cleanup()
                }
                InstallStatus.FAILED -> {
                    Log.e(TAG, "Update failed")
                    onUpdateFailed?.invoke("Update installation failed")
                    cleanup()
                }
                else -> {
                    Log.d(TAG, "Install status: ${state.installStatus()}")
                }
            }
        }
    }
    
    /**
     * アップデートをチェック
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            
            Log.d(TAG, "Update availability: ${appUpdateInfo.updateAvailability()}")
            Log.d(TAG, "Available version code: ${appUpdateInfo.availableVersionCode()}")
            Log.d(TAG, "Current version code: ${getCurrentVersionCode()}")
            
            when {
                // アップデート不要
                appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> {
                    UpdateCheckResult.NoUpdateAvailable
                }
                // 強制更新が必要（最小サポートバージョン未満）
                shouldForceUpdate(appUpdateInfo) -> {
                    if (appUpdateInfo.isImmediateUpdateAllowed) {
                        UpdateCheckResult.ImmediateUpdateRequired(appUpdateInfo)
                    } else {
                        UpdateCheckResult.UpdateNotAllowed("Immediate update not allowed")
                    }
                }
                // Flexible更新が利用可能
                appUpdateInfo.isFlexibleUpdateAllowed -> {
                    UpdateCheckResult.FlexibleUpdateAvailable(appUpdateInfo)
                }
                // Immediate更新が利用可能
                appUpdateInfo.isImmediateUpdateAllowed -> {
                    UpdateCheckResult.ImmediateUpdateAvailable(appUpdateInfo)
                }
                else -> {
                    UpdateCheckResult.UpdateNotAllowed("No update type allowed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for update", e)
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 強制更新が必要かチェック
     */
    private fun shouldForceUpdate(appUpdateInfo: AppUpdateInfo): Boolean {
        val currentVersion = getCurrentVersionCode()
        return currentVersion < MIN_SUPPORTED_VERSION
    }
    
    /**
     * 現在のバージョンコードを取得
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            0
        }
    }
    
    /**
     * Flexible更新を開始
     */
    fun startFlexibleUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo): Boolean {
        return try {
            appUpdateManager.registerListener(installStateUpdatedListener)
            
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                UPDATE_REQUEST_CODE
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start flexible update", e)
            onUpdateFailed?.invoke(e.message ?: "Failed to start update")
            false
        }
    }
    
    /**
     * Immediate更新を開始
     */
    fun startImmediateUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo): Boolean {
        return try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                UPDATE_REQUEST_CODE
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start immediate update", e)
            onUpdateFailed?.invoke(e.message ?: "Failed to start update")
            false
        }
    }
    
    /**
     * ダウンロード済み更新をインストール
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
    
    /**
     * 更新が中断されていないかチェック（アプリ再開時）
     */
    suspend fun checkUpdateInProgress(activity: Activity) {
        try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(TAG, "Update downloaded, prompting to install")
                onDownloadCompleted?.invoke()
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Immediate更新が中断されていた場合、再開
                if (appUpdateInfo.isImmediateUpdateAllowed) {
                    startImmediateUpdate(activity, appUpdateInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update in progress", e)
        }
    }
    
    /**
     * リソースをクリーンアップ
     */
    fun cleanup() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}

/**
 * アップデートチェック結果
 */
sealed class UpdateCheckResult {
    object NoUpdateAvailable : UpdateCheckResult()
    data class FlexibleUpdateAvailable(val appUpdateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class ImmediateUpdateAvailable(val appUpdateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class ImmediateUpdateRequired(val appUpdateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class UpdateNotAllowed(val reason: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
