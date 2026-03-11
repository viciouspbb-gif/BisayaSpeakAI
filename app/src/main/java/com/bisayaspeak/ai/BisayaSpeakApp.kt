package com.bisayaspeak.ai

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.bisayaspeak.ai.data.local.AppDatabase
import com.bisayaspeak.ai.data.local.DatabaseInitializer
import com.bisayaspeak.ai.data.repository.DbSeedStateRepository
import com.bisayaspeak.ai.data.repository.LevelConfigRepository
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import com.bisayaspeak.ai.di.getAppEntryPoint
import com.bisayaspeak.ai.feature.ProFeatureGate
import com.bisayaspeak.ai.remote.RemoteLevelConfigManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
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
import javax.inject.Inject

@HiltAndroidApp
class BisayaSpeakApp : Application(), Configuration.Provider {

    companion object {
        lateinit var instance: BisayaSpeakApp
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    lateinit var database: AppDatabase
        private set

    lateinit var questionRepository: QuestionRepository
        private set

    lateinit var userProgressRepository: UserProgressRepository
        private set

    lateinit var dbSeedStateRepository: DbSeedStateRepository
        private set

    private lateinit var levelConfigRepository: LevelConfigRepository
    private lateinit var remoteLevelConfigManager: RemoteLevelConfigManager

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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.e("APP_CLASS", "appClass=" + this::class.java.name)
        instance = this
        _proVersionState.value = false
        levelConfigRepository = LevelConfigRepository(this)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        Log.d("BisayaSpeakApp", "Application started")
        
        // 通知チャンネルを事前に作成（存在保証）
        createNotificationChannel()
        
        // EntryPointでNotificationSchedulerを取得してscheduleする
        applicationScope.launch(Dispatchers.IO) {
            try {
                val entryPoint = getAppEntryPoint(this@BisayaSpeakApp)
                val scheduler = entryPoint.getNotificationScheduler()
                scheduler.scheduleDailyNotification()
                Log.d("FATAL_CHECK", "通知予約成功")
            } catch (e: Exception) {
                Log.e("FATAL_CHECK", "予約失敗: ${e.message}")
            }
        }
        
        // 全ての重い処理をIOスレッドに放り込む
        applicationScope.launch(Dispatchers.IO) {
            try {
                // 3秒待機してから通知予約（ユーザー体験優先）
                kotlinx.coroutines.delay(3000)
                val entryPoint = getAppEntryPoint(this@BisayaSpeakApp)
                val scheduler = entryPoint.getNotificationScheduler()
                scheduler.scheduleDailyNotification()
                Log.d("BisayaSpeakApp", "18:00頃通知スケジュールを正常に初期化しました")
                _startupState.value = AppStartupState.Ready
            } catch (e: Exception) {
                Log.e("BisayaSpeakApp", "18:00頃通知スケジュールの初期化に失敗しました", e)
                _startupState.value = AppStartupState.Failed(e)
            }
        }
        
        // DB初期化もIOスレッドで実行
        applicationScope.launch(Dispatchers.IO) {
            try {
                initializeRemoteConfig()
        initializeDatabase()
                Log.d("BisayaSpeakApp", "データベース初期化完了")
            } catch (e: Exception) {
                Log.e("BisayaSpeakApp", "データベース初期化失敗", e)
            }
        }
        
        // 広告初期化もIOスレッドで実行
        applicationScope.launch(Dispatchers.IO) {
            try {
                initializeAds()
                Log.d("BisayaSpeakApp", "広告初期化完了")
            } catch (e: Exception) {
                Log.e("BisayaSpeakApp", "広告初期化失敗", e)
            }
        }
        
        scheduleInitialization()
    }
    
    /**
     * 通知チャンネルを事前に作成（存在保証）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "bisaya_notification_v2", // ③ チャンネルIDを更新して強制リセット
                "ビサヤ語学習通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "毎日のビサヤ語学習をサポートする通知"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            
            Log.d("BisayaSpeakApp", "通知チャンネルを作成: bisaya_notification_v2")
        }
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
        database = AppDatabase.getInstance(this@BisayaSpeakApp)
        questionRepository = QuestionRepository(database.questionDao())
        userProgressRepository = UserProgressRepository(database.userProgressDao())
        dbSeedStateRepository = DbSeedStateRepository(this)
        triggerDatabaseSeed()
    }
    
    private fun initializeDatabase() {
        // DatabaseInitializerをIOスレッドで実行
        applicationScope.launch(Dispatchers.IO) {
            initializeCoreDependencies()
        }
    }

    private fun initializeRemoteConfig() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                levelConfigRepository.ensureInitialized()
                remoteLevelConfigManager = RemoteLevelConfigManager(levelConfigRepository)
                remoteLevelConfigManager.initialize()
                remoteLevelConfigManager.fetchAndActivate()
                Log.d("BisayaSpeakApp", "Remote Config fetched successfully")
            } catch (e: Exception) {
                Log.e("BisayaSpeakApp", "Remote Config fetch failed", e)
            }
        }
    }
    
    private fun initializeAds() {
        // MobileAds.initializeをIOスレッドで実行
        try {
            com.google.android.gms.ads.MobileAds.initialize(this@BisayaSpeakApp) {
                Log.d("BisayaSpeakApp", "MobileAds初期化完了")
            }
        } catch (e: Exception) {
            Log.e("BisayaSpeakApp", "MobileAds初期化失敗", e)
        }
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
                seedStateRepository = dbSeedStateRepository,
                levelConfigRepository = levelConfigRepository
            )
        }
    }
}

sealed class AppStartupState {
    data object Loading : AppStartupState()
    data object Ready : AppStartupState()
    data class Failed(val throwable: Throwable) : AppStartupState()
}
