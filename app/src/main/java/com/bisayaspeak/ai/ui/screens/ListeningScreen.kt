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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    level: DifficultyLevel,
    isPremium: Boolean = false,
    onNavigateBack: () -> Unit,
    onShowRewardedAd: (() -> Unit) -> Unit = {},
    navController: NavHostController,
    viewModel: ListeningViewModel = viewModel()
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

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var localTts: TextToSpeech? = null
        val instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = localTts ?: return@TextToSpeech
                val preferred = Locale("fil")
                val fallback = Locale("id")
                val result = engine.setLanguage(preferred)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(fallback)
                }
            }
        }
        localTts = instance
        tts = instance
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
        }
    }
    
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
            if (session?.completed == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.loading),
                        color = Color.White
                    )
                }
            } else if (currentQuestion != null && session != null) {
                val question = currentQuestion
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ListeningHeader(session = session)
                    ListeningAudioCoachRow(
                        session = session,
                        questionType = question.type,
                        isPlaying = isPlaying,
                        onPlayAudio = { viewModel.playAudio() }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ListeningAnswerArea(
                            question = question,
                            selectedWords = selectedWords,
                            shuffledWords = shuffledWords,
                            showResult = showResult,
                            isCorrect = isCorrect,
                            onRemoveWordAt = { index -> viewModel.removeWordAt(index) },
                            onSelectWord = { word ->
                                tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
                                viewModel.selectWord(word)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
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
                val usedCount = selectedWords.count { it == word }
                val totalAvailable = words.count { it == word }
                val isUsed = usedCount >= totalAvailable
                val chipColor = if (isUsed) Color(0xFF2E2E3E) else Color(0xFFEDE4F3)
                val textColor = if (isUsed) Color.Gray else Color.Black

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
                    isEnabled = !isUsed && !showResult
                    layoutParams = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    setOnClickListener {
                        if (!isUsed && !showResult) {
                            onSelectWord(word)
                        }
                    }
                }
                flexbox.addView(textView)
            }
        }
    )
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
private fun ListeningAudioCoachRow(
    session: ListeningSession,
    questionType: QuestionType,
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
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val title = when (questionType) {
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
                if (questionType == QuestionType.LISTENING) {
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (questionType == QuestionType.TRANSLATION) {
                                "日本語から正しいセブアノ語を組み立ててみよう。音声ヒントはありません。"
                            } else {
                                "語順問題です。提示された単語を並べ替えて正しいフレーズを作ろう。"
                            },
                            color = Color(0xFF3D2C5E),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            fontSize = 14.sp
                        )
                    }
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

        FlexboxWordGrid(
            words = shuffledWords,
            selectedWords = selectedWords,
            showResult = showResult,
            onSelectWord = onSelectWord
        )

        AnimatedVisibility(visible = showResult) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE4F3)),
                shape = RoundedCornerShape(16.dp)
            ) {
                val explanationScrollState = rememberScrollState()
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
                                .heightIn(min = 140.dp)
                                .verticalScroll(explanationScrollState),
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
                                    Text(
                                        text = "続きはスクロールしてね",
                                        color = Color(0xFF8E8E9A),
                                        fontSize = 12.sp
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
