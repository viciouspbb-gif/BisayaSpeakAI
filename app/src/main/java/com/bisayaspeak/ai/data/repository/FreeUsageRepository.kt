package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.freeUsageDataStore: DataStore<Preferences> by preferencesDataStore(name = "free_usage_prefs")

class FreeUsageRepository(private val context: Context) {

    companion object {
        const val MAX_TRANSLATE_PER_DAY = 4
        const val MAX_TALK_TURNS_PER_DAY = 3
        const val MAX_SANPO_PER_DAY = 1

        private const val PERMANENT_DAY_KEY = "PERMANENT_QUOTA"

        private val DAY_KEY = stringPreferencesKey("day_key")
        private val TRANSLATE_COUNT_KEY = intPreferencesKey("translate_count")
        private val TALK_TURN_COUNT_KEY = intPreferencesKey("talk_turn_count")
        private val SANPO_COUNT_KEY = intPreferencesKey("sanpo_count")
        private val INSTALL_ID_KEY = stringPreferencesKey("install_id")
    }

    private data class UsageState(
        val dayKey: String?,
        val translateCount: Int,
        val talkTurnCount: Int,
        val sanpoCount: Int,
        val installId: String
    )

    suspend fun resetIfNewDay(@Suppress("UNUSED_PARAMETER") nowDayKey: String) {
        context.freeUsageDataStore.edit { prefs ->
            ensureInstallId(prefs)
            if (prefs[DAY_KEY] != PERMANENT_DAY_KEY) {
                prefs[DAY_KEY] = PERMANENT_DAY_KEY
            }
        }
    }

    suspend fun canUseTranslate(): Boolean {
        val state = readState()
        return state.translateCount < MAX_TRANSLATE_PER_DAY
    }

    suspend fun consumeTranslate() {
        incrementCounter(TRANSLATE_COUNT_KEY)
    }

    suspend fun canUseTalkTurn(): Boolean {
        val state = readState()
        return state.talkTurnCount < MAX_TALK_TURNS_PER_DAY
    }

    suspend fun consumeTalkTurn() {
        incrementCounter(TALK_TURN_COUNT_KEY)
    }

    suspend fun canStartSanpo(): Boolean {
        val state = readState()
        return state.sanpoCount < MAX_SANPO_PER_DAY
    }

    suspend fun consumeSanpoStart() {
        incrementCounter(SANPO_COUNT_KEY)
    }

    suspend fun getInstallId(): String {
        val state = readState()
        return state.installId
    }

    suspend fun getDayKey(): String? = readState().dayKey

    suspend fun getTranslateCount(): Int = readState().translateCount

    suspend fun getTalkTurnCount(): Int = readState().talkTurnCount

    suspend fun getSanpoCount(): Int = readState().sanpoCount

    private suspend fun readState(): UsageState {
        val prefs = context.freeUsageDataStore.data.first()
        val installId = prefs[INSTALL_ID_KEY] ?: generateInstallId(prefs)
        return UsageState(
            dayKey = prefs[DAY_KEY] ?: PERMANENT_DAY_KEY,
            translateCount = prefs[TRANSLATE_COUNT_KEY] ?: 0,
            talkTurnCount = prefs[TALK_TURN_COUNT_KEY] ?: 0,
            sanpoCount = prefs[SANPO_COUNT_KEY] ?: 0,
            installId = installId
        )
    }

    private suspend fun incrementCounter(key: Preferences.Key<Int>) {
        context.freeUsageDataStore.edit { prefs ->
            ensureInstallId(prefs)
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
    }

    private fun ensureInstallId(prefs: MutablePreferences) {
        if (prefs[INSTALL_ID_KEY] == null) {
            prefs[INSTALL_ID_KEY] = UUID.randomUUID().toString()
        }
    }

    private suspend fun generateInstallId(prefs: Preferences): String {
        val newId = UUID.randomUUID().toString()
        context.freeUsageDataStore.edit { editPrefs ->
            if (editPrefs[INSTALL_ID_KEY] == null) {
                editPrefs[INSTALL_ID_KEY] = newId
            }
        }
        return context.freeUsageDataStore.data.first()[INSTALL_ID_KEY] ?: newId
    }
}
