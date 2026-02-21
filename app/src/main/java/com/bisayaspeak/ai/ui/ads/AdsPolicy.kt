package com.bisayaspeak.ai.ui.ads

import android.util.Log
import com.bisayaspeak.ai.BisayaSpeakApp
import com.bisayaspeak.ai.BuildConfig

/**
 * 広告表示ポリシーを集約。
 * LITEビルドでは広告SDKを完全無効化する。
 */
object AdsPolicy {
    /**
     * 実機デバッグ検証時は常に広告挙動を有効化する。
     * LiteビルドかつReleaseのみ広告を止め、それ以外は有効。
     */
    private const val TAG = "ads_policy"

    val areAdsEnabled: Boolean
        get() {
            val baseEnabled = !BisayaSpeakApp.instance.isProVersion
            val enabled = when {
                BuildConfig.DEBUG && BuildConfig.IS_LITE_BUILD -> true
                BuildConfig.DEBUG -> baseEnabled
                else -> baseEnabled
            }
            Log.d(
                TAG,
                "areAdsEnabled=$enabled flavor=${BuildConfig.FLAVOR} debug=${BuildConfig.DEBUG} basePro=${BisayaSpeakApp.instance.isProVersion}"
            )
            return enabled
        }
}
