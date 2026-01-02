package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.model.MissionChatMessage
import com.bisayaspeak.ai.data.model.MissionContext
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.UUID

class GeminiMissionRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val missionModelName: String = "gemini-2.0-flash",
    private val translationModelName: String = "gemini-1.5-flash"
) {

    private val missionModel by lazy {
        GenerativeModel(
            modelName = missionModelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.65f
                maxOutputTokens = 768
                topP = 0.9f
                topK = 64
            }
        )
    }

    private val translationModel by lazy {
        GenerativeModel(
            modelName = translationModelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.2f
                maxOutputTokens = 256
            }
        )
    }

    fun streamMissionReply(
        context: MissionContext,
        history: List<MissionHistoryMessage>,
        userMessage: String
    ): Flow<MissionChatMessage> = flow {
        val systemPrompt = buildMissionPrompt(context)
        val contents = buildConversationContents(systemPrompt, history, userMessage)
        val aiMessageId = UUID.randomUUID().toString()
        val accumulator = StringBuilder()
        missionModel.generateContentStream(*contents.toTypedArray()).collect { chunk ->
            val text = chunk.text ?: ""
            if (text.isBlank()) return@collect
            accumulator.append(text)
            val parsed = parseDualResponse(accumulator.toString())
            emit(
                MissionChatMessage(
                    id = aiMessageId,
                    primaryText = parsed.first,
                    secondaryText = parsed.second,
                    isUser = false,
                    isGoalFlagged = accumulator.contains(GOAL_TAG, ignoreCase = true)
                )
            )
        }
    }.onStart {
        Log.d(TAG, "Mission chat start: ${context.title}")
    }.onCompletion { cause ->
        if (cause != null) {
            Log.e(TAG, "Mission chat failed", cause)
        } else {
            Log.d(TAG, "Mission chat completed")
        }
    }

    suspend fun translate(
        text: String,
        direction: TranslationDirection
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val instruction = when (direction) {
                TranslationDirection.JA_TO_CEB ->
                    "Translate this to Cebuano (Bisaya) naturally, considering slang and casual tone when appropriate. Return ONLY the translation."
                TranslationDirection.CEB_TO_JA ->
                    "Translate this Bisaya (Cebuano) text into natural Japanese, keeping the nuance and politeness. Return ONLY the translation."
            }

            val response = translationModel.generateContent(
                content {
                    text(instruction)
                    text(text.trim())
                }
            )
            response.text?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Empty translation result")
        }
    }

    private fun buildConversationContents(
        systemPrompt: String,
        history: List<MissionHistoryMessage>,
        userMessage: String
    ): List<Content> {
        val contents = mutableListOf<Content>()
        contents += content(role = "user") { text("SYSTEM INSTRUCTION:\n$systemPrompt") }
        contents += content(role = "model") { text("Understood. I will stay in character.") }

        history.forEach { message ->
            val role = if (message.isUser) "user" else "model"
            contents += content(role = role) { text(message.text) }
        }
        contents += content(role = "user") { text(userMessage) }
        return contents
    }

    private fun buildMissionPrompt(context: MissionContext): String {
        val hintsBlock = if (context.hints.isNotEmpty()) {
            "- Mission hints: ${context.hints.joinToString(", ")}"
        } else {
            ""
        }
        val toneBlock = context.tone?.let { "- Character tone: $it" } ?: ""
        return """
            You are the AI character for the "AI Mission Talk" feature of Bisaya Speak.
            Stay IN CHARACTER as ${context.role} inside the situation "${context.situation}".
            Never break character or mention being AI.
            
            Mission goal: ${context.goal}
            Turn limit: ${context.turnLimit} turns maximum. Drive the conversation toward the goal quickly.
            Level: ${context.level.displayName}
            $toneBlock
            $hintsBlock
            
            Response rules:
            1. Always speak in Bisaya conversational tone.
            2. Provide the Bisaya line and its Japanese translation using the format:
               [BISAYA]: ...
               [JAPANESE]: ...
            3. Ask a follow-up question or prompt every turn to keep the mission moving.
            4. When the mission goal is clearly satisfied, append the token $GOAL_TAG at the END of the Bisaya line.
            5. Keep responses within 2 concise sentences.
        """.trimIndent()
    }

    private fun parseDualResponse(raw: String): Pair<String, String?> {
        val cleaned = raw.replace(GOAL_TAG, "").trim()
        val bisaya = BISAYA_REGEX.find(cleaned)?.groupValues?.get(1)?.trim()
        val japanese = JAPANESE_REGEX.find(cleaned)?.groupValues?.get(1)?.trim()
        return when {
            bisaya != null -> bisaya to japanese
            else -> cleaned to null
        }
    }

    companion object {
        private const val TAG = "GeminiMissionRepo"
        private const val GOAL_TAG = "[GOAL_ACHIEVED]"

        private val BISAYA_REGEX =
            """\[BISAYA\]:\s*(.+?)(?=\[JAPANESE]|$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        private val JAPANESE_REGEX =
            """\[JAPANESE\]:\s*(.+)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
}
