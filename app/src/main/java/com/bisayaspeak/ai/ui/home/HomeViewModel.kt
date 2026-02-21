package com.bisayaspeak.ai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.domain.honor.HonorLevelManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeStatus(
    val currentLevel: Int = 1,
    val totalLessonsCompleted: Int = 0,
    val honorTitle: String = "",
    val honorNickname: String = "",
    val progressToNextLevel: Float = 0f,
    val lessonsForCurrentLevel: Int = 0,
    val lessonsForNextLevel: Int = 1,
    val lessonsRemainingToNext: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository(application)

    private val lessonsFlow = usageRepository.getTotalLessonsCompleted()

    val homeStatus: StateFlow<HomeStatus> = lessonsFlow.map { lessonsCompleted ->
        val progress = HonorLevelManager.getProgress(lessonsCompleted)
        val honor = LevelHonorHelper.getHonorInfo(application.applicationContext, progress.level)
        HomeStatus(
            currentLevel = progress.level,
            totalLessonsCompleted = lessonsCompleted,
            honorTitle = honor.title,
            honorNickname = honor.nickname,
            progressToNextLevel = progress.progressToNextLevel,
            lessonsForCurrentLevel = progress.lessonsForCurrentLevel,
            lessonsForNextLevel = progress.lessonsForNextLevel,
            lessonsRemainingToNext = progress.lessonsRemainingToNext
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeStatus()
    )
}
