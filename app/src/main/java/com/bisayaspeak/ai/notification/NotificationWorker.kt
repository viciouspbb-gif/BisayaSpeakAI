package com.bisayaspeak.ai.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.di.getAppEntryPoint
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = getAppEntryPoint(applicationContext)
            val localNotificationManager = entryPoint.getLocalNotificationManager()
            val notificationScheduler = entryPoint.getNotificationScheduler()
            val notificationContentGenerator = entryPoint.getNotificationContentGenerator()

            val isProVersion = BuildConfig.FLAVOR == "pro"

            // スマート通知ロジック：PRO版でも動的フレーズを適用
            val content = if (isProVersion) {
                val generated = notificationContentGenerator.generateNotificationContent(true)
                NotificationContent(
                    title = "Tali (タリ) 🦉",
                    body = generated?.body ?: "今日の散歩、一緒に行かない？",
                    deepLink = "app://tari_walk/main"
                )
            } else {
                notificationContentGenerator.generateNotificationContent(false) ?: NotificationContent(
                    title = "今日のビサヤ語",
                    body = "Kumusta ka - お元気ですか？",
                    deepLink = "app://study/main_lesson"
                )
            }

            localNotificationManager.showNotification(content)
            
            // 次回のスケジュール（後ほどここをさらにスマート化します）
            notificationScheduler.scheduleDailyNotification()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Notification error", e)
            Result.failure()
        }
    }
}