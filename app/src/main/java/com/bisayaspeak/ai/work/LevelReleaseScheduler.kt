package com.bisayaspeak.ai.work

import android.content.Context
import androidx.work.*
import com.bisayaspeak.ai.data.repository.TimeReleaseRepository
import com.bisayaspeak.ai.di.getAppEntryPoint
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.Duration
import javax.inject.Inject

/**
 * レベル解放スケジューラー
 * WorkManagerを使用してレベル解放通知をスケジュール
 */
class LevelReleaseScheduler @Inject constructor(
    private val context: Context
) {
    
    @Inject
    lateinit var timeReleaseRepository: TimeReleaseRepository
    
    /**
     * すべてのレベル解放通知をスケジュール
     */
    fun scheduleAllLevelReleases() {
        val entryPoint = getAppEntryPoint(context)
        timeReleaseRepository = entryPoint.getTimeReleaseRepository()
        
        val schedules = timeReleaseRepository.getAllReleaseSchedules()
        
        schedules.forEach { schedule ->
            if (!schedule.isReleased) {
                scheduleLevelRelease(schedule.level, schedule.releaseDateTime)
            }
        }
    }
    
    /**
     * 特定レベルの解放通知をスケジュール
     */
    private fun scheduleLevelRelease(level: Int, releaseDateTime: ZonedDateTime) {
        val currentJST = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        
        // 過去の日付ならスケジュールしない
        if (releaseDateTime.isBefore(currentJST)) {
            return
        }
        
        // 遅延時間を計算
        val delay = Duration.between(currentJST, releaseDateTime)
        
        // WorkRequestを作成
        val workRequest = OneTimeWorkRequestBuilder<LevelReleaseNotificationWorker>()
            .setInitialDelay(delay)
            .setInputData(
                workDataOf("level" to level)
            )
            .addTag("level_release_$level")
            .build()
        
        // WorkManagerにスケジュール
        WorkManager.getInstance(context).enqueueUniqueWork(
            "level_release_$level",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * 特定レベルのスケジュールをキャンセル
     */
    fun cancelLevelRelease(level: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("level_release_$level")
    }
    
    /**
     * すべてのレベル解放スケジュールをキャンセル
     */
    fun cancelAllLevelReleases() {
        WorkManager.getInstance(context).cancelAllWorkByTag("level_release")
    }
    
    /**
     * スケジュール状態を確認
     */
    fun getScheduleStatus(level: Int): WorkInfo? {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("level_release_$level")
            .value
        
        return workInfos?.firstOrNull()
    }
}
