package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Pro版専用：ChatGPT連携で発音フィードバックを提供
 * 「どこが弱いか」「改善ポイント」を会話形式で提示
 */
class PronunciationFeedbackRepository {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = BuildConfig.SERVER_BASE_URL.trimEnd('/')
    
    /**
     * Pro版専用：発音フィードバックを取得
     * @param word 発音した単語
     * @param score 発音スコア（0-100）
     * @param targetLanguage 目標言語（Bisaya）
     * @return フィードバックメッセージ
     */
    suspend fun getPronunciationFeedback(
        word: String,
        score: Int,
        targetLanguage: String = "Bisaya"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // ChatGPT APIにリクエスト
            val prompt = buildFeedbackPrompt(word, score, targetLanguage)
            
            val jsonBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "あなたはビサヤ語の発音コーチです。学習者に優しく、具体的なアドバイスを提供してください。")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 200)
                put("temperature", 0.7)
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/api/pronunciation/feedback")
                .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val feedback = jsonResponse.optString("feedback", "")
                    
                    if (feedback.isNotEmpty()) {
                        Log.d("PronunciationFeedback", "Feedback received: $feedback")
                        Result.success(feedback)
                    } else {
                        Result.failure(Exception("Empty feedback"))
                    }
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e("PronunciationFeedback", "API error: ${response.code}")
                // フォールバック：基本的なフィードバックを返す
                Result.success(getBasicFeedback(score))
            }
        } catch (e: IOException) {
            Log.e("PronunciationFeedback", "Network error", e)
            // フォールバック：基本的なフィードバックを返す
            Result.success(getBasicFeedback(score))
        } catch (e: Exception) {
            Log.e("PronunciationFeedback", "Unexpected error", e)
            Result.failure(e)
        }
    }
    
    /**
     * ChatGPT用のプロンプトを構築
     */
    private fun buildFeedbackPrompt(word: String, score: Int, targetLanguage: String): String {
        return when {
            score >= 80 -> {
                "「$word」の発音スコアは${score}点でした。素晴らしい発音です！さらに上達するための1つのアドバイスを50文字以内で教えてください。"
            }
            score >= 60 -> {
                "「$word」の発音スコアは${score}点でした。良い発音ですが、改善の余地があります。どこを改善すべきか、具体的なポイントを50文字以内で教えてください。"
            }
            else -> {
                "「$word」の発音スコアは${score}点でした。発音が弱い部分と、改善するための具体的な練習方法を50文字以内で教えてください。"
            }
        }
    }
    
    /**
     * 基本的なフィードバック（フォールバック用）
     */
    private fun getBasicFeedback(score: Int): String {
        return when {
            score >= 80 -> "素晴らしい発音です！この調子で練習を続けましょう。"
            score >= 60 -> "良い発音です。もう少し明瞭に発音すると、さらに良くなります。"
            else -> "発音を改善しましょう。ゆっくりと、はっきりと発音してみてください。"
        }
    }
}
