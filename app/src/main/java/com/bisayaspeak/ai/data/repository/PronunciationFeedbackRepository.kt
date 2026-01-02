package com.bisayaspeak.ai.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 発音フィードバック（暫定オフライン版）
 *
 * OpenAI/サーバー経由の実装は課金停止に伴い停止。
 * 当面はクライアント側で簡易フィードバックを生成し、
 * UI を壊さずに動作させる。
 */
class PronunciationFeedbackRepository {

    /**
     * 発音フィードバックを取得（ダミー応答）
     *
     * @param word 発音した単語
     * @param score 発音スコア（0-100）
     * @param targetLanguage 目標言語（既定: Bisaya）
     */
    suspend fun getPronunciationFeedback(
        word: String,
        score: Int,
        targetLanguage: String = "Bisaya"
    ): Result<String> = withContext(Dispatchers.Default) {
        Result.success(buildOfflineFeedback(word, score, targetLanguage))
    }

    private fun buildOfflineFeedback(word: String, score: Int, targetLanguage: String): String {
        val baseAdvice = when {
            score >= 80 -> "この調子で自信を持って発音し続けましょう。"
            score >= 60 -> "語尾をもう少し丁寧に伸ばし、母音をクリアにすると自然になります。"
            else -> "ゆっくり大きく口を開け、子音をはっきり区切る練習をしてみましょう。"
        }
        return "「$word」の推定スコア：$score 点\n$baseAdvice\n(Offline feedback / maintenance mode)"
    }
}
