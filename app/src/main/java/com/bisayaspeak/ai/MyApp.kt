package com.bisayaspeak.ai

import android.app.Application
import android.util.Log
import com.bisayaspeak.ai.ui.ads.AdMobManager
import com.bisayaspeak.ai.ui.ads.AdsPolicy
import com.google.android.gms.ads.MobileAds

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApp", "Application started")

        if (AdsPolicy.areAdsEnabled) {
            // ★ これがないと広告は一生出ない
            MobileAds.initialize(this)

            // ★ AdMobManagerを使うなら initialize を呼ぶ
            AdMobManager.initialize(this)
        }
    }
}
