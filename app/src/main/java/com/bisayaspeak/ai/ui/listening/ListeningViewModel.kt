package com.bisayaspeak.ai.ui.listening

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.UserPreferencesRepository
import com.bisayaspeak.ai.data.repository.WhisperRepository
import com.bisayaspeak.ai.utils.VoiceRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val currentTurn: Int = 0,
    val isFinished: Boolean = false
)

data class ChatMessage(
    val role: String,
    val text: String,
    val translation: String? = null,
    val options: List<OptionItem> = emptyList()
)

data class OptionItem(val text: String, val translation: String, val tone: String)

class ListeningViewModel(
    application: Application,
    private val whisperRepository: WhisperRepository = WhisperRepository(),
    private val chatRepository: OpenAiChatRepository = OpenAiChatRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val voiceRecorder = VoiceRecorder(application)
    private var isUserFemale: Boolean = true

    companion object {
        private const val MAX_TURNS = 8
        private const val CLIMAX_TURN = 6
    }

    init {
        observeUserGender()
    }

    fun setUserGender(isFemale: Boolean) {
        isUserFemale = isFemale
    }

    fun startRecording() {
        if (_uiState.value.isFinished) {
            _uiState.value = _uiState.value.copy(error = "この会話は完結しました。TOPへ戻って新しいレッスンを開始してください。")
            return
        }
        try {
            voiceRecorder.startRecording()
            _uiState.value = _uiState.value.copy(isRecording = true, error = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "録音エラー: ${e.message}")
        }
    }

    fun stopRecordingAndSend() {
        if (_uiState.value.isFinished) {
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                error = "ストーリーは完結しました。レッスン完了ボタンからTOPへ戻ってください。"
            )
            return
        }
        _uiState.value = _uiState.value.copy(isRecording = false, isLoading = true)
        val file = voiceRecorder.stopRecording()
        if (file != null && file.exists()) {
            processAudioFile(file)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "録音ファイルの取得に失敗しました")
        }
    }

    private fun processAudioFile(file: File) {
        viewModelScope.launch {
            try {
                val result = whisperRepository.transcribe(file)

                result.fold(
                    onSuccess = { transcript ->
                        val userText = transcript.trim()
                        if (userText.isBlank()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "音声を認識できませんでした。もう一度お試しください。"
                            )
                            return@fold
                        }

                        addMessage(role = "user", text = userText)
                        fetchAiResponse(userText)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "音声解析に失敗しました"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ListeningViewModel", "processAudioFile failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "不明なエラーが発生しました"
                )
            }
        }
    }

    private suspend fun fetchAiResponse(userText: String) {
        try {
            val rawResponse = generateAiResponse(userText)
            parseAndAddAiResponse(rawResponse)
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "fetchAiResponse failed", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.localizedMessage ?: "AI応答の取得に失敗しました"
            )
        }
    }

    private suspend fun generateAiResponse(userText: String): String {
        Log.d("GenderCheck", "Sending request. User isFemale: $isUserFemale")
        val addressTerm = if (isUserFemale) {
            "Gwapa (Beautiful Female)"
        } else {
            "Gwapo (Handsome Male)"
        }
        val shouldConclude = _uiState.value.currentTurn >= CLIMAX_TURN
        return chatRepository.generateListeningReply(
            userMessage = userText,
            isUserFemale = isUserFemale,
            addressTerm = addressTerm,
            shouldConclude = shouldConclude,
            farewellExamples = listOf("Sige, amping!", "Kita-kita ra ta puhon!")
        )
    }

    private fun parseAndAddAiResponse(jsonString: String) {
        try {
            val cleanJson = jsonString
                .replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()

            val jsonObject = JSONObject(cleanJson)
            val aiSpeech = jsonObject.optString("aiSpeech").orEmpty()
            val aiTranslation = jsonObject.optString("aiTranslation").orEmpty()
            val optionsArray = jsonObject.optJSONArray("options")

            val options = mutableListOf<OptionItem>()
            if (optionsArray != null) {
                for (i in 0 until optionsArray.length()) {
                    val option = optionsArray.optJSONObject(i) ?: continue
                    val text = option.optString("text").orEmpty()
                    val translation = option.optString("translation").orEmpty()
                    val tone = option.optString("tone").orEmpty()
                    if (text.isNotBlank()) {
                        options += OptionItem(text = text, translation = translation, tone = tone)
                    }
                }
            }

            addMessage(
                role = "assistant",
                text = aiSpeech.ifBlank { "..." },
                translation = aiTranslation.ifBlank { null },
                options = options
            )

            _uiState.value = _uiState.value.copy(isLoading = false, error = null)
        } catch (e: Exception) {
            Log.e("ListeningViewModel", "JSON parse failed", e)
            addMessage(
                role = "assistant",
                text = jsonString,
                translation = null,
                options = emptyList()
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "AI応答の解析に失敗しました"
            )
        }
    }

    private fun addMessage(role: String, text: String, translation: String? = null, options: List<OptionItem> = emptyList()) {
        val current = _uiState.value.messages.toMutableList()
        current.add(ChatMessage(role = role, text = text, translation = translation, options = options))
        val nextTurn = (_uiState.value.currentTurn + 1).coerceAtMost(MAX_TURNS)
        val reachedLimit = role == "assistant" && nextTurn >= MAX_TURNS
        _uiState.value = _uiState.value.copy(
            messages = current,
            currentTurn = nextTurn,
            isFinished = _uiState.value.isFinished || reachedLimit
        )
    }

    private fun observeUserGender() {
        viewModelScope.launch {
            userPreferencesRepository.userGender.collect { gender ->
                isUserFemale = gender == UserGender.FEMALE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
    }
}
