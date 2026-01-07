package com.bisayaspeak.ai.data.model

import androidx.compose.ui.graphics.Color

/**
 * ロールプレイのジャンル
 */
data class RolePlayGenre(
    val id: String,
    val titleJa: String,
    val titleEn: String,
    val description: String,
    val accentColor: Color,
    val scenes: List<RolePlayScene>
)

/**
 * ロールプレイのシーン
 */
data class RolePlayScene(
    val id: String,
    val genreId: String,
    val titleJa: String,
    val titleEn: String,
    val description: String,
    val difficulty: SceneDifficulty,
    val isFreeTrialAvailable: Boolean = false,
    val estimatedMinutes: Int = 5,
    val scriptPath: String? = null // 台本JSONのパス
)

/**
 * シーンの難易度
 */
enum class SceneDifficulty(val displayName: String) {
    BEGINNER("初級"),
    INTERMEDIATE("中級"),
    ADVANCED("上級")
}

/**
 * 台本データ（無料ユーザー用）
 */
data class ScriptData(
    val sceneId: String,
    val title: String,
    val description: String,
    val dialogues: List<ScriptDialogue>
)

/**
 * 台本の1つの会話
 */
data class ScriptDialogue(
    val speaker: String, // "AI" or "User"
    val textBisaya: String,
    val textJapanese: String,
    val textEnglish: String,
    val note: String? = null // 発音のヒントなど
)

/**
 * ロールプレイの進行状態
 */
data class RolePlayProgress(
    val sceneId: String,
    val isCompleted: Boolean = false,
    val turnCount: Int = 0,
    val goalAchieved: Boolean = false,
    val feedback: String? = null,
    val evaluation: RolePlayEvaluation? = null
)

/**
 * ロールプレイの評価結果
 */
data class RolePlayEvaluation(
    val stars: Int, // 1-3
    val goodPoint: String, // 良かった点
    val improvementPoint: String, // 改善点
    val badge: RolePlayBadge, // 獲得バッジ
    val encouragement: String // 励ましのメッセージ
)

/**
 * ロールプレイバッジ
 */
enum class RolePlayBadge(val displayName: String, val emoji: String) {
    TRAVEL_BEGINNER("Travel Beginner", "✈️"),
    MARKET_NEGOTIATOR("Market Negotiator", "🛒"),
    DIRECTION_MASTER("Direction Master", "🗺️"),
    SMOOTH_TALKER("Smooth Talker", "💬"),
    HEART_COMMUNICATOR("Heart Communicator", "❤️"),
    CHECKIN_MASTER("Check-in Master", "🧳"),
    RESTAURANT_PRO("Restaurant Pro", "🍽️"),
    TAXI_EXPERT("Taxi Expert", "🚕"),
    SHOPPING_STAR("Shopping Star", "🛍️"),
    FRIEND_MAKER("Friend Maker", "👥"),
    BUSINESS_PROFESSIONAL("Business Professional", "💼"),
    STUDENT_ACHIEVER("Student Achiever", "📚"),
    PROBLEM_SOLVER("Problem Solver", "🔧")
}

/**
 * ジャンル別AIキャラクター設定
 */
enum class GenreCharacterTemplate(val tone: String, val description: String) {
    TRAVEL(
        "フォーマルで丁寧",
        "空港職員、ホテルスタッフ、観光案内など、プロフェッショナルな対応を心がけます。"
    ),
    SHOPPING(
        "カジュアルでローカル",
        "市場の店員のように親しみやすく、値段交渉にも応じます。"
    ),
    TRANSPORTATION(
        "短く実用的",
        "タクシー運転手や案内係として、方向指示に特化した会話をします。"
    ),
    DAILY_LIFE(
        "フレンドリー",
        "友達のように気軽に話し、日常的な話題で盛り上がります。"
    ),
    DATING(
        "親しみのあるカジュアル",
        "優しく親しみやすいトーンで、適度な距離感を保ちます。"
    ),
    BUSINESS(
        "丁寧でフォーマル",
        "ビジネスシーンに相応しい礼儀正しい対応をします。"
    ),
    SCHOOL(
        "教えるようなトーン",
        "先生や先輩のように、分かりやすく説明します。"
    )
}
