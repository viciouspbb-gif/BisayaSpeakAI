package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.ui.viewmodel.AiTranslatorViewModel
import com.bisayaspeak.ai.ui.viewmodel.TranslatorCandidate
import com.bisayaspeak.ai.ui.viewmodel.TranslatorExplanation
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUiState
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUsageStatus
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTranslatorScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: AiTranslatorViewModel = viewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val usageStatus by viewModel.usageStatus.collectAsState()
    val candidates by viewModel.candidates.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    val isPremiumUser by PremiumStatusProvider.isPremiumUser.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }
    val canUseTranslate = isPremiumUser || (usageStatus?.canUse ?: true)

    DisposableEffect(Unit) {
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

            if (!isPremiumUser) {
                usageStatus?.let { FreeUsageCounter(it) }
            }

            TranslateActionButton(
                isTranslating = uiState is TranslatorUiState.Loading,
                canUseFreeQuota = canUseTranslate,
                isPremiumUser = isPremiumUser,
                onTranslate = { viewModel.translate(isPremiumUser) },
                onUpgrade = onNavigateToUpgrade
            )

            TranslatorResultsSection(
                candidates = candidates,
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
                },
                fallbackText = translatedText
            )

            if (uiState is TranslatorUiState.Error) {
                ErrorBanner(
                    message = (uiState as TranslatorUiState.Error).message
                )
            }
        }
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

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = Color(0xFF421920),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFFFB4C8),
            modifier = Modifier.padding(16.dp)
        )
    }
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
    canUseFreeQuota: Boolean,
    isPremiumUser: Boolean,
    onTranslate: () -> Unit,
    onUpgrade: () -> Unit
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
            !isPremiumUser && !canUseFreeQuota -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.translator_limit_reached_label),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onUpgrade,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = stringResource(R.string.translator_upgrade_cta),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            else -> {
                Button(
                    onClick = onTranslate,
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
    candidates: List<TranslatorCandidate>,
    explanation: TranslatorExplanation?,
    canSpeakBisaya: Boolean,
    onCopy: (String) -> Unit,
    onSpeak: (String) -> Unit,
    fallbackText: String
) {
    val items = if (candidates.isNotEmpty()) candidates else listOf(
        TranslatorCandidate(
            bisaya = fallbackText,
            japanese = "",
            english = "",
            politeness = "",
            situation = "",
            nuance = "",
            tip = ""
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEachIndexed { index, candidate ->
            TranslatorCandidateCard(
                candidate = candidate,
                isPrimary = index == 0,
                canSpeak = canSpeakBisaya,
                onCopy = { onCopy(candidate.bisaya) },
                onSpeak = { onSpeak(candidate.bisaya) }
            )
        }

        explanation?.let {
            TranslatorExplanationCard(it)
        }
    }
}

@Composable
private fun TranslatorCandidateCard(
    candidate: TranslatorCandidate,
    isPrimary: Boolean,
    canSpeak: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    val cardColor = if (isPrimary) Color(0xFF10213A) else Color(0xFF0B172A)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = candidate.bisaya.ifBlank { stringResource(R.string.translator_result_placeholder) },
                    color = Color(0xFF4ADE80),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCopy, enabled = candidate.bisaya.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.translator_copy_desc),
                        tint = Color.White
                    )
                }
                if (canSpeak && candidate.bisaya.isNotBlank()) {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.translator_tap_to_play_bisaya),
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }
            if (candidate.japanese.isNotBlank()) {
                Text(candidate.japanese, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            if (candidate.english.isNotBlank()) {
                Text(candidate.english, color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (candidate.politeness.isNotBlank()) {
                    TranslatorInfoPill(stringResource(R.string.dictionary_politeness_label, candidate.politeness))
                }
                if (candidate.situation.isNotBlank()) {
                    TranslatorInfoPill(stringResource(R.string.dictionary_situation_label, candidate.situation))
                }
            }
            if (candidate.nuance.isNotBlank()) {
                Text(candidate.nuance, color = Color(0xFFD7E0F5), fontSize = 13.sp)
            }
            if (candidate.tip.isNotBlank()) {
                Text(candidate.tip, color = Color(0xFF93E6C8), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TranslatorInfoPill(text: String) {
    Surface(shape = CircleShape, color = Color(0x3322C55E)) {
        Text(
            text = text,
            color = Color(0xFF22C55E),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TranslatorExplanationCard(explanation: TranslatorExplanation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1424)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.dictionary_explanation_title), color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
            Text(explanation.summary, color = Color.White)
            Text(stringResource(R.string.dictionary_usage_title), color = Color(0xFF38BDF8), fontSize = 13.sp)
            Text(explanation.usage, color = Color(0xFFD7E0F5))
            if (explanation.relatedPhrases.isNotEmpty()) {
                Text(stringResource(R.string.dictionary_related_title), color = Color(0xFF22C55E), fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    explanation.relatedPhrases.forEach { phrase ->
                        Text("・$phrase", color = Color(0xFFA5B4FC))
                    }
                }
            }
        }
    }
}
