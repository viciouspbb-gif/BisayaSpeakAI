package com.bisayaspeak.ai.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * アセット内のJSONからRoomに初期データを流し込むユーティリティ。
 * DBが空の時のみ動作する。
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    private const val SEED_FILE = "listening_seed.json"

    private val gson = Gson()

    suspend fun initialize(
        context: Context,
        questionDao: QuestionDao,
        userProgressDao: UserProgressDao
    ) {
        val currentCount = runCatching { questionDao.countQuestions() }.getOrElse { throwable ->
            Log.e(TAG, "Failed to read question count", throwable)
            return
        }
        if (currentCount > 0) {
            Log.d(TAG, "Database already seeded (entries=$currentCount)")
            return
        }

        runCatching {
            val questions = loadQuestionsFromAssets(context)
            if (questions.isEmpty()) {
                Log.w(TAG, "Seed file has no content: $SEED_FILE")
                return
            }
            questionDao.insertQuestions(questions)
            Log.d(TAG, "Inserted ${questions.size} questions from $SEED_FILE")

            seedUserProgress(userProgressDao)
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to import questions from assets", throwable)
        }
    }

    private suspend fun seedUserProgress(userProgressDao: UserProgressDao) {
        runCatching {
            // Lv1のみ解放、他はロック
            (1..30).forEach { level ->
                userProgressDao.upsert(
                    UserProgress(
                        level = level,
                        stars = 0,
                        isUnlocked = level == 1
                    )
                )
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to seed user progress", throwable)
        }
    }

    private fun loadQuestionsFromAssets(context: Context): List<Question> {
        context.assets.open(SEED_FILE).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val listType = object : TypeToken<List<LevelSeedDto>>() {}.type
                val seedItems: List<LevelSeedDto> = gson.fromJson(reader, listType) ?: emptyList()
                return seedItems.flatMap { levelDto ->
                    levelDto.questions.map { questionDto ->
                        Question(
                            sentence = questionDto.cebuano,
                            meaning = questionDto.japanese,
                            level = levelDto.level,
                            type = "LISTENING"
                        )
                    }
                }
            }
        }
    }

    private data class LevelSeedDto(
        val level: Int,
        val questions: List<QuestionSeedDto>
    )

    private data class QuestionSeedDto(
        val cebuano: String,
        val japanese: String,
        val audio: String?
    )
}
