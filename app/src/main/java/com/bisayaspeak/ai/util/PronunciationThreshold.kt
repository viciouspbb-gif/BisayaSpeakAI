package com.bisayaspeak.ai.util

import com.bisayaspeak.ai.data.model.PronunciationStatus

/**
 * 発音判定の閾値管理（Cascade-style適用）
 * Lite版（無料）: 合格閾値0.4、シンプルでポジティブなフィードバック
 * Pro版（Premium）: 合格閾値0.6-0.7、厳密判定、ChatGPT連携フィードバック
 */
object PronunciationThreshold {
    
    /**
     * Lite版（無料ユーザー）の閾値
     * 合格閾値：0.2（20%）で超甘い判定、初心者が楽しく学習できる
     */
    private const val LITE_PERFECT_THRESHOLD = 40  // 40%以上 → Perfect（成功）
    private const val LITE_OKAY_THRESHOLD = 20     // 20-39% → Okay（成功）
    
    /**
     * Pro版（Premiumユーザー）- 甘め判定モード
     * 上級者が選択できる「甘め判定」オプション
     */
    private const val PRO_LENIENT_PERFECT_THRESHOLD = 50  // 50%以上 → Perfect
    private const val PRO_LENIENT_OKAY_THRESHOLD = 30     // 30-49% → Okay
    
    /**
     * Pro版（Premiumユーザー）- 厳密判定モード（デフォルト）
     * より正確な発音を求める厳密な判定（合格閾値0.4-0.6）
     */
    private const val PRO_STRICT_PERFECT_THRESHOLD = 60   // 60%以上 → Perfect
    private const val PRO_STRICT_OKAY_THRESHOLD = 40      // 40-59% → Okay
    
    /**
     * Pro版の判定モード
     */
    enum class ProJudgmentMode {
        STRICT,   // 厳密判定（デフォルト）
        LENIENT   // 甘め判定
    }
    
    /**
     * スコアから判定結果を取得
     * @param score 発音スコア（0-100）
     * @param isPremium Premium会員かどうか
     * @param proMode Pro版の判定モード（デフォルト：厳密判定）
     * @return 判定結果
     */
    fun getStatus(
        score: Int, 
        isPremium: Boolean, 
        proMode: ProJudgmentMode = ProJudgmentMode.STRICT
    ): PronunciationStatus {
        return if (isPremium) {
            // Pro版：厳密判定 or 甘め判定
            when (proMode) {
                ProJudgmentMode.STRICT -> {
                    when {
                        score >= PRO_STRICT_PERFECT_THRESHOLD -> PronunciationStatus.PERFECT
                        score >= PRO_STRICT_OKAY_THRESHOLD -> PronunciationStatus.OKAY
                        else -> PronunciationStatus.TRY_AGAIN
                    }
                }
                ProJudgmentMode.LENIENT -> {
                    when {
                        score >= PRO_LENIENT_PERFECT_THRESHOLD -> PronunciationStatus.PERFECT
                        score >= PRO_LENIENT_OKAY_THRESHOLD -> PronunciationStatus.OKAY
                        else -> PronunciationStatus.TRY_AGAIN
                    }
                }
            }
        } else {
            // Lite版：甘め判定（合格閾値0.4）
            when {
                score >= LITE_PERFECT_THRESHOLD -> PronunciationStatus.PERFECT
                score >= LITE_OKAY_THRESHOLD -> PronunciationStatus.OKAY
                else -> PronunciationStatus.TRY_AGAIN
            }
        }
    }
    
    /**
     * 閾値情報を取得（デバッグ用）
     */
    fun getThresholdInfo(isPremium: Boolean, proMode: ProJudgmentMode = ProJudgmentMode.STRICT): String {
        return if (isPremium) {
            when (proMode) {
                ProJudgmentMode.STRICT -> "Pro版（厳密）: Perfect≥$PRO_STRICT_PERFECT_THRESHOLD%, Okay≥$PRO_STRICT_OKAY_THRESHOLD%"
                ProJudgmentMode.LENIENT -> "Pro版（甘め）: Perfect≥$PRO_LENIENT_PERFECT_THRESHOLD%, Okay≥$PRO_LENIENT_OKAY_THRESHOLD%"
            }
        } else {
            "Lite版: Perfect≥$LITE_PERFECT_THRESHOLD%, Okay≥$LITE_OKAY_THRESHOLD%"
        }
    }
}
