package com.bisayaspeak.ai.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * フレーズ抽出器
 */
@Singleton
class PhraseExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    
    private var cachedPhrases: List<PhraseData>? = null
    
    /**
     * JSONからランダムなフレーズを抽出
     */
    suspend fun extractRandomPhrase(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val phrases = getCachedPhrases()
                if (phrases.isEmpty()) return@withContext null
                
                phrases.random().ceb
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * キャッシュされたフレーズデータを取得
     */
    private suspend fun getCachedPhrases(): List<PhraseData> {
        return cachedPhrases ?: loadPhrases().also { cachedPhrases = it }
    }
    
    /**
     * JSONからフレーズデータを読み込み
     */
    private suspend fun loadPhrases(): List<PhraseData> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("content/learning_content_v1.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                
                val jsonObject = json.decodeFromString<JsonObject>(jsonString)
                val phrasesArray = jsonObject["phrases"]?.jsonArray ?: return@withContext emptyList()
                
                phrasesArray.mapNotNull { element ->
                    val phraseObject = element as? JsonObject ?: return@mapNotNull null
                    PhraseData(
                        id = phraseObject["id"]?.jsonPrimitive?.content ?: "",
                        ceb = phraseObject["ceb"]?.jsonPrimitive?.content ?: "",
                        translations = (phraseObject["translations"] as? JsonObject)?.let { translations ->
                            Translations(
                                en = translations["en"]?.jsonPrimitive?.content ?: "",
                                ja = translations["ja"]?.jsonPrimitive?.content ?: ""
                            )
                        } ?: Translations("", "")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * フレーズの翻訳を取得
     */
    suspend fun getTranslations(phrase: String): Map<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val phrases = getCachedPhrases()
                val phraseData = phrases.find { it.ceb == phrase }
                phraseData?.translations?.let {
                    mapOf("en" to it.en, "ja" to it.ja)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * フレーズデータモデル
     */
    data class PhraseData(
        val id: String,
        val ceb: String,
        val translations: Translations
    )
    
    data class Translations(
        val en: String,
        val ja: String
    )
}
