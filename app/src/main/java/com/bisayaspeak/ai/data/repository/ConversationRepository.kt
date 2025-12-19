package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ConversationRepository {

    private val baseUrl = BuildConfig.SERVER_BASE_URL

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // -----------------------------
    // セッション管理
    // -----------------------------

    private var currentSession: ConversationSession? = null

    fun createNewSession() {
        currentSession = ConversationSession(mutableListOf())
    }

    fun getCurrentSession(): ConversationSession? = currentSession

    private fun addTurn(speaker: Speaker, text: String, translation: String = "") {
        val session = currentSession ?: return
        session.turns.add(
            ConversationTurn(
                speaker = speaker,
                text = text,
                translation = translation
            )
        )
    }

    // ==========================================================
    // ① FREE TALK (POST /api/chat/free)
    // ==========================================================
    suspend fun sendFreeTalkMessage(text: String): Result<Unit> =
        withContext(Dispatchers.IO) {

            try {
                val json = JSONObject()
                json.put("level", "beginner")
                json.put("message", text)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/chat/free")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("Free talk failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)

                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    // USER turn
                    addTurn(Speaker.USER, text)
                    // AI turn
                    addTurn(Speaker.AI, aiText, translation)

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================================
    // ② ROLEPLAY START (POST /api/roleplay/start)
    // ==========================================================
    suspend fun startRolePlay(sceneId: String, level: String): Result<Unit> =
        withContext(Dispatchers.IO) {

            try {
                // sceneIdをセッションに保存
                currentSession?.sceneId = sceneId

                val json = JSONObject()
                json.put("scene_id", sceneId)
                json.put("level", level)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/roleplay/start")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("RolePlay start failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)

                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    addTurn(Speaker.AI, aiText, translation)

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================================
    // ② ROLEPLAY CHAT (POST /api/roleplay/chat)
    // ==========================================================
    suspend fun sendRolePlayMessage(sceneId: String, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {

            try {
                val json = JSONObject()
                json.put("scene_id", sceneId)
                json.put("text", message)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/roleplay/chat")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("RolePlay Chat failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)

                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    // USER turn
                    addTurn(Speaker.USER, message)
                    // AI turn
                    addTurn(Speaker.AI, aiText, translation)

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================================
    // ③ 翻訳 API  (POST /api/translate)
    // ==========================================================
    suspend fun translateToVisayan(text: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("text", text)
                    put("source", "ja")
                    put("target", "ceb")
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

                val request = Request.Builder()
                    .url("$baseUrl/api/translate")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string()
                    Log.d("TranslateAPI", "HTTP ${response.code} | Body: $bodyStr")

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Translation failed (${response.code}): $bodyStr"))
                    }

                    val body = bodyStr
                        ?: return@withContext Result.failure(Exception("Empty response body"))

                    val jsonObj = JSONObject(body)

                    // Try multiple possible field names
                    val translated = when {
                        jsonObj.has("visayan") -> jsonObj.getString("visayan")
                        jsonObj.has("translated") -> jsonObj.getString("translated")
                        jsonObj.has("translation") -> jsonObj.getString("translation")
                        jsonObj.has("text") -> jsonObj.getString("text")
                        else -> ""
                    }

                    if (translated.isBlank()) {
                        return@withContext Result.failure(Exception("No translation text returned. Response: $body"))
                    }

                    Result.success(translated)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================================
    // ④ 発音診断 (POST /api/pronounce/check)
    // ==========================================================
    suspend fun checkPronunciation(audioFile: File, word: String, level: String)
            : Result<PronunciationResult> =
        withContext(Dispatchers.IO) {

            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "audio",
                        audioFile.name,
                        audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
                    )
                    .addFormDataPart("word", word)
                    .addFormDataPart("level", level)
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/api/pronounce/check")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("Pronunciation check failed"))

                    val bodyStr = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(bodyStr)

                    val result = PronunciationResult(
                        score = jsonObj.getInt("score"),
                        feedback = jsonObj.getString("feedback"),
                        details = jsonObj.getJSONObject("details").toString()
                    )

                    Result.success(result)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==========================================================
    // ⑤ AI会話用: 初回オープニングメッセージ生成
    // ==========================================================
    suspend fun createOpeningTurn(
        scenario: String,
        mode: ConversationMode,
        level: String
    ): Result<ConversationTurn> = withContext(Dispatchers.IO) {
        try {
            // シナリオ系モードの場合はロールプレイ開始APIを使用
            if (mode == ConversationMode.SCENARIO ||
                mode == ConversationMode.ROLEPLAY ||
                mode == ConversationMode.ROLEPLAY_SCENE
            ) {
                val json = JSONObject()
                json.put("scene_id", scenario)
                json.put("level", level)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/roleplay/start")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("Opening message failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)
                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    Result.success(
                        ConversationTurn(
                            speaker = Speaker.AI,
                            text = aiText,
                            translation = translation
                        )
                    )
                }
            } else {
                // フリートークモードの場合は簡単な挨拶を返す
                Result.success(
                    ConversationTurn(
                        speaker = Speaker.AI,
                        text = "Kumusta! Unsaon nako ikatabang nimo karon?",
                        translation = "こんにちは！今日はどのようにお手伝いしましょうか？"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // ⑥ AI会話用: 会話履歴を元にAI返信生成
    // ==========================================================
    suspend fun reply(
        history: List<ConversationTurn>,
        scenario: String,
        mode: ConversationMode,
        level: String
    ): Result<ConversationTurn> = withContext(Dispatchers.IO) {
        try {
            // 最後のユーザーメッセージを取得
            val lastUserMessage = history.lastOrNull { it.speaker == Speaker.USER }?.text
                ?: return@withContext Result.failure(Exception("No user message found"))

            // シナリオ系モードの場合はロールプレイチャットAPIを使用
            if (mode == ConversationMode.SCENARIO ||
                mode == ConversationMode.ROLEPLAY ||
                mode == ConversationMode.ROLEPLAY_SCENE
            ) {
                val json = JSONObject()
                json.put("scene_id", scenario)
                json.put("text", lastUserMessage)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/roleplay/chat")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("AI reply failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)
                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    Result.success(
                        ConversationTurn(
                            speaker = Speaker.AI,
                            text = aiText,
                            translation = translation
                        )
                    )
                }
            } else {
                // フリートークモードの場合
                val json = JSONObject()
                json.put("level", level)
                json.put("message", lastUserMessage)

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val request = Request.Builder()
                    .url("$baseUrl/api/chat/free")
                    .post(json.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        return@withContext Result.failure(Exception("AI reply failed"))

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response"))

                    val jsonObj = JSONObject(body)
                    val aiText = jsonObj.getString("reply")
                    val translation = jsonObj.optString("translation", "")

                    Result.success(
                        ConversationTurn(
                            speaker = Speaker.AI,
                            text = aiText,
                            translation = translation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
