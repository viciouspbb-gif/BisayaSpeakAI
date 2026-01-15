package com.bisayaspeak.ai.ui.roleplay

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.model.MissionHistoryMessage
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val translation: String? = null,
    val voiceCue: GeminiVoiceCue? = null
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun RoleplayChatScreen(
    scenarioId: String,
    onBackClick: () -> Unit,
    isProVersion: Boolean = false,
    onSaveAndExit: (List<MissionHistoryMessage>) -> Unit,
    viewModel: RoleplayChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }
    var initialLineSpoken by remember(scenarioId) { mutableStateOf(false) }

    val audioPermissionGrantedState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPermissionRequest by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGrantedState.value = granted
        if (granted && pendingPermissionRequest) {
            viewModel.startVoiceRecording()
        }
        pendingPermissionRequest = false
    }

    val ttsLogTag = remember { "RoleplayTTS" }

    LaunchedEffect(Unit) {
        Log.d(ttsLogTag, "Screen entered -> stopping all previous GeminiVoiceService instances")
        GeminiVoiceService.stopAllActive()
    }

    val speakAiLine = remember(voiceService) {
        { text: String, cue: GeminiVoiceCue?, messageId: String? ->
            val speechText = primarySpeechText(text)
            if (speechText.isNotBlank()) {
                Log.d(ttsLogTag, "AI speak request id=$messageId length=${text.length}")
            }
            voiceService.speak(
                text = speechText,
                cue = cue ?: GeminiVoiceCue.HIGH_PITCH,
                onStart = {
                    Log.d(ttsLogTag, "AI speak started id=$messageId")
                    messageId?.let { viewModel.notifyVoicePlaybackStarted(it) }
                },
                onComplete = {
                    Log.d(ttsLogTag, "AI speak completed id=$messageId")
                    messageId?.let { viewModel.notifyVoicePlaybackFinished(it) }
                },
                onError = {
                    Log.w(ttsLogTag, "AI speak error id=$messageId", it)
                    messageId?.let { viewModel.notifyVoicePlaybackFinished(it) }
                }
            )
        }
    }

    val speakUserPreview = remember(voiceService) {
        { text: String ->
            if (text.isNotBlank()) {
                Log.d(ttsLogTag, "User preview request length=${text.length}")
                voiceService.speak(
                    text = text,
                    cue = GeminiVoiceCue.DEFAULT,
                    onStart = { Log.d(ttsLogTag, "User preview started length=${text.length}") },
                    onComplete = { Log.d(ttsLogTag, "User preview completed length=${text.length}") },
                    onError = { Log.w(ttsLogTag, "User preview error length=${text.length}", it) }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(ttsLogTag, "RoleplayChatScreen disposed - stopping TTS playback only")
            voiceService.stop()
        }
    }

    LaunchedEffect(isProVersion) {
        viewModel.setProAccess(isProVersion)
    }

    LaunchedEffect(scenarioId, isProVersion) {
        viewModel.loadScenario(scenarioId, isProVersion)
    }

    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }

    LaunchedEffect(latestAiLine?.id) {
        latestAiLine?.let { message ->
            speakAiLine(message.text, message.voiceCue, message.id)
            initialLineSpoken = true
        }
    }

    LaunchedEffect(uiState.currentScenario?.initialMessage, initialLineSpoken) {
        val initialLine = uiState.currentScenario?.initialMessage
        if (!initialLineSpoken && !initialLine.isNullOrBlank()) {
            speakAiLine(initialLine, GeminiVoiceCue.HIGH_PITCH, null)
            initialLineSpoken = true
        }
    }

    val pendingExitHistory = uiState.pendingExitHistory

    LaunchedEffect(pendingExitHistory) {
        pendingExitHistory?.let {
            onSaveAndExit(it)
            viewModel.consumePendingExitHistory()
        }
    }

    val scenarioTitle = uiState.currentScenario?.title ?: "タリとの散歩道"
    val isSavingHistory = uiState.isSavingHistory
    val saveHistoryError = uiState.saveHistoryError

    var activeDragOptionId by remember { mutableStateOf<String?>(null) }
    val dropZoneBounds = remember { mutableStateOf<Rect?>(null) }

    val screenScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scenarioTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveAndExit()
                        },
                        enabled = !isSavingHistory
                    ) {
                        if (isSavingHistory) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "散歩を保存してTOPへ",
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF172554),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(screenScrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StageSection(
                latestAiLine = latestAiLine,
                speakingMessageId = speakingMessageId,
                initialLine = uiState.currentScenario?.initialMessage,
                onReplayRequest = {
                    latestAiLine?.let { message ->
                        speakAiLine(message.text, message.voiceCue, message.id)
                    } ?: uiState.currentScenario?.initialMessage?.let { intro ->
                        speakAiLine(intro, GeminiVoiceCue.HIGH_PITCH, null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            Spacer(modifier = Modifier.height(16.dp))

            DropConfirmationTray(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .onGloballyPositioned { dropZoneBounds.value = it.windowRect() },
                isHighlighted = activeDragOptionId != null,
                lockedOption = uiState.lockedOption
            )

            Spacer(modifier = Modifier.height(16.dp))

            ResponsePanel(
                modifier = Modifier
                    .fillMaxWidth(),
                isLoading = uiState.isLoading,
                options = uiState.options,
                peekedHintIds = uiState.peekedHintOptionIds,
                onSelect = { viewModel.selectOption(it) },
                onHintPeek = { viewModel.markHintPeeked(it) },
                trayBounds = { dropZoneBounds.value },
                onDragActiveChange = { activeDragOptionId = it },
                onPreview = { text -> speakUserPreview(text) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            val audioPermissionGranted = audioPermissionGrantedState.value
            val micButtonAction = {
                when {
                    uiState.isVoiceRecording -> viewModel.stopVoiceRecordingAndSend()
                    uiState.isVoiceTranscribing -> Unit
                    !uiState.isVoiceRecording -> {
                        if (!audioPermissionGranted) {
                            pendingPermissionRequest = true
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.startVoiceRecording()
                        }
                    }
                }
            }

            VoiceInputPanel(
                modifier = Modifier.fillMaxWidth(),
                isRecording = uiState.isVoiceRecording,
                isTranscribing = uiState.isVoiceTranscribing,
                permissionGranted = audioPermissionGranted,
                lastTranscribedText = uiState.lastTranscribedText,
                errorMessage = uiState.voiceErrorMessage,
                onMicClick = micButtonAction,
                onCancelRecording = { viewModel.cancelVoiceRecording() }
            )

            if (!saveHistoryError.isNullOrEmpty()) {
                Text(
                    text = saveHistoryError,
                    color = Color(0xFFFFB4AB),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                )
            }
        }
    }
}

private fun LayoutCoordinates.windowRect(): Rect {
    val position = positionInWindow()
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    return Rect(
        position.x,
        position.y,
        position.x + width,
        position.y + height
    )
}

private fun Rect.translate(dx: Float, dy: Float): Rect {
    return Rect(
        left + dx,
        top + dy,
        right + dx,
        bottom + dy
    )
}

private fun Rect.expand(by: Float): Rect {
    return Rect(
        left - by,
        top - by,
        right + by,
        bottom + by
    )
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun StageSection(
    latestAiLine: ChatMessage?,
    speakingMessageId: String?,
    initialLine: String?,
    onReplayRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleKey = latestAiLine?.id ?: "initial"
    val fullDisplayText = latestAiLine?.text ?: initialLine ?: "Maayong buntag! はじめようかの？"
    val splitResult = remember(fullDisplayText) {
        splitInlineTranslation(fullDisplayText)
    }
    val primaryLine = splitResult.first
    val inlineTranslation = splitResult.second
    val translationText = latestAiLine?.translation.orEmpty()
    val translationToShow = inlineTranslation ?: translationText
    val hasTranslation = translationToShow.isNotBlank()
    var showTranslation by remember(bubbleKey) { mutableStateOf(false) }
    val displayText = primaryLine.ifBlank { fullDisplayText }
    val isSpeaking = latestAiLine?.id != null && speakingMessageId == latestAiLine.id
    val interactionSource = remember(bubbleKey) { MutableInteractionSource() }

    LaunchedEffect(interactionSource, bubbleKey) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                showTranslation = false
            }
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1D2B55))
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(id = R.drawable.char_tarsier),
            contentDescription = "タルシエ先生",
            modifier = Modifier
                .size(170.dp)
                .padding(bottom = 8.dp),
            contentScale = ContentScale.Fit
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onReplayRequest() },
                    onLongClick = {
                        if (hasTranslation) {
                            showTranslation = true
                        }
                    }
                ),
            color = Color(0xFFFFF4DB),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = if (isSpeaking) 10.dp else 2.dp,
            shadowElevation = if (isSpeaking) 14.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = displayText,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "ai-line"
                ) { animatedLine ->
                    Text(
                        text = animatedLine,
                        color = Color(0xFF5B3600),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSpeaking) FontWeight.Black else FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(
                    visible = showTranslation && hasTranslation,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = translationToShow,
                        color = Color(0xCC5B3600),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsePanel(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    options: List<RoleplayOption>,
    peekedHintIds: Set<String>,
    onSelect: (String) -> Unit,
    onHintPeek: (String) -> Unit,
    trayBounds: () -> Rect?,
    onDragActiveChange: (String?) -> Unit,
    onPreview: (String) -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(36.dp)),
        color = Color(0xCC0D1424)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                options.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "先生の返答を待っています…",
                            color = Color(0xFFFFF176)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = 160.dp
                        )
                    ) {
                        items(options, key = { it.id }) { option ->
                            RoleplayOptionCard(
                                option = option,
                                hintRevealed = option.id in peekedHintIds,
                                trayBounds = trayBounds,
                                onSelect = onSelect,
                                onPeekHint = onHintPeek,
                                onDragActiveChange = { active ->
                                    onDragActiveChange(active)
                                },
                                onPreview = onPreview
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun DropConfirmationTray(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean,
    lockedOption: RoleplayOption?
) {
    val borderColor = if (isHighlighted) Color(0xFF66F6D5) else Color(0xFF1F2A44)
    val backgroundColor = if (isHighlighted) Color(0xFF0F263D) else Color(0xFF091427)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (lockedOption == null) {
            Text(
                text = "回答ボックス",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "あなたの回答",
                    color = Color(0xFF8FD3FF),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = lockedOption.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceInputPanel(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isTranscribing: Boolean,
    permissionGranted: Boolean,
    lastTranscribedText: String?,
    errorMessage: String?,
    onMicClick: () -> Unit,
    onCancelRecording: () -> Unit
) {
    val trimmedPreview = lastTranscribedText?.let { text ->
        if (text.length > 80) text.take(80) + "…" else text
    }
    val statusText = when {
        isRecording -> "録音中…タップで送信"
        isTranscribing -> "音声テキスト化中…"
        else -> "自由に話しかけてOK！"
    }
    val helperText = when {
        isRecording -> "終わったらもう一度タップ。キャンセルも可能です。"
        isTranscribing -> "タリがビサヤ語に変換しています。少し待ってね。"
        else -> "選択肢以外のアドリブ台詞も大歓迎。"
    }
    val buttonColor = when {
        !permissionGranted -> Color(0xFF475569)
        isRecording -> Color(0xFFE11D48)
        isTranscribing -> Color(0xFF2563EB)
        else -> Color(0xFF22C55E)
    }
    val icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF0B1124),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = onMicClick,
                    enabled = permissionGranted && !isTranscribing,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isRecording) "録音停止" else "録音開始",
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = helperText,
                        color = Color(0xFF9FB4D3),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (isRecording) {
                    TextButton(onClick = onCancelRecording) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "録音を破棄",
                            tint = Color.White
                        )
                        Text(
                            text = "キャンセル",
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isTranscribing, modifier = Modifier.padding(top = 12.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E3A5F)
                )
            }

            AnimatedVisibility(visible = !permissionGranted, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "マイク権限が必要です。ボタンを押して許可すると録音できます。",
                    color = Color(0xFFFFB4AB),
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(
                visible = !errorMessage.isNullOrBlank(),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFFFB4AB),
                    fontSize = 13.sp
                )
            }

            AnimatedVisibility(
                visible = !trimmedPreview.isNullOrBlank() && !isRecording && !isTranscribing,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Column {
                    Text(
                        text = "前回の音声メモ",
                        color = Color(0xFF8FD3FF),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "「$trimmedPreview」",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoleplayOptionCard(
    option: RoleplayOption,
    hintRevealed: Boolean,
    trayBounds: () -> Rect?,
    onSelect: (String) -> Unit,
    onPeekHint: (String) -> Unit,
    onDragActiveChange: (String?) -> Unit,
    onPreview: (String) -> Unit
) {
    var showTranslation by remember(option.id) { mutableStateOf(false) }
    var hasPeeked by remember(option.id) { mutableStateOf(false) }
    var dragOffset by remember(option.id) { mutableStateOf(Offset.Zero) }
    var optionBounds by remember(option.id) { mutableStateOf<Rect?>(null) }
    val translationAvailable = !option.hint.isNullOrBlank()
    val density = LocalDensity.current
    val interactionSource = remember(option.id) { MutableInteractionSource() }

    LaunchedEffect(option.id, interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                showTranslation = false
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .zIndex(if (dragOffset != Offset.Zero) 1f else 0f)
            .onGloballyPositioned { coords ->
                optionBounds = coords.windowRect()
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onPreview(option.text) },
                onLongClick = {
                    if (translationAvailable) {
                        showTranslation = true
                        if (!hasPeeked) {
                            onPeekHint(option.id)
                            hasPeeked = true
                        }
                    }
                },
                onDoubleClick = { onSelect(option.id) }
            )
            .pointerInput(option.id, trayBounds()) {
                detectDragGestures(
                    onDragStart = {
                        onPreview(option.text)
                        onDragActiveChange(option.id)
                    },
                    onDrag = { change, dragAmount ->
                        change.consumePositionChange()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        val currentRect = optionBounds?.translate(dragOffset.x, dragOffset.y)
                        val expandedDropRect = trayBounds()?.expand(by = with(density) { 64.dp.toPx() })
                        val hit = currentRect != null && expandedDropRect != null && expandedDropRect.overlaps(currentRect)
                        if (hit) {
                            Log.d("RoleplayChat", "Drop success for option=${option.id}")
                            onSelect(option.id)
                        }
                        dragOffset = Offset.Zero
                        onDragActiveChange(null)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onDragActiveChange(null)
                    }
                )
            },
        color = if (hintRevealed) Color(0xFF273251) else Color(0xFF16223B),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = if (showTranslation) 6.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (showTranslation) Color(0xFF64F2C2) else Color(0xFF2C3A56)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (showTranslation && translationAvailable) option.hint!! else option.text,
                color = Color(0xFFFFF176),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    showTranslation -> "指を離すとビサヤ語に戻ります"
                    translationAvailable -> "長押しで日本語訳 / ダブルタップで決定"
                    else -> "ダブルタップまたはドラッグで決定"
                },
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

