package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.MissionChatMessage
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.data.model.getMissionScenario
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class MissionStatus {
    NOT_READY,
    IN_PROGRESS,
    CLEARED,
    FAILED,
    ERROR
}

data class MissionTalkUiState(
    val scenario: MissionScenario? = null,
    val messages: List<MissionChatMessage> = emptyList(),
    val inputText: String = "",
    val remainingTurns: Int = 0,
    val isSending: Boolean = false,
    val missionStatus: MissionStatus = MissionStatus.NOT_READY,
    val showSuccessDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val errorMessage: String? = null
)

class MissionTalkViewModel(
    private val scenarioId: String,
    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository()
) : ViewModel() {

    private val missionScenario: MissionScenario? = getMissionScenario(scenarioId)

    private val history = mutableListOf<MissionHistoryMessage>().apply {
        missionScenario?.openingMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { add(MissionHistoryMessage(text = it, isUser = false)) }
    }

    private val _uiState = MutableStateFlow(
        MissionTalkUiState(
            scenario = missionScenario,
            messages = missionScenario?.openingMessage
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    listOf(
                        MissionChatMessage(
                            primaryText = it,
                            isUser = false
                        )
                    )
                }.orEmpty(),
            remainingTurns = missionScenario?.context?.turnLimit ?: 0,
            missionStatus = missionScenario?.let { MissionStatus.IN_PROGRESS } ?: MissionStatus.ERROR,
            errorMessage = if (missionScenario == null) "ミッション情報が見つかりませんでした" else null
        )
    )
    val uiState: StateFlow<MissionTalkUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun appendHint(hint: String) {
        val current = _uiState.value.inputText
        val appended = if (current.isBlank()) hint else "$current $hint"
        onInputChange(appended.trim())
    }

    fun sendMessage() {
        val scenario = missionScenario ?: return
        val trimmed = _uiState.value.inputText.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return
        if (_uiState.value.remainingTurns <= 0) {
            triggerFailure()
            return
        }

        val userMessage = MissionChatMessage(
            primaryText = trimmed,
            isUser = true
        )

        val updatedMessages = _uiState.value.messages + userMessage
        val newRemaining = (_uiState.value.remainingTurns - 1).coerceAtLeast(0)

        history.add(MissionHistoryMessage(text = trimmed, isUser = true))

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isSending = true,
                remainingTurns = newRemaining,
                missionStatus = MissionStatus.IN_PROGRESS,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val prompt = buildMissionPrompt(scenario, trimmed, newRemaining)
                val rawResponse = chatRepository.generateJsonResponse(prompt)
                val payload = parseMissionReply(rawResponse)
                val aiText = payload.aiResponse.ifBlank { "..." }
                history.add(MissionHistoryMessage(aiText, isUser = false))
                val aiMessage = MissionChatMessage(
                    primaryText = aiText,
                    isUser = false,
                    isGoalFlagged = payload.goalAchieved
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isSending = false,
                        missionStatus = if (payload.goalAchieved) MissionStatus.CLEARED else it.missionStatus
                    )
                }

                if (payload.goalAchieved) {
                    triggerSuccess()
                } else if (_uiState.value.remainingTurns <= 0) {
                    triggerFailure()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = e.message ?: "ミッション応答の取得に失敗しました"
                    )
                }
                if (_uiState.value.remainingTurns <= 0) {
                    triggerFailure()
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun dismissFailedDialog() {
        _uiState.update { it.copy(showFailedDialog = false) }
    }

    private fun triggerSuccess() {
        _uiState.update {
            it.copy(
                missionStatus = MissionStatus.CLEARED,
                showSuccessDialog = true
            )
        }
    }

    private fun triggerFailure() {
        if (_uiState.value.missionStatus == MissionStatus.CLEARED) return
        _uiState.update {
            it.copy(
                missionStatus = MissionStatus.FAILED,
                showFailedDialog = true
            )
        }
    }

    companion object {
        fun factory(scenarioId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MissionTalkViewModel(scenarioId) as T
                }
            }
    }

    private fun buildMissionPrompt(
        scenario: MissionScenario,
        userMessage: String,
        turnsRemaining: Int
    ): String {
        val historyText = history.joinToString(separator = "\n") { entry ->
            val speaker = if (entry.isUser) "USER" else "AI"
            "$speaker: ${entry.text}"
        }.ifBlank { "No previous messages." }

        val hints = scenario.context.hints.joinToString(separator = "\n") { "- $it" }

        return """
            You are ${scenario.context.role}.
            Situation: ${scenario.context.situation}
            Mission Goal: ${scenario.context.goal}
            Tone: ${scenario.context.tone ?: "Friendly and helpful"}
            Provide concise replies primarily in Bisaya with light Japanese support if needed.
            Refer to these useful hint phrases:
            $hints

            Conversation history:
            $historyText

            Turns remaining for the learner: $turnsRemaining
            Latest learner message: $userMessage

            Respond strictly in JSON with the following schema:
            {
              "aiResponse": "string",
              "goalAchieved": true | false
            }
            The aiResponse must be plain text (no markdown). Set goalAchieved true only if the mission goal is clearly met.
        """.trimIndent()
    }

    private fun parseMissionReply(raw: String): MissionAiResponse {
        return try {
            val json = JSONObject(raw)
            MissionAiResponse(
                aiResponse = json.optString("aiResponse", raw),
                goalAchieved = json.optBoolean("goalAchieved", false)
            )
        } catch (_: Exception) {
            MissionAiResponse(aiResponse = raw, goalAchieved = false)
        }
    }

    private data class MissionAiResponse(
        val aiResponse: String,
        val goalAchieved: Boolean
    )
}
