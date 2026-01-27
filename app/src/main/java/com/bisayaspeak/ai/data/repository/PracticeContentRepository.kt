package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.PracticeItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.Locale

class PracticeContentRepository(
    private val appContext: Context,
    private val gson: Gson = Gson()
) {

    fun loadPracticeItemsV1(locale: Locale = Locale.getDefault()): List<PracticeItem> {
        val raw = readAssetText("content/practice_items_v1.json")
        val listType = object : TypeToken<List<PracticeItemAssetItem>>() {}.type
        val items: List<PracticeItemAssetItem> = gson.fromJson(raw, listType) ?: emptyList()
        return items.map { it.toPracticeItem(locale) }
    }

    fun getItemsByCategory(
        category: String,
        locale: Locale = Locale.getDefault(),
        includePremium: Boolean = false
    ): List<PracticeItem> {
        val items = loadPracticeItemsV1(locale)
        return items.filter { item ->
            val categoryMatches = item.category.equals(category, ignoreCase = true)
            categoryMatches && (includePremium || !item.isPremium)
        }
    }

    fun getRandomQuestions(
        category: String,
        count: Int = 5,
        locale: Locale = Locale.getDefault()
    ): List<PracticeItem> {
        val items = getItemsByCategory(category, locale, includePremium = false)
        return if (items.size <= count) items.shuffled() else items.shuffled().take(count)
    }

    private fun readAssetText(path: String): String {
        appContext.assets.open(path).use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                return reader.readText()
            }
        }
    }
}

private data class PracticeItemAssetItem(
    val id: String,
    val category: Map<String, String>?,
    val ceb: String,
    val pronunciation: String?,
    val difficulty: Int?,
    val isPremium: Boolean?,
    val translations: Map<String, PracticeTranslation>?,
    val description: Map<String, String>?
) {

    fun toPracticeItem(locale: Locale): PracticeItem {
        val language = locale.language.lowercase(Locale.US)
        val englishMeaning = translations?.get("en")?.meaning.orEmpty()
        val japaneseMeaning = translations?.get("ja")?.meaning.orEmpty()

        val resolvedCategory = when (language) {
            "ja" -> category?.get("ja") ?: category?.get("en") ?: ""
            else -> category?.get("en") ?: category?.get("ja") ?: ""
        }

        val resolvedDescription = when (language) {
            "ja" -> description?.get("ja") ?: description?.get("en")
            else -> description?.get("en") ?: description?.get("ja")
        }

        val showJapanese = language == "ja"
        return PracticeItem(
            id = id,
            category = resolvedCategory,
            bisaya = ceb,
            japanese = if (showJapanese) japaneseMeaning else "",
            english = englishMeaning,
            pronunciation = pronunciation ?: "",
            difficulty = difficulty ?: 1,
            isPremium = isPremium ?: false,
            description = resolvedDescription
        )
    }
}

private data class PracticeTranslation(
    val meaning: String
)
