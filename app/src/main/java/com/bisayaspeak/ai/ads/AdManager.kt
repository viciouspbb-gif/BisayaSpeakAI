package com.bisayaspeak.ai.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.ads.AdsPolicy

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {

    private const val TAG = "LearnBisaya"
    private const val DEBUG_TAG = "LearnBisaya"

    private inline fun logDebug(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    private inline fun logFlow(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(DEBUG_TAG, message())
        }
    }

    // テスト用ID
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"

    private val AD_UNIT_ID_INTERSTITIAL = AdUnitIds.INTERSTITIAL_MAIN
    private val AD_UNIT_ID_REWARD = AdUnitIds.REWARDED_MAIN

    private const val INIT_DELAY_MS = 1_000L
    private const val MIN_BACKOFF_MS = 2_000L
    private const val MAX_BACKOFF_MS = 32_000L

    enum class InterstitialAttemptResult {
        SHOWN,
        NOT_READY,
        FAILED,
        SKIPPED_BY_POLICY
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initializationJob: Job? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var playCounter = 0
    private var interstitialRetryDelayMs = MIN_BACKOFF_MS
    private var rewardRetryDelayMs = MIN_BACKOFF_MS
    private var isInitialized = false

    // ViewModel連携用コールバック
    private var adLoadCallback: ((Boolean) -> Unit)? = null
    private var rewardLoadCallback: ((Boolean) -> Unit)? = null
    private var interstitialLoadCallback: ((Boolean) -> Unit)? = null

    fun initialize(
        context: Context,
        initializationStatus: InitializationStatus? = null,
        requestConfiguration: RequestConfiguration? = null
    ) {
        logDebug { "initialize() invoked" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, skipping initialization")
            return
        }

        if (isInitialized) {
            Log.w(TAG, "Already initialized. Skipping duplicate call.")
            return
        }

        if (!BuildConfig.DEBUG && BuildConfig.IS_LITE_BUILD) {
            Log.w(
                TAG,
                "Lite release build detected. Register test devices before manual ad clicks to avoid policy violations."
            )
        }

        initializationJob = scope.launch {
            try {
                logDebug { "Marking AdMob as initialized (adapters=${initializationStatus?.adapterStatusMap?.keys})" }
                delay(INIT_DELAY_MS)

                requestConfiguration?.let { config ->
                    try {
                        MobileAds.setRequestConfiguration(config)
                    } catch (configError: Exception) {
                        Log.e(TAG, "Failed to set request configuration: ${configError.message}", configError)
                    }
                }
                isInitialized = true
                val appContext = context.applicationContext
                scope.launch { loadInterstitialInternal(appContext) }
                scope.launch { loadRewardInternal(appContext) }
            } catch (e: Exception) {
                Log.e(TAG, "Ad initialization marking failed: ${e.message}", e)
            }
        }
    }

    fun loadInterstitial(context: Context) {
        logDebug { "loadInterstitial()" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, skipping interstitial load")
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized yet. Skipping interstitial load request.")
            return
        }
        scope.launch {
            loadInterstitialInternal(context.applicationContext)
        }
    }

    fun loadReward(context: Context) {
        logDebug { "loadReward()" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, skipping reward load")
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized yet. Skipping reward load request.")
            return
        }
        scope.launch {
            loadRewardInternal(context.applicationContext)
        }
    }

    private suspend fun loadInterstitialInternal(context: Context) {
        logDebug { "loadInterstitialInternal()" }
        try {
            withContext(Dispatchers.Main) {
                logDebug { "Loading interstitial ad" }
                val adRequest = AdRequest.Builder().build()

                InterstitialAd.load(
                    context,
                    AD_UNIT_ID_INTERSTITIAL,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            logDebug { "Interstitial onAdLoaded" }
                            interstitialAd = ad
                            interstitialRetryDelayMs = MIN_BACKOFF_MS
                            logDebug { "Interstitial loaded: responseInfo=${ad.responseInfo}" }
                            interstitialLoadCallback?.invoke(true)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.w(TAG, "Interstitial failed: code=${adError.code}, message=${adError.message}")
                            interstitialAd = null
                            scheduleInterstitialRetry(context)
                            interstitialLoadCallback?.invoke(false)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load interstitial: ${e.message}", e)
            scheduleInterstitialRetry(context)
            interstitialLoadCallback?.invoke(false)
        }
    }

    private suspend fun loadRewardInternal(context: Context) {
        logDebug { "loadRewardInternal()" }
        val adUnitId = AD_UNIT_ID_REWARD

        if (adUnitId.isBlank()) {
            Log.e(TAG, "Rewarded ad unit id is blank. Falling back to retry.")
            scheduleRewardRetry(context)
            return
        }
        try {
            withContext(Dispatchers.Main) {
                logDebug { "Loading rewarded ad (unit=$adUnitId)" }
                val adRequest = AdRequest.Builder().build()

                RewardedAd.load(
                    context,
                    adUnitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            rewardRetryDelayMs = MIN_BACKOFF_MS
                            rewardLoadCallback?.invoke(true)
                            logDebug {
                                "Rewarded ad loaded: format=${ad.responseInfo?.loadedAdapterResponseInfo?.adError?.message ?: "n/a"}, response=${ad.responseInfo}"
                            }
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            rewardedAd = null
                            rewardLoadCallback?.invoke(false)
                            Log.w(
                                TAG,
                                "Rewarded load failed (unit=$adUnitId): code=${adError.code}, message=${adError.message}"
                            )
                            scheduleRewardRetry(context)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded ad: ${e.message}", e)
            rewardLoadCallback?.invoke(false)
            scheduleRewardRetry(context)
        }
    }

    private fun scheduleInterstitialRetry(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return
        val delayMs = interstitialRetryDelayMs
        interstitialRetryDelayMs = (interstitialRetryDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        logDebug { "Scheduling interstitial retry in $delayMs ms" }

        scope.launch {
            delay(delayMs)
            loadInterstitialInternal(context)
        }
    }

    private fun scheduleRewardRetry(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return
        val delayMs = rewardRetryDelayMs
        rewardRetryDelayMs = (rewardRetryDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        logDebug { "Scheduling reward retry in $delayMs ms" }

        scope.launch {
            delay(delayMs)
            loadRewardInternal(context)
        }
    }

    // 広告状態確認メソッド
    fun isRewardedAdReady(): Boolean {
        return rewardedAd != null
    }

    // 初期化状態確認メソッド
    fun isInitialized(): Boolean {
        return isInitialized
    }

    // ViewModel連携用コールバック設定
    fun setAdLoadCallback(callback: (Boolean) -> Unit) {
        adLoadCallback = callback
    }

    fun setRewardLoadCallback(callback: (Boolean) -> Unit) {
        rewardLoadCallback = callback
    }

    fun setInterstitialLoadCallback(callback: ((Boolean) -> Unit)?) {
        interstitialLoadCallback = callback
    }

    fun showInterstitialNow(activity: Activity, onAdClosed: () -> Unit) {
        logDebug { "showInterstitialNow()" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, calling onAdClosed immediately")
            onAdClosed.safeInvoke()
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "Interstitial requested before initialization. Ignoring show request.")
            onAdClosed.safeInvoke()
            return
        }
        try {
            val ad = interstitialAd
            if (ad == null) {
                Log.w(TAG, "Interstitial not ready, skipping show request")
                loadInterstitial(activity.applicationContext)

                onAdClosed.safeInvoke()
                return
            }

            logDebug { "Showing interstitial ad" }
            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            logDebug { "Interstitial dismissed" }
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial show failed: ${adError.message}")
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while showing interstitial: ${e.message}", e)
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "showInterstitialNow failed: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    fun showInterstitialWithTimeout(
        activity: Activity,
        timeoutMs: Long = 2_000L,
        onAdClosed: () -> Unit,
        onAttemptResult: ((InterstitialAttemptResult) -> Unit)? = null
    ) {
        logDebug { "showInterstitialWithTimeout(timeoutMs=$timeoutMs)" }
        logFlow { "showInterstitialWithTimeout 呼び出し (timeoutMs=$timeoutMs)" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, calling onAdClosed immediately")
            logFlow { "Ads無効のため即座にonAdClosed" }
            onAttemptResult?.invoke(InterstitialAttemptResult.SKIPPED_BY_POLICY)
            onAdClosed.safeInvoke()
            return
        }
        if (!isInitialized) {
            Log.w(TAG, "Interstitial timeout show requested before initialization. Skipping.")
            onAttemptResult?.invoke(InterstitialAttemptResult.NOT_READY)
            onAdClosed.safeInvoke()
            return
        }

        val finished = AtomicBoolean(false)
        val resultDelivered = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        logFlow { "2秒タイマー作動開始" }

        val timeoutRunnable = Runnable {
            logFlow { "タイムアウト発生 -> navigateToResult 強制呼び出し" }
            if (finished.compareAndSet(false, true)) {
                Log.w(TAG, "Interstitial timeout reached ($timeoutMs ms)")
                onAdClosed.safeInvoke()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        fun finish(reason: String) {
            if (finished.compareAndSet(false, true)) {
                logFlow { "finish 呼び出し reason=$reason -> onAdClosed 実行" }
                logDebug { "showInterstitialWithTimeout finished ($reason)" }
                handler.removeCallbacks(timeoutRunnable)
                onAdClosed.safeInvoke()
            }
        }

        fun deliverResult(result: InterstitialAttemptResult) {
            if (resultDelivered.compareAndSet(false, true)) {
                onAttemptResult?.invoke(result)
            }
        }

        try {
            val ad = interstitialAd
            if (ad == null) {
                Log.w(TAG, "Interstitial not ready before timeout show")
                logFlow { "interstitialAd == null -> 即座にonAdClosed" }
                loadInterstitial(activity.applicationContext)
                deliverResult(InterstitialAttemptResult.NOT_READY)
                finish("interstitial_not_ready_before_show")
                return
            }

            logDebug { "Showing interstitial with timeout" }
            logFlow { "interstitialAd.show() 実行 (メインスレッドへpost)" }
            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            logDebug { "Interstitial dismissed (timeout variant)" }
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            deliverResult(InterstitialAttemptResult.SHOWN)
                            finish("onAdDismissedFullScreenContent")
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Interstitial timeout show failed: ${adError.message}")
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            deliverResult(InterstitialAttemptResult.FAILED)
                            finish("onAdFailedToShowFullScreenContent")
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while showing interstitial with timeout: ${e.message}", e)
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    deliverResult(InterstitialAttemptResult.FAILED)
                    finish("exception_in_runOnUiThread")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "showInterstitialWithTimeout failed: ${e.message}", e)
            deliverResult(InterstitialAttemptResult.FAILED)
            finish("exception_before_show")
        }
    }

    fun checkAndShowInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        logDebug { "checkAndShowInterstitial()" }
        if (!AdsPolicy.areAdsEnabled) {
            Log.i(TAG, "Ads disabled, calling onAdClosed immediately")
            onAdClosed.safeInvoke()
            return
        }

        if (!isInitialized) {
            Log.w("AdManager", "checkAndShowInterstitial invoked before initialization. Skipping.")
            onAdClosed.safeInvoke()
            return
        }
        try {
            playCounter++
            if (playCounter % 2 != 0) {

                onAdClosed.safeInvoke()
                return
            }

            val ad = interstitialAd
            if (ad == null) {
                loadInterstitial(activity.applicationContext)

                onAdClosed.safeInvoke()
                return
            }

            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            Log.e(TAG, "Interstitial show failed: ${adError.message}")
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    interstitialAd = null
                    Log.e(TAG, "Error showing interstitial: ${e.message}", e)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndShowInterstitial failed: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    fun showRewardAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (!AdsPolicy.areAdsEnabled) {
            onAdClosed.safeInvoke()
            return
        }
        if (!isInitialized) {
            Log.w("AdManager", "Reward ad requested before initialization. Skipping show.")
            onAdClosed.safeInvoke()
            return
        }

        try {
            val ad = rewardedAd
            if (ad == null) {
                Log.w(TAG, "Rewarded ad not ready")
                loadReward(activity.applicationContext)
                onAdClosed.safeInvoke()
                return
            }

            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadReward(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedAd = null
                            Log.e(TAG, "Rewarded show failed: ${adError.message}")
                            loadReward(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }
                    }
                    ad.show(activity) { _ ->

                        try {
                            onRewardEarned()
                        } catch (callbackError: Exception) {
                            Log.e("AdManager", "Reward callback failed: ${callbackError.message}", callbackError)
                        }
                    }
                } catch (e: Exception) {
                    rewardedAd = null
                    Log.e(TAG, "Error showing rewarded ad: ${e.message}", e)
                    loadReward(activity.applicationContext)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "showRewardAd failed: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    private fun (() -> Unit).safeInvoke() {
        try {
            this()
        } catch (e: Exception) {
            Log.e(TAG, "Callback execution failed: ${e.message}", e)
        }
    }
}