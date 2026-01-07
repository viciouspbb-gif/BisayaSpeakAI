package com.bisayaspeak.ai

import android.content.Context

object GameDataManager {
    private const val PREF_NAME = "game_data_pref"
    private const val KEY_HINT_COUNT = "hint_count"
    private const val MAX_HINTS = 3

    fun getHintCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HINT_COUNT, MAX_HINTS)
    }

    fun useHint(context: Context): Int {
        val current = getHintCount(context)
        if (current > 0) {
            val newCount = current - 1
            saveHintCount(context, newCount)
            return newCount
        }
        return 0
    }

    fun recoverHints(context: Context) {
        saveHintCount(context, MAX_HINTS)
    }

    private fun saveHintCount(context: Context, count: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_HINT_COUNT, count).apply()
    }
}
