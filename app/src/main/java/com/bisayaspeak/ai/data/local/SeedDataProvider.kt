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
        // Legacy seeding disabled – JSON v2 assets now provide canonical data.
        // This method intentionally does nothing to avoid inserting outdated dummy rows.
        if (questionDao.countQuestions() > 0) return
    }

    private fun buildBaseQuestions(): List<Question> = emptyList()

    private fun buildGeneratedQuestions(): List<Question> = emptyList()

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
