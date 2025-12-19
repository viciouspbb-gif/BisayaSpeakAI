package com.bisayaspeak.ai.data.dictionary

import com.bisayaspeak.ai.data.model.LearningLevel

data class BisayaPhrase(
    val bisaya: String,
    val japanese: String,
    val english: String,
    val level: LearningLevel
)

object BisayaDictionary {
    fun getPhrasesByLevel(level: LearningLevel): List<BisayaPhrase> {
        return allPhrases.filter { it.level == level }
    }

    private val allPhrases = listOf(
        BisayaPhrase(
            bisaya = "Maayong buntag",
            japanese = "おはようございます",
            english = "Good morning",
            level = LearningLevel.BEGINNER
        ),
        BisayaPhrase(
            bisaya = "Kumusta ka",
            japanese = "元気ですか",
            english = "How are you",
            level = LearningLevel.BEGINNER
        ),
        BisayaPhrase(
            bisaya = "Salamat",
            japanese = "ありがとう",
            english = "Thank you",
            level = LearningLevel.BEGINNER
        ),
        BisayaPhrase(
            bisaya = "Palihug",
            japanese = "お願いします",
            english = "Please",
            level = LearningLevel.BEGINNER
        ),
        BisayaPhrase(
            bisaya = "Pila ang presyo",
            japanese = "いくらですか",
            english = "How much is it",
            level = LearningLevel.INTERMEDIATE
        ),
        BisayaPhrase(
            bisaya = "Asa ang CR",
            japanese = "トイレはどこですか",
            english = "Where is the restroom",
            level = LearningLevel.INTERMEDIATE
        ),
        BisayaPhrase(
            bisaya = "Gusto kong mopalit",
            japanese = "買いたいです",
            english = "I want to buy",
            level = LearningLevel.ADVANCED
        )
    )
}
