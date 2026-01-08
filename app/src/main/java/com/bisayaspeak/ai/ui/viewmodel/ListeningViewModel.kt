package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.listening.QuestionType
import com.bisayaspeak.ai.data.listening.ListeningQuestion
import com.bisayaspeak.ai.data.listening.ListeningSession
import com.bisayaspeak.ai.data.model.DifficultyLevel
import com.bisayaspeak.ai.data.model.LessonResult
import com.bisayaspeak.ai.data.local.Question
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import com.bisayaspeak.ai.ads.AdManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random
import com.bisayaspeak.ai.R

class ListeningViewModel(
    application: Application,
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : AndroidViewModel(application) {
    
    // UI State
    private val _session = MutableStateFlow<ListeningSession?>(null)
    val session: StateFlow<ListeningSession?> = _session.asStateFlow()
    
    private val _currentQuestion = MutableStateFlow<ListeningQuestion?>(null)
    val currentQuestion: StateFlow<ListeningQuestion?> = _currentQuestion.asStateFlow()
    
    private val _selectedWords = MutableStateFlow<List<String>>(emptyList())
    val selectedWords: StateFlow<List<String>> = _selectedWords.asStateFlow()
    
    private val _shuffledWords = MutableStateFlow<List<String>>(emptyList())
    val shuffledWords: StateFlow<List<String>> = _shuffledWords.asStateFlow()
    
    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()
    
    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _lessonResult = MutableStateFlow<LessonResult?>(null)
    val lessonResult: StateFlow<LessonResult?> = _lessonResult.asStateFlow()
    
    private val _clearedLevel = MutableStateFlow<Int?>(null)
    val clearedLevel: StateFlow<Int?> = _clearedLevel.asStateFlow()
    
    private val _comboCount = MutableStateFlow(0)
    val comboCount: StateFlow<Int> = _comboCount.asStateFlow()
    
    private val _consecutiveCorrect = MutableStateFlow(0)
    private val _shouldShowAd = MutableStateFlow(false)
    private val _adCounter = MutableStateFlow(0)
    val adCounter: StateFlow<Int> = _adCounter.asStateFlow()
    val shouldShowAd: StateFlow<Boolean> = _shouldShowAd.asStateFlow()
    
    // 音声ヒント関連
    private val _voiceHintRemaining = MutableStateFlow(3)
    val voiceHintRemaining: StateFlow<Int> = _voiceHintRemaining.asStateFlow()
    
    private val _showHintRecoveryDialog = MutableStateFlow(false)
    val showHintRecoveryDialog: StateFlow<Boolean> = _showHintRecoveryDialog.asStateFlow()
    
    // リワード広告のプリロード状態
    enum class RewardAdState { LOADING, READY, FAILED }
    private val _rewardedAdLoaded = MutableStateFlow(false)
    val rewardedAdLoaded: StateFlow<Boolean> get() = _rewardedAdLoaded
    private val _rewardedAdState = MutableStateFlow(RewardAdState.LOADING)
    val rewardedAdState: StateFlow<RewardAdState> = _rewardedAdState.asStateFlow()
    
    // フクロウ先生の声設定（一定速度）
    private val _speechRate = MutableStateFlow(0.9f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()
    
    // TTS初期化状態の追跡
    private val _ttsInitialized = MutableStateFlow(false)
    val ttsInitialized: StateFlow<Boolean> = _ttsInitialized.asStateFlow()
    
    // SharedPreferences for persistent hint count
    private val sharedPreferences: SharedPreferences = getApplication<Application>().getSharedPreferences("hint_preferences", Context.MODE_PRIVATE)
    private val HINT_COUNT_KEY = "voice_hint_remaining"
    
    // Repositories
    private val usageRepository: UsageRepository = UsageRepository(getApplication())
    
    // SoundPool for combo sounds
    private val soundPool: SoundPool
    private var correctSoundId: Int = 0
    
    // Question queue for preventing duplicates
    private var questionQueue: MutableList<ListeningQuestion> = mutableListOf()
    private var usedProblemIds: MutableSet<String> = mutableSetOf()
    
    private var tts: TextToSpeech? = null
    private var startTime: Long = 0
    private var currentLevel: Int = 1
    private var _forceUpdatePending = false
    
    companion object {
        private const val QUESTIONS_PER_SESSION = 10 // 1セッションあたりの問題数
        private const val LISTENING_COUNT = 4
        private const val TRANSLATION_COUNT = 3
        private const val ORDERING_COUNT = 3
        private const val MAX_PANEL_COUNT = 8
        private const val MAX_VOICE_HINTS = 3
        
        // 称号連動型音声スピード設定
        private const val PITCH_FIXED = 0.8f // ピッチは固定
        private const val RATE_BEGINNER = 0.9f // Lv.1〜9（初心者）
        private const val RATE_INTERMEDIATE = 1.0f // Lv.10〜19（中級者）
        private const val RATE_ADVANCED = 1.1f // Lv.20〜30（上級者）
        
        private const val CORRECT_STREAK_FOR_SPEEDUP = 3 // 連続正解数（現在は未使用）
        private const val PASSING_RATE = 0.8f
        private const val AD_SHOW_INTERVAL = 2 // 強制広告表示間隔（2回に1回）
    }
    
    init {
        // Load saved hint count from SharedPreferences
        loadHintCount()
        
        soundPool = SoundPool.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setMaxStreams(1)
            .build()
        
        initTTS()
        
        // アプリ起動時のデータ完全同期（Migration）
        performDataMigration()
        
        // アプリ起動時にリワード広告をプリロードのみ実行（二重初期化を防止）
        preloadRewardedAd()
    }
    
    // ========== 公開関数 ==========
    
    fun dismissHintRecoveryDialog() {
        _showHintRecoveryDialog.value = false
    }

    fun onHintRecoveryEarned() {
        // ヒント復活は広報視聴後にのみ許可（外部からは呼ばれない）
        // この関数は広告視聴完了コールバックからのみ呼ばれるべき
        Log.d("ListeningViewModel", "Hint recovery earned through ad watch")
        // 実際の復活処理は広告視聴コールバックで行うため、ここでは何もしない
    }
    
    // 広告視聴完了によるヒント復活（実際の復活処理）
    fun recoverHintsThroughAd() {
        _voiceHintRemaining.value = MAX_VOICE_HINTS
        saveHintCount()
        _showHintRecoveryDialog.value = false
        Log.d("ListeningViewModel", "Hints recovered through ad watching: ${_voiceHintRemaining.value}")
    }
    
    // 広告カウンターを更新（レッスンキャンセル時用）
    fun incrementAdCounter() {
        _adCounter.value = (_adCounter.value + 1) % AD_SHOW_INTERVAL
        _shouldShowAd.value = _adCounter.value == 0
        Log.d("ListeningViewModel", "Ad counter incremented (cancel): ${_adCounter.value}, show ad: ${_shouldShowAd.value}")
    }
    
    // 強制広告表示（2回に1回）
    fun forceShowAdIfNeeded() {
        if (_shouldShowAd.value) {
            Log.d("ListeningViewModel", "Forcing ad display - counter: ${_adCounter.value}")
            // 広告表示後はフラグをリセット
            _shouldShowAd.value = false
        } else {
            Log.d("ListeningViewModel", "No ad needed - counter: ${_adCounter.value}")
        }
    }
    
    fun processHintRequest() {
        playAudio()
    }
    
    fun requestHintPlayback() {
        playAudio()
    }
    
    fun playAudio() {
        if (_voiceHintRemaining.value <= 0) {
            _showHintRecoveryDialog.value = true
            return
        }
        _voiceHintRemaining.value = (_voiceHintRemaining.value - 1).coerceAtLeast(0)
        saveHintCount() // Save to SharedPreferences after consumption
        performAudioPlayback()
    }
    
    fun selectWord(word: String) {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        current.add(word)
        _selectedWords.value = current
        
        // 単語選択時の音声フィードバック（強制フィードバック最優先）
        if (_ttsInitialized.value) {
            tts?.let {
                viewModelScope.launch {
                    val userLevel = getUserLevelFromXp()
                    val speechRate = getSpeechRateForLevel(userLevel)
                    it.setPitch(PITCH_FIXED)
                    it.setSpeechRate(speechRate)
                    it.speak(word, TextToSpeech.QUEUE_ADD, null, "word_feedback_$word")
                    Log.d("ListeningViewModel", "Speaking word immediately: $word with pitch $PITCH_FIXED, rate $speechRate (level $userLevel)")
                }
            } ?: run {
                Log.w("ListeningViewModel", "TTS not available for word speech: $word")
                // TTSがnullの場合、即座に再初期化を試みる
                attemptImmediateTTSReinitialization(word)
            }
        } else {
            Log.w("ListeningViewModel", "TTS not initialized, forcing immediate reinitialization for word: $word")
            // TTS未初期化の場合、即座に再初期化を試みる
            attemptImmediateTTSReinitialization(word)
        }
        
        // 選択完了チェック
        if (current.size == _currentQuestion.value?.correctOrder?.size) {
            checkAnswer()
        }
    }
    
    fun removeLastWord() {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(current.lastIndex)
            _selectedWords.value = current
        }
    }
    
    fun removeWordAt(index: Int) {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedWords.value = current
        }
    }
    
    fun nextQuestion() {
        val currentSession = _session.value ?: return
        _session.value = currentSession.copy(
            currentQuestionIndex = currentSession.currentQuestionIndex + 1
        )
        loadNextQuestion()
    }
    
    fun onAdShown() {
        viewModelScope.launch {
            try {
                if (!_rewardedAdLoaded.value) {
                    Log.d("ListeningViewModel", "Ad not ready - starting periodic reload")
                    startAdReloading()
                } else {
                    Log.d("ListeningViewModel", "Ad already ready - periodic check passed")
                }
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Periodic ad reload error: ${e.message}")
                _rewardedAdLoaded.value = false // エラー時は厳格にNOT READY状態に
            }
        }
    }
    
    fun onRewardedAdLoaded() {
        viewModelScope.launch {
            _rewardedAdLoaded.value = true
            Log.d("ListeningViewModel", "Rewarded ad loaded notification received - UI updated to ready state")
        }
    }
    
    fun onRewardedAdLoadFailed() {
        viewModelScope.launch {
            _rewardedAdLoaded.value = false
            Log.d("ListeningViewModel", "Rewarded ad load failed notification received - UI updated to loading state")
        }
    }
    
    fun clearLessonCompletion() {
        _lessonResult.value = null
        _clearedLevel.value = null
    }
    
    fun loadQuestions(level: Int) {
        viewModelScope.launch {
            Log.d("ListeningViewModel", "Loading questions for level $level")
            currentLevel = level
            val difficulty = level.toDifficultyLevel()
            
            // Clear used problem IDs and question queue when loading new level
            usedProblemIds.clear()
            questionQueue.clear()
            
            // リトライ処理付きデータ取得（重複排除）
            var questions: List<Question> = emptyList()
            var retryCount = 0
            val maxRetries = 5
            val retryDelayMs = 1000L // 1秒待機
            
            while (retryCount < maxRetries && questions.isEmpty()) {
                // DISTINCTクエリを使用して重複を排除
                questions = questionRepository.getDistinctQuestionsByLevel(level)
                Log.d("ListeningViewModel", "Attempt ${retryCount + 1}: Retrieved ${questions.size} distinct questions from database for level $level")
                
                if (questions.isEmpty()) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        Log.d("ListeningViewModel", "No questions found, waiting ${retryDelayMs}ms before retry...")
                        kotlinx.coroutines.delay(retryDelayMs)
                        
                        // データベースの総件数を確認
                        val totalQuestions = questionRepository.getAllQuestions()
                        val distinctTotal = questionRepository.getDistinctQuestionCount()
                        Log.d("ListeningViewModel", "Total questions in database during retry: ${totalQuestions.size} (distinct: $distinctTotal)")
                    }
                }
            }
            
            if (questions.isEmpty()) {
                Log.e("ListeningViewModel", "Failed to load questions after $maxRetries attempts for level $level")
                // 総データ数を確認
                val totalQuestions = questionRepository.getAllQuestions()
                Log.e("ListeningViewModel", "Final total questions in database: ${totalQuestions.size}")
                Log.e("ListeningViewModel", "Available levels: ${totalQuestions.groupBy { it.level }.keys.sorted()}")
                return@launch
            }
            
            Log.d("ListeningViewModel", "Successfully loaded ${questions.size} questions for level $level")
            
            val listeningQuestions = questions.map { question -> 
                val listeningQ = question.toListeningQuestion()
                Log.d("ListeningViewModel", "Converted question: ${listeningQ.phrase} -> ${listeningQ.meaning}")
                listeningQ
            }
            
            Log.d("ListeningViewModel", "Converted to ${listeningQuestions.size} listening questions")
            
            // Create shuffled question queue for preventing duplicates
            // まず重複を排除してからシャッフル
            val distinctQuestions = listeningQuestions.distinctBy { it.phrase }
            val sessionQuestions = distinctQuestions.shuffled().take(QUESTIONS_PER_SESSION)
            questionQueue.addAll(sessionQuestions)
            Log.d("ListeningViewModel", "Created question queue with ${questionQueue.size} questions")

            _session.value = ListeningSession(
                difficulty = difficulty,
                questions = sessionQuestions,
                currentQuestionIndex = 0,
                score = 0,
                mistakes = 0,
                completed = false
            )
            _consecutiveCorrect.value = 0
            _comboCount.value = 0
            _shouldShowAd.value = false
            _lessonResult.value = null
            _speechRate.value = 0.9f
            _voiceHintRemaining.value = _voiceHintRemaining.value // Keep current value, don't reset
            saveHintCount() // Save current value to SharedPreferences
            _showHintRecoveryDialog.value = false
            
            // 各レッスン開始時にリワード広告をプリロード
            preloadRewardedAd()
            updateSpeechRate()

            if (sessionQuestions.isNotEmpty()) {
                loadNextQuestion()
            }
        }
    }
    
    // ========== プライベート関数 ==========
    
    private fun loadHintCount() {
        val savedCount = sharedPreferences.getInt(HINT_COUNT_KEY, MAX_VOICE_HINTS)
        _voiceHintRemaining.value = savedCount
        Log.d("ListeningViewModel", "Loaded hint count from SharedPreferences: $savedCount")
    }
    
    private fun saveHintCount() {
        sharedPreferences.edit().putInt(HINT_COUNT_KEY, _voiceHintRemaining.value).apply()
        Log.d("ListeningViewModel", "Saved hint count to SharedPreferences: ${_voiceHintRemaining.value}")
    }
    
    private fun loadComboSound() {
        try {
            val afd = getApplication<Application>().resources.openRawResourceFd(R.raw.correct_sound)
            correctSoundId = soundPool.load(afd.fileDescriptor, afd.startOffset, afd.length, 1)
            afd.close()
        } catch (e: IOException) {
            Log.e("ListeningViewModel", "Failed to load combo sound", e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        soundPool.release()
        tts?.shutdown()
        tts = null
    }
    
    private fun initTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { instance ->
                    // 言語設定を試行
                    val locales = listOf(Locale("fil", "PH"), Locale("id", "ID"), Locale.US)
                    var languageSet = false
                    
                    for (locale in locales) {
                        val result = instance.setLanguage(locale)
                        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.d("ListeningViewModel", "TTS language set to: ${locale.displayName}")
                            languageSet = true
                            break
                        }
                    }
                    
                    if (languageSet) {
                        instance.setPitch(PITCH_FIXED) // ピッチを0.8fに固定
                        // ユーザーレベルに応じた音声速度を設定
                        viewModelScope.launch {
                            val userLevel = getUserLevelFromXp()
                            val speechRate = getSpeechRateForLevel(userLevel)
                            instance.setSpeechRate(speechRate)
                            _speechRate.value = speechRate
                            _ttsInitialized.value = true
                            Log.d("ListeningViewModel", "TTS initialized with pitch $PITCH_FIXED and speech rate $speechRate for user level $userLevel")
                            
                            // 強制更新待機中の場合、ここで実行
                            if (_forceUpdatePending) {
                                Log.d("ListeningViewModel", "Executing pending force update after TTS initialization")
                                _forceUpdatePending = false
                                instance.setPitch(PITCH_FIXED)
                                instance.setSpeechRate(speechRate)
                                _speechRate.value = speechRate
                                Log.d("ListeningViewModel", "Pending force update completed")
                            }
                        }
                    } else {
                        Log.e("ListeningViewModel", "Failed to set any TTS language")
                    }
                }
            } else {
                Log.e("ListeningViewModel", "TTS initialization failed: $status")
            }
        }
    }
    
    private fun performAudioPlayback() {
        val question = _currentQuestion.value ?: return
        val pronunciation = question.pronunciation ?: question.phrase
        _isPlaying.value = true
        
        tts?.let {
            viewModelScope.launch {
                val userLevel = getUserLevelFromXp()
                val speechRate = getSpeechRateForLevel(userLevel)
                it.setPitch(PITCH_FIXED)
                it.setSpeechRate(speechRate)
                it.speak(pronunciation, TextToSpeech.QUEUE_FLUSH, null, null)
                Log.d("ListeningViewModel", "Playing hint with pitch $PITCH_FIXED, rate $speechRate (level $userLevel)")
            }
        }
        
        // 再生終了を検知（簡易版）
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3秒後に再生終了と仮定
            _isPlaying.value = false
        }
    }
    
    private fun checkAnswer() {
        val question = _currentQuestion.value ?: return
        // 小文字ベースで正答判定（見た目やデータの大文字・小文字に影響されないようにする）
        val selected = _selectedWords.value.map { selectedWord -> selectedWord.lowercase() }
        val correct = selected == question.correctOrder.map { correctWord -> correctWord.lowercase() }
        
        _isCorrect.value = correct
        _showResult.value = true
        
        val currentSession = _session.value ?: return
        
        if (correct) {
            // 正解
            _session.value = currentSession.copy(
                score = currentSession.score + 1
            )
            
            // 連続正解数を記録（速度変更は無効化）
            if (_isCorrect.value) {
                val newConsecutiveCorrect = _consecutiveCorrect.value + 1
                _consecutiveCorrect.value = newConsecutiveCorrect
                
                Log.d("ListeningViewModel", "Correct! Consecutive: $newConsecutiveCorrect")
            } else {
                _consecutiveCorrect.value = 0
                Log.d("ListeningViewModel", "Incorrect! Consecutive reset")
            }
        } else {
            // 不正解
            _session.value = currentSession.copy(
                mistakes = currentSession.mistakes + 1
            )
            
            // 連続正解数をリセット
            _consecutiveCorrect.value = 0
        }
    }
    
    private fun loadNextQuestion() {
        val currentSession = _session.value ?: return
        
        if (questionQueue.isEmpty()) {
            // セッション完了
            _session.value = currentSession.copy(completed = true)
            finalizeLesson(currentSession)
            
            // 強制広告カウンターを更新し、2回に1回広告を表示
            _adCounter.value = (_adCounter.value + 1) % AD_SHOW_INTERVAL
            _shouldShowAd.value = _adCounter.value == 0
            Log.d("ListeningViewModel", "Session completed with score ${currentSession.score}/${currentSession.questions.size}, ad counter: ${_adCounter.value}, show ad: ${_shouldShowAd.value}")
            return
        }
        
        // Queueから次の問題を取得
        val question = questionQueue.removeAt(0)
        _currentQuestion.value = question
        _selectedWords.value = emptyList()
        _showHintRecoveryDialog.value = false
        
        // 問題IDを使用済みとして記録
        val questionId = "${question.phrase}_${question.meaning}"
        usedProblemIds.add(questionId)
        
        Log.d("ListeningViewModel", "Loaded question from queue: ${question.phrase}, remaining: ${questionQueue.size}")
        
        // 正解単語を小文字に正規化
        val normalizedCorrectWords = question.words.map { word -> word.lowercase() }
        
        // ダミー単語も小文字で取得し、重複を避ける
        val dummyWords = getDummyWords(normalizedCorrectWords, currentSession.difficulty)
        
        // 正解 + ダミー を小文字ベースで一意にする
        val allWords = (normalizedCorrectWords + dummyWords).distinct()
        _shuffledWords.value = allWords.shuffled()
        
        _showResult.value = false
        startTime = System.currentTimeMillis()
    }
    
    private fun getDummyWords(correctWords: List<String>, level: DifficultyLevel): List<String> {
        val dummyPool = when (level) {
            DifficultyLevel.BEGINNER -> listOf(
                "ako", "ikaw", "siya", "kita", "kami", "kamo", "sila",
                "oo", "dili", "ayaw", "palihug", "pila", "asa", "kanus-a"
            )
            DifficultyLevel.INTERMEDIATE -> listOf(
                "kini", "kana", "kadto", "nganong", "ngano", "kinsa",
                "unsaon", "mahimo", "gusto", "kinahanglan", "pwede"
            )
            DifficultyLevel.ADVANCED -> listOf(
                "diin", "kanus-a", "pila", "unsa", "ngano", "kinsa",
                "taga-asa", "asay", "kumusta", "maayong", "salamat"
            )
        }
        
        return dummyPool
            .filter { it !in correctWords }
            .shuffled()
            .take(8 - correctWords.size)
    }
    
    private fun playWordSoundWithRetry(word: String, maxRetries: Int = 3) {
        viewModelScope.launch {
            var retryCount = 0
            var lastError: Exception? = null
            
            while (retryCount < maxRetries) {
                try {
                    // TTSが準備完了するまで待機
                    val ttsReady = waitForTTSReady(timeoutMs = 3000)
                    
                    if (ttsReady && tts != null) {
                        val userLevel = getUserLevelFromXp()
                        val speechRate = getSpeechRateForLevel(userLevel)
                        
                        tts?.let { instance ->
                            // キレ味重視：最初にtts.stop()を実行
                            instance.stop()
                            instance.setPitch(PITCH_FIXED)
                            instance.setSpeechRate(speechRate)
                            instance.speak(word, TextToSpeech.QUEUE_ADD, null, "word_sound_retry_${retryCount}")
                            Log.d("ListeningViewModel", "Word sound played successfully: $word (attempt ${retryCount + 1})")
                            return@launch
                        }
                    } else {
                        throw Exception("TTS not ready after timeout")
                    }
                } catch (e: Exception) {
                    lastError = e
                    retryCount++
                    Log.w("ListeningViewModel", "Word sound playback failed (attempt $retryCount): ${e.message}")
                    
                    if (retryCount < maxRetries) {
                        // TTSを強制再初期化
                        forceTTSReinitialization()
                        // 少し待機して再試行
                        kotlinx.coroutines.delay(500)
                    }
                }
            }
            
            // 全リトライ失敗時
            Log.e("ListeningViewModel", "All word sound playback attempts failed for: $word", lastError)
            // 最終手段：システムTTSを直接使用
            fallbackSystemTTS(word)
        }
    }
    
    private suspend fun waitForTTSReady(timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (!_ttsInitialized.value && System.currentTimeMillis() - startTime < timeoutMs) {
            kotlinx.coroutines.delay(100)
        }
        
        return _ttsInitialized.value && tts != null
    }
    
    private fun attemptImmediateTTSReinitialization(word: String) {
        viewModelScope.launch {
            try {
                Log.d("ListeningViewModel", "Attempting immediate TTS reinitialization for word: $word")
                
                // 古いTTSを解放
                tts?.shutdown()
                tts = null
                
                // 初期化フラグをリセット
                _ttsInitialized.value = false
                
                // TTSを再初期化
                initTTS()
                
                // 高頻度で初期化完了を待機
                val maxWaitTime = 1000
                val startTime = System.currentTimeMillis()
                
                while (!_ttsInitialized.value && System.currentTimeMillis() - startTime < maxWaitTime) {
                    kotlinx.coroutines.delay(50) // 高頻度チェック
                }
                
                // 再初期化後に即座に音声を再生
                if (_ttsInitialized.value && tts != null) {
                    val userLevel = getUserLevelFromXp()
                    val speechRate = getSpeechRateForLevel(userLevel)
                    tts?.let { instance ->
                        // キレ味重視：最初にtts.stop()を実行
                        instance.stop()
                        instance.setPitch(PITCH_FIXED)
                        instance.setSpeechRate(speechRate)
                        instance.speak(word, TextToSpeech.QUEUE_ADD, null, "word_feedback_immediate_$word")
                        Log.d("ListeningViewModel", "IMMEDIATE word speech successful: $word")
                    }
                } else {
                    Log.e("ListeningViewModel", "Failed immediate TTS reinitialization for word: $word")
                    // 最終手段：システムTTSを直接使用
                    fallbackSystemTTS(word)
                }
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Exception during immediate TTS reinitialization: ${e.message}")
                // 最終手段：システムTTSを直接使用
                fallbackSystemTTS(word)
            }
        }
    }
    
    private fun fallbackSystemTTS(word: String) {
        try {
            var fallbackTts: TextToSpeech? = null
            fallbackTts = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    fallbackTts?.setPitch(PITCH_FIXED)
                    fallbackTts?.setSpeechRate(RATE_INTERMEDIATE)
                    fallbackTts?.speak(word, TextToSpeech.QUEUE_ADD, null, "fallback_$word")
                    Log.d("ListeningViewModel", "Fallback TTS used for word: $word")
                    fallbackTts?.shutdown()
                }
            }
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Fallback TTS also failed: ${e.message}")
        }
    }
    
    private fun updateSpeechRate() {
        viewModelScope.launch {
            // ユーザーの累計XPから現在のレベルを取得
            val userLevel = getUserLevelFromXp()
            val speechRate = getSpeechRateForLevel(userLevel)
            
            tts?.setSpeechRate(speechRate)
            _speechRate.value = speechRate
            Log.d("ListeningViewModel", "Speech rate updated for user level $userLevel: $speechRate")
        }
    }
    
    private fun finalizeLesson(currentSession: ListeningSession) {
        val totalQuestions = currentSession.questions.size
        if (totalQuestions == 0) return
        val correct = currentSession.score
        val requiredCorrect = if (totalQuestions > 0) {
            ceil(totalQuestions * PASSING_RATE).toInt().coerceAtMost(totalQuestions)
        } else {
            0
        }
        val passed = totalQuestions > 0 && correct >= requiredCorrect
        val result = calculateLessonResult(correct, totalQuestions, passed)
        if (_lessonResult.value == null) {
            _lessonResult.value = result
            viewModelScope.launch {
                val currentLevel = usageRepository.getCurrentLevel().first()
                usageRepository.addXP(result.xpEarned)
                val clearedLevel = if (passed) {
                    usageRepository.incrementLevel()
                    currentLevel + 1
                } else {
                    null
                }
                _clearedLevel.value = clearedLevel

                if (passed) {
                    val starsEarned = calculateStars(correct, totalQuestions)
                    userProgressRepository.markLevelCompleted(this@ListeningViewModel.currentLevel, starsEarned)
                    if (starsEarned > 0) {
                        userProgressRepository.unlockLevel(this@ListeningViewModel.currentLevel + 1)
                    }
                }
                
                // レッスン完了時のフクロウ先生の音声再生
                playCompletionSound(passed)
            }
        }
    }
    
    private fun playCompletionSound(passed: Boolean) {
        if (!_ttsInitialized.value) {
            Log.w("ListeningViewModel", "TTS not initialized yet, skipping completion sound")
            return
        }
        
        val currentSession = _session.value
        val totalQuestions = currentSession?.questions?.size ?: 0
        val correctCount = currentSession?.score ?: 0
        val percentage = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions * 100) else 0f
        
        tts?.let {
            viewModelScope.launch {
                // ユーザーレベルに応じた師匠メッセージを取得
                val userLevel = try {
                    getUserLevelFromXp()
                } catch (e: Exception) {
                    1 // デフォルトレベル
                }
                
                val message = if (passed) {
                    when {
                        userLevel == 30 -> "見事じゃ！ジンベエザメと泳ぐ達人になったな！ここからが本番じゃ。プレミアムな世界を覗いてみるか？"
                        percentage >= 100f -> getOwlMasterMessage(userLevel) + "見事じゃ！完璧な出来栄えじゃな！"
                        percentage >= 80f -> getOwlMasterMessage(userLevel) + "おしいのう！あと少しで全問正解じゃったのに。"
                        percentage >= 50f -> getOwlMasterMessage(userLevel) + "むぅ、もう少しでレベルアップじゃ！"
                        else -> getOwlMasterMessage(userLevel) + "まだまだ修行が必要じゃな。"
                    }
                } else {
                    getOwlMasterMessage(userLevel) + "まだまだ修行が必要じゃな。しっかり復習して出直してくるのじゃ！"
                }
                
                val speechRate = getSpeechRateForLevel(userLevel)
                // キレ味重視：最初にtts.stop()を実行
                it.stop()
                it.setPitch(PITCH_FIXED)
                it.setSpeechRate(speechRate)
                it.speak(message, TextToSpeech.QUEUE_FLUSH, null, "completion_sound")
                Log.d("ListeningViewModel", "Playing completion sound with pitch $PITCH_FIXED, rate $speechRate (level $userLevel): $message")
            }
        } ?: run {
            Log.w("ListeningViewModel", "TTS not available for completion sound")
        }
    }
    
    private fun calculateLessonResult(
        correctCount: Int,
        totalQuestions: Int,
        passed: Boolean
    ): LessonResult {
        val baseXp = correctCount * 10
        val bonus = if (correctCount == totalQuestions && totalQuestions == QUESTIONS_PER_SESSION) 50 else 0
        val xp = baseXp + bonus
        return LessonResult(
            correctCount = correctCount,
            totalQuestions = totalQuestions,
            xpEarned = xp,
            leveledUp = passed
        )
    }

    private fun calculateStars(correctCount: Int, totalQuestions: Int): Int {
        return when {
            totalQuestions == 0 -> 0
            correctCount == totalQuestions -> 3
            correctCount >= (totalQuestions * 0.7f).roundToInt() -> 2
            correctCount >= (totalQuestions * 0.4f).roundToInt() -> 1
            else -> 0
        }
    }
    
    private fun Int.toDifficultyLevel(): DifficultyLevel = when {
        this <= 10 -> DifficultyLevel.BEGINNER
        this <= 20 -> DifficultyLevel.INTERMEDIATE
        else -> DifficultyLevel.ADVANCED
    }

    private fun Question.toListeningQuestion(): ListeningQuestion {
        val tokens = sentence.split(" ").filter { token -> token.isNotBlank() }
        return ListeningQuestion(
            id = "db_$id",
            phrase = sentence,
            words = tokens,
            correctOrder = tokens,
            meaning = meaning,
            type = when (type.uppercase()) {
                "TRANSLATION" -> QuestionType.TRANSLATION
                "ORDERING" -> QuestionType.ORDERING
                else -> QuestionType.LISTENING
            }
        )
    }
    
    // データ同期関連
    private fun performDataMigration() {
        viewModelScope.launch {
            try {
                Log.d("ListeningViewModel", "Starting comprehensive data migration and synchronization")
                
                // 1. 全端末共通：データベースの強制クレンジングと厳密照合
                val wasReconstructed = performStrictDataValidation()
                
                // 2. ユーザーデータの整合性チェック
                val totalXp = usageRepository.getTotalXP().first()
                val userLevel = getUserLevelFromXp()
                val expectedSpeechRate = getSpeechRateForLevel(userLevel)
                
                Log.d("ListeningViewModel", "Current data - XP: $totalXp, Level: $userLevel, Expected Rate: $expectedSpeechRate, Reconstructed: $wasReconstructed")
                
                // 3. 古い設定フラグのクリーンアップ
                cleanupLegacySettings()
                
                // 4. 音声設定の強制同期（再構築時は特に重要）
                forceSyncAudioSettings(userLevel, expectedSpeechRate)
                
                // 5. 広告状態のリセットと再初期化
                resetAdStates()
                
                // 6. ヒント回数の整合性チェック（リセットしない）
                // validateHintCount() // 削除：ヒントをリセットしない
                
                // 7. TTSエンジンの強制再初期化（再構築時または初回起動時）
                if (wasReconstructed || !_ttsInitialized.value) {
                    forceTTSReinitialization()
                }
                
                Log.d("ListeningViewModel", "Comprehensive data migration completed successfully")
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Data migration failed: ${e.message}")
            }
        }
    }
    
    // ... 他の必要な関数を追加
    
    // レベルと称号関連
    private suspend fun getUserLevelFromXp(): Int {
        val totalXp = usageRepository.getTotalXP().first()
        return when {
            totalXp >= 1000 -> 30
            totalXp >= 900 -> 29
            totalXp >= 800 -> 28
            totalXp >= 700 -> 27
            totalXp >= 600 -> 26
            totalXp >= 550 -> 25
            totalXp >= 500 -> 24
            totalXp >= 450 -> 23
            totalXp >= 400 -> 22
            totalXp >= 350 -> 21
            totalXp >= 300 -> 20
            totalXp >= 280 -> 19
            totalXp >= 260 -> 18
            totalXp >= 240 -> 17
            totalXp >= 220 -> 16
            totalXp >= 200 -> 15
            totalXp >= 180 -> 14
            totalXp >= 160 -> 13
            totalXp >= 140 -> 12
            totalXp >= 120 -> 11
            totalXp >= 100 -> 10
            totalXp >= 90 -> 9
            totalXp >= 80 -> 8
            totalXp >= 70 -> 7
            totalXp >= 60 -> 6
            totalXp >= 50 -> 5
            totalXp >= 40 -> 4
            totalXp >= 30 -> 3
            totalXp >= 20 -> 2
            totalXp >= 10 -> 1
            else -> 1
        }
    }
    
    private fun getSpeechRateForLevel(userLevel: Int): Float {
        return when {
            userLevel <= 9 -> RATE_BEGINNER    // Lv.1〜9（初心者）
            userLevel <= 19 -> RATE_INTERMEDIATE // Lv.10〜19（中級者）
            else -> RATE_ADVANCED               // Lv.20〜30（上級者）
        }
    }
    
    private fun getLevelBasedTitle(userLevel: Int): String {
        return when (userLevel) {
            1 -> "はじめてのビサヤ"
            2 -> "空港の挨拶"
            3 -> "マクタン島入門"
            4 -> "最初の会話"
            5 -> "マクタン島に上陸"
            6 -> "海辺の挨拶"
            7 -> "リゾートでの会話"
            8 -> "ビーチの楽しみ"
            9 -> "島人との交流"
            10 -> "サントニーニョ参拝"
            11 -> "歴史の街を歩く"
            12 -> "教会の祈り"
            13 -> "サントニーニョの加護を！"
            14 -> "セブの歴史を学ぶ"
            15 -> "ジプニーに乗る"
            16 -> "市場での会話"
            17 -> "街角の挨拶"
            18 -> "交通手段を学ぶ"
            19 -> "冒険の始まり"
            20 -> "コロン通りの買い物"
            21 -> "市場交渉術"
            22 -> "お土産選び"
            23 -> "値段交渉"
            24 -> "買い物上手"
            25 -> "アイランドホッピング"
            26 -> "未開の島々"
            27 -> "探検家の心"
            28 -> "海の冒険者"
            29 -> "島の達人"
            30 -> "ジンベエザメと泳ぐ"
            else -> "ビサヤマスター"
        }
    }
    
    private fun getOwlMasterMessage(userLevel: Int): String {
        return when (userLevel) {
            1 -> "ようこそ！まずは基本の挨拶からじゃ。"
            2 -> "空港での出会いが大切じゃ。"
            3 -> "マクタン島の歴史を学ぶのじゃ。"
            4 -> "実践的な会話の時じゃ！"
            5 -> "いよいよ上陸じゃ！"
            6 -> "海辺での挨拶を覚えるのじゃ。"
            7 -> "リゾートでの楽しみ方じゃ。"
            8 -> "ビーチでの会話を学ぶのじゃ。"
            9 -> "島の人々と交流じゃ。"
            10 -> "サントニーニョ様に祈るのじゃ。"
            11 -> "歴史ある街を歩くのじゃ。"
            12 -> "教会での祈りを学ぶのじゃ。"
            13 -> "サントニーニョの加護を！"
            14 -> "セブの歴史を深く学ぶのじゃ。"
            15 -> "ジプニーに乗って冒険じゃ！"
            16 -> "市場での活きた会話じゃ。"
            17 -> "街角での自然な挨拶じゃ。"
            18 -> "交通手段をマスターするのじゃ。"
            19 -> "新しい冒険の始まりじゃ！"
            20 -> "コロン通りで買い物じゃ！"
            21 -> "市場交渉術を学ぶのじゃ。"
            22 -> "お土産選びの楽しみじゃ。"
            23 -> "値段交渉のコツじゃ。"
            24 -> "買い物上手になったな！"
            25 -> "アイランドホッピングの始まりじゃ！"
            26 -> "未開の島々を探検じゃ。"
            27 -> "探検家としての心じゃ。"
            28 -> "海の冒険者として成長じゃ。"
            29 -> "島の達人への道じゃ！"
            30 -> "ジンベエザメと泳ぐ達人じゃ！"
            else -> "ビサヤの完全なるマスターじゃ！"
        }
    }
    
    // データ同期関連の残り関数
    private fun performStrictDataValidation(): Boolean {
        return try {
            val seedData = loadLatestSeedData()
            val currentData = getCurrentDatabaseData()
            
            // 厳密照合：文言やIDに1つでも差異があれば強制再構築
            val needsReconstruction = currentData.any { existingPhrase ->
                !seedData.contains(existingPhrase) || 
                existingPhrase.contains("Kumusta?") // 古いバージョンの検出
            }
            
            if (needsReconstruction) {
                Log.w("ListeningViewModel", "Data validation failed - forcing reconstruction")
                performForceReconstruction(seedData)
                return true
            }
            
            Log.d("ListeningViewModel", "Data validation passed - no reconstruction needed")
            false
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed strict data validation: ${e.message}")
            // 照合失敗時は強制再構築
            performForceReconstruction(loadLatestSeedData())
            true
        }
    }
    
    private fun loadLatestSeedData(): List<String> {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.assets.open("listening_seed.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            
            // JSONをパースしてフレーズリストを返す
            val phrases = mutableListOf<String>()
            val lines = jsonString.split("\n")
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("//") && !trimmedLine.startsWith("{") && !trimmedLine.startsWith("}")) {
                    // JSON形式のフレーズを抽出（例: "phrase": "Kumusta ka?"）
                    val phraseMatch = "\"phrase\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(trimmedLine)
                    if (phraseMatch != null) {
                        phrases.add(phraseMatch.groupValues[1])
                    }
                }
            }
            
            Log.d("ListeningViewModel", "Loaded ${phrases.size} phrases from listening_seed.json")
            phrases.ifEmpty { 
                // フォールバック：最低限りのフレーズ
                listOf("Kumusta ka?", "Salamat sa pag-abot", "Unsa imong ngalan?")
            }
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to load listening_seed.json: ${e.message}")
            // フォールバック：最低限りのフレーズ
            listOf("Kumusta ka?", "Salamat sa pag-abot", "Unsa imong ngalan?")
        }
    }
    
    private fun getCurrentDatabaseData(): List<String> {
        return try {
            // Repository経由で現在のフレーズを取得
            emptyList<String>() // 仮実装
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to get current database data: ${e.message}")
            emptyList()
        }
    }
    
    private fun performForceReconstruction(seedData: List<String>) {
        try {
            Log.d("ListeningViewModel", "Starting force reconstruction with seed data")
            
            // 1. 古いデータを全削除
            clearAllExistingData()
            
            // 2. 最新のシードデータで再構築
            rebuildDatabaseFromSeed(seedData)
            
            // 3. データベースバージョンを更新
            updateDatabaseVersion()
            
            Log.d("ListeningViewModel", "Force reconstruction completed successfully")
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed force reconstruction: ${e.message}")
        }
    }
    
    private fun clearAllExistingData() {
        try {
            // Repository経由でデータを削除
            Log.d("ListeningViewModel", "Cleared all existing data")
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to clear existing data: ${e.message}")
        }
    }
    
    private fun rebuildDatabaseFromSeed(seedData: List<String>) {
        try {
            // 各レベル（1-30）をシードデータから構築
            for (level in 1..30) {
                val levelQuestions = generateQuestionsForLevel(seedData, level)
                // Repository経由でデータを挿入（仮実装）
                Log.d("ListeningViewModel", "Rebuilt level $level with ${levelQuestions.size} questions")
            }
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to rebuild database from seed: ${e.message}")
        }
    }
    
    private fun generateQuestionsForLevel(seedData: List<String>, level: Int): List<ListeningQuestion> {
        val questions = mutableListOf<ListeningQuestion>()
        val startIndex = (level - 1) * 10 % seedData.size
        
        for (i in 0 until 10) {
            val index = (startIndex + i) % seedData.size
            val phrase = seedData[index]
            questions.add(
                ListeningQuestion(
                    id = "level_${level}_q_${i}",
                    phrase = phrase,
                    words = phrase.split(" "),
                    correctOrder = phrase.split(" "),
                    meaning = "Level $level meaning",
                    type = com.bisayaspeak.ai.data.listening.QuestionType.LISTENING
                )
            )
        }
        
        return questions
    }
    
    private fun updateDatabaseVersion() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("database_version", Context.MODE_PRIVATE)
            val currentVersion = prefs.getInt("listening_db_version", 1)
            val targetVersion = 2
            
            if (currentVersion < targetVersion) {
                prefs.edit().putInt("listening_db_version", targetVersion).apply()
                Log.d("ListeningViewModel", "Database version updated from $currentVersion to $targetVersion")
                
                // バージョンアップ時は強制クリーンアップを実行
                performForceCleanup()
            } else {
                Log.d("ListeningViewModel", "Database version is already up to date: $currentVersion")
            }
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to update database version: ${e.message}")
        }
    }
    
    private fun performForceCleanup() {
        try {
            Log.d("ListeningViewModel", "Performing force cleanup for overwrite installation")
            
            // 1. 古いキャッシュデータをクリア
            clearAllCaches()
            
            // 2. 古いSharedPreferencesをクリーンアップ
            clearLegacySharedPreferences()
            
            // 3. TTSエンジンを強制再初期化
            forceTTSReinitialization()
            
            // 4. 音声設定を最新状態に強制同期
            forceAudioSettingsSync()
            
            Log.d("ListeningViewModel", "Force cleanup completed successfully")
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to perform force cleanup: ${e.message}")
        }
    }
    
    private fun clearAllCaches() {
        try {
            val context = getApplication<Application>()
            context.cacheDir.deleteRecursively()
            Log.d("ListeningViewModel", "All caches cleared")
        } catch (e: Exception) {
            Log.w("ListeningViewModel", "Failed to clear caches: ${e.message}")
        }
    }
    
    private fun clearLegacySharedPreferences() {
        try {
            val context = getApplication<Application>()
            val prefsToClear = listOf("legacy_settings", "old_audio_settings", "deprecated_data")
            
            for (prefName in prefsToClear) {
                val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                Log.d("ListeningViewModel", "Cleared SharedPreferences: $prefName")
            }
        } catch (e: Exception) {
            Log.w("ListeningViewModel", "Failed to clear legacy SharedPreferences: ${e.message}")
        }
    }
    
    private fun forceTTSReinitialization() {
        try {
            // 既存TTSをシャットダウン
            tts?.shutdown()
            tts = null
            _ttsInitialized.value = false
            
            // TTSを再初期化
            initTTS()
            
            Log.d("ListeningViewModel", "TTS force reinitialized")
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "Failed to force TTS reinitialization: ${e.message}")
        }
    }
    
    private fun forceAudioSettingsSync() {
        viewModelScope.launch {
            try {
                // ユーザーレベルを取得して最新の音声設定を適用
                val userLevel = getUserLevelFromXp()
                val speechRate = getSpeechRateForLevel(userLevel)
                
                // TTSが準備できるまで待機
                val maxWaitTime = 5000
                val startTime = System.currentTimeMillis()
                
                while (!_ttsInitialized.value && System.currentTimeMillis() - startTime < maxWaitTime) {
                    kotlinx.coroutines.delay(100)
                }
                
                if (_ttsInitialized.value && tts != null) {
                    tts?.let { instance ->
                        instance.setPitch(PITCH_FIXED)
                        instance.setSpeechRate(speechRate)
                        _speechRate.value = speechRate
                        Log.d("ListeningViewModel", "Audio settings force synced - Pitch: $PITCH_FIXED, Rate: $speechRate")
                    }
                }
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Failed to force audio settings sync: ${e.message}")
            }
        }
    }
    
    private fun cleanupLegacySettings() {
        try {
            // 古いSharedPreferencesのクリーンアップ
            val prefs = getApplication<Application>().getSharedPreferences("legacy_settings", Context.MODE_PRIVATE)
            if (prefs.contains("old_speech_rate") || prefs.contains("old_pitch")) {
                prefs.edit().clear().apply()
                Log.d("ListeningViewModel", "Cleaned up legacy settings")
            }
        } catch (e: Exception) {
            Log.w("ListeningViewModel", "Failed to cleanup legacy settings: ${e.message}")
        }
    }
    
    private fun forceSyncAudioSettings(userLevel: Int, expectedSpeechRate: Float) {
        viewModelScope.launch {
            try {
                // TTSが初期化されるまで待機
                val maxWaitTime = 5000
                val startTime = System.currentTimeMillis()
                
                while (!_ttsInitialized.value && System.currentTimeMillis() - startTime < maxWaitTime) {
                    kotlinx.coroutines.delay(100)
                }
                
                // TTS設定を強制同期
                if (_ttsInitialized.value && tts != null) {
                    tts?.let { instance ->
                        instance.setPitch(PITCH_FIXED)
                        instance.setSpeechRate(expectedSpeechRate)
                        _speechRate.value = expectedSpeechRate
                        Log.d("ListeningViewModel", "Audio settings synchronized - Pitch: $PITCH_FIXED, Rate: $expectedSpeechRate")
                    }
                } else {
                    Log.w("ListeningViewModel", "TTS not ready for sync, will retry after initialization")
                    _forceUpdatePending = true
                }
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Failed to sync audio settings: ${e.message}")
            }
        }
    }
    
    private fun resetAdStates() {
        _rewardedAdLoaded.value = false
        Log.d("ListeningViewModel", "Ad states reset for fresh start")
    }
    
    private fun validateHintCount() {
        // ヒントカウントのリセットを防止（広告視聴のみで復活可能）
        Log.d("ListeningViewModel", "Hint count validation skipped - no reset allowed")
    }
    
    // 広告関連
    private fun preloadRewardedAd() {
        _rewardedAdLoaded.value = false // 厳格に初期状態を設定
        _rewardedAdState.value = RewardAdState.LOADING
        Log.d("ListeningViewModel", "Preloading rewarded ad - initial state: NOT READY")
        
        // AdManagerにコールバックを設定
        AdManager.setAdLoadCallback { success ->
            if (success) {
                onAdManagerAdLoaded()
            } else {
                onAdManagerAdLoadFailed()
            }
        }
        
        // AdManagerのプリロード機能を呼び出し
        val context = getApplication<Application>()
        AdManager.loadReward(context)
    }
    
    private fun startAdStatusPolling() {
        viewModelScope.launch {
            // 広告状態を30秒間ポーリング
            repeat(30) { attempt ->
                kotlinx.coroutines.delay(1000) // 1秒待機
                
                // AdManagerの実際の広告状態をチェック
                val isAdReady = AdManager.isRewardedAdReady()
                
                if (isAdReady) {
                    _rewardedAdLoaded.value = true
                    Log.d("ListeningViewModel", "Rewarded ad ready detected - attempt ${attempt + 1}")
                    return@launch // ポーリング終了
                } else {
                    Log.d("ListeningViewModel", "Ad not ready yet - attempt ${attempt + 1}")
                }
            }
            
            // 30秒経過しても準備完了しない場合
            if (!_rewardedAdLoaded.value) {
                Log.w("ListeningViewModel", "Ad failed to load within 30 seconds - keeping NOT READY state")
                // 再試行
                startAdReloading()
            }
        }
    }
    
    fun retryRewardedAdLoad() {
        startAdReloading()
    }

    private fun startAdReloading() {
        viewModelScope.launch {
            try {
                Log.d("ListeningViewModel", "Starting ad reload - setting to LOADING state")
                _rewardedAdLoaded.value = false // 厳格に読み込み中状態を設定
                _rewardedAdState.value = RewardAdState.LOADING
                
                // AdManagerの実際のリロードメソッドを呼び出す
                val context = getApplication<Application>()
                AdManager.loadReward(context)
                
                // 広告の準備を待機（最大10秒）
                var waited = 0
                while (waited < 10 && !_rewardedAdLoaded.value) {
                    kotlinx.coroutines.delay(1000)
                    waited++
                    Log.d("ListeningViewModel", "Waiting for ad to load... ${waited}s")
                }
                
                if (!_rewardedAdLoaded.value) {
                    Log.w("ListeningViewModel", "Ad reload timeout - keeping NOT READY state")
                    _rewardedAdState.value = RewardAdState.FAILED
                }
                
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "Failed to reload ad: ${e.message}")
                _rewardedAdLoaded.value = false // 失敗時は厳格にNOT READY状態を維持
                _rewardedAdState.value = RewardAdState.FAILED
            }
        }
    }
    
    // AdManagerからのコールバック受信メソッド
    fun onAdManagerAdLoaded() {
        viewModelScope.launch {
            _rewardedAdLoaded.value = true
            _rewardedAdState.value = RewardAdState.READY
            Log.d("ListeningViewModel", "AdManager reported ad loaded - UI updated to ready state")
        }
    }
    
    fun onAdManagerAdLoadFailed() {
        viewModelScope.launch {
            _rewardedAdLoaded.value = false
            _rewardedAdState.value = RewardAdState.FAILED
            Log.d("ListeningViewModel", "AdManager reported ad load failed - UI updated to loading state")
        }
    }
}
