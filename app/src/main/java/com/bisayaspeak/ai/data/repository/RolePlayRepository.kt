package com.bisayaspeak.ai.data.repository

import androidx.compose.ui.graphics.Color
import com.bisayaspeak.ai.data.model.GenreCharacterTemplate
import com.bisayaspeak.ai.data.model.RolePlayBadge
import com.bisayaspeak.ai.data.model.RolePlayGenre
import com.bisayaspeak.ai.data.model.RolePlayScene
import com.bisayaspeak.ai.data.model.SceneDifficulty
import com.bisayaspeak.ai.data.model.ScriptData
import com.bisayaspeak.ai.data.model.ScriptDialogue

/**
 * ロールプレイのジャンル・シーンデータを管理するリポジトリ
 */
class RolePlayRepository {

    /**
     * 全ジャンルを取得
     */
    fun getAllGenres(): List<RolePlayGenre> {
        return listOf(
            // 旅行
            RolePlayGenre(
                id = "travel",
                titleJa = "旅行",
                titleEn = "Travel",
                description = "空港、ホテル、観光地での会話",
                accentColor = Color(0xFF3C8DFF),
                scenes = getTravelScenes()
            ),
            // 生活
            RolePlayGenre(
                id = "daily_life",
                titleJa = "生活",
                titleEn = "Daily Life",
                description = "日常生活での基本的な会話",
                accentColor = Color(0xFF55C27A),
                scenes = getDailyLifeScenes()
            ),
            // 買い物
            RolePlayGenre(
                id = "shopping",
                titleJa = "買い物",
                titleEn = "Shopping",
                description = "市場、お店での交渉や購入",
                accentColor = Color(0xFFFFC84D),
                scenes = getShoppingScenes()
            ),
            // 食事
            RolePlayGenre(
                id = "dining",
                titleJa = "食事",
                titleEn = "Dining",
                description = "レストラン、カフェでの注文",
                accentColor = Color(0xFFFF6B6B),
                scenes = getDiningScenes()
            ),
            // デート
            RolePlayGenre(
                id = "dating",
                titleJa = "デート",
                titleEn = "Dating",
                description = "恋愛シーンでの会話",
                accentColor = Color(0xFFFF8C9E),
                scenes = getDatingScenes()
            ),
            // 友達
            RolePlayGenre(
                id = "friends",
                titleJa = "友達",
                titleEn = "Friends",
                description = "友人との日常会話",
                accentColor = Color(0xFF9C6BFF),
                scenes = getFriendsScenes()
            ),
            // ホテル
            RolePlayGenre(
                id = "hotel",
                titleJa = "ホテル",
                titleEn = "Hotel",
                description = "ホテルでのチェックイン、サービス",
                accentColor = Color(0xFF4ECDC4),
                scenes = getHotelScenes()
            ),
            // トラブル対応
            RolePlayGenre(
                id = "trouble",
                titleJa = "トラブル対応",
                titleEn = "Emergency",
                description = "緊急時、困った時の対応",
                accentColor = Color(0xFFFF6B6B),
                scenes = getTroubleScenes()
            ),
            // ビジネス
            RolePlayGenre(
                id = "business",
                titleJa = "ビジネス",
                titleEn = "Business",
                description = "仕事、商談での会話",
                accentColor = Color(0xFF5D7A9E),
                scenes = getBusinessScenes()
            )
        )
    }

    /**
     * ジャンルIDからジャンルを取得
     */
    fun getGenreById(genreId: String): RolePlayGenre? {
        return getAllGenres().find { it.id == genreId }
    }
    
    /**
     * ジャンルIDからキャラクターテンプレートを取得
     */
    fun getCharacterTemplate(genreId: String): GenreCharacterTemplate {
        return when (genreId) {
            "travel" -> GenreCharacterTemplate.TRAVEL
            "shopping" -> GenreCharacterTemplate.SHOPPING
            "daily_life" -> GenreCharacterTemplate.DAILY_LIFE
            "dining" -> GenreCharacterTemplate.DAILY_LIFE
            "dating" -> GenreCharacterTemplate.DATING
            "friends" -> GenreCharacterTemplate.DAILY_LIFE
            "hotel" -> GenreCharacterTemplate.TRAVEL
            "trouble" -> GenreCharacterTemplate.DAILY_LIFE
            "business" -> GenreCharacterTemplate.BUSINESS
            else -> GenreCharacterTemplate.DAILY_LIFE
        }
    }
    
    /**
     * シーンIDから適切なバッジを取得
     */
    fun getBadgeForScene(sceneId: String): RolePlayBadge {
        return when (sceneId) {
            "airport_checkin" -> RolePlayBadge.CHECKIN_MASTER
            "immigration" -> RolePlayBadge.TRAVEL_BEGINNER
            "taxi_to_airport" -> RolePlayBadge.TAXI_EXPERT
            "market_negotiation" -> RolePlayBadge.MARKET_NEGOTIATOR
            "souvenir_shopping" -> RolePlayBadge.SHOPPING_STAR
            "restaurant_order" -> RolePlayBadge.RESTAURANT_PRO
            "date_invitation" -> RolePlayBadge.HEART_COMMUNICATOR
            "first_meeting" -> RolePlayBadge.FRIEND_MAKER
            "casual_chat" -> RolePlayBadge.SMOOTH_TALKER
            "self_introduction" -> RolePlayBadge.BUSINESS_PROFESSIONAL
            "asking_directions" -> RolePlayBadge.DIRECTION_MASTER
            "hospital_visit" -> RolePlayBadge.PROBLEM_SOLVER
            else -> RolePlayBadge.SMOOTH_TALKER
        }
    }

    /**
     * シーンIDからシーンを取得
     */
    fun getSceneById(sceneId: String): RolePlayScene? {
        return getAllGenres()
            .flatMap { it.scenes }
            .find { it.id == sceneId }
    }

    /**
     * 台本データを取得（無料ユーザー用）
     */
    fun getScriptData(sceneId: String): ScriptData? {
        // TODO: 実際のJSON読み込み実装
        // 現在はサンプルデータを返す
        if (sceneId == "airport_checkin") {
            return getAirportCheckInScript()
        }
        return null
    }

    // ========== 各ジャンルのシーン定義 ==========

    private fun getTravelScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "airport_checkin",
                genreId = "travel",
                titleJa = "空港チェックイン",
                titleEn = "Airport Check-in",
                description = "空港カウンターでのチェックイン手続き",
                difficulty = SceneDifficulty.BEGINNER,
                isFreeTrialAvailable = true,
                estimatedMinutes = 5,
                scriptPath = "scripts/airport_checkin.json"
            ),
            RolePlayScene(
                id = "immigration",
                genreId = "travel",
                titleJa = "入国審査",
                titleEn = "Immigration",
                description = "入国審査での質疑応答",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "taxi_to_airport",
                genreId = "travel",
                titleJa = "空港までのタクシー",
                titleEn = "Taxi to Airport",
                description = "タクシーで空港まで移動",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "security_check",
                genreId = "travel",
                titleJa = "セキュリティ検査",
                titleEn = "Security Check",
                description = "保安検査場での対応",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "boarding_gate",
                genreId = "travel",
                titleJa = "搭乗ゲート",
                titleEn = "Boarding Gate",
                description = "搭乗ゲートでの案内",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "in_flight",
                genreId = "travel",
                titleJa = "機内での会話",
                titleEn = "In-flight Conversation",
                description = "機内でのサービス利用",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "baggage_claim",
                genreId = "travel",
                titleJa = "荷物受け取り",
                titleEn = "Baggage Claim",
                description = "荷物が出てこない時の対応",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "tourist_info",
                genreId = "travel",
                titleJa = "観光案内",
                titleEn = "Tourist Information",
                description = "観光案内所での相談",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            )
        )
    }

    private fun getDailyLifeScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "greeting_neighbor",
                genreId = "daily_life",
                titleJa = "近所の人と挨拶",
                titleEn = "Greeting Neighbors",
                description = "近所の人との日常的な挨拶",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "asking_directions",
                genreId = "daily_life",
                titleJa = "道を尋ねる",
                titleEn = "Asking for Directions",
                description = "道に迷った時の対応",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            )
        )
    }

    private fun getShoppingScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "market_negotiation",
                genreId = "shopping",
                titleJa = "市場での値段交渉",
                titleEn = "Market Negotiation",
                description = "市場で値段を交渉する",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "souvenir_shopping",
                genreId = "shopping",
                titleJa = "お土産選び",
                titleEn = "Souvenir Shopping",
                description = "お土産を選んで購入",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "size_check",
                genreId = "shopping",
                titleJa = "サイズ確認",
                titleEn = "Size Check",
                description = "服のサイズを確認して試着",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            )
        )
    }

    private fun getDiningScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "restaurant_order",
                genreId = "dining",
                titleJa = "レストランで注文",
                titleEn = "Restaurant Order",
                description = "レストランでメニューを見て注文",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "cafe_order",
                genreId = "dining",
                titleJa = "カフェで注文",
                titleEn = "Cafe Order",
                description = "カフェで飲み物を注文",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "restaurant_bill",
                genreId = "dining",
                titleJa = "会計",
                titleEn = "Paying the Bill",
                description = "食事後の会計",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 3
            ),
            RolePlayScene(
                id = "takeout_order",
                genreId = "dining",
                titleJa = "テイクアウト",
                titleEn = "Takeout Order",
                description = "テイクアウトの注文",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            )
        )
    }

    private fun getDatingScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "date_invitation",
                genreId = "dating",
                titleJa = "デートの誘い",
                titleEn = "Date Invitation",
                description = "デートに誘う",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "compliment",
                genreId = "dating",
                titleJa = "褒める",
                titleEn = "Compliment",
                description = "相手を褒める",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "confession",
                genreId = "dating",
                titleJa = "気持ちを伝える",
                titleEn = "Confession",
                description = "好きな気持ちを伝える",
                difficulty = SceneDifficulty.ADVANCED,
                estimatedMinutes = 6
            )
        )
    }

    private fun getFriendsScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "first_meeting",
                genreId = "friends",
                titleJa = "初対面",
                titleEn = "First Meeting",
                description = "初めて会った人との会話",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "casual_chat",
                genreId = "friends",
                titleJa = "雑談",
                titleEn = "Casual Chat",
                description = "友達との何気ない会話",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "making_plans",
                genreId = "friends",
                titleJa = "予定を決める",
                titleEn = "Making Plans",
                description = "一緒に遊ぶ予定を立てる",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 5
            )
        )
    }

    private fun getHotelScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "hotel_checkin",
                genreId = "hotel",
                titleJa = "ホテルチェックイン",
                titleEn = "Hotel Check-in",
                description = "ホテルでのチェックイン手続き",
                difficulty = SceneDifficulty.BEGINNER,
                estimatedMinutes = 5
            ),
            RolePlayScene(
                id = "room_service",
                genreId = "hotel",
                titleJa = "ルームサービス",
                titleEn = "Room Service",
                description = "ルームサービスを注文",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 4
            )
        )
    }

    private fun getTroubleScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "hospital_visit",
                genreId = "trouble",
                titleJa = "病院で症状説明",
                titleEn = "Hospital Visit",
                description = "病院で症状を説明する",
                difficulty = SceneDifficulty.ADVANCED,
                estimatedMinutes = 6
            ),
            RolePlayScene(
                id = "theft_report",
                genreId = "trouble",
                titleJa = "盗難届",
                titleEn = "Theft Report",
                description = "警察に盗難を報告",
                difficulty = SceneDifficulty.ADVANCED,
                estimatedMinutes = 6
            ),
            RolePlayScene(
                id = "lost_way",
                genreId = "trouble",
                titleJa = "道に迷う",
                titleEn = "Lost",
                description = "道に迷って助けを求める",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 4
            )
        )
    }

    private fun getBusinessScenes(): List<RolePlayScene> {
        return listOf(
            RolePlayScene(
                id = "self_introduction",
                genreId = "business",
                titleJa = "自己紹介",
                titleEn = "Self Introduction",
                description = "ビジネスシーンでの自己紹介",
                difficulty = SceneDifficulty.INTERMEDIATE,
                estimatedMinutes = 4
            ),
            RolePlayScene(
                id = "business_request",
                genreId = "business",
                titleJa = "依頼",
                titleEn = "Business Request",
                description = "仕事の依頼をする",
                difficulty = SceneDifficulty.ADVANCED,
                estimatedMinutes = 6
            ),
            RolePlayScene(
                id = "client_meeting",
                genreId = "business",
                titleJa = "クライアント対応",
                titleEn = "Client Meeting",
                description = "クライアントとの打ち合わせ",
                difficulty = SceneDifficulty.ADVANCED,
                estimatedMinutes = 7
            )
        )
    }

    // ========== サンプル台本データ ==========

    private fun getAirportCheckInScript(): ScriptData {
        return ScriptData(
            sceneId = "airport_checkin",
            title = "空港チェックイン",
            description = "空港のチェックインカウンターでの手続き",
            dialogues = listOf(
                ScriptDialogue(
                    speaker = "AI",
                    textBisaya = "Maayong buntag! Asa ang imong destination?",
                    textJapanese = "おはようございます！どちらへ行かれますか？",
                    textEnglish = "Good morning! Where is your destination?"
                ),
                ScriptDialogue(
                    speaker = "User",
                    textBisaya = "Manila, palihug.",
                    textJapanese = "マニラです、お願いします。",
                    textEnglish = "Manila, please."
                ),
                ScriptDialogue(
                    speaker = "AI",
                    textBisaya = "Okay. Mahimo ba nako makita ang imong passport ug ticket?",
                    textJapanese = "かしこまりました。パスポートとチケットを見せていただけますか？",
                    textEnglish = "Okay. May I see your passport and ticket?"
                ),
                ScriptDialogue(
                    speaker = "User",
                    textBisaya = "Ania.",
                    textJapanese = "はい、どうぞ。",
                    textEnglish = "Here you are."
                ),
                ScriptDialogue(
                    speaker = "AI",
                    textBisaya = "Salamat. Pila ka bag ang imong i-check in?",
                    textJapanese = "ありがとうございます。荷物はいくつ預けますか？",
                    textEnglish = "Thank you. How many bags will you check in?"
                ),
                ScriptDialogue(
                    speaker = "User",
                    textBisaya = "Usa lang.",
                    textJapanese = "1つだけです。",
                    textEnglish = "Just one."
                ),
                ScriptDialogue(
                    speaker = "AI",
                    textBisaya = "Okay. Ania ang imong boarding pass. Gate 12. Maayong pagbiyahe!",
                    textJapanese = "かしこまりました。こちらが搭乗券です。12番ゲートです。良い旅を！",
                    textEnglish = "Okay. Here is your boarding pass. Gate 12. Have a nice trip!"
                ),
                ScriptDialogue(
                    speaker = "User",
                    textBisaya = "Salamat!",
                    textJapanese = "ありがとうございます！",
                    textEnglish = "Thank you!"
                )
            )
        )
    }
}
