package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisayaspeak.ai.data.UserGender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val USER_GENDER_KEY = stringPreferencesKey("user_gender")
    }

    val userGender: Flow<UserGender> = context.userPreferencesDataStore.data.map { preferences ->
        val stored = preferences[USER_GENDER_KEY].orEmpty()
        runCatching { UserGender.valueOf(stored) }.getOrElse { UserGender.SECRET }
    }

    suspend fun saveUserGender(gender: UserGender) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[USER_GENDER_KEY] = gender.name
        }
    }
}
