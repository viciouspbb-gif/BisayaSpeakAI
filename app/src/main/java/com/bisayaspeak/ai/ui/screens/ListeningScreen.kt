package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.bisayaspeak.ai.R
import androidx.compose.material.icons.filled.Star
import com.bisayaspeak.ai.data.model.DifficultyLevel
import com.bisayaspeak.ai.data.listening.ListeningQuestion
import com.bisayaspeak.ai.data.listening.ListeningSession
import com.bisayaspeak.ai.data.listening.QuestionType
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.bisayaspeak.ai.ui.navigation.AppRoute
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListeningScreen(
    level: Int,
    isPremium: Boolean = false,
    onNavigateBack: () -> Unit,
    onShowRewardedAd: (() -> Unit) -> Unit = {},
    navController: NavHostController,
    viewModel: ListeningViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    val session = viewModel.session.collectAsState().value
    val currentQuestion = viewModel.currentQuestion.collectAsState().value
    val selectedWords by viewModel.selectedWords.collectAsState()
    val shuffledWords by viewModel.shuffledWords.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val shouldShowAd by viewModel.shouldShowAd.collectAsState()
    val lessonResult by viewModel.lessonResult.collectAsState()
    val clearedLevel by viewModel.clearedLevel.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()
    val voiceHintRemaining by viewModel.voiceHintRemaining.collectAsState()
    val showHintRecoveryDialog by viewModel.showHintRecoveryDialog.collectAsState()
    val configuration = LocalConfiguration.current

    val screenTitle = when (currentQuestion?.type) {
        QuestionType.TRANSLATION -> "翻訳練習"
        QuestionType.ORDERING -> "並べ替え練習"
        else -> stringResource(R.string.listening_practice)
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsInit: TextToSpeech? = null
        ttsInit = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInit?.let { instance ->
                    var result = instance.setLanguage(Locale("id"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        result = instance.setLanguage(Locale("fil"))
                    }
                    instance.setPitch(0.7f)
                    instance.setSpeechRate(0.85f)
                }
            }
        }
        tts = ttsInit
        onDispose {
            ttsInit?.stop()
            ttsInit?.shutdown()
        }
    }
    
    LaunchedEffect(level) {
        sessionManager.startSession()
        viewModel.loadQuestions(level)
    }
    
    LaunchedEffect(shouldShowAd) {
        if (shouldShowAd) {
            sessionManager.onSessionComplete(activity) {
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
            if (session != null && !session.completed) {
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }
    
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
        bottomBar = {
            Column {
                ListeningBottomBar(
                    showResult = showResult,
                    sessionCompleted = session?.completed == true,
                    onNext = { viewModel.nextQuestion() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (session?.completed == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.loading), color = Color.White)
                }
            } else if (currentQuestion != null && session != null) {
                val question = currentQuestion
                val sectionSpacingWeight = 0.05f
                val buttonHeight = 54.dp
                val buttonMinWidth = 180.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 40.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.85f, fill = true)
                            .heightIn(max = 72.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.char_owl),
                            contentDescription = stringResource(R.string.listening_practice),
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit
                        )
                        Button(
                            onClick = {
                                viewModel.playAudio()
                            },
                            enabled = !isPlaying,
                            modifier = Modifier
                                .height(buttonHeight)
                                .widthIn(min = buttonMinWidth),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3246))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.play_audio),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "音声ヒント (残り${voiceHintRemaining.coerceAtLeast(0)})",
                                color = Color.White,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .weight(sectionSpacingWeight, fill = true)
                            .heightIn(max = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f, fill = true)
                            .heightIn(max = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question.meaning.ifBlank { question.phrase },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.9f, fill = true)
                            .heightIn(max = 180.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "あなたの回答",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            AnswerSlots(
                                slotCount = question.correctOrder.size,
                                selectedWords = selectedWords,
                                onRemoveWord = { index -> viewModel.removeWordAt(index) }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .weight(sectionSpacingWeight, fill = true)
                            .heightIn(max = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f, fill = true)
                            .heightIn(max = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showResult) {
                            ResultCard(isCorrect = isCorrect, question = question)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f, fill = true)
                            .heightIn(max = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (comboCount > 0) {
                            Text(
                                text = "🔥 ${comboCount} Combo!",
                                color = Color(0xFFFFA726),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .weight(sectionSpacingWeight, fill = true)
                            .heightIn(max = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.8f, fill = true)
                            .heightIn(max = 190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FlexboxWordGrid(
                            words = shuffledWords,
                            selectedWords = selectedWords,
                            showResult = showResult,
                            onSelectWord = { word ->
                                tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
                                viewModel.selectWord(word)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showHintRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHintRecoveryDialog() },
            confirmButton = {
                TextButton(onClick = {
                    onShowRewardedAd {
                        viewModel.onHintRecoveryEarned()
                    }
                }) {
                    Text("動画を見て回復")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissHintRecoveryDialog() }) {
                    Text("閉じる")
                }
            },
            title = { Text("音声ヒントが必要です") },
            text = {
                Text(
                    "動画広告を視聴すると音声ヒントを3回分回復できます。\nPRO版ならこの制限は一切ありません。"
                )
            }
        )
    }
}

@Composable
private fun QuestionTypeBadge(type: QuestionType) {
    val (label, color) = when (type) {
        QuestionType.LISTENING -> "Listening" to Color(0xFF4A90E2)
        QuestionType.TRANSLATION -> "Translation" to Color(0xFF34D399)
        QuestionType.ORDERING -> "Ordering" to Color(0xFFFFA726)
    }
    Surface(
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FlexboxWordGrid(
    words: List<String>,
    selectedWords: List<String>,
    showResult: Boolean,
    onSelectWord: (String) -> Unit
) {
    val density = LocalDensity.current
    val horizontalPadding = with(density) { 16.dp.toPx().roundToInt() }
    val verticalPadding = with(density) { 10.dp.toPx().roundToInt() }
    val chipCornerRadius = with(density) { 16.dp.toPx() }
    val margin = with(density) { 6.dp.toPx().roundToInt() }
    val selectionCounts = selectedWords.groupingBy { it }.eachCount().toMutableMap()

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            FlexboxLayout(context).apply {
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.CENTER
                alignItems = AlignItems.CENTER
                clipToPadding = false
            }
        },
        update = { flexbox ->
            flexbox.removeAllViews()
            words.forEach { word ->
                val shouldHide = selectionCounts[word]?.let { it > 0 } == true
                if (shouldHide) {
                    selectionCounts[word] = selectionCounts[word]!! - 1
                }
                val chipColor = if (shouldHide) Color.Transparent else Color(0xFFEDE4F3)
                val textColor = if (shouldHide) Color.Transparent else Color.Black

                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    this.cornerRadius = chipCornerRadius
                    setColor(chipColor.toArgb())
                }

                val textView = TextView(flexbox.context).apply {
                    text = word
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(textColor.toArgb())
                    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                    background = drawable
                    isEnabled = !shouldHide && !showResult
                    layoutParams = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    setOnClickListener {
                        if (!shouldHide && !showResult) {
                            onSelectWord(word)
                        }
                    }
                }
                flexbox.addView(textView)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
                    .clickable(enabled = word != null) {
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
private fun ResultCard(isCorrect: Boolean, question: ListeningQuestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
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
private fun ListeningHeader(session: ListeningSession) {
    val progress = if (session.questions.isNotEmpty()) {
        (session.currentQuestionIndex + 1f) / session.questions.size.toFloat()
    } else 0f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Q${session.currentQuestionIndex + 1}/${session.questions.size}",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Score: ${session.score}",
                color = Color(0xFF4A90E2),
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Color(0xFF4A90E2),
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun QuestionArea(
    question: ListeningQuestion,
    session: ListeningSession,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 12.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFEDE4F3))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.char_owl),
                contentDescription = "Listening owl coach",
                modifier = Modifier
                    .heightIn(min = 60.dp, max = 200.dp)
                    .widthIn(min = 60.dp, max = 200.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val title = when (question.type) {
                    QuestionType.LISTENING -> "フクロウ先生の音声ヒント"
                    QuestionType.TRANSLATION -> "翻訳ミッション"
                    QuestionType.ORDERING -> "語順ミッション"
                }
                Text(
                    text = title,
                    color = Color(0xFF5C4B8A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                if (question.type == QuestionType.LISTENING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(pulseScale)
                            .clip(bubbleShape)
                            .background(
                                if (isPlaying) Color(0xFF4A90E2).copy(alpha = 0.6f) else Color(0xFF4A90E2)
                            )
                            .clickable(enabled = !isPlaying) { onPlayAudio() }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (isPlaying) "再生中..." else "音声を再生",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Tap & listen",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    val missionLabel = if (question.type == QuestionType.TRANSLATION) "翻訳ミッション" else "並べ替えミッション"
                    val helperText = if (question.type == QuestionType.TRANSLATION) {
                        "この日本語をセブアノ語にしよう。"
                    } else {
                        "日本語の意味に合う語順を組み立てよう。"
                    }
                    Text(
                        text = missionLabel,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = question.meaning,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = helperText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "正解数: ${session.score}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "ミス: ${session.mistakes}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeningAnswerArea(
    question: ListeningQuestion,
    selectedWords: List<String>,
    shuffledWords: List<String>,
    showResult: Boolean,
    isCorrect: Boolean,
    onRemoveWordAt: (Int) -> Unit,
    onSelectWord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            QuestionTypeBadge(type = question.type)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            val correctWordCount = question.correctOrder.size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "あなたの回答",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(correctWordCount) { index ->
                        val isFilled = index < selectedWords.size
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .widthIn(min = 72.dp)
                                .shadow(
                                    elevation = if (isFilled) 6.dp else 0.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    clip = false
                                )
                                .background(
                                    Color(0xFF2C2C3E),
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
                                            color = Color.Gray.copy(alpha = 0.6f),
                                            strokeWidth = 1.dp,
                                            cornerRadius = 12.dp
                                        )
                                    }
                                )
                                .clickable(enabled = isFilled && !showResult) {
                                    onRemoveWordAt(index)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                Text(
                                    text = selectedWords[index],
                                    color = Color.White,
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

        AnimatedVisibility(visible = showResult) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE4F3)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isCorrect) "✓ " + stringResource(R.string.correct) else "✗ " + stringResource(
                            R.string.incorrect
                        ),
                        color = if (isCorrect) Color(0xFF4A90E2) else MaterialTheme.colorScheme.error,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isCorrect) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.char_tarsier),
                                    contentDescription = "Tarsier coach explaining",
                                    modifier = Modifier.size(64.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Column {
                                    Text(
                                        text = "タルシエ先生の解説",
                                        color = Color(0xFF4A4A5E),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Text(
                                text = stringResource(
                                    R.string.correct_answer,
                                    question.correctOrder.joinToString(" ")
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(R.string.meaning, question.meaning),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.meaning, question.meaning),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
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
