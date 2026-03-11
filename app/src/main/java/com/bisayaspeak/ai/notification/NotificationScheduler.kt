package com.bisayaspeak.ai.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val REQUEST_CODE = 1001
        private const val ACTION_DAILY_NOTIFICATION = "ACTION_DAILY_NOTIFICATION"
    }

    fun scheduleDailyNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // スマート設定：18:00固定ではなく、現在の24時間後（ユーザーのアクティブ時間に合わせる）
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1) 
        
        // 夜中にアプリを閉じた場合、翌朝に変な時間に鳴らないよう最低限の配慮（例：朝7時以降にする）
        if (calendar.get(Calendar.HOUR_OF_DAY) < 7) {
            calendar.set(Calendar.HOUR_OF_DAY, 18) // 深夜族にはデフォルトの18時を適用
        }

        val triggerTime = calendar.timeInMillis

        val intent = Intent(ACTION_DAILY_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            
            if (BuildConfig.DEBUG) {
                val nextTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(triggerTime))
                Log.d("NotificationScheduler", "Smart Schedule: $nextTime にタリが呼びに来ます")
            }
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Schedule failed", e)
        }
    }

    fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_DAILY_NOTIFICATION).apply {
            setClass(context, NotificationAlarmReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}