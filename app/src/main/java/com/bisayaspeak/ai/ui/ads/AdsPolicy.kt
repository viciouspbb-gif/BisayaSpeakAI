package com.bisayaspeak.ai.ui.ads

import com.bisayaspeak.ai.BisayaSpeakApp

/**
 * 広告表示ポリシーを集約。
 * LITEビルドでは広告SDKを完全無効化する。
 */
object AdsPolicy {
    /**
     * 実機デバッグ検証時は常に広告挙動を有効化する。
     * LiteビルドかつReleaseのみ広告を止め、それ以外は有効。
     */
    val areAdsEnabled: Boolean
        get() = !BisayaSpeakApp.instance.isProVersion
}
