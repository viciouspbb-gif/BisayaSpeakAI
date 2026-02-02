package com.bisayaspeak.ai.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * AudioRecordを使用したPCM16bit録音処理
 * 音声評価API用にWAV/PCM16bit/16kHz形式で録音
 */
class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    data class RecordingResult(
        val file: File?,
        val isSilent: Boolean,
        val duration: Long
    )

    /**
     * 録音を開始（5秒間録音）
     */
    suspend fun startRecording(outputFile: File): RecordingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        return@withContext try {
            // 端末依存のバッファサイズを取得し、2倍にして安定性を向上
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e("AudioRecorder", "Invalid buffer size: $minBufferSize")
                return@withContext RecordingResult(null, true, 0)
            }
            
            val bufferSize = minBufferSize * 2
            Log.d("AudioRecorder", "Buffer size: $bufferSize bytes (min: $minBufferSize)")

            val buffer = ByteArray(bufferSize)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord not initialized")
                return@withContext RecordingResult(null, true, 0)
            }

            audioRecord?.startRecording()
            isRecording = true

            Log.d("AudioRecorder", "Recording started: PCM16bit, 16kHz, mono")

            val fos = FileOutputStream(outputFile)
            var totalData = 0
            var silentCount = 0
            var soundCount = 0
            val maxDuration = 5000L // 5秒
            val silenceThreshold = 500 // 無音判定の閾値を上げて感度向上

            while (isRecording && (System.currentTimeMillis() - startTime) < maxDuration) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    fos.write(buffer, 0, read)
                    totalData += read

                    // 改善された無音判定：振幅の平均値を計算
                    var sumAmplitude = 0L
                    for (i in 0 until read step 2) {
                        if (i + 1 < read) {
                            val sample = (buffer[i].toInt() or (buffer[i + 1].toInt() shl 8)).toShort()
                            sumAmplitude += abs(sample.toInt())
                        }
                    }
                    val avgAmplitude = if (read > 0) sumAmplitude / (read / 2) else 0
                    
                    if (avgAmplitude < silenceThreshold) {
                        silentCount++
                    } else {
                        soundCount++
                    }
                }
            }

            isRecording = false
            fos.flush()
            fos.close()

            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null

            val duration = System.currentTimeMillis() - startTime
            // 音声データが十分にあり、音声フレームが一定数以上あれば有効と判定
            val isSilent = totalData < 5000 || soundCount < 5
            
            Log.d("AudioRecorder", "Recording completed: PCM size=$totalData bytes, duration=$duration ms, silent=$isSilent, soundFrames=$soundCount, silentFrames=$silentCount")

            RecordingResult(outputFile, isSilent, duration)

        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording failed", e)
            isRecording = false
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
            RecordingResult(null, true, System.currentTimeMillis() - startTime)
        }
    }

    /**
     * 録音を停止
     */
    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Stop failed", e)
        }
    }
}
