package com.bisayaspeak.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.voice.VoiceInputRecorder
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PASSING_SCORE = 80

sealed interface DojoStatus {
    object Idle : DojoStatus
    object PlayingSample : DojoStatus
    object Recording : DojoStatus
    object Processing : DojoStatus
    data class Error(val message: String) : DojoStatus
}

data class SamplePhrase(
    val bisaya: String,
    val japanese: String,
    val romanized: String
)

data class PracticeResult(
    val transcript: String,
    val score: Int,
    val feedback: String,
    val passed: Boolean
)

data class TariDojoUiState(
    val sample: SamplePhrase = SamplePhrase(
        bisaya = "Maayong buntag",
        japanese = "おはよう",
        romanized = "マアヨン ブンタッグ"
    ),
    val status: DojoStatus = DojoStatus.Idle,
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val isPlayingSample: Boolean = false,
    val result: PracticeResult? = null
)

class TariDojoViewModel(
    application: Application,
    private val whisperRepository: WhisperRepository = WhisperRepository(),
    private val voiceService: GeminiVoiceService = GeminiVoiceService(application.applicationContext),
    private val voiceRecorder: VoiceInputRecorder = VoiceInputRecorder(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TariDojoUiState())
    val uiState: StateFlow<TariDojoUiState> = _uiState.asStateFlow()

    private var currentRecording: File? = null

    fun playSample() {
        val phrase = _uiState.value.sample
        voiceService.speak(
            text = phrase.bisaya,
            cue = GeminiVoiceCue.DEFAULT,
            onStart = {
                _uiState.update { it.copy(isPlayingSample = true, status = DojoStatus.PlayingSample) }
            },
            onComplete = {
                _uiState.update { it.copy(isPlayingSample = false, status = DojoStatus.Idle) }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isPlayingSample = false,
                        status = DojoStatus.Error(error.message ?: "お手本の再生に失敗しました")
                    )
                }
            }
        )
    }

    fun toggleRecording() {
        val state = _uiState.value
        if (state.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (_uiState.value.isProcessing) return
        try {
            val file = voiceRecorder.startRecording()
            currentRecording = file
            _uiState.update {
                it.copy(
                    isRecording = true,
                    status = DojoStatus.Recording,
                    result = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    status = DojoStatus.Error(e.message ?: "録音の準備中です。もう一度タップしてください。")
                )
            }
        }
    }

    private fun stopRecording() {
        if (!_uiState.value.isRecording) return
        val recorded = try {
            voiceRecorder.stopRecording()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    status = DojoStatus.Error(e.message ?: "録音が中断されました。もう一度タップしてください。")
                )
            }
            null
        }
        _uiState.update { it.copy(isRecording = false) }
        if (recorded == null || !recorded.exists() || recorded.length() == 0L) {
            _uiState.update {
                it.copy(
                    status = DojoStatus.Error("音声が検出できませんでした。もう一度タップしてください。")
                )
            }
            return
        }
        currentRecording = recorded
        viewModelScope.launch {
            scoreRecording(recorded)
        }
    }

    private suspend fun scoreRecording(file: File) {
        _uiState.update { it.copy(isProcessing = true, status = DojoStatus.Processing) }
        try {
            val transcript = whisperRepository.transcribe(file).getOrNull().orEmpty().trim()
            file.delete()
            currentRecording = null
            if (transcript.isBlank()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        status = DojoStatus.Error("音声が検出できませんでした。もう一度タップしてください。")
                    )
                }
                return
            }
            val sample = _uiState.value.sample
            val score = calculateScore(sample.bisaya, transcript)
            val passed = score >= PASSING_SCORE
            val feedback = if (passed) {
                "合格（仏）: タリ「完璧だよ！」"
            } else {
                "不合格（鬼）: タリ「もっと腹から声出して！」"
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    status = DojoStatus.Idle,
                    result = PracticeResult(transcript = transcript, score = score, feedback = feedback, passed = passed)
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    status = DojoStatus.Error(e.message ?: "採点に失敗しました。再度お試しください。")
                )
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(result = null, status = DojoStatus.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            voiceRecorder.cancelRecording()
        } catch (_: Exception) {
        }
        currentRecording?.delete()
        voiceService.stop()
    }

    private fun calculateScore(targetRaw: String, actualRaw: String): Int {
        val target = normalize(targetRaw)
        val actual = normalize(actualRaw)
        val maxLen = max(target.length, actual.length).coerceAtLeast(1)
        val distance = levenshtein(target, actual)
        val score = (((maxLen - distance).toDouble() / maxLen) * 100).roundToInt()
        return min(100, max(0, score))
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace("á", "a")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val costs = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            var lastValue = i
            costs[0] = i + 1
            for (j in b.indices) {
                val newValue = if (a[i] == b[j]) {
                    lastValue
                } else {
                    min(min(costs[j], costs[j + 1]), lastValue) + 1
                }
                lastValue = costs[j + 1]
                costs[j + 1] = newValue
            }
        }
        return costs[b.length]
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                TariDojoViewModel(application)
            }
        }
    }
}
