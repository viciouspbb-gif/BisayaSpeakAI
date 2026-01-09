package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
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
    isPremium: Boolean = false,
    onCompleted: (RoleplayResultPayload) -> Unit,
    viewModel: RoleplayChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val speakingMessageId by viewModel.speakingMessageId.collectAsState()
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }

    DisposableEffect(Unit) {
        onDispose { voiceService.shutdown() }
    }

    LaunchedEffect(scenarioId) {
        viewModel.loadScenario(scenarioId)
    }

    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }

    LaunchedEffect(latestAiLine?.id) {
        latestAiLine?.let { message ->
            voiceService.speak(
                text = message.text,
                cue = message.voiceCue ?: GeminiVoiceCue.DEFAULT,
                onStart = { viewModel.notifyVoicePlaybackStarted(message.id) },
                onComplete = { viewModel.notifyVoicePlaybackFinished(message.id) },
                onError = { viewModel.notifyVoicePlaybackFinished(message.id) }
            )
        }
    }

    val pendingResult = uiState.pendingResult

    LaunchedEffect(pendingResult) {
        pendingResult?.let {
            onCompleted(it)
            viewModel.consumePendingResult()
        }
    }

    val scenarioTitle = uiState.currentScenario?.title ?: "タリとの散歩道"

    var activeDragOptionId by remember { mutableStateOf<String?>(null) }
    val dropZoneBounds = remember { mutableStateOf<Rect?>(null) }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StageSection(
                latestAiLine = latestAiLine,
                speakingMessageId = speakingMessageId,
                initialLine = uiState.currentScenario?.initialMessage,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            DropConfirmationTray(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .onGloballyPositioned { dropZoneBounds.value = it.windowRect() },
                isHighlighted = activeDragOptionId != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            ResponsePanel(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxWidth(),
                isLoading = uiState.isLoading,
                options = uiState.options,
                peekedHintIds = uiState.peekedHintOptionIds,
                onSelect = { viewModel.selectOption(it) },
                onHintPeek = { viewModel.markHintPeeked(it) },
                trayBounds = { dropZoneBounds.value },
                onDragActiveChange = { activeDragOptionId = it }
            )
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

private fun Offset.distanceSquaredTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy
}

private val ViewConfiguration.doubleTapSlop: Float
    get() = touchSlop * 2f

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun StageSection(
    latestAiLine: ChatMessage?,
    speakingMessageId: String?,
    initialLine: String?,
    modifier: Modifier = Modifier
) {
    val bubbleKey = latestAiLine?.id ?: "initial"
    val translationText = latestAiLine?.translation.orEmpty()
    var showTranslation by remember(bubbleKey) { mutableStateOf(false) }
    val displayText = latestAiLine?.text ?: initialLine ?: "Maayong buntag! はじめようかの？"
    val isSpeaking = latestAiLine?.id != null && speakingMessageId == latestAiLine.id

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
                .pointerInput(bubbleKey, translationText) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (translationText.isBlank()) {
                            waitForUpOrCancellation()
                            return@awaitEachGesture
                        }
                        showTranslation = true
                        waitForUpOrCancellation()
                        showTranslation = false
                    }
                },
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
                    visible = showTranslation && translationText.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = translationText,
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
    onDragActiveChange: (String?) -> Unit
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
                            color = Color(0xFF9FB4D3)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
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
                                }
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
    isHighlighted: Boolean
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
        Text(
            text = "回答ボックス",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleplayOptionCard(
    option: RoleplayOption,
    hintRevealed: Boolean,
    trayBounds: () -> Rect?,
    onSelect: (String) -> Unit,
    onPeekHint: (String) -> Unit,
    onDragActiveChange: (String?) -> Unit
) {
    var showTranslation by remember(option.id) { mutableStateOf(false) }
    var hasPeeked by remember(option.id) { mutableStateOf(false) }
    var dragOffset by remember(option.id) { mutableStateOf(Offset.Zero) }
    var optionBounds by remember(option.id) { mutableStateOf<Rect?>(null) }
    val translationAvailable = !option.hint.isNullOrBlank()
    val viewConfiguration = LocalViewConfiguration.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .zIndex(if (dragOffset != Offset.Zero) 1f else 0f)
            .onGloballyPositioned { coords ->
                optionBounds = coords.windowRect()
            }
            .pointerInput(option.id, translationAvailable, viewConfiguration) {
                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                val doubleTapDistanceSquared = viewConfiguration.doubleTapSlop * viewConfiguration.doubleTapSlop
                var lastTapTime = 0L
                var lastTapPosition = Offset.Unspecified
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    if (translationAvailable) {
                        showTranslation = true
                        if (!hasPeeked) {
                            onPeekHint(option.id)
                            hasPeeked = true
                        }
                    }
                    val up = waitForUpOrCancellation()
                    if (translationAvailable) {
                        showTranslation = false
                    }
                    if (up == null) {
                        lastTapTime = 0L
                        lastTapPosition = Offset.Unspecified
                        return@awaitEachGesture
                    }
                    val now = up.uptimeMillis
                    val withinTime = lastTapTime != 0L && now - lastTapTime <= doubleTapTimeout
                    val withinDistance =
                        lastTapPosition != Offset.Unspecified && lastTapPosition.distanceSquaredTo(downPosition) <= doubleTapDistanceSquared
                    if (withinTime && withinDistance) {
                        onSelect(option.id)
                        lastTapTime = 0L
                        lastTapPosition = Offset.Unspecified
                    } else {
                        lastTapTime = now
                        lastTapPosition = downPosition
                    }
                }
            }
            .pointerInput(option.id, trayBounds()) {
                detectDragGestures(
                    onDragStart = {
                        onDragActiveChange(option.id)
                    },
                    onDrag = { change, dragAmount ->
                        change.consumePositionChange()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        val currentRect = optionBounds?.translate(dragOffset.x, dragOffset.y)
                        val dropRect = trayBounds()
                        if (currentRect != null && dropRect != null && currentRect.overlaps(dropRect)) {
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
                color = Color.White,
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
                color = Color(0xFF9FB4D3),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

