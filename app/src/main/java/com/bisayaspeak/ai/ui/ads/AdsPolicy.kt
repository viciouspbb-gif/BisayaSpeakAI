package com.bisayaspeak.ai.ui.ads

import com.bisayaspeak.ai.BuildConfig

/**
 * 広告表示ポリシーを集約。
 * LITEビルドでは広告SDKを完全無効化する。
 */
object AdsPolicy {
    val areAdsEnabled: Boolean = !BuildConfig.IS_LITE_BUILD
}
