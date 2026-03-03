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

/**
 * 通知処理を行うWorkManager Worker
 * BroadcastReceiverから呼び出され、重い処理をバックグラウンドで実行
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "NotificationWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "NotificationWorker開始")
        
        return try {
            // 現在時刻をログ
            val currentTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            Log.d(TAG, "通知処理開始時刻: $currentTime")
            
            // EntryPointから依存関係を取得
            val entryPoint = getAppEntryPoint(applicationContext)
            val localNotificationManager = entryPoint.getLocalNotificationManager()
            val notificationScheduler = entryPoint.getNotificationScheduler()
            val notificationContentGenerator = entryPoint.getNotificationContentGenerator()
            
            // 課金状態をビルドバリアントで物理的に切り分け
            val isProVersion = BuildConfig.FLAVOR == "pro"
            Log.d(TAG, "ビルドバリアントによる課金判定: ${BuildConfig.FLAVOR} -> PRO=$isProVersion")
            
            // 通知内容を最終固定
            val content = if (isProVersion) {
                // PRO版（タリモード）- 固定フレーズ
                NotificationContent(
                    title = "タリからのメッセージ",
                    body = "今日の散歩どうする？",
                    deepLink = "app://study/main_lesson"
                )
            } else {
                // LITE版（教材モード）- JSONから取得
                Log.d(TAG, "ジェネレータ呼び出し（LITE版）")
                val generatedContent = notificationContentGenerator.generateNotificationContent(false)
                
                if (generatedContent != null) {
                    generatedContent.copy(
                        title = "今日のビサヤ語",
                        body = "Kumusta ka - わかりますか？" // 固定形式で表示
                    )
                } else {
                    // フォールバック
                    NotificationContent(
                        title = "今日のビサヤ語",
                        body = "Kumusta ka - わかりますか？",
                        deepLink = "app://study/main_lesson"
                    )
                }
            }
            
            Log.d(TAG, "通知内容確定: title=${content.title}, body=${content.body}")
            
            // 通知を表示
            Log.d(TAG, "表示関数を呼びます")
            localNotificationManager.showNotification(content)
            Log.d(TAG, "通知表示完了: ${content.title}")
            
            // 次の日の通知を再スケジュール
            Log.d(TAG, "次回通知のスケジュールを開始")
            notificationScheduler.scheduleDailyNotification()
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "次回通知のスケジュール完了")
            }
            
            Log.d(TAG, "通知処理成功")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "通知処理で例外が発生", e)
            Result.failure()
        }
    }
}
