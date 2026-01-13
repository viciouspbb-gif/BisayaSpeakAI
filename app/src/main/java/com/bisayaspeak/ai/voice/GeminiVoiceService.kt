package com.bisayaspeak.ai.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class GeminiVoiceCue {
    DEFAULT,
    WHISPER,
    HIGH_PITCH
}

class GeminiVoiceService(private val context: Context) {
    init {
        GeminiVoiceSupervisor.register(this)
        initializeTts()
    }

    private lateinit var tts: TextToSpeech

    private fun initializeTts() {
        if (::tts.isInitialized) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale("ceb", "PH"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US)
                }
                tts.setPitch(1.4f)
                tts.setSpeechRate(1.2f)
                ready = true
                Log.d("GeminiVoiceService", "TextToSpeech initialized successfully (locale=${tts.voice?.locale})")
                flushPendingQueue()
            } else {
                Log.e("GeminiVoiceService", "Failed to initialize TextToSpeech: status=$status")
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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

    @Volatile
    private var ready: Boolean = false
    private val pendingQueue = ConcurrentLinkedQueue<QueuedSpeech>()

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
        val speechRequest = QueuedSpeech(text, cue, onStart, onComplete, onError)
        if (!ready) {
            pendingQueue.offer(speechRequest)
            Log.d("GeminiVoiceService", "TTS not ready. Queued text length=${text.length}")
            return
        }

        playSpeech(speechRequest)
    }

    private fun playSpeech(request: QueuedSpeech) {
        currentOnComplete = request.onComplete
        currentOnError = request.onError
        currentOnStart = request.onStart

        val params = request.cue.voiceParams()
        tts.setPitch(params.pitch)
        tts.setSpeechRate(params.rate)

        val bundle = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, params.volume)
        }

        val utteranceId = "gemini-${System.currentTimeMillis()}"
        val result = tts.speak(request.text, TextToSpeech.QUEUE_FLUSH, bundle, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            request.onError?.invoke(IllegalStateException("Failed to start Gemini voice playback ($result)"))
        }
    }

    private fun flushPendingQueue() {
        while (pendingQueue.isNotEmpty()) {
            val request = pendingQueue.poll() ?: continue
            Log.d("GeminiVoiceService", "Flushing queued text length=${request.text.length}")
            playSpeech(request)
        }
    }

    fun stop() {
        Log.d("GeminiVoiceService", "Stopping current TTS playback")
        tts.stop()
        currentOnStart = null
        currentOnComplete = null
        currentOnError = null
    }

    fun shutdown() {
        Log.d("GeminiVoiceService", "Shutting down TextToSpeech engine")
        stop()
        tts.shutdown()
        GeminiVoiceSupervisor.unregister(this)
    }

    fun clearQueue() {
        pendingQueue.clear()
    }

    fun stopAndClearQueue() {
        clearQueue()
        stop()
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

    private data class QueuedSpeech(
        val text: String,
        val cue: GeminiVoiceCue,
        val onStart: (() -> Unit)?,
        val onComplete: (() -> Unit)?,
        val onError: ((Throwable) -> Unit)?
    )

    companion object {
        fun stopAllActive() {
            GeminiVoiceSupervisor.stopAndClearAll()
        }
    }
}

private object GeminiVoiceSupervisor {
    private val services = mutableSetOf<GeminiVoiceService>()

    @Synchronized
    fun register(service: GeminiVoiceService) {
        services.add(service)
    }

    @Synchronized
    fun unregister(service: GeminiVoiceService) {
        services.remove(service)
    }

    @Synchronized
    fun stopAndClearAll() {
        services.forEach { it.stopAndClearQueue() }
    }
}
