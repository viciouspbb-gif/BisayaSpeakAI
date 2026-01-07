package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiMissionRepository {

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val modelName = "gemini-flash-latest"
    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    suspend fun generateMission(topic: String): String {
        val prompt = """
            Make a mission for: $topic
            (Output MUST be clean text, no markdown)
        """.trimIndent()
        return callGeminiApi(prompt)
    }

    suspend fun translateText(text: String): String {
        val prompt = """
            Translate to Bisaya: $text
            (Output ONLY the translated text)
        """.trimIndent()
        return callGeminiApi(prompt)
    }

    suspend fun generateRoleplayReply(prompt: String): String {
        return callGeminiApi(prompt)
    }

    private suspend fun callGeminiApi(promptText: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", promptText)
                            )
                        )
                    )
                )
                put("safetySettings", JSONArray().apply {
                    put(JSONObject().put("category", "HARM_CATEGORY_HARASSMENT").put("threshold", "BLOCK_NONE"))
                    put(JSONObject().put("category", "HARM_CATEGORY_HATE_SPEECH").put("threshold", "BLOCK_NONE"))
                    put(JSONObject().put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT").put("threshold", "BLOCK_NONE"))
                    put(JSONObject().put("category", "HARM_CATEGORY_DANGEROUS_CONTENT").put("threshold", "BLOCK_NONE"))
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                return@withContext try {
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    parts.getJSONObject(0).getString("text")
                } catch (e: Exception) {
                    Log.e("GeminiRepo", "Parse Error: $response", e)
                    "Error: Could not parse response."
                }
            } else {
                val errorStream = connection.errorStream
                val errorReader = BufferedReader(InputStreamReader(errorStream))
                val errorResponse = StringBuilder()
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }
                errorReader.close()
                Log.e("GeminiRepo", "API Error ($responseCode): $errorResponse")
                "Error: API returned $responseCode"
            }
        } catch (e: Exception) {
            Log.e("GeminiRepo", "Network Error", e)
            "Error: ${e.message}"
        }
    }
}
