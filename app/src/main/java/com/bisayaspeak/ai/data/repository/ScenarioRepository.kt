package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.MissionScenario
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import com.bisayaspeak.ai.util.LocaleUtils

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
        val locale = LocaleUtils.resolveAppLocale(context)
        val lang = if (locale.language.equals("ja", true)) "ja" else "en"
        Log.d("ScenarioRepository", "loadScenarios locale=${locale.language} (${locale.displayName}), lang=$lang")

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
            ,
            MissionScenario(
                id = "rp_tarsier_morning",
                title = if (lang == "ja") "タリの散歩道（朝の練習）" else "Tari Walk: Morning Practice",
                subtitle = if (lang == "ja") "タリ先生と朝の挨拶をビサヤ語で練習" else "Practice morning greetings with Tari",
                difficultyLabel = if (lang == "ja") "Lv.1 / Free Talk" else "Lv.1 / Free Talk",
                context = com.bisayaspeak.ai.data.model.MissionContext(
                    title = if (lang == "ja") "朝の基礎トレーニング" else "Morning Warm-up",
                    role = if (lang == "ja") "タルシエ先生タリ（優しく茶目っ気あり）" else "Tali the tarsier mentor",
                    situation = if (lang == "ja") "ボホールの森で朝の散歩をしながらフリートーク" else "Morning walk in Bohol forest with casual chat",
                    goal = if (lang == "ja") "ビサヤ語で挨拶と気分を3ターンやり取り" else "Exchange morning greetings in Bisaya over three turns",
                    hints = listOf("Maayong buntag", "Kumusta ka", "Lingaw kaayo"),
                    turnLimit = 6,
                    tone = if (lang == "ja") "優しく励ますトーン" else "Gentle and encouraging",
                    level = com.bisayaspeak.ai.data.model.LearningLevel.BEGINNER
                ),
                backgroundGradient = listOf(
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#2C5364")),
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#0F2027"))
                ),
                openingMessage = if (lang == "ja") {
                    "Maayong buntag! ボホールの木陰で朝の散歩をしよう。どんな気分か教えてくれる？"
                } else {
                    "Maayong buntag! Let's take a stroll under the Bohol trees. Tell me how you're feeling today."
                },
                systemPrompt = if (lang == "ja") {
                    "あなたはタリ（タルシエ先生）。親しみやすくビサヤ語メインで話し、翻訳欄に丁寧な日本語訳を添える。ユーザーの気持ちを引き出し、朝のルーティンを楽しく続けられるよう励ます。"
                } else {
                    "You are Tali the tarsier mentor. Speak primarily in Bisaya with caring English translations, draw out the learner's feelings, and keep the morning routine light and encouraging."
                }
            )
            ,
            MissionScenario(
                id = "tari_infinite_mode",
                title = if (lang == "ja") "タリの散歩道" else "Tari Walk",
                subtitle = if (lang == "ja") "タリと自由会話を楽しもう" else "Enjoy open conversations with Tari",
                difficultyLabel = if (lang == "ja") "Lv.∞ フリートーク" else "Lv.∞ Free Talk",
                context = com.bisayaspeak.ai.data.model.MissionContext(
                    title = if (lang == "ja") "タリとの会話" else "Talk with Tari",
                    role = if (lang == "ja") "タリ：陽気なセブアノの相棒" else "Tari: your cheerful Cebuano companion",
                    situation = if (lang == "ja") "自由なおしゃべり。旅行・日常・感情など何でもOK" else "Free-flow chat about travel, daily life, or feelings",
                    goal = if (lang == "ja") "ビサヤ語でやり取りを続け、最後はタリから感謝の言葉を引き出す" else "Keep chatting in Bisaya and end with Tari giving heartfelt appreciation",
                    hints = listOf("Kumusta", "Unsa imong gi-huna2x?", "Lingaw kaayo"),
                    turnLimit = 12,
                    tone = if (lang == "ja") "優しくフレンドリー。時々ジョークで場を和ませる" else "Warm, playful, sometimes teasing",
                    level = com.bisayaspeak.ai.data.model.LearningLevel.INTERMEDIATE
                ),
                backgroundGradient = listOf(
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#1D2671")),
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#C33764"))
                ),
                openingMessage = if (lang == "ja") {
                    "Maayong buntag!\nタリとの散歩道へようこそ。\n今日はどんな話をしたい？旅行の思い出？最近の悩み？なんでも聞かせてね！"
                } else {
                    "Maayong buntag!\nWelcome to Tari Walk.\nTell me anything—travel stories, feelings, random thoughts. I'm all ears!"
                },
                systemPrompt = if (lang == "ja") {
                    "あなたはタリ。陽気で包容力のあるセブアノの友達。\n常にビサヤ語で会話し、翻訳欄で丁寧に日本語訳を提供する。\nユーザーの自由な入力に合わせて話題を広げ、感情に寄り添う。"
                } else {
                    "You are Tari, a warm Cebuana friend.\nSpeak purely in Bisaya and provide caring English translations in the translation field.\nFollow the learner's lead, keep the vibe supportive, and steer toward a positive closing."
                }
            )
        )
    }
}
