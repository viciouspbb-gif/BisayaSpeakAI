package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.ui.viewmodel.AiTranslatorViewModel
import com.bisayaspeak.ai.ui.viewmodel.TranslatorCandidate
import com.bisayaspeak.ai.ui.viewmodel.TranslatorCandidateDisplay
import com.bisayaspeak.ai.ui.viewmodel.TranslatorEvent
import com.bisayaspeak.ai.ui.viewmodel.TranslatorExplanation
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUiState
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUsageStatus
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.util.findActivityOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTranslatorScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    isProVersion: Boolean = false,
    viewModel: AiTranslatorViewModel = viewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val usageStatus by viewModel.usageStatus.collectAsState()
    val candidates by viewModel.candidates.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    // 渡されたisProVersionを最優先で使用
    val effectiveIsPremium = isProVersion

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isDebugProBuild = BuildConfig.DEBUG && !BuildConfig.IS_LITE_BUILD
    val voiceService = remember { GeminiVoiceService(context) }

    val limitReached = !effectiveIsPremium && (usageStatus?.canUse == false)
    val limitKey = if (limitReached) {
        listOfNotNull(usageStatus?.dayKey, usageStatus?.usedCount?.toString()).joinToString(":")
    } else null
    var showTranslatorUpsell by remember { mutableStateOf(false) }

    LaunchedEffect(limitKey) {
        if (limitKey == null) {
            showTranslatorUpsell = false
        }
    }

    DisposableEffect(Unit) {
        AdManager.initialize(context.applicationContext)
        onDispose {
            voiceService.stop()
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.onInputChange(spoken)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            launchSpeechRecognizer(direction, speechLauncher, context)
        }
    }

    LaunchedEffect(isDebugProBuild) {
        if (isDebugProBuild) {
            return@LaunchedEffect
        }
        AdManager.loadInterstitial(context.applicationContext)
        viewModel.events.collect { event ->
            when (event) {
                is TranslatorEvent.RequireAd -> {
                    val activity = context.findActivityOrNull()
                    if (activity == null) {
                        viewModel.onAdResult(AdManager.InterstitialAttemptResult.NOT_READY)
                        return@collect
                    }
                    AdManager.showInterstitialWithTimeout(
                        activity = activity,
                        timeoutMs = 3_000L,
                        onAdClosed = {
                            viewModel.onAdResult(AdManager.InterstitialAttemptResult.SHOWN)
                        },
                        onAttemptResult = { result ->
                            if (result != null) {
                                viewModel.onAdResult(result)
                            }
                        }
                    )
                }
                is TranslatorEvent.ShowToast -> {
                    Toast.makeText(context, event.messageResId, Toast.LENGTH_SHORT).show()
                }
                TranslatorEvent.ShowUpsell -> {
                    showTranslatorUpsell = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF02040A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(stringResource(R.string.bisaya_translate))
                        Text(
                            text = stringResource(R.string.translator_top_subtitle),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF02040A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF02040A))
                .padding(padding)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InputCard(
                text = inputText,
                isLoading = uiState is TranslatorUiState.Loading,
                onTextChange = viewModel::onInputChange,
                onClear = viewModel::clearAll,
                onMicClick = {
                    if (hasMicPermission) {
                        launchSpeechRecognizer(direction, speechLauncher, context)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            TranslateActionButton(
                isTranslating = uiState is TranslatorUiState.Loading,
                limitReached = limitReached,
                onTranslate = { viewModel.translate(effectiveIsPremium) },
                onLimitReached = { showTranslatorUpsell = true }
            )

            val rawCandidates = if (candidates.isNotEmpty()) {
                candidates
            } else {
                listOf(
                    TranslatorCandidate(
                        bisaya = translatedText,
                        japanese = "",
                        english = "",
                        politeness = "",
                        situation = "",
                        nuance = "",
                        tip = ""
                    )
                )
            }
            val candidateDisplays = viewModel.buildCandidateDisplayList(rawCandidates)
            val primaryCandidate = candidateDisplays.firstOrNull()

            TranslatorResultsSection(
                primaryCandidate = primaryCandidate,
                explanation = explanation,
                canSpeakBisaya = direction == TranslationDirection.JA_TO_CEB,
                onCopy = { text ->
                    if (text.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(
                            context,
                            context.getString(R.string.translator_copy_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSpeak = { text ->
                    if (text.isNotBlank()) {
                        voiceService.stop()
                        voiceService.speak(text = text, cue = GeminiVoiceCue.TRANSLATOR_SWIFT)
                    }
                }
            )
        }
    }

    if (showTranslatorUpsell) {
        AlertDialog(
            onDismissRequest = { showTranslatorUpsell = false },
            title = {
                Text(
                    text = stringResource(R.string.translator_limit_upsell_title),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.translator_limit_upsell_subtitle_unlimited),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.translator_limit_upsell_subtitle_nuance),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.translator_limit_upsell_subtitle_no_ads),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTranslatorUpsell = false
                    onNavigateToUpgrade()
                }) {
                    Text(stringResource(R.string.translator_limit_upsell_cta))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTranslatorUpsell = false }) {
                    Text(stringResource(R.string.translator_limit_upsell_later))
                }
            }
        )
    }
}

@Composable
private fun InputCard(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onMicClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .widthIn(max = 720.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1828)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.translator_input_label),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.translator_auto_detect_label),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.translator_clear_desc),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onMicClick,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.translator_mic_desc),
                            tint = Color.White
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF16253C)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    BasicTextField(
                        value = text,
                        enabled = !isLoading,
                        onValueChange = onTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        modifier = Modifier.fillMaxSize()
                    ) { inner ->
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.translator_input_placeholder),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                }
            }
        }
    }
}

private fun buildUsageTips(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val normalized = raw.replace("\r", "\n")
    val candidates = normalized.split('\n', '。', '.', '！', '!', '？', '?')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return candidates.take(2)
}

private fun buildContextNotes(candidate: TranslatorCandidate?): List<String> {
    if (candidate == null) return emptyList()
    val notes = mutableListOf<String>()
    candidate.politeness.takeIf { it.isNotBlank() }?.let { notes += it }
    candidate.situation.takeIf { it.isNotBlank() }?.let { notes += it }
    candidate.nuance.takeIf { it.isNotBlank() }?.let { notes += it }
    candidate.tip.takeIf { it.isNotBlank() }?.let { notes += it }
    return notes.take(4)
}

private fun launchSpeechRecognizer(
    direction: TranslationDirection,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    context: android.content.Context
) {
    val language = when (direction) {
        TranslationDirection.JA_TO_CEB -> "ja-JP"
        TranslationDirection.CEB_TO_JA -> "ceb-PH"
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            context.getString(R.string.translator_voice_prompt)
        )
    }
    launcher.launch(intent)
}

@Composable
private fun FreeUsageCounter(usageStatus: TranslatorUsageStatus) {
    val remaining = (usageStatus.maxCount - usageStatus.usedCount).coerceAtLeast(0)
    Text(
        text = stringResource(R.string.translator_usage_remaining, remaining, usageStatus.maxCount),
        color = Color(0xFFFF8A80),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun TranslateActionButton(
    isTranslating: Boolean,
    limitReached: Boolean,
    onTranslate: () -> Unit,
    onLimitReached: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF00C896), Color(0xFF0EB5E0)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        when {
            isTranslating -> {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            }
            else -> {
                Button(
                    onClick = { 
                        if (limitReached) {
                            onLimitReached() // 4回使い切った後：ダイアログ（アップセル）を出すだけ
                        } else {
                            onTranslate()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = stringResource(R.string.translator_translate_button),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslatorResultsSection(
    primaryCandidate: TranslatorCandidateDisplay?,
    explanation: TranslatorExplanation?,
    canSpeakBisaya: Boolean,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TranslatorUnifiedResultCard(
            candidateDisplay = primaryCandidate,
            canSpeak = canSpeakBisaya,
            onCopy = onCopy,
            onSpeak = onSpeak
        )

        explanation?.let {
            TranslatorTariMemoCard(explanation = it, candidate = primaryCandidate?.candidate)
        }
    }
}

@Composable
private fun TranslatorUnifiedResultCard(
    candidateDisplay: TranslatorCandidateDisplay?,
    canSpeak: Boolean,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val bisayaText = candidateDisplay?.candidate?.bisaya?.takeIf { it.isNotBlank() }
    val japaneseText = candidateDisplay?.candidate?.japanese?.takeIf { it.isNotBlank() }
    val placeholder = stringResource(R.string.translator_result_language_placeholder)
    val bisayaDisplay = bisayaText ?: stringResource(R.string.translator_result_placeholder)
    val japaneseDisplay = japaneseText ?: placeholder
    val isCopyEnabled = bisayaText?.isNotBlank() == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10213A)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bisayaDisplay,
                    color = Color(0xFF4ADE80),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (isCopyEnabled) onCopy(bisayaDisplay) }, enabled = isCopyEnabled) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.translator_copy_desc),
                        tint = Color.White
                    )
                }
                if (canSpeak && isCopyEnabled) {
                    IconButton(onClick = { onSpeak(bisayaDisplay) }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.translator_tap_to_play_bisaya),
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            Text(
                text = japaneseDisplay,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TranslatorTariMemoCard(explanation: TranslatorExplanation, candidate: TranslatorCandidate?) {
    val summaryText = explanation.summary.takeIf { it.isNotBlank() }
    val usageTips = remember(explanation.usage) { buildUsageTips(explanation.usage) }
    val relatedList = explanation.relatedPhrases.filter { it.isNotBlank() }.take(3)
    val contextNotes = remember(candidate) { buildContextNotes(candidate) }

    if (summaryText == null && usageTips.isEmpty() && relatedList.isEmpty() && contextNotes.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1424)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.translator_tari_memo_title),
                color = Color(0xFFFB7185),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            summaryText?.let {
                Text(text = it, color = Color.White, lineHeight = 20.sp)
            }

            if (contextNotes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.translator_tari_memo_context_title),
                    color = Color(0xFFFCD34D),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    contextNotes.forEach { note ->
                        Text(text = "• $note", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    }
                }
            }

            if (usageTips.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.translator_tari_memo_usage_title),
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    usageTips.forEach { tip ->
                        Text(text = "• $tip", color = Color(0xFFD7E0F5), fontSize = 13.sp)
                    }
                }
            }

            if (relatedList.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.translator_tari_memo_related_title),
                    color = Color(0xFF22C55E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    relatedList.forEach { phrase ->
                        Text(text = "• $phrase", color = Color(0xFFA5B4FC), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
