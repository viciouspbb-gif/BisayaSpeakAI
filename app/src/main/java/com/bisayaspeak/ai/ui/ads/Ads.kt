package com.bisayaspeak.ai.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bisayaspeak.ai.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

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

// 旧AdMobManagerはAdManagerへ統合済み。Ads.ktではユニットIDとバナー描画のみ保持。
