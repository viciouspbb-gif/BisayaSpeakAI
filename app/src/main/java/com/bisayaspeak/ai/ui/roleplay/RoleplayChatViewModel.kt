package com.bisayaspeak.ai.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.data.repository.GeminiMissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// 選択肢の単語データ
data class WordChip(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isSelected: Boolean = false
)

data class ReviewItem(
    val id: String = UUID.randomUUID().toString(),
    val scenarioId: String,
    val scenarioTitle: String,
    val phrase: String,
    val translation: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RoleplayUiState(
    val currentScenario: RoleplayScenarioDefinition? = null,
    val missionGoal: String = "",
    val aiCharacterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = "",
    val isLoading: Boolean = false
)

class RoleplayChatViewModel(
    private val repository: GeminiMissionRepository = GeminiMissionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoleplayUiState())
    val uiState: StateFlow<RoleplayUiState> = _uiState.asStateFlow()

    // 選択可能な単語プール（本来はシナリオごとに用意するが、今は固定）
    private val defaultWordPool = listOf(
        "Gusto", "ko", "mopalit", "ug", "tubig",
        "palihug", "tagpila", "ni?", "Bayad", "Salamat"
    )

    // 画面で選択されている単語リスト（入力欄）
    private val _selectedWords = MutableStateFlow<List<WordChip>>(emptyList())
    val selectedWords: StateFlow<List<WordChip>> = _selectedWords.asStateFlow()

    // 選択肢として表示する単語リスト（下のプール）
    private val _availableWords = MutableStateFlow<List<WordChip>>(emptyList())
    val availableWords: StateFlow<List<WordChip>> = _availableWords.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _reviewItems = MutableStateFlow<List<ReviewItem>>(emptyList())
    val reviewItems: StateFlow<List<ReviewItem>> = _reviewItems.asStateFlow()

    private val history = mutableListOf<MissionHistoryMessage>()

    fun loadScenario(scenarioId: String) {
        val definition = getRoleplayScenarioDefinition(scenarioId)

        // 単語プールを初期化（シャッフルしてセット）
        val chips = defaultWordPool.map { text ->
            WordChip(text = text)
        }.shuffled()

        history.clear()
        val openingMessage = definition.initialMessage.trim()
        history.add(MissionHistoryMessage(openingMessage, isUser = false))

        _uiState.update {
            it.copy(
                currentScenario = definition,
                missionGoal = definition.goal,
                aiCharacterName = definition.aiRole,
                systemPrompt = definition.systemPrompt,
                messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = openingMessage,
                        isUser = false
                    )
                ),
                isLoading = false
            )
        }
        _availableWords.value = chips
        _selectedWords.value = emptyList()
        _inputText.value = ""
    }

    fun onInputTextChange(newText: String) {
        _inputText.value = newText
    }

    // 単語をタップした時の処理（プールから入力欄へ）
    fun onWordClick(chip: WordChip) {
        if (!chip.isSelected) {
            _selectedWords.update { list -> list + chip }
            _availableWords.update { list ->
                list.map { if (it.id == chip.id) it.copy(isSelected = true) else it }
            }
            // TODO: ここで音声再生 (TTS) を呼ぶ
        }
    }

    // 入力欄の単語をタップして戻す処理（入力欄からプールへ）
    fun onSelectedWordClick(chip: WordChip) {
        _selectedWords.update { list -> list.filter { it.id != chip.id } }
        _availableWords.update { list ->
            list.map { if (it.id == chip.id) it.copy(isSelected = false) else it }
        }
    }

    fun sendSelectedWordsMessage() {
        val currentSentence = _selectedWords.value.joinToString(" ") { it.text }
        if (currentSentence.isBlank()) return

        sendMessage(currentSentence)

        // 入力エリアをクリア＆単語プールをリセット
        _selectedWords.value = emptyList()
        _availableWords.update { list -> list.map { it.copy(isSelected = false) } }
    }

    fun sendHintPhrase(hintPhrase: HintPhrase) {
        sendMessage(
            userText = hintPhrase.nativeText,
            fromHint = true,
            translation = hintPhrase.translation
        )
    }

    fun sendMessage(userText: String, fromHint: Boolean = false, translation: String? = null) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val scenarioForReview = if (fromHint) _uiState.value.currentScenario else null

        // 1. ユーザーメッセージ表示
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            isUser = true
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true
            )
        }

        _inputText.value = ""
        history.add(MissionHistoryMessage(trimmed, isUser = true))

        if (fromHint && scenarioForReview != null && translation != null) {
            val reviewItem = ReviewItem(
                scenarioId = scenarioForReview.id,
                scenarioTitle = scenarioForReview.title,
                phrase = trimmed,
                translation = translation,
                timestamp = System.currentTimeMillis()
            )
            _reviewItems.update { it + reviewItem }
        }

        val scenario = _uiState.value.currentScenario ?: return

        viewModelScope.launch {
            val result = repository.generateRoleplayReply(
                systemPrompt = scenario.systemPrompt,
                history = history.toList(),
                userMessage = trimmed
            )

            result.onSuccess { reply ->
                val aiMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = reply,
                    isUser = false
                )
                history.add(MissionHistoryMessage(reply, isUser = false))
                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMsg,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                val fallbackText = "AIの応答取得に失敗しました: ${error.message ?: "Unknown error"}"
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = fallbackText,
                    isUser = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMsg,
                        isLoading = false
                    )
                }
            }
        }
    }
}
