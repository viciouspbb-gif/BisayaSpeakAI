package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisayaspeak.ai.domain.honor.HonorLevelManager
import com.bisayaspeak.ai.domain.xp.XpProgressManager
import com.bisayaspeak.ai.domain.xp.XpUpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_prefs")

class UsageRepository(private val context: Context) {
    
    companion object {
        private val DIAGNOSIS_COUNT_KEY = intPreferencesKey("diagnosis_count")
        private val AD_WATCH_COUNT_KEY = intPreferencesKey("ad_watch_count")
        private val CURRENT_LEVEL_KEY = intPreferencesKey("current_level")
        private val CURRENT_XP_KEY = intPreferencesKey("current_xp")
        private val TEACHER_LEVEL_KEY = intPreferencesKey("teacher_level")
        private val DAILY_MISSION_PROGRESS_KEY = stringPreferencesKey("daily_mission_progress")
        private val TOTAL_LESSONS_COMPLETED_KEY = intPreferencesKey("total_lessons_completed")
        private const val MAX_FREE_DIAGNOSIS = 10
        // リワード視聴ごとに +2 回の診断権を付与
        private const val BONUS_PER_AD = 2
        private const val MAX_AD_WATCH = 3
        private const val LISTENING_TARGET = 2
        private const val LISTENING_FALLBACK_THRESHOLD = 3
        private const val LISTENING_MAX_TRACKED = LISTENING_FALLBACK_THRESHOLD
        private const val TRANSLATOR_TARGET = 1

        private fun currentDayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()
    }
    
    // デバッグビルドかどうか（あなたのデバイスのみ無制限）
    private val isDebugBuild: Boolean
        get() = com.bisayaspeak.ai.BuildConfig.DEBUG
    
    /**
     * 残り診断回数を取得
     */
    fun getRemainingCount(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            if (isDebugBuild) {
                return@map 9999 // デバッグビルド（開発者のみ）では無制限
            }
            val diagnosisCount = preferences[DIAGNOSIS_COUNT_KEY] ?: 0
            val adWatchCount = preferences[AD_WATCH_COUNT_KEY] ?: 0
            val totalAvailable = MAX_FREE_DIAGNOSIS + (adWatchCount * BONUS_PER_AD)
            maxOf(0, totalAvailable - diagnosisCount)
        }
    }
    
    /**
     * 現在のレベルを取得（初期値: 1）
     */
    fun getCurrentLevel(): Flow<Int> {
        return getTotalLessonsCompleted().map { lessons ->
            HonorLevelManager.levelForLessons(lessons)
        }
    }
    
    fun getXpState(): Flow<XpState> {
        return context.dataStore.data.map { preferences ->
            val level = preferences[TEACHER_LEVEL_KEY] ?: 1
            val xp = preferences[CURRENT_XP_KEY] ?: 0
            XpState(level, xp)
        }
    }

    fun getDailyMissionProgress(): Flow<DailyMissionProgress> {
        return context.dataStore.data.map { preferences ->
            resolveDailyMissionProgress(preferences)
        }
    }

    /**
     * 累計レッスン完了数を取得（初期値: 0）
     */
    fun getTotalLessonsCompleted(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[TOTAL_LESSONS_COMPLETED_KEY] ?: 0
        }
    }
    
    /**
     * 診断可能かどうか
     */
    fun canDiagnose(): Flow<Boolean> {
        if (isDebugBuild) {
            return kotlinx.coroutines.flow.flowOf(true) // デバッグビルド（開発者のみ）では常にtrue
        }
        return getRemainingCount().map { it > 0 }
    }
    
    /**
     * 広告視聴可能かどうか
     */
    fun canWatchAd(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            val adWatchCount = preferences[AD_WATCH_COUNT_KEY] ?: 0
            adWatchCount < MAX_AD_WATCH
        }
    }
    
    /**
     * 広告視聴回数を取得
     */
    fun getAdWatchCount(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[AD_WATCH_COUNT_KEY] ?: 0
        }
    }
    
    /**
     * 診断回数を増やす
     */
    suspend fun incrementDiagnosisCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[DIAGNOSIS_COUNT_KEY] ?: 0
            preferences[DIAGNOSIS_COUNT_KEY] = currentCount + 1
        }
    }
    
    /**
     * 広告視聴回数を増やす
     */
    suspend fun incrementAdWatchCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[AD_WATCH_COUNT_KEY] ?: 0
            if (currentCount < MAX_AD_WATCH) {
                preferences[AD_WATCH_COUNT_KEY] = currentCount + 1
            }
        }
    }
    
    suspend fun addXp(amount: Int): XpUpdateResult {
        if (amount <= 0) {
            val snapshot = getCurrentXpSnapshot()
            return XpUpdateResult(
                newLevel = snapshot.teacherLevel,
                xpIntoLevel = snapshot.xpIntoLevel,
                leveledUp = false,
                levelsGained = 0,
                requiredXpForLevel = XpProgressManager.requiredXp(snapshot.teacherLevel)
            )
        }

        var updateResult: XpUpdateResult? = null
        context.dataStore.edit { preferences ->
            val currentLevel = preferences[TEACHER_LEVEL_KEY] ?: 1
            val currentXp = preferences[CURRENT_XP_KEY] ?: 0
            val result = XpProgressManager.applyXpGain(currentXp, currentLevel, amount)
            preferences[TEACHER_LEVEL_KEY] = result.newLevel
            preferences[CURRENT_XP_KEY] = result.xpIntoLevel
            updateResult = result
        }
        return updateResult!!
    }

    suspend fun incrementDailyMission(type: DailyMissionType, increment: Int = 1): DailyMissionUpdate {
        var update: DailyMissionProgress? = null
        var justCompleted = false
        var fallbackCompleted = false
        context.dataStore.edit { preferences ->
            val baseline = resolveDailyMissionProgress(preferences)
            var adjusted = when (type) {
                DailyMissionType.LISTENING -> {
                    val newCount = (baseline.listeningCount + increment).coerceAtMost(LISTENING_MAX_TRACKED)
                    justCompleted = baseline.listeningCount < LISTENING_TARGET && newCount >= LISTENING_TARGET
                    baseline.copy(listeningCount = newCount)
                }

                DailyMissionType.TRANSLATOR -> {
                    val newCount = (baseline.translatorCount + increment).coerceAtMost(TRANSLATOR_TARGET)
                    justCompleted = baseline.translatorCount < TRANSLATOR_TARGET && newCount >= TRANSLATOR_TARGET
                    val resetFallback = baseline.translatorFallbackGranted && newCount >= TRANSLATOR_TARGET
                    baseline.copy(
                        translatorCount = newCount,
                        translatorFallbackGranted = if (resetFallback) false else baseline.translatorFallbackGranted
                    )
                }

                DailyMissionType.SANPO -> {
                    justCompleted = !baseline.sanpoCompleted
                    baseline.copy(sanpoCompleted = true)
                }
            }

            if (type == DailyMissionType.LISTENING) {
                val fallbackEligible = !baseline.translatorFallbackGranted &&
                    baseline.translatorCount < TRANSLATOR_TARGET &&
                    adjusted.listeningCount >= LISTENING_FALLBACK_THRESHOLD
                if (fallbackEligible) {
                    adjusted = adjusted.copy(
                        translatorCount = TRANSLATOR_TARGET,
                        translatorFallbackGranted = true
                    )
                    fallbackCompleted = true
                }
            }

            preferences[DAILY_MISSION_PROGRESS_KEY] = adjusted.serialize()
            update = adjusted
        }
        return DailyMissionUpdate(update!!, justCompleted, fallbackCompleted)
    }

    // /**
    //  * レベルを1つ上げる
    //  */
    // suspend fun incrementLevel() {
    //     context.dataStore.edit { preferences ->
    //         val currentLevel = preferences[CURRENT_LEVEL_KEY] ?: 1
    //         preferences[CURRENT_LEVEL_KEY] = currentLevel + 1
    //     }
    // }

    /**
     * 累計レッスン完了数を1つ増やす
     */
    suspend fun incrementTotalLessonsCompleted() {
        context.dataStore.edit { preferences ->
            val totalCompleted = preferences[TOTAL_LESSONS_COMPLETED_KEY] ?: 0
            preferences[TOTAL_LESSONS_COMPLETED_KEY] = totalCompleted + 1
        }
    }
    
    /**
     * カウントをリセット
     */
    suspend fun resetCounts() {
        context.dataStore.edit { preferences ->
            preferences[DIAGNOSIS_COUNT_KEY] = 0
            preferences[AD_WATCH_COUNT_KEY] = 0
            preferences[CURRENT_LEVEL_KEY] = 1
            preferences[CURRENT_XP_KEY] = 0
            preferences[TEACHER_LEVEL_KEY] = 1
            preferences[TOTAL_LESSONS_COMPLETED_KEY] = 0
            preferences.remove(DAILY_MISSION_PROGRESS_KEY)
        }
    }

    private fun resolveDailyMissionProgress(preferences: Preferences): DailyMissionProgress {
        val raw = preferences[DAILY_MISSION_PROGRESS_KEY]
        val today = currentDayKey()
        val parsed = parseDailyMissionProgress(raw)
        return if (parsed.dayKey == today) parsed else DailyMissionProgress(dayKey = today)
    }

    private fun parseDailyMissionProgress(raw: String?): DailyMissionProgress {
        if (raw.isNullOrBlank()) {
            return DailyMissionProgress(dayKey = currentDayKey())
        }
        val parts = raw.split("|")
        if (parts.size < 4) return DailyMissionProgress(dayKey = currentDayKey())
        return DailyMissionProgress(
            dayKey = parts[0],
            listeningCount = parts[1].toIntOrNull() ?: 0,
            translatorCount = parts[2].toIntOrNull() ?: 0,
            sanpoCompleted = parts[3] == "1",
            translatorFallbackGranted = parts.getOrNull(4)?.toIntOrNull() == 1
        )
    }

    private fun DailyMissionProgress.serialize(): String {
        val sanpoBit = if (sanpoCompleted) "1" else "0"
        val fallbackBit = if (translatorFallbackGranted) "1" else "0"
        return listOf(dayKey, listeningCount.toString(), translatorCount.toString(), sanpoBit, fallbackBit).joinToString(separator = "|")
    }

    private suspend fun getCurrentXpSnapshot(): XpState {
        val preferences = context.dataStore.data.first()
        val level = preferences[TEACHER_LEVEL_KEY] ?: 1
        val xp = preferences[CURRENT_XP_KEY] ?: 0
        return XpState(level, xp)
    }

}

data class XpState(
    val teacherLevel: Int = 1,
    val xpIntoLevel: Int = 0
)

data class DailyMissionProgress(
    val dayKey: String,
    val listeningCount: Int = 0,
    val translatorCount: Int = 0,
    val sanpoCompleted: Boolean = false,
    val translatorFallbackGranted: Boolean = false
)

data class DailyMissionUpdate(
    val progress: DailyMissionProgress,
    val justCompleted: Boolean,
    val fallbackCompleted: Boolean = false
)

enum class DailyMissionType {
    LISTENING,
    TRANSLATOR,
    SANPO
}
