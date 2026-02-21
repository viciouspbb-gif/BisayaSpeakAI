package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.R
import androidx.core.content.ContextCompat
import com.bisayaspeak.ai.data.model.PracticeItem
import com.bisayaspeak.ai.util.AudioUtil
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.PronunciationStatus
import com.bisayaspeak.ai.data.repository.PracticeContentRepository
import com.bisayaspeak.ai.data.repository.PronunciationRepository
import com.bisayaspeak.ai.data.repository.PronunciationFeedbackRepository
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.util.AudioRecorder
import com.bisayaspeak.ai.util.PronunciationThreshold
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import androidx.activity.compose.BackHandler
import com.bisayaspeak.ai.util.LocaleUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeQuizScreen(
    category: String,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    isPremium: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pronunciationRepository = remember { PronunciationRepository() }
    val feedbackRepository = remember { PronunciationFeedbackRepository() }
    
    // セッション管理
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    var sessionStarted by remember { mutableStateOf(false) }
    var sessionCompletedCount by remember { mutableStateOf(0) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    // 5問をランダムに抽出
    val questions = remember(category) {
        try {
            PracticeContentRepository(context.applicationContext).getRandomQuestions(category, 5)
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    val isJapaneseLocale = remember { LocaleUtils.isJapanese(context) }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    val currentQuestion = questions.getOrNull(currentQuestionIndex)
    
    var isRecording by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PronunciationStatus?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Pro版専用：判定モードとフィードバック
    var proJudgmentMode by remember { mutableStateOf(PronunciationThreshold.ProJudgmentMode.STRICT) }
    var aiFeedback by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    ) }
    
    // 問題インデックス管理
    var lastQuestionIndex by remember { mutableStateOf(-1) }
    
    // スコアと判定履歴
    var currentScore by remember { mutableStateOf<Int?>(null) }
    var judgmentHistory by remember { mutableStateOf<List<Pair<Int, PronunciationStatus>>>(emptyList()) }
    
    val audioRecorder = remember { AudioRecorder() }
    var audioFile: File? by remember { mutableStateOf(null) }
    
    // 権限リクエスト
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    // TTS初期化
    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = ttsInstance?.setLanguage(Locale("fil", "PH"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance?.setLanguage(Locale.US)
                }
                ttsInstance?.setSpeechRate(0.7f)
            }
        }
        ttsInstance
    }
    
    // セッション開始
    LaunchedEffect(Unit) {
        sessionManager.startSession()
        sessionStarted = true
    }
    
    // 中断時の広告表示（統一ルール：中断 = 1回広告）
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            audioRecorder.stopRecording()
            if (sessionStarted && currentQuestionIndex < questions.size) {
                android.util.Log.d("PracticeQuiz", "Session interrupted, showing ad")
                val activity = context as? Activity
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }
    
    // バックボタン処理
    BackHandler {
        if (sessionStarted && currentQuestionIndex < questions.size) {
            val activity = context as? Activity
            sessionManager.onSessionInterrupted(activity) {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }
    
    // 問題が変わったときのリセット
    LaunchedEffect(currentQuestionIndex) {
        if (currentQuestionIndex != lastQuestionIndex) {
            lastQuestionIndex = currentQuestionIndex
            result = null
            errorMessage = null
            currentScore = null
            judgmentHistory = emptyList()
        }
    }
    
    // 5問終了時の処理（統一ルール：1セット完了 = 1回広告）
    LaunchedEffect(currentQuestionIndex) {
        if (currentQuestionIndex >= questions.size && sessionStarted) {
            android.util.Log.d("PracticeQuiz", "5 questions completed, showing ad")
            sessionCompletedCount++
            val activity = context as? Activity
            
            // 広告表示後、5回ごとにUpgrade提案
            sessionManager.onSessionComplete(activity) {
                scope.launch {
                    if (!isPremium && sessionCompletedCount % 5 == 0) {
                        showUpgradeDialog = true
                    } else {
                        delay(500)
                        onNavigateBack()
                    }
                }
            }
        }
    }
    
    // Upgradeダイアログ
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUpgradeDialog = false
                onNavigateBack()
            },
            title = { Text(stringResource(R.string.upgrade_suggestion_title)) },
            text = { Text(stringResource(R.string.upgrade_suggestion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpgradeDialog = false
                        onNavigateToUpgrade()
                    }
                ) {
                    Text(stringResource(R.string.upgrade_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUpgradeDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(
                                R.string.practice_quiz_question_progress,
                                currentQuestionIndex + 1,
                                questions.size
                            ),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
            // 広告バナー（スクロール対象外で画面下部に固定）
            SmartAdBanner(isPremium = isPremium)
        },
        containerColor = Color.Black
    ) { padding ->
        currentQuestion?.let {
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val needsScroll = maxHeight < 720.dp
                val scrollState = rememberScrollState()

                val promptSection: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Bisaya",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentQuestion.bisaya,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    softWrap = true
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                val translationLabel = if (isJapaneseLocale) {
                                    stringResource(R.string.language_japanese)
                                } else {
                                    stringResource(R.string.english)
                                }
                                val translationText = if (isJapaneseLocale) {
                                    currentQuestion.japanese.ifBlank { currentQuestion.english }
                                } else {
                                    currentQuestion.english.ifBlank { currentQuestion.japanese }
                                }

                                Text(
                                    text = translationLabel,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = translationText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }

                val practiceSection: @Composable () -> Unit = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isPremium) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.practice_quiz_judgment_mode,
                                            stringResource(
                                                if (proJudgmentMode == PronunciationThreshold.ProJudgmentMode.STRICT) {
                                                    R.string.practice_quiz_judgment_mode_strict
                                                } else {
                                                    R.string.practice_quiz_judgment_mode_lenient
                                                }
                                            )
                                        ),
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = proJudgmentMode == PronunciationThreshold.ProJudgmentMode.LENIENT,
                                        onCheckedChange = { isLenient ->
                                            proJudgmentMode = if (isLenient) {
                                                PronunciationThreshold.ProJudgmentMode.LENIENT
                                            } else {
                                                PronunciationThreshold.ProJudgmentMode.STRICT
                                            }
                                            aiFeedback = null
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                tts?.speak(currentQuestion.bisaya, TextToSpeech.QUEUE_FLUSH, null, null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EE)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.practice_quiz_play_sample_audio), fontSize = 14.sp)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "recording")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = if (isRecording) 1.15f else 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            FloatingActionButton(
                                onClick = {
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@FloatingActionButton
                                    }

                                    if (isRecording || isAnalyzing) return@FloatingActionButton

                                    scope.launch {
                                        try {
                                            val file = File(context.cacheDir, "practice_${System.currentTimeMillis()}.wav")
                                            audioFile = file

                                            isRecording = true
                                            result = null
                                            errorMessage = null

                                            val recordingResult = audioRecorder.startRecording(file)

                                            isRecording = false

                                            android.util.Log.d("DEBUG", "Recording result - File: ${recordingResult.file?.absolutePath}")
                                            android.util.Log.d("DEBUG", "File size: ${recordingResult.file?.length() ?: 0} bytes")
                                            android.util.Log.d("DEBUG", "Silent: ${recordingResult.isSilent}")
                                            android.util.Log.d("DEBUG", "Duration: ${recordingResult.duration} ms")

                                            if (recordingResult.file == null) {
                                                android.util.Log.e("DEBUG", "❌ Recording file is NULL!")
                                                errorMessage = context.getString(R.string.practice_quiz_recording_failed)
                                                result = PronunciationStatus.TRY_AGAIN
                                                return@launch
                                            }

                                            if (recordingResult.isSilent) {
                                                android.util.Log.w("DEBUG", "⚠️ Recording is SILENT! No sound detected.")
                                                result = PronunciationStatus.TRY_AGAIN
                                                return@launch
                                            }

                                            android.util.Log.d("PracticeQuiz", "Starting pronunciation check for word: ${currentQuestion.bisaya}")

                                            val wavFile = File(context.cacheDir, "final_${System.currentTimeMillis()}.wav")
                                            AudioUtil.pcmToWav(recordingResult.file!!, wavFile, 16000)
                                            audioFile = wavFile

                                            android.util.Log.d("PracticeQuiz", "PCM converted to WAV: ${wavFile.absolutePath}, size: ${wavFile.length()} bytes")

                                            wavFile.let { file ->
                                                isAnalyzing = true
                                                android.util.Log.d("PracticeQuiz", "Analyzing audio file: ${file.absolutePath}, size: ${file.length()} bytes")
                                                try {
                                                    val apiResult = pronunciationRepository.checkPronunciation(
                                                        audioFile = file,
                                                        word = currentQuestion.bisaya,
                                                        level = LearningLevel.BEGINNER
                                                    )

                                                    android.util.Log.d("PracticeQuiz", "API result: success=${apiResult.isSuccess}")

                                                    if (apiResult.isSuccess) {
                                                        val response = apiResult.getOrNull()
                                                        val score = response?.score ?: 0

                                                        currentScore = score
                                                        result = PronunciationThreshold.getStatus(score, isPremium, proJudgmentMode)
                                                        judgmentHistory = judgmentHistory + (score to result!!)

                                                        android.util.Log.i("PracticeQuiz", "=======================================")
                                                        android.util.Log.i("PracticeQuiz", "Word: ${currentQuestion.bisaya}")
                                                        android.util.Log.i("PracticeQuiz", "Score: $score / 100")
                                                        android.util.Log.i("PracticeQuiz", "Result: $result")
                                                        android.util.Log.i("PracticeQuiz", "Thresholds: ${PronunciationThreshold.getThresholdInfo(isPremium, proJudgmentMode)}")
                                                        android.util.Log.i("PracticeQuiz", "Attempts: ${judgmentHistory.size}")
                                                        android.util.Log.i("PracticeQuiz", "=======================================")

                                                        if (isPremium && result == PronunciationStatus.TRY_AGAIN) {
                                                            scope.launch {
                                                                val feedbackResult = feedbackRepository.getPronunciationFeedback(
                                                                    word = currentQuestion.bisaya,
                                                                    score = score,
                                                                    targetLanguage = "Bisaya"
                                                                )
                                                                if (feedbackResult.isSuccess) {
                                                                    aiFeedback = feedbackResult.getOrNull()
                                                                    android.util.Log.d("PracticeQuiz", "AI Feedback: $aiFeedback")
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        result = PronunciationStatus.TRY_AGAIN
                                                        errorMessage = context.getString(R.string.practice_quiz_evaluation_failed)
                                                    }
                                                } catch (e: Exception) {
                                                    result = PronunciationStatus.TRY_AGAIN
                                                    errorMessage = context.getString(
                                                        R.string.practice_quiz_error_generic,
                                                        e.message ?: ""
                                                    )
                                                } finally {
                                                    isAnalyzing = false
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isRecording = false
                                            errorMessage = context.getString(
                                                R.string.practice_quiz_recording_error,
                                                e.message ?: ""
                                            )
                                            result = PronunciationStatus.TRY_AGAIN
                                            android.util.Log.e("PracticeQuiz", "Recording error", e)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(scale),
                                containerColor = when {
                                    isRecording -> Color(0xFFE91E63)
                                    isAnalyzing -> Color(0xFF9E9E9E)
                                    result == PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336)
                                    else -> Color(0xFF03DAC5)
                                },
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = if (isRecording) 12.dp else 6.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record",
                                    modifier = Modifier.size(44.dp),
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = when {
                                    isAnalyzing -> stringResource(R.string.practice_quiz_status_analyzing)
                                    isRecording -> stringResource(R.string.practice_quiz_status_recording)
                                    result == PronunciationStatus.TRY_AGAIN -> stringResource(R.string.practice_quiz_status_try_again)
                                    else -> stringResource(R.string.practice_quiz_status_tap_to_record)
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isRecording -> Color(0xFFE91E63)
                                    isAnalyzing -> Color.Gray
                                    result == PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336)
                                    else -> Color.White
                                }
                            )

                            if (result != null) {
                                android.util.Log.d("PracticeQuiz", "🎨 Rendering result UI: $result")
                            }

                            AnimatedVisibility(
                                visible = result != null,
                                enter = fadeIn() + expandVertically() + androidx.compose.animation.scaleIn(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                val resultValue = result
                                if (resultValue != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (resultValue) {
                                                    PronunciationStatus.PERFECT -> Color(0xFF4CAF50)
                                                    PronunciationStatus.OKAY -> Color(0xFFFFC107)
                                                    PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336)
                                                }
                                            ),
                                            elevation = CardDefaults.cardElevation(4.dp)
                                        ) {
                                            Text(
                                                text = when (resultValue) {
                                                    PronunciationStatus.PERFECT -> stringResource(R.string.practice_quiz_result_perfect)
                                                    PronunciationStatus.OKAY -> stringResource(R.string.practice_quiz_result_okay)
                                                    PronunciationStatus.TRY_AGAIN -> stringResource(R.string.practice_quiz_result_try_again)
                                                },
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                            )
                                        }
                                        Text(
                                            text = when (resultValue) {
                                                PronunciationStatus.PERFECT -> stringResource(R.string.practice_quiz_feedback_perfect)
                                                PronunciationStatus.OKAY -> stringResource(R.string.practice_quiz_feedback_okay)
                                                PronunciationStatus.TRY_AGAIN -> stringResource(R.string.practice_quiz_status_try_again)
                                            },
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (isPremium && aiFeedback != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A237E).copy(alpha = 0.9f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "🤖",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.practice_quiz_ai_pronunciation_coach),
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = aiFeedback!!,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFC107)
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedVisibility(
                            visible = result == PronunciationStatus.TRY_AGAIN,
                            enter = fadeIn() + expandVertically() + androidx.compose.animation.slideInVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.05f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(1000),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .scale(pulseScale),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF44336))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "🎤",
                                        fontSize = 32.sp
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.practice_quiz_try_again_title),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF44336)
                                        )
                                        Text(
                                            text = stringResource(R.string.practice_quiz_try_again_subtitle),
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = result != null && result != PronunciationStatus.TRY_AGAIN,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Button(
                                onClick = {
                                    result = null
                                    errorMessage = null
                                    currentQuestionIndex++
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (result) {
                                        PronunciationStatus.PERFECT -> Color(0xFF4CAF50)
                                        PronunciationStatus.OKAY -> Color(0xFFFFC107)
                                        else -> Color(0xFF03DAC5)
                                    }
                                ),
                                enabled = result != null
                            ) {
                                Text(
                                    text = if (currentQuestionIndex < questions.size - 1) {
                                        stringResource(R.string.practice_quiz_next_question)
                                    } else {
                                        stringResource(R.string.done)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                val baseModifier = if (needsScroll) {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                }

                Column(
                    modifier = baseModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (needsScroll) {
                        promptSection()
                        Spacer(modifier = Modifier.height(16.dp))
                        practiceSection()
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxWidth()
                        ) {
                            promptSection()
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxWidth()
                        ) {
                            practiceSection()
                        }
                    }
                }
            }
        }
    }
}
