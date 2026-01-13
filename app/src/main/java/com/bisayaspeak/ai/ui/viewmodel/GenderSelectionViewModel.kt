package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

class GenderSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application.applicationContext)

    fun saveUserGender(gender: UserGender) {
        viewModelScope.launch {
            repository.saveUserGender(gender)
        }
    }
}
