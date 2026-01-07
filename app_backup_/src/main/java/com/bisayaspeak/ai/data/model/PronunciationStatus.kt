package com.bisayaspeak.ai.data.model

/**
 * 発音判定結果のラベル（UI表示用）
 */
enum class PronunciationStatus {
    PERFECT,  // 緑 - 明瞭で基準に近い発音
    OKAY,     // 黄 - 誤差ありだが許容範囲
    TRY_AGAIN // 赤 - 大きく外れている
}
