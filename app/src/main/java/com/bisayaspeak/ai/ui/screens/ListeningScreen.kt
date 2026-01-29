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
 * Universal rule: yellow ad button means ready to play immediately
 */
private fun executeAdPlaybackIfReady(activity: Activity?, context: Context, viewModel: ListeningViewModel, rewardedAdLoaded: Boolean) {
    // Universal rule: yellow = ready = play immediately
    if (rewardedAdLoaded) {
        Log.d("ListeningScreen", "Ad ready - executing immediate playback (universal rule)")

        if (activity != null) {
            AdManager.showRewardAd(
                activity = activity,
                onRewardEarned = {
                    // Recover hints only after reward is earned
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
        // If not yellow, do nothing (no misleading behavior)
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
            // Update ad counter even on lesson cancel
            viewModel.incrementAdCounter()

            // Force-show ad if needed
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

    // Observe session state so the UI updates reliably
    LaunchedEffect(session) {
        val currentSession = session
        Log.d("ListeningScreen", "Session state changed: ${currentSession?.let { "questions=${it.questions.size}, completed=${it.completed}" } ?: "null"}")
        if (currentSession != null && currentSession.questions.isNotEmpty()) {
            Log.d("ListeningScreen", "Session loaded successfully with ${currentSession.questions.size} questions")
        }
    }

    // Observe current question changes
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
            // Use AppRoute.LessonResult.route to generate the correct route
            val destinationRoute = AppRoute.LessonResult.route
                .replace("{correctCount}", result.correctCount.toString())
                .replace("{totalQuestions}", result.totalQuestions.toString())
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
                Log.e("DEBUG_ADS", "[ListeningScreen] Starting interstitial (LaunchedEffect)")
                AdManager.showInterstitialWithTimeout(
                    activity = activity,
                    timeoutMs = 2_000L
                ) {
                    Log.e("DEBUG_ADS", "[ListeningScreen] navigateToResult right before run (LaunchedEffect)")
                    Log.e("ListeningScreen", "★★★ TIMEOUT OR AD CLOSED, FORCING NAVIGATION ★★★")
                    navigateToResult()
                }
            } else {
                Log.e("DEBUG_ADS", "[ListeningScreen] Activity is null; navigateToResult directly")
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
                    // Header
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        IconButton(onClick = { handleBackNavigation() }, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        if (comboCount > 0 && !showResult) {
                            Text("🔥 ${comboCount} Combo!", color = Color(0xFFFFA726), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd))
                        }
                    }

                    // Mascot & hint button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.char_owl),
                            contentDescription = "Owl coach",
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
                                    voiceHintRemaining > 0 -> stringResource(R.string.listening_hint_remaining, voiceHintRemaining)
                                    rewardedAdState == ListeningViewModel.RewardAdState.READY && adsEnabled -> stringResource(R.string.listening_hint_recover_by_ad)
                                    rewardedAdState == ListeningViewModel.RewardAdState.FAILED || !adsEnabled -> stringResource(R.string.listening_hint_view)
                                    else -> stringResource(R.string.listening_ad_loading)
                                },
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prompt label
                    Text(
                        text = stringResource(R.string.listening_prompt_label),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Prompt
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

                    // Answer area
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.listening_your_answer), color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnswerSlots(
                            slotCount = question?.correctOrder?.size ?: 0,
                            selectedWords = selectedWords,
                            onRemoveWord = { index -> viewModel.removeWordAt(index) }
                        )
                    }

                    // Word selection & result card
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
                                        viewModel.selectWord(word) // Audio feedback is handled in the ViewModel
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
                            // If the ad is preloaded, show immediately
                            AdManager.showRewardAd(
                                activity = activity,
                                onRewardEarned = {
                                    // Recover hints only after reward is earned
                                    viewModel.recoverHintsThroughAd()
                                    Log.d("ListeningScreen", "Hint recovery ad watched (preloaded)")

                                    // After watching: recover hints only (no auto playback)
                                },
                                onAdClosed = {}
                            )
                        } else {
                            // If the ad is not ready, show on-demand
                            AdManager.showRewardAd(
                                activity = activity,
                                onRewardEarned = {
                                    // Recover hints only after reward is earned
                                    viewModel.recoverHintsThroughAd()
                                    Log.d("ListeningScreen", "Hint recovery ad watched (on-demand)")
                                },
                                onAdClosed = {}
                            )
                        }
                    }
                }) {
                    Text(
                        text = if (rewardedAdLoaded) {
                            stringResource(R.string.listening_recover_hint_watch_video)
                        } else {
                            stringResource(R.string.listening_ad_loading)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissHintRecoveryDialog() }) {
                    Text(stringResource(R.string.close))
                }
            },
            title = { Text(stringResource(R.string.listening_no_hints_title)) },
            text = { Text(stringResource(R.string.listening_no_hints_message)) }
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
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.char_tarsier),
                contentDescription = "Tarsier coach",
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isCorrect) {
                    stringResource(R.string.listening_result_correct)
                } else {
                    stringResource(R.string.listening_result_incorrect)
                },
                color = if (isCorrect) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
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
                    Text(stringResource(R.string.listening_next), color = Color.White, fontWeight = FontWeight.Bold)
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
