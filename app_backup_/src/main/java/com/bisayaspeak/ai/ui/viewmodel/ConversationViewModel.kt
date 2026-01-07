package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.*
import com.bisayaspeak.ai.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI状態（3種類のみ）
 */
sealed class ConversationUiState {
    object Idle : ConversationUiState()
    object Loading : ConversationUiState()
    data class Error(val message: String) : ConversationUiState()
}

/**
 * AI会話機能のViewModel
 * 
 * 【仕様】
 * 1. 最初にAIが自動で話し始める（startConversation）
 * 2. テンプレート縛りなし - 任意の文章を送信可能
 * 3. 会話履歴はConversationTurnのリストで管理
 * 4. UI Stateは3つ（Idle, Loading, Error）
 */
class ConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConversationRepository()

    // 会話履歴
    private val _conversationTurns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationTurns: StateFlow<List<ConversationTurn>> = _conversationTurns

    // UI状態
    private val _uiState = MutableStateFlow<ConversationUiState>(ConversationUiState.Idle)
    val uiState: StateFlow<ConversationUiState> = _uiState

    // 会話設定（保持用）
    private var currentScenario: String = ""
    private var currentMode: ConversationMode = ConversationMode.FREE_TALK
    private var currentLevel: String = "beginner"

    /**
     * 会話を開始する（初回AIメッセージ生成）
     * 会話履歴が空のときだけ実行される
     * 
     * @param scenario シナリオID（フリートークの場合は空文字）
     * @param mode 会話モード
     * @param level 学習レベル（"beginner", "intermediate", "advanced"）
     */
    fun startConversation(
        scenario: String = "",
        mode: ConversationMode = ConversationMode.FREE_TALK,
        level: String = "beginner"
    ) {
        // 会話履歴が既にある場合は何もしない
        if (_conversationTurns.value.isNotEmpty()) {
            return
        }

        // 設定を保存
        currentScenario = scenario
        currentMode = mode
        currentLevel = level

        viewModelScope.launch {
            _uiState.value = ConversationUiState.Loading

            val result = repository.createOpeningTurn(scenario, mode, level)

            result.onSuccess { turn ->
                // AIの初回メッセージを履歴に追加
                _conversationTurns.value = listOf(turn)
                _uiState.value = ConversationUiState.Idle
            }.onFailure { error ->
                _uiState.value = ConversationUiState.Error(
                    error.message ?: "会話の開始に失敗しました"
                )
            }
        }
    }

    /**
     * ユーザーメッセージを送信してAI返信を取得
     * 
     * 【仕様】
     * - 空文字（空白のみ）の場合は拒否
     * - テンプレート縛りなし - 任意の文章を送信可能
     * - ユーザー送信 → 履歴に追加 → AI返信 → 履歴に追加
     * 
     * @param text ユーザーが入力したメッセージ
     */
    fun sendUserMessage(text: String) {
        // 空文字チェック（空白のみも拒否）
        if (text.isBlank()) {
            _uiState.value = ConversationUiState.Error("メッセージを入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = ConversationUiState.Loading

            // ユーザーのメッセージを履歴に追加
            val userTurn = ConversationTurn(
                speaker = Speaker.USER,
                text = text,
                translation = ""
            )
            val updatedHistory = _conversationTurns.value + userTurn
            _conversationTurns.value = updatedHistory

            // AI返信を取得
            val result = repository.reply(
                history = updatedHistory,
                scenario = currentScenario,
                mode = currentMode,
                level = currentLevel
            )

            result.onSuccess { aiTurn ->
                // AIの返信を履歴に追加
                _conversationTurns.value = updatedHistory + aiTurn
                _uiState.value = ConversationUiState.Idle
            }.onFailure { error ->
                _uiState.value = ConversationUiState.Error(
                    error.message ?: "AI返信の取得に失敗しました"
                )
            }
        }
    }

    /**
     * 会話履歴をクリア
     */
    fun clearConversation() {
        _conversationTurns.value = emptyList()
        _uiState.value = ConversationUiState.Idle
    }

    /**
     * エラー状態をクリア
     */
    fun clearError() {
        if (_uiState.value is ConversationUiState.Error) {
            _uiState.value = ConversationUiState.Idle
        }
    }
}
