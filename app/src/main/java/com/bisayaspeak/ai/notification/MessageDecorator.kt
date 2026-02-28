package com.bisayaspeak.ai.notification

import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 曜日判定ユーティリティ
 */
@Singleton
class DayOfWeekChecker @Inject constructor() {
    
    /**
     * 現在の曜日を取得
     * @return Calendar.DAY_OF_WEEK (1=日曜日, 2=月曜日, ..., 7=土曜日)
     */
    fun getCurrentDayOfWeek(): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        return calendar.get(Calendar.DAY_OF_WEEK)
    }
    
    /**
     * 土曜日かどうかを判定
     */
    fun isSaturday(): Boolean {
        return getCurrentDayOfWeek() == Calendar.SATURDAY
    }
    
    /**
     * 日曜日かどうかを判定
     */
    fun isSunday(): Boolean {
        return getCurrentDayOfWeek() == Calendar.SUNDAY
    }
    
    /**
     * 平日かどうかを判定（月〜金）
     */
    fun isWeekday(): Boolean {
        val day = getCurrentDayOfWeek()
        return day in Calendar.MONDAY..Calendar.FRIDAY
    }
    
    /**
     * 曜日名を取得
     */
    fun getDayName(): String {
        return when (getCurrentDayOfWeek()) {
            Calendar.SUNDAY -> "日曜日"
            Calendar.MONDAY -> "月曜日"
            Calendar.TUESDAY -> "火曜日"
            Calendar.WEDNESDAY -> "水曜日"
            Calendar.THURSDAY -> "木曜日"
            Calendar.FRIDAY -> "金曜日"
            Calendar.SATURDAY -> "土曜日"
            else -> "不明"
        }
    }
}

/**
 * メッセージ装飾エンジン（CTA連結）
 */
@Singleton
class MessageDecorator @Inject constructor() {
    
    /**
     * 無料ユーザー向けメッセージにCTAを追加
     */
    fun decorateFreeUserMessage(phrase: String): String {
        return "$phrase + 「タリと練習してみよう！」"
    }
    
    /**
     * 有料ユーザー向けメッセージにCTAを追加
     */
    fun decoratePaidUserMessage(phrase: String): String {
        return "「$phrase」って、今言いたくなっちゃった。+ 「今日はどんな会話する？」"
    }
    
    /**
     * 土曜日限定有料ユーザー向けメッセージ
     */
    fun decorateSaturdayPaidUserMessage(theme: SaturdayTheme): String {
        val themeText = when (theme) {
            SaturdayTheme.LOVE -> "恋愛"
            SaturdayTheme.BUSINESS -> "ビジネス"
        }
        return "今日はタリと（${themeText}）会話してみる？ 最初に話しかけてね。"
    }
    
    /**
     * 土曜日限定無料ユーザー向けメッセージ
     */
    fun decorateSaturdayFreeUserMessage(): String {
        return "タリと自由に会話してみる？ プレミアムプランで可能です。"
    }
}

/**
 * 土曜日のテーマ定義
 */
enum class SaturdayTheme {
    LOVE,      // 恋愛
    BUSINESS   // ビジネス
}

/**
 * 土曜日テーマ管理
 */
@Singleton
class SaturdayThemeManager @Inject constructor() {
    
    private var currentTheme: SaturdayTheme = SaturdayTheme.LOVE
    
    /**
     * 現在のテーマを取得
     */
    fun getCurrentTheme(): SaturdayTheme {
        return currentTheme
    }
    
    /**
     * テーマを切り替え（月ごとまたはランダム）
     */
    fun switchTheme() {
        currentTheme = when (currentTheme) {
            SaturdayTheme.LOVE -> SaturdayTheme.BUSINESS
            SaturdayTheme.BUSINESS -> SaturdayTheme.LOVE
        }
    }
    
    /**
     * ラダムにテーマを選択
     */
    fun randomTheme(): SaturdayTheme {
        return if (kotlin.random.Random.nextBoolean()) {
            SaturdayTheme.LOVE
        } else {
            SaturdayTheme.BUSINESS
        }
    }
    
    /**
     * 月に基づいてテーマを決定
     */
    fun getThemeByMonth(): SaturdayTheme {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return if (month % 2 == 0) {
            SaturdayTheme.LOVE
        } else {
            SaturdayTheme.BUSINESS
        }
    }
}

/**
 * 土曜日専用通知コンテンツ
 */
@Singleton
class SaturdayNotificationContent @Inject constructor(
    private val saturdayThemeManager: SaturdayThemeManager,
    private val messageDecorator: MessageDecorator
) {
    
    /**
     * 有料ユーザー向け土曜日通知を生成
     */
    fun generatePaidUserContent(): NotificationContent {
        val theme = saturdayThemeManager.getCurrentTheme()
        return NotificationContent(
            title = "土曜日の特別な時間",
            body = messageDecorator.decorateSaturdayPaidUserMessage(theme),
            deepLink = "app://tari_walk/main"
        )
    }
    
    /**
     * 無料ユーザー向け土曜日通知を生成
     */
    fun generateFreeUserContent(): NotificationContent {
        return NotificationContent(
            title = "タリからのお誘い",
            body = messageDecorator.decorateSaturdayFreeUserMessage(),
            deepLink = "app://study/main_lesson"
        )
    }
}
