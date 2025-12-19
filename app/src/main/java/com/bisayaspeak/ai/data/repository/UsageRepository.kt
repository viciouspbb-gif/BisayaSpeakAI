package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_prefs")

class UsageRepository(private val context: Context) {
    
    companion object {
        private val DIAGNOSIS_COUNT_KEY = intPreferencesKey("diagnosis_count")
        private val AD_WATCH_COUNT_KEY = intPreferencesKey("ad_watch_count")
        private val CURRENT_LEVEL_KEY = intPreferencesKey("current_level")
        private val TOTAL_XP_KEY = intPreferencesKey("total_xp")
        private const val MAX_FREE_DIAGNOSIS = 10
        // リワード視聴ごとに +2 回の診断権を付与
        private const val BONUS_PER_AD = 2
        private const val MAX_AD_WATCH = 3
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
        return context.dataStore.data.map { preferences ->
            preferences[CURRENT_LEVEL_KEY] ?: 1
        }
    }
    
    /**
     * 累計XPを取得（初期値: 0）
     */
    fun getTotalXP(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[TOTAL_XP_KEY] ?: 0
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
    
    /**
     * XPを加算
     */
    suspend fun addXP(amount: Int) {
        if (amount <= 0) return
        context.dataStore.edit { preferences ->
            val currentXP = preferences[TOTAL_XP_KEY] ?: 0
            preferences[TOTAL_XP_KEY] = currentXP + amount
        }
    }
    
    /**
     * レベルを1つ上げる
     */
    suspend fun incrementLevel() {
        context.dataStore.edit { preferences ->
            val currentLevel = preferences[CURRENT_LEVEL_KEY] ?: 1
            preferences[CURRENT_LEVEL_KEY] = currentLevel + 1
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
            preferences[TOTAL_XP_KEY] = 0
        }
    }
}
