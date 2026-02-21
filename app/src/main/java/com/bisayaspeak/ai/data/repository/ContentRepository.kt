package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.LearningContent
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.util.LocaleUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.Locale

class ContentRepository(
    private val appContext: Context,
    private val gson: Gson = Gson()
) {

    fun loadLearningContentV1(): List<LearningContent> {
        val raw = readAssetText("content/learning_content_v1.json")
        val listType = object : TypeToken<List<LearningContentAssetItem>>() {}.type
        val items: List<LearningContentAssetItem> = gson.fromJson(raw, listType) ?: emptyList()
        return items.mapNotNull { it.toLearningContent() }
    }

    private fun readAssetText(path: String): String {
        appContext.assets.open(path).use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                return reader.readText()
            }
        }
    }
}

private data class LearningContentAssetItem(
    val id: String,
    val level: String,
    val category: String,
    val ceb: String,
    val translations: Map<String, LearningContentTranslation>?
) {

    fun toLearningContent(): LearningContent? {
        val learningLevel = when (level.lowercase(Locale.US)) {
            "beginner" -> LearningLevel.BEGINNER
            "intermediate" -> LearningLevel.INTERMEDIATE
            "advanced" -> LearningLevel.ADVANCED
            else -> return null
        }

        return LearningContent(
            id = id,
            bisaya = ceb,
            japanese = translations?.get("ja")?.meaning.orEmpty(),
            english = translations?.get("en")?.meaning.orEmpty(),
            category = category,
            level = learningLevel
        )
    }
}

private data class LearningContentTranslation(
    val meaning: String
)
