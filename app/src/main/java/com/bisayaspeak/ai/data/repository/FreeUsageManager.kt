package com.bisayaspeak.ai.data.repository

import android.util.Log
import com.bisayaspeak.ai.BisayaSpeakApp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * FreeUsageRepository をアプリ全体で使い回すためのヘルパー。
 */
object FreeUsageManager {

    private val repository by lazy { FreeUsageRepository(BisayaSpeakApp.instance) }
    private val mutex = Mutex()
    private val tokyoZoneId = ZoneId.of("Asia/Tokyo")
    private val dayFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private const val LOG_TAG = "LearnBisaya"

    fun currentDayKey(): String = ZonedDateTime.now(tokyoZoneId).format(dayFormatter)

    suspend fun resetIfNewDay(dayKey: String = currentDayKey()) {
        mutex.withLock {
            repository.resetIfNewDay(dayKey)
        }
    }

    suspend fun canUseTranslate(): Boolean = mutex.withLock { repository.canUseTranslate() }

    suspend fun consumeTranslate() = mutex.withLock { 
        Log.d("FreeUsage", "Count Incremented BEFORE Ad")
        repository.consumeTranslate() 
    }

    suspend fun canUseTalkTurn(): Boolean = mutex.withLock { repository.canUseTalkTurn() }

    suspend fun consumeTalkTurn() = mutex.withLock { repository.consumeTalkTurn() }

    suspend fun canStartSanpo(): Boolean = mutex.withLock { repository.canStartSanpo() }

    suspend fun consumeSanpoStart() = mutex.withLock { repository.consumeSanpoStart() }

    suspend fun translateCount(): Int = mutex.withLock { repository.getTranslateCount() }

    suspend fun talkTurnCount(): Int = mutex.withLock { repository.getTalkTurnCount() }

    suspend fun sanpoCount(): Int = mutex.withLock { repository.getSanpoCount() }

    suspend fun installId(): String = mutex.withLock { repository.getInstallId() }

    suspend fun dayKey(): String? = mutex.withLock { repository.getDayKey() }

    fun logUsage(message: String) {
        Log.d(LOG_TAG, message)
    }
}
