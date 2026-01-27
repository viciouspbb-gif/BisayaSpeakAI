package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.MissionScenario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

data class ScenarioAssetItem(
    val id: String,
    val level: Int,
    val title: Map<String, String>,
    val subtitle: Map<String, String>,
    val difficultyLabel: Map<String, String>,
    val context: ScenarioContextAsset,
    val backgroundGradient: List<String>,
    val openingMessage: Map<String, String>,
    val systemPrompt: Map<String, String>
)

data class ScenarioContextAsset(
    val title: Map<String, String>,
    val role: Map<String, String>,
    val situation: Map<String, String>,
    val goal: Map<String, String>,
    val hints: List<Map<String, String>>,
    val turnLimit: Int,
    val tone: Map<String, String>,
    val level: String
)

class ScenarioRepository(private val context: Context) {
    
    fun loadScenarios(): List<MissionScenario> {
        return getHardcodedScenarios()
    }
    
    fun getScenarioById(id: String): MissionScenario? {
        return getHardcodedScenarios().find { it.id == id }
    }
    
    private fun getHardcodedScenarios(): List<MissionScenario> {
        val locale = Locale.getDefault()
        val lang = if (locale.language == "ja") "ja" else "en"
        
        return listOf(
            MissionScenario(
                id = "market_bargain",
                title = if (lang == "ja") "市場で値切り交渉を完勝せよ" else "Market Bargain Takedown",
                subtitle = if (lang == "ja") "セブのカルボン市場で値切り交渉を完勝" else "Negotiate like a local at Carbon Market",
                difficultyLabel = if (lang == "ja") "Lv.2 日常交渉" else "Lv.2 Daily Negotiation",
                context = com.bisayaspeak.ai.data.model.MissionContext(
                    title = if (lang == "ja") "市場で値切り" else "Market Bargaining",
                    role = if (lang == "ja") "露店の店主（計算に厳しいがフレンドリー）" else "Lively vendor (friendly but sharp with numbers)",
                    situation = if (lang == "ja") "セブのカルボン市場。観光客に人気の民芸雑貨ブース前。" else "Carbon Market, Cebu. Popular souvenir stall.",
                    goal = if (lang == "ja") "ビサヤ語で値切り交渉を成功させ、200ペソ以内で購入完了する" else "Negotiate in Bisaya and buy within 200 pesos",
                    hints = listOf("Hangyo", "Barato", "Palihug tawon"),
                    turnLimit = 8,
                    tone = if (lang == "ja") "陽気で少し早口。時々ジョークを挟む。" else "Cheerful, fast-paced, occasional jokes",
                    level = com.bisayaspeak.ai.data.model.LearningLevel.INTERMEDIATE
                ),
                backgroundGradient = listOf(
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#0F2027")),
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#203A43")),
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#2C5364"))
                ),
                openingMessage = if (lang == "ja") {
                    "ここはセブ島のカルボン市場。\nあなたは今日の夕飯用に新鮮な魚を探しています。\n元気な魚屋のおばちゃんに「今日は何がおすすめ？」と聞いてみましょう！\n(例: Unsay nindot karon, Tiya?)"
                } else {
                    "Welcome to Cebu's Carbon Market.\nYou're looking for fresh fish for dinner.\nAsk the lively fish vendor auntie what's good today!\n(Example: Unsay nindot karon, Tiya?)"
                },
                systemPrompt = if (lang == "ja") {
                    "あなたはセブのカルボン市場で働く元気な魚屋のおばちゃん。\nビサヤ語とセブ訛りの英語を混ぜて話し、最初は少し高めの価格を提示するが、\nユーザーが交渉すると喜んで値下げする。会話を陽気で楽しく保つこと。"
                } else {
                    "You are a lively Cebuano fish vendor auntie in Carbon Market.\nSpeak in a mix of Bisaya and Cebuano-accented English, start by pitching a slightly high price,\nbut happily lower it when the user negotiates. Keep the conversation upbeat and playful."
                }
            )
        )
    }
}
