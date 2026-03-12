package com.bisayaspeak.ai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.domain.xp.XpProgressManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val lessonsRemainingToNext: Int = 0,
    val xpLevel: Int = 1,
    val xpCurrent: Int = 0,
    val xpRequired: Int = 100,
    val xpProgressFraction: Float = 0f,
    val xpHighlightTick: Int = 0,
    val xpLevelUpTick: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository(application)

    private val lessonsFlow = usageRepository.getTotalLessonsCompleted()
    private var lastXpLevel = 1
    private var lastXpValue = 0
    private var highlightTick = 0
    private var levelUpTick = 0
    private var xpBootstrapped = false

    private val xpFlow = usageRepository.getXpState()
        .map { xpState ->
            val requiredXp = XpProgressManager.requiredXp(xpState.teacherLevel)
            if (xpBootstrapped) {
                val leveledUp = xpState.teacherLevel > lastXpLevel
                val gainedXp = leveledUp || xpState.xpIntoLevel > lastXpValue
                if (leveledUp) {
                    levelUpTick += 1
                }
                if (gainedXp) {
                    highlightTick += 1
                }
            } else {
                xpBootstrapped = true
            }
            lastXpLevel = xpState.teacherLevel
            lastXpValue = xpState.xpIntoLevel
            XpUiState(
                level = xpState.teacherLevel,
                currentXp = xpState.xpIntoLevel,
                requiredXp = requiredXp,
                progressFraction = (xpState.xpIntoLevel.toFloat() / requiredXp).coerceIn(0f, 1f),
                highlightTick = highlightTick,
                levelUpTick = levelUpTick
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = XpUiState()
        )

    val homeStatus: StateFlow<HomeStatus> = combine(lessonsFlow, xpFlow) { lessonsCompleted, xp ->
        val honor = LevelHonorHelper.getHonorInfo(application.applicationContext, xp.level)
        val xpRemaining = (xp.requiredXp - xp.currentXp).coerceAtLeast(0)
        HomeStatus(
            currentLevel = xp.level,
            totalLessonsCompleted = lessonsCompleted,
            honorTitle = honor.title,
            honorNickname = honor.nickname,
            progressToNextLevel = xp.progressFraction,
            lessonsForCurrentLevel = xp.requiredXp,
            lessonsForNextLevel = xp.requiredXp,
            lessonsRemainingToNext = xpRemaining,
            xpLevel = xp.level,
            xpCurrent = xp.currentXp,
            xpRequired = xp.requiredXp,
            xpProgressFraction = xp.progressFraction,
            xpHighlightTick = xp.highlightTick,
            xpLevelUpTick = xp.levelUpTick
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeStatus()
    )
}

private data class XpUiState(
    val level: Int = 1,
    val currentXp: Int = 0,
    val requiredXp: Int = 100,
    val progressFraction: Float = 0f,
    val highlightTick: Int = 0,
    val levelUpTick: Int = 0
)
