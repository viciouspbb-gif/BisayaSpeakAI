package com.bisayaspeak.ai.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * アセット内のJSONからRoomに初期データを流し込むユーティリティ。
 * DBが空の時のみ動作する。
 */
object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"
    private const val SEED_FILE = "content/listening_seed_v2.json"

    private val gson = Gson()

    suspend fun initialize(
        context: Context,
        questionDao: QuestionDao,
        userProgressDao: UserProgressDao
    ) {
        // Temporary hard reset to ensure latest seed data is loaded (remove after migration)
        runCatching {
            Log.w(TAG, "Force-clearing questions table before seeding (temporary)")
            questionDao.clearAll()
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to force-clear database", throwable)
            return
        }

        val currentCount = runCatching { questionDao.countQuestions() }.getOrElse { throwable ->
            Log.e(TAG, "Failed to read question count", throwable)
            return
        }
        
        Log.d(TAG, "Current DB question count: $currentCount")
        
        // 強制リロードチェック - LV1データがなければ必ず再初期化
        val lv1Questions = runCatching { questionDao.getQuestionsByLevel(1) }.getOrElse { emptyList() }
        val needsForceReload = lv1Questions.isEmpty()
        
        if (needsForceReload) {
            Log.w(TAG, "LV1 questions missing or empty! Forcing complete database reload.")
            runCatching { questionDao.clearAll() }.onFailure { throwable ->
                Log.e(TAG, "Failed to clear database for force reload", throwable)
                return
            }
            val countAfterClear = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
            Log.d(TAG, "Questions count after force clear: $countAfterClear")
        }
        
        // 最終的なデータベース状態を再確認
        val finalCount = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
        val finalLv1Count = runCatching { questionDao.getQuestionsByLevel(1) }.getOrElse { emptyList() }.size
        
        Log.d(TAG, "Final DB state - Total: $finalCount, LV1: $finalLv1Count")
        
        val needsRefresh = if (needsForceReload) {
            true
        } else if (finalCount == 0) {
            Log.d(TAG, "Database is empty, needs seeding")
            true
        } else if (finalLv1Count == 0) {
            Log.w(TAG, "Database has data but no LV1 questions, forcing refresh")
            runCatching { questionDao.clearAll() }.onFailure { throwable ->
                Log.e(TAG, "Failed to clear for LV1 refresh", throwable)
                return
            }
            true
        } else {
            val containsLegacy = runCatching { questionDao.containsKeyword("bisaya") }.getOrElse { throwable ->
                Log.e(TAG, "Failed to inspect legacy data", throwable)
                false
            }
            if (containsLegacy) {
                Log.w(TAG, "Legacy placeholder data detected. Clearing questions table.")
                runCatching { questionDao.clearAll() }.onFailure { throwable ->
                    Log.e(TAG, "Failed to clear legacy questions", throwable)
                    return
                }
                // クリア後の確認
                val countAfterClear = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
                Log.d(TAG, "Questions count after clear: $countAfterClear")
                true
            } else {
                Log.d(TAG, "Database already seeded with valid data (entries=$finalCount)")
                false
            }
        }
        
        if (!needsRefresh) {
            Log.d(TAG, "No refresh needed, database is properly initialized")
            return
        }

        runCatching {
            val questions = loadQuestionsFromAssets(context)
            if (questions.isEmpty()) {
                Log.w(TAG, "Seed file has no content: $SEED_FILE")
                return
            }
            
            // 必ずクリアして重複を完全防止
            val preClearCount = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
            if (preClearCount > 0) {
                Log.w(TAG, "Clearing existing database (${preClearCount} entries) to prevent duplicates")
                runCatching { questionDao.clearAll() }.onFailure { throwable ->
                    Log.e(TAG, "Failed to clear database", throwable)
                    return
                }
                val countAfterClear = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
                Log.d(TAG, "Database cleared, current count: $countAfterClear")
            }
            
            Log.d(TAG, "Starting insertion of ${questions.size} questions")
            questionDao.insertQuestions(questions)
            
            // 挿入後の確認
            val insertedCount = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
            Log.d(TAG, "Successfully inserted ${questions.size} questions from $SEED_FILE")
            Log.d(TAG, "Total questions in DB after insertion: $insertedCount")
            
            // 重複チェック - 厳密な検証
            if (insertedCount != questions.size) {
                Log.e(TAG, "CRITICAL: Database count mismatch! Expected: ${questions.size}, Actual: $insertedCount")
                if (insertedCount > questions.size) {
                    Log.e(TAG, "Duplicates detected! Clearing and re-inserting...")
                    runCatching { questionDao.clearAll() }.onFailure { throwable ->
                        Log.e(TAG, "Failed to clear duplicates", throwable)
                        return
                    }
                    questionDao.insertQuestions(questions)
                    val finalCount = runCatching { questionDao.countQuestions() }.getOrElse { 0 }
                    Log.d(TAG, "Final count after re-insertion: $finalCount")
                }
            }
            
            // LV1のデータ確認
            val lv1QuestionsAfter = runCatching { questionDao.getQuestionsByLevel(1) }.getOrElse { emptyList() }
            Log.d(TAG, "LV1 questions available: ${lv1QuestionsAfter.size}")
            if (lv1QuestionsAfter.isNotEmpty()) {
                Log.d(TAG, "First LV1 question: ${lv1QuestionsAfter.first().sentence}")
                // LV1の重複チェック
                val lv1Distinct = lv1QuestionsAfter.distinctBy { it.sentence }
                if (lv1Distinct.size != lv1QuestionsAfter.size) {
                    Log.e(TAG, "LV1 has duplicates! Total: ${lv1QuestionsAfter.size}, Distinct: ${lv1Distinct.size}")
                }
            } else {
                Log.e(TAG, "CRITICAL: No LV1 questions found after insertion!")
            }

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

                    val language = Locale.getDefault().language.lowercase(Locale.US)
                    val meaningKey = if (language == "ja") "ja" else "en"
                    
                    val questions = seedItems.map { seedDto ->
                        val rawJa = seedDto.translations?.get("ja")?.meaning.orEmpty()
                        val rawEn = seedDto.translations?.get("en")?.meaning.orEmpty()
                        val fallbackMeaning = seedDto.native
                        val meaningJa = rawJa.ifBlank { rawEn.ifBlank { fallbackMeaning } }
                        val meaningEn = rawEn.ifBlank { rawJa.ifBlank { fallbackMeaning } }

                        Question(
                            sentence = seedDto.native,
                            meaningJa = meaningJa,
                            meaningEn = meaningEn,
                            level = seedDto.level,
                            type = "LISTENING"
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
