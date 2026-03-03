package com.bisayaspeak.ai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bisayaspeak.ai.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

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
        
        // WorkManagerに処理を委譲して即座にreturn
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
        
        Log.d("NotificationAlarmReceiver", "WorkManagerに処理を委譲しました")
    }
}
