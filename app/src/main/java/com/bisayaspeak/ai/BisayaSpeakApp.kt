package com.bisayaspeak.ai

import android.app.Application
import android.util.Log
import com.bisayaspeak.ai.data.local.AppDatabase
import com.bisayaspeak.ai.data.local.DatabaseInitializer
import com.bisayaspeak.ai.data.repository.DbSeedStateRepository
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import com.bisayaspeak.ai.feature.ProFeatureGate
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BisayaSpeakApp : Application() {

    companion object {
        lateinit var instance: BisayaSpeakApp
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var questionRepository: QuestionRepository
        private set

    lateinit var userProgressRepository: UserProgressRepository
        private set

    lateinit var dbSeedStateRepository: DbSeedStateRepository
        private set

    private val _proVersionState = MutableStateFlow(false)
    val proVersionState: StateFlow<Boolean> = _proVersionState.asStateFlow()

    private val _startupState = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val startupState: StateFlow<AppStartupState> = _startupState.asStateFlow()

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
        scheduleInitialization()
    }

    suspend fun awaitInitialization() {
        val current = _startupState.value
        if (current is AppStartupState.Ready) return
        if (current is AppStartupState.Failed) throw current.throwable
        when (val result = startupState.filter { it !is AppStartupState.Loading }.first()) {
            AppStartupState.Ready -> return
            is AppStartupState.Failed -> throw result.throwable
            else -> return
        }
    }

    private suspend fun initializeCoreDependencies() {
        database = withContext(Dispatchers.IO) { AppDatabase.getInstance(this@BisayaSpeakApp) }
        questionRepository = QuestionRepository(database.questionDao())
        userProgressRepository = UserProgressRepository(database.userProgressDao())
        dbSeedStateRepository = DbSeedStateRepository(this)
        triggerDatabaseSeed()
    }

    private fun scheduleInitialization() {
        applicationScope.launch {
            try {
                initializeCoreDependencies()
                _startupState.value = AppStartupState.Ready
            } catch (t: Throwable) {
                Log.e("BisayaSpeakApp", "Failed to initialize core dependencies", t)
                _startupState.value = AppStartupState.Failed(t)
            }
        }
    }

    fun retryInitialization() {
        if (_startupState.value is AppStartupState.Loading) return
        _startupState.value = AppStartupState.Loading
        scheduleInitialization()
    }

    fun triggerDatabaseSeed() {
        applicationScope.launch {
            DatabaseInitializer.initialize(
                context = applicationContext,
                database = database,
                seedStateRepository = dbSeedStateRepository
            )
        }
    }
}

sealed class AppStartupState {
    data object Loading : AppStartupState()
    data object Ready : AppStartupState()
    data class Failed(val throwable: Throwable) : AppStartupState()
}
