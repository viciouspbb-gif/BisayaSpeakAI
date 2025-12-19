package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.SourceLang
import com.bisayaspeak.ai.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 翻訳画面のUI状態
 */
sealed interface TranslateUiState {
    object Idle : TranslateUiState
    object Loading : TranslateUiState
    data class Error(val message: String) : TranslateUiState
    object Success : TranslateUiState
}

/**
 * 翻訳機能のViewModel
 * 
 * 【仕様】
 * - 入力言語: 日本語 or 英語
 * - 翻訳先: ビサヤ語（セブアノ語）固定
 * - UIState管理: Idle, Loading, Error, Success
 */
class TranslateViewModel : ViewModel() {
    
    private val repository = ConversationRepository()
    
    // 入力言語（デフォルト: 日本語）
    private val _sourceLanguage = MutableStateFlow(SourceLang.JAPANESE)
    val sourceLanguage: StateFlow<SourceLang> = _sourceLanguage.asStateFlow()
    
    // 入力テキスト
    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText.asStateFlow()
    
    // 翻訳結果テキスト
    private val _targetText = MutableStateFlow("")
    val targetText: StateFlow<String> = _targetText.asStateFlow()
    
    // UI状態
    private val _uiState = MutableStateFlow<TranslateUiState>(TranslateUiState.Idle)
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()
    
    /**
     * 入力言語を変更
     */
    fun onSourceLanguageChange(lang: SourceLang) {
        _sourceLanguage.value = lang
        // 言語変更時は翻訳結果をクリア
        _targetText.value = ""
        _uiState.value = TranslateUiState.Idle
    }

    /**
     * 入力言語をトグル（日本語 ⇄ 英語）
     */
    fun toggleSourceLanguage() {
        val nextLang = when (_sourceLanguage.value) {
            SourceLang.JAPANESE -> SourceLang.ENGLISH
            SourceLang.ENGLISH -> SourceLang.JAPANESE
        }
        onSourceLanguageChange(nextLang)
    }
    
    /**
     * 入力テキストを変更
     */
    fun onSourceTextChange(text: String) {
        _sourceText.value = text
    }
    
    /**
     * 翻訳を実行
     * 入力言語からビサヤ語（セブアノ語）へ翻訳
     */
    fun translate() {
        val text = _sourceText.value
        
        // 空文字チェック
        if (text.isBlank()) {
            _uiState.value = TranslateUiState.Error("翻訳するテキストを入力してください")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = TranslateUiState.Loading
            
            try {
                // Repository の翻訳メソッドを呼び出し
                // 現在のAPIは言語指定なしで日本語/英語を自動判定
                val result = repository.translateToVisayan(text)
                
                result.onSuccess { translatedText ->
                    _targetText.value = translatedText
                    _uiState.value = TranslateUiState.Success
                }.onFailure { error ->
                    _uiState.value = TranslateUiState.Error(
                        error.message ?: "翻訳に失敗しました"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TranslateUiState.Error(
                    e.message ?: "エラーが発生しました"
                )
            }
        }
    }
    
    /**
     * 翻訳結果とエラーをクリア
     */
    fun clearTranslation() {
        _targetText.value = ""
        _uiState.value = TranslateUiState.Idle
    }
    
    /**
     * すべてをリセット
     */
    fun reset() {
        _sourceText.value = ""
        _targetText.value = ""
        _sourceLanguage.value = SourceLang.JAPANESE
        _uiState.value = TranslateUiState.Idle
    }
}
