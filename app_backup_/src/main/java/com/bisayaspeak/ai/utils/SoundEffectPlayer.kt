package com.bisayaspeak.ai.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.bisayaspeak.ai.R

/**
 * ローカル効果音を再生するクラス（決定版）
 * 正解/不正解の効果音などに使用
 * 
 * MediaPlayerを毎回新規作成し、再生完了後に自動解放することで
 * 無音バグを防止
 */
class SoundEffectPlayer(private val context: Context) {

    private var correctPlayer: MediaPlayer? = null
    private var incorrectPlayer: MediaPlayer? = null

    /**
     * 正解効果音を再生（柔らかいピンポーン）
     */
    fun playCorrect() {
        playSound(R.raw.correct, isCorrect = true)
    }

    /**
     * 不正解効果音を再生（低めのブー）
     */
    fun playIncorrect() {
        playSound(R.raw.incorrect, isCorrect = false)
    }

    /**
     * 効果音を再生（内部メソッド）
     */
    private fun playSound(resId: Int, isCorrect: Boolean) {
        try {
            val player = MediaPlayer.create(context, resId)
            if (isCorrect) correctPlayer = player else incorrectPlayer = player

            player?.setOnCompletionListener {
                it.reset()
                it.release()
            }

            player?.start()

            Log.d("SoundEffectPlayer", "Playing sound: ${if (isCorrect) "correct" else "incorrect"}")
        } catch (e: Exception) {
            Log.e("SoundEffectPlayer", "Failed to play sound", e)
        }
    }

    /**
     * リソースのクリーンアップ
     */
    fun release() {
        correctPlayer?.release()
        incorrectPlayer?.release()
        correctPlayer = null
        incorrectPlayer = null
    }
}
