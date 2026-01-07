package com.bisayaspeak.ai.util

import java.io.*

/**
 * 音声ファイル変換ユーティリティ
 */
object AudioUtil {

    /**
     * PCMファイルをWAVファイルに変換（WAVヘッダを付与）
     * @param pcmFile 入力PCMファイル
     * @param wavFile 出力WAVファイル
     * @param sampleRate サンプリングレート（デフォルト: 16000Hz）
     */
    fun pcmToWav(pcmFile: File, wavFile: File, sampleRate: Int = 16000) {
        val pcmData = pcmFile.readBytes()
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8

        val out = DataOutputStream(FileOutputStream(wavFile))

        // WAV Header
        out.writeBytes("RIFF")
        out.writeInt(Integer.reverseBytes((totalDataLen).toInt()))
        out.writeBytes("WAVE")
        out.writeBytes("fmt ")
        out.writeInt(Integer.reverseBytes(16)) // Subchunk1Size
        out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM
        out.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        out.writeInt(Integer.reverseBytes(sampleRate))
        out.writeInt(Integer.reverseBytes(byteRate))
        out.writeShort(java.lang.Short.reverseBytes((channels * 16 / 8).toShort()).toInt())
        out.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt())
        out.writeBytes("data")
        out.writeInt(Integer.reverseBytes(totalAudioLen.toInt()))

        out.write(pcmData)
        out.flush()
        out.close()
    }
}
