package com.bisayaspeak.ai.data.remote

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiApi {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    companion object {
        private const val BASE_URL = "https://api.openai.com/v1/"

        fun create(apiKey: String): OpenAiApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $apiKey")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenAiApi::class.java)
        }
    }
}

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
) {
    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String
    )

    data class ResponseFormat(
        @SerializedName("type") val type: String,
        @SerializedName("json_schema") val jsonSchema: JsonSchema? = null
    )

    data class JsonSchema(
        @SerializedName("name") val name: String,
        @SerializedName("schema") val schema: JsonObject
    )
}

data class ChatCompletionResponse(
    @SerializedName("choices") val choices: List<Choice>
) {
    data class Choice(
        @SerializedName("message") val message: Message?
    )

    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String?
    )
}
