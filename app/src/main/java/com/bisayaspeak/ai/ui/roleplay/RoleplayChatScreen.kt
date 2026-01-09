package com.bisayaspeak.ai.ui.roleplay

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }
    val pendingResult = uiState.pendingResult

    LaunchedEffect(pendingResult) {
        pendingResult?.let {
            onCompleted(it)
            viewModel.consumePendingResult()
        }
    }

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
                painter = painterResource(id = R.drawable.char_tarsier),
                contentDescription = "タルシエ先生",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            val isSpeaking = latestAiLine?.id != null && speakingMessageId == latestAiLine.id
            val bubbleColor = Color(0xFFFFF4DB)
            val bubbleBorderColor = Color(0xFFFFE3AC)
            val primaryTextColor = Color(0xFF5B3600)
            val translationColor = Color(0xCC5B3600)
            val aiLineText = latestAiLine?.text
                ?: uiState.currentScenario?.initialMessage
                ?: "Maayong buntag! はじめようかの？"
            val translationText = latestAiLine?.translation.orEmpty()
            var aiTranslationVisible by remember(latestAiLine?.id) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = if (isSpeaking) 8.dp else 2.dp,
                    shadowElevation = if (isSpeaking) 10.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = bubbleBorderColor, shape = RoundedCornerShape(28.dp))
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if (translationText.isNotBlank()) aiTranslationVisible = true
                                    false
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    aiTranslationVisible = false
                                    false
                                }
                                else -> false
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = aiLineText,
                            color = primaryTextColor,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSpeaking) FontWeight.Black else FontWeight.SemiBold
                        )

                        AnimatedVisibility(
                            visible = aiTranslationVisible && translationText.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = translationText,
                                color = translationColor,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                            )
                        }

                        if (!aiTranslationVisible && translationText.isNotBlank()) {
                            Text(
                                text = "長押しで日本語訳",
                                color = translationColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
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
    var isPressing by remember(option.id) { mutableStateOf(false) }
    var hasPeeked by remember(option.id) { mutableStateOf(false) }
    val translationAvailable = !option.hint.isNullOrBlank()

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
                            if (translationAvailable) {
                                isPressing = true
                                if (!hasPeeked) {
                                    onPeekHint(option.id)
                                    hasPeeked = true
                                }
                            }
                            false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isPressing = false
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isPressing && translationAvailable) option.hint!! else option.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isPressing && translationAvailable) {
                    Text(
                        text = "長押しで日本語訳",
                        fontSize = 12.sp,
                        color = Color(0xFFB3DAFF),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

