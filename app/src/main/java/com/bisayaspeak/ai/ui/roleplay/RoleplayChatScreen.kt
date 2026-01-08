package com.bisayaspeak.ai.ui.roleplay

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService

// 繝・・繧ｿ繧ｯ繝ｩ繧ｹ
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val translation: String? = null,
    val voiceCue: GeminiVoiceCue? = null
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RoleplayChatScreen(
    scenarioId: String,
    onBackClick: () -> Unit,
    isPremium: Boolean = false,
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

    LaunchedEffect(uiState.messages) {
        val latest = uiState.messages.lastOrNull { !it.isUser }
        latest?.voiceCue?.let { cue ->
            voiceService.speak(
                text = latest.text,
                cue = cue,
                onStart = { viewModel.notifyVoicePlaybackStarted(latest.id) },
                onComplete = { viewModel.notifyVoicePlaybackFinished(latest.id) },
                onError = { viewModel.notifyVoicePlaybackFinished(latest.id) }
            )
        }
    }

    var showTranslation by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }
    val hintCandidate: RoleplayOption? = uiState.options.firstOrNull()

    val scenarioTitle = uiState.currentScenario?.title ?: "AI ロールプレイ"

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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.char_owl),
                contentDescription = "フクロウ師匠",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            val isSpeaking = latestAiLine?.id != null && speakingMessageId == latestAiLine.id
            val bubbleColor by animateColorAsState(
                targetValue = when {
                    showTranslation -> Color(0xFF2C3A5A)
                    isSpeaking -> Color(0xFF1F2E4C)
                    else -> Color(0xFF151F2B)
                },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "bubbleColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = if (isSpeaking) 6.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInteropFilter { motionEvent ->
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> showTranslation = true
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> showTranslation = false
                            }
                            true
                        }
                ) {
                    Text(
                        text = latestAiLine?.text ?: uiState.currentScenario?.initialMessage
                            ?: "Maayong buntag! はじめようかの？",
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSpeaking) FontWeight.Black else FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showTranslation && latestAiLine?.translation != null,
                    enter = fadeIn(tween(150)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(150)) + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                ) {
                    Surface(
                        color = Color(0xFF253554),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = latestAiLine?.translation ?: "",
                            color = Color(0xFFDCEBFF),
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Button(
                onClick = { showHint = !showHint },
                enabled = hintCandidate != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (showHint) "ヒントを隠す" else "ヒントを見る")
            }

            if (showHint && hintCandidate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = hintCandidate.hint ?: "ヒントはまだありません。",
                    color = Color(0xFFDBEAFE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x3322568E),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RoleplayOptionsPanel(
                isLoading = uiState.isLoading,
                options = uiState.options,
                peekedHintIds = uiState.peekedHintOptionIds,
                onSelect = { viewModel.selectOption(it) },
                onHintPeek = { viewModel.markHintPeeked(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            SmartAdBanner(
                isPremium = isPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (uiState.showCompletionDialog) {
            CompletionCelebrationDialog(
                passed = uiState.completionScore >= 80,
                score = uiState.completionScore,
                onDismiss = {
                    viewModel.dismissCompletionDialog()
                    viewModel.markUnlockHandled()
                }
            )
        }
    }
}

@Composable
fun RoleplayOptionsPanel(
    isLoading: Boolean,
    options: List<RoleplayOption>,
    peekedHintIds: Set<String>,
    onSelect: (String) -> Unit,
    onHintPeek: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        } else {
            options.forEach { option ->
                RoleplayOptionItem(
                    option = option,
                    hintRevealed = option.id in peekedHintIds,
                    onSelect = onSelect,
                    onPeekHint = onHintPeek
                )
            }
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RoleplayOptionItem(
    option: RoleplayOption,
    hintRevealed: Boolean,
    onSelect: (String) -> Unit,
    onPeekHint: (String) -> Unit
) {
    var hintVisible by remember(option.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (!hintVisible) {
                                hintVisible = true
                                onPeekHint(option.id)
                            }
                            false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            hintVisible = false
                            false
                        }
                        else -> false
                    }
                },
            onClick = { onSelect(option.id) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hintRevealed) Color(0xFF4A5B78) else Color(0xFF007AFF),
                contentColor = Color.White
            )
        ) {
            Text(
                text = option.text,
                fontSize = 16.sp
            )
        }

        AnimatedVisibility(
            visible = hintVisible && option.hint != null,
            enter = fadeIn(tween(150)) + slideInVertically { -it / 3 },
            exit = fadeOut(tween(120)) + slideOutVertically { -it / 3 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-42).dp)
        ) {
            Surface(
                color = Color(0xCC1C253C),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = option.hint ?: "",
                    fontSize = 13.sp,
                    color = Color(0xFFDBEAFE),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CompletionCelebrationDialog(
    passed: Boolean,
    score: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val bounce = rememberInfiniteTransition(label = "monkey-bounce").animateFloat(
            initialValue = 0f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "monkey-bounce-anim"
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (passed) "Level 2 解放！" else "もう一度挑戦！",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "スコア: $score%",
                    color = Color(0xFF9CC1FF),
                    fontSize = 16.sp
                )
                Text(
                    text = "🐒",
                    fontSize = 56.sp,
                    modifier = Modifier.offset(y = (-bounce.value).dp)
                )
                Text(
                    text = if (passed) "タルシエ先生が喜んで跳ねています！" else "タルシエ先生はまだ見守っています。",
                    color = Color(0xFFBFD6FF),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C6EF5))
                ) {
                    Text(text = if (passed) "続けてLv.2へ" else "戻る")
                }
            }
        }
    }
}
