package com.bisayaspeak.ai.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleUtils {

    private const val TAG = "LocaleUtils"
    private val _localeState = MutableStateFlow(resolveSystemLocale(null))
    val localeState: StateFlow<Locale> = _localeState.asStateFlow()

    fun currentLocale(): Locale = _localeState.value

    fun resolveAppLocale(context: Context? = null): Locale {
        val resolved = resolveSystemLocale(context)
        updateStateIfNeeded(resolved)
        return _localeState.value
    }

    fun refreshLocale(context: Context? = null) {
        val resolved = resolveSystemLocale(context)
        updateStateIfNeeded(resolved)
    }

    fun setLocale(locale: Locale) {
        updateStateIfNeeded(locale)
    }

    fun isJapanese(context: Context? = null): Boolean {
        return resolveAppLocale(context).language.equals("ja", ignoreCase = true)
    }

    private fun resolveSystemLocale(context: Context?): Locale {
        context?.resources?.configuration?.locales
            ?.takeIf { !it.isEmpty }
            ?.get(0)
            ?.let { return it }

        return Locale.getDefault()
    }

    private fun updateStateIfNeeded(newLocale: Locale) {
        val previous = _localeState.value
        if (previous != newLocale) {
            Log.d(TAG, "Locale updated: ${'$'}previous -> ${'$'}newLocale")
            _localeState.value = newLocale
        } else {
            Log.d(TAG, "Locale unchanged: ${'$'}newLocale")
        }
    }
}
