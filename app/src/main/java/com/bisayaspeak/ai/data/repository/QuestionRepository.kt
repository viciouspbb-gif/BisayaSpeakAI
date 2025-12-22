package com.bisayaspeak.ai.data.repository

import com.bisayaspeak.ai.data.local.Question
import com.bisayaspeak.ai.data.local.QuestionDao

/**
 * Provides access to question data with simple helpers for level filtering.
 */
class QuestionRepository(
    private val questionDao: QuestionDao
) {
    suspend fun getQuestionsByLevel(level: Int): List<Question> =
        questionDao.getQuestionsByLevel(level)

    suspend fun getQuestionsInRange(minLevel: Int, maxLevel: Int): List<Question> =
        questionDao.getQuestionsInRange(minLevel, maxLevel)

    suspend fun getAllQuestions(): List<Question> =
        questionDao.getQuestionsInRange(1, Int.MAX_VALUE)

    suspend fun insertQuestions(questions: List<Question>) {
        questionDao.insertQuestions(questions)
    }

    suspend fun getQuestionCount(): Int = questionDao.countQuestions()
}
