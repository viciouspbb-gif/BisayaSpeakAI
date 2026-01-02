package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.data.repository.GeminiMissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TranslatorUiState {
    object Idle : TranslatorUiState
    object Loading : TranslatorUiState
    data class Error(val message: String) : TranslatorUiState
    object Success : TranslatorUiState
}

class AiTranslatorViewModel(
    private val repository: GeminiMissionRepository = GeminiMissionRepository()
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _direction = MutableStateFlow(TranslationDirection.JA_TO_CEB)
    val direction: StateFlow<TranslationDirection> = _direction.asStateFlow()

    private val _uiState = MutableStateFlow<TranslatorUiState>(TranslatorUiState.Idle)
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun swapDirection() {
        _direction.value = when (_direction.value) {
            TranslationDirection.JA_TO_CEB -> TranslationDirection.CEB_TO_JA
            TranslationDirection.CEB_TO_JA -> TranslationDirection.JA_TO_CEB
        }
        _translatedText.value = ""
        _uiState.value = TranslatorUiState.Idle
    }

    fun clearAll() {
        _inputText.value = ""
        _translatedText.value = ""
        _uiState.value = TranslatorUiState.Idle
    }

    fun translate() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _uiState.value = TranslatorUiState.Error("翻訳するテキストを入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading
            repository.translate(text, _direction.value)
                .onSuccess { result ->
                    _translatedText.value = result
                    _uiState.value = TranslatorUiState.Success
                }
                .onFailure { throwable ->
                    _uiState.value =
                        TranslatorUiState.Error(throwable.message ?: "翻訳に失敗しました")
                }
        }
    }
}
