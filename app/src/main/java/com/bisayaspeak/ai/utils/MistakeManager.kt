package com.bisayaspeak.ai.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MistakeManager {
    private val _mistakeIds = MutableStateFlow<Set<String>>(emptySet())
    val mistakeIds: StateFlow<Set<String>> = _mistakeIds.asStateFlow()

    fun addMistake(questionId: String) {
        _mistakeIds.value = _mistakeIds.value + questionId
    }

    fun removeMistake(questionId: String) {
        _mistakeIds.value = _mistakeIds.value - questionId
    }

    fun clearAll() {
        _mistakeIds.value = emptySet()
    }
}
