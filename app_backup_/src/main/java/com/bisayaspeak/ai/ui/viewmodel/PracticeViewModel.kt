package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.PracticeData
import com.bisayaspeak.ai.data.model.PracticeItem
import com.bisayaspeak.ai.data.repository.PronunciationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 練習画面のViewModel
 */
class PracticeViewModel : ViewModel() {
    
    private val pronunciationRepository = PronunciationRepository()
    
    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Checking)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()
    
    // 全Practiceデータ
    private val _practiceItems = MutableStateFlow<List<PracticeItem>>(PracticeData.allItems)
    val practiceItems: StateFlow<List<PracticeItem>> = _practiceItems.asStateFlow()
    
    // カテゴリ別にグループ化
    private val _groupedItems = MutableStateFlow<Map<String, List<PracticeItem>>>(emptyMap())
    val groupedItems: StateFlow<Map<String, List<PracticeItem>>> = _groupedItems.asStateFlow()
    
    init {
        checkServerHealth()
        loadPracticeItems()
        viewModelScope.launch {
            _groupedItems.value = practiceItems.value.groupBy { it.category }
        }
    }
    
    private fun loadPracticeItems() {
        viewModelScope.launch {
            // カテゴリ別にグループ化
            _groupedItems.value = PracticeData.allItems.groupBy { it.category }
        }
    }
    
    /**
     * 特定カテゴリのアイテムを取得
     */
    fun getItemsByCategory(category: String): List<PracticeItem> {
        return _practiceItems.value.filter { it.category == category }
    }
    
    /**
     * IDでアイテムを取得
     */
    fun getItemById(id: String): PracticeItem? {
        return _practiceItems.value.find { it.id == id }
    }
    
    /**
     * サーバーのヘルスチェック
     */
    fun checkServerHealth() {
        viewModelScope.launch {
            android.util.Log.d("PracticeViewModel", "サーバーヘルスチェック開始")
            _serverStatus.value = ServerStatus.Checking
            
            val result = pronunciationRepository.checkServerHealth()
            
            _serverStatus.value = if (result.isSuccess && result.getOrNull() == true) {
                android.util.Log.d("PracticeViewModel", "サーバー接続成功")
                ServerStatus.Online
            } else {
                android.util.Log.e("PracticeViewModel", "サーバー接続失敗: ${result.exceptionOrNull()?.message}")
                ServerStatus.Offline(result.exceptionOrNull()?.message ?: "サーバーに接続できません")
            }
        }
    }
}

/**
 * サーバー状態
 */
sealed class ServerStatus {
    object Checking : ServerStatus()
    object Online : ServerStatus()
    data class Offline(val message: String) : ServerStatus()
}
