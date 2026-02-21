package com.bisayaspeak.ai.feature

import com.bisayaspeak.ai.BuildConfig

/**
 * グローバルな機能アクセス制御。
 * LiteビルドではPro機能を無効化し、将来の新機能もここで一括判定する。
 */
object FeatureAccessPolicy {
    /** Liteビルド以外 = Proバージョン */
    val isProVersion: Boolean = !BuildConfig.IS_LITE_BUILD
}
