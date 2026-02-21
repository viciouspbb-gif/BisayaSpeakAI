package com.bisayaspeak.ai.data.model

/**
 * 翻訳元言語
 */
enum class SourceLang(val displayName: String, val apiCode: String) {
    JAPANESE("日本語", "ja"),
    ENGLISH("英語", "en");

    companion object {
        fun fromApiCode(code: String): SourceLang {
            return entries.find { it.apiCode == code } ?: JAPANESE
        }
    }
}
