package com.bisayaspeak.ai.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

enum class GeminiVoiceCue {
    DEFAULT,
    WHISPER,
    HIGH_PITCH
}

class GeminiVoiceService(private val context: Context) {
    private val tts: TextToSpeech by lazy {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("ceb", "PH"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US)
                }
                ready = true
            } else {
                Log.e("GeminiVoiceService", "Failed to initialize TextToSpeech: status=$status")
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { currentOnStart?.invoke() }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post { currentOnComplete?.invoke() }
                }

                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        currentOnError?.invoke(IllegalStateException("Gemini TTS playback error"))
                    }
                }
            })
        }
    }

    @Volatile
    private var ready: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentOnStart: (() -> Unit)? = null
    private var currentOnComplete: (() -> Unit)? = null
    private var currentOnError: ((Throwable) -> Unit)? = null

    fun speak(
        text: String,
        cue: GeminiVoiceCue = GeminiVoiceCue.DEFAULT,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (!ready) {
            onError?.invoke(IllegalStateException("GeminiVoiceService not ready"))
            return
        }

        currentOnComplete = onComplete
        currentOnError = onError
        currentOnStart = onStart

        val params = cue.voiceParams()
        tts.setPitch(params.pitch)
        tts.setSpeechRate(params.rate)

        val bundle = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, params.volume)
        }

        val utteranceId = "gemini-${System.currentTimeMillis()}"
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            onError?.invoke(IllegalStateException("Failed to start Gemini voice playback ($result)"))
        }
    }

    fun stop() {
        tts.stop()
        currentOnStart = null
        currentOnComplete = null
        currentOnError = null
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun GeminiVoiceCue.voiceParams(): VoiceParams {
        return when (this) {
            GeminiVoiceCue.DEFAULT -> VoiceParams(pitch = 1.0f, rate = 1.0f, volume = 1.0f)
            GeminiVoiceCue.WHISPER -> VoiceParams(pitch = 0.85f, rate = 0.9f, volume = 0.6f)
            GeminiVoiceCue.HIGH_PITCH -> VoiceParams(pitch = 1.25f, rate = 1.05f, volume = 1.0f)
        }
    }

    private data class VoiceParams(
        val pitch: Float,
        val rate: Float,
        val volume: Float
    )
}
