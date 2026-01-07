package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.PronunciationData
import com.bisayaspeak.ai.data.repository.PronunciationRepository
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.utils.WavRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * 録音と診断のViewModel
 */
class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PronunciationRepository()
    private val usageRepository = UsageRepository(application)
    
    private var wavRecorder: WavRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var tts: TextToSpeech? = null
    private var currentLevel: LearningLevel = LearningLevel.BEGINNER
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _diagnosisState = MutableStateFlow<DiagnosisState>(DiagnosisState.Idle)
    val diagnosisState: StateFlow<DiagnosisState> = _diagnosisState.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _isPlayingReference = MutableStateFlow(false)
    val isPlayingReference: StateFlow<Boolean> = _isPlayingReference.asStateFlow()

    // 録音中の音量レベル (0.0〜1.0)
    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()
    
    private var referenceMediaPlayer: MediaPlayer? = null
    
    // 使用回数管理
    val remainingCount = usageRepository.getRemainingCount()
    val canDiagnose = usageRepository.canDiagnose()
    val canWatchAd = usageRepository.canWatchAd()
    val adWatchCount = usageRepository.getAdWatchCount()
    
    /**
     * 録音を開始
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                // 手本音声が再生中の場合は停止して待機
                if (_isPlayingReference.value) {
                    android.util.Log.d("RecordingViewModel", "手本音声再生中のため停止します")
                    stopPlayingReference()
                    // 音声が完全に停止するまで少し待つ
                    kotlinx.coroutines.delay(300)
                }
                
                // 一時ファイルを作成（WAV形式）
                audioFile = File.createTempFile(
                    "bisaya_recording_",
                    ".wav",
                    getApplication<Application>().cacheDir
                )
                
                wavRecorder = WavRecorder().apply {
                    // 音量変化をUIに届ける
                    setOnAmplitudeListener { level ->
                        _volumeLevel.value = level
                    }
                }
                wavRecorder?.startRecording(audioFile!!)
                
                _recordingState.value = RecordingState.Recording
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error(e.message ?: "録音開始エラー")
            }
        }
    }
    
    /**
     * 録音を停止
     */
    fun stopRecording() {
        try {
            wavRecorder?.stopRecording()
            wavRecorder = null
            _volumeLevel.value = 0f
            _recordingState.value = RecordingState.Completed(audioFile)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "録音停止エラー")
        }
    }
    
    /**
     * 発音を診断
     */
    fun diagnosePronunciation(word: String, level: LearningLevel) {
        val file = audioFile ?: run {
            android.util.Log.e("RecordingViewModel", "音声ファイルがありません")
            _diagnosisState.value = DiagnosisState.Error("音声ファイルがありません")
            return
        }
        
        // ファイルサイズチェック（無音録音の検出）
        val fileSize = file.length()
        android.util.Log.d("RecordingViewModel", "音声ファイルサイズ: $fileSize bytes")
        
        if (fileSize < 1000) { // 1KB未満は無音と判断
            android.util.Log.e("RecordingViewModel", "録音が短すぎるか無音です")
            _diagnosisState.value = DiagnosisState.Error("録音が検出されませんでした。\nもう一度、はっきりと発音してください。")
            return
        }
        
        // MediaPlayerで録音時間をチェック
        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }
            val duration = mp.duration
            mp.release()
            
            android.util.Log.d("RecordingViewModel", "録音時間: ${duration}ms")
            
            if (duration < 300) { // 0.3秒未満は短すぎる
                android.util.Log.e("RecordingViewModel", "録音が短すぎます")
                _diagnosisState.value = DiagnosisState.Error("録音が短すぎます。\nもう一度、ゆっくりと発音してください。")
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingViewModel", "音声ファイルの検証エラー: ${e.message}")
            _diagnosisState.value = DiagnosisState.Error("音声ファイルが正しく録音されませんでした。\nもう一度お試しください。")
            return
        }
        
        viewModelScope.launch {
            android.util.Log.d("RecordingViewModel", "診断開始: word=$word, level=$level, file=${file.absolutePath}")
            _diagnosisState.value = DiagnosisState.Loading
            
            val result = repository.checkPronunciation(file, word, level)
            
            _diagnosisState.value = if (result.isSuccess) {
                val response = result.getOrNull()!!
                android.util.Log.d("RecordingViewModel", "診断成功")
                android.util.Log.d("RecordingViewModel", "スコア: ${response.score}")
                android.util.Log.d("RecordingViewModel", "フィードバック: ${response.feedback}")
                
                // スコアが0の場合は参照音声がない
                if (response.score == 0) {
                    android.util.Log.w("RecordingViewModel", "警告: スコアが0です。サーバー側で参照音声が見つかりません。")
                    android.util.Log.w("RecordingViewModel", "フィードバック: ${response.feedback}")
                    
                    // 参照音声がない場合はエラーとして扱う
                    val context = getApplication<Application>()
                    DiagnosisState.Error("この単語の参照音声がまだ準備されていません。\n別の単語で試してください。")
                } else {
                    // PronunciationResponseをPronunciationDataに変換
                    val data = PronunciationData(
                        score = response.score,
                        feedback = response.feedback ?: "診断結果を取得できませんでした",
                        detailedFeedback = response.detailedFeedback ?: emptyList(),
                        tips = response.tips ?: emptyList()
                    )
                    
                    // 診断成功時に回数を増やす
                    usageRepository.incrementDiagnosisCount()
                    DiagnosisState.Success(data)
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "診断エラー"
                android.util.Log.e("RecordingViewModel", "診断失敗: $errorMessage")
                
                // エラーメッセージをユーザーフレンドリーに変換
                val context = getApplication<Application>()
                val friendlyMessage = when {
                    errorMessage.contains("timeout", ignoreCase = true) -> 
                        context.getString(com.bisayaspeak.ai.R.string.error_server_starting)
                    errorMessage.contains("network", ignoreCase = true) || 
                    errorMessage.contains("connection", ignoreCase = true) -> 
                        context.getString(com.bisayaspeak.ai.R.string.error_network)
                    errorMessage.contains("500", ignoreCase = true) -> 
                        context.getString(com.bisayaspeak.ai.R.string.error_server_error)
                    else -> context.getString(com.bisayaspeak.ai.R.string.error_diagnosis)
                }
                
                DiagnosisState.Error(friendlyMessage)
            }
        }
    }
    
    /**
     * 録音した音声を再生
     */
    fun playRecording() {
        val file = audioFile ?: return
        
        try {
            stopPlaying() // 既に再生中なら停止
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                }
                start()
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            _isPlaying.value = false
        }
    }
    
    /**
     * 再生を停止
     */
    fun stopPlaying() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }
    
    /**
     * お手本音声を再生
     */
    fun playReferenceAudio(word: String, level: LearningLevel = LearningLevel.BEGINNER) {
        currentLevel = level
        viewModelScope.launch {
            try {
                android.util.Log.d("RecordingViewModel", "お手本音声再生開始: $word")
                stopPlayingReference() // 既に再生中なら停止
                
                // サーバーから参照音声を取得
                android.util.Log.d("RecordingViewModel", "サーバーから音声取得中...")
                val response = repository.getReferenceAudio(word)
                
                if (response.isSuccess) {
                    val audioData = response.getOrNull()
                    android.util.Log.d("RecordingViewModel", "音声データ取得成功: ${audioData?.size} bytes")
                    if (audioData != null && audioData.isNotEmpty()) {
                        // 一時ファイルに保存
                        val tempFile = File.createTempFile(
                            "reference_",
                            ".mp3",
                            getApplication<Application>().cacheDir
                        )
                        tempFile.writeBytes(audioData)
                        android.util.Log.d("RecordingViewModel", "一時ファイル保存: ${tempFile.absolutePath}")
                        
                        // 再生
                        referenceMediaPlayer = MediaPlayer().apply {
                            setDataSource(tempFile.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                _isPlayingReference.value = false
                                tempFile.delete()
                            }
                            start()
                        }
                        _isPlayingReference.value = true
                        android.util.Log.d("RecordingViewModel", "再生開始")
                    } else {
                        android.util.Log.e("RecordingViewModel", "音声データが空")
                    }
                } else {
                    android.util.Log.e("RecordingViewModel", "音声取得失敗: ${response.exceptionOrNull()?.message}")
                    // サーバーから取得できない場合はTTSで代用
                    android.util.Log.d("RecordingViewModel", "TTSで代用再生")
                    playWithTTS(word, currentLevel)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecordingViewModel", "エラー: ${e.message}", e)
                _isPlayingReference.value = false
            }
        }
    }
    
    /**
     * TTSで音声を再生（レベル別の速度調整）
     */
    private fun playWithTTS(text: String, level: LearningLevel = LearningLevel.BEGINNER) {
        val speechRate = when (level) {
            LearningLevel.BEGINNER -> 0.7f      // 🕐 遅い
            LearningLevel.INTERMEDIATE -> 1.0f  // ⏱ 通常
            LearningLevel.ADVANCED -> 1.3f      // ⚡ 速い
        }
        
        if (tts == null) {
            tts = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("fil", "PH"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        android.util.Log.e("RecordingViewModel", "TTS言語がサポートされていません")
                    } else {
                        tts?.setSpeechRate(speechRate)
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                        _isPlayingReference.value = true
                    }
                }
            }
        } else {
            tts?.setSpeechRate(speechRate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            _isPlayingReference.value = true
        }
        
        // 再生終了後にフラグをリセット（簡易的に3秒後）
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _isPlayingReference.value = false
        }
    }
    
    /**
     * お手本音声の再生を停止
     */
    fun stopPlayingReference() {
        referenceMediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        referenceMediaPlayer = null
        tts?.stop()
        _isPlayingReference.value = false
    }
    
    /**
     * 広告視聴後に回数を復活
     */
    fun onAdWatched() {
        viewModelScope.launch {
            usageRepository.incrementAdWatchCount()
        }
    }
    
    /**
     * 状態をリセット
     */
    fun reset() {
        stopPlaying()
        stopPlayingReference()
        _recordingState.value = RecordingState.Idle
        _diagnosisState.value = DiagnosisState.Idle
        _volumeLevel.value = 0f
        audioFile?.delete()
        audioFile = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPlaying()
        stopPlayingReference()
        wavRecorder?.stopRecording()
        audioFile?.delete()
        tts?.shutdown()
        _volumeLevel.value = 0f
    }
}

/**
 * 録音状態
 */
sealed class RecordingState {
    object Idle : RecordingState()
    object Recording : RecordingState()
    data class Completed(val file: File?) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

/**
 * 診断状態
 */
sealed class DiagnosisState {
    object Idle : DiagnosisState()
    object Loading : DiagnosisState()
    data class Success(val result: PronunciationData) : DiagnosisState()
    data class Error(val message: String) : DiagnosisState()
}
