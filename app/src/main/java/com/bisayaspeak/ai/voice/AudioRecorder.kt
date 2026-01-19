package com.bisayaspeak.ai.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.io.File

/**
 * MediaRecorder を使った会話入力向け録音ユーティリティ。
 */
class VoiceInputRecorder(private val context: Context) {
    companion object {
        private const val TAG = "ConversationRecorder"
        private const val COOLDOWN_MS = 300L
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording: Boolean = false
    private var nextAvailableAt: Long = 0L

    fun startRecording(): File {
        stopAndCleanup(deleteFile = false, applyCooldown = false)
        currentFile = null

        val now = SystemClock.elapsedRealtime()
        if (now < nextAvailableAt) {
            throw IllegalStateException("録音の準備中です。少し待ってからもう一度お試しください。")
        }

        val outputDir = File(context.cacheDir, "voice_inputs").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File.createTempFile("talk_", ".m4a", outputDir)

        val recorder = MediaRecorder()
        mediaRecorder = recorder

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioSamplingRate(16000)
        recorder.setAudioEncodingBitRate(96000)
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.prepare()
        recorder.start()

        currentFile = outputFile
        isRecording = true
        Log.d(TAG, "Recording started -> ${outputFile.absolutePath}")
        return outputFile
    }

    fun stopRecording(): File? {
        val file = currentFile
        if (!isRecording) {
            return file
        }
        try {
            mediaRecorder?.let { recorder ->
                try {
                    recorder.stop()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "MediaRecorder stop failed", e)
                    file?.delete()
                    throw IllegalStateException("録音データの保存に失敗しました", e)
                }
            }
        } finally {
            stopAndCleanup(deleteFile = false, applyCooldown = true)
        }
        return file
    }

    fun cancelRecording() {
        val file = currentFile
        if (isRecording) {
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {
            } finally {
                stopAndCleanup(deleteFile = true, applyCooldown = true)
            }
        } else {
            stopAndCleanup(deleteFile = false, applyCooldown = false)
        }
        file?.delete()
        currentFile = null
    }

    private fun stopAndCleanup(deleteFile: Boolean, applyCooldown: Boolean) {
        try {
            mediaRecorder?.apply {
                try {
                    reset()
                } catch (_: Exception) {
                }
                try {
                    release()
                } catch (_: Exception) {
                }
            }
        } finally {
            mediaRecorder = null
            if (deleteFile) {
                currentFile?.delete()
            }
            if (applyCooldown) {
                nextAvailableAt = SystemClock.elapsedRealtime() + COOLDOWN_MS
            }
            isRecording = false
            if (deleteFile) {
                currentFile = null
            }
        }
    }
}
