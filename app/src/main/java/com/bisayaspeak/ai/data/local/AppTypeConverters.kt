package com.bisayaspeak.ai.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AppTypeConverters {
    private val gson = Gson()
    private val translationsType = object : TypeToken<Map<String, String>>() {}.type

    @TypeConverter
    @JvmStatic
    fun fromTranslations(value: Map<String, String>?): String? {
        if (value.isNullOrEmpty()) return null
        return gson.toJson(value)
    }

    @TypeConverter
    @JvmStatic
    fun toTranslations(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) return emptyMap()
        return runCatching { gson.fromJson<Map<String, String>>(json, translationsType) }
            .getOrElse { emptyMap() }
    }
}
