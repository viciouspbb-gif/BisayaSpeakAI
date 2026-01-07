package com.bisayaspeak.ai.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// ★ポイント：引数は modifier だけ！ adUnitId は不要です。
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // ここで自動的にIDをセットします
                adUnitId = AdManager.BANNER_TEST_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}