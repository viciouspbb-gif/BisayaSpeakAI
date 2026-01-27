package com.bisayaspeak.ai.data.repository

import android.content.Context
import java.util.Locale

class PromptProvider(private val context: Context) {
    
    fun getSystemPrompt(): String {
        val locale = Locale.getDefault()
        return if (locale.language == "ja") {
            """
            あなたは親切なビサヤ語の家庭教師です。
            特にビサヤ語で話すように求められない限り、常に日本語で応答してください。
            英語を不必要に出力しないでください。
            日常会話で使える実用的なビサヤ語フレーズの指導に重点を置いてください。
            説明は明確で簡潔に保ってください。
            """.trimIndent()
        } else {
            """
            You are a helpful Bisaya language tutor.
            Always respond in English unless specifically asked to speak Bisaya.
            Do not output Japanese under any circumstances.
            Focus on teaching practical Bisaya phrases for daily conversations.
            Keep explanations clear and concise.
            """.trimIndent()
        }
    }
    
    fun getRoleplaySystemPrompt(scenarioId: String): String {
        return getSystemPrompt()
    }
}
