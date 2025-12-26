package com.bisayaspeak.ai.ui.roleplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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

data class RoleplayUiState(
    val currentScenario: RoleplayScenarioDefinition? = null,
    val missionGoal: String = "",
    val aiCharacterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = "",
    val isLoading: Boolean = false
)

class RoleplayChatViewModel : ViewModel() {

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

    fun loadScenario(scenarioId: String) {
        val definition = getRoleplayScenarioDefinition(scenarioId)

        // 単語プールを初期化（シャッフルしてセット）
        val chips = defaultWordPool.map { text ->
            WordChip(text = text)
        }.shuffled()

        _uiState.update {
            it.copy(
                currentScenario = definition,
                missionGoal = definition.goal,
                aiCharacterName = definition.aiRole,
                systemPrompt = definition.systemPrompt,
                messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = definition.initialMessage,
                        isUser = false
                    )
                ),
                isLoading = false
            )
        }
        _availableWords.value = chips
        _selectedWords.value = emptyList()
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

    fun sendMessage() {
        val currentSentence = _selectedWords.value.joinToString(" ") { it.text }
        if (currentSentence.isBlank()) return

        // 1. ユーザーメッセージ表示
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = currentSentence,
            isUser = true
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true
            )
        }

        // 入力エリアをクリア＆単語プールをリセット
        _selectedWords.value = emptyList()
        _availableWords.update { list -> list.map { it.copy(isSelected = false) } }

        // 2. AI返答（モック）
        viewModelScope.launch {
            delay(1500)
            val aiResponseText = mockAiResponse(currentSentence, _uiState.value.currentScenario)
            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = aiResponseText,
                isUser = false
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + aiMsg,
                    isLoading = false
                )
            }
        }
    }

    private fun mockAiResponse(input: String, scenario: RoleplayScenarioDefinition?): String {
        return if (input.contains("Gusto", ignoreCase = true)) {
            "Ah, sige sige. Pila kabuok? (ああ、はいはい。いくつ？)"
        } else if (input.contains("Salamat", ignoreCase = true)) {
            "Way sapayan! (どういたしまして！)"
        } else {
            "Okey, unsa pa? (オーケー、他には？)"
        }
    }
}
