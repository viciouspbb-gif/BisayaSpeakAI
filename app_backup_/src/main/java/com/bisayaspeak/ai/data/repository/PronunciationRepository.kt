package com.bisayaspeak.ai.data.repository

import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.PronunciationResponse
import com.bisayaspeak.ai.data.model.FeedbackDetail
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import com.google.gson.Gson
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PronunciationRepository {
    
    companion object {
        private const val BASE_URL = "https://bisaya-speak-ai-server.onrender.com"
        private const val TIMEOUT_SECONDS = 60L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 発音をチェック
     */
    suspend fun checkPronunciation(
        audioFile: File,
        word: String,
        level: LearningLevel
    ): Result<PronunciationResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                )
                .addFormDataPart("word", word)
                .addFormDataPart("level", level.apiValue)
                .build()
            
            val request = Request.Builder()
                .url("$BASE_URL/api/pronounce/check")
                .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("PronunciationRepository", "Response: $responseBody")
                
                // サーバーのレスポンス形式を確認
                val jsonResponse = org.json.JSONObject(responseBody ?: "{}")
                
                // サーバーは { "status": "success", "data": { ... } } 形式で返す
                val data = if (jsonResponse.has("data")) {
                    jsonResponse.getJSONObject("data")
                } else {
                    jsonResponse
                }
                
                val pronunciationScore = data.optInt("pronunciation_score", 0)
                val feedback = data.optJSONObject("feedback")
                
                // 詳細フィードバックをパース
                val detailsList = mutableListOf<FeedbackDetail>()
                feedback?.optJSONArray("details")?.let { detailsArray ->
                    for (i in 0 until detailsArray.length()) {
                        val detail = detailsArray.getJSONObject(i)
                        detailsList.add(
                            FeedbackDetail(
                                aspect = detail.optString("aspect", ""),
                                score = detail.optDouble("score", 0.0).toInt(),
                                comment = detail.optString("comment", "")
                            )
                        )
                    }
                }
                
                // Tipsをパース
                val tipsList = mutableListOf<String>()
                val tipsValue = feedback?.opt("tips")
                when (tipsValue) {
                    is org.json.JSONArray -> {
                        for (i in 0 until tipsValue.length()) {
                            tipsList.add(tipsValue.getString(i))
                        }
                    }
                    is String -> {
                        tipsList.add(tipsValue)
                    }
                }
                
                val pronunciationResponse = PronunciationResponse(
                    score = pronunciationScore,
                    feedback = feedback?.optString("overall"),
                    detailedFeedback = if (detailsList.isNotEmpty()) detailsList else null,
                    tips = if (tipsList.isNotEmpty()) tipsList else null
                )
                
                android.util.Log.d("PronunciationRepository", "Parsed score: $pronunciationScore")
                Result.success(pronunciationResponse)
            } else {
                Result.failure(Exception("Server error: ${response.code}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("PronunciationRepository", "Error checking pronunciation", e)
            Result.failure(e)
        }
    }
    
    /**
     * 参照音声を取得
     */
    suspend fun getReferenceAudio(word: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/reference-audio/$word")
                .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val audioData = response.body?.bytes()
                if (audioData != null) {
                    Result.success(audioData)
                } else {
                    Result.failure(Exception("Empty audio data"))
                }
            } else {
                Result.failure(Exception("Server error: ${response.code}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("PronunciationRepository", "Error getting reference audio", e)
            Result.failure(e)
        }
    }
    
    /**
     * サーバーのヘルスチェック
     */
    suspend fun checkServerHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            android.util.Log.e("PronunciationRepository", "Error checking server health", e)
            Result.failure(e)
        }
    }
}
