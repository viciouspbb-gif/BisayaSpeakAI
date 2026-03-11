package com.bisayaspeak.ai.data.remote

import android.util.Log
import com.bisayaspeak.ai.data.local.Question
import com.bisayaspeak.ai.data.local.QuestionSeedParser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class CloudQuestionDownloader {

    private val storage = Firebase.storage

    suspend fun fetchQuestionsForLevels(levels: Set<Int>): List<Question> {
        if (levels.isEmpty()) return emptyList()
        val ranges = levels.map { determineRange(it) }.distinct()
        val result = mutableListOf<Question>()
        for (range in ranges) {
            result += downloadRange(range)
        }
        return result.filter { it.level in levels }
    }

    private fun determineRange(level: Int): IntRange {
        val start = ((level - 1) / RANGE_SIZE) * RANGE_SIZE + 1
        val end = start + RANGE_SIZE - 1
        return start..end
    }

    private suspend fun downloadRange(range: IntRange): List<Question> {
        val fileName = "levels_${range.first}_${range.last}.json"
        val path = "$CONTENT_FOLDER/$fileName"
        return runCatching {
            val bytes = storage.reference.child(path).getBytes(MAX_DOWNLOAD_BYTES).await()
            val jsonText = bytes.toString(Charsets.UTF_8)
            QuestionSeedParser.parse(jsonText)
        }.onSuccess {
            Log.d(TAG, "Downloaded ${it.size} questions from $path")
        }.onFailure {
            Log.e(TAG, "Failed to download $path", it)
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "CloudQuestionDownloader"
        private const val CONTENT_FOLDER = "content_updates"
        private const val RANGE_SIZE = 5
        private const val MAX_DOWNLOAD_BYTES = 5L * 1024L * 1024L // 5MB
    }
}
