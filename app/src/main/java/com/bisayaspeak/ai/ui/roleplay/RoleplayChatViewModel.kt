package com.bisayaspeak.ai.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.repository.GeminiMissionRepository
import com.bisayaspeak.ai.utils.MistakeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RoleplayOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val hint: String? = null,
    val tone: String? = null
)

data class RoleplayUiState(
    val currentScenario: RoleplayScenarioDefinition? = null,
    val missionGoal: String = "",
    val aiCharacterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = "",
    val isLoading: Boolean = false,
    val options: List<RoleplayOption> = emptyList(),
    val revealedHintOptionIds: Set<String> = emptySet()
)

class RoleplayChatViewModel(
    private val repository: GeminiMissionRepository = GeminiMissionRepository()
) : ViewModel() {

    private val startToken = "[START_CONVERSATION]"

    private val _uiState = MutableStateFlow(RoleplayUiState())
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<MissionHistoryMessage>()

    fun loadScenario(scenarioId: String) {
        val definition = getRoleplayScenarioDefinition(scenarioId)
        history.clear()

        _uiState.value = RoleplayUiState(
            currentScenario = definition,
            missionGoal = definition.goal,
            aiCharacterName = definition.aiRole,
            systemPrompt = definition.systemPrompt,
            messages = emptyList(),
            isLoading = true
        )

        requestAiTurn(
            scenario = definition,
            userMessage = startToken
        )
    }

    fun selectOption(optionId: String) {
        val scenario = _uiState.value.currentScenario ?: return
        if (_uiState.value.isLoading) return
        val option = _uiState.value.options.find { it.id == optionId } ?: return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = option.text,
            isUser = true
        )
        history.add(MissionHistoryMessage(option.text, isUser = true))

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                options = emptyList(),
                revealedHintOptionIds = emptySet()
            )
        }

        requestAiTurn(scenario, option.text)
    }

    fun revealHint(optionId: String) {
        val option = _uiState.value.options.find { it.id == optionId } ?: return
        MistakeManager.addMistake(option.text)
        _uiState.update {
            it.copy(revealedHintOptionIds = it.revealedHintOptionIds + optionId)
        }
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
                        revealedHintOptionIds = emptySet()
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
                revealedHintOptionIds = emptySet()
            )
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
        }

        val basePrompt = if (scenario.systemPrompt.isBlank()) {
            """
            You are ${scenario.aiRole}.
            Situation: ${scenario.situation}
            Goal: ${scenario.goal}
            """
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
