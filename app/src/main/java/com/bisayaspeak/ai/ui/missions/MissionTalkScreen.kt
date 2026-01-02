package com.bisayaspeak.ai.ui.missions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.MissionChatMessage
import com.bisayaspeak.ai.data.model.MissionContext
import com.bisayaspeak.ai.data.model.MissionScenario
import com.bisayaspeak.ai.ui.viewmodel.MissionStatus
import com.bisayaspeak.ai.ui.viewmodel.MissionTalkViewModel
import kotlinx.coroutines.launch

private enum class InputMode { VOICE, KEYBOARD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionTalkScreen(
    scenarioId: String,
    onNavigateBack: () -> Unit,
    viewModel: MissionTalkViewModel = viewModel(factory = MissionTalkViewModel.factory(scenarioId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val scenario = uiState.scenario
    val listState = rememberLazyListState()
    var inputMode by remember { mutableStateOf(InputMode.VOICE) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onInputChange(spoken)
            }
        }
    }

    var hasMicPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) launchSpeechRecognizer(context, speechLauncher)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            MissionTopBar(
                scenario = scenario,
                remainingTurns = uiState.remainingTurns,
                onBack = onNavigateBack
            )
        },
        containerColor = Color(0xFF050A14)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050A14))
                .padding(padding)
        ) {
            if (scenario == null) {
                MissionErrorState(onNavigateBack = onNavigateBack)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    MissionHeaderInfo(scenario.context, uiState.remainingTurns)

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionHintRow(
                        hints = scenario.context.hints,
                        onHintClick = viewModel::appendHint
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionChatTimeline(
                        messages = uiState.messages,
                        listState = listState,
                        isStreaming = uiState.isStreaming
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionInputCard(
                        inputText = uiState.inputText,
                        onTextChange = viewModel::onInputChange,
                        onSend = {
                            viewModel.sendMessage()
                            inputMode = InputMode.KEYBOARD
                        },
                        isSending = uiState.isSending,
                        inputMode = inputMode,
                        onToggleMode = {
                            inputMode = if (inputMode == InputMode.VOICE) InputMode.KEYBOARD else InputMode.VOICE
                        }
                    )
                }

                if (inputMode == InputMode.VOICE) {
                    MissionMicButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        isListening = uiState.isStreaming
                    ) {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            launchSpeechRecognizer(context, speechLauncher)
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSuccessDialog) {
        MissionResultDialog(
            success = true,
            title = "MISSION CLEAR!",
            description = "🎉 目標達成！ビサヤ語でのミッションに成功しました！",
            primaryButton = "次のミッションへ",
            onDismiss = viewModel::dismissSuccessDialog
        )
    }

    if (uiState.showFailedDialog) {
        MissionResultDialog(
            success = false,
            title = "FAILED... 惜しい！",
            description = "もう少しでゴールでした。次はもっと攻めてみましょう！",
            primaryButton = "再挑戦",
            onDismiss = viewModel::dismissFailedDialog
        )
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("閉じる") }
            },
            title = { Text("エラー") },
            text = { Text(message) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionTopBar(
    scenario: MissionScenario?,
    remainingTurns: Int,
    onBack: () -> Unit
) {
    val totalTurns = scenario?.context?.turnLimit ?: remainingTurns
    TopAppBar(
        title = {
            Column {
                Text(scenario?.title ?: "AI Mission Talk")
                if (scenario != null) {
                    Text(
                        text = "残りターン: $remainingTurns / $totalTurns",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF050A14),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun MissionHeaderInfo(context: MissionContext, remainingTurns: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1C2E)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = context.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = context.goal,
                color = Color.White.copy(alpha = 0.85f)
            )
            Surface(
                color = Color(0xFF183054),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "残りターン $remainingTurns / ${context.turnLimit}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color(0xFF9DD8FF),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionHintRow(
    hints: List<String>,
    onHintClick: (String) -> Unit
) {
    if (hints.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.TipsAndUpdates,
                contentDescription = null,
                tint = Color(0xFFFFC857)
            )
            Text("MISSION カンペ", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hints.take(4).forEach { hint ->
                Surface(
                    color = Color(0x3329B6F6),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clickable { onHintClick(hint) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = hint,
                            color = Color(0xFF9DD8FF),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionChatTimeline(
    messages: List<MissionChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isStreaming: Boolean
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040A12)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MissionChatBubble(message)
                }
            }

            if (isStreaming) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        color = Color(0x4429B6F6),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF4FC3F7),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "AI考え中…",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionChatBubble(message: MissionChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) Color(0xFF1B3A57) else Color(0xFF0D1724)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp
                    )
                )
                .background(bubbleColor)
                .padding(14.dp)
                .widthIn(max = 320.dp)
        ) {
            Text(
                text = message.primaryText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            message.secondaryText?.takeIf { it.isNotBlank() }?.let { translation ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = translation,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            if (!message.isUser && message.isGoalFlagged) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GOAL ACHIEVED",
                        color = Color(0xFFFFD166),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionInputCard(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    inputMode: InputMode,
    onToggleMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1623))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (inputMode == InputMode.VOICE) "Speak → Edit → Send" else "Keyboard入力中",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggleMode) {
                    Icon(
                        imageVector = if (inputMode == InputMode.VOICE) Icons.Default.Keyboard else Icons.Default.Mic,
                        contentDescription = "入力切り替え",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                placeholder = { Text("音声認識の結果がここに表示されます", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF152235),
                    unfocusedContainerColor = Color(0xFF0F1C2E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onSend,
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) Color(0xFF00C896) else Color(0xFF1A2C42)
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "送信",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MissionMicButton(
    modifier: Modifier = Modifier,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF00C9A7), Color(0xFF0077FF))
                    )
                )
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "音声入力",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }
        AnimatedVisibility(
            visible = isListening,
            modifier = Modifier
                .align(Alignment.Center)
                .size(150.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

@Composable
private fun MissionResultDialog(
    success: Boolean,
    title: String,
    description: String,
    primaryButton: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(primaryButton)
            }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (success) Color(0xFFFFD166) else Color(0xFFFF6B6B)
            )
        },
        text = { Text(description) },
        containerColor = Color(0xFF0F1C2E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun MissionErrorState(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ミッション情報の読み込みに失敗しました。",
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onNavigateBack) {
            Text("戻る")
        }
    }
}

private fun launchSpeechRecognizer(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, detectInputLanguage())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "ビサヤ語で話す内容を日本語で話してOK（編集できます）")
    }
    launcher.launch(intent)
}

private fun detectInputLanguage(): String {
    val deviceLang = java.util.Locale.getDefault().language
    return when (deviceLang) {
        "ja" -> "ja-JP"
        "fil", "tl" -> "fil-PH"
        "ceb" -> "en-US"
        else -> "en-US"
    }
}
