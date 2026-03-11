package com.bisayaspeak.ai.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.bisayaspeak.ai.data.remote.CloudQuestionDownloader
import com.bisayaspeak.ai.data.repository.DbSeedStateRepository
import com.bisayaspeak.ai.data.repository.LevelConfigRepository
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
    private const val DEFAULT_MAX_LEVEL = 35

    private val seedMutex = Mutex()
    private val cloudDownloader by lazy { CloudQuestionDownloader() }

    suspend fun initialize(
        context: Context,
        database: AppDatabase,
        seedStateRepository: DbSeedStateRepository = DbSeedStateRepository(context),
        levelConfigRepository: LevelConfigRepository = LevelConfigRepository(context)
    ) {
        seedMutex.withLock {
            val isAlreadySeeded = seedStateRepository.seededFlow.first()
            Log.d(TAG, "initialize() start, db_seeded=$isAlreadySeeded")
            Log.d("DEBUG_SEED", "initialize() invoked, db_seeded=$isAlreadySeeded")
            val questionDao = database.questionDao()
            val userProgressDao = database.userProgressDao()
            val requiredMaxLevel = runCatching { levelConfigRepository.getLatestMaxLevel() }
                .getOrElse { DEFAULT_MAX_LEVEL }
            val requiredLevels = 1..maxOf(DEFAULT_MAX_LEVEL, requiredMaxLevel)

            val existingCount = runCatching { questionDao.countQuestions() }.getOrElse { throwable ->
                Log.e(TAG, "Failed to count existing questions", throwable)
                Log.e("DEBUG_SEED", "Failed to count existing questions: ${'$'}{throwable.message}", throwable)
                0
            }
            val existingLv1Count = runCatching { questionDao.getQuestionsByLevel(1).size }.getOrElse { 0 }
            val hasExistingData = existingCount > 0 || existingLv1Count > 0

            if (isAlreadySeeded || hasExistingData) {
                val missingLevels = detectMissingLevels(questionDao, requiredLevels)
                if (missingLevels.isNotEmpty()) {
                    Log.d(TAG, "Detected missing levels ${'$'}{missingLevels.joinToString()} in existing DB. Supplementing from seed.")
                    Log.d("DEBUG_SEED", "Existing DB missing ${'$'}{missingLevels.size} levels -> reinserting from JSON")
                    supplementMissingLevels(context, questionDao, userProgressDao, missingLevels)
                    seedStateRepository.setDbSeeded(true)
                } else {
                    if (!isAlreadySeeded) {
                        seedStateRepository.setDbSeeded(true)
                    }
                    Log.d(TAG, "Database already seeded with all required levels. Skipping full seed.")
                }
                return
            }

            Log.d(TAG, "Starting initial database seed (db_seeded=false)")

            runCatching {
                Log.d(TAG, "Beginning seed transaction")
                Log.d("DEBUG_SEED", "Seed transaction start")
                val assetQuestions = loadQuestionsFromAssets(context)
                database.withTransaction {
                    questionDao.clearAll()
                    if (assetQuestions.isEmpty()) {
                        error("Seed file has no content: $SEED_FILE")
                    }

                    Log.d(TAG, "Inserting ${'$'}{assetQuestions.size} questions from $SEED_FILE")
                    Log.d("DEBUG_SEED", "Inserting ${'$'}{assetQuestions.size} questions")
                    Log.d("DEBUG_SEED", "Inserting ${'$'}{assetQuestions.size} questions from JSON...")
                    questionDao.insertQuestions(assetQuestions)
                    assetQuestions.forEachIndexed { index, question ->
                        Log.d("DEBUG_SEED", "Inserted question ${'$'}{index + 1}/${'$'}{assetQuestions.size}: level=${'$'}{question.level}, sentence=${'$'}{question.sentence}")
                    }
                    seedUserProgress(userProgressDao, requiredLevels)
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

                val missingLevelsAfterSeed = detectMissingLevels(questionDao, requiredLevels)
                if (missingLevelsAfterSeed.isNotEmpty()) {
                    Log.d(TAG, "Detected missing levels ${'$'}{missingLevelsAfterSeed.joinToString()} after seed. Supplementing from JSON.")
                    Log.d("DEBUG_SEED", "Seed missing ${'$'}{missingLevelsAfterSeed.size} levels -> reinserting from JSON")
                    supplementMissingLevels(context, questionDao, userProgressDao, missingLevelsAfterSeed)
                }
            }.onFailure { throwable ->
                Log.e("DEBUG_SEED", "SEED FAILED with: ${'$'}{throwable.message}", throwable)
                Log.e(TAG, "Database seeding failed", throwable)
                Log.e("DEBUG_SEED", "Database seeding failed: ${'$'}{throwable.message}", throwable)
                seedStateRepository.setDbSeeded(false)
            }
        }
    }

    private suspend fun seedUserProgress(
        userProgressDao: UserProgressDao,
        requiredLevels: IntRange
    ) {
        runCatching {
            requiredLevels.forEach { level ->
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

    private suspend fun detectMissingLevels(
        questionDao: QuestionDao,
        requiredLevels: IntRange
    ): List<Int> {
        val missing = mutableListOf<Int>()
        requiredLevels.forEach { level ->
            val levelCount = runCatching { questionDao.getQuestionsByLevel(level).size }
                .getOrElse {
                    Log.e(TAG, "Failed to fetch questions for level $level", it)
                    0
                }
            if (levelCount == 0) {
                missing.add(level)
            }
        }
        return missing
    }

    private suspend fun supplementMissingLevels(
        context: Context,
        questionDao: QuestionDao,
        userProgressDao: UserProgressDao,
        missingLevels: List<Int>
    ) {
        if (missingLevels.isEmpty()) return

        val insertedLevels = mutableSetOf<Int>()
        val assetQuestions = loadQuestionsFromAssets(context)
        val assetLevelSet = assetQuestions.map { it.level }.toSet()
        val assetTargetLevels = missingLevels.filter { it in assetLevelSet }
        if (assetTargetLevels.isNotEmpty()) {
            val toInsert = assetQuestions.filter { it.level in assetTargetLevels.toSet() }
            Log.d(TAG, "Supplementing ${'$'}{toInsert.size} questions from assets for levels ${'$'}{assetTargetLevels.joinToString()}")
            questionDao.insertQuestions(toInsert)
            insertedLevels.addAll(assetTargetLevels)
        }

        val remainingLevels = missingLevels.filterNot { it in assetLevelSet }.toSet()
        if (remainingLevels.isNotEmpty()) {
            val cloudQuestions = downloadQuestionsFromCloud(remainingLevels)
            if (cloudQuestions.isNotEmpty()) {
                Log.d(TAG, "Supplementing ${cloudQuestions.size} questions from cloud for levels ${remainingLevels.joinToString()}")
                questionDao.insertQuestions(cloudQuestions)
                insertedLevels.addAll(cloudQuestions.map { it.level })
            } else {
                Log.w(TAG, "No cloud questions found for levels: ${remainingLevels.joinToString()}")
            }
        }

        insertedLevels.forEach { level ->
            userProgressDao.upsert(
                UserProgress(
                    level = level,
                    stars = 0,
                    isUnlocked = level == 1
                )
            )
        }
    }

    private suspend fun downloadQuestionsFromCloud(levels: Set<Int>): List<Question> {
        return runCatching { cloudDownloader.fetchQuestionsForLevels(levels) }
            .onFailure { Log.e(TAG, "Failed to fetch questions from cloud", it) }
            .getOrDefault(emptyList())
    }

    private fun loadQuestionsFromAssets(context: Context): List<Question> {
        Log.d(TAG, "Loading questions from assets: $SEED_FILE")
        return try {
            context.assets.open(SEED_FILE).use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val jsonText = reader.readText()
                    QuestionSeedParser.parse(jsonText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load questions from assets", e)
            emptyList()
        }
    }
}
