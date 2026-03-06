package com.bisayaspeak.ai.data.repository

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * タイムリリース管理リポジトリ
 * レベルの自動解放ロジックを管理
 */
@Singleton
class TimeReleaseRepository @Inject constructor() {
    
    /**
     * レベル解放スケジュール
     */
    data class LevelReleaseSchedule(
        val level: Int,
        val releaseDateTime: ZonedDateTime, // JST
        val isReleased: Boolean = false
    )
    
    /**
     * 解放スケジュールの定義
     */
    private val releaseSchedules = listOf(
        LevelReleaseSchedule(
            level = 31,
            releaseDateTime = ZonedDateTime.of(2026, 3, 6, 18, 0, 0, 0, ZoneId.of("Asia/Tokyo")),
            isReleased = true // 既に解放済み
        ),
        LevelReleaseSchedule(
            level = 32,
            releaseDateTime = ZonedDateTime.of(2026, 3, 13, 18, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        ),
        LevelReleaseSchedule(
            level = 33,
            releaseDateTime = ZonedDateTime.of(2026, 3, 20, 18, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        ),
        LevelReleaseSchedule(
            level = 34,
            releaseDateTime = ZonedDateTime.of(2026, 3, 27, 18, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        ),
        LevelReleaseSchedule(
            level = 35,
            releaseDateTime = ZonedDateTime.of(2026, 4, 3, 18, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        )
    )
    
    /**
     * 指定されたレベルが解放されているかチェック
     */
    fun isLevelReleased(level: Int): Boolean {
        val currentJST = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val schedule = releaseSchedules.find { it.level == level }
        
        return when {
            schedule == null -> false // 不明なレベル
            schedule.isReleased -> true // 手動解放済み
            currentJST.isAfter(schedule.releaseDateTime) || currentJST.isEqual(schedule.releaseDateTime) -> true
            else -> false
        }
    }
    
    /**
     * 解放までの残り時間を取得
     */
    fun getTimeUntilRelease(level: Int): String? {
        if (isLevelReleased(level)) return null
        
        val schedule = releaseSchedules.find { it.level == level } ?: return null
        val currentJST = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val duration = java.time.Duration.between(currentJST, schedule.releaseDateTime)
        
        return when {
            duration.isNegative -> null // 解放済み
            duration.toDays() > 0 -> "${duration.toDays()}日後"
            duration.toHours() > 0 -> "${duration.toHours()}時間後"
            duration.toMinutes() > 0 -> "${duration.toMinutes()}分後"
            else -> "まもなく解放"
        }
    }
    
    /**
     * 解放日時のフォーマット文字列を取得
     */
    fun getReleaseDateTimeString(level: Int): String? {
        val schedule = releaseSchedules.find { it.level == level } ?: return null
        
        if (isLevelReleased(level)) return "解放済み"
        
        val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
        return schedule.releaseDateTime.format(formatter)
    }
    
    /**
     * すべての解放スケジュールを取得
     */
    fun getAllReleaseSchedules(): List<LevelReleaseSchedule> {
        return releaseSchedules.map { schedule ->
            schedule.copy(isReleased = isLevelReleased(schedule.level))
        }
    }
    
    /**
     * 次に解放されるレベルを取得
     */
    fun getNextReleaseLevel(): LevelReleaseSchedule? {
        val currentJST = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        
        return releaseSchedules
            .filter { !isLevelReleased(it.level) }
            .filter { currentJST.isBefore(it.releaseDateTime) }
            .minByOrNull { it.releaseDateTime }
    }
    
    /**
     * 解放されたばかりのレベルを取得（現在時刻から1時間以内）
     */
    fun getRecentlyReleasedLevels(): List<Int> {
        val currentJST = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val oneHourAgo = currentJST.minusHours(1)
        
        return releaseSchedules
            .filter { schedule ->
                val releaseTime = schedule.releaseDateTime
                releaseTime.isAfter(oneHourAgo) && releaseTime.isBefore(currentJST.plusMinutes(5))
            }
            .map { it.level }
    }
}
