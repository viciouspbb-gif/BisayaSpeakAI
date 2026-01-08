package com.bisayaspeak.ai.voice

import android.content.Context

/**
 * Placeholder implementation for the Gemini-driven voice service.
 * The heavy lifting (Gemini API calls, MediaPlayer management, caching, etc.)
 * will be added incrementally to keep the app in a runnable state.
 */
class GeminiVoiceService(
    private val context: Context
) {

    /**
     * Request Gemini audio for the provided text and play it.
     * For now this is a no-op and simply invokes the callback.
     */
    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        // TODO: Wire up Gemini audio generation + MediaPlayer playback.
        onStart?.invoke()
        onComplete?.invoke()
    }

    /**
     * Stop any ongoing playback. No-op until MediaPlayer wiring is added.
     */
    fun stop() {
        // TODO: Stop MediaPlayer when implemented.
    }
}
