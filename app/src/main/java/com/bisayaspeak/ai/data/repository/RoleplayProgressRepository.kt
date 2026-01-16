package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val PROGRESS_FOLDER = "roleplay_progress"

class RoleplayProgressRepository(private val context: Context) {

    private fun progressDir(): File = File(context.filesDir, PROGRESS_FOLDER).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private fun scenarioFile(scenarioId: String): File {
        val safeScenarioId = scenarioId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(progressDir(), "${safeScenarioId}_progress.json")
    }

    suspend fun saveProgress(snapshot: RoleplayProgressSnapshot) = withContext(Dispatchers.IO) {
        val file = scenarioFile(snapshot.scenarioId)
        val payload = JSONObject().apply {
            put("scenarioId", snapshot.scenarioId)
            put("completedTurns", snapshot.completedTurns)
            put("successfulTurns", snapshot.successfulTurns)
            put("timestamp", snapshot.timestamp)
            put("history", JSONArray().apply {
                snapshot.history.forEach { message ->
                    put(JSONObject().apply {
                        put("text", message.text)
                        put("isUser", message.isUser)
                    })
                }
            })
            put("messages", JSONArray().apply {
                snapshot.messages.forEach { stored ->
                    put(JSONObject().apply {
                        put("id", stored.id)
                        put("text", stored.text)
                        put("isUser", stored.isUser)
                        put("translation", stored.translation ?: JSONObject.NULL)
                        put("voiceCue", stored.voiceCue ?: JSONObject.NULL)
                    })
                }
            })
            put("options", JSONArray().apply {
                snapshot.options.forEach { option ->
                    put(JSONObject().apply {
                        put("id", option.id)
                        put("text", option.text)
                        put("hint", option.hint ?: JSONObject.NULL)
                        put("tone", option.tone ?: JSONObject.NULL)
                        put("requiresPro", option.requiresPro)
                        put("nextTurnId", option.nextTurnId ?: JSONObject.NULL)
                        put("branchKey", option.branchKey ?: JSONObject.NULL)
                        put("branchValue", option.branchValue ?: JSONObject.NULL)
                    })
                }
            })
            put("peekedHints", JSONArray().apply {
                snapshot.peekedHintIds.forEach { put(it) }
            })
            put("branchFacts", JSONObject().apply {
                snapshot.branchFacts.forEach { (key, value) ->
                    put(key, value)
                }
            })
            put("scriptedCurrentTurnId", snapshot.scriptedCurrentTurnId ?: JSONObject.NULL)
            put("scriptedAwaitingTurnId", snapshot.scriptedAwaitingTurnId ?: JSONObject.NULL)
            put("isScriptedScenario", snapshot.isScriptedScenario)
            put("endingTurnTarget", snapshot.endingTurnTarget)
            put("endingSuggestionRequested", snapshot.endingSuggestionRequested)
            put("endingDialogShown", snapshot.endingDialogShown)
        }
        file.writeText(payload.toString(2), Charsets.UTF_8)
    }

    suspend fun loadProgress(scenarioId: String): RoleplayProgressSnapshot? = withContext(Dispatchers.IO) {
        val file = scenarioFile(scenarioId)
        if (!file.exists()) return@withContext null
        runCatching {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            RoleplayProgressSnapshot(
                scenarioId = json.getString("scenarioId"),
                completedTurns = json.optInt("completedTurns"),
                successfulTurns = json.optInt("successfulTurns"),
                history = json.optJSONArray("history")?.let { array ->
                    buildList(array.length()) {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            add(
                                MissionHistoryMessage(
                                    text = item.optString("text"),
                                    isUser = item.optBoolean("isUser")
                                )
                            )
                        }
                    }
                }.orEmpty(),
                messages = json.optJSONArray("messages")?.let { array ->
                    buildList(array.length()) {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            add(
                                StoredChatMessage(
                                    id = item.optString("id"),
                                    text = item.optString("text"),
                                    isUser = item.optBoolean("isUser"),
                                    translation = item.readNullableString("translation"),
                                    voiceCue = item.readNullableString("voiceCue")
                                )
                            )
                        }
                    }
                }.orEmpty(),
                options = json.optJSONArray("options")?.let { array ->
                    buildList(array.length()) {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            add(
                                StoredRoleplayOption(
                                    id = item.optString("id"),
                                    text = item.optString("text"),
                                    hint = item.readNullableString("hint"),
                                    tone = item.readNullableString("tone"),
                                    requiresPro = item.optBoolean("requiresPro"),
                                    nextTurnId = item.readNullableString("nextTurnId"),
                                    branchKey = item.readNullableString("branchKey"),
                                    branchValue = item.readNullableString("branchValue")
                                )
                            )
                        }
                    }
                }.orEmpty(),
                peekedHintIds = json.optJSONArray("peekedHints")?.let { array ->
                    buildSet {
                        for (i in 0 until array.length()) {
                            val value = array.optString(i)
                            if (value.isNotBlank()) add(value)
                        }
                    }
                } ?: emptySet(),
                branchFacts = json.optJSONObject("branchFacts")?.let { obj ->
                    buildMap {
                        obj.keys().forEach { key ->
                            put(key, obj.optString(key))
                        }
                    }
                } ?: emptyMap(),
                timestamp = json.optLong("timestamp"),
                scriptedCurrentTurnId = json.optString("scriptedCurrentTurnId").takeIf { it.isNotBlank() },
                scriptedAwaitingTurnId = json.optString("scriptedAwaitingTurnId").takeIf { it.isNotBlank() },
                isScriptedScenario = json.optBoolean("isScriptedScenario", false),
                endingTurnTarget = json.optInt("endingTurnTarget", 10),
                endingSuggestionRequested = json.optBoolean("endingSuggestionRequested", false),
                endingDialogShown = json.optBoolean("endingDialogShown", false)
            )
        }.getOrElse {
            file.delete()
            null
        }
    }

    suspend fun clearProgress(scenarioId: String) = withContext(Dispatchers.IO) {
        scenarioFile(scenarioId).takeIf { it.exists() }?.delete()
    }

    suspend fun hasProgress(scenarioId: String): Boolean = withContext(Dispatchers.IO) {
        val file = scenarioFile(scenarioId)
        file.exists() && file.length() > 0
    }
}

data class RoleplayProgressSnapshot(
    val scenarioId: String,
    val completedTurns: Int,
    val successfulTurns: Int,
    val history: List<MissionHistoryMessage>,
    val messages: List<StoredChatMessage>,
    val options: List<StoredRoleplayOption>,
    val peekedHintIds: Set<String>,
    val branchFacts: Map<String, String>,
    val timestamp: Long,
    val scriptedCurrentTurnId: String?,
    val scriptedAwaitingTurnId: String?,
    val isScriptedScenario: Boolean,
    val endingTurnTarget: Int,
    val endingSuggestionRequested: Boolean,
    val endingDialogShown: Boolean
)

data class StoredChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val translation: String?,
    val voiceCue: String?
)

data class StoredRoleplayOption(
    val id: String,
    val text: String,
    val hint: String?,
    val tone: String?,
    val requiresPro: Boolean,
    val nextTurnId: String?,
    val branchKey: String?,
    val branchValue: String?
)

private fun JSONObject.readNullableString(key: String): String? = if (isNull(key)) null else optString(key)
