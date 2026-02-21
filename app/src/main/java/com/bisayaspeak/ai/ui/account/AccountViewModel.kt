package com.bisayaspeak.ai.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountProfileUiState(
    val nickname: String = "",
    val gender: UserGender = UserGender.OTHER,
    val savedNickname: String = "",
    val savedGender: UserGender = UserGender.OTHER,
    val isSaving: Boolean = false,
    val lastSavedAt: Long? = null,
    val errorMessage: String? = null
) {
    val isDirty: Boolean get() = nickname != savedNickname || gender != savedGender
    val isValid: Boolean get() = nickname.isNotBlank()
}

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application.applicationContext)

    private val _profileState = MutableStateFlow(AccountProfileUiState())
    val profileState: StateFlow<AccountProfileUiState> = _profileState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                _profileState.update { current ->
                    val base = current.copy(
                        savedNickname = profile.nickname,
                        savedGender = profile.gender,
                        errorMessage = null
                    )
                    if (!current.isDirty || current.isSaving) {
                        base.copy(
                            nickname = profile.nickname,
                            gender = profile.gender,
                            isSaving = false
                        )
                    } else {
                        base.copy(isSaving = false)
                    }
                }
            }
        }
    }

    fun onNicknameChange(value: String) {
        _profileState.update { it.copy(nickname = value.take(24)) }
    }

    fun onGenderChange(gender: UserGender) {
        _profileState.update { it.copy(gender = gender) }
    }

    fun saveProfile() {
        val current = _profileState.value
        if (!current.isValid || !current.isDirty || current.isSaving) return
        viewModelScope.launch {
            _profileState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.saveUserProfile(current.nickname.trim(), current.gender)
            }.onSuccess {
                _profileState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedAt = System.currentTimeMillis(),
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _profileState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "保存に失敗しました")
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AccountViewModel(application)
            }
        }
    }
}
