package com.bisayaspeak.ai

import android.content.Context

object LessonStatusManager {

    private const val PREF_NAME = "lesson_status_pref"
    private const val KEY_CLEARED_PREFIX = "lesson_cleared_"
    private const val KEY_AD_UNLOCKED_PREFIX = "lesson_ad_unlocked_"

    enum class Status {
        LOCKED,
        NEED_AD,
        OPEN,
        CLEARED
    }

    fun getLessonStatus(context: Context, level: Int, isPro: Boolean): Status {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (level == 1) {
            return if (isCleared(context, 1)) Status.CLEARED else Status.OPEN
        }

        if (!isCleared(context, level - 1)) {
            return Status.LOCKED
        }

        if (isCleared(context, level)) {
            return Status.CLEARED
        }

        if (level >= 11 && !isPro) {
            val isUnlockedByAd = prefs.getBoolean(KEY_AD_UNLOCKED_PREFIX + level, false)
            if (!isUnlockedByAd) {
                return Status.NEED_AD
            }
        }

        return Status.OPEN
    }

    fun setLessonCleared(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CLEARED_PREFIX + level, true).apply()
    }

    fun setLessonUnlockedByAd(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AD_UNLOCKED_PREFIX + level, true).apply()
    }

    private fun isCleared(context: Context, level: Int): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CLEARED_PREFIX + level, false)
    }
}
