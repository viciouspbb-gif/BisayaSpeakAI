package com.bisayaspeak.ai.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * デバッグ用通知スケジューラー（開発・テスト専用）
 */
@Singleton
class DebugNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val REQUEST_CODE = 2001
        private const val ACTION_DEBUG_NOTIFICATION = "ACTION_DEBUG_NOTIFICATION"
    }
    
    /**
     * デバッグ用：1〜2分後に通知をスケジュール
     */
    fun scheduleDebugNotification(delayMinutes: Int = 1) {
        if (!BuildConfig.DEBUG) {
            Log.w("DebugNotificationScheduler", "デバッグビルド以外では実行されません")
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 指定分後の時刻を計算
        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000)
        
        // PendingIntentを作成
        val intent = Intent(ACTION_DEBUG_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            // 確実に発火
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            
            val nextTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(triggerTime))
            Log.d("DebugNotificationScheduler", "デバッグ通知を予約: $nextTime ( ${delayMinutes}分後)")
            
        } catch (e: SecurityException) {
            Log.e("DebugNotificationScheduler", "デバッグ通知予約失敗", e)
        }
    }
    
    /**
     * デバッグ用通知をキャンセル
     */
    fun cancelDebugNotification() {
        if (!BuildConfig.DEBUG) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(ACTION_DEBUG_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d("DebugNotificationScheduler", "デバッグ通知をキャンセル")
    }
}
