package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenderSelectionUiState(
    val selectedGender: UserGender = UserGender.SECRET,
    val isSaving: Boolean = false
)

class GenderSelectionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(GenderSelectionUiState())
    val uiState: StateFlow<GenderSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userGender.collect { stored ->
                _uiState.update { it.copy(selectedGender = stored) }
            }
        }
    }

    fun selectGender(gender: UserGender) {
        _uiState.update { it.copy(selectedGender = gender) }
    }

    fun saveSelectedGender(onSaved: (() -> Unit)? = null) {
        if (_uiState.value.isSaving) return
        val targetGender = _uiState.value.selectedGender
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.saveUserGender(targetGender)
            _uiState.update { it.copy(isSaving = false) }
            onSaved?.invoke()
        }
    }
}
