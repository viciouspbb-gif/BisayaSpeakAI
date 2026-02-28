package com.bisayaspeak.ai.di

import android.content.Context
import com.bisayaspeak.ai.notification.NotificationScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hiltのlateinitに頼らない安全なEntryPoint
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationSchedulerEntryPoint {
    fun getNotificationScheduler(): NotificationScheduler
}

/**
 * 安全にNotificationSchedulerを取得するヘルパー関数
 */
fun getNotificationSchedulerSafely(context: Context): NotificationScheduler? {
    return try {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationSchedulerEntryPoint::class.java
        )
        entryPoint.getNotificationScheduler()
    } catch (e: Exception) {
        null
    }
}
