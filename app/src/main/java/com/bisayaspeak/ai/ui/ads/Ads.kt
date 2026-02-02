package com.bisayaspeak.ai.ui.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bisayaspeak.ai.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob のユニットID定義
 *
 * Debugビルド時はGoogle公式テストIDを使用し、
 * Releaseビルド時は本番IDを使用します。
 */
object AdUnitIds {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    private fun resolve(value: String?, fallback: String): String {
        return value?.takeIf { it.isNotBlank() } ?: fallback
    }

    // バナー（ホーム／アカウント／共通）
    val BANNER_MAIN: String = resolve(BuildConfig.BANNER_AD_UNIT_ID, TEST_BANNER)

    // インタースティシャル（クイズ開始など）
    val INTERSTITIAL_MAIN: String = resolve(BuildConfig.INTERSTITIAL_AD_UNIT_ID, TEST_INTERSTITIAL)

    // リワード（クイズ終了／発音／リスニング／ロールプレイなど）
    val REWARDED_MAIN: String = resolve(BuildConfig.REWARDED_AD_UNIT_ID, TEST_REWARDED)
}

/**
 * 共通のバナー表示用 Composable
 */
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    if (!AdsPolicy.areAdsEnabled) return

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

/**
 * インタースティシャル & リワード広告の簡易マネージャー
 */
object AdMobManager {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    /**
     * アプリ起動時に一度だけ呼び出す初期化処理
     */
    fun initialize(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return

        MobileAds.initialize(context)

        // テストデバイスIDを設定（Debug版のみ）
        if (com.bisayaspeak.ai.BuildConfig.DEBUG) {
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf("65517BD99C066A9E4BEE9A493FAC483A"))
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
        
        // 初期ロード（広告を事前に準備）
        preloadAds(context)
    }
    
    /**
     * 広告を事前にロード
     */
    fun preloadAds(context: Context) {
        if (!AdsPolicy.areAdsEnabled) return

        loadInterstitial(context)
        loadRewarded(context)
    }

    // ===== インタースティシャル =====

    fun loadInterstitial(
        context: Context,
        adUnitId: String = AdUnitIds.INTERSTITIAL_MAIN,
        onLoaded: () -> Unit = {},
        onFailed: (LoadAdError) -> Unit = {}
    ) {
        if (!AdsPolicy.areAdsEnabled) {
            interstitialAd = null
            onLoaded()
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    onFailed(error)
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        if (!AdsPolicy.areAdsEnabled) {
            onDismissed()
            return
        }

        val ad = interstitialAd ?: run {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                onDismissed()
            }
        }

        ad.show(activity)
    }

    // ===== リワード広告 =====

    fun loadRewarded(
        context: Context,
        adUnitId: String = AdUnitIds.REWARDED_MAIN,
        onLoaded: () -> Unit = {},
        onFailed: (LoadAdError) -> Unit = {}
    ) {
        if (!AdsPolicy.areAdsEnabled) {
            rewardedAd = null
            onLoaded()
            return
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    onFailed(error)
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onEarned: (rewardAmount: Int, rewardType: String) -> Unit,
        onDismissed: () -> Unit = {}
    ) {
        if (!AdsPolicy.areAdsEnabled) {
            onEarned(0, "lite_disabled")
            onDismissed()
            return
        }

        val ad = rewardedAd ?: run {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                onDismissed()
            }
        }

        ad.show(activity) { rewardItem ->
            onEarned(rewardItem.amount, rewardItem.type)
        }
    }
}
