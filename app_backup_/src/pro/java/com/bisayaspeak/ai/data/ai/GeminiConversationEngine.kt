package com.bisayaspeak.ai.data.ai

import com.bisayaspeak.ai.data.model.ConversationMode
import com.bisayaspeak.ai.data.model.ConversationTurn
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.RolePlayScenario
import com.bisayaspeak.ai.data.model.Speaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiConversationEngine(private val apiKey: String) {
    
    companion object {
        private const val ROLEPLAY_PROMPT_TEMPLATE = """
あなたは「Learn Bisaya AI」のロールプレイ専用アシスタントです。
ユーザーと1対1で"実践会話"を行い、以下のルールに忠実に従ってください。

【あなたの役割】
- あなたは %s ジャンルの「%s」シチュエーションに登場する人物になりきってください。
  例：空港チェックイン職員、店員、タクシー運転手、先生、友人 など
- 絶対に「AIアシスタントとして」答えないでください。
- ロールプレイ中は「キャラのまま」で、現実的な会話を続けてください。%s

【会話レベル】
ユーザーの学習レベル：%s
- BEGINNER：短く・簡単な語彙中心
- INTERMEDIATE：日常会話レベル
- ADVANCED：自然で流暢な会話／比喩／説明要求も可

ユーザーのレベルに出力を自動調整してください。

【会話構造】
1. 最初のメッセージで「シーンの開始」を宣言せず、自然に話しかけてください。
   例）空港チェックイン → "Maayong hapon. Passport, palihug."
2. ユーザーの返答内容に応じて会話をリアルに進行
3. 誤った内容には軽く訂正や確認を入れる
4. 不自然な沈黙を避けるため、毎ターン「問い」を含める
5. シーンには必ず「ゴール」がある
   - 空港チェックイン：搭乗券発行
   - レストラン：注文完了
   - タクシー：目的地到着
   - 恋愛：連絡先交換 など
6. ゴールに到達すると自然に締めて終了
   例）"Okay, tanan na. Maayong pagbiyahe!"

【間違えた場合の対応】
- 真っ赤な否定ではなく、自然に軌道修正する
- 正しい例文を1つだけ短く提示
  例）"Hapit na. Pwede pud nimo isulti: 'Gusto ko mag-check in, palihug.'"

【禁止事項】
- シーンの途中で AI らしい説明を挿まない
- キャラクターを壊してメタ発言しない
- 長すぎる文章や講義調の文章を禁止
- 1ターンで複数の質問をしない

【ゴール判定ロジック】
以下のどれかに該当したら「ゴール達成」とする：
- 必要情報（名前／行き先／注文など）が揃った
- 明確に会話が完了した
- 6～10ターンで進行し、自然に終わる条件が揃った場合

ゴール達成時には、必ずレスポンスに [GOAL_ACHIEVED] を含めてください。

【終了メッセージ】
ゴール後に1～2行で簡単に締める：
"Maayo. Nahuman na ang check-in. Maayong pagbiyahe!"

※終了後は追加の説明や講義をしない

【出力形式】
[BISAYA]: <ビサヤ語のセリフのみ、50～100文字程度>
[JAPANESE]: <日本語訳>

キャラになりきり一人称で話し、自然に会話を開始してください。
"""
        
        private const val CHARACTER_TONE_TEMPLATE = """

【キャラクターのトーン】
%s
このトーンを一貫して保ち、キャラクターに合った言葉遣いと態度で対応してください。
"""
    }
    
    init {
        android.util.Log.d("GeminiEngine", "Initialized with API key: ${apiKey.take(10)}...")
        android.util.Log.d("GeminiEngine", "API key length: ${apiKey.length}")
        
        // 利用可能なモデルをリスト
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                android.util.Log.d("GeminiEngine", "Available models response: $body")
            } catch (e: Exception) {
                android.util.Log.e("GeminiEngine", "Failed to list models", e)
            }
        }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Gemini API v1beta - gemini-2.5-flashを使用（最新の安定版）
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    
    suspend fun generateResponse(
        userMessage: String,
        mode: ConversationMode,
        level: LearningLevel,
        scenario: RolePlayScenario?,
        conversationHistory: List<ConversationTurn>,
        rolePlayGenre: String? = null,
        rolePlayScene: String? = null,
        characterTone: String? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("GeminiEngine", "generateResponse called: $userMessage")
            val systemPrompt = buildSystemPrompt(mode, level, scenario, rolePlayGenre, rolePlayScene, characterTone)
            val prompt = buildPrompt(systemPrompt, userMessage, conversationHistory)
            android.util.Log.d("GeminiEngine", "Prompt: $prompt")
            
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2000)
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            android.util.Log.d("GeminiEngine", "Response code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("GeminiEngine", "Response body: $responseBody")
                val jsonResponse = JSONObject(responseBody ?: "")
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.getJSONObject("content")
                    
                    // Gemini 2.5では"parts"が存在しない場合がある（thoughtsのみの場合）
                    val text = if (contentObj.has("parts")) {
                        contentObj.getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    } else if (contentObj.has("thoughts")) {
                        // thoughtsから取得
                        contentObj.getJSONArray("thoughts")
                            .getJSONObject(0)
                            .getString("thought")
                    } else {
                        throw Exception("No text content in response")
                    }
                    
                    android.util.Log.d("GeminiEngine", "AI response: $text")
                    // レスポンスをパース（ビサヤ語と日本語訳）
                    val (bisaya, japanese) = parseResponse(text)
                    android.util.Log.d("GeminiEngine", "Parsed - Bisaya: $bisaya, Japanese: $japanese")
                    Result.success(Pair(bisaya, japanese))
                } else {
                    android.util.Log.e("GeminiEngine", "No candidates in response")
                    Result.failure(Exception("No response from Gemini"))
                }
            } else {
                val errorBody = response.body?.string()
                android.util.Log.e("GeminiEngine", "API Error: ${response.code}, Body: $errorBody")
                Result.failure(Exception("API Error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiEngine", "Error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun buildSystemPrompt(
        mode: ConversationMode,
        level: LearningLevel,
        scenario: RolePlayScenario?,
        rolePlayGenre: String? = null,
        rolePlayScene: String? = null,
        characterTone: String? = null
    ): String {
        val levelInstructions = when (level) {
            LearningLevel.BEGINNER -> "You must use only extremely basic Bisaya vocabulary and grammar and respond in very short, clear sentences. Always provide the Bisaya sentence followed by its Japanese translation. Do NOT use technical terms or complex expressions."
            LearningLevel.INTERMEDIATE -> "Use everyday Bisaya vocabulary and standard grammar to produce natural, fluent sentences. Always provide the Bisaya sentence and its Japanese translation. Occasionally include practical phrases related to markets or transportation when appropriate."
            LearningLevel.ADVANCED -> "Use sophisticated Bisaya vocabulary and grammar suitable for business or complex emotional discussions. Provide fluent, detailed sentences with natural expressions, and include metaphors or idioms when appropriate. Always include the Japanese translation after the Bisaya response."
        }
        
        val modeInstructions = when (mode) {
            ConversationMode.ROLEPLAY_SCENE -> {
                buildRolePlayScenePrompt(level, rolePlayGenre, rolePlayScene, characterTone)
            }
            ConversationMode.FREE_TALK -> "Have a natural, friendly conversation in Bisaya."
            ConversationMode.SCENARIO, ConversationMode.ROLEPLAY, ConversationMode.ROLE_PLAY -> {
                scenario?.let {
                    "You are playing the role of ${it.aiRoleEn} in the scenario: ${it.descriptionEn}. Stay in character."
                } ?: "Have a natural conversation."
            }
            ConversationMode.TOPIC -> "Discuss the given topic in depth, asking follow-up questions."
            ConversationMode.SHADOWING -> "Help the user practice shadowing by providing clear, simple phrases to repeat."
            ConversationMode.WORD_DRILL -> "Provide word drills and vocabulary practice in Bisaya."
        }
        
        return """
            You are a Bisaya language conversation partner helping Japanese learners practice Bisaya.
            
            Level: ${level.displayName}
            $levelInstructions
            
            Mode: ${mode.name}
            $modeInstructions
            
            Response format:
            [BISAYA]: <your response in Bisaya>
            [JAPANESE]: <Japanese translation>
            
            Keep responses natural and conversational. Always provide both Bisaya and Japanese.
        """.trimIndent()
    }
    
    private fun buildRolePlayScenePrompt(
        level: LearningLevel,
        genre: String?,
        scene: String?,
        characterTone: String? = null
    ): String {
        val toneInstruction = if (characterTone != null) {
            String.format(CHARACTER_TONE_TEMPLATE, characterTone)
        } else {
            ""
        }
        
        return String.format(
            ROLEPLAY_PROMPT_TEMPLATE,
            genre ?: "general",
            scene ?: "conversation",
            toneInstruction,
            level.displayName
        )
    }
    
    private fun buildPrompt(
        systemPrompt: String,
        userMessage: String,
        history: List<ConversationTurn>
    ): String {
        val historyText = history.takeLast(5).joinToString("\n") { turn ->
            val speaker = if (turn.speaker == Speaker.USER) "User" else "AI"
            "$speaker: ${turn.text}"
        }
        
        return """
            $systemPrompt
            
            Conversation history:
            $historyText
            
            User: $userMessage
            
            Please respond in the specified format.
        """.trimIndent()
    }
    
    private fun parseResponse(content: String): Pair<String, String> {
        // フォーマットに従ったレスポンスをパース
        val bisayaRegex = """\[BISAYA\]:\s*(.+?)(?=\[JAPANESE\]|$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val japaneseRegex = """\[JAPANESE\]:\s*(.+?)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        val bisayaMatch = bisayaRegex.find(content)
        val japaneseMatch = japaneseRegex.find(content)
        
        // フォーマットが見つかった場合
        if (bisayaMatch != null) {
            val bisaya = bisayaMatch.groupValues[1].trim()
            val japanese = japaneseMatch?.groupValues?.get(1)?.trim() ?: ""
            return Pair(bisaya, japanese)
        }
        
        // フォーマットがない場合は、全体をBisayaとして返す（fallback）
        android.util.Log.d("GeminiEngine", "Format not found, using raw content as Bisaya")
        return Pair(content.trim(), "")
    }
    
    /**
     * 日本語からビサヤ語に翻訳
     */
    suspend fun translateJapaneseToVisayan(japaneseText: String): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Translate the following Japanese text to Bisaya (Cebuano) language.
                Only return the Bisaya translation, nothing else.
                
                Japanese: $japaneseText
                Bisaya:
            """.trimIndent()
            
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 500)
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            android.util.Log.d("GeminiEngine", "Translation response code: ${response.code}")
            android.util.Log.d("GeminiEngine", "Translation response body: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("API error: ${response.code}")
            }
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                throw Exception("No translation generated")
            }
            
            val candidate = candidates.getJSONObject(0)
            val contentObj = candidate.getJSONObject("content")
            
            // Gemini 2.5では"parts"が存在しない場合がある
            val text = if (contentObj.has("parts")) {
                contentObj.getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else if (contentObj.has("thoughts")) {
                contentObj.getJSONArray("thoughts")
                    .getJSONObject(0)
                    .getString("thought")
            } else {
                throw Exception("No text content in translation response")
            }
            
            text.trim()
        } catch (e: Exception) {
            android.util.Log.e("GeminiEngine", "Translation error", e)
            // エラー時は元のテキストを返す
            japaneseText
        }
    }
}
