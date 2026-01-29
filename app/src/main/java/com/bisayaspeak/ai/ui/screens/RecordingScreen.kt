package com.bisayaspeak.ai.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.LearningContent
import com.bisayaspeak.ai.data.model.LearningLevel
import androidx.compose.ui.res.stringResource
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.util.LocaleUtils
import com.bisayaspeak.ai.ui.viewmodel.DiagnosisState
import com.bisayaspeak.ai.ui.viewmodel.RecordingState
import com.bisayaspeak.ai.ui.viewmodel.RecordingViewModel
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import android.app.Activity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    content: LearningContent,
    level: LearningLevel,
    onBackClick: () -> Unit,
    onResultReady: () -> Unit,
    onShowInterstitialAd: () -> Unit,
    onShowRewardedAd: () -> Unit = {},
    isPremium: Boolean = false,
    viewModel: RecordingViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Session management
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    var sessionStarted by remember { mutableStateOf(false) }
    
    val recordingState by viewModel.recordingState.collectAsState()
    val diagnosisState by viewModel.diagnosisState.collectAsState()
    val remainingCount by viewModel.remainingCount.collectAsState(initial = 0)
    val volumeLevel by viewModel.volumeLevel.collectAsState()

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        sessionManager.startSession()
        sessionStarted = true
        if (!micPermissionState.status.isGranted) {
            micPermissionState.launchPermissionRequest()
        }
    }

    // Show an ad on interruption (rule: interruption = 1 ad)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.reset()
            if (sessionStarted && diagnosisState !is DiagnosisState.Success) {
                android.util.Log.d("RecordingScreen", "Session interrupted, showing ad")
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }
    
    // Back handling
    BackHandler {
        if (sessionStarted && diagnosisState !is DiagnosisState.Success) {
            sessionManager.onSessionInterrupted(activity) {
                onBackClick()
            }
        } else {
            onBackClick()
        }
    }

    // On success: show ad then navigate to result (rule: 1 set complete = 1 ad)
    LaunchedEffect(diagnosisState) {
        if (diagnosisState is DiagnosisState.Success) {
            android.util.Log.d("RecordingScreen", "Diagnosis success, showing ad")
            sessionManager.onSessionComplete(activity) {
                onResultReady()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recording_title), fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Remaining attempts
            RemainingCountCard(remaining = remainingCount)

            Spacer(Modifier.height(24.dp))

            // Reference card
            ContentCard(
                content = content,
                level = level,
                isPlaying = viewModel.isPlayingReference.collectAsState().value,
                onTogglePlay = {
                    if (viewModel.isPlayingReference.value) {
                        viewModel.stopPlayingReference()
                    } else {
                        viewModel.playReferenceAudio(content.bisayaText, level)
                    }
                }
            )

            Spacer(Modifier.height(30.dp))

            // UI based on recording state
            when (recordingState) {

                RecordingState.Idle -> IdleRecordButton(
                    onRecord = {
                        if (micPermissionState.status.isGranted) {
                            viewModel.startRecording()
                        } else micPermissionState.launchPermissionRequest()
                    }
                )

                is RecordingState.Recording -> RecordingUI(
                    volume = volumeLevel,
                    onStop = { viewModel.stopRecording() }
                )

                is RecordingState.Completed -> CompletedUI(
                    isPlaying = viewModel.isPlaying.collectAsState().value,
                    canDiagnose = remainingCount > 0,
                    onPlay = {
                        if (viewModel.isPlaying.value) viewModel.stopPlaying()
                        else viewModel.playRecording()
                    },
                    onRetry = { viewModel.reset() },
                    onDiagnose = {
                        if (remainingCount > 0) {
                            viewModel.diagnosePronunciation(content.bisayaText, level)
                        } else {
                            // If there are no remaining attempts, show a rewarded ad to recover attempts
                            onShowRewardedAd()
                        }
                    }
                )

                is RecordingState.Error -> ErrorUI(
                    message = (recordingState as RecordingState.Error).message,
                    onRetry = { viewModel.reset() }
                )
            }

            // UI based on diagnosis state (managed separately from recording)
            when (diagnosisState) {

                DiagnosisState.Idle -> {
                    // Initial state (no UI)
                }

                DiagnosisState.Loading -> LoadingUI()

                is DiagnosisState.Error -> ErrorUI(
                    message = (diagnosisState as DiagnosisState.Error).message,
                    onRetry = { viewModel.reset() }
                )

                is DiagnosisState.Success -> {
                    // Handled by outer LaunchedEffect; no UI here
                }
            }
        }
    }
}

@Composable
fun RemainingCountCard(remaining: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (remaining > 0) Color(0xFFECF4FF) else Color(0xFFFFEBEB)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = if (remaining > 0) Color(0xFF1976D2) else Color(0xFFD32F2F)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.remaining_count, remaining),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ContentCard(
    content: LearningContent,
    level: LearningLevel,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val isJapanese = LocaleUtils.isJapanese()
            Text(
                text = content.bisayaText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            val translation = if (isJapanese) {
                content.japaneseTranslation.ifBlank { content.englishTranslation }
            } else {
                content.englishTranslation.ifBlank { content.japaneseTranslation }
            }

            Text(
                text = stringResource(R.string.recording_pronunciation_format, content.pronunciation),
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = translation,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onTogglePlay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) stringResource(R.string.stop_reference) else stringResource(R.string.play_reference))
            }
        }
    }
}

@Composable
fun IdleRecordButton(onRecord: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.tap_to_record), fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        FloatingActionButton(
            onClick = onRecord,
            modifier = Modifier.size(90.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun RecordingUI(volume: Float, onStop: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "recording")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.recording), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Icon(Icons.Default.Stop, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.stop_recording), fontSize = 18.sp)
        }
    }
}

@Composable
fun CompletedUI(
    isPlaying: Boolean,
    canDiagnose: Boolean,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onDiagnose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(70.dp)
        )
        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.recording_completed), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        // Playback button
        OutlinedButton(
            onClick = onPlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isPlaying) stringResource(R.string.stop_recording) else stringResource(R.string.play_recording), fontSize = 18.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onDiagnose,
            enabled = canDiagnose,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Psychology, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (canDiagnose) stringResource(R.string.diagnose) else stringResource(R.string.recording_no_remaining),
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.record_again), fontSize = 18.sp)
        }
    }
}

@Composable
fun LoadingUI() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.analyzing), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun ErrorUI(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.error_occurred), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry), fontSize = 18.sp)
        }
    }
}
