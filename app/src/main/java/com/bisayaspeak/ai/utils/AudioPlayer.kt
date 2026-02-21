package com.bisayaspeak.ai.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    suspend fun playAudioBytes(audioBytes: ByteArray) = withContext(Dispatchers.IO) {

        try {
            Log.d("AudioPlayer", "Start playback, bytes=${audioBytes.size}")

            // 前のプレイヤー破棄
            mediaPlayer?.release()
            mediaPlayer = null

            // 一時ファイル作成
            val tempFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
            tempFile.writeBytes(audioBytes)
            Log.d("AudioPlayer", "Temp file=${tempFile.absolutePath}, size=${tempFile.length()}")

            // MediaPlayer 新規インスタンス
            val player = MediaPlayer()
            mediaPlayer = player

            // DataSource 設定（IOスレッドでOK）
            player.setDataSource(tempFile.absolutePath)

            // コールバック設定（UIスレッドに戻さなくてOK）
            player.setOnPreparedListener {
                Log.d("AudioPlayer", "Prepared → start()")
                it.start()
            }

            player.setOnCompletionListener {
                Log.d("AudioPlayer", "Completed → cleanup")
                it.release()
                mediaPlayer = null
                tempFile.delete()
            }

            player.setOnErrorListener { mp, what, extra ->
                Log.e("AudioPlayer", "Error what=$what extra=$extra")
                mp.release()
                mediaPlayer = null
                tempFile.delete()
                true
            }

            // 非同期準備（ここがIOでOK）
            player.prepareAsync()

        } catch (e: Exception) {
            Log.e("AudioPlayer", "Exception:", e)
        }
    }
    
    /**
     * リソースIDから効果音を再生
     */
    fun playSound(resId: Int) {
        try {
            // 前のプレイヤー停止
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            // 新しいMediaPlayerを作成して再生
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
            
            Log.d("AudioPlayer", "Playing sound resource: $resId")
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing sound", e)
        }
    }
}
