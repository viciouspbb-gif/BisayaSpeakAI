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
        private val IS_PREMIUM_USER = booleanPreferencesKey("is_premium_user")
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
    
    // プレミアムユーザー管理
    val isPremiumUser: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_PREMIUM_USER] ?: false
        }
    
    suspend fun setPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM_USER] = isPremium
        }
    }
    
    // 同期用のメソッド（通知システム用）
    suspend fun isPremiumUser(): Boolean {
        var result = false
        context.dataStore.data.map { preferences ->
            preferences[IS_PREMIUM_USER] ?: false
        }.collect { result = it }
        return result
    }
}
