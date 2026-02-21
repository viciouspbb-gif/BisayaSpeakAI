package com.bisayaspeak.ai.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 完全無料のGoogle TTSサービス
 * OpenAI TTSの代替として使用する
 */
class FreeTTSService(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true

            // Cebuano（ビサヤ語）を試す
            val cebuanoResult = tts?.setLanguage(Locale("ceb"))
            
            if (cebuanoResult == TextToSpeech.LANG_MISSING_DATA || 
                cebuanoResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Cebuanoが使えない場合は英語にフォールバック
                Log.w("FreeTTS", "Cebuano not available, falling back to English")
                tts?.setLanguage(Locale.ENGLISH)
            } else {
                Log.d("FreeTTS", "TTS initialized with Cebuano")
            }
            
            // 音声スピードを少し遅くする
            tts?.setSpeechRate(0.9f)
        } else {
            Log.e("FreeTTS", "Initialization failed")
        }
    }

    /**
     * テキストを音声で再生
     */
    fun play(text: String) {
        if (!isReady) {
            Log.w("FreeTTS", "TTS not ready, skipping: $text")
            return
        }
        
        Log.d("FreeTTS", "Speaking: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
    }

    /**
     * リソースのクリーンアップ
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
    }
}
