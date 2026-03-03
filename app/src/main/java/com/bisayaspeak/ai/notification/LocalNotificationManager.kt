package com.bisayaspeak.ai.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.MainActivity
import com.bisayaspeak.ai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知コンテンツのデータモデル
 */
data class NotificationContent(
    val title: String,
    val body: String,
    val deepLink: String
)

/**
 * ローカル通知マネージャー
 */
@Singleton
class LocalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "bisaya_notification_v2"
        private const val CHANNEL_NAME = "ビサヤ語学習通知"
        private const val CHANNEL_DESCRIPTION = "毎日のビサヤ語学習をサポートする通知"
        private const val NOTIFICATION_ID = 1001
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * 通知チャンネルを作成
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 通知を表示
     */
    fun showNotification(content: NotificationContent) {
        // MainActivityを起動するインテントを作成
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 通知権限をチェック
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12以前は権限不要
        }
    }
}

/**
 * 通知スケジューラー
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val REQUEST_CODE = 1001
        private const val ACTION_DAILY_NOTIFICATION = "ACTION_DAILY_NOTIFICATION"
    }
    
    /**
     * 毎日18:00頃の通知をスケジュール（確実性優先）
     */
    fun scheduleDailyNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 今日の18:00を計算
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // 今日の18:00が過ぎている場合は明日の18:00に設定
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val triggerTime = calendar.timeInMillis
        
        // PendingIntentを作成（一意性保証）
        val intent = Intent(ACTION_DAILY_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // Android 12+で正確なアラーム権限をチェック
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    // 権限がない場合は設定画面へ誘導
                    Log.e("NotificationScheduler", "SCHEDULE_EXACT_ALARM権限なし、設定画面へ誘導します")
                    requestExactAlarmPermission()
                    
                    // フォールバック（多少遅れる可能性あり）
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    
                    if (BuildConfig.DEBUG) {
                        Log.w("NotificationScheduler", "SCHEDULE_EXACT_ALARM権限なし、setAndAllowWhileIdleでフォールバック（18:00頃に通知）")
                    }
                } else {
                    // 権限がある場合もsetAndAllowWhileIdleを使用（ポリシー準拠）
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    
                    if (BuildConfig.DEBUG) {
                        Log.d("NotificationScheduler", "setAndAllowWhileIdleでアラームを設定（18:00頃に通知）")
                    }
                }
            } else {
                // Android 11以前もsetAndAllowWhileIdleを使用（ポリシー準拠）
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            // デバッグログ
            if (BuildConfig.DEBUG) {
                val nextTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(triggerTime))
                android.util.Log.d("NotificationScheduler", "次回18:00頃通知を予約: $nextTime")
            }
            
        } catch (e: SecurityException) {
            // その他のセキュリティ例外のフォールバック
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            
            if (BuildConfig.DEBUG) {
                android.util.Log.w("NotificationScheduler", "セキュリティ例外、setAndAllowWhileIdleでフォールバック（18:00頃に通知）")
            }
        }
    }

    /**
     * 全ての通知をキャンセル
     */
    fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(ACTION_DAILY_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        
        if (BuildConfig.DEBUG) {
            android.util.Log.d("NotificationScheduler", "18:00頃通知予約をキャンセル")
        }
    }
    
    /**
     * Android 12+で正確なアラーム権限を要求
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                
                // ContextがActivityでない場合はFLAG_ACTIVITY_NEW_TASKを追加
                val flags = if (context is android.app.Activity) {
                    0
                } else {
                    Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(intent.addFlags(flags))
                
                Log.d("NotificationScheduler", "正確なアラーム権限の設定画面を起動しました")
                
            } catch (e: Exception) {
                Log.e("NotificationScheduler", "正確なアラーム権限の設定画面起動に失敗", e)
                
                // フォールバック：アプリ設定画面へ誘導
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    
                    val flags = if (context is android.app.Activity) {
                        0
                    } else {
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    
                    context.startActivity(intent.addFlags(flags))
                    
                    Log.d("NotificationScheduler", "アプリ設定画面を起動しました（フォールバック）")
                    
                } catch (fallbackException: Exception) {
                    Log.e("NotificationScheduler", "アプリ設定画面の起動にも失敗", fallbackException)
                }
            }
        }
    }
}
