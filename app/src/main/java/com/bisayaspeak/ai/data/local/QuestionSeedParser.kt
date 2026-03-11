package com.bisayaspeak.ai.data.local

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object QuestionSeedParser {
    private const val TAG = "QuestionSeedParser"
    private val gson = Gson()
    private val listType = object : TypeToken<List<QuestionSeedDto>>() {}.type

    fun parse(jsonText: String): List<Question> {
        return try {
            val seedItems: List<QuestionSeedDto> = gson.fromJson(jsonText, listType) ?: emptyList()
            Log.d(TAG, "Parsed ${seedItems.size} seed items from JSON")
            seedItems.map { seedDto ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse seed JSON", e)
            emptyList()
        }
    }

    private data class QuestionSeedDto(
        val id: Int,
        val level: Int,
        val native: String,
        val translations: Map<String, TranslationDto>?,
        val words: List<String>?
    )

    private data class TranslationDto(
        val meaning: String
    )
}
