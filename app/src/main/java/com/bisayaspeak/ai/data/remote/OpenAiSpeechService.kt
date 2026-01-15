package com.bisayaspeak.ai.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenAiSpeechService {

    @Streaming
    @Headers("Content-Type: application/json")
    @POST("audio/speech")
    suspend fun synthesizeSpeech(
        @Body request: SpeechRequest
    ): ResponseBody

    data class SpeechRequest(
        @SerializedName("model") val model: String = "tts-1",
        @SerializedName("voice") val voice: String = "nova",
        @SerializedName("input") val input: String,
        @SerializedName("format") val format: String = "mp3",
        @SerializedName("speed") val speed: Float = 1.0f
    )

    companion object {
        private const val BASE_URL = "https://api.openai.com/v1/"

        fun create(apiKey: String): OpenAiSpeechService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Accept", "audio/mpeg")
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
                .create(OpenAiSpeechService::class.java)
        }
    }
}
