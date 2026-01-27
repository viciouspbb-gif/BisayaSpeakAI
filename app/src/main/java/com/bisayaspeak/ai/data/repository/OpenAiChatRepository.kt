package com.bisayaspeak.ai.data.repository

import android.os.SystemClock
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.remote.ChatCompletionRequest
import com.bisayaspeak.ai.data.remote.ChatCompletionResponse
import com.bisayaspeak.ai.data.remote.OpenAiApi
import java.net.SocketTimeoutException

class OpenAiChatRepository(
    private val api: OpenAiApi = OpenAiApi.create(BuildConfig.OPENAI_API_KEY),
    private val promptProvider: PromptProvider? = null
) {

    companion object {
        private const val MODEL = "gpt-4o-mini"
        private const val TEMPERATURE = 0.7
        private const val TAG = "OpenAiChatRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    suspend fun generateListeningReply(
        userMessage: String,
        isUserFemale: Boolean,
        addressTerm: String,
        shouldConclude: Boolean = false,
        farewellExamples: List<String> = emptyList()
    ): String {
        val basePrompt = promptProvider?.getSystemPrompt() ?: buildString {
            appendLine("You are Tari, a friendly Bisaya teacher.")
            appendLine("The user is ${if (isUserFemale) "FEMALE" else "MALE"} and must be addressed as '$addressTerm'.")
            appendLine("Teach Bisaya with short, encouraging lines and keep translations in Japanese.")
            appendLine("User input may be recognized as Katakana Japanese but it actually represents Bisaya intent—interpret it accurately based on context.")
            appendLine("Output MUST be a JSON object with 'aiSpeech', 'aiTranslation', and 'options' fields.")
            appendLine("Always produce exactly 3 options in the 'options' array, each containing Bisaya text, a Japanese translation, and a tone descriptor.")
        }
        
        val systemPrompt = buildString {
            append(basePrompt)
            if (shouldConclude) {
                appendLine("This is the climax of today's mini-episode.")
                appendLine("Wrap the scene naturally, praise the learner's progress, and gently end the conversation.")
                val farewellList = if (farewellExamples.isNotEmpty()) {
                    farewellExamples.joinToString(", ")
                } else {
                    "\"Sige, amping!\", \"Kita-kita ra ta puhon!\""
                }
                appendLine("Finish the conversation naturally and use farewell examples such as $farewellList.")
            }
        }

        val responseFormat = ChatCompletionRequest.ResponseFormat(
            type = "json_object"
        )
        return sendPrompt(
            systemPrompt = systemPrompt,
            userPrompt = userMessage,
            temperature = TEMPERATURE,
            responseFormat = responseFormat
        )
    }

    suspend fun generateJsonResponse(
        prompt: String,
        temperature: Double = TEMPERATURE
    ): String {
        return sendPrompt(userPrompt = prompt, temperature = temperature)
    }

    suspend fun sendPrompt(
        systemPrompt: String? = null,
        userPrompt: String,
        temperature: Double = TEMPERATURE,
        responseFormat: ChatCompletionRequest.ResponseFormat? = null
    ): String {
        val messages = buildList {
            systemPrompt?.let { add(ChatCompletionRequest.Message(role = "system", content = it)) }
            add(ChatCompletionRequest.Message(role = "user", content = userPrompt))
        }
        val request = ChatCompletionRequest(
            model = MODEL,
            messages = messages,
            temperature = temperature,
            responseFormat = responseFormat
        )
        var lastTimeout: SocketTimeoutException? = null
        repeat(MAX_RETRY_ATTEMPTS) { attemptIndex ->
            val attemptNumber = attemptIndex + 1
            val start = SystemClock.elapsedRealtime()
            try {
                val response = api.createChatCompletion(request)
                return extractContent(response)
            } catch (e: SocketTimeoutException) {
                val elapsedMs = SystemClock.elapsedRealtime() - start
                lastTimeout = e
                Log.w(
                    TAG,
                    "OpenAI chat timeout (attempt $attemptNumber/$MAX_RETRY_ATTEMPTS, " +
                        "elapsed=${elapsedMs}ms, userPromptLength=${userPrompt.length}, " +
                        "preview=\"${userPrompt.safePreview()}\"",
                    e
                )
                if (attemptNumber == MAX_RETRY_ATTEMPTS) {
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenAI chat completion failed", e)
                throw e
            }
        }

        throw lastTimeout ?: IllegalStateException("OpenAI chat failed without exception")
    }

    private fun extractContent(response: ChatCompletionResponse): String {
        val content = response.choices.firstOrNull()?.message?.content
        require(!content.isNullOrBlank()) { "Empty response from OpenAI" }
        return content.trim()
    }

    private fun String.safePreview(maxLength: Int = 160): String {
        val sanitized = replace('\n', ' ').replace('\r', ' ')
        return if (sanitized.length <= maxLength) sanitized else sanitized.take(maxLength) + "…"
    }
}
