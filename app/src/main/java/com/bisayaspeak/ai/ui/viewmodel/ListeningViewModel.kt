package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.listening.ListeningQuestion
import com.bisayaspeak.ai.data.listening.ListeningQuestions
import com.bisayaspeak.ai.data.listening.ListeningSession
import com.bisayaspeak.ai.data.listening.QuestionType
import com.bisayaspeak.ai.data.model.DifficultyLevel
import com.bisayaspeak.ai.data.model.LessonResult
import com.bisayaspeak.ai.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random

class ListeningViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _session = MutableStateFlow<ListeningSession?>(null)
    val session: StateFlow<ListeningSession?> = _session.asStateFlow()
    
    private val _currentQuestion = MutableStateFlow<ListeningQuestion?>(null)
    val currentQuestion: StateFlow<ListeningQuestion?> = _currentQuestion.asStateFlow()
    
    private val _selectedWords = MutableStateFlow<List<String>>(emptyList())
    val selectedWords: StateFlow<List<String>> = _selectedWords.asStateFlow()
    
    private val _shuffledWords = MutableStateFlow<List<String>>(emptyList())
    val shuffledWords: StateFlow<List<String>> = _shuffledWords.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()
    
    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()
    
    private val _lessonResult = MutableStateFlow<LessonResult?>(null)
    val lessonResult: StateFlow<LessonResult?> = _lessonResult.asStateFlow()
    
    private val _clearedLevel = MutableStateFlow<Int?>(null)
    val clearedLevel: StateFlow<Int?> = _clearedLevel.asStateFlow()
    
    // 広告表示フラグ（セッション完了時のみ）
    private val _shouldShowAd = MutableStateFlow(false)
    val shouldShowAd: StateFlow<Boolean> = _shouldShowAd.asStateFlow()
    
    // 既出問題を記録（レベル別）
    private val _usedQuestionIds = MutableStateFlow<Map<DifficultyLevel, Set<String>>>(emptyMap())
    
    // 正解率に応じた音声速度
    private val _speechRate = MutableStateFlow(0.7f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()
    
    // 連続正解数
    private val _consecutiveCorrect = MutableStateFlow(0)
    val consecutiveCorrect: StateFlow<Int> = _consecutiveCorrect.asStateFlow()
    
    private var tts: TextToSpeech? = null
    private val usageRepository = UsageRepository(application)
    private var startTime: Long = 0
    
    companion object {
        private const val QUESTIONS_PER_SESSION = 10 // 1セッションあたりの問題数
        private const val LISTENING_COUNT = 4
        private const val TRANSLATION_COUNT = 3
        private const val ORDERING_COUNT = 3
        private const val MAX_PANEL_COUNT = 8
        private const val MIN_SPEECH_RATE = 0.6f // 最低速度（遅い）
        private const val MAX_SPEECH_RATE = 1.0f // 最高速度（通常）
        private const val CORRECT_STREAK_FOR_SPEEDUP = 3 // 速度を上げる連続正解数
    }
    
    init {
        initTTS()
    }
    
    private fun initTTS() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tagalogLocale = Locale("fil", "PH")
                val fallbackLocale = Locale("in", "ID")
                val primaryResult = tts?.setLanguage(tagalogLocale)
                val primarySupported = primaryResult != TextToSpeech.LANG_MISSING_DATA &&
                        primaryResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!primarySupported) {
                    Log.w("ListeningViewModel", "Tagalog TTS not supported, trying Indonesian fallback")
                    val fallbackResult = tts?.setLanguage(fallbackLocale)
                    val fallbackSupported = fallbackResult != TextToSpeech.LANG_MISSING_DATA &&
                            fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!fallbackSupported) {
                        Log.w("ListeningViewModel", "Indonesian TTS not supported, falling back to default locale")
                        tts?.language = Locale.US
                    }
                }
                updateSpeechRate()
            } else {
                Log.e("ListeningViewModel", "TTS initialization failed: $status")
            }
        }
    }
    
    /**
     * 音声速度を更新
     */
    private fun updateSpeechRate() {
        tts?.setSpeechRate(_speechRate.value)
        Log.d("ListeningViewModel", "Speech rate updated to ${_speechRate.value}")
    }
    
    /**
     * セッション開始
     */
    fun startSession(level: DifficultyLevel) {
        val isLiteBuild = BuildConfig.IS_LITE_BUILD
        val selectedQuestions = if (isLiteBuild) {
            ListeningQuestions.getLiteSession(totalQuestions = QUESTIONS_PER_SESSION)
        } else {
            val usedIds = _usedQuestionIds.value[level] ?: emptySet()
            val source = prepareQuestionSource(level, usedIds)
            val mixed = buildMixedQuestionSet(source)
            val newUsedIds = usedIds + mixed.map { it.id }
            _usedQuestionIds.value = _usedQuestionIds.value + (level to newUsedIds)
            Log.d("ListeningViewModel", "Session started (mixed types) with ${mixed.size} questions for $level")
            mixed
        }
        
        _session.value = ListeningSession(
            difficulty = level,
            questions = selectedQuestions,
            currentQuestionIndex = 0,
            score = 0,
            mistakes = 0
        )
        _consecutiveCorrect.value = 0
        _shouldShowAd.value = false
        _lessonResult.value = null
        _clearedLevel.value = null
        // セッション開始時は初期速度にリセット
        _speechRate.value = 0.7f
        updateSpeechRate()
        loadNextQuestion()
    }
    
    /**
     * 次の問題を読み込み
     */
    private fun loadNextQuestion() {
        val currentSession = _session.value ?: return
        
        if (currentSession.currentQuestionIndex >= currentSession.questions.size) {
            // セッション完了
            _session.value = currentSession.copy(completed = true)
            finalizeLesson(currentSession)
            
            // セッション完了時に広告フラグを立てる（統一ルール：1セット完了 = 1回広告）
            _shouldShowAd.value = true
            Log.d("ListeningViewModel", "Session completed with score ${currentSession.score}/${currentSession.questions.size}, ad flag set")
            return
        }
        
        val question = currentSession.questions[currentSession.currentQuestionIndex]
        _currentQuestion.value = question
        _selectedWords.value = emptyList()
        
        // 正解単語を小文字に正規化
        val normalizedCorrectWords = question.words.map { it.lowercase() }
        
        // ダミー単語も小文字で取得し、重複を避ける
        val dummyWords = getDummyWords(normalizedCorrectWords, currentSession.difficulty)
        
        // 正解 + ダミー を小文字ベースで一意にする
        val allWords = (normalizedCorrectWords + dummyWords).distinct()
        _shuffledWords.value = allWords.shuffled()
        
        _showResult.value = false
        startTime = System.currentTimeMillis()
    }
    
    /**
     * ダミー単語を取得
     */
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
                "tungod", "apan", "bisan", "kung", "kay", "aron",
                "samtang", "hangtud", "sukad", "human", "usa"
            )
        }
        
        // プールを小文字化し、正解単語（小文字）と被らないものだけにする
        val availableDummies = dummyPool
            .map { it.lowercase() }
            .filter { it !in correctWords }
        
        val maxDummyCount = (MAX_PANEL_COUNT - correctWords.size).coerceAtLeast(0)
        if (maxDummyCount == 0) return emptyList()
        return availableDummies.shuffled().take(maxDummyCount)
    }
    
    /**
     * 音声再生
     */
    fun playAudio() {
        val question = _currentQuestion.value ?: return
        val pronunciation = question.pronunciation ?: question.phrase
        _isPlaying.value = true
        
        tts?.speak(pronunciation, TextToSpeech.QUEUE_FLUSH, null, null)
        
        // 再生終了を検知（簡易版）
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3秒後に再生終了とみなす
            _isPlaying.value = false
        }
    }
    
    /**
     * 単語を選択
     */
    fun selectWord(word: String) {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        current.add(word)
        _selectedWords.value = current
        
        // 全ての単語を選択したら自動チェック
        val question = _currentQuestion.value ?: return
        if (current.size == question.correctOrder.size) {
            checkAnswer()
        }
    }
    
    /**
     * 最後の単語を削除
     */
    fun removeLastWord() {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(current.lastIndex)
            _selectedWords.value = current
        }
    }
    
    /**
     * 指定位置の単語を削除（回答エリアから選択肢エリアに戻す）
     */
    fun removeWordAt(index: Int) {
        if (_showResult.value) return
        
        val current = _selectedWords.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedWords.value = current
        }
    }
    
    /**
     * 回答をチェック
     */
    private fun checkAnswer() {
        val question = _currentQuestion.value ?: return
        // 小文字ベースで正答判定（見た目やデータの大文字・小文字に影響されないようにする）
        val selected = _selectedWords.value.map { it.lowercase() }
        val correct = selected == question.correctOrder.map { it.lowercase() }
        
        _isCorrect.value = correct
        _showResult.value = true
        
        val currentSession = _session.value ?: return
        
        if (correct) {
            // 正解
            _session.value = currentSession.copy(
                score = currentSession.score + 1
            )
            
            // 連続正解数を増やす
            val newConsecutiveCorrect = _consecutiveCorrect.value + 1
            _consecutiveCorrect.value = newConsecutiveCorrect
            
            // 3問連続正解で速度を上げる（難易度アップ）
            if (newConsecutiveCorrect % CORRECT_STREAK_FOR_SPEEDUP == 0) {
                val newRate = (_speechRate.value + 0.1f).coerceAtMost(MAX_SPEECH_RATE)
                if (newRate != _speechRate.value) {
                    _speechRate.value = newRate
                    updateSpeechRate()
                    Log.d("ListeningViewModel", "Speed increased to ${_speechRate.value} after $newConsecutiveCorrect correct")
                }
            }
        } else {
            // 不正解
            _session.value = currentSession.copy(
                mistakes = currentSession.mistakes + 1
            )
            
            // 連続正解数をリセット
            _consecutiveCorrect.value = 0
            
            // 速度を下げる（難易度ダウン）
            val newRate = (_speechRate.value - 0.1f).coerceAtLeast(MIN_SPEECH_RATE)
            if (newRate != _speechRate.value) {
                _speechRate.value = newRate
                updateSpeechRate()
                Log.d("ListeningViewModel", "Speed decreased to ${_speechRate.value} after mistake")
            }
        }
    }
    
    /**
     * 次の問題へ
     */
    fun nextQuestion() {
        val currentSession = _session.value ?: return
        _session.value = currentSession.copy(
            currentQuestionIndex = currentSession.currentQuestionIndex + 1
        )
        loadNextQuestion()
    }
    
    /**
     * 広告表示完了後のリセット
     */
    fun onAdShown() {
        _shouldShowAd.value = false
        Log.d("ListeningViewModel", "Ad shown, flag reset")
    }
    
    private fun finalizeLesson(currentSession: ListeningSession) {
        val totalQuestions = currentSession.questions.size
        if (totalQuestions == 0) return
        val correct = currentSession.score
        val result = calculateLessonResult(correct, totalQuestions)
        if (_lessonResult.value == null) {
            _lessonResult.value = result
            viewModelScope.launch {
                val currentLevel = usageRepository.getCurrentLevel().first()
                usageRepository.addXP(result.xpEarned)
                val clearedLevel = if (result.leveledUp) {
                    usageRepository.incrementLevel()
                    currentLevel + 1
                } else {
                    currentLevel
                }
                _clearedLevel.value = clearedLevel
            }
        }
    }
    
    private fun calculateLessonResult(correctCount: Int, totalQuestions: Int): LessonResult {
        val baseXp = correctCount * 10
        val bonus = if (correctCount == totalQuestions && totalQuestions == QUESTIONS_PER_SESSION) 50 else 0
        val xp = baseXp + bonus
        val leveledUp = correctCount >= 8
        return LessonResult(
            correctCount = correctCount,
            totalQuestions = totalQuestions,
            xpEarned = xp,
            leveledUp = leveledUp
        )
    }
    
    private fun prepareQuestionSource(
        level: DifficultyLevel,
        usedIds: Set<String>
    ): List<ListeningQuestion> {
        val levelQuestions = ListeningQuestions.getQuestionsByLevel(level)
        val unusedLevelQuestions = levelQuestions.filterNot { it.id in usedIds }
        val baseSource = when {
            unusedLevelQuestions.size >= QUESTIONS_PER_SESSION -> unusedLevelQuestions
            levelQuestions.size >= QUESTIONS_PER_SESSION -> levelQuestions
            else -> ListeningQuestions.getAllQuestions()
        }
        if (unusedLevelQuestions.isEmpty() && levelQuestions.size < QUESTIONS_PER_SESSION) {
            // 全問消費済 → リセット
            _usedQuestionIds.value = _usedQuestionIds.value - level
        }
        return baseSource
    }
    
    private fun buildMixedQuestionSet(source: List<ListeningQuestion>): List<ListeningQuestion> {
        if (source.isEmpty()) return emptyList()
        val random = Random(System.currentTimeMillis())
        val distinctSource = source.distinctBy { it.id }.toMutableList()
        if (distinctSource.size < QUESTIONS_PER_SESSION) {
            val additional = ListeningQuestions.getAllQuestions()
                .filterNot { existing -> distinctSource.any { it.id == existing.id } }
                .shuffled(random)
            distinctSource += additional
        }
        val pool = distinctSource.shuffled(random).toMutableList()
        
        fun take(count: Int): MutableList<ListeningQuestion> {
            val actualCount = minOf(count, pool.size)
            val taken = pool.take(actualCount).toMutableList()
            pool.removeAll(taken)
            if (taken.size < count) {
                val refill = ListeningQuestions.getAllQuestions()
                    .filterNot { existing -> taken.any { it.id == existing.id } }
                    .shuffled(random)
                    .take(count - taken.size)
                taken += refill
            }
            return taken
        }
        
        val listening = take(LISTENING_COUNT).map { it.copy(type = QuestionType.LISTENING) }
        val translation = take(TRANSLATION_COUNT).map { it.copy(type = QuestionType.TRANSLATION) }
        val ordering = take(ORDERING_COUNT).map { it.copy(type = QuestionType.ORDERING) }
        
        val combined = (listening + translation + ordering).shuffled(random)
        return combined.take(QUESTIONS_PER_SESSION)
    }
    
    /**
     * 既出問題をリセット（デバッグ用）
     */
    fun resetUsedQuestions(level: DifficultyLevel? = null) {
        if (level != null) {
            _usedQuestionIds.value = _usedQuestionIds.value - level
            Log.d("ListeningViewModel", "Reset used questions for $level")
        } else {
            _usedQuestionIds.value = emptyMap()
            Log.d("ListeningViewModel", "Reset all used questions")
        }
    }
    
    /**
     * 既出問題の統計を取得
     */
    fun getUsedQuestionsStats(level: DifficultyLevel): Pair<Int, Int> {
        val allQuestions = ListeningQuestions.getQuestionsByLevel(level)
        val usedIds = _usedQuestionIds.value[level] ?: emptySet()
        return Pair(usedIds.size, allQuestions.size)
    }
    
    fun clearLessonCompletion() {
        _lessonResult.value = null
        _clearedLevel.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
