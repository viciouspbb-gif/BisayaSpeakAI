package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.voice.EnvironmentSoundPlayer
import com.bisayaspeak.ai.voice.EnvironmentVolumePreset
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
    private var soundJob: Job? = null

    fun selectSoundscape(soundscape: Soundscape) {
        _uiState.update { it.copy(selectedSoundscape = soundscape) }
        ensureEnvironmentLoop()
    }

    fun startTraining() {
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                currentQuestionIndex = 0,
                currentQuestion = LISTENING_DECK.first(),
                currentOptions = emptyList(),
                correctCount = 0,
                finished = false,
                selectedAnswer = null,
                isAnswerEnabled = false,
                expression = TariExpression.Neutral,
                feedbackMessage = "環境音を聞いてタリの号令を待とう"
            )
        }
        launchQuestion(0)
    }

    fun retry() {
        startTraining()
    }

    fun submitAnswer(option: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (!state.isAnswerEnabled) return
        val isCorrect = option == question.phrase
        val updatedCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
        _uiState.update {
            it.copy(
                correctCount = updatedCorrect,
                selectedAnswer = option,
                isAnswerEnabled = false,
                feedbackMessage = if (isCorrect) {
                    "✨ 正解！耳が冴えてる！"
                } else {
                    "修行が足りん！正解は \"${question.phrase}\""
                },
                expression = if (isCorrect) TariExpression.Happy else TariExpression.Frustrated
            )
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
        if (!state.isAnswerEnabled) return
        voiceService.stop()
        voiceService.speak(
            text = option,
            cue = GeminiVoiceCue.DEFAULT,
            onStart = {
                _uiState.update { it.copy(isTtsPlaying = true) }
            },
            onComplete = {
                _uiState.update { it.copy(isTtsPlaying = false) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isTtsPlaying = false,
                        feedbackMessage = error.message ?: "音声再生に失敗しました"
                    )
                }
            }
        )
    }

    private fun launchQuestion(index: Int) {
        ensureEnvironmentLoop()
        val question = LISTENING_DECK[index]
        _uiState.update {
            it.copy(
                currentQuestionIndex = index,
                currentQuestion = question,
                currentOptions = emptyList(),
                isAnswerEnabled = false,
                selectedAnswer = null,
                feedbackMessage = "タリが読み上げ中…",
                expression = TariExpression.Neutral
            )
        }
        voiceService.stop()
        val cue = randomCue()
        voiceService.speak(
            text = question.phrase,
            cue = cue,
            onStart = {
                _uiState.update { it.copy(isTtsPlaying = true) }
            },
            onComplete = {
                _uiState.update {
                    it.copy(
                        isTtsPlaying = false,
                        currentOptions = question.combinedOptions(),
                        isAnswerEnabled = true,
                        feedbackMessage = "聞こえたフレーズを選んで！"
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isTtsPlaying = false,
                        currentOptions = question.combinedOptions(),
                        isAnswerEnabled = true,
                        feedbackMessage = error.message ?: "TTS再生に失敗しました。選択肢で回答してください。"
                    )
                }
            }
        )
    }

    private fun finishSession() {
        voiceService.stop()
        _uiState.update {
            it.copy(
                finished = true,
                currentQuestion = null,
                currentOptions = emptyList(),
                feedbackMessage = "修行完了！もう一度挑戦する？",
                expression = TariExpression.Happy
            )
        }
        environmentSoundPlayer.pause()
    }

    private fun ensureEnvironmentLoop() {
        val state = _uiState.value
        if (state.finished || state.currentQuestionIndex < 0) return
        soundJob?.cancel()
        soundJob = viewModelScope.launch {
            environmentSoundPlayer.play(state.selectedSoundscape, state.volumePreset)
        }
    }

    fun setVolumePreset(preset: EnvironmentVolumePreset) {
        _uiState.update { it.copy(volumePreset = preset) }
        environmentSoundPlayer.setVolume(preset.volume)
    }

    override fun onCleared() {
        super.onCleared()
        advanceJob?.cancel()
        soundJob?.cancel()
        voiceService.stop()
        environmentSoundPlayer.release()
    }

    private fun randomCue(): GeminiVoiceCue {
        val candidates = listOf(
            GeminiVoiceCue.DEFAULT,
            GeminiVoiceCue.WHISPER,
            GeminiVoiceCue.HIGH_PITCH,
            GeminiVoiceCue.LOW_PITCH,
            GeminiVoiceCue.ALIEN
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
    val selectedSoundscape: Soundscape = Soundscape.JEEPNEY,
    val volumePreset: EnvironmentVolumePreset = EnvironmentVolumePreset.STANDARD,
    val currentQuestionIndex: Int = -1,
    val currentQuestion: ListeningQuestion? = null,
    val currentOptions: List<String> = emptyList(),
    val isTtsPlaying: Boolean = false,
    val isAnswerEnabled: Boolean = false,
    val selectedAnswer: String? = null,
    val correctCount: Int = 0,
    val totalQuestions: Int = LISTENING_DECK.size,
    val finished: Boolean = false,
    val feedbackMessage: String? = null,
    val expression: TariExpression = TariExpression.Neutral
)

enum class TariExpression(val emoji: String, val caption: String) {
    Neutral("😐", "集中中"),
    Happy("😄", "ご機嫌タリ"),
    Frustrated("😖", "ビンタ寸前")
}
