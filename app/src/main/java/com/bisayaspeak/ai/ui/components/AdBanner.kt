package com.bisayaspeak.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bisayaspeak.ai.ui.ads.AdsPolicy
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ads.AdManager

/**
 * 広告バナー（画面下部固定用）
 * Scaffold の bottomBar に配置することで、スクロール対象外で常に表示
 */
@Composable
fun AdBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    // Premium会員または広告無効時は非表示
    if (isPremium || !AdsPolicy.areAdsEnabled) {
        return
    }
    
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdUnitIds.BANNER_MAIN
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

/**
 * 広告バナー（スマートバナー版）
 * 端末サイズに応じて最適なサイズを自動選択
 */
@Composable
fun SmartAdBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    // Premium会員または広告無効時は非表示
    if (isPremium || !AdsPolicy.areAdsEnabled) {
        return
    }
    
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.SMART_BANNER)
                    adUnitId = AdUnitIds.BANNER_MAIN
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                adView.destroy()
            },
            update = { adView ->
                adView.loadAd(AdRequest.Builder().build())
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}
