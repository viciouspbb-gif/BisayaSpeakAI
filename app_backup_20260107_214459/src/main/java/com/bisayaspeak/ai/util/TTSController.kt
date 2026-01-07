package com.bisayaspeak.ai.util

import android.speech.tts.TextToSpeech

/**
 * TTS（音声読み上げ）を管理するシングルトンクラス
 * 複数のTTSインスタンスを一括で停止できる
 */
object TTSController {
    private val ttsInstances = mutableListOf<TextToSpeech>()
    
    /**
     * TTSインスタンスを登録
     */
    fun register(tts: TextToSpeech) {
        ttsInstances.add(tts)
    }
    
    /**
     * TTSインスタンスを解除
     */
    fun unregister(tts: TextToSpeech) {
        ttsInstances.remove(tts)
    }
    
    /**
     * すべてのTTSを停止
     */
    fun stopAll() {
        ttsInstances.forEach { tts ->
            try {
                tts.stop()
            } catch (e: Exception) {
                // エラーを無視
            }
        }
    }
}
