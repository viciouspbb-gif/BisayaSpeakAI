package com.bisayaspeak.ai.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    // 録音を開始する
    fun startRecording() {
        // キャッシュディレクトリに保存用ファイルを作成
        outputFile = File(context.cacheDir, "audio_record.m4a")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // m4a形式
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    // AACエンコード
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
                Log.d("VoiceRecorder", "Recording started: ${outputFile?.absolutePath}")
            } catch (e: IOException) {
                Log.e("VoiceRecorder", "prepare() failed", e)
            } catch (e: Exception) {
                Log.e("VoiceRecorder", "start() failed", e)
            }
        }
    }

    // 録音を停止し、録音されたファイルを返す
    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("VoiceRecorder", "Recording stopped")
            outputFile // 録音データを返す
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "stop() failed", e)
            null
        }
    }

    // リソース解放
    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
