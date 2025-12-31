@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.listening.ListeningQuestion
import com.bisayaspeak.ai.data.listening.ListeningSession
import com.bisayaspeak.ai.data.listening.QuestionType
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel

@Composable
fun ListeningScreen(
    level: Int,
    isPremium: Boolean = false,
    onNavigateBack: () -> Unit,
    onShowRewardedAd: () -> Unit = {},
    navController: NavHostController,
    viewModel: ListeningViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sessionManager = remember { PracticeSessionManager(isPremium) }

    val session by viewModel.session.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val selectedWords by viewModel.selectedWords.collectAsState()
    val shuffledWords by viewModel.shuffledWords.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val shouldShowAd by viewModel.shouldShowAd.collectAsState()
    val lessonResult by viewModel.lessonResult.collectAsState()
    val clearedLevel by viewModel.clearedLevel.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val configuration = LocalConfiguration.current
    val needsScroll = configuration.screenHeightDp <= 640

    LaunchedEffect(level) {
        sessionManager.startSession()
        viewModel.loadQuestions(level)
    }

    LaunchedEffect(shouldShowAd) {
        if (shouldShowAd) {
            sessionManager.onSessionComplete(activity) {
                onShowRewardedAd()
                viewModel.onAdShown()
            }
        }
    }

    LaunchedEffect(lessonResult, clearedLevel, session?.completed) {
        val result = lessonResult
        val levelCleared = clearedLevel
        if (session?.completed == true && result != null && levelCleared != null) {
            navController.navigate("result_screen/${result.correctCount}/${result.xpEarned}/$levelCleared") {
                popUpTo(AppRoute.Listening.route) { inclusive = true }
            }
            viewModel.clearLessonCompletion()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentSession = session
            if (currentSession != null && !currentSession.completed) {
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }

    BackHandler {
        val currentSession = session
        if (currentSession != null && !currentSession.completed) {
            sessionManager.onSessionInterrupted(activity) {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }

    val availableWords = remember(shuffledWords, selectedWords) {
        val usedCounts = selectedWords.groupingBy { it }.eachCount().toMutableMap()
        val remaining = mutableListOf<String>()
        shuffledWords.forEach { word ->
            val count = usedCounts[word] ?: 0
            if (count > 0) {
                usedCounts[word] = count - 1
            } else {
                remaining += word
            }
        }
        remaining
    }

    val screenTitle = when (currentQuestion?.type) {
        QuestionType.TRANSLATION -> "翻訳練習"
        QuestionType.ORDERING -> "並べ替え練習"
        else -> stringResource(R.string.listening_practice)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = screenTitle, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ListeningBottomBar(
                showResult = showResult,
                sessionCompleted = session?.completed == true,
                onNext = { viewModel.nextQuestion() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            when {
                session?.completed == true -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.loading), color = Color.White)
                    }
                }
                currentQuestion == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.loading), color = Color.White)
                    }
                }
                else -> {
                    ListeningContent(
                        session = session,
                        question = currentQuestion!!,
                        selectedWords = selectedWords,
                        availableWords = availableWords,
                        showResult = showResult,
                        isCorrect = isCorrect,
                        comboCount = comboCount,
                        isPlaying = isPlaying,
                        needsScroll = needsScroll,
                        onPlayAudio = { viewModel.playAudio() },
                        onRemoveWord = { index -> viewModel.removeWordAt(index) },
                        onSelectWord = { viewModel.selectWord(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningContent(
    session: ListeningSession?,
    question: ListeningQuestion,
    selectedWords: List<String>,
    availableWords: List<String>,
    showResult: Boolean,
    isCorrect: Boolean,
    comboCount: Int,
    isPlaying: Boolean,
    needsScroll: Boolean,
    onPlayAudio: () -> Unit,
    onRemoveWord: (Int) -> Unit,
    onSelectWord: (String) -> Unit
) {
    val topScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        ListeningHeader(session = session)
        Spacer(modifier = Modifier.height(8.dp))

        val questionModifier = if (needsScroll) {
            Modifier
                .fillMaxSize()
                .verticalScroll(topScrollState)
        } else {
            Modifier.fillMaxSize()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2130))
        ) {
            Column(
                modifier = questionModifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFF6C63FF)
                )
                Text(
                    text = question.meaning.ifBlank { question.phrase },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onPlayAudio,
                    enabled = !isPlaying,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3246))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = stringResource(R.string.play_audio),
                        tint = Color(0xFF6C63FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Listening", color = Color.White)
                }
            }
        }

        if (comboCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            ComboBadge(comboCount = comboCount)
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        AnswerSection(
            question = question,
            selectedWords = selectedWords,
            availableWords = availableWords,
            showResult = showResult,
            isCorrect = isCorrect,
            onRemoveWord = onRemoveWord,
            onSelectWord = onSelectWord
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ListeningHeader(session: ListeningSession?) {
    val progress = session?.questions?.takeIf { it.isNotEmpty() }?.let {
        (session.currentQuestionIndex + 1f) / it.size.toFloat()
    } ?: 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = session?.let {
                "Q${it.currentQuestionIndex + 1}/${it.questions.size}"
            } ?: "Q -/-",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .height(8.dp),
            color = Color(0xFF6C63FF),
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Text(
            text = session?.score?.let { "Score: $it" } ?: "Score: 0",
            color = Color(0xFF6C63FF),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ComboBadge(comboCount: Int) {
    Text(
        text = "🔥 $comboCount Combo!",
        color = Color(0xFFFFB74D),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x33FFB74D), RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AnswerSection(
    question: ListeningQuestion,
    selectedWords: List<String>,
    availableWords: List<String>,
    showResult: Boolean,
    isCorrect: Boolean,
    onRemoveWord: (Int) -> Unit,
    onSelectWord: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11121F)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "あなたの回答",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            AnswerSlots(
                slotCount = question.correctOrder.size,
                selectedWords = selectedWords,
                onRemoveWord = onRemoveWord
            )
            Text(
                text = "単語をタップして文章を完成させよう",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            WordChoiceGrid(
                words = availableWords,
                enabled = !showResult && selectedWords.size < question.correctOrder.size,
                onWordClick = onSelectWord
            )
            AnimatedVisibility(
                visible = showResult,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ResultCard(isCorrect = isCorrect, question = question)
            }
        }
    }
}

@Composable
private fun AnswerSlots(
    slotCount: Int,
    selectedWords: List<String>,
    onRemoveWord: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(slotCount) { index ->
            val word = selectedWords.getOrNull(index)
            Box(
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .height(48.dp)
                    .background(Color(0xFF1C1F2E), RoundedCornerShape(12.dp))
                    .then(
                        if (word == null) {
                            Modifier.dashedBorder(
                                color = Color.Gray.copy(alpha = 0.5f),
                                strokeWidth = 1.dp,
                                cornerRadius = 12.dp
                            )
                        } else {
                            Modifier.border(
                                width = 2.dp,
                                color = Color(0xFF6C63FF),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    )
                    .padding(horizontal = 12.dp)
                    .clickableWithFeedback(enabled = word != null) {
                        onRemoveWord(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (word != null) {
                    Text(
                        text = word,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WordChoiceGrid(
    words: List<String>,
    enabled: Boolean,
    onWordClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        words.forEach { word ->
            Surface(
                onClick = { onWordClick(word) },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2D3246),
                modifier = Modifier
                    .padding(4.dp)
                    .defaultMinSize(minWidth = 64.dp)
            ) {
                Text(
                    text = word,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ResultCard(isCorrect: Boolean, question: ListeningQuestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isCorrect) stringResource(R.string.correct) else stringResource(R.string.incorrect),
                color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.correct_answer, question.correctOrder.joinToString(" ")),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.meaning, question.meaning),
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
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

    Surface(color = Color.Black) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = bottomPadding)
        ) {
            if (showResult && !sessionCompleted) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) {
                    Text(
                        text = stringResource(R.string.next) + " →",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
        drawRoundRect(
            color = color,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            style = stroke
        )
    }
)

private fun Modifier.clickableWithFeedback(
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = if (enabled) {
    this.then(Modifier.clickable(onClick = onClick))
} else {
    this
}

private fun Modifier.stroke(
    color: Color,
    strokeWidth: Dp
): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(
            width = strokeWidth.toPx()
        )
        drawRect(
            color = color,
            style = stroke
        )
    }
)