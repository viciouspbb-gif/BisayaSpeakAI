package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.voice.DojoSePlayer
import com.bisayaspeak.ai.voice.EnvironmentSoundPlayer
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.voice.Soundscape
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val LISTENING_DECK = listOf(
    ListeningQuestion(
        phrase = "Pila ka tawo?",
        meaning = "何人ですか？",
        distractors = listOf("Pila ka kilo?", "Pila ka tuig?")
    ),
    ListeningQuestion(
        phrase = "Walay sukli.",
        meaning = "お釣りがない。",
        distractors = listOf("Walay suli.", "Way lami.")
    ),
    ListeningQuestion(
        phrase = "Diretso lang.",
        meaning = "まっすぐ行って。",
        distractors = listOf("Delikado lang.", "Dali-dali lang.")
    ),
    ListeningQuestion(
        phrase = "Lami kaayo!",
        meaning = "すごく美味しい！",
        distractors = listOf("Layo kaayo!", "Lahi kaayo!")
    ),
    ListeningQuestion(
        phrase = "Unsa may imo?",
        meaning = "あなたは何にする？",
        distractors = listOf("Unsa may iyo?", "Unsa may ato?")
    )
)

class DojoListeningViewModel(
    application: Application,
    private val voiceService: GeminiVoiceService = GeminiVoiceService(application.applicationContext),
    private val environmentSoundPlayer: EnvironmentSoundPlayer = EnvironmentSoundPlayer(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        ListeningQuizUiState(totalQuestions = LISTENING_DECK.size)
    )
    val uiState: StateFlow<ListeningQuizUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null
    private var ambienceJob: Job? = null
    private var fadeJob: Job? = null
    private val sePlayer = DojoSePlayer()

    fun startTraining() {
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                hasStarted = true,
                currentQuestionIndex = 0,
                currentQuestion = LISTENING_DECK.first(),
                currentOptions = emptyList(),
                correctCount = 0,
                finished = false,
                previewingOption = null,
                answeredOption = null,
                answeredCorrect = null,
                selectedAnswer = null,
                isAnswerEnabled = false,
                streakCount = 0,
                roundState = DojoRoundState.LISTENING,
                currentCue = GeminiVoiceCue.DEFAULT
            )
        }
        launchQuestion(0)
        playIntroAmbience()
    }

    fun retry() {
        startTraining()
    }

    fun submitAnswer(option: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.roundState != DojoRoundState.ANSWERING) return
        if (!state.isAnswerEnabled) return
        if (state.answeredOption != null) return
        val isCorrect = option == question.phrase
        val updatedCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
        val newStreak = if (isCorrect) state.streakCount + 1 else 0
        _uiState.update {
            it.copy(
                correctCount = updatedCorrect,
                selectedAnswer = option,
                isAnswerEnabled = false,
                previewingOption = null,
                answeredOption = option,
                answeredCorrect = isCorrect,
                streakCount = newStreak,
                roundState = DojoRoundState.ANSWERED
            )
        }
        if (isCorrect) {
            sePlayer.playCorrect()
        } else {
            sePlayer.playIncorrect()
        }
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(1400)
            val nextIndex = state.currentQuestionIndex + 1
            if (nextIndex >= LISTENING_DECK.size) {
                finishSession()
            } else {
                launchQuestion(nextIndex)
            }
        }
    }

    fun previewOption(option: String) {
        val state = _uiState.value
        if (state.finished) return
        if (state.roundState != DojoRoundState.ANSWERING) return
        if (!state.isAnswerEnabled) return
        if (state.answeredOption != null) return
        voiceService.stop()
        voiceService.speak(
            text = option,
            cue = GeminiVoiceCue.DEFAULT,
            onStart = {
                _uiState.update { it.copy(previewingOption = option) }
            },
            onComplete = {
                _uiState.update { current ->
                    if (current.previewingOption == option) current.copy(previewingOption = null) else current
                }
            },
            onError = {
                _uiState.update { current ->
                    if (current.previewingOption == option) current.copy(previewingOption = null) else current
                }
            }
        )
    }

    fun replayCurrentQuestion() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (!state.hasStarted || state.finished) return
        startQuestionAmbience()
        voiceService.stop()
        voiceService.speak(
            text = question.phrase,
            cue = state.currentCue,
            onStart = {},
            onComplete = { stopQuestionAmbience() },
            onError = { stopQuestionAmbience() }
        )
    }

    private fun launchQuestion(index: Int) {
        val question = LISTENING_DECK[index]
        _uiState.update { current ->
            current.copy(
                currentQuestionIndex = index,
                currentQuestion = question,
                currentOptions = emptyList(),
                isAnswerEnabled = false,
                selectedAnswer = null,
                previewingOption = null,
                answeredOption = null,
                answeredCorrect = null,
                streakCount = current.streakCount,
                roundState = DojoRoundState.LISTENING
            )
        }
        startQuestionAmbience()
        voiceService.stop()
        val cue = randomCue()
        voiceService.speak(
            text = question.phrase,
            cue = cue,
            onStart = {
                _uiState.update { current -> current.copy(currentCue = cue) }
            },
            onComplete = {
                stopQuestionAmbience()
                _uiState.update {
                    it.copy(
                        currentOptions = question.combinedOptions(),
                        isAnswerEnabled = true,
                        roundState = DojoRoundState.ANSWERING
                    )
                }
            },
            onError = {
                stopQuestionAmbience()
                _uiState.update {
                    it.copy(
                        currentOptions = question.combinedOptions(),
                        isAnswerEnabled = true,
                        roundState = DojoRoundState.ANSWERING
                    )
                }
            }
        )
    }

    private fun finishSession() {
        voiceService.stop()
        _uiState.update { current ->
            current.copy(
                finished = true,
                currentQuestion = null,
                currentOptions = emptyList(),
                isAnswerEnabled = false,
                selectedAnswer = null,
                previewingOption = null,
                answeredOption = null,
                answeredCorrect = null,
                roundState = DojoRoundState.FINISHED
            )
        }
        stopQuestionAmbience(force = true)
    }

    private fun playIntroAmbience() {
        val soundscape = Soundscape.values().random()
        ambienceJob?.cancel()
        fadeJob?.cancel()
        ambienceJob = viewModelScope.launch {
            environmentSoundPlayer.play(soundscape)
            environmentSoundPlayer.setVolume(0.6f)
            delay(2500)
            environmentSoundPlayer.fadeOutAndStop(900)
        }
    }

    private fun startQuestionAmbience() {
        ambienceJob?.cancel()
        fadeJob?.cancel()
        ambienceJob = viewModelScope.launch {
            val soundscape = Soundscape.values().random()
            environmentSoundPlayer.play(soundscape)
            environmentSoundPlayer.setVolume(0.5f)
        }
    }

    private fun stopQuestionAmbience(force: Boolean = false) {
        if (force) {
            ambienceJob?.cancel()
            fadeJob?.cancel()
            environmentSoundPlayer.stop()
            return
        }
        fadeJob?.cancel()
        fadeJob = viewModelScope.launch {
            environmentSoundPlayer.fadeOutAndStop(800)
        }
    }

    override fun onCleared() {
        super.onCleared()
        advanceJob?.cancel()
        ambienceJob?.cancel()
        fadeJob?.cancel()
        voiceService.stop()
        environmentSoundPlayer.release()
        sePlayer.shutdown()
    }

    private fun randomCue(): GeminiVoiceCue {
        val candidates = listOf(
            GeminiVoiceCue.DEFAULT,
            GeminiVoiceCue.TALK_LOW,
            GeminiVoiceCue.TALK_HIGH
        )
        return candidates.random()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DojoListeningViewModel(app)
            }
        }
    }
}

data class ListeningQuestion(
    val phrase: String,
    val meaning: String,
    val distractors: List<String>
) {
    fun combinedOptions(): List<String> = (distractors + phrase).shuffled()
}

data class ListeningQuizUiState(
    val currentQuestionIndex: Int = -1,
    val currentQuestion: ListeningQuestion? = null,
    val currentOptions: List<String> = emptyList(),
    val isAnswerEnabled: Boolean = false,
    val selectedAnswer: String? = null,
    val previewingOption: String? = null,
    val answeredOption: String? = null,
    val answeredCorrect: Boolean? = null,
    val correctCount: Int = 0,
    val totalQuestions: Int = LISTENING_DECK.size,
    val finished: Boolean = false,
    val hasStarted: Boolean = false,
    val streakCount: Int = 0,
    val roundState: DojoRoundState = DojoRoundState.IDLE,
    val currentCue: GeminiVoiceCue = GeminiVoiceCue.DEFAULT
)

enum class DojoRoundState {
    IDLE,
    LISTENING,
    ANSWERING,
    ANSWERED,
    FINISHED
}
