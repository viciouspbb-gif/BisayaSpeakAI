package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.remote.OpenAiWhisperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class WhisperRepository(
    private val service: OpenAiWhisperService = OpenAiWhisperService.create(BuildConfig.OPENAI_API_KEY)
) {

    companion object {
        private const val MODEL = "whisper-1"
        private const val LANGUAGE = "ceb" // Cebuano has no ISO-639-1 code, so we may omit this param
        private const val TAG = "WhisperRepository"
        private const val ERROR_TAG = "WhisperError"
    }

    suspend fun transcribe(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                val message = "Audio file is empty or missing: ${file.absolutePath}"
                Log.e(TAG, message)
                Log.e(ERROR_TAG, message)
                return@withContext Result.failure(IllegalStateException("Audio file is empty"))
            }

            val mediaType = when {
                file.name.endsWith(".m4a", ignoreCase = true) -> "audio/m4a"
                file.name.endsWith(".aac", ignoreCase = true) -> "audio/aac"
                file.name.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                else -> "audio/*"
            }.toMediaTypeOrNull()

            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mediaType)
            )

            val modelPart = RequestBody.create("text/plain".toMediaTypeOrNull(), MODEL)
            val languagePart = LANGUAGE
                .takeIf { it.length == 2 } // Whisper only accepts ISO-639-1 (2-letter) codes
                ?.let { RequestBody.create("text/plain".toMediaTypeOrNull(), it) }

            val response = service.transcribeAudio(
                file = filePart,
                model = modelPart,
                language = languagePart
            )

            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                val message = "Empty transcription response: $response"
                Log.e(TAG, message)
                Log.e(ERROR_TAG, message)
                Result.failure(IllegalStateException("Failed to transcribe audio"))
            } else {
                Result.success(text)
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "HTTP ${e.code()} transcription failure", e)
            Log.e(ERROR_TAG, "HTTP ${e.code()} error body: $errorBody")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Log.e(ERROR_TAG, "Unexpected transcription error: ${e.message}")
            Result.failure(e)
        } finally {
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Failed to delete temp audio file: ${file.absolutePath}")
            }
        }
    }
}
