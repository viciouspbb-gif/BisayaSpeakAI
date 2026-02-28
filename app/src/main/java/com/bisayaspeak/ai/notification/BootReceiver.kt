package com.bisayaspeak.ai.notification

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.di.getAppEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 端末再起動時の通知再スケジュール用Receiver
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (BuildConfig.DEBUG) {
                Log.d("BootReceiver", "端末起動を検知、18:00頃通知を再スケジュール")
            }
            
            // 端末再起動後に通知を再スケジュール
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // EntryPoint経由で安全に取得
                    val entryPoint = getAppEntryPoint(context.applicationContext as Application)
                    val scheduler = entryPoint.getNotificationScheduler()
                    scheduler.scheduleDailyNotification()
                    
                    if (BuildConfig.DEBUG) {
                        Log.d("BootReceiver", "18:00頃通知の再スケジュール完了")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("BootReceiver", "18:00頃通知の再スケジュール失敗", e)
                    }
                }
            }
        }
    }
}
