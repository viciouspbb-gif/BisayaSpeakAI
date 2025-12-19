package com.bisayaspeak.ai.data.repository.mock

import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.mock.MockQuizQuestion
import kotlin.random.Random

class MockQuizRepository {

    private val beginnerQuestions = listOf(
        MockQuizQuestion(
            id = "b1",
            level = LearningLevel.BEGINNER,
            questionJa = "「こんにちは」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'こんにちは' in Bisaya?",
            options = listOf("Kumusta", "Salamat", "Maayong gabii", "Adios"),
            correctIndex = 0,
            explanationJa = "挨拶として広く使える表現です。"
        ),
        MockQuizQuestion(
            id = "b2",
            level = LearningLevel.BEGINNER,
            questionJa = "「ありがとう」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'ありがとう' in Bisaya?",
            options = listOf("Palihug", "Salamat", "Maayo", "Oo"),
            correctIndex = 1,
            explanationJa = "感謝を伝える基本フレーズです。"
        ),
        MockQuizQuestion(
            id = "b3",
            level = LearningLevel.BEGINNER,
            questionJa = "「はい」を意味する単語はどれですか？",
            questionVisayan = "Which word means 'yes'?",
            options = listOf("Dili", "Oo", "Babay", "Tubig"),
            correctIndex = 1,
            explanationJa = "『Dili』は『いいえ』です。逆に覚えないよう注意。"
        ),
        MockQuizQuestion(
            id = "b4",
            level = LearningLevel.BEGINNER,
            questionJa = "「トイレはどこですか？」のビサヤ語はどれですか？",
            questionVisayan = "Which is 'Where is the bathroom?' in Bisaya?",
            options = listOf(
                "Asa ang banyo?",
                "Asa ka paingon?",
                "Asa ang beach?",
                "Asa ang jeepney?"
            ),
            correctIndex = 0,
            explanationJa = "旅行中によく使うフレーズです。"
        ),
        MockQuizQuestion(
            id = "b5",
            level = LearningLevel.BEGINNER,
            questionJa = "「おはようございます」を意味する表現はどれですか？",
            questionVisayan = "Which phrase means 'おはようございます'?",
            options = listOf("Maayong buntag", "Maayong hapon", "Maayong gabii", "Kumusta"),
            correctIndex = 0,
            explanationJa = "朝の挨拶として使います。"
        ),
        MockQuizQuestion(
            id = "b6",
            level = LearningLevel.BEGINNER,
            questionJa = "「いいえ」を意味する単語はどれですか？",
            questionVisayan = "Which word means 'no'?",
            options = listOf("Oo", "Dili", "Salamat", "Kumusta"),
            correctIndex = 1,
            explanationJa = "否定の返答に使います。"
        ),
        MockQuizQuestion(
            id = "b7",
            level = LearningLevel.BEGINNER,
            questionJa = "「水」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'water' in Bisaya?",
            options = listOf("Tubig", "Kape", "Gatas", "Juice"),
            correctIndex = 0,
            explanationJa = "レストランでよく使う単語です。"
        ),
        MockQuizQuestion(
            id = "b8",
            level = LearningLevel.BEGINNER,
            questionJa = "「さようなら」を意味する表現はどれですか？",
            questionVisayan = "Which phrase means 'goodbye'?",
            options = listOf("Kumusta", "Salamat", "Babay", "Palihug"),
            correctIndex = 2,
            explanationJa = "別れの挨拶です。"
        ),
        MockQuizQuestion(
            id = "b9",
            level = LearningLevel.BEGINNER,
            questionJa = "「お願いします」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'please' in Bisaya?",
            options = listOf("Salamat", "Palihug", "Kumusta", "Oo"),
            correctIndex = 1,
            explanationJa = "丁寧にお願いするときに使います。"
        ),
        MockQuizQuestion(
            id = "b10",
            level = LearningLevel.BEGINNER,
            questionJa = "「こんばんは」を意味する表現はどれですか？",
            questionVisayan = "Which phrase means 'good evening'?",
            options = listOf("Maayong buntag", "Maayong hapon", "Maayong gabii", "Babay"),
            correctIndex = 2,
            explanationJa = "夜の挨拶として使います。"
        ),
        MockQuizQuestion(
            id = "b11",
            level = LearningLevel.BEGINNER,
            questionJa = "「美味しい」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'delicious' in Bisaya?",
            options = listOf("Lami", "Maayo", "Nindot", "Dako"),
            correctIndex = 0,
            explanationJa = "食事の感想を伝えるときに使います。"
        ),
        MockQuizQuestion(
            id = "b12",
            level = LearningLevel.BEGINNER,
            questionJa = "「いくらですか？」を意味する表現はどれですか？",
            questionVisayan = "Which phrase means 'how much'?",
            options = listOf("Pila ni?", "Asa ni?", "Kinsa ni?", "Unsa ni?"),
            correctIndex = 0,
            explanationJa = "買い物で値段を聞くときに使います。"
        ),
        MockQuizQuestion(
            id = "b13",
            level = LearningLevel.BEGINNER,
            questionJa = "「ビーチはどこですか？」はどれですか？",
            questionVisayan = "Which is 'Where is the beach?' in Bisaya?",
            options = listOf("Asa ang beach?", "Asa ang banyo?", "Asa ang hotel?", "Asa ang airport?"),
            correctIndex = 0,
            explanationJa = "観光地を探すときに使います。"
        ),
        MockQuizQuestion(
            id = "b14",
            level = LearningLevel.BEGINNER,
            questionJa = "「ごめんなさい」をビサヤ語で何と言いますか？",
            questionVisayan = "How do you say 'sorry' in Bisaya?",
            options = listOf("Salamat", "Pasensya", "Kumusta", "Babay"),
            correctIndex = 1,
            explanationJa = "謝罪するときに使います。"
        ),
        MockQuizQuestion(
            id = "b15",
            level = LearningLevel.BEGINNER,
            questionJa = "「良い」を意味する単語はどれですか？",
            questionVisayan = "Which word means 'good'?",
            options = listOf("Dili", "Maayo", "Dako", "Gamay"),
            correctIndex = 1,
            explanationJa = "状態や品質を表現するときに使います。"
        )
    )

    private val intermediateQuestions = listOf(
        MockQuizQuestion(
            id = "i1",
            level = LearningLevel.INTERMEDIATE,
            questionJa = "市場で『割引できますか？』と言いたいときの表現は？",
            questionVisayan = "How do you ask for a discount at the market?",
            options = listOf(
                "Pwede ba nga tabangan nimo ko?",
                "Pwede ba ko magpa-reserve?",
                "Pwede ba nga discount?",
                "Pwede ko mangutana?"
            ),
            correctIndex = 2,
            explanationJa = "買い物で値引きをお願いするときの便利なフレーズです。"
        ),
        MockQuizQuestion(
            id = "i2",
            level = LearningLevel.INTERMEDIATE,
            questionJa = "『ジープニーはどこですか？』はどれですか？",
            questionVisayan = "Which is 'Where is the jeepney?' in Bisaya?",
            options = listOf(
                "Asa ang jeepney?",
                "Asa ang beach?",
                "Asa ang hospital?",
                "Asa ang imong kwarto?"
            ),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "i3",
            level = LearningLevel.INTERMEDIATE,
            questionJa = "レストランで『とても美味しい』と言いたいときの表現は？",
            questionVisayan = "How do you say 'very delicious' in Bisaya?",
            options = listOf("Lami kaayo", "Maayo kaayo", "Nindot kaayo", "Salamat kaayo"),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "i4",
            level = LearningLevel.INTERMEDIATE,
            questionJa = "『病院はどこですか？』はどれですか？",
            questionVisayan = "Which is 'Where is the hospital?' in Bisaya?",
            options = listOf(
                "Asa ang hospital?",
                "Asa ang beach?",
                "Asa ang banyo?",
                "Asa ang jeepney?"
            ),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "i5",
            level = LearningLevel.INTERMEDIATE,
            questionJa = "『助けて！』に最も近い表現はどれですか？",
            questionVisayan = "Which expression means 'Help!'?",
            options = listOf("Tabang!", "Salamat!", "Babay!", "Pasensya!"),
            correctIndex = 0
        )
    )

    private val advancedQuestions = listOf(
        MockQuizQuestion(
            id = "a1",
            level = LearningLevel.ADVANCED,
            questionJa = "『アヤラへの行き方は？』に最も近い表現は？",
            questionVisayan = "Which is closest to 'How do I get to Ayala?'",
            options = listOf(
                "Unsaon nako pag-adto sa Ayala?",
                "Adto ko sa Ayala.",
                "Asa ang Ayala?",
                "Pila ang pamasahe?"
            ),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "a2",
            level = LearningLevel.ADVANCED,
            questionJa = "『予約を修正してください』のビサヤ語はどれですか？",
            questionVisayan = "Which is 'Please fix my booking' in Bisaya?",
            options = listOf(
                "Palihug ayuha ang akong booking.",
                "Palihug ihatud ko sa Ayala.",
                "Palihug ipakita ang imong passport.",
                "Palihug tabangi ko."),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "a3",
            level = LearningLevel.ADVANCED,
            questionJa = "『すぐに医者が必要です』はどれですか？",
            questionVisayan = "Which sentence means 'I need a doctor immediately'?",
            options = listOf(
                "Kinahanglan nako og doktor dayon.",
                "Kinahanglan nako og kwarto dayon.",
                "Kinahanglan nako og jeepney dayon.",
                "Kinahanglan nako og tabang dayon."
            ),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "a4",
            level = LearningLevel.ADVANCED,
            questionJa = "『ここにいてくれて嬉しいです』はどれですか？",
            questionVisayan = "Which is 'I'm very happy you're here' in Bisaya?",
            options = listOf(
                "Nalipay kaayo ko nga naa ka dinhi.",
                "Nasuko ko tungod sa imong gibuhat.",
                "Nindot kaayo diri.",
                "Maayo kaayo ko karon."
            ),
            correctIndex = 0
        ),
        MockQuizQuestion(
            id = "a5",
            level = LearningLevel.ADVANCED,
            questionJa = "『あなたの計画に同意しません』はどれですか？",
            questionVisayan = "Which means 'I don't agree with your plan'?",
            options = listOf(
                "Dili ko mouyon sa imong plano.",
                "Mouyon ko sa imong plano.",
                "Nalipay ko sa imong plano.",
                "Wala koy plano."
            ),
            correctIndex = 0
        )
    )

    fun getQuestionsByLevel(level: LearningLevel): List<MockQuizQuestion> {
        return when (level) {
            LearningLevel.BEGINNER -> beginnerQuestions
            LearningLevel.INTERMEDIATE -> intermediateQuestions
            LearningLevel.ADVANCED -> advancedQuestions
        }
    }

    fun getLiteQuizSet(
        totalQuestions: Int = 10,
        hardCountRange: IntRange = 1..2
    ): List<MockQuizQuestion> {
        if (totalQuestions <= 0) return emptyList()

        val beginnerPool = beginnerQuestions.shuffled().toMutableList()
        val intermediatePool = intermediateQuestions.shuffled().toMutableList()
        val advancedPool = advancedQuestions.shuffled().toMutableList()

        val hardSource = when {
            advancedPool.isNotEmpty() -> advancedPool
            intermediatePool.isNotEmpty() -> intermediatePool
            else -> beginnerPool
        }

        val requestedHardCount = if (hardCountRange.first == hardCountRange.last) {
            hardCountRange.first
        } else {
            Random.nextInt(hardCountRange.first, hardCountRange.last + 1)
        }
        val hardCount = requestedHardCount.coerceAtLeast(1).coerceAtMost(hardSource.size.coerceAtLeast(1))
        val hardQuestions = hardSource.take(hardCount)
        hardSource.removeAll(hardQuestions)

        val remainingNeeded = (totalQuestions - hardQuestions.size).coerceAtLeast(0)
        val beginnerTarget = remainingNeeded.coerceAtLeast(0).coerceAtMost(4).coerceAtMost(beginnerPool.size)
        val beginnerSelection = beginnerPool.take(beginnerTarget)
        beginnerPool.removeAll(beginnerSelection)

        val remainingAfterBeginner = remainingNeeded - beginnerSelection.size
        val intermediateSelection = intermediatePool.take(remainingAfterBeginner.coerceAtMost(intermediatePool.size))
        intermediatePool.removeAll(intermediateSelection)

        val combined = mutableListOf<MockQuizQuestion>().apply {
            addAll(beginnerSelection)
            addAll(intermediateSelection)
            addAll(hardQuestions)
        }

        if (combined.size < totalQuestions) {
            val fallbackPool = (beginnerPool + intermediatePool + advancedPool).filter { it !in combined }
            combined += fallbackPool.shuffled().take(totalQuestions - combined.size)
        }

        return combined.take(totalQuestions).shuffled()
    }
}
