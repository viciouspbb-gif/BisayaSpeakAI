package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.ListeningDataRepository
import com.bisayaspeak.ai.data.repository.TimeReleaseRepository
import com.bisayaspeak.ai.data.repository.FirebaseDataCleanupRepository
import com.bisayaspeak.ai.work.LevelReleaseScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * リスニングデータ管理ViewModel
 * Firebaseへのデータ追加とタイムリリース管理
 */
@HiltViewModel
class ListeningDataViewModel @Inject constructor(
    private val listeningDataRepository: ListeningDataRepository,
    private val timeReleaseRepository: TimeReleaseRepository,
    private val levelReleaseScheduler: LevelReleaseScheduler,
    private val cleanupRepository: FirebaseDataCleanupRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ListeningDataUiState>(ListeningDataUiState.Idle)
    val uiState: StateFlow<ListeningDataUiState> = _uiState.asStateFlow()
    
    /**
     * LV31データをFirebaseに追加
     */
    fun addLevel31Data() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            val result = listeningDataRepository.addLevel31Data()
            
            when {
                result.isSuccess -> {
                    _uiState.value = ListeningDataUiState.Success("LV31データをFirebaseに追加しました")
                }
                result.isFailure -> {
                    _uiState.value = ListeningDataUiState.Error(
                        result.exceptionOrNull()?.message ?: "データ追加に失敗しました"
                    )
                }
            }
        }
    }
    
    /**
     * LV32-35データをFirebaseに一括追加
     */
    fun addLevel32To35Data() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            val result = listeningDataRepository.addLevel32To35Data()
            
            when {
                result.isSuccess -> {
                    _uiState.value = ListeningDataUiState.Success("LV32-35データをFirebaseに一括追加しました")
                    // 通知スケジュールを設定
                    levelReleaseScheduler.scheduleAllLevelReleases()
                }
                result.isFailure -> {
                    _uiState.value = ListeningDataUiState.Error(
                        result.exceptionOrNull()?.message ?: "データ追加に失敗しました"
                    )
                }
            }
        }
    }
    
    /**
     * 緊急ロールバック：LV32-35データを削除
     */
    fun rollbackLevel32To35Data() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            val result = cleanupRepository.rollbackLevel32To35Data()
            
            when {
                result.isSuccess -> {
                    _uiState.value = ListeningDataUiState.Success("LV32-35データを削除しました（ロールバック完了）")
                }
                result.isFailure -> {
                    _uiState.value = ListeningDataUiState.Error(
                        result.exceptionOrNull()?.message ?: "ロールバックに失敗しました"
                    )
                }
            }
        }
    }
    
    /**
     * 現在のデータ状態を確認
     */
    fun checkCurrentDataStatus() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            try {
                val statusResult = cleanupRepository.checkCurrentDataStatus()
                if (statusResult.isSuccess) {
                    val existingIds = statusResult.getOrNull() ?: emptyList()
                    _uiState.value = ListeningDataUiState.DataStatusLoaded(existingIds)
                } else {
                    _uiState.value = ListeningDataUiState.Error(
                        statusResult.exceptionOrNull()?.message ?: "データ状態取得に失敗しました"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ListeningDataUiState.Error(
                    e.message ?: "データ状態取得に失敗しました"
                )
            }
        }
    }
    
    /**
     * すべてのリスニングデータを取得
     */
    fun getAllListeningData() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            val result = listeningDataRepository.getAllListeningData()
            
            when {
                result.isSuccess -> {
                    val data = result.getOrNull()
                    _uiState.value = ListeningDataUiState.DataLoaded(data ?: emptyList())
                }
                result.isFailure -> {
                    _uiState.value = ListeningDataUiState.Error(
                        result.exceptionOrNull()?.message ?: "データ取得に失敗しました"
                    )
                }
            }
        }
    }
    
    /**
     * レベル解放スケジュールを取得
     */
    fun getReleaseSchedules() {
        viewModelScope.launch {
            _uiState.value = ListeningDataUiState.Loading
            
            try {
                val schedules = timeReleaseRepository.getAllReleaseSchedules()
                _uiState.value = ListeningDataUiState.ReleaseSchedulesLoaded(schedules)
            } catch (e: Exception) {
                _uiState.value = ListeningDataUiState.Error(
                    e.message ?: "スケジュール取得に失敗しました"
                )
            }
        }
    }
    
    /**
     * 通知スケジュールを設定
     */
    fun scheduleNotifications() {
        viewModelScope.launch {
            try {
                levelReleaseScheduler.scheduleAllLevelReleases()
                _uiState.value = ListeningDataUiState.Success("通知スケジュールを設定しました")
            } catch (e: Exception) {
                _uiState.value = ListeningDataUiState.Error(
                    e.message ?: "通知スケジュール設定に失敗しました"
                )
            }
        }
    }
    
    /**
     * UI状態をリセット
     */
    fun resetUiState() {
        _uiState.value = ListeningDataUiState.Idle
    }
}

/**
 * UI状態の定義
 */
sealed class ListeningDataUiState {
    data object Idle : ListeningDataUiState()
    data object Loading : ListeningDataUiState()
    data class Success(val message: String) : ListeningDataUiState()
    data class DataLoaded(val data: List<Map<String, Any>>) : ListeningDataUiState()
    data class DataStatusLoaded(val existingIds: List<Long>) : ListeningDataUiState()
    data class ReleaseSchedulesLoaded(val schedules: List<com.bisayaspeak.ai.data.repository.TimeReleaseRepository.LevelReleaseSchedule>) : ListeningDataUiState()
    data class Error(val message: String) : ListeningDataUiState()
}
