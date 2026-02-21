package com.bisayaspeak.ai.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    throw IllegalStateException("Context is not an Activity: $this")
}

fun Context.findActivityOrNull(): Activity? = try {
    findActivity()
} catch (_: IllegalStateException) {
    null
}
