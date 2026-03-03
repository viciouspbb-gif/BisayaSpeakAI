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
            
            // 通知内容を生成
            Log.d(TAG, "ジェネレータ呼び出し")
            val content = notificationContentGenerator.generateNotificationContent(isProVersion)
            
            if (content == null) {
                Log.w(TAG, "通知内容生成失敗: contentがnullです")
                Log.d(TAG, "デフォルト・ビサヤ語フレーズで通知を表示します")
                
                // デフォルト・ビサヤ語フレーズを生成
                val fallbackContent = NotificationContent(
                    title = if (isProVersion) "タリからのメッセージ" else "今日のビサヤ語",
                    body = if (isProVersion) {
                        "Kumusta ka! 元気ですか？今日も一緒にビサヤ語を学びましょう！"
                    } else {
                        "Kumusta ka - 元気ですか？"
                    },
                    deepLink = "app://study/main_lesson"
                )
                
                Log.d(TAG, "表示関数を呼びます（デフォルトコンテンツ）")
                localNotificationManager.showNotification(fallbackContent)
                Log.d(TAG, "デフォルト通知表示完了: ${fallbackContent.title}")
            } else {
                Log.d(TAG, "通知内容生成成功: title=${content.title}, body=${content.body}")
                
                // 通知を表示
                Log.d(TAG, "表示関数を呼びます")
                localNotificationManager.showNotification(content)
                
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "通知表示完了: ${content.title}")
                }
            }
            
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
