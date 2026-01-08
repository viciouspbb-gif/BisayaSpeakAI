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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.ui.viewmodel.PracticeViewModel
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.PronunciationStatus
import com.bisayaspeak.ai.data.repository.PronunciationRepository
import com.bisayaspeak.ai.data.repository.PronunciationFeedbackRepository
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.util.AudioRecorder
import com.bisayaspeak.ai.util.PronunciationThreshold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeWordDetailScreen(
    id: String,
    onNavigateBack: () -> Unit,
    isPremium: Boolean = false,
    viewModel: PracticeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val practiceViewModel: PracticeViewModel = viewModel()
    val pronunciationRepository = remember { PronunciationRepository() }
    val feedbackRepository = remember { PronunciationFeedbackRepository() }
    
    val word = practiceViewModel.getItemById(id)
    
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
    
    // 広告連動用のカウンター
    var tryAgainCount by remember { mutableStateOf(0) }
    var perfectCount by remember { mutableStateOf(0) }
    var silentTryAgainCount by remember { mutableStateOf(0) } // 無音Try Againカウント
    var lastWordId by remember { mutableStateOf("") }
    
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
    
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
            audioRecorder.stopRecording()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        word?.bisaya ?: "Word Detail",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
        if (word != null) {
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
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Bisaya",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.bisaya,
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
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "日本語",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.japanese,
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
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "English",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.english,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                softWrap = true
                            )
                        }
                    }
                }
                
                // 補足説明（descriptionがある場合のみ表示）
                word.description?.let { desc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = Color(0xFFB0B0B0),
                                lineHeight = 16.sp
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

                    // Play Sample Button
                    Button(
                        onClick = {
                            tts?.speak(word.bisaya, TextToSpeech.QUEUE_FLUSH, null, null)
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
                    
                        val isPronunciationMaintenance = true
                        FloatingActionButton(
                            onClick = {
                            if (isPronunciationMaintenance) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("現在アップグレード準備中です (Under maintenance for upgrade)")
                                }
                                return@FloatingActionButton
                            }
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@FloatingActionButton
                            }
                            
                            if (isRecording || isAnalyzing) return@FloatingActionButton
                            
                            // ワンタップで録音開始 → 無音検知で自動停止 → 自動判定
                            scope.launch {
                                try {
                                    // 単語が変わったらカウンターリセット
                                    if (lastWordId != id) {
                                        tryAgainCount = 0
                                        perfectCount = 0
                                        silentTryAgainCount = 0
                                        lastWordId = id
                                    }
                                    
                                    val file = File(context.cacheDir, "practice_${System.currentTimeMillis()}.pcm")
                                    audioFile = file
                                    
                                    isRecording = true
                                    result = null
                                    errorMessage = null
                                    
                                    // AudioRecorderで録音（無音検知付き）
                                    val recordingResult = audioRecorder.startRecording(file)
                                    
                                    isRecording = false
                                    
                                    // 録音結果を確認
                                    if (recordingResult.file == null) {
                                        errorMessage = "録音に失敗しました"
                                        result = PronunciationStatus.TRY_AGAIN
                                        return@launch
                                    }
                                    
                                    // 無音判定（最初から3秒無音）
                                    if (recordingResult.isSilent) {
                                        result = PronunciationStatus.TRY_AGAIN
                                        silentTryAgainCount++
                                        
                                        // 無音Try Againが3回連続 → インタースティシャル広告
                                        if (!isPremium && silentTryAgainCount >= 3) {
                                            val activity = context as? Activity
                                            activity?.let { safeActivity ->
                                                AdManager.showInterstitialWithTimeout(safeActivity, timeoutMs = 3_000L) {
                                                    AdManager.loadInterstitial(context)
                                                }
                                            }
                                            silentTryAgainCount = 0
                                        }
                                        return@launch
                                    }
                                    
                                    // 音声があった場合は無音カウンターをリセット
                                    silentTryAgainCount = 0
                                    
                                    // 自動判定
                                    recordingResult.file.let { file ->
                                        isAnalyzing = true
                                        try {
                                            val apiResult = pronunciationRepository.checkPronunciation(
                                                audioFile = file,
                                                word = word.bisaya,
                                                level = LearningLevel.BEGINNER
                                            )
                                            
                                            if (apiResult.isSuccess) {
                                                val response = apiResult.getOrNull()
                                                val score = response?.score ?: 0
                                                
                                                // Lite版/Pro版の判定基準を使用（Pro版は判定モードを適用）
                                                result = PronunciationThreshold.getStatus(score, isPremium, proJudgmentMode)
                                                
                                                // デバッグログ（内部ログのみ、UI非表示）
                                                android.util.Log.i("PracticeWordDetail", "═══════════════════════════════════════")
                                                android.util.Log.i("PracticeWordDetail", "📝 単語: ${word.bisaya}")
                                                android.util.Log.i("PracticeWordDetail", "🎯 スコア: $score / 100")
                                                android.util.Log.i("PracticeWordDetail", "✅ 判定: $result")
                                                android.util.Log.i("PracticeWordDetail", "📊 閾値: ${PronunciationThreshold.getThresholdInfo(isPremium, proJudgmentMode)}")
                                                android.util.Log.i("PracticeWordDetail", "═══════════════════════════════════════")
                                                
                                                // Pro版専用：ChatGPT連携でフィードバックを取得
                                                if (isPremium && result == PronunciationStatus.TRY_AGAIN) {
                                                    scope.launch {
                                                        val feedbackResult = feedbackRepository.getPronunciationFeedback(
                                                            word = word.bisaya,
                                                            score = score,
                                                            targetLanguage = "Bisaya"
                                                        )
                                                        if (feedbackResult.isSuccess) {
                                                            aiFeedback = feedbackResult.getOrNull()
                                                            android.util.Log.d("PracticeWordDetail", "AI Feedback: $aiFeedback")
                                                        }
                                                    }
                                                }
                                                
                                                // 広告連動ロジック
                                                if (!isPremium) {
                                                    when (result) {
                                                        PronunciationStatus.TRY_AGAIN -> {
                                                            tryAgainCount++
                                                            perfectCount = 0
                                                            if (tryAgainCount >= 3) {
                                                                // 3回連続Try Again → インタースティシャル広告
                                                                val activity = context as? Activity
                                                                activity?.let { safeActivity ->
                                                                    AdManager.showInterstitialNow(safeActivity) {
                                                                        AdManager.loadInterstitial(context)
                                                                    }
                                                                }
                                                                tryAgainCount = 0
                                                            }
                                                        }
                                                        PronunciationStatus.PERFECT -> {
                                                            perfectCount++
                                                            tryAgainCount = 0
                                                            if (perfectCount >= 2) {
                                                                // 2回Perfect成功 → インタースティシャル広告
                                                                val activity = context as? Activity
                                                                activity?.let { safeActivity ->
                                                                    AdManager.showInterstitialNow(safeActivity) {
                                                                        AdManager.loadInterstitial(context)
                                                                    }
                                                                }
                                                                perfectCount = 0
                                                            }
                                                        }
                                                        PronunciationStatus.OKAY -> {
                                                            // カウンターリセット
                                                            tryAgainCount = 0
                                                            perfectCount = 0
                                                        }
                                                        else -> {}
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
                                    android.util.Log.e("PracticeWordDetail", "Recording error", e)
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
                    
                    // 判定結果をマイクの下に表示（フェードインアニメーション）
                    AnimatedVisibility(
                        visible = result != null,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        result?.let { resultValue ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
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
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    
                    // Try Again時の再録音促進UI
                    if (result == PronunciationStatus.TRY_AGAIN) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF44336))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎤",
                                    fontSize = 24.sp
                                )
                                Column {
                                    Text(
                                        text = "もう一度挑戦しましょう",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF44336)
                                    )
                                    Text(
                                        text = "マイクボタンをタップして再録音",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Word not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Word not found",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}
