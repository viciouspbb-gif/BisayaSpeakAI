package com.bisayaspeak.ai.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.remote.OpenAiSpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

enum class GeminiVoiceCue(val voiceName: String, val speed: Float) {
    DEFAULT("verse", 0.9f),
    TALK_LOW("verse", 0.82f),
    TALK_HIGH("verse", 0.96f),
    TRANSLATOR_SWIFT("verse", 1.04f)
}

class GeminiVoiceService(
    private val context: Context,
    private val speechService: OpenAiSpeechService = OpenAiSpeechService.create(BuildConfig.OPENAI_API_KEY)
) {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(supervisorJob + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null
    private var currentJob: Job? = null
    private var isSpeaking: Boolean = false
    private val pendingRequests = ArrayDeque<VoiceRequest>()

    init {
        GeminiVoiceSupervisor.register(this)
    }

    fun speak(
        text: String,
        cue: GeminiVoiceCue = GeminiVoiceCue.DEFAULT,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (text.isBlank()) return
        val request = VoiceRequest(text, cue, onStart, onComplete, onError)
        if (isSpeaking) {
            pendingRequests.addLast(request)
        } else {
            playRequest(request)
        }
    }

    private fun playRequest(request: VoiceRequest) {
        isSpeaking = true
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val audioBytes = speechService.synthesizeSpeech(
                    OpenAiSpeechService.SpeechRequest(
                        input = request.text,
                        voice = request.cue.voiceName,
                        model = "gpt-4o-mini-tts",
                        format = "mp3",
                        speed = request.cue.speed
                    )
                ).use { it.bytes() }
                playAudio(audioBytes, request)
            } catch (e: Exception) {
                Log.e("GeminiVoiceService", "Failed to synthesize speech", e)
                withContext(Dispatchers.Main) {
                    request.onError?.invoke(e)
                }
                dispatchNext()
            }
        }
    }

    private suspend fun playAudio(
        audioBytes: ByteArray,
        request: VoiceRequest
    ) {
        withContext(Dispatchers.Main) {
            stopPlayer()
            if (audioBytes.isEmpty()) {
                request.onError?.invoke(IllegalStateException("Empty audio response"))
                dispatchNext()
                return@withContext
            }

            val audioFile = File.createTempFile("nova_voice_", ".mp3", context.cacheDir).apply {
                outputStream().use { it.write(audioBytes) }
            }
            tempFile = audioFile

            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { player ->
                    mainHandler.post { request.onStart?.invoke() }
                    player.start()
                }
                setOnCompletionListener {
                    mainHandler.post { request.onComplete?.invoke() }
                    dispatchNext()
                }
                setOnErrorListener { _, what, extra ->
                    val error = IllegalStateException("MediaPlayer error what=$what extra=$extra")
                    mainHandler.post { request.onError?.invoke(error) }
                    dispatchNext()
                    true
                }
                prepareAsync()
            }

            mediaPlayer = player
        }
    }

    fun stop() {
        currentJob?.cancel()
        mainHandler.post { stopPlayer() }
        pendingRequests.clear()
        isSpeaking = false
    }

    private fun stopPlayer() {
        mediaPlayer?.let { player ->
            try {
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                player.stop()
            } catch (_: Exception) {
            } finally {
                try {
                    player.reset()
                } catch (_: Exception) {
                }
                player.release()
            }
        }
        mediaPlayer = null
        tempFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        tempFile = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
        GeminiVoiceSupervisor.unregister(this)
    }

    private fun dispatchNext() {
        val next = if (pendingRequests.isEmpty()) null else pendingRequests.removeFirst()
        if (next != null) {
            playRequest(next)
        } else {
            isSpeaking = false
        }
    }

    private data class VoiceRequest(
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
        services.forEach { it.stop() }
    }
}
