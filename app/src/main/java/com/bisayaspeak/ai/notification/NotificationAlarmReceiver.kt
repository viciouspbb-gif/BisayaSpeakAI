package com.bisayaspeak.ai.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.preferences.PreferencesManager
import com.bisayaspeak.ai.di.getAppEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 通知用BroadcastReceiver（AlarmManagerから呼び出される）
 */
class NotificationAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // ② レシーバーの「生存確認」ログの追加
        Log.e("ALARM_HIT", "信号受信！")
        
        // デバッグログ
        if (BuildConfig.DEBUG) {
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            Log.d("NotificationAlarmReceiver", "通知受信: $currentTime")
        }
        
        // IO Coroutineで通知処理を実行
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // EntryPoint経由で安全に依存関係を取得
                val entryPoint = getAppEntryPoint(context.applicationContext as Application)
                val localNotificationManager = entryPoint.getLocalNotificationManager()
                val notificationContentGenerator = entryPoint.getNotificationContentGenerator()
                val preferencesManager = entryPoint.getPreferencesManager()
                
                // Intent extraから取得せず、PreferencesManagerから課金状態を取得
                val isPaidUser = preferencesManager.isPremiumUser()
                
                if (BuildConfig.DEBUG) {
                    Log.d("NotificationAlarmReceiver", "課金状態: 有料=$isPaidUser")
                }
                
                // 通知内容を生成
                val content = notificationContentGenerator.generateNotificationContent(isPaidUser)
                
                if (BuildConfig.DEBUG) {
                    Log.d("NotificationAlarmReceiver", "通知内容生成: ${content != null}")
                }
                
                if (content != null) {
                    // 通知を表示
                    showNotification(context, content)
                }
                
                // 次の日の通知を再スケジュール
                val notificationScheduler = entryPoint.getNotificationScheduler()
                notificationScheduler.scheduleDailyNotification()
                
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("NotificationAlarmReceiver", "18:00頃通知処理エラー", e)
                }
            }
        }
    }
    
    /**
     * 通知を表示
     */
    private fun showNotification(context: Context, content: NotificationContent) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        // 通知チャンネルを作成（Android 8.0以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bisaya_notification_v2", // ③ チャンネルIDを更新して強制リセット
                "ビサヤ語学習通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "毎日のビサヤ語学習をサポートする通知"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 通知権限をチェック
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            if (BuildConfig.DEBUG) {
                Log.w("NotificationAlarmReceiver", "POST_NOTIFICATIONS権限なし")
            }
            return
        }
        
        val notification = NotificationCompat.Builder(context, "bisaya_daily_notification")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(1001, notification)
        
        if (BuildConfig.DEBUG) {
            Log.d("NotificationAlarmReceiver", "通知表示: ${content.title}")
        }
    }
}
