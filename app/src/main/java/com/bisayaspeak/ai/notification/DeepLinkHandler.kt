package com.bisayaspeak.ai.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavController
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deep Linkハンドラー
 */
@Singleton
class DeepLinkHandler @Inject constructor() {
    
    /**
     * Deep Linkを処理
     */
    fun handleDeepLink(deepLink: String, navController: NavController): Boolean {
        return when {
            deepLink.startsWith("app://study/main_lesson") -> {
                navigateToMainLesson(navController)
                true
            }
            deepLink.startsWith("app://tari_walk/main") -> {
                navigateToTariWalk(navController)
                true
            }
            else -> {
                false
            }
        }
    }
    
    /**
     * 学習メイン画面へ遷移
     */
    private fun navigateToMainLesson(navController: NavController) {
        try {
            navController.navigate("main_lesson") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            // ナビゲーションエラーの場合はホームへ
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    
    /**
     * タリの散歩道へ遷移
     */
    private fun navigateToTariWalk(navController: NavController) {
        try {
            navController.navigate("tari_walk") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            // ナビゲーションエラーの場合はホームへ
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    
    /**
     * IntentからDeep Linkを抽出
     */
    fun extractDeepLink(intent: Intent): String? {
        return intent.data?.toString()
    }
}

/**
 * WorkManager用の通知ワーカー
 */
class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // 通知を表示
            val isPaidUser = inputData.getBoolean(KEY_IS_PAID_USER, false)
            
            // 実際の実装ではDIコンテナからインスタンスを取得
            // val notificationManager = // DIから取得
            // notificationManager.showNotification(isPaidUser)
            
            // 次の日の通知を再スケジュール
            scheduleNextNotification(isPaidUser)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun scheduleNextNotification(isPaidUser: Boolean) {
        val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInitialDelay(24, TimeUnit.HOURS)
            .setInputData(
                workDataOf(KEY_IS_PAID_USER to isPaidUser)
            )
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_notification",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    companion object {
        const val KEY_IS_PAID_USER = "is_paid_user"
        
        fun scheduleNotification(context: Context, isPaidUser: Boolean) {
            val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
                .setInitialDelay(calculateDelayUntil18(), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(KEY_IS_PAID_USER to isPaidUser)
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "daily_notification",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        private fun calculateDelayUntil18(): Long {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 18)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            val targetTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            
            return if (targetTime > currentTime) {
                targetTime - currentTime
            } else {
                // 今日の18:00が過ぎている場合は明日の18:00まで
                (targetTime + 24 * 60 * 60 * 1000) - currentTime
            }
        }
        
        fun cancelNotification(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("daily_notification")
        }
    }
}

/**
 * 通知初期化マネージャー
 */
@Singleton
class NotificationInitializer @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val notificationManager: LocalNotificationManager,
    private val preferencesManager: com.bisayaspeak.ai.data.preferences.PreferencesManager
) {
    
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * アプリ起動時に通知を初期化
     */
    fun initializeNotifications() {
        scope.launch {
            // 通知権限チェック
            if (notificationManager.hasNotificationPermission()) {
                notificationScheduler.scheduleDailyNotification()
            }
        }
    }
    
    /**
     * ユーザー状態が変更された時に通知を再スケジュール
     */
    fun onUserStatusChanged() {
        scope.launch {
            // 既存の通知をキャンセルして再スケジュール
            notificationScheduler.cancelAll()
            notificationScheduler.scheduleDailyNotification()
        }
    }
}
