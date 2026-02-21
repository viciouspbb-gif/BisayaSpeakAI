package com.bisayaspeak.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.UserGender
import java.util.Locale
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
        private val ROLEPLAY_TUTORIAL_LOCALE_KEY = stringPreferencesKey("roleplay_tutorial_locale")
        private val SANPO_CYCLE_STATE_KEY = stringPreferencesKey("sanpo_cycle_state")
        private val SANPO_TURN_COUNT_KEY = intPreferencesKey("sanpo_turn_count")
        private val SANPO_MONET_COUNT_KEY = intPreferencesKey("sanpo_monet_count")
        private const val LEGACY_JA_GUEST = "ゲストユーザー"
    }

    val userProfile: Flow<UserProfilePreferences> = context.userPreferencesDataStore.data.map { preferences ->
        val gender = runCatching {
            val stored = preferences[USER_GENDER_KEY].orEmpty()
            UserGender.valueOf(stored)
        }.getOrElse { UserGender.OTHER }
        val fallback = guestNicknameForLocale()
        val storedNickname = preferences[USER_NICKNAME_KEY]?.takeIf { it.isNotBlank() }
        val nickname = when {
            storedNickname == null -> fallback
            storedNickname == LEGACY_JA_GUEST && !isJapaneseLocale() -> fallback
            else -> storedNickname
        }
        UserProfilePreferences(nickname = nickname, gender = gender)
    }

    val userGender: Flow<UserGender> = userProfile.map { it.gender }

    val roleplayTutorialSeen: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[ROLEPLAY_TUTORIAL_SEEN_KEY] ?: false
    }

    val roleplayTutorialVersion: Flow<Int> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[ROLEPLAY_TUTORIAL_VERSION_KEY] ?: 0
    }

    val roleplayTutorialLocale: Flow<String?> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[ROLEPLAY_TUTORIAL_LOCALE_KEY]
    }

    val sanpoCycleState: Flow<String> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[SANPO_CYCLE_STATE_KEY] ?: "NEW"
    }

    val sanpoTurnCount: Flow<Int> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[SANPO_TURN_COUNT_KEY] ?: 0
    }

    val sanpoMonetCount: Flow<Int> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[SANPO_MONET_COUNT_KEY] ?: 0
    }

    suspend fun saveUserProfile(nickname: String, gender: UserGender) {
        val fallback = guestNicknameForLocale()
        val sanitized = nickname.trim().ifBlank { fallback }
        val normalized = if (sanitized == LEGACY_JA_GUEST && !isJapaneseLocale()) {
            fallback
        } else {
            sanitized
        }
        context.userPreferencesDataStore.edit { prefs ->
            prefs[USER_NICKNAME_KEY] = normalized
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

    suspend fun setRoleplayTutorialLocale(localeTag: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[ROLEPLAY_TUTORIAL_LOCALE_KEY] = localeTag
        }
    }

    suspend fun setSanpoCycleState(state: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[SANPO_CYCLE_STATE_KEY] = state
        }
    }

    suspend fun setSanpoTurnCount(count: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[SANPO_TURN_COUNT_KEY] = count
        }
    }

    suspend fun setSanpoMonetCount(count: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[SANPO_MONET_COUNT_KEY] = count
        }
    }

    private fun guestNicknameForLocale(): String {
        return context.getString(R.string.account_guest_nickname_default)
    }

    private fun isJapaneseLocale(): Boolean = Locale.getDefault().language == "ja"
}
