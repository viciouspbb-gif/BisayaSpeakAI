package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.levelConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "level_config_prefs"
)

class LevelConfigRepository(private val context: Context) {

    companion object {
        private val LATEST_MAX_LEVEL_KEY = intPreferencesKey("latest_max_level")
        private val INITIALIZED_KEY = booleanPreferencesKey("level_release_initialized")
        const val DEFAULT_MAX_LEVEL = 32
    }

    val latestMaxLevelFlow: Flow<Int> = context.levelConfigDataStore.data.map { prefs ->
        prefs[LATEST_MAX_LEVEL_KEY] ?: DEFAULT_MAX_LEVEL
    }

    suspend fun getLatestMaxLevel(): Int = latestMaxLevelFlow.first()

    suspend fun setLatestMaxLevel(value: Int) {
        context.levelConfigDataStore.edit { prefs ->
            prefs[LATEST_MAX_LEVEL_KEY] = value
        }
    }

    suspend fun ensureInitialized() {
        context.levelConfigDataStore.edit { prefs ->
            if (prefs[INITIALIZED_KEY] != true) {
                prefs[LATEST_MAX_LEVEL_KEY] = DEFAULT_MAX_LEVEL
                prefs[INITIALIZED_KEY] = true
            }
        }
    }

    suspend fun updateMaxLevelIfHigher(value: Int): Boolean {
        val current = getLatestMaxLevel()
        if (value > current) {
            setLatestMaxLevel(value)
            return true
        }
        return false
    }
}
