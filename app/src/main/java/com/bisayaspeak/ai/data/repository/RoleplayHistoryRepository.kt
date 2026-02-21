package com.bisayaspeak.ai.data.repository

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.bisayaspeak.ai.data.model.MissionHistoryMessage

class RoleplayHistoryRepository(private val context: Context) {

    private val folderName = "roleplay_history"

    suspend fun saveSession(
        scenarioId: String?,
        history: List<MissionHistoryMessage>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (history.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("会話履歴が空のため保存できません。"))
            }
            val targetDir = File(context.filesDir, folderName).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val safeScenario = scenarioId?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "unknown"
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(targetDir, "${safeScenario}_$timestamp.json")

            val historyArray = JSONArray().apply {
                history.forEach { message ->
                    put(
                        JSONObject().apply {
                            put("speaker", if (message.isUser) "user" else "ai")
                            put("text", message.text)
                        }
                    )
                }
            }

            val payload = JSONObject().apply {
                put("scenarioId", scenarioId ?: JSONObject.NULL)
                put("timestamp", System.currentTimeMillis())
                put("history", historyArray)
            }

            file.writeText(payload.toString(2), Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
