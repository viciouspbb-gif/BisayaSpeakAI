package com.bisayaspeak.ai.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.repository.GeminiMissionRepository
import com.bisayaspeak.ai.utils.MistakeManager
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.LessonStatusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class RoleplayOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val hint: String? = null,
    val tone: String? = null
)

data class RoleplayResultPayload(
    val correctCount: Int,
    val totalQuestions: Int,
    val earnedXp: Int,
    val clearedLevel: Int,
    val leveledUp: Boolean
)

data class RoleplayUiState(
    val currentScenario: RoleplayScenarioDefinition? = null,
    val missionGoal: String = "",
    val aiCharacterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = "",
    val isLoading: Boolean = false,
    val options: List<RoleplayOption> = emptyList(),
    val peekedHintOptionIds: Set<String> = emptySet(),
    val completedTurns: Int = 0,
    val successfulTurns: Int = 0,
    val showCompletionDialog: Boolean = false,
    val completionScore: Int = 0,
    val pendingUnlockLevel: Int? = null,
    val pendingResult: RoleplayResultPayload? = null
)

class RoleplayChatViewModel(
    private val repository: GeminiMissionRepository = GeminiMissionRepository()
) : ViewModel() {

    private companion object {
        private const val START_TOKEN = "[START_CONVERSATION]"
        private const val COMPLETION_SCORE = 90
        private const val COMPLETION_THRESHOLD = 80
    }

    private val _uiState = MutableStateFlow(RoleplayUiState())
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val history = mutableListOf<MissionHistoryMessage>()
    private var scriptedRuntime: ScriptedRuntime? = null

    fun loadScenario(scenarioId: String) {
        val definition = getRoleplayScenarioDefinition(scenarioId)
        history.clear()
        scriptedRuntime = scriptedScenarioDefinitions[scenarioId]?.let { ScriptedRuntime(it) }

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = definition.systemPrompt,
            messages = emptyList(),
            isLoading = scriptedRuntime == null
        )

        scriptedRuntime?.let {
            deliverScriptedTurn()
        } ?: requestAiTurn(
            scenario = definition,
            userMessage = START_TOKEN
        )
    }

    fun selectOption(optionId: String) {
        val option = _uiState.value.options.find { it.id == optionId } ?: return
        if (_uiState.value.isLoading && scriptedRuntime == null) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = option.text,
            isUser = true,
            translation = option.hint
        )
        history.add(MissionHistoryMessage(option.text, isUser = true))

        val usedHint = option.id in _uiState.value.peekedHintOptionIds

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = scriptedRuntime == null,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                completedTurns = it.completedTurns + 1,
                successfulTurns = it.successfulTurns + if (usedHint) 0 else 1
            )
        }

        scriptedRuntime?.let {
            if (it.turnPointer >= it.scenario.turns.size) {
                finalizeScriptedScenario()
            } else {
                deliverScriptedTurn()
            }
            return
        }

        val scenario = _uiState.value.currentScenario ?: return
        requestAiTurn(scenario, option.text)
    }

    fun markHintPeeked(optionId: String) {
        val option = _uiState.value.options.find { it.id == optionId } ?: return
        MistakeManager.addMistake(option.text)
        _uiState.update {
            it.copy(peekedHintOptionIds = it.peekedHintOptionIds + optionId)
        }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
    }

    fun consumePendingResult() {
        _uiState.update { it.copy(pendingResult = null) }
    }

    fun markUnlockHandled() {
        _uiState.update { it.copy(pendingUnlockLevel = null) }
    }

    fun notifyVoicePlaybackStarted(messageId: String) {
        _speakingMessageId.value = messageId
    }

    fun notifyVoicePlaybackFinished(messageId: String) {
        if (_speakingMessageId.value == messageId) {
            _speakingMessageId.value = null
        }
    }

    private fun deliverScriptedTurn() {
        val runtime = scriptedRuntime ?: return
        val turn = runtime.scenario.turns.getOrNull(runtime.turnPointer)
        if (turn == null) {
            finalizeScriptedScenario()
            return
        }

        history.add(MissionHistoryMessage(turn.aiText, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = turn.aiText,
            isUser = false,
            translation = turn.translation,
            voiceCue = turn.voiceCue
        )
        val options = turn.options.map {
            RoleplayOption(
                text = it.text,
                hint = it.translation
            )
        }

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = options,
                peekedHintOptionIds = emptySet()
            )
        }
        runtime.turnPointer++
    }

    fun forceCompleteScenario() {
        queueCompletion(calculateScore())
    }

    private fun finalizeScriptedScenario() {
        scriptedRuntime = null
        queueCompletion(calculateScore())
    }

    private fun requestAiTurn(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = buildRoleplayPrompt(scenario, userMessage)
                val rawResponse = repository.generateRoleplayReply(prompt)
                val payload = parseRoleplayPayload(rawResponse)
                applyAiPayload(payload)
            } catch (e: Exception) {
                val fallbackText = "AIの応答取得に失敗しました: ${e.message ?: "Unknown error"}"
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = fallbackText,
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMsg,
                        isLoading = false,
                        options = emptyList(),
                        peekedHintOptionIds = emptySet()
                    )
                }
            }
        }
    }

    private fun applyAiPayload(payload: RoleplayAiResponsePayload) {
        val aiText = payload.aiResponse.ifBlank { "..." }
        history.add(MissionHistoryMessage(aiText, isUser = false))
        val aiMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = aiText,
            isUser = false
        )
        val options = payload.options
            .filter { it.text.isNotBlank() }
            .map {
                RoleplayOption(
                    text = it.text,
                    hint = it.translation,
                    tone = it.tone
                )
            }

        _uiState.update {
            it.copy(
                messages = it.messages + aiMsg,
                isLoading = false,
                options = options,
                peekedHintOptionIds = emptySet()
            )
        }

        if (options.isEmpty()) {
            queueCompletion(calculateScore())
        }
    }

    private fun buildResultPayload(score: Int): RoleplayResultPayload {
        val totalTurns = _uiState.value.completedTurns.coerceAtLeast(1)
        val successful = _uiState.value.successfulTurns.coerceAtMost(totalTurns)
        val xp = (successful * 20).coerceAtLeast(10)
        val clearedLevel = _uiState.value.currentScenario?.level ?: 1
        val leveledUp = score >= COMPLETION_THRESHOLD
        return RoleplayResultPayload(
            correctCount = successful,
            totalQuestions = totalTurns,
            earnedXp = xp,
            clearedLevel = clearedLevel,
            leveledUp = leveledUp
        )
    }

    private fun queueCompletion(score: Int) {
        if (_uiState.value.pendingResult != null) return
        val scenarioLevel = _uiState.value.currentScenario?.level ?: 1
        val payload = buildResultPayload(score)
        _uiState.update {
            it.copy(
                isLoading = false,
                options = emptyList(),
                peekedHintOptionIds = emptySet(),
                showCompletionDialog = false,
                completionScore = score,
                pendingUnlockLevel = if (score >= COMPLETION_THRESHOLD) scenarioLevel + 1 else null,
                pendingResult = payload
            )
        }
    }

    private fun calculateScore(): Int {
        val turns = _uiState.value.completedTurns
        return if (turns > 0) {
            ((_uiState.value.successfulTurns.toFloat() / turns.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            COMPLETION_SCORE
        }
    }

    private fun buildRoleplayPrompt(
        scenario: RoleplayScenarioDefinition,
        userMessage: String
    ): String {
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }

        val hints = scenario.hintPhrases.joinToString(separator = "\n") {
            "- ${it.nativeText} (${it.translation})"
        }.ifBlank { "- (none)" }

        val basePrompt = if (scenario.systemPrompt.isBlank()) {
            """
            You are ${scenario.aiRole}.
            Situation: ${scenario.situation}
            Goal: ${scenario.goal}
            """.trimIndent()
        } else scenario.systemPrompt

        return """
            $basePrompt

            Helpful hint phrases:
            $hints

            Conversation history:
            $historyText

            Latest learner message: $userMessage

            Respond strictly in JSON:
            {
              "aiResponse": "assistant reply in Bisaya with light Japanese hints if needed",
              "options": [
                {
                  "text": "suggested learner reply in Bisaya",
                  "translation": "Japanese translation or hint",
                  "tone": "short tone description"
                }
              ]
            }
            Provide 2-3 concise options. Do not include markdown.
        """.trimIndent()
    }

    private fun parseRoleplayPayload(raw: String): RoleplayAiResponsePayload {
        return try {
            val json = JSONObject(raw)
            val aiResponse = json.optString("aiResponse", raw)
            val optionsArray = json.optJSONArray("options") ?: JSONArray()
            val options = mutableListOf<RoleplayAiOption>()
            for (i in 0 until optionsArray.length()) {
                val item = optionsArray.optJSONObject(i) ?: continue
                options += RoleplayAiOption(
                    text = item.optString("text"),
                    translation = item.optString("translation"),
                    tone = item.optString("tone")
                )
            }
            RoleplayAiResponsePayload(aiResponse, options)
        } catch (_: Exception) {
            RoleplayAiResponsePayload(raw, emptyList())
        }
    }

    private data class RoleplayAiResponsePayload(
        val aiResponse: String,
        val options: List<RoleplayAiOption>
    )

    private data class RoleplayAiOption(
        val text: String,
        val translation: String?,
        val tone: String?
    )
}

private data class ScriptedScenario(
    val turns: List<ScriptedTurn>
)

private data class ScriptedTurn(
    val aiText: String,
    val translation: String,
    val voiceCue: GeminiVoiceCue = GeminiVoiceCue.DEFAULT,
    val options: List<ScriptedOption>
)

private data class ScriptedOption(
    val text: String,
    val translation: String
)

private class ScriptedRuntime(val scenario: ScriptedScenario) {
    var turnPointer: Int = 0
}

private val scriptedScenarioDefinitions: Map<String, ScriptedScenario> = mapOf(
    "rp_tarsier_morning" to ScriptedScenario(
        turns = listOf(
            ScriptedTurn(
                aiText = "Maayong buntag! Ako si Tarsier Master Tali. Handom ko nga ma-init imong adlaw.",
                translation = "おはよう！タルシエ先生タリだよ。今日も良い朝にしよう。",
                options = listOf(
                    ScriptedOption(
                        text = "Maayong buntag, Tali! Andam ko sa leksyon.",
                        translation = "おはようタリ！レッスンの準備できてるよ。"
                    ),
                    ScriptedOption(
                        text = "Maayong buntag! Nalipay ko makakita nimo.",
                        translation = "おはよう！会えてうれしいよ。"
                    )
                )
            ),
            ScriptedTurn(
                aiText = "Kumusta ka? Murag daghan ka'g enerhiya ron!",
                translation = "調子はどう？エネルギー満タンみたいだね！",
                voiceCue = GeminiVoiceCue.WHISPER,
                options = listOf(
                    ScriptedOption(
                        text = "Maayo ra ko, salamat! Motivated ko kaayo.",
                        translation = "元気だよ、ありがとう！やる気満々。"
                    ),
                    ScriptedOption(
                        text = "Gikapoy gamay pero padayon gihapon.",
                        translation = "ちょっと疲れてるけど頑張るよ。"
                    )
                )
            ),
            ScriptedTurn(
                aiText = "Sige, babay! Balika ko unya ha, aron mas kusgan imong Bisaya.",
                translation = "じゃあね！また戻ってきて、もっとビサヤ語を鍛えよう！",
                options = listOf(
                    ScriptedOption(
                        text = "Sige, babay! Kita ta puhon.",
                        translation = "またね！また会おう。"
                    ),
                    ScriptedOption(
                        text = "Daghang salamat, Master Tali!",
                        translation = "ありがとう、タリ先生！"
                    )
                )
            )
        )
    )
)
