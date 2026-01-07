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
import kotlin.random.Random
import kotlin.math.roundToInt

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
    
    // 正解率に応じた音声速度
    private val _speechRate = MutableStateFlow(0.7f)
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
        private const val MIN_SPEECH_RATE = 0.6f // 最低速度（遅い）
        private const val MAX_SPEECH_RATE = 1.0f // 最高速度（通常）
        private const val CORRECT_STREAK_FOR_SPEEDUP = 3 // 速度を上げる連続正解数

        private val dummyListeningQuestions = listOf(
            createDummyQuestion(
                id = "dummy_listening_1",
                phrase = "Maayong buntag bisaya",
                meaning = "おはようございます",
                type = QuestionType.LISTENING
            ),
            createDummyQuestion(
                id = "dummy_listening_2",
                phrase = "Kumusta ka karon",
                meaning = "今の調子はどう？",
                type = QuestionType.LISTENING
            ),
            createDummyQuestion(
                id = "dummy_listening_3",
                phrase = "Palihug tabangi ko",
                meaning = "助けてください",
                type = QuestionType.LISTENING
            ),
            createDummyQuestion(
                id = "dummy_listening_4",
                phrase = "Pila ang oras",
                meaning = "今何時ですか？",
                type = QuestionType.LISTENING
            )
        )

        private val dummyTranslationQuestions = listOf(
            createDummyQuestion(
                id = "dummy_translation_1",
                phrase = "Maayong gabii",
                meaning = "こんばんは",
                type = QuestionType.TRANSLATION
            ),
            createDummyQuestion(
                id = "dummy_translation_2",
                phrase = "Gusto ko kape",
                meaning = "コーヒーが欲しいです",
                type = QuestionType.TRANSLATION
            ),
            createDummyQuestion(
                id = "dummy_translation_3",
                phrase = "Asa ang banyo",
                meaning = "トイレはどこですか？",
                type = QuestionType.TRANSLATION
            ),
            createDummyQuestion(
                id = "dummy_translation_4",
                phrase = "Salamat kaayo",
                meaning = "本当にありがとう",
                type = QuestionType.TRANSLATION
            )
        )

        private val dummyOrderingQuestions = listOf(
            createDummyQuestion(
                id = "dummy_ordering_1",
                phrase = "Unsa imong pangalan",
                meaning = "あなたの名前は何ですか？",
                type = QuestionType.ORDERING
            ),
            createDummyQuestion(
                id = "dummy_ordering_2",
                phrase = "Kanus-a ka moabot",
                meaning = "いつ到着しますか？",
                type = QuestionType.ORDERING
            ),
            createDummyQuestion(
                id = "dummy_ordering_3",
                phrase = "Pwede ko mangutana",
                meaning = "質問してもいいですか？",
                type = QuestionType.ORDERING
            ),
            createDummyQuestion(
                id = "dummy_ordering_4",
                phrase = "Dili ko gusto ana",
                meaning = "それは好きではありません",
                type = QuestionType.ORDERING
            )
        )

        private fun createDummyQuestion(
            id: String,
            phrase: String,
            meaning: String,
            type: QuestionType
        ): ListeningQuestion {
            val tokens = phrase.split(" ").filter { it.isNotBlank() }
            return ListeningQuestion(
                id = id,
                phrase = phrase,
                words = tokens,
                correctOrder = tokens,
                meaning = meaning,
                type = type
            )
        }
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
    
    private fun playComboSound(combo: Int) {
        if (correctSoundId == 0) return
        val pitch = (1f + (combo - 1) * 0.08f).coerceIn(1f, 1.6f)
        soundPool.play(correctSoundId, 1f, 1f, 1, 0, pitch)
    }
    
    /**
     * 音声速度を更新
     */
    private fun updateSpeechRate() {
        tts?.setSpeechRate(_speechRate.value)
        Log.d("ListeningViewModel", "Speech rate updated to ${_speechRate.value}")
    }
    
    fun loadQuestions(level: Int) {
        viewModelScope.launch {
            currentLevel = level
            val difficulty = level.toDifficultyLevel()
            val questions = questionRepository.getQuestionsByLevel(level)
            val listeningQuestions = questions.map { question -> question.toListeningQuestion() }
            val sessionQuestions = buildSessionQuestions(listeningQuestions)

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
            _speechRate.value = 0.7f
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
            
            // 連続正解数を増やす
            val newConsecutiveCorrect = _consecutiveCorrect.value + 1
            _consecutiveCorrect.value = newConsecutiveCorrect
            val newCombo = _comboCount.value + 1
            _comboCount.value = newCombo
            playComboSound(newCombo)
            
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
            _comboCount.value = 0
            
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

                val starsEarned = calculateStars(correct, totalQuestions)
                userProgressRepository.markLevelCompleted(this@ListeningViewModel.currentLevel, starsEarned)
                if (starsEarned > 0) {
                    userProgressRepository.unlockLevel(this@ListeningViewModel.currentLevel + 1)
                }
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
        val listeningPool = allQuestions.filter { question -> question.type == QuestionType.LISTENING }
        val translationPool = allQuestions.filter { question -> question.type == QuestionType.TRANSLATION }
        val orderingPool = allQuestions.filter { question -> question.type == QuestionType.ORDERING }

        val selectedListening = selectWithFallback(
            primary = listeningPool,
            fallback = dummyListeningQuestions,
            desiredCount = LISTENING_COUNT
        )
        val selectedTranslation = selectWithFallback(
            primary = translationPool,
            fallback = dummyTranslationQuestions,
            desiredCount = TRANSLATION_COUNT
        )
        val selectedOrdering = selectWithFallback(
            primary = orderingPool,
            fallback = dummyOrderingQuestions,
            desiredCount = ORDERING_COUNT
        )

        val combined = mutableListOf<ListeningQuestion>().apply {
            addAll(selectedListening)
            addAll(selectedTranslation)
            addAll(selectedOrdering)
        }

        return ensureUniqueSessionQuestions(
            candidates = combined,
            desiredCount = QUESTIONS_PER_SESSION
        )
    }

    private fun selectWithFallback(
        primary: List<ListeningQuestion>,
        fallback: List<ListeningQuestion>,
        desiredCount: Int
    ): List<ListeningQuestion> {
        if (desiredCount <= 0) return emptyList()
        val result = primary.shuffled().take(desiredCount).toMutableList()
        if (result.size >= desiredCount) return result

        if (fallback.isEmpty()) return result
        var fallbackIndex = 0
        while (result.size < desiredCount) {
            val base = fallback[fallbackIndex % fallback.size]
            val uniqueId = "${base.id}_${Random.nextInt(1_000_000)}"
            result += base.copy(id = uniqueId)
            fallbackIndex++
        }
        return result
    }

    private fun ensureUniqueSessionQuestions(
        candidates: List<ListeningQuestion>,
        desiredCount: Int
    ): List<ListeningQuestion> {
        if (desiredCount <= 0) return emptyList()

        val uniqueMap = LinkedHashMap<String, ListeningQuestion>()
        candidates.forEach { question ->
            val key = question.phrase.trim().lowercase()
            uniqueMap.putIfAbsent(key, question)
        }

        if (uniqueMap.size < desiredCount) {
            val fallbackPool = dummyListeningQuestions + dummyTranslationQuestions + dummyOrderingQuestions
            var fallbackIndex = 0
            var attempts = 0
            val maxAttempts = (fallbackPool.size.coerceAtLeast(1)) * 3

            while (uniqueMap.size < desiredCount && fallbackPool.isNotEmpty() && attempts < maxAttempts) {
                val base = fallbackPool[fallbackIndex % fallbackPool.size]
                val key = base.phrase.trim().lowercase()
                if (!uniqueMap.containsKey(key)) {
                    val uniqueId = "${base.id}_${Random.nextInt(1_000_000)}"
                    uniqueMap[key] = base.copy(id = uniqueId)
                }
                fallbackIndex++
                attempts++
            }
        }

        return uniqueMap.values.shuffled().take(desiredCount)
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
