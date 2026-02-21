package com.bisayaspeak.ai.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV形式で録音するユーティリティクラス
 */
class WavRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var amplitudeListener: ((Float) -> Unit)? = null
    
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER

    /**
     * 音量レベル更新用のリスナーを設定
     * level は 0.0〜1.0 の正規化されたRMS値
     */
    fun setOnAmplitudeListener(listener: ((Float) -> Unit)?) {
        amplitudeListener = listener
    }
    
    /**
     * 録音を開始
     */
    fun startRecording(outputFile: File) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            isRecording = true
            
            recordingThread = Thread {
                writeAudioDataToFile(outputFile)
            }
            recordingThread?.start()
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 録音を停止
     */
    fun stopRecording() {
        android.util.Log.d("WavRecorder", "録音停止中...")
        isRecording = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        recordingThread?.join()
        recordingThread = null
        android.util.Log.d("WavRecorder", "録音停止完了")
    }
    
    /**
     * 音声データをファイルに書き込み
     */
    private fun writeAudioDataToFile(outputFile: File) {
        val data = ByteArray(bufferSize)
        val outputStream = FileOutputStream(outputFile)
        
        // WAVヘッダーのためのスペースを確保
        val header = ByteArray(44)
        outputStream.write(header)
        
        var totalAudioLen = 0L
        
        while (isRecording) {
            val read = audioRecord?.read(data, 0, bufferSize) ?: 0
            if (read > 0) {
                outputStream.write(data, 0, read)
                totalAudioLen += read

                // バッファからRMS音量を計算してコールバック
                try {
                    val shortCount = read / 2
                    if (shortCount > 0) {
                        var sum = 0.0
                        var i = 0
                        while (i < shortCount) {
                            // リトルエンディアンのPCM16ビットをshortに変換
                            val lo = data[i * 2].toInt()
                            val hi = data[i * 2 + 1].toInt()
                            val sample = (hi shl 8) or (lo and 0xFF)
                            sum += (sample * sample).toDouble()
                            i++
                        }
                        val rms = kotlin.math.sqrt(sum / shortCount)
                        val normalized = (rms / 32768f).toFloat().coerceIn(0f, 1f)
                        amplitudeListener?.invoke(normalized)
                    }
                } catch (_: Exception) {
                    // 音量計算で例外が出ても録音自体は継続する
                }
            }
        }
        
        outputStream.close()
        
        // WAVヘッダーを書き込み
        writeWavHeader(outputFile, totalAudioLen)
    }
    
    /**
     * WAVヘッダーを書き込み
     */
    private fun writeWavHeader(file: File, audioDataLen: Long) {
        android.util.Log.d("WavRecorder", "WAVヘッダー書き込み開始")
        android.util.Log.d("WavRecorder", "音声データサイズ: $audioDataLen bytes")
        
        val totalDataLen = audioDataLen + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2 // 16bit = 2 bytes
        
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            
            // RIFF header
            raf.write("RIFF".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(raf, totalDataLen.toInt())
            raf.write("WAVE".toByteArray(Charsets.US_ASCII))
            
            // fmt chunk
            raf.write("fmt ".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(raf, 16) // fmt chunk size
            writeLittleEndianShort(raf, 1) // audio format (PCM)
            writeLittleEndianShort(raf, channels) // number of channels
            writeLittleEndianInt(raf, SAMPLE_RATE) // sample rate
            writeLittleEndianInt(raf, byteRate) // byte rate
            writeLittleEndianShort(raf, channels * 2) // block align
            writeLittleEndianShort(raf, 16) // bits per sample
            
            // data chunk
            raf.write("data".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(raf, audioDataLen.toInt())
        }
        
        android.util.Log.d("WavRecorder", "WAVヘッダー書き込み完了")
        android.util.Log.d("WavRecorder", "最終ファイルサイズ: ${file.length()} bytes")
        
        // ファイルの先頭4バイトを確認（デバッグ用）
        val header = ByteArray(4)
        RandomAccessFile(file, "r").use { raf ->
            raf.read(header)
        }
        val headerString = String(header, Charsets.US_ASCII)
        android.util.Log.d("WavRecorder", "ファイルヘッダー: $headerString (期待値: RIFF)")
    }
    
    /**
     * Little Endianで4バイト整数を書き込み
     */
    private fun writeLittleEndianInt(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value shr 8) and 0xFF)
        raf.writeByte((value shr 16) and 0xFF)
        raf.writeByte((value shr 24) and 0xFF)
    }
    
    /**
     * Little Endianで2バイト整数を書き込み
     */
    private fun writeLittleEndianShort(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value shr 8) and 0xFF)
    }
}
