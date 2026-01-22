package com.bisayaspeak.ai.voice

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * タリ道場・空手チョップリスニング用の環境音プレイヤー。
 * 1. 擬似環境音をキャッシュ(WAV)へ生成
 * 2. ExoPlayer でループ再生
 * 3. 音量プリセットによる難易度調整
 */
class EnvironmentSoundPlayer(private val context: Context) {

    private var player: ExoPlayer? = null
    private var currentSoundscape: Soundscape? = null
    private var currentMedia: MediaItem? = null

    /**
     * 指定された環境音を生成＆再生。生成処理は IO スレッドで行う。
     */
    suspend fun play(
        soundscape: Soundscape,
        preset: EnvironmentVolumePreset = EnvironmentVolumePreset.STANDARD
    ) {
        val mediaItem = withContext(Dispatchers.IO) {
            val file = ensureSoundscapeFile(soundscape)
            MediaItem.fromUri(Uri.fromFile(file))
        }

        preparePlayerIfNeeded()
        if (currentMedia?.localConfiguration?.uri != mediaItem.localConfiguration?.uri) {
            player?.setMediaItem(mediaItem)
            currentMedia = mediaItem
        }
        player?.volume = preset.volume
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.prepare()
        player?.playWhenReady = true
        currentSoundscape = soundscape
    }

    fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.playWhenReady = true
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        currentSoundscape = null
        currentMedia = null
    }

    suspend fun fadeOutAndStop(durationMillis: Long = 800) {
        val targetPlayer = player ?: return
        val steps = 8
        val stepDuration = (durationMillis / steps).coerceAtLeast(20L)
        val initialVolume = targetPlayer.volume
        for (i in steps downTo 1) {
            val progress = (i - 1).toFloat() / steps
            targetPlayer.volume = initialVolume * progress
            delay(stepDuration)
        }
        stop()
    }

    fun release() {
        stop()
        player?.release()
        player = null
    }

    private suspend fun ensureSoundscapeFile(soundscape: Soundscape): File {
        val cacheFile = File(context.cacheDir, "tari_soundscape_${soundscape.name.lowercase()}.wav")
        if (cacheFile.exists()) return cacheFile
        writeSoundscape(cacheFile, soundscape)
        return cacheFile
    }

    @OptIn(UnstableApi::class)
    private fun preparePlayerIfNeeded() {
        if (player != null) return
        player = ExoPlayer.Builder(context).build().apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(attrs, true)
        }
    }

    private suspend fun writeSoundscape(target: File, soundscape: Soundscape) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val sampleRate = 16_000
        val totalSamples = sampleRate * soundscape.durationSeconds
        val pcm = ByteArray(totalSamples * 2)
        val random = Random(soundscape.seed)
        var index = 0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val value = when (soundscape) {
                Soundscape.JEEPNEY -> {
                    val engine = sin(2 * PI * 48 * t) * 0.32
                    val horn = if (i % (sampleRate * 3) < sampleRate / 2) sin(2 * PI * 280 * t) * 0.18 else 0.0
                    val chatter = random.nextDouble(-0.35, 0.35)
                    engine + horn + chatter
                }
                Soundscape.TROPICAL_STORM -> {
                    val rumble = sin(2 * PI * 16 * t) * 0.4
                    val rain = random.nextDouble(-0.55, 0.55)
                    val gust = sin(2 * PI * (12 + random.nextDouble(0.0, 4.0)) * t) * 0.25
                    rumble + rain + gust
                }
                Soundscape.NIGHT_MARKET -> {
                    val melody = sin(2 * PI * 220 * t) * 0.12
                    val chatter = random.nextDouble(-0.45, 0.45)
                    val cookware = sin(2 * PI * 520 * t) * 0.08
                    melody + chatter + cookware
                }
            }
            val clamped = value.coerceIn(-1.0, 1.0)
            val sample = (clamped * Short.MAX_VALUE).toInt()
            pcm[index++] = (sample and 0xFF).toByte()
            pcm[index++] = ((sample shr 8) and 0xFF).toByte()
        }

        DataOutputStream(BufferedOutputStream(FileOutputStream(target))).use { out ->
            writeWavHeader(out, pcm.size, sampleRate)
            out.write(pcm)
        }
    }

    private fun writeWavHeader(out: DataOutputStream, dataSize: Int, sampleRate: Int) {
        val byteRate = sampleRate * 2
        out.writeBytes("RIFF")
        out.writeInt(Integer.reverseBytes(36 + dataSize))
        out.writeBytes("WAVE")
        out.writeBytes("fmt ")
        out.writeInt(Integer.reverseBytes(16))
        out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM
        out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // Mono
        out.writeInt(Integer.reverseBytes(sampleRate))
        out.writeInt(Integer.reverseBytes(byteRate))
        out.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt())
        out.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
        out.writeBytes("data")
        out.writeInt(Integer.reverseBytes(dataSize))
    }
}

enum class Soundscape(val displayName: String, val durationSeconds: Int, val seed: Int) {
    JEEPNEY("市街", 14, 42),
    TROPICAL_STORM("豪雨", 16, 77),
    NIGHT_MARKET("夜市", 12, 19)
}

enum class EnvironmentVolumePreset(val label: String, val volume: Float) {
    STANDARD("修行", 0.65f),
    TRAINING("静寂", 0.5f),
    LOUD("修羅場", 0.9f)
}
