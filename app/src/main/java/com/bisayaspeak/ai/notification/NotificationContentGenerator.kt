package com.bisayaspeak.ai.notification

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ユーザーセグメント別の通知内容生成
 */
@Singleton
class NotificationContentGenerator @Inject constructor(
    private val phraseExtractor: PhraseExtractor,
    private val messageDecorator: MessageDecorator,
    private val dayOfWeekChecker: DayOfWeekChecker,
    private val saturdayNotificationContent: SaturdayNotificationContent
) {
    
    /**
     * 通知内容を生成（土曜日判定を含む）
     */
    suspend fun generateNotificationContent(isPaidUser: Boolean): NotificationContent? {
        return if (dayOfWeekChecker.isSaturday()) {
            // 土曜日限定通知
            if (isPaidUser) {
                saturdayNotificationContent.generatePaidUserContent()
            } else {
                saturdayNotificationContent.generateFreeUserContent()
            }
        } else {
            // 平日・日曜日の通常通知
            generateDailyNotificationContent(isPaidUser)
        }
    }
    
    /**
     * 平日・日曜日の通常通知内容を生成
     */
    private suspend fun generateDailyNotificationContent(isPaidUser: Boolean): NotificationContent? {
        val phrase = phraseExtractor.extractRandomPhrase() ?: return null
        
        return if (isPaidUser) {
            // 有料ユーザー：タイトル「タリからのメッセージ / Message from Tali」
            NotificationContent(
                title = "タリからのメッセージ / Message from Tali",
                body = "「${phrase}」って、今言いたくなっちゃった。",
                deepLink = "app://tari_walk/main"
            )
        } else {
            // 無料ユーザー：タイトル「今日のビサヤ語 / Today's Bisaya」
            val translations = phraseExtractor.getTranslations(phrase) ?: mapOf("ja" to "", "en" to "")
            NotificationContent(
                title = "今日のビサヤ語 / Today's Bisaya",
                body = "${phrase} (${translations["ja"]} / ${translations["en"]})",
                deepLink = "app://study/main_lesson"
            )
        }
    }
    
    /**
     * LV31データ追加完了後の通知内容を生成
     */
    suspend fun generateLevel31CompleteNotification(): NotificationContent {
        return NotificationContent(
            title = "リスニングデータ更新完了 / Listening Data Updated",
            body = "LV31の新しいリスニング練習データが追加されました！ / New LV31 listening practice data has been added!",
            deepLink = "app://study/main_lesson"
        )
    }
    
    /**
     * 無料ユーザー向け通知内容を生成（レガシー互換）
     */
    suspend fun generateFreeUserContent(): NotificationContent? {
        return generateNotificationContent(isPaidUser = false)
    }
    
    /**
     * 有料ユーザー向け通知内容を生成（レガシー互換）
     */
    suspend fun generatePaidUserContent(): NotificationContent? {
        return generateNotificationContent(isPaidUser = true)
    }
}
