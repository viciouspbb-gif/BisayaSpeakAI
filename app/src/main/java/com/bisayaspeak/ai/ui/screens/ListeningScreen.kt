package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
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
import androidx.compose.runtime.getValue
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
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.ads.AdsPolicy
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.google.android.flexbox.AlignItems
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 全端末共通ルール：広告ボタン黄色＝即実行
 */
private fun executeAdPlaybackIfReady(activity: Activity?, context: Context, viewModel: ListeningViewModel, rewardedAdLoaded: Boolean) {
    // 全端末共通ルール：黄色＝準備完了＝即再生
    if (rewardedAdLoaded) {
        Log.d("ListeningScreen", "Ad ready - executing immediate playback (universal rule)")
        
        if (activity != null) {
            AdManager.showRewardAd(
                activity = activity,
                onRewardEarned = {
                    // 広告視聴完了時のみヒントを復活
                    viewModel.recoverHintsThroughAd()
                    Log.d("ListeningScreen", "Reward earned, hints recovered through ad watching")
                },
                onAdClosed = {
                    Log.d("ListeningScreen", "Reward ad closed")
                }
            )
        } else {
            Log.e("ListeningScreen", "Activity is null, cannot show ad")
        }
    } else {
        // 黄色でない場合は何もしない（表示の嘘を完全排除）
        Log.w("ListeningScreen", "Ad not ready - button should be disabled, no action taken")
    }
}

@Composable
fun ListeningScreen(
    navController: NavHostController,
    level: Int,
    viewModel: ListeningViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val session by viewModel.session.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val lessonResult by viewModel.lessonResult.collectAsState()
    val clearedLevel by viewModel.clearedLevel.collectAsState()
    val comboCount by viewModel.comboCount.collectAsState()
    val voiceHintRemaining by viewModel.voiceHintRemaining.collectAsState()
    val showHintRecoveryDialog by viewModel.showHintRecoveryDialog.collectAsState()
    val rewardedAdLoaded by viewModel.rewardedAdLoaded.collectAsState()
    val rewardedAdState by viewModel.rewardedAdState.collectAsState()
    val selectedWords by viewModel.selectedWords.collectAsState()
    val shuffledWords by viewModel.shuffledWords.collectAsState()

    var navigationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(session?.completed) {
        if (session?.completed != true) {
            navigationTriggered = false
        }
    }

    fun handleBackNavigation() {
        if (session?.completed == true) {
            navController.navigate(AppRoute.LevelSelection.route) {
                popUpTo(AppRoute.Listening.route) { inclusive = true }
            }
        } else {
            // レッスンキャンセル時にも広告カウンターを更新
            viewModel.incrementAdCounter()
            
            // 広告表示が必要な場合は強制表示
            if (activity != null) {
                AdManager.checkAndShowInterstitial(activity) {
                    navController.popBackStack()
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    fun requestHintPlayback() {
        if (isPlaying) return
        viewModel.processHintRequest()
    }

    LaunchedEffect(level) {
        Log.d("ListeningScreen", "Loading questions for level $level")
        viewModel.loadQuestions(level)
    }
    
    // セッション状態を監視し、UIが確実に更新されるようにする
    LaunchedEffect(session) {
        val currentSession = session
        Log.d("ListeningScreen", "Session state changed: ${currentSession?.let { "questions=${it.questions.size}, completed=${it.completed}" } ?: "null"}")
        if (currentSession != null && currentSession.questions.isNotEmpty()) {
            Log.d("ListeningScreen", "Session loaded successfully with ${currentSession.questions.size} questions")
        }
    }
    
    // 現在の問題変化を監視
    LaunchedEffect(currentQuestion) {
        Log.d("ListeningScreen", "Current question changed: ${currentQuestion?.phrase}")
    }

    LaunchedEffect(lessonResult, clearedLevel, session?.completed) {
        val result = lessonResult
        val currentSession = session
        if (currentSession?.completed == true && result != null) {
            val levelCleared = clearedLevel
            val displayLevel = levelCleared ?: level
            val leveledUpFlag = result.leveledUp.toString()
            // AppRoute.LessonResult.route を使用して正しいルートを生成
            val destinationRoute = AppRoute.LessonResult.route
                .replace("{correctCount}", result.correctCount.toString())
                .replace("{totalQuestions}", result.totalQuestions.toString())
                .replace("{earnedXP}", result.xpEarned.toString())
                .replace("{clearedLevel}", displayLevel.toString())
                .replace("{leveledUp}", leveledUpFlag)
            val navigateToResult = {
                if (!navigationTriggered) {
                    navigationTriggered = true
                    navController.navigate(destinationRoute) {
                        popUpTo(AppRoute.Listening.route) { inclusive = true }
                    }
                    viewModel.clearLessonCompletion()
                }
            }
            if (result.leveledUp) {
                LessonStatusManager.setLessonCleared(context, level)
            }
            if (activity != null) {
                Log.e("DEBUG_ADS", "[ListeningScreen] 広告表示開始 (LaunchedEffect)")
                AdManager.showInterstitialWithTimeout(
                    activity = activity,
                    timeoutMs = 2_000L
                ) {
                    Log.e("DEBUG_ADS", "[ListeningScreen] navigateToResult 実行直前 (LaunchedEffect)")
                    Log.e("ListeningScreen", "★★★ TIMEOUT OR AD CLOSED, FORCING NAVIGATION ★★★")
                    navigateToResult()
                }
            } else {
                Log.e("DEBUG_ADS", "[ListeningScreen] Activityがnullのため直接navigateToResult 実行直前")
                Log.e("ListeningScreen", "★★★ NO ACTIVITY, FORCING NAVIGATION ★★★")
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
                    AdMobBanner(adUnitId = AdUnitIds.BANNER_MAIN)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.char_owl),
                            contentDescription = "フクロウ先生",
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                        val adsEnabled = AdsPolicy.areAdsEnabled
                        val hintButtonEnabled = !isPlaying && (
                            voiceHintRemaining > 0 ||
                                rewardedAdState == ListeningViewModel.RewardAdState.READY ||
                                rewardedAdState == ListeningViewModel.RewardAdState.FAILED ||
                                !adsEnabled
                            )
                        Button(
                            onClick = {
                                when {
                                    voiceHintRemaining > 0 -> requestHintPlayback()
                                    !adsEnabled -> viewModel.forceHintPlaybackWithoutAds()
                                    rewardedAdState == ListeningViewModel.RewardAdState.READY -> executeAdPlaybackIfReady(activity, context, viewModel, true)
                                    rewardedAdState == ListeningViewModel.RewardAdState.FAILED -> viewModel.forceHintPlaybackWithoutAds()
                                    else -> Log.d("ListeningScreen", "Hint button pressed but ad still loading")
                                }
                            },
                            enabled = hintButtonEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    voiceHintRemaining > 0 -> Color(0xFF2D3246)
                                    rewardedAdState == ListeningViewModel.RewardAdState.READY -> Color(0xFFFFA726)
                                    rewardedAdState == ListeningViewModel.RewardAdState.FAILED || !adsEnabled -> Color(0xFF4E342E)
                                    else -> Color(0xFF333333)
                                },
                                disabledContainerColor = Color(0xFF333333)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    voiceHintRemaining > 0 -> "ヒント ($voiceHintRemaining)"
                                    rewardedAdState == ListeningViewModel.RewardAdState.READY && adsEnabled -> "広告で回復"
                                    rewardedAdState == ListeningViewModel.RewardAdState.FAILED || !adsEnabled -> "ヒントを見る"
                                    else -> "広告準備中..."
                                },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question?.meaning?.ifBlank { question?.phrase } ?: "",
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
                            slotCount = question?.correctOrder?.size ?: 0,
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
                                        viewModel.selectWord(word) // 音声フィードバックはViewModel側で実行
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
                        if (rewardedAdLoaded) {
                            // 広告がプリロード済みの場合、即時表示
                            AdManager.showRewardAd(
                                activity = activity,
                                onRewardEarned = {
                                    // 広告視聴完了時のみヒントを復活
                                    viewModel.recoverHintsThroughAd()
                                    Log.d("ListeningScreen", "Hint recovery ad watched (preloaded)")
                                    
                                    // 広告視聴後はヒントを回復するだけで、自動再生はしない
                                },
                                onAdClosed = {}
                            )
                        } else {
                            // 広告が準備できていない場合、ロードしてから表示
                            AdManager.showRewardAd(
                                activity = activity,
                                onRewardEarned = {
                                    // 広告視聴完了時のみヒントを復活
                                    viewModel.recoverHintsThroughAd()
                                    Log.d("ListeningScreen", "Hint recovery ad watched (on-demand)")
                                },
                                onAdClosed = {}
                            )
                        }
                    }
                }) {
                    Text(if (rewardedAdLoaded) "動画を見てヒントを回復" else "広告を読み込み中...")
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
