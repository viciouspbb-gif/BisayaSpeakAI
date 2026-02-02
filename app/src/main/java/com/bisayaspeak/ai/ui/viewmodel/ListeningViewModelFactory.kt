package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository

class ListeningViewModelFactory(
    private val application: Application,
    private val repository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListeningViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListeningViewModel(application, repository, userProgressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
