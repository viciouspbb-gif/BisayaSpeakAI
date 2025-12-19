package com.bisayaspeak.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 購入状態を保存するDataStore
 */
class PurchaseStore(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "purchase_prefs")
        
        private val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
        private val HAS_PREMIUM_AI = booleanPreferencesKey("has_premium_ai")
    }
    
    /**
     * Pro解放状態を取得
     */
    val isProUnlocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PRO_UNLOCKED] ?: false
    }
    
    /**
     * Premium AI状態を取得
     */
    val hasPremiumAI: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_PREMIUM_AI] ?: false
    }
    
    /**
     * Pro解放状態を保存
     */
    suspend fun setProUnlocked(unlocked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PRO_UNLOCKED] = unlocked
        }
    }
    
    /**
     * Premium AI状態を保存
     */
    suspend fun setPremiumAI(hasPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_PREMIUM_AI] = hasPremium
        }
    }
    
    /**
     * すべての購入状態をクリア（デバッグ用）
     */
    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
