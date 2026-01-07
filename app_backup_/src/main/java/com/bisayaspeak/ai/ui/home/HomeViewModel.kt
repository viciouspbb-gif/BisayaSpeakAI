package com.bisayaspeak.ai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.UsageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeStatus(
    val currentLevel: Int = 1,
    val totalXp: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository(application)

    private val levelFlow = usageRepository.getCurrentLevel()
    private val xpFlow = usageRepository.getTotalXP()

    val homeStatus: StateFlow<HomeStatus> = combine(levelFlow, xpFlow) { level, xp ->
        HomeStatus(
            currentLevel = level,
            totalXp = xp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeStatus()
    )
}
