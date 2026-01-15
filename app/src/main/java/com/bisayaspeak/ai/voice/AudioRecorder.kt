package com.bisayaspeak.ai.voice

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

/**
 * MediaRecorder を使った会話入力向け録音ユーティリティ。
 */
class VoiceInputRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording: Boolean = false

    fun startRecording(): File {
        stopRecordingInternal(releaseOnly = true)

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
        Log.d("ConversationRecorder", "Recording started -> ${outputFile.absolutePath}")
        return outputFile
    }

    fun stopRecording(): File? {
        val file = currentFile
        if (!isRecording) {
            return file
        }
        stopRecordingInternal(releaseOnly = false)
        return file
    }

    fun cancelRecording() {
        val file = currentFile
        if (isRecording) {
            stopRecordingInternal(releaseOnly = false)
        } else {
            stopRecordingInternal(releaseOnly = true)
        }
        file?.delete()
        currentFile = null
    }

    private fun stopRecordingInternal(releaseOnly: Boolean) {
        try {
            mediaRecorder?.apply {
                try {
                    if (isRecording) {
                        stop()
                    }
                } catch (_: Exception) {
                }
                reset()
                release()
            }
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
            isRecording = false
            if (!releaseOnly) {
                currentFile = null
            }
        }
    }
}
