package com.bisayaspeak.ai

import android.app.Application
import android.util.Log
import com.bisayaspeak.ai.data.local.AppDatabase
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import com.bisayaspeak.ai.feature.ProFeatureGate
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BisayaSpeakApp : Application() {

    companion object {
        lateinit var instance: BisayaSpeakApp
            private set
    }

    lateinit var questionRepository: QuestionRepository
        private set

    lateinit var userProgressRepository: UserProgressRepository
        private set

    private val _proVersionState = MutableStateFlow(false)
    val proVersionState: StateFlow<Boolean> = _proVersionState.asStateFlow()

    var isProVersion: Boolean
        get() = ProFeatureGate.isProFeatureEnabled(_proVersionState.value)
        set(value) {
            if (_proVersionState.value != value) {
                _proVersionState.value = value
            }
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _proVersionState.value = false

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        Log.d("BisayaSpeakApp", "Application started")

        val database = AppDatabase.getInstance(this)
        questionRepository = QuestionRepository(database.questionDao())
        userProgressRepository = UserProgressRepository(database.userProgressDao())
    }
}
