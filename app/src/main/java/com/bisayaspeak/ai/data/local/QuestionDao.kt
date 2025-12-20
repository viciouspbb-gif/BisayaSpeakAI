package com.bisayaspeak.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE level = :level")
    suspend fun getQuestionsByLevel(level: Int): List<Question>

    @Query("SELECT * FROM questions WHERE level BETWEEN :minLevel AND :maxLevel")
    suspend fun getQuestionsInRange(minLevel: Int, maxLevel: Int): List<Question>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun countQuestions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)
}
