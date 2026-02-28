package com.bisayaspeak.ai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationResetViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    
    private val _resetStatus = MutableStateFlow("")
    val resetStatus: StateFlow<String> = _resetStatus
    
    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting
    
    fun resetSchedule() {
        if (_isResetting.value) return
        
        _isResetting.value = true
        _resetStatus.value = "リセット中..."
        
        viewModelScope.launch {
            try {
                notificationScheduler.cancelAll()
                notificationScheduler.scheduleDailyNotification()
                
                _resetStatus.value = "✅ 通知スケジュールを正常に再構築しました\n\n次の18:00に通知が送信されます。"
                
            } catch (e: Exception) {
                _resetStatus.value = "❌ リセットに失敗しました: ${e.message}"
            } finally {
                _isResetting.value = false
            }
        }
    }
}
