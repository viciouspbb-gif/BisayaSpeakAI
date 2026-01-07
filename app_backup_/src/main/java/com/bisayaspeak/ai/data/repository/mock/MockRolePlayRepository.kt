package com.bisayaspeak.ai.data.repository.mock

import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.mock.MockRolePlayChoice
import com.bisayaspeak.ai.data.model.mock.MockRolePlayScenario
import com.bisayaspeak.ai.data.model.mock.MockRolePlayStep

class MockRolePlayRepository {

    private val scenarios: List<MockRolePlayScenario> = listOf(
        createAirportCheckInScenario(),
        createHotelCheckInScenario(),
        createRestaurantOrderScenario()
    )

    fun getScenarios(): List<MockRolePlayScenario> = scenarios

    fun getScenarioById(id: String): MockRolePlayScenario? =
        scenarios.find { it.id == id }

    private fun createAirportCheckInScenario(): MockRolePlayScenario {
        return MockRolePlayScenario(
            id = "airport_checkin",
            titleJa = "空港チェックイン",
            level = LearningLevel.BEGINNER,
            npcName = "受付スタッフ",
            npcIcon = "🛫",
            steps = listOf(
                MockRolePlayStep(
                    id = "a1",
                    aiLineVisayan = "Maayong buntag. Asa ka paingon?",
                    aiLineJa = "おはようございます。どちらへ向かいますか？",
                    choices = listOf(
                        MockRolePlayChoice("Paingon ko sa Cebu.", "セブに行きます。", true),
                        MockRolePlayChoice("Dili ko ganahan mokaon.", "食べたくない。", false),
                        MockRolePlayChoice("Asa ang hospital?", "病院どこ？", false)
                    )
                ),
                MockRolePlayStep(
                    id = "a2",
                    aiLineVisayan = "Sige. Makita nako ang imong passport ug ticket?",
                    aiLineJa = "わかりました。パスポートとチケットを見せていただけますか？",
                    choices = listOf(
                        MockRolePlayChoice("Mao ni.", "はい、どうぞ。", true),
                        MockRolePlayChoice("Naa kay tubig?", "水ある？", false),
                        MockRolePlayChoice("Dili ko ganahan.", "嫌です。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "a3",
                    aiLineVisayan = "Salamat. Ang imong bagahe kay 18 kilos, okay ra.",
                    aiLineJa = "ありがとうございます。荷物は18キロで問題ありません。",
                    choices = listOf(
                        MockRolePlayChoice("Sige, salamat.", "わかりました、ありがとう。", true),
                        MockRolePlayChoice("Pwede ba ko magdala ug gamay pa?", "もう少し荷物を持ち込んでもいいですか？", false),
                        MockRolePlayChoice("Pwede ba nako ablihon ang bagahe?", "荷物を開けてもいいですか？", false)
                    )
                ),
                MockRolePlayStep(
                    id = "a4",
                    aiLineVisayan = "Naa na ang imong boarding pass. Ang gate kay sa Gate 12.",
                    aiLineJa = "ボーディングパスはこちらです。搭乗口は12番です。",
                    choices = listOf(
                        MockRolePlayChoice("Asa ang Gate 12?", "12番ゲートはどこですか？", true),
                        MockRolePlayChoice("Gigutom ko.", "お腹すいた。", false),
                        MockRolePlayChoice("Naa kay isda?", "魚ある？", false)
                    )
                ),
                MockRolePlayStep(
                    id = "a5",
                    aiLineVisayan = "Ang Gate 12 kay sa tuo, unya liko sa wala.",
                    aiLineJa = "12番ゲートは右に進んで左に曲がったところです。",
                    choices = listOf(
                        MockRolePlayChoice("Salamat kaayo!", "ありがとうございます！", true),
                        MockRolePlayChoice("Asa ang hotel?", "ホテルどこ？", false),
                        MockRolePlayChoice("Dili ko gusto.", "好きじゃない。", false)
                    ),
                    isFinal = true
                )
            )
        )
    }

    private fun createHotelCheckInScenario(): MockRolePlayScenario {
        return MockRolePlayScenario(
            id = "hotel_checkin",
            titleJa = "ホテルチェックイン",
            level = LearningLevel.BEGINNER,
            npcName = "フロントスタッフ",
            npcIcon = "🏨",
            steps = listOf(
                MockRolePlayStep(
                    id = "h1",
                    aiLineVisayan = "Maayong buntag. Unsa imong pangalan?",
                    aiLineJa = "おはようございます。お名前をお願いします。",
                    choices = listOf(
                        MockRolePlayChoice("Ako si Tanaka.", "田中です。", true),
                        MockRolePlayChoice("Asa ang airport?", "空港どこ？", false),
                        MockRolePlayChoice("Gigutom ko.", "お腹すいた。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "h2",
                    aiLineVisayan = "Salamat, Mr. Tanaka. Naa kay reservation?",
                    aiLineJa = "田中様、ご予約はありますか？",
                    choices = listOf(
                        MockRolePlayChoice("Oo, naa.", "はい、あります。", true),
                        MockRolePlayChoice("Wala ko kasabot.", "理解できない。", false),
                        MockRolePlayChoice("Dili ko ganahan mokaon.", "食べたくない。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "h3",
                    aiLineVisayan = "Ayos. Ang reservation kay usa ka gabii.",
                    aiLineJa = "一泊のご予約ですね。",
                    choices = listOf(
                        MockRolePlayChoice("Sakto.", "はい、その通りです。", true),
                        MockRolePlayChoice("Asa ang dagat?", "海どこ？", false),
                        MockRolePlayChoice("Dili ko gusto.", "嫌だ。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "h4",
                    aiLineVisayan = "Okay. Kini ang keycard.",
                    aiLineJa = "こちらがキーです。",
                    choices = listOf(
                        MockRolePlayChoice("Asa ang kwarto?", "部屋はどこですか？", true),
                        MockRolePlayChoice("Naa kay isda?", "魚ある？", false),
                        MockRolePlayChoice("Dili ko ganahan.", "嫌です。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "h5",
                    aiLineVisayan = "Ang imong kwarto kay sa 5th floor, room 512.",
                    aiLineJa = "部屋は5階の512号室です。",
                    choices = listOf(
                        MockRolePlayChoice("Salamat. Asa ang elevator?", "ありがとう。エレベーターはどこ？", true),
                        MockRolePlayChoice("Naa kay isda?", "魚ある？", false),
                        MockRolePlayChoice("Dili ko mahadlok.", "怖くない。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "h6",
                    aiLineVisayan = "Ang elevator naa sa unahan, dayon liko sa wala. Mao na ang paingon sa imong kwarto.",
                    aiLineJa = "エレベーターはこの先の左にあります。そこからお部屋に向かえます。",
                    choices = listOf(
                        MockRolePlayChoice("Salamat kaayo!", "ありがとうございます！", true),
                        MockRolePlayChoice("Dili ko gusto.", "嫌だ。", false),
                        MockRolePlayChoice("Asa ang airport?", "空港どこ？", false)
                    ),
                    isFinal = true
                )
            )
        )
    }

    private fun createRestaurantOrderScenario(): MockRolePlayScenario {
        return MockRolePlayScenario(
            id = "restaurant_order",
            titleJa = "レストラン注文",
            level = LearningLevel.BEGINNER,
            npcName = "店員",
            npcIcon = "🍽️",
            steps = listOf(
                MockRolePlayStep(
                    id = "r1",
                    aiLineVisayan = "Maayong adlaw. Unsa imong gusto kaonon?",
                    aiLineJa = "こんにちは。何を召し上がりますか？",
                    choices = listOf(
                        MockRolePlayChoice("Gusto ko ug manok.", "チキンが食べたい。", true),
                        MockRolePlayChoice("Asa ang airport?", "空港どこ？", false),
                        MockRolePlayChoice("Dili ko ganahan mokaon.", "食べたくない。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "r2",
                    aiLineVisayan = "Okay, usa ka manok meal. Unsa imong gusto imnon?",
                    aiLineJa = "わかりました。飲み物は何にしますか？",
                    choices = listOf(
                        MockRolePlayChoice("Tubig lang.", "水だけで。", true),
                        MockRolePlayChoice("Asa ang hotel?", "ホテルどこ？", false),
                        MockRolePlayChoice("Dili ko ganahan.", "嫌だ。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "r3",
                    aiLineVisayan = "Ayos. Tubig ug manok.",
                    aiLineJa = "了解です。水とチキンですね。",
                    choices = listOf(
                        MockRolePlayChoice("Tagpila tanan?", "全部でいくら？", true),
                        MockRolePlayChoice("Asa ang hospital?", "病院どこ？", false),
                        MockRolePlayChoice("Dili ko gusto.", "嫌だ。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "r4",
                    aiLineVisayan = "Php 180 tanan.",
                    aiLineJa = "全部で180ペソです。",
                    choices = listOf(
                        MockRolePlayChoice("Sige, mao ni.", "はい、どうぞ。", true),
                        MockRolePlayChoice("Libre?", "無料？", false),
                        MockRolePlayChoice("Dili ko ganahan.", "嫌だ。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "r5",
                    aiLineVisayan = "Salamat. Ihatod nako sa imong lamesa.",
                    aiLineJa = "ありがとう。料理は席にお持ちします。",
                    choices = listOf(
                        MockRolePlayChoice("Salamat!", "ありがとう！", true),
                        MockRolePlayChoice("Asa ang CR?", "トイレどこ？", false),
                        MockRolePlayChoice("Dili ko mahadlok.", "怖くない。", false)
                    )
                ),
                MockRolePlayStep(
                    id = "r6",
                    aiLineVisayan = "Maayo ang imong pagkaon!",
                    aiLineJa = "お食事をお楽しみください！",
                    choices = listOf(
                        MockRolePlayChoice("Salamat kaayo!", "ありがとうございます！", true),
                        MockRolePlayChoice("Dili ko gusto.", "嫌だ。", false),
                        MockRolePlayChoice("Asa ang airport?", "空港どこ？", false)
                    ),
                    isFinal = true
                )
            )
        )
    }
}
