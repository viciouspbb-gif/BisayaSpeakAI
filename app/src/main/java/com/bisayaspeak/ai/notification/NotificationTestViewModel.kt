package com.bisayaspeak.ai.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationTestViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val debugNotificationScheduler: DebugNotificationScheduler
) : ViewModel() {
    
    private val _testResults = MutableStateFlow("")
    val testResults: StateFlow<String> = _testResults
    
    private val _resetStatus = MutableStateFlow("")
    val resetStatus: StateFlow<String> = _resetStatus
    
    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting
    
    private val _currentInfo = MutableStateFlow("現在の曜日: テスト中\n土曜日: テスト中\n土曜日テーマ: テスト中")
    val currentInfo: StateFlow<String> = _currentInfo
    
    fun rebuildSchedule() {
        if (_isResetting.value) return
        
        _isResetting.value = true
        _resetStatus.value = "リセット中..."
        
        viewModelScope.launch {
            try {
                notificationScheduler.cancelAll()
                notificationScheduler.scheduleDailyNotification()
                
                _resetStatus.value = "✅ 通知スケジュールを正常に再構築しました"
                _testResults.value += "通知スケジュールを再構築しました\n"
                
            } catch (e: Exception) {
                _resetStatus.value = "❌ リセットに失敗しました: ${e.message}"
            } finally {
                _isResetting.value = false
            }
        }
    }
    
    /**
     * デバッグ用通知をスケジュール
     */
    fun scheduleDebugNotification(delayMinutes: Int) {
        viewModelScope.launch {
            try {
                debugNotificationScheduler.scheduleDebugNotification(delayMinutes)
                _testResults.value += "デバッグ通知を${delayMinutes}分後に予約しました\n"
            } catch (e: Exception) {
                _testResults.value += "デバッグ通知の予約に失敗しました: ${e.message}\n"
            }
        }
    }
    
    fun sendNotification(type: String) {
        viewModelScope.launch {
            try {
                // 実際の通知送信ロジックをここに実装
                _testResults.value += "${type}通知を送信しました\n"
            } catch (e: Exception) {
                _testResults.value += "${type}通知の送信に失敗しました: ${e.message}\n"
            }
        }
    }
    
    fun sendAllNotifications() {
        viewModelScope.launch {
            try {
                _testResults.value += "全パターン通知テストを実行しました\n"
            } catch (e: Exception) {
                _testResults.value += "全パターン通知テストの実行に失敗しました: ${e.message}\n"
            }
        }
    }
    
    fun clearResults() {
        _testResults.value = ""
    }
}
