package com.bisayaspeak.ai.ui.util

import android.app.Activity
import android.util.Log
import com.bisayaspeak.ai.ads.AdManager

/**
 * 全練習モード共通の広告管理マネージャー
 * 
 * 統一ルール：
 * - 1セット完了 = 1回広告表示（必ず）
 * - 中断時 = 1回広告表示（必ず）
 * - 通信エラー後のリトライ成功 = 1回広告表示（必ず）
 * 
 * 旧仕様（削除）：
 * - 10回連続正解で広告
 * - 2回ミスで広告
 */
class PracticeSessionManager(
    private val isPremium: Boolean
) {
    private var sessionStarted = false
    private var sessionCompleted = false
    private var adShownForSession = false
    
    companion object {
        private const val TAG = "PracticeSessionManager"
    }
    
    /**
     * セッション開始を記録
     */
    fun startSession() {
        sessionStarted = true
        sessionCompleted = false
        adShownForSession = false
        Log.d(TAG, "Session started")
    }
    
    /**
     * セッション完了時の広告表示
     * 1セット完了 = 1回広告（必ず）
     */
    fun onSessionComplete(activity: Activity?, onAdDismissed: () -> Unit = {}) {
        if (isPremium) {
            Log.d(TAG, "Premium user - no ad on session complete")
            onAdDismissed()
            return
        }
        
        if (!sessionStarted) {
            Log.w(TAG, "Session not started - skipping ad")
            onAdDismissed()
            return
        }
        
        if (adShownForSession) {
            Log.d(TAG, "Ad already shown for this session")
            onAdDismissed()
            return
        }
        
        sessionCompleted = true
        adShownForSession = true
        
        Log.d(TAG, "Showing interstitial ad on session complete")
        activity?.let { safeActivity ->
            AdManager.showInterstitialWithTimeout(
                activity = safeActivity,
                timeoutMs = 3_000L,
                onAdClosed = {
                    AdManager.loadInterstitial(safeActivity.applicationContext)
                    onAdDismissed()
                }
            )
        } ?: onAdDismissed()
    }
    
    /**
     * 中断時の広告表示
     * 画面遷移/バック/ホーム = 1回広告（必ず）
     */
    fun onSessionInterrupted(activity: Activity?, onAdDismissed: () -> Unit = {}) {
        if (isPremium) {
            Log.d(TAG, "Premium user - no ad on interruption")
            onAdDismissed()
            return
        }
        
        if (!sessionStarted) {
            Log.d(TAG, "Session not started - skipping ad on interruption")
            onAdDismissed()
            return
        }
        
        if (sessionCompleted) {
            Log.d(TAG, "Session already completed - skipping ad on interruption")
            onAdDismissed()
            return
        }
        
        if (adShownForSession) {
            Log.d(TAG, "Ad already shown for this session")
            onAdDismissed()
            return
        }
        
        adShownForSession = true
        
        Log.d(TAG, "Showing interstitial ad on session interruption")
        activity?.let { safeActivity ->
            AdManager.showInterstitialWithTimeout(
                activity = safeActivity,
                timeoutMs = 3_000L,
                onAdClosed = {
                    AdManager.loadInterstitial(safeActivity.applicationContext)
                    onAdDismissed()
                }
            )
        } ?: onAdDismissed()
    }
    
    /**
     * エラー後のリトライ成功時の広告表示
     * 通信エラー → リトライ成功 = 1回広告（必ず）
     */
    fun onRetrySuccess(activity: Activity?, onAdDismissed: () -> Unit = {}) {
        if (isPremium) {
            Log.d(TAG, "Premium user - no ad on retry success")
            onAdDismissed()
            return
        }
        
        if (!sessionStarted) {
            Log.w(TAG, "Session not started - skipping ad on retry")
            onAdDismissed()
            return
        }
        
        Log.d(TAG, "Showing interstitial ad on retry success")
        activity?.let { safeActivity ->
            AdManager.showInterstitialWithTimeout(
                activity = safeActivity,
                timeoutMs = 3_000L,
                onAdClosed = {
                    AdManager.loadInterstitial(safeActivity.applicationContext)
                    onAdDismissed()
                }
            )
        } ?: onAdDismissed()
    }
    
    /**
     * セッションリセット
     */
    fun reset() {
        sessionStarted = false
        sessionCompleted = false
        adShownForSession = false
        Log.d(TAG, "Session reset")
    }
    
    /**
     * デバッグ情報取得
     */
    fun getDebugInfo(): String {
        return "SessionManager[started=$sessionStarted, completed=$sessionCompleted, adShown=$adShownForSession, premium=$isPremium]"
    }
}
