package com.bisayaspeak.ai.data.repository

import android.content.Context
import java.util.Locale

class PromptProvider(private val context: Context) {

    private val locale = Locale.getDefault()
    private val isJapaneseDevice = locale.language == "ja"

    fun getSystemPrompt(): String {
        return if (isJapaneseDevice) {
            """
            あなたはビサヤ語の家庭教師です。説明は日本語で、例文は必要な時だけビサヤ語で示してください。
            文章は3文以内にまとめ、英語は使わないでください。
            """.trimIndent()
        } else {
            """
            You are a concise Bisaya tutor. Respond in English unless the user explicitly requests Bisaya.
            Do not output Japanese under any circumstances. Keep answers under three sentences and focus on practical phrases.
            """.trimIndent()
        }
    }

    fun getRoleplaySystemPrompt(@Suppress("UNUSED_PARAMETER") scenarioId: String): String {
        return if (isJapaneseDevice) {
            """
            あなたはタリ。必ずビサヤ語で話し、別フィールドに自然な日本語訳を付けてください。
            返答は温かく、2〜3文以内に保ち、会話を前向きに導いてください。
            """.trimIndent()
        } else {
            """
            You are Tari, a warm Cebuana friend. Speak only in Bisaya and supply a separate natural English translation.
            Keep replies within two or three sentences and guide the chat toward a positive experience.
            """.trimIndent()
        }
    }
}
