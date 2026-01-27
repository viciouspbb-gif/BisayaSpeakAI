package com.bisayaspeak.ai.ui.home

import kotlin.math.max
import java.util.Locale

/**
 * Honor information for home screen and other UI.
 */
data class LevelHonorInfo(
    val level: Int,
    val title: String,
    val nickname: String
)

object LevelHonorHelper {

    private val honorTableEn: List<LevelHonorInfo> = listOf(
        LevelHonorInfo(1, "First Bisaya", "Journey Start!"),
        LevelHonorInfo(2, "Airport Greetings", "Immigration Master"),
        LevelHonorInfo(3, "Mactan Island Intro", "Island Debut"),
        LevelHonorInfo(4, "First Conversation", "First Step"),
        LevelHonorInfo(5, "Mactan Landing", "Landing Challenger"),
        LevelHonorInfo(6, "Beach Greetings", "Beach Friend"),
        LevelHonorInfo(7, "Resort Conversation", "Resort Guide"),
        LevelHonorInfo(8, "Beach Fun", "Wave Speaker"),
        LevelHonorInfo(9, "Islander Chat", "Island Mate"),
        LevelHonorInfo(10, "Santo Niño Visit", "Prayer Traveler"),
        LevelHonorInfo(11, "Historic Streets", "Knowledgeable Traveler"),
        LevelHonorInfo(12, "Church Prayers", "Silent Storyteller"),
        LevelHonorInfo(13, "Santo Niño Blessing!", "Blessing Caster"),
        LevelHonorInfo(14, "Cebu History", "Time Traveler"),
        LevelHonorInfo(15, "Jeepney Ride", "Street Pilot"),
        LevelHonorInfo(16, "Market Talk", "Market Master"),
        LevelHonorInfo(17, "Street Greetings", "Street Smile"),
        LevelHonorInfo(18, "Transport Learning", "Mobility Guide"),
        LevelHonorInfo(19, "Adventure Start", "Adventurer"),
        LevelHonorInfo(20, "Colon Street Shopping", "Shopping Expert"),
        LevelHonorInfo(21, "Market Negotiation", "Price Breaker"),
        LevelHonorInfo(22, "Souvenir Selection", "Gift Hunter"),
        LevelHonorInfo(23, "Price Haggling", "Smart Talker"),
        LevelHonorInfo(24, "Shopping Pro", "Bargain King"),
        LevelHonorInfo(25, "Island Hopping", "Island Adventurer"),
        LevelHonorInfo(26, "Uncharted Islands", "Frontier Diver"),
        LevelHonorInfo(27, "Explorer Heart", "Trailblazer"),
        LevelHonorInfo(28, "Sea Adventurer", "Marine Ranger"),
        LevelHonorInfo(29, "Island Master", "Cebu Sage"),
        LevelHonorInfo(30, "Swimming with Whale Shark", "Cebu Master!")
    )

    private val honorTableJa: List<LevelHonorInfo> = listOf(
        LevelHonorInfo(1, "はじめてのビサヤ", "旅のスタート！"),
        LevelHonorInfo(2, "空港の挨拶", "入国の達人"),
        LevelHonorInfo(3, "マクタン島入門", "島デビュー"),
        LevelHonorInfo(4, "最初の会話", "会話の第一歩"),
        LevelHonorInfo(5, "マクタン島に上陸", "上陸チャレンジャー"),
        LevelHonorInfo(6, "海辺の挨拶", "ビーチフレンド"),
        LevelHonorInfo(7, "リゾートでの会話", "リゾート案内人"),
        LevelHonorInfo(8, "ビーチの楽しみ", "波乗りスピーカー"),
        LevelHonorInfo(9, "島人との交流", "アイランドメイト"),
        LevelHonorInfo(10, "サントニーニョ参拝", "祈りの旅人"),
        LevelHonorInfo(11, "歴史の街を歩く", "博識トラベラー"),
        LevelHonorInfo(12, "教会の祈り", "静寂の語り部"),
        LevelHonorInfo(13, "サントニーニョの加護を！", "祝福キャスター"),
        LevelHonorInfo(14, "セブの歴史を学ぶ", "時空探訪者"),
        LevelHonorInfo(15, "ジプニーに乗る", "街乗りパイロット"),
        LevelHonorInfo(16, "市場での会話", "マーケットマスター"),
        LevelHonorInfo(17, "街角の挨拶", "ストリートスマイル"),
        LevelHonorInfo(18, "交通手段を学ぶ", "モビリティガイド"),
        LevelHonorInfo(19, "冒険の始まり", "アドベンチャラー"),
        LevelHonorInfo(20, "コロン通りの買い物", "ショッピング達人"),
        LevelHonorInfo(21, "市場交渉術", "プライスブレイカー"),
        LevelHonorInfo(22, "お土産選び", "ギフトハンター"),
        LevelHonorInfo(23, "値段交渉", "スマートトーカー"),
        LevelHonorInfo(24, "買い物上手", "バーゲンキング"),
        LevelHonorInfo(25, "アイランドホッピング", "島めぐり冒険者"),
        LevelHonorInfo(26, "未開の島々", "フロンティアダイバー"),
        LevelHonorInfo(27, "探検家の心", "トレイルブレイザー"),
        LevelHonorInfo(28, "海の冒険者", "マリンレンジャー"),
        LevelHonorInfo(29, "島の達人", "セブの賢者"),
        LevelHonorInfo(30, "ジンベエザメと泳ぐ", "セブの達人！")
    )

    fun getHonorInfo(rawLevel: Int): LevelHonorInfo {
        val level = max(1, rawLevel).coerceAtMost(30)
        val honorTable = if (Locale.getDefault().language == "ja") honorTableJa else honorTableEn
        return honorTable.getOrNull(level - 1)
            ?: if (Locale.getDefault().language == "ja") {
                LevelHonorInfo(level, "ビサヤマスター", "語学冒険王")
            } else {
                LevelHonorInfo(level, "Bisaya Master", "Language Adventure King")
            }
    }
}
