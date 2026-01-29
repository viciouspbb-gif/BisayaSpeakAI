package com.bisayaspeak.ai.data.local

object SeedDataProvider {
    private val cebuanoNumbers = mapOf(
        1 to "Usa",
        2 to "Duha",
        3 to "Tulo",
        4 to "Upat",
        5 to "Lima",
        6 to "Unom",
        7 to "Pito",
        8 to "Walo",
        9 to "Siyam",
        10 to "Napulo",
        11 to "Napulo'g Usa",
        12 to "Napulo'g Duha",
        13 to "Napulo'g Tulo",
        14 to "Napulo'g Upat",
        15 to "Napulo'g Lima",
        16 to "Napulo'g Unom",
        17 to "Napulo'g Pito",
        18 to "Napulo'g Walo",
        19 to "Napulo'g Siyam",
        20 to "Kaluhaan",
        21 to "Kaluhaan ug Usa",
        22 to "Kaluhaan ug Duha",
        23 to "Kaluhaan ug Tulo",
        24 to "Kaluhaan ug Upat",
        25 to "Kaluhaan ug Lima",
        26 to "Kaluhaan ug Unom",
        27 to "Kaluhaan ug Pito",
        28 to "Kaluhaan ug Walo",
        29 to "Kaluhaan ug Siyam",
        30 to "Katloan"
    )

    suspend fun seed(questionDao: QuestionDao) {
        if (questionDao.countQuestions() > 0) return
        val questions = buildBaseQuestions() + buildGeneratedQuestions()
        questionDao.insertQuestions(questions)
    }

    private fun buildBaseQuestions(): List<Question> {
        return listOf(
            // Level 1
            q("Maayong buntag", "おはようございます", 1, LISTENING),
            q("Maayong gabii", "こんばんは", 1, LISTENING),
            q("Kumusta ka", "お元気ですか？", 1, LISTENING),
            q("Ako si Ricky", "私はリッキーです", 1, LISTENING),
            q("Maayo man", "元気です。", 1, TRANSLATION),
            q("Salamat", "ありがとう。", 1, TRANSLATION),
            q("Oo", "はい。", 1, TRANSLATION),
            q("Dili ko", "私は違います / いいえ", 1, ORDERING),
            q("Ikaw si Ana", "あなたはアナです", 1, ORDERING),
            q("Kinsa ka", "あなたは誰ですか？", 1, ORDERING),

            // Level 2
            q("Unsa kini", "これは何ですか？", 2, LISTENING),
            q("Libro kini", "これは本です", 2, LISTENING),
            q("Kani akong balay", "これは私の家です", 2, LISTENING),
            q("Kana imong iro", "あれはあなたの犬です", 2, LISTENING),
            q("Unsa kana", "それは何？", 2, TRANSLATION),
            q("Tubig kini", "これは水です。", 2, TRANSLATION),
            q("Iring kana", "あれは猫です。", 2, TRANSLATION),
            q("Lami kini", "これは美味しい", 2, ORDERING),
            q("Dili kana ako", "あれは私のではありません", 2, ORDERING),
            q("Unsa to", "あれは何でしたか？", 2, ORDERING),

            // Level 3
            q("Mokaon ko", "私は食べます", 3, LISTENING),
            q("Moinom ko", "私は飲みます", 3, LISTENING),
            q("Molakaw ka", "あなたは行きますか？", 3, LISTENING),
            q("Matulog na ko", "もう寝ます", 3, LISTENING),
            q("Modagan ko", "私は走ります。", 3, TRANSLATION),
            q("Mokaon og kan-on", "ご飯を食べる。", 3, TRANSLATION),
            q("Moinom og tubig", "水を飲む。", 3, TRANSLATION),
            q("Mopalit ko", "私は買います", 3, ORDERING),
            q("Moambak siya", "彼はジャンプします", 3, ORDERING),
            q("Milingkod sila", "彼らは座りました", 3, ORDERING),

            // Level 4
            q("Maayo kini", "これは良いです", 4, LISTENING),
            q("Dako ang balay", "その家は大きいです", 4, LISTENING),
            q("Gamay ang iro", "その犬は小さいです", 4, LISTENING),
            q("Lami ang pagkaon", "その食事は美味しいです", 4, LISTENING),
            q("Init karon", "暑いです。", 4, TRANSLATION),
            q("Tugnaw", "寒いです。", 4, TRANSLATION),
            q("Guwapa ka", "あなたは美しい。", 4, TRANSLATION),
            q("Gwapo siya", "彼はハンサムです", 4, ORDERING),
            q("Layua uy", "遠いなあ", 4, ORDERING),
            q("Duol ra", "近いです", 4, ORDERING),

            // Level 5
            q("Asa ka", "どこにいるの？", 5, LISTENING),
            q("Naa ko diri", "私はここにいます", 5, LISTENING),
            q("Asa ang CR", "トイレはどこですか？", 5, LISTENING),
            q("Naa sa layo", "遠くにあります", 5, LISTENING),
            q("Asa ang balay", "家はどこ？", 5, TRANSLATION),
            q("Naa sa Cebu", "セブにいます。", 5, TRANSLATION),
            q("Naa sa eskwelahan", "学校にいます。", 5, TRANSLATION),
            q("Wala diri", "ここにはない", 5, ORDERING),
            q("Naa koy kwarta", "私はお金を持っています", 5, ORDERING),
            q("Asa man dapita", "どの辺りですか？", 5, ORDERING)
        )
    }

    private fun buildGeneratedQuestions(): List<Question> {
        val generated = mutableListOf<Question>()
        for (level in 6..30) {
            val levelWord = numberToCebuano(level)
            repeat(4) { index ->
                val sampleWord = numberToCebuano(index + 1)
                generated += q(
                    sentence = "Level $levelWord Listening $sampleWord",
                    meaningJa = "レベル$level リスニング サンプル${index + 1}",
                    level = level,
                    type = LISTENING
                )
            }
            repeat(3) { index ->
                val sampleWord = numberToCebuano(index + 1)
                generated += q(
                    sentence = "Level $levelWord Translation $sampleWord",
                    meaningJa = "レベル$level 翻訳 サンプル${index + 1}",
                    level = level,
                    type = TRANSLATION
                )
            }
            repeat(3) { index ->
                val sampleWord = numberToCebuano(index + 1)
                generated += q(
                    sentence = "Level $levelWord Ordering $sampleWord",
                    meaningJa = "レベル$level 並べ替え サンプル${index + 1}",
                    level = level,
                    type = ORDERING
                )
            }
        }
        return generated
    }

    private fun numberToCebuano(number: Int): String {
        return cebuanoNumbers[number] ?: number.toString()
    }

    private fun q(
        sentence: String,
        meaningJa: String,
        level: Int,
        type: String,
        meaningEn: String = meaningJa
    ) =
        Question(
            sentence = sentence,
            meaningJa = meaningJa,
            meaningEn = meaningEn,
            level = level,
            type = type
        )

    @Suppress("FunctionName")
    private fun q(sentence: String, meaning: String, level: Int, type: String): Question =
        q(
            sentence = sentence,
            meaningJa = meaning,
            level = level,
            type = type,
            meaningEn = meaning
        )

    private const val LISTENING = "LISTENING"
    private const val TRANSLATION = "TRANSLATION"
    private const val ORDERING = "ORDERING"
}
