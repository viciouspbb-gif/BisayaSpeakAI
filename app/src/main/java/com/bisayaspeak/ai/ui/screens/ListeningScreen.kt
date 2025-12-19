package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.bisayaspeak.ai.R
import androidx.compose.material.icons.filled.Star
import com.bisayaspeak.ai.data.model.DifficultyLevel
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListeningScreen(
    level: DifficultyLevel,
    isPremium: Boolean = false,
    onNavigateBack: () -> Unit,
    onShowRewardedAd: (() -> Unit) -> Unit = {},
    viewModel: ListeningViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // セッション管理
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    
    val session = viewModel.session.collectAsState().value
    val currentQuestion = viewModel.currentQuestion.collectAsState().value
    val selectedWords by viewModel.selectedWords.collectAsState()
    val shuffledWords by viewModel.shuffledWords.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val shouldShowAd by viewModel.shouldShowAd.collectAsState()
    
    LaunchedEffect(Unit) {
        sessionManager.startSession()
        viewModel.startSession(level)
    }
    
    // セッション完了時の広告表示（統一ルール：1セット完了 = 1回広告）
    LaunchedEffect(shouldShowAd) {
        if (shouldShowAd) {
            android.util.Log.d("ListeningScreen", "Session completed, showing ad")
            sessionManager.onSessionComplete(activity) {
                viewModel.onAdShown()
            }
        }
    }
    
    // 中断時の広告表示（統一ルール：中断 = 1回広告）
    DisposableEffect(Unit) {
        onDispose {
            if (session != null && !session.completed) {
                android.util.Log.d("ListeningScreen", "Session interrupted, showing ad")
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }
    
    // バックボタン処理
    BackHandler {
        if (session != null && !session.completed) {
            sessionManager.onSessionInterrupted(activity) {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }
    
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.listening_practice),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (session?.completed == true) {
                ListeningResultScreen(
                    session = session!!,
                    onRestart = { viewModel.startSession(level) },
                    onBack = onNavigateBack
                )
            } else if (currentQuestion != null && session != null) {
                val question = currentQuestion
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.listening_helper_text),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEDE4F3)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Q${session.currentQuestionIndex + 1}/${session.questions.size}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFF4A90E2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "${session.score}",
                                            color = Color(0xFF4A90E2),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (isPlaying) 1.05f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                Button(
                                    onClick = { viewModel.playAudio() },
                                    enabled = !isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .scale(pulseScale),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A90E2),
                                        disabledContainerColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 2.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isPlaying) "🎵 Playing..." else "🎧 Tap to Listen",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEDE4F3)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Your Answer:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val correctWordCount = question.correctOrder.size
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(correctWordCount) { index ->
                                        val isFilled = index < selectedWords.size
                                        Box(
                                            modifier = Modifier
                                                .height(52.dp)
                                                .widthIn(min = 80.dp)
                                                .shadow(
                                                    elevation = if (isFilled) 6.dp else 0.dp,
                                                    shape = RoundedCornerShape(12.dp),
                                                    clip = false
                                                )
                                                .background(
                                                    Color.White,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .then(
                                                    if (isFilled) {
                                                        Modifier.border(
                                                            2.dp,
                                                            Color(0xFF4A90E2),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                    } else {
                                                        Modifier.dashedBorder(
                                                            color = Color.LightGray.copy(alpha = 0.8f),
                                                            strokeWidth = 1.dp,
                                                            cornerRadius = 12.dp
                                                        )
                                                    }
                                                )
                                                .clickable(enabled = isFilled && !showResult) {
                                                    viewModel.removeWordAt(index)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isFilled) {
                                                Text(
                                                    text = selectedWords[index],
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${selectedWords.size}/${correctWordCount} words selected",
                                    color = if (selectedWords.size == correctWordCount) Color(0xFF4A90E2) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (showResult) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFEDE4F3)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isCorrect) "✓ " + stringResource(R.string.correct) else "✗ " + stringResource(R.string.incorrect),
                                        color = if (isCorrect) Color(0xFF4A90E2) else MaterialTheme.colorScheme.error,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (!isCorrect) {
                                        Text(
                                            text = stringResource(R.string.correct_answer, question.correctOrder.joinToString(" ")),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = stringResource(R.string.meaning, question.meaning),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.nextQuestion() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4A90E2)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.next) + " →",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEDE4F3)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Select Words:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val coroutineScope = rememberCoroutineScope()
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    shuffledWords.forEach { word ->
                                        val isUsed = selectedWords.count { it == word } >= shuffledWords.count { it == word }
                                        var isPressed by remember { mutableStateOf(false) }
                                        val scale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.92f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "scale"
                                        )
                                        val backgroundColor by animateColorAsState(
                                            targetValue = when {
                                                isUsed -> Color(0xFFE0E0E0)
                                                else -> Color(0xFFEDE4F3)
                                            },
                                            animationSpec = tween(200),
                                            label = "color"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .scale(scale)
                                                .background(
                                                    backgroundColor,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable(enabled = !isUsed && !showResult) {
                                                    isPressed = true
                                                    viewModel.selectWord(word)
                                                    coroutineScope.launch {
                                                        delay(100)
                                                        isPressed = false
                                                    }
                                                }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = word,
                                                color = if (isUsed) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningBottomBar(
    showResult: Boolean,
    sessionCompleted: Boolean,
    onNext: () -> Unit
) {
    val bottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    Surface(
        color = Color(0xFF111111),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = bottomPadding)
        ) {
            if (showResult && !sessionCompleted) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.next) + " →",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AdPromptDialog(
    mistakes: Int,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.mistakes_warning),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.watch_ad_reset),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.continue_after_reset),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.watch_ad), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.later))
            }
        },
        containerColor = Color(0xFFEDE4F3)
    )
}

@Composable
fun ListeningResultScreen(
    session: com.bisayaspeak.ai.data.listening.ListeningSession,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.completed),
            color = Color(0xFF4A90E2),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEDE4F3)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.score_result),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                Text(
                    text = "${session.score}/${session.questions.size}",
                    color = Color(0xFF4A90E2),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val accuracy = if (session.questions.isNotEmpty()) {
                    (session.score.toFloat() / session.questions.size * 100).toInt()
                } else 0
                
                Text(
                    text = stringResource(R.string.accuracy, accuracy),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2)
            )
        ) {
            Text(stringResource(R.string.try_again_listening), fontSize = 18.sp, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF4A90E2)
            )
        ) {
            Text(stringResource(R.string.back_button), fontSize = 18.sp)
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp,
    cornerRadius: Dp
): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        val radiusPx = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
            style = stroke
        )
    }
)
