package com.bisayaspeak.ai.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * タリ道場専用の簡易SEプレイヤー。
 * PCMバッファを生成して AudioTrack で再生することで、
 * 外部ファイルに頼らず低音系の効果音を鳴らす。
 */
class DojoSePlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playCorrect() {
        playTone(frequency = 90.0, durationMs = 220, decay = 0.7)
    }

    fun playIncorrect() {
        playTone(frequency = 140.0, durationMs = 180, decay = 0.5)
    }

    private fun playTone(frequency: Double, durationMs: Int, decay: Double) {
        scope.launch {
            val sampleRate = 16_000
            val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val buffer = ShortArray(totalSamples)
            for (i in 0 until totalSamples) {
                val envelope = (1.0 - (i / totalSamples.toDouble())) * decay
                val value = sin(2.0 * PI * frequency * i / sampleRate) * envelope
                buffer[i] = (value * Short.MAX_VALUE).toInt().toShort()
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buffer.size * 2)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            delay(durationMs + 50L)
            track.release()
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
