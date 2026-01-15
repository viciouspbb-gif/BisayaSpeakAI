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

enum class GeminiVoiceCue {
    DEFAULT,
    WHISPER,
    HIGH_PITCH
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
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val audioBytes = speechService.synthesizeSpeech(
                    OpenAiSpeechService.SpeechRequest(
                        input = text,
                        voice = "nova",
                        model = "tts-1",
                        format = "mp3",
                        speed = 0.9f
                    )
                ).use { it.bytes() }
                playAudio(audioBytes, onStart, onComplete, onError)
            } catch (e: Exception) {
                Log.e("GeminiVoiceService", "Failed to synthesize speech", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }
        }
    }

    private suspend fun playAudio(
        audioBytes: ByteArray,
        onStart: (() -> Unit)?,
        onComplete: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        withContext(Dispatchers.Main) {
            stopPlayer()
            if (audioBytes.isEmpty()) {
                onError?.invoke(IllegalStateException("Empty audio response"))
                return@withContext
            }

            val audioFile = File.createTempFile("nova_voice_", ".mp3", context.cacheDir).apply {
                outputStream().use { it.write(audioBytes) }
            }
            tempFile = audioFile

            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    onStart?.invoke()
                    it.start()
                }
                setOnCompletionListener {
                    onComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    val error = IllegalStateException("MediaPlayer error what=$what extra=$extra")
                    onError?.invoke(error)
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
