package com.bisayaspeak.ai.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bisayaspeak.ai.data.preferences.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope

/**
 * 土曜日判定付き通知ワーカー
 */
@HiltWorker
class EnhancedDailyNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localNotificationManager: LocalNotificationManager,
    private val notificationContentGenerator: NotificationContentGenerator,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            coroutineScope {
                val isPaidUser = preferencesManager.isPremiumUser()
                val content = notificationContentGenerator.generateNotificationContent(isPaidUser)
                
                if (content != null) {
                    localNotificationManager.showNotification(content)
                }
                
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
