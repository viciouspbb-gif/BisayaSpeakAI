package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.navigation.NavHostController
import com.bisayaspeak.ai.GameDataManager
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ads.AdMobBanner
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import java.util.Locale
import kotlin.math.roundToInt

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
    val lessonResult by viewModel.lessonResult.collectAsState()
    val clearedLevel by viewModel.clearedLevel.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()

    val voiceHintRemaining by viewModel.voiceHintRemaining.collectAsState()
    val showHintRecoveryDialog by viewModel.showHintRecoveryDialog.collectAsState()
    var hintPlaybackTrigger by remember { mutableStateOf(0) }

    // TTSの準備
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // フクロウ先生の声（全TTS共通）
    val speakWord = { word: String ->
        tts?.let {
            it.stop()
            it.setPitch(0.7f)
            it.setSpeechRate(0.9f)
            it.speak(word, TextToSpeech.QUEUE_FLUSH, null, "WordVoice_$word")
        }
    }

    val handleBackNavigation = {
        if (activity != null) {
            AdManager.checkAndShowInterstitial(activity) {
                if (session != null && !session.completed) {
                    sessionManager.onSessionInterrupted(activity) { onNavigateBack() }
                } else {
                    onNavigateBack()
                }
            }
        } else {
            onNavigateBack()
        }
    }

    DisposableEffect(context) {
        var ttsInit: TextToSpeech? = null

        ttsInit = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInit?.let { instance ->
                    var result = instance.setLanguage(Locale("id"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        result = instance.setLanguage(Locale("fil"))
                    }
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        instance.setLanguage(Locale.US)
                    }
                    instance.setPitch(0.7f)
                    instance.setSpeechRate(0.9f)
                }
            }
        }
        tts = ttsInit
        onDispose { ttsInit?.stop(); ttsInit?.shutdown() }
    }

    LaunchedEffect(hintPlaybackTrigger) {
        if (hintPlaybackTrigger == 0) return@LaunchedEffect
        viewModel.playAudio()
    }

    fun requestHintPlayback() {
        if (isPlaying) return
        viewModel.processHintRequest()
    }

    LaunchedEffect(level) {
        sessionManager.startSession()
        Log.d("ListeningScreen", "Loading questions for level $level")
        viewModel.loadQuestions(level)
    }
    
    // セッション状態を監視し、UIが確実に更新されるようにする
    LaunchedEffect(session) {
        Log.d("ListeningScreen", "Session state changed: ${session?.let { "questions=${it.questions.size}, completed=${it.completed}" } ?: "null"}")
        if (session != null && session.questions.isNotEmpty()) {
            Log.d("ListeningScreen", "Session loaded successfully with ${session.questions.size} questions")
        }
    }
    
    // 現在の問題変化を監視
    LaunchedEffect(currentQuestion) {
        Log.d("ListeningScreen", "Current question changed: ${currentQuestion?.phrase}")
    }

    LaunchedEffect(lessonResult, clearedLevel, session?.completed) {
        val result = lessonResult
        if (session?.completed == true && result != null) {
            val levelCleared = clearedLevel
            val displayLevel = levelCleared ?: level
            val leveledUpFlag = if (result.leveledUp) 1 else 0
            val destinationRoute = "result_screen/${result.correctCount}/${result.totalQuestions}/${result.xpEarned}/$displayLevel/$leveledUpFlag"
            val navigateToResult = {
                navController.navigate(destinationRoute) {
                    popUpTo(AppRoute.Listening.route) { inclusive = true }
                }
                viewModel.clearLessonCompletion()
            }
            if (result.leveledUp) {
                LessonStatusManager.setLessonCleared(context, level)
            }
            if (activity != null) {
                AdManager.checkAndShowInterstitial(activity) {
                    navigateToResult()
                }
            } else {
                navigateToResult()
            }
        }
    }

    BackHandler { handleBackNavigation() }

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
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner()
                }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.loading), color = Color.White)
                }
            } else if (currentQuestion != null && session != null) {
                val question = currentQuestion

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ヘッダー
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        IconButton(onClick = { handleBackNavigation() }, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        if (comboCount > 0 && !showResult) {
                            Text("🔥 ${comboCount} Combo!", color = Color(0xFFFFA726), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd))
                        }
                    }

                    // キャラ & ヒントボタン
                    Row(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.char_owl),
                            contentDescription = "Owl",
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                        Button(
                            onClick = { requestHintPlayback() },
                            enabled = !isPlaying,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voiceHintRemaining > 0) Color(0xFF2D3246) else Color(0xFFFFA726)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (voiceHintRemaining > 0) "ヒント ($voiceHintRemaining)" else "広告で回復",
                                color = Color.White, 
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 問題文ラベル
                    Text(
                        text = "問題文",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 問題文
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 回答エリア
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("あなたの回答", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnswerSlots(
                            slotCount = question.correctOrder.size,
                            selectedWords = selectedWords,
                            onRemoveWord = { index -> viewModel.removeWordAt(index) }
                        )
                    }

                    // 単語選択 & 結果カード
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column {
                            AnimatedVisibility(
                                visible = !showResult,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                FlexboxWordGrid(
                                    words = shuffledWords,
                                    selectedWords = selectedWords,
                                    showResult = showResult,
                                    onSelectWord = { word ->
                                        speakWord(word)
                                        viewModel.selectWord(word)
                                    }
                                )
                            }
                        }

                        Column {
                            AnimatedVisibility(
                                visible = showResult,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                ResultCard(
                                    isCorrect = isCorrect
                                )
                            }
                        }
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
                    if (activity != null) {
                        AdManager.showRewardAd(
                            activity = activity,
                            onRewardEarned = {
                                GameDataManager.recoverHints(context)
                                viewModel.onHintRecoveryEarned()
                                Toast.makeText(context, "ヒントが全回復しました！", Toast.LENGTH_SHORT).show()
                                
                                // 広告視聴後、すぐにヒントを再生
                                viewModel.processHintRequest()
                            },
                            onAdClosed = {}
                        )
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
            title = { Text("ヒントがありません") },
            text = { Text("動画広告を見るとヒントが3回分回復します。") }
        )
    }
}

// 以下、変更なしの部品
@Composable
private fun ResultCard(isCorrect: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCorrect) {
                Image(painterResource(id = R.drawable.char_tarsier), "Correct", Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("正解！完璧だね！", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            } else {
                Text("残念...", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                Text("ヒントを聞いてみて", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FlexboxWordGrid(words: List<String>, selectedWords: List<String>, showResult: Boolean, onSelectWord: (String) -> Unit) {
    val density = LocalDensity.current
    val horizontalPadding = with(density) { 16.dp.toPx().roundToInt() }
    val verticalPadding = with(density) { 10.dp.toPx().roundToInt() }
    val margin = with(density) { 6.dp.toPx().roundToInt() }
    val selectionCounts = selectedWords.groupingBy { it }.eachCount().toMutableMap()
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            FlexboxLayout(context).apply { flexWrap = FlexWrap.WRAP; justifyContent = JustifyContent.CENTER; alignItems = AlignItems.CENTER }
        },
        update = { flexbox ->
            flexbox.removeAllViews()
            words.forEach { word ->
                val shouldHide = selectionCounts[word]?.let { it > 0 } == true
                if (shouldHide) selectionCounts[word] = selectionCounts[word]!! - 1
                val chipColor = if (shouldHide) Color.Transparent else Color(0xFFEDE4F3)
                val textColor = if (shouldHide) Color.Transparent else Color.Black
                val drawable = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = with(density) { 16.dp.toPx() }; setColor(chipColor.toArgb()) }
                val textView = TextView(flexbox.context).apply {
                    text = word; typeface = Typeface.DEFAULT_BOLD; setTextColor(textColor.toArgb())
                    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                    background = drawable; isEnabled = !shouldHide && !showResult
                    layoutParams = FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(margin, margin, margin, margin) }
                    setOnClickListener { if (!shouldHide && !showResult) onSelectWord(word) }
                }
                flexbox.addView(textView)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerSlots(slotCount: Int, selectedWords: List<String>, onRemoveWord: (Int) -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(slotCount) { index ->
            val word = selectedWords.getOrNull(index)
            Box(
                modifier = Modifier
                    .widthIn(min = 60.dp).height(48.dp)
                    .background(Color(0xFF1C1F2E), RoundedCornerShape(8.dp))
                    .then(if (word == null) Modifier.dashedBorder(Color.Gray, 1.dp, 8.dp) else Modifier.border(2.dp, Color(0xFF6C63FF), RoundedCornerShape(8.dp)))
                    .padding(horizontal = 8.dp).clickable(enabled = word != null) { onRemoveWord(index) },
                contentAlignment = Alignment.Center
            ) { if (word != null) Text(word, color = Color.White, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun ListeningBottomBar(showResult: Boolean, sessionCompleted: Boolean, onNext: () -> Unit) {
    Surface(color = Color(0xFF111111)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (showResult && !sessionCompleted) {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))) {
                    Text("次へ →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private fun Modifier.dashedBorder(color: Color, strokeWidth: Dp, cornerRadius: Dp): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        drawRoundRect(color = color, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()), style = stroke)
    }
)