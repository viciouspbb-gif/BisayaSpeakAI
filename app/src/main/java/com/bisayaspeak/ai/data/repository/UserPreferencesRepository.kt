package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisayaspeak.ai.data.UserGender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

data class UserProfilePreferences(
    val nickname: String = "",
    val gender: UserGender = UserGender.OTHER
)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val USER_GENDER_KEY = stringPreferencesKey("user_gender")
        private val USER_NICKNAME_KEY = stringPreferencesKey("user_nickname")
        private val ROLEPLAY_TUTORIAL_SEEN_KEY = booleanPreferencesKey("roleplay_tutorial_seen")
        private val ROLEPLAY_TUTORIAL_VERSION_KEY = intPreferencesKey("roleplay_tutorial_version")
    }

    val userProfile: Flow<UserProfilePreferences> = context.userPreferencesDataStore.data.map { preferences ->
        val gender = runCatching {
            val stored = preferences[USER_GENDER_KEY].orEmpty()
            UserGender.valueOf(stored)
        }.getOrElse { UserGender.OTHER }
        val nickname = preferences[USER_NICKNAME_KEY]
            ?.takeIf { it.isNotBlank() }
            ?: "ゲストユーザー"
        UserProfilePreferences(nickname = nickname, gender = gender)
    }

    val userGender: Flow<UserGender> = userProfile.map { it.gender }

    val roleplayTutorialSeen: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[ROLEPLAY_TUTORIAL_SEEN_KEY] ?: false
    }

    val roleplayTutorialVersion: Flow<Int> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[ROLEPLAY_TUTORIAL_VERSION_KEY] ?: 0
    }

    suspend fun saveUserProfile(nickname: String, gender: UserGender) {
        val sanitized = nickname.trim().ifBlank { "ゲストユーザー" }
        context.userPreferencesDataStore.edit { prefs ->
            prefs[USER_NICKNAME_KEY] = sanitized
            prefs[USER_GENDER_KEY] = gender.name
        }
    }

    suspend fun saveUserGender(gender: UserGender) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[USER_GENDER_KEY] = gender.name
        }
    }

    suspend fun setRoleplayTutorialSeen(seen: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[ROLEPLAY_TUTORIAL_SEEN_KEY] = seen
        }
    }

    suspend fun setRoleplayTutorialVersion(version: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[ROLEPLAY_TUTORIAL_VERSION_KEY] = version
        }
    }
}
