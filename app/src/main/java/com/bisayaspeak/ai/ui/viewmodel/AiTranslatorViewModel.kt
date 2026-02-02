package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.PromptProvider
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
    private val repository: OpenAiChatRepository = OpenAiChatRepository(),
    private val promptProvider: PromptProvider? = null
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
            _uiState.value = TranslatorUiState.Error("Please enter text to translate")
            return
        }

        viewModelScope.launch {
            _uiState.value = TranslatorUiState.Loading
            try {
                val direction = _direction.value
                val result = translateWithOpenAi(text, direction)
                _translatedText.value = sanitizeTranslation(result, direction)
                _uiState.value = TranslatorUiState.Success
            } catch (e: Exception) {
                _uiState.value =
                    TranslatorUiState.Error(e.message ?: "Translation failed")
            }
        }
    }

    private suspend fun translateWithOpenAi(
        text: String,
        direction: TranslationDirection
    ): String {
        val basePrompt = promptProvider?.getSystemPrompt() ?: "You are a helpful translator."
        
        val (systemPrompt, temperature) = when (direction) {
            TranslationDirection.JA_TO_CEB -> {
                "$basePrompt Convert the user's Japanese message into natural Bisaya suitable for friendly conversation. Respond with Bisaya only." to 0.2
            }
            TranslationDirection.CEB_TO_JA -> {
                "$basePrompt Convert the user's Bisaya (Cebuano) message into natural Japanese. Respond with Japanese only." to 0.2
            }
        }
        return repository.sendPrompt(
            systemPrompt = systemPrompt,
            userPrompt = text,
            temperature = temperature
        )
    }

    private fun sanitizeTranslation(
        raw: String,
        direction: TranslationDirection
    ): String {
        if (BuildConfig.DEBUG) return raw
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        fun Char.isJapaneseChar(): Boolean {
            val code = code
            return (code in 0x3040..0x30FF) ||
                (code in 0x3400..0x4DBF) ||
                (code in 0x4E00..0x9FFF) ||
                (code in 0xF900..0xFAFF)
        }

        return when (direction) {
            TranslationDirection.JA_TO_CEB -> {
                val firstJapaneseIndex = trimmed.indexOfFirst { it.isJapaneseChar() }
                val bisayaOnly = if (firstJapaneseIndex >= 0) {
                    trimmed.substring(0, firstJapaneseIndex).trim().trimEnd { it == '-' || it == '–' }
                } else {
                    trimmed
                }
                if (bisayaOnly.isNotBlank()) bisayaOnly else trimmed
            }

            TranslationDirection.CEB_TO_JA -> {
                val firstJapaneseIndex = trimmed.indexOfFirst { it.isJapaneseChar() }
                if (firstJapaneseIndex >= 0) {
                    trimmed.substring(firstJapaneseIndex).trim()
                } else {
                    trimmed
                }
            }
        }
    }
}
