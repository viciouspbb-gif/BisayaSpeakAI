package com.bisayaspeak.ai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bisaya_speak_prefs")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val AI_CONVERSATION_COUNT = intPreferencesKey("ai_conversation_count")
        private val FREE_TRIAL_USED = booleanPreferencesKey("free_trial_used")
    }

    val aiConversationCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AI_CONVERSATION_COUNT] ?: 0
        }

    suspend fun saveAiConversationCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[AI_CONVERSATION_COUNT] = count
        }
    }

    suspend fun resetAiConversationCount() {
        saveAiConversationCount(0)
    }
    
    // 無料体験管理
    val freeTrialUsed: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FREE_TRIAL_USED] ?: false
        }
    
    suspend fun markFreeTrialAsUsed() {
        context.dataStore.edit { preferences ->
            preferences[FREE_TRIAL_USED] = true
        }
    }
    
    suspend fun resetFreeTrial() {
        context.dataStore.edit { preferences ->
            preferences[FREE_TRIAL_USED] = false
        }
    }
}
