package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dbSeedStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "db_seed_state"
)

class DbSeedStateRepository(private val context: Context) {

    companion object {
        private val DB_SEEDED_KEY = booleanPreferencesKey("db_seeded")
    }

    val seededFlow: Flow<Boolean> = context.dbSeedStateDataStore.data.map { prefs ->
        prefs[DB_SEEDED_KEY] ?: false
    }

    suspend fun setDbSeeded(value: Boolean) {
        context.dbSeedStateDataStore.edit { prefs ->
            prefs[DB_SEEDED_KEY] = value
        }
    }
}
