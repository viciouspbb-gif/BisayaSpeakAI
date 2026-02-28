package com.bisayaspeak.ai.di

import android.content.Context
import com.bisayaspeak.ai.data.preferences.PreferencesManager
import com.bisayaspeak.ai.notification.LocalNotificationManager
import com.bisayaspeak.ai.notification.NotificationContentGenerator
import com.bisayaspeak.ai.notification.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Application起動時クラッシュ根絶用EntryPoint
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun getNotificationScheduler(): NotificationScheduler
    fun getNotificationContentGenerator(): NotificationContentGenerator
    fun getPreferencesManager(): PreferencesManager
    fun getLocalNotificationManager(): LocalNotificationManager
}

/**
 * 安全にNotificationSchedulerを取得するヘルパー関数
 */
fun getAppEntryPoint(context: Context): AppEntryPoint {
    return EntryPointAccessors.fromApplication(
        context.applicationContext,
        AppEntryPoint::class.java
    )
}
