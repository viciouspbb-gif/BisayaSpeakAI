package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_SANPO_TURN = 12
private const val FAREWELL_PHASE_TURN = 10

/**
 * SANPO（タリ散歩道）専用の会話ViewModel。
 * MissionTalkViewModelやDOJOロジックから完全に分離する。
 */
class SanpoTalkViewModel(
    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository(),
    private val baseSystemPrompt: String = DEFAULT_SANPO_SYSTEM_PROMPT
) : ViewModel() {

    private var turnCount = 0

    private val _uiState = MutableStateFlow(SanpoTalkUiState())
    val uiState: StateFlow<SanpoTalkUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage() {
        val currentState = _uiState.value

        if (currentState.isSending || currentState.isSessionEnded) return

        val trimmed = currentState.inputText.trim()
        if (trimmed.isEmpty()) return

        if (turnCount >= MAX_SANPO_TURN) {
            _uiState.update {
                it.copy(
                    inputText = "",
                    isSessionEnded = true
                )
            }
            return
        }

        val nextTurn = turnCount + 1
        turnCount = nextTurn

        val userMessage = SanpoChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )

        val updatedMessages = currentState.messages + userMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isSending = true,
                turnCount = nextTurn,
                isFarewellPhase = nextTurn >= FAREWELL_PHASE_TURN
            )
        }

        viewModelScope.launch {
            try {

                val reply = chatRepository.generateSanpoReply(
                    baseSystemPrompt = baseSystemPrompt,
                    userMessage = trimmed,
                    turnCount = nextTurn
                )

                val aiMessage = SanpoChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = reply.trim().ifBlank { "..." },
                    isUser = false
                )

                val shouldEnd =
                    reply.contains("[TOPページへ]") ||
                    nextTurn >= MAX_SANPO_TURN

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isSending = false,
                        isSessionEnded = state.isSessionEnded || shouldEnd
                    )
                }

            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = t.message
                            ?: "SANPOモードの応答取得に失敗しました。"
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_SANPO_SYSTEM_PROMPT = """
            【SANPO｜タリ散歩道】
            - あなたはフィリピン在住の親友 Tari。敬語は禁止、タメ口でラフに話す。
            - 1ターンにつき2行以内の軽い雑談にとどめ、すぐ次の話題を投げること。
            - 12ターンで必ず帰宅し、どれだけ盛り上がっても延長しない。
            - 選択肢やJSONは使わず、シンプルなテキストのみで返答する。
        """.trimIndent()
    }
}

data class SanpoChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)

data class SanpoTalkUiState(
    val messages: List<SanpoChatMessage> = emptyList(),
    val inputText: String = "",
    val turnCount: Int = 0,
    val isFarewellPhase: Boolean = false,
    val isSessionEnded: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
