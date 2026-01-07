package com.bisayaspeak.ai.data.ai

import com.bisayaspeak.ai.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object TextTranslator {

    private val client = OkHttpClient()

    private val translateUrl = BuildConfig.SERVER_BASE_URL.trimEnd('/') + "/translate"

    fun translate(
        text: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject()
        json.put("text", text)
        json.put("source", "ja")
        json.put("target", "ceb")

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(translateUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    onError("HTTP ${response.code}")
                    return
                }

                val jsonObj = JSONObject(bodyString)
                val visayan = jsonObj.optString("visayan", "")
                if (visayan.isBlank()) {
                    onError("No visayan text returned")
                } else {
                    onSuccess(visayan)
                }
            }
        })
    }
}
