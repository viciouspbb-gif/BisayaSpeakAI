package com.bisayaspeak.ai.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.bisayaspeak.ai.ui.ads.AdsPolicy
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

object AdManager {

    // テスト用ID
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"

    private const val AD_UNIT_ID_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val AD_UNIT_ID_REWARD = "ca-app-pub-3940256099942544/5224354917"

    private const val INIT_DELAY_MS = 1_000L
    private const val MIN_BACKOFF_MS = 2_000L
    private const val MAX_BACKOFF_MS = 32_000L

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

    fun initialize(context: Context) {
        Log.e("AdManager", "★★★ initialize CALLED ★★★")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, skipping initialization")
            return
        }
        if (initializationJob?.isActive == true) {
            Log.e("AdManager", "Initialization already in progress")
            return
        }

        val appContext = context.applicationContext
        initializationJob = scope.launch {
            try {
                Log.e("AdManager", "Starting initialization with delay ${INIT_DELAY_MS}ms")
                delay(INIT_DELAY_MS)
                withContext(Dispatchers.Main) {
                    Log.e("AdManager", "Calling MobileAds.initialize...")
                    MobileAds.initialize(appContext) {
                        Log.e("AdManager", "MobileAds.initialize completed: adapters=${it.adapterStatusMap.keys}")
                        isInitialized = true
                        scope.launch { loadInterstitialInternal(appContext) }
                        scope.launch { loadRewardInternal(appContext) }
                    }
                }
            } catch (e: Exception) {
                Log.e("AdManager", "Ad initialization failed: ${e.message}", e)
            }
        }
    }

    fun loadInterstitial(context: Context) {
        Log.e("AdManager", "★★★ loadInterstitial CALLED ★★★")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, skipping interstitial load")
            return
        }
        if (!isInitialized) {
            Log.e("AdManager", "Not initialized yet, skipping interstitial load")
            return
        }
        scope.launch {
            loadInterstitialInternal(context.applicationContext)
        }
    }

    fun loadReward(context: Context) {
        Log.e("AdManager", "★★★ loadReward CALLED ★★★")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, skipping reward load")
            return
        }
        if (!isInitialized) {
            Log.e("AdManager", "Not initialized yet, skipping reward load")
            return
        }
        scope.launch {
            loadRewardInternal(context.applicationContext)
        }
    }

    private suspend fun loadInterstitialInternal(context: Context) {
        Log.e("AdManager", "★★★ loadInterstitialInternal CALLED ★★★")
        try {
            withContext(Dispatchers.Main) {
                Log.e("AdManager", "Loading interstitial ad...")
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    context,
                    AD_UNIT_ID_INTERSTITIAL,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            Log.e("AdManager", "★★★ onAdLoaded CALLED ★★★")
                            interstitialAd = ad
                            interstitialRetryDelayMs = MIN_BACKOFF_MS
                            Log.e("AdManager", "Interstitial ad loaded: responseInfo=${ad.responseInfo}")
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.e("AdManager", "★★★ onAdFailedToLoad CALLED ★★★")
                            interstitialAd = null
                            Log.e("AdManager", "Interstitial load failed: code=${adError.code}, message=${adError.message}, response=${adError.responseInfo}")
                            scheduleInterstitialRetry(context)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("AdManager", "Failed to load interstitial: ${e.message}", e)
            scheduleInterstitialRetry(context)
        }
    }

    private suspend fun loadRewardInternal(context: Context) {
        Log.e("AdManager", "★★★ loadRewardInternal CALLED ★★★")
        try {
            withContext(Dispatchers.Main) {
                Log.e("AdManager", "Loading rewarded ad...")
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    AD_UNIT_ID_REWARD,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            Log.e("AdManager", "★★★ onAdLoaded (rewarded) CALLED ★★★")
                            rewardedAd = ad
                            rewardRetryDelayMs = MIN_BACKOFF_MS
                            adLoadCallback?.invoke(true)
                            Log.e("AdManager", "Rewarded ad loaded: responseInfo=${ad.responseInfo}")
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.e("AdManager", "★★★ onAdFailedToLoad (rewarded) CALLED ★★★")
                            rewardedAd = null
                            adLoadCallback?.invoke(false)
                            Log.e("AdManager", "Rewarded load failed: code=${adError.code}, message=${adError.message}, response=${adError.responseInfo}")
                            scheduleRewardRetry(context)
                        }
                    })
            }
        } catch (e: Exception) {
            Log.e("AdManager", "Failed to load rewarded ad: ${e.message}", e)
            adLoadCallback?.invoke(false)
            scheduleRewardRetry(context)
        }
    }

    private fun scheduleInterstitialRetry(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return
        val delayMs = interstitialRetryDelayMs
        interstitialRetryDelayMs = (interstitialRetryDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        Log.e("AdManager", "Scheduling interstitial retry in $delayMs ms")
        scope.launch {
            delay(delayMs)
            loadInterstitialInternal(context)
        }
    }

    private fun scheduleRewardRetry(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return
        val delayMs = rewardRetryDelayMs
        rewardRetryDelayMs = (rewardRetryDelayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        Log.e("AdManager", "Scheduling reward retry in $delayMs ms")
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

    fun showInterstitialNow(activity: Activity, onAdClosed: () -> Unit) {
        Log.e("AdManager", "★★★ showInterstitialNow CALLED ★★★")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, calling onAdClosed immediately")
            onAdClosed.safeInvoke()
            return
        }
        try {
            val ad = interstitialAd
            if (ad == null) {
                Log.e("AdManager", "★★★ NO AD AVAILABLE ★★★, skipping ad and forcing navigation")
                loadInterstitial(activity.applicationContext)
                onAdClosed.safeInvoke()
                return
            }

            Log.e("AdManager", "★★★ SHOWING INTERSTITIAL AD ★★★")
            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.e("AdManager", "★★★ onAdDismissedFullScreenContent CALLED ★★★")
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdManager", "★★★ onAdFailedToShowFullScreenContent CALLED ★★★: ${adError.message}")
                            interstitialAd = null
                            Log.e("AdManager", "Interstitial show failed: ${adError.message}")
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.e("AdManager", "★★★ EXCEPTION IN SHOW AD ★★★: ${e.message}", e)
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "★★★ EXCEPTION IN showInterstitialNow ★★★: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    fun showInterstitialWithTimeout(
        activity: Activity,
        timeoutMs: Long = 2_000L,
        onAdClosed: () -> Unit
    ) {
        Log.e("AdManager", "★★★ showInterstitialWithTimeout CALLED ★★★ timeoutMs=$timeoutMs")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, calling onAdClosed immediately")
            onAdClosed.safeInvoke()
            return
        }

        val finished = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (finished.compareAndSet(false, true)) {
                Log.e("AdManager", "★★★ TIMEOUT REACHED ★★★ ($timeoutMs ms), forcing navigation")
                onAdClosed.safeInvoke()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)

        fun finish() {
            if (finished.compareAndSet(false, true)) {
                Log.e("AdManager", "★★★ FINISH CALLED ★★★")
                handler.removeCallbacks(timeoutRunnable)
                onAdClosed.safeInvoke()
            }
        }

        try {
            val ad = interstitialAd
            if (ad == null) {
                Log.e("AdManager", "★★★ NO AD AVAILABLE ★★★, skipping ad and forcing navigation")
                handler.removeCallbacks(timeoutRunnable)
                finish()
                loadInterstitial(activity.applicationContext)
                return
            }

            Log.e("AdManager", "★★★ SHOWING INTERSTITIAL AD ★★★")
            activity.runOnUiThread {
                try {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.e("AdManager", "★★★ onAdDismissedFullScreenContent CALLED ★★★")
                            interstitialAd = null
                            loadInterstitial(activity.applicationContext)
                            finish()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AdManager", "★★★ onAdFailedToShowFullScreenContent CALLED ★★★: ${adError.message}")
                            interstitialAd = null
                            Log.e("AdManager", "Interstitial timeout show failed: ${adError.message}")
                            loadInterstitial(activity.applicationContext)
                            finish()
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    Log.e("AdManager", "★★★ EXCEPTION IN SHOW AD ★★★: ${e.message}", e)
                    interstitialAd = null
                    loadInterstitial(activity.applicationContext)
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "★★★ EXCEPTION IN showInterstitialWithTimeout ★★★: ${e.message}", e)
            handler.removeCallbacks(timeoutRunnable)
            finish()
        }
    }

    fun checkAndShowInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        Log.e("AdManager", "★★★ checkAndShowInterstitial CALLED ★★★")
        if (!AdsPolicy.areAdsEnabled) {
            Log.e("AdManager", "Ads disabled, calling onAdClosed immediately")
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
                            Log.e("AdManager", "Interstitial show failed: ${adError.message}")
                            loadInterstitial(activity.applicationContext)
                            onAdClosed.safeInvoke()
                        }
                    }
                    ad.show(activity)
                } catch (e: Exception) {
                    interstitialAd = null
                    Log.e("AdManager", "Error showing interstitial: ${e.message}", e)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "checkAndShowInterstitial failed: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    fun showRewardAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (!AdsPolicy.areAdsEnabled) {
            onAdClosed.safeInvoke()
            return
        }

        try {
            val ad = rewardedAd
            if (ad == null) {
                Log.w("AdManager", "Rewarded ad not ready")
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
                            Log.e("AdManager", "Rewarded show failed: ${adError.message}")
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
                    Log.e("AdManager", "Error showing rewarded ad: ${e.message}", e)
                    loadReward(activity.applicationContext)
                    onAdClosed.safeInvoke()
                }
            }
        } catch (e: Exception) {
            Log.e("AdManager", "showRewardAd failed: ${e.message}", e)
            onAdClosed.safeInvoke()
        }
    }

    private fun (() -> Unit).safeInvoke() {
        try {
            this()
        } catch (e: Exception) {
            Log.e("AdManager", "Callback execution failed: ${e.message}", e)
        }
    }
}