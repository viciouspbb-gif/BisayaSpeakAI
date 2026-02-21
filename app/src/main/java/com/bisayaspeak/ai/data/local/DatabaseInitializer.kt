package com.bisayaspeak.ai.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.bisayaspeak.ai.data.repository.DbSeedStateRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * アセット内のJSONからRoomに初期データを流し込むユーティリティ。
 * db_seededフラグがtrueになるまで一度だけ動作する。
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    private const val SEED_FILE = "content/listening_seed_v2.json"

    private val gson = Gson()
    private val seedMutex = Mutex()

    suspend fun initialize(
        context: Context,
        database: AppDatabase,
        seedStateRepository: DbSeedStateRepository = DbSeedStateRepository(context)
    ) {
        seedMutex.withLock {
            val isAlreadySeeded = seedStateRepository.seededFlow.first()
            Log.d(TAG, "initialize() start, db_seeded=$isAlreadySeeded")
            Log.d("DEBUG_SEED", "initialize() invoked, db_seeded=$isAlreadySeeded")
            val questionDao = database.questionDao()
            val userProgressDao = database.userProgressDao()

            if (isAlreadySeeded) {
                Log.d(TAG, "Skipping seeding – already completed")
                Log.d("DEBUG_SEED", "Skipping seeding because db_seeded already true")
                return
            }

            val existingCount = runCatching { questionDao.countQuestions() }.getOrElse { throwable ->
                Log.e(TAG, "Failed to count existing questions", throwable)
                Log.e("DEBUG_SEED", "Failed to count existing questions: ${throwable.message}", throwable)
                0
            }
            val existingLv1Count = runCatching { questionDao.getQuestionsByLevel(1).size }.getOrElse { 0 }

            if (existingCount > 0 || existingLv1Count > 0) {
                Log.d(TAG, "Existing questions detected (total=$existingCount, lv1=$existingLv1Count). Marking as seeded without reimport.")
                Log.d("DEBUG_SEED", "Existing DB detected, marking seeded and skipping clearAll")
                seedStateRepository.setDbSeeded(true)
                return
            }

            Log.d(TAG, "Starting initial database seed (db_seeded=false)")

            runCatching {
                Log.d(TAG, "Beginning seed transaction")
                Log.d("DEBUG_SEED", "Seed transaction start")
                database.withTransaction {
                    questionDao.clearAll()
                    val questions = loadQuestionsFromAssets(context)
                    if (questions.isEmpty()) {
                        error("Seed file has no content: $SEED_FILE")
                    }

                    Log.d(TAG, "Inserting ${questions.size} questions from $SEED_FILE")
                    Log.d("DEBUG_SEED", "Inserting ${questions.size} questions")
                    questionDao.insertQuestions(questions)
                    questions.forEachIndexed { index, question ->
                        Log.d("DEBUG_SEED", "Inserted question ${index + 1}/${questions.size}: level=${question.level}, sentence=${question.sentence}")
                    }
                    seedUserProgress(userProgressDao)
                }
                Log.d(TAG, "Seed transaction completed")
                Log.d("DEBUG_SEED", "Seed transaction completed")

                val lv1Count = questionDao.getQuestionsByLevel(1).size
                if (lv1Count == 0) {
                    error("Seeding failed – LV1 questions missing after insert")
                }

                seedStateRepository.setDbSeeded(true)
                Log.d(TAG, "Database seeding completed successfully (LV1 count=$lv1Count)")
                Log.d("DEBUG_SEED", "Database seeding finished successfully, LV1=$lv1Count")
            }.onFailure { throwable ->
                Log.e(TAG, "Database seeding failed", throwable)
                Log.e("DEBUG_SEED", "Database seeding failed: ${throwable.message}", throwable)
                seedStateRepository.setDbSeeded(false)
            }
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
        Log.d(TAG, "Loading questions from assets: $SEED_FILE")
        return try {
            context.assets.open(SEED_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // JSON構造を確認 - 実際のデータ構造に合わせる
                    val jsonText = reader.readText()
                    Log.d(TAG, "JSON file size: ${jsonText.length} characters")
                    
                    // 直接パースして構造を確認
                    val listType = object : TypeToken<List<QuestionSeedDto>>() {}.type
                    val seedItems: List<QuestionSeedDto> = gson.fromJson(jsonText, listType) ?: emptyList()
                    Log.d(TAG, "Parsed ${seedItems.size} seed items from JSON")

                    val questions = seedItems.map { seedDto ->
                        val rawTranslations = seedDto.translations
                            ?.mapValues { it.value.meaning.orEmpty() }
                            ?.filterValues { it.isNotBlank() }
                            .orEmpty()

                        val fallbackMeaning = seedDto.native
                        val meaningJa = rawTranslations["ja"].orEmpty()
                            .ifBlank { rawTranslations["en"].orEmpty() }
                            .ifBlank { fallbackMeaning }
                        val meaningEn = rawTranslations["en"].orEmpty()
                            .ifBlank { rawTranslations["ja"].orEmpty() }
                            .ifBlank { fallbackMeaning }

                        val translations = buildMap {
                            if (meaningJa.isNotBlank()) put("ja", meaningJa)
                            if (meaningEn.isNotBlank()) put("en", meaningEn)
                        }

                        Question(
                            sentence = seedDto.native,
                            meaningJa = meaningJa,
                            meaningEn = meaningEn,
                            level = seedDto.level,
                            type = "LISTENING",
                            translations = translations
                        )
                    }

                    questions.take(5).forEachIndexed { index, sample ->
                        Log.d(TAG, "Seed sample[${'$'}index]: ${'$'}{sample.sentence} / JA=${'$'}{sample.meaningJa} / EN=${'$'}{sample.meaningEn}")
                    }
                    
                    // レベルごとの件数をログ出力
                    val levelCounts = questions.groupBy { it.level }.mapValues { it.value.size }
                    Log.d(TAG, "Questions by level: $levelCounts")
                    Log.d(TAG, "Total questions to insert: ${questions.size}")
                    
                    questions
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load questions from assets", e)
            emptyList()
        }
    }

    private data class QuestionSeedDto(
        val id: Int,
        val level: Int,
        val native: String,
        val translations: Map<String, TranslationDto>?,
        val words: List<String>
    )

    private data class TranslationDto(
        val meaning: String
    )
}
