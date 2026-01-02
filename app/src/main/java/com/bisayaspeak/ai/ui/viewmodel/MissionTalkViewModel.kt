package com.bisayaspeak.ai.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.MissionChatMessage
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.data.model.getMissionScenario
import com.bisayaspeak.ai.data.repository.GeminiMissionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isStreaming: Boolean = false,
    val missionStatus: MissionStatus = MissionStatus.NOT_READY,
    val showSuccessDialog: Boolean = false,
    val showFailedDialog: Boolean = false,
    val errorMessage: String? = null
)

class MissionTalkViewModel(
    private val scenarioId: String,
    private val repository: GeminiMissionRepository = GeminiMissionRepository()
) : ViewModel() {

    private val missionScenario: MissionScenario? = getMissionScenario(scenarioId)

    private val history = mutableListOf<MissionHistoryMessage>()
    private var streamJob: Job? = null

    private val _uiState = MutableStateFlow(
        MissionTalkUiState(
            scenario = missionScenario,
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
        Log.d("MissionTalk", "User message sent: $trimmed")

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
                isStreaming = true,
                remainingTurns = newRemaining,
                missionStatus = MissionStatus.IN_PROGRESS
            )
        }

        streamJob?.cancel()
        val job = viewModelScope.launch {
            var latestChunk: MissionChatMessage? = null
            Log.d("MissionTalk", "Requesting AI reply...")
            repository.streamMissionReply(
                context = scenario.context,
                history = history.toList(),
                userMessage = trimmed
            ).collect { chunk ->
                Log.d("MissionTalk", "Received chunk: ${chunk.primaryText}")
                latestChunk = chunk
                applyAiChunk(chunk)
            }

            _uiState.update { it.copy(isSending = false, isStreaming = false) }

            latestChunk?.let { chunk ->
                history.add(MissionHistoryMessage(chunk.primaryText, isUser = false))
                if (chunk.isGoalFlagged) {
                    triggerSuccess()
                } else if (_uiState.value.remainingTurns <= 0) {
                    triggerFailure()
                }
            } ?: run {
                if (_uiState.value.remainingTurns <= 0) triggerFailure()
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause != null) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        isStreaming = false,
                        errorMessage = cause.message ?: "ミッション応答の取得に失敗しました"
                    )
                }
            }
        }
        streamJob = job
    }

    private fun applyAiChunk(chunk: MissionChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        val existingIndex = currentMessages.indexOfFirst { !it.isUser && it.id == chunk.id }

        if (existingIndex >= 0) {
            currentMessages[existingIndex] = chunk
        } else {
            currentMessages += chunk
        }

        _uiState.update {
            it.copy(
                messages = currentMessages,
                missionStatus = MissionStatus.IN_PROGRESS
            )
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

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
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
}
