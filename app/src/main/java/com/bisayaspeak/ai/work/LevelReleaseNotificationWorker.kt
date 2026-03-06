package com.bisayaspeak.ai.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bisayaspeak.ai.data.repository.TimeReleaseRepository
import com.bisayaspeak.ai.di.getAppEntryPoint
import com.bisayaspeak.ai.notification.LocalNotificationManager
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * レベル解放通知スケジュールワーカー
 * 指定された時刻にレベル解放通知を送信
 */
class LevelReleaseNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    @Inject
    lateinit var timeReleaseRepository: TimeReleaseRepository
    
    @Inject
    lateinit var localNotificationManager: LocalNotificationManager
    
    override suspend fun doWork(): Result {
        return try {
            val entryPoint = getAppEntryPoint(applicationContext)
            entryPoint.inject(this)
            
            val level = inputData.getInt("level", -1)
            if (level == -1) {
                return Result.failure()
            }
            
            // 通知メッセージを生成
            val notificationContent = createNotificationContent(level)
            
            // 通知を送信
            localNotificationManager.showNotification(notificationContent)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    /**
     * 通知コンテンツを生成
     */
    private fun createNotificationContent(level: Int): com.bisayaspeak.ai.notification.NotificationContent {
        val titleJa = "[新着] リスニングLV${level}が解放されました！"
        val titleEn = "[New] Listening LV${level} is now available!"
        val bodyJa = "新しいフレーズをチェックしよう"
        val bodyEn = "Check out the new phrases"
        
        return com.bisayaspeak.ai.notification.NotificationContent(
            title = "$titleJa / $titleEn",
            body = "$bodyJa / $bodyEn",
            deepLink = "app://study/level_selection"
        )
    }
}
