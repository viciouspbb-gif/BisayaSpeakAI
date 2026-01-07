package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.local.Question
import com.bisayaspeak.ai.data.listening.ListeningQuestion
import com.bisayaspeak.ai.data.listening.ListeningSession
import com.bisayaspeak.ai.data.listening.QuestionType
import com.bisayaspeak.ai.data.model.DifficultyLevel
import com.bisayaspeak.ai.data.model.LessonResult
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

class ListeningViewModel(
    application: Application,
    private val questionRepository: QuestionRepository,
    private val userProgressRepository: UserProgressRepository
) : AndroidViewModel(application) {
    
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
    
    // フクロウ先生の声設定（一定速度）
    private val _speechRate = MutableStateFlow(0.9f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()
    
    // 連続正解数
    private val _consecutiveCorrect = MutableStateFlow(0)
    val consecutiveCorrect: StateFlow<Int> = _consecutiveCorrect.asStateFlow()
    
    private val _comboCount = MutableStateFlow(0)
    val comboCount: StateFlow<Int> = _comboCount.asStateFlow()
    private val _voiceHintRemaining = MutableStateFlow(MAX_VOICE_HINTS)
    val voiceHintRemaining: StateFlow<Int> = _voiceHintRemaining.asStateFlow()
    private val _showHintRecoveryDialog = MutableStateFlow(false)
    val showHintRecoveryDialog: StateFlow<Boolean> = _showHintRecoveryDialog.asStateFlow()
    
    private var tts: TextToSpeech? = null
    private val soundPool: SoundPool
    private var correctSoundId: Int = 0
    private val usageRepository = UsageRepository(application)
    private var startTime: Long = 0
    private var currentLevel: Int = 1
    
    companion object {
        private const val QUESTIONS_PER_SESSION = 10 // 1セッションあたりの問題数
        private const val LISTENING_COUNT = 4
        private const val TRANSLATION_COUNT = 3
        private const val ORDERING_COUNT = 3
        private const val MAX_PANEL_COUNT = 8
        private const val MAX_VOICE_HINTS = 3
        private const val FIXED_SPEECH_RATE = 0.9f // フクロウ先生設定（固定）
        private const val CORRECT_STREAK_FOR_SPEEDUP = 3 // 連続正解数（現在は未使用）
        private const val PASSING_RATE = 0.8f
    }

    fun dismissHintRecoveryDialog() {
        _showHintRecoveryDialog.value = false
    }

    fun onHintRecoveryEarned() {
        _voiceHintRemaining.value = MAX_VOICE_HINTS
        _showHintRecoveryDialog.value = false
    }
    
    init {
        soundPool = SoundPool.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setMaxStreams(1)
            .build()
        loadComboSound()
        initTTS()
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
                val tagalogLocale = Locale("fil", "PH")
                val fallbackLocale = Locale("in", "ID")
                val primaryResult = tts?.setLanguage(tagalogLocale)
                val primarySupported = primaryResult != TextToSpeech.LANG_MISSING_DATA &&
                        primaryResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!primarySupported) {
                    Log.w("ListeningViewModel", "Tagalog TTS not supported, trying Indonesian fallback")
                    tts = TextToSpeech(getApplication()) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            tts?.let { instance ->
                                var result = instance.setLanguage(Locale("id"))
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    result = instance.setLanguage(Locale("fil"))
                                }
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    instance.setLanguage(Locale.US)
                                }
                                instance.setPitch(0.7f)
                                instance.setSpeechRate(FIXED_SPEECH_RATE)
                                _speechRate.value = FIXED_SPEECH_RATE
                                Log.d("ListeningViewModel", "TTS initialized with fixed speech rate: $FIXED_SPEECH_RATE")
                            }
                        } else {
                            Log.e("ListeningViewModel", "TTS initialization failed: $status")
                        }
                    }
                }
            } else {
                Log.e("ListeningViewModel", "TTS initialization failed: $status")
            }
        }
    }
    
    private fun playComboSound(combo: Int) {
        if (correctSoundId == 0) return
        val pitch = (1f + (combo - 1) * 0.08f).coerceIn(1f, 1.6f)
        soundPool.play(correctSoundId, 1f, 1f, 1, 0, pitch)
    }
    
    /**
     * 音声速度を更新（固定速度）
     */
    private fun updateSpeechRate() {
        tts?.setSpeechRate(FIXED_SPEECH_RATE)
        _speechRate.value = FIXED_SPEECH_RATE
        Log.d("ListeningViewModel", "Speech rate set to fixed value: $FIXED_SPEECH_RATE")
    }
    
    fun loadQuestions(level: Int) {
        viewModelScope.launch {
            Log.d("ListeningViewModel", "Loading questions for level $level")
            currentLevel = level
            val difficulty = level.toDifficultyLevel()
            
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
            val sessionQuestions = buildSessionQuestions(listeningQuestions)
            Log.d("ListeningViewModel", "Built session with ${sessionQuestions.size} questions")

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
            _voiceHintRemaining.value = MAX_VOICE_HINTS
            _showHintRecoveryDialog.value = false
            updateSpeechRate()

            if (sessionQuestions.isNotEmpty()) {
                loadNextQuestion()
            }
        }
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
        _showHintRecoveryDialog.value = false
        
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
            .map { candidate -> candidate.lowercase() }
            .filter { candidate -> candidate !in correctWords }
        
        val maxDummyCount = (MAX_PANEL_COUNT - correctWords.size).coerceAtLeast(0)
        if (maxDummyCount == 0) return emptyList()
        return availableDummies.shuffled().take(maxDummyCount)
    }
    
    /**
     * ヒントリクエスト処理（UIからの呼び出し用）
     */
    fun processHintRequest() {
        if (_voiceHintRemaining.value <= 0) {
            _showHintRecoveryDialog.value = true
        } else {
            playAudio()
        }
    }

    /**
     * 音声ヒント再生
     */
    fun playAudio() {
        if (_voiceHintRemaining.value <= 0) {
            _showHintRecoveryDialog.value = true
            return
        }
        _voiceHintRemaining.value = (_voiceHintRemaining.value - 1).coerceAtLeast(0)
        performAudioPlayback()
    }

    private fun performAudioPlayback() {
        val question = _currentQuestion.value ?: return
        val pronunciation = question.pronunciation ?: question.phrase
        _isPlaying.value = true
        
        tts?.let {
            it.setPitch(0.7f)
            it.setSpeechRate(FIXED_SPEECH_RATE)
            it.speak(pronunciation, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        
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
            }
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

    private fun buildSessionQuestions(allQuestions: List<ListeningQuestion>): List<ListeningQuestion> {
        if (allQuestions.isEmpty()) return emptyList()

        // まずIDで重複を排除（確実な一意性保証）
        val distinctQuestions = allQuestions.distinctBy { it.phrase }
        if (distinctQuestions.size != allQuestions.size) {
            Log.w("ListeningViewModel", "Removed duplicates: ${allQuestions.size} -> ${distinctQuestions.size}")
        }

        val listeningPool = distinctQuestions.filter { question -> question.type == QuestionType.LISTENING }
        val translationPool = distinctQuestions.filter { question -> question.type == QuestionType.TRANSLATION }
        val orderingPool = distinctQuestions.filter { question -> question.type == QuestionType.ORDERING }

        val combined = mutableListOf<ListeningQuestion>().apply {
            addAll(selectFromPool(listeningPool, LISTENING_COUNT))
            addAll(selectFromPool(translationPool, TRANSLATION_COUNT))
            addAll(selectFromPool(orderingPool, ORDERING_COUNT))
        }

        if (combined.isEmpty()) {
            combined += replicateFromPool(distinctQuestions, QUESTIONS_PER_SESSION)
        }

        if (combined.size < QUESTIONS_PER_SESSION) {
            combined += replicateFromPool(distinctQuestions, QUESTIONS_PER_SESSION - combined.size)
        }

        // 最終的な重複チェックとシャッフル
        val finalQuestions = combined.shuffled().take(QUESTIONS_PER_SESSION)
        val finalDistinct = finalQuestions.distinctBy { it.phrase }
        
        if (finalDistinct.size != finalQuestions.size) {
            Log.w("ListeningViewModel", "Final session had duplicates, ensuring uniqueness")
            // 重複がある場合はユニークな問題で埋める
            val additionalQuestions = distinctQuestions.filter { 
                !finalDistinct.contains(it) 
            }.shuffled()
            val result = (finalDistinct + additionalQuestions).take(QUESTIONS_PER_SESSION)
            Log.d("ListeningViewModel", "Session questions: ${result.size} unique questions")
            return result
        }
        
        Log.d("ListeningViewModel", "Session built with ${finalQuestions.size} unique questions")
        return finalQuestions
    }

    private fun selectFromPool(
        pool: List<ListeningQuestion>,
        desiredCount: Int
    ): MutableList<ListeningQuestion> {
        if (pool.isEmpty() || desiredCount <= 0) return mutableListOf()
        val result = pool.shuffled().take(desiredCount).toMutableList()
        if (result.size < desiredCount) {
            result += replicateFromPool(pool, desiredCount - result.size)
        }
        return result
    }

    private fun replicateFromPool(
        pool: List<ListeningQuestion>,
        needed: Int
    ): List<ListeningQuestion> {
        if (pool.isEmpty() || needed <= 0) return emptyList()
        val result = mutableListOf<ListeningQuestion>()
        var index = 0
        while (result.size < needed) {
            val base = pool[index % pool.size]
            val duplicateId = "${base.id}_dup_${index}_${Random.nextInt(1_000_000)}"
            result += base.copy(id = duplicateId)
            index++
        }
        return result
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

    fun clearLessonCompletion() {
        _lessonResult.value = null
        _clearedLevel.value = null
    }
}
