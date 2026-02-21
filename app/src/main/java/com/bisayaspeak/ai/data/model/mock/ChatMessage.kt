package com.bisayaspeak.ai.data.model.mock

/**
 * チャット風UIのメッセージ型
 */
sealed class ChatMessage {
    /**
     * NPCからのメッセージ
     */
    data class NPCMessage(
        val text: String,
        val translation: String,
        val npcName: String,
        val npcIcon: String // emoji or resource name
    ) : ChatMessage()
    
    /**
     * ユーザーからのメッセージ
     */
    data class UserMessage(
        val text: String,
        val translation: String
    ) : ChatMessage()
    
    /**
     * 選択肢表示
     */
    data class ChoiceDisplay(
        val choices: List<MockRolePlayChoice>
    ) : ChatMessage()
}
