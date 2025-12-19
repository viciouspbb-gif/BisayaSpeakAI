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
import com.bisayaspeak.ai.data.model.PracticeData
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.PronunciationStatus
import com.bisayaspeak.ai.data.repository.PronunciationRepository
import com.bisayaspeak.ai.data.repository.PronunciationFeedbackRepository
import com.bisayaspeak.ai.ui.ads.AdMobManager
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.util.AudioRecorder
import com.bisayaspeak.ai.util.PronunciationThreshold
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import androidx.activity.compose.BackHandler
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
        PracticeData.getRandomQuestions(category, 5)
    }
    
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
                            text = "問題 ${currentQuestionIndex + 1} / ${questions.size}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "戻る",
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
        if (currentQuestion != null) {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                // 単語表示領域（スクロール対応で柔軟に）
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bisaya Text
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

                    // Japanese Translation
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
                                text = "日本語",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion.japanese,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                softWrap = true
                            )
                        }
                    }

                    // English Translation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp),
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
                                text = "English",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion.english,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                softWrap = true
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 録音・フィードバック領域（スクロール対応で柔軟に）
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pro版専用：判定モード切り替え
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
                                    text = "判定モード：${if (proJudgmentMode == PronunciationThreshold.ProJudgmentMode.STRICT) "厳密" else "甘め"}",
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
                                        // モード変更時にフィードバックをクリア
                                        aiFeedback = null
                                    }
                                )
                            }
                        }
                    }
                    
                    // Play Sample Button
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
                        Text("サンプル音声を再生", fontSize = 14.sp)
                    }
                    
                    // マイクアイコンボタン（常に表示）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // マイクアイコン
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
                            
                            // ワンタップで録音開始 → 無音検知で自動停止 → 自動判定
                            scope.launch {
                                try {
                                    val file = File(context.cacheDir, "practice_${System.currentTimeMillis()}.wav")
                                    audioFile = file
                                    
                                    isRecording = true
                                    result = null
                                    errorMessage = null
                                    
                                    // AudioRecorderで録音（無音検知付き）
                                    val recordingResult = audioRecorder.startRecording(file)
                                    
                                    isRecording = false
                                    
                                    // 🔍 デバッグログ追加
                                    android.util.Log.d("DEBUG", "Recording result - File: ${recordingResult.file?.absolutePath}")
                                    android.util.Log.d("DEBUG", "File size: ${recordingResult.file?.length() ?: 0} bytes")
                                    android.util.Log.d("DEBUG", "Silent: ${recordingResult.isSilent}")
                                    android.util.Log.d("DEBUG", "Duration: ${recordingResult.duration} ms")
                                    
                                    // 録音結果を確認
                                    if (recordingResult.file == null) {
                                        android.util.Log.e("DEBUG", "❌ Recording file is NULL!")
                                        errorMessage = "録音に失敗しました"
                                        result = PronunciationStatus.TRY_AGAIN
                                        return@launch
                                    }
                                    
                                    // 無音判定（最初から3秒無音）
                                    if (recordingResult.isSilent) {
                                        android.util.Log.w("DEBUG", "⚠️ Recording is SILENT! No sound detected.")
                                        result = PronunciationStatus.TRY_AGAIN
                                        return@launch
                                    }
                                    
                                    android.util.Log.d("PracticeQuiz", "Starting pronunciation check for word: ${currentQuestion.bisaya}")
                                    
                                    // PCM → WAV変換（WAVヘッダを付与）
                                    val wavFile = File(context.cacheDir, "final_${System.currentTimeMillis()}.wav")
                                    AudioUtil.pcmToWav(recordingResult.file!!, wavFile, 16000)
                                    audioFile = wavFile
                                    
                                    android.util.Log.d("PracticeQuiz", "PCM converted to WAV: ${wavFile.absolutePath}, size: ${wavFile.length()} bytes")
                                    
                                    // 自動判定
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
                                                
                                                // スコアを保存
                                                currentScore = score
                                                
                                                // Lite版/Pro版の判定基準を使用（Pro版は判定モードを適用）
                                                result = PronunciationThreshold.getStatus(score, isPremium, proJudgmentMode)
                                                
                                                // 判定履歴に追加
                                                judgmentHistory = judgmentHistory + (score to result!!)
                                                
                                                // デバッグログ（内部ログのみ、UI非表示）
                                                android.util.Log.i("PracticeQuiz", "═══════════════════════════════════════")
                                                android.util.Log.i("PracticeQuiz", "📝 単語: ${currentQuestion.bisaya}")
                                                android.util.Log.i("PracticeQuiz", "🎯 スコア: $score / 100")
                                                android.util.Log.i("PracticeQuiz", "✅ 判定: $result")
                                                android.util.Log.i("PracticeQuiz", "📊 閾値: ${PronunciationThreshold.getThresholdInfo(isPremium, proJudgmentMode)}")
                                                android.util.Log.i("PracticeQuiz", "🔄 試行回数: ${judgmentHistory.size}")
                                                android.util.Log.i("PracticeQuiz", "═══════════════════════════════════════")
                                                
                                                // Pro版専用：ChatGPT連携でフィードバックを取得
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
                                                errorMessage = "評価に失敗しました"
                                            }
                                        } catch (e: Exception) {
                                            result = PronunciationStatus.TRY_AGAIN
                                            errorMessage = "エラー: ${e.message}"
                                        } finally {
                                            isAnalyzing = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    isRecording = false
                                    errorMessage = "録音エラー: ${e.message}"
                                    result = PronunciationStatus.TRY_AGAIN
                                    android.util.Log.e("PracticeQuiz", "Recording error", e)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(90.dp)
                            .scale(scale),
                        containerColor = when {
                            isRecording -> Color(0xFFE91E63) // 録音中は紫ピンク
                            isAnalyzing -> Color(0xFF9E9E9E) // 評価中はグレー
                            result == PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336) // Try Again時は赤
                            else -> Color(0xFF03DAC5) // 通常は青緑
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
                    
                    // 状態テキスト
                    Text(
                        text = when {
                            isAnalyzing -> "評価中..."
                            isRecording -> "録音中..."
                            result == PronunciationStatus.TRY_AGAIN -> "もう一度録音してみましょう"
                            else -> "タップして録音"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isRecording -> Color(0xFFE91E63)
                            isAnalyzing -> Color.Gray
                            result == PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336) // Try Again時は赤
                            else -> Color.White
                        }
                    )
                    
                    // 🔍 デバッグ: result の状態を常に表示
                    if (result != null) {
                        android.util.Log.d("PracticeQuiz", "🎨 Rendering result UI: $result")
                    }
                    
                    // 判定結果をマイクの下に表示（フェードインアニメーション）
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
                                            PronunciationStatus.PERFECT -> Color(0xFF4CAF50)  // 緑
                                            PronunciationStatus.OKAY -> Color(0xFFFFC107)     // 黄
                                            PronunciationStatus.TRY_AGAIN -> Color(0xFFF44336) // 赤
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Text(
                                        text = when (resultValue) {
                                            PronunciationStatus.PERFECT -> "Perfect!"
                                            PronunciationStatus.OKAY -> "Okay"
                                            PronunciationStatus.TRY_AGAIN -> "Try Again"
                                        },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                    )
                                }
                                Text(
                                    text = when (resultValue) {
                                        PronunciationStatus.PERFECT -> "よくできました！🎉"
                                        PronunciationStatus.OKAY -> "良い発音です！もう少しで完璧！👍"
                                        PronunciationStatus.TRY_AGAIN -> "もう一度録音してみましょう"
                                    },
                                    fontSize = 14.sp,
                                    color = when (resultValue) {
                                        PronunciationStatus.PERFECT -> Color.White  // 白（緑背景に映える）
                                        PronunciationStatus.OKAY -> Color.White     // 白（黄背景に映える）
                                        PronunciationStatus.TRY_AGAIN -> Color.White // 白（赤背景に映える）
                                    },
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                
                            }
                        }
                    }
                    }
                    
                    // Pro版専用：AIフィードバック表示
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
                                        text = "AI発音コーチ",
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
                    
                    // Error Message
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
                    
                    // Try Again時の再録音促進UI（アニメーション付き）
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
                                    text = "もう一度挑戦しましょう",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF44336)
                                )
                                Text(
                                    text = "マイクボタンをタップして再録音",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        }
                    }
                    
                    // 次へボタン（Perfect/Okayの場合のみ表示）
                    AnimatedVisibility(
                        visible = result != null && result != PronunciationStatus.TRY_AGAIN,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Button(
                            onClick = {
                                // 次の問題へ
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
                                    PronunciationStatus.PERFECT -> Color(0xFF4CAF50)  // 緑
                                    PronunciationStatus.OKAY -> Color(0xFFFFC107)     // 黄
                                    else -> Color(0xFF03DAC5)  // それ以外は青緑
                                }
                            ),
                            enabled = result != null
                        ) {
                            Text(
                                text = if (currentQuestionIndex < questions.size - 1) "次の問題へ" else "完了",
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
        }
    }
}
