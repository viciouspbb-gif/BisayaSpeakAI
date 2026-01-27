package com.bisayaspeak.ai.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeStatus(
    val currentLevel: Int = 1,
    val totalXp: Int = 0,
    val honorTitle: String = "",
    val honorNickname: String = "",
    val progressToNextLevel: Float = 0f,
    val xpForCurrentLevel: Int = 0,
    val xpForNextLevel: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val usageRepository = UsageRepository(application)

    private val xpFlow = usageRepository.getTotalXP()

    private val levelFlow: Flow<Int> = xpFlow.map { xp ->
        when {
            xp >= 1000 -> 30
            xp >= 900 -> 29
            xp >= 800 -> 28
            xp >= 700 -> 27
            xp >= 600 -> 26
            xp >= 550 -> 25
            xp >= 500 -> 24
            xp >= 450 -> 23
            xp >= 400 -> 22
            xp >= 350 -> 21
            xp >= 300 -> 20
            xp >= 280 -> 19
            xp >= 260 -> 18
            xp >= 240 -> 17
            xp >= 220 -> 16
            xp >= 200 -> 15
            xp >= 180 -> 14
            xp >= 160 -> 13
            xp >= 140 -> 12
            xp >= 120 -> 11
            xp >= 100 -> 10
            xp >= 90 -> 9
            xp >= 80 -> 8
            xp >= 70 -> 7
            xp >= 60 -> 6
            xp >= 50 -> 5
            xp >= 40 -> 4
            xp >= 30 -> 3
            xp >= 20 -> 2
            xp >= 10 -> 1
            else -> 1
        }
    }

    val homeStatus: StateFlow<HomeStatus> = combine(levelFlow, xpFlow) { level, xp ->
        val honor = LevelHonorHelper.getHonorInfo(level)
        val xpThresholds = getLevelThreshold(level)
        val progress = xpThresholds?.let { thresholds ->
            val range = thresholds.second - thresholds.first
            if (range <= 0) 1f else ((xp - thresholds.first) / range.toFloat()).coerceIn(0f, 1f)
        } ?: 1f
        HomeStatus(
            currentLevel = level,
            totalXp = xp,
            honorTitle = honor.title,
            honorNickname = honor.nickname,
            progressToNextLevel = progress,
            xpForCurrentLevel = xpThresholds?.first ?: 0,
            xpForNextLevel = xpThresholds?.second ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeStatus()
    )

    private fun getLevelThreshold(level: Int): Pair<Int, Int>? {
        return when (level) {
            1 -> 0 to 10
            2 -> 10 to 20
            3 -> 20 to 30
            4 -> 30 to 40
            5 -> 40 to 50
            6 -> 50 to 60
            7 -> 60 to 70
            8 -> 70 to 80
            9 -> 80 to 90
            10 -> 90 to 100
            11 -> 100 to 120
            12 -> 120 to 140
            13 -> 140 to 160
            14 -> 160 to 180
            15 -> 180 to 200
            16 -> 200 to 220
            17 -> 220 to 240
            18 -> 240 to 260
            19 -> 260 to 280
            20 -> 280 to 300
            21 -> 300 to 350
            22 -> 350 to 400
            23 -> 400 to 450
            24 -> 450 to 500
            25 -> 500 to 550
            26 -> 550 to 600
            27 -> 600 to 700
            28 -> 700 to 800
            29 -> 800 to 900
            30 -> 900 to 1000
            else -> null
        }
    }
}
