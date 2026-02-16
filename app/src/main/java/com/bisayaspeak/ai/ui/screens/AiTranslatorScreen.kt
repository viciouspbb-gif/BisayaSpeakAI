package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.ui.viewmodel.AiTranslatorViewModel
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUiState
import com.bisayaspeak.ai.ui.viewmodel.TranslatorUsageStatus
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService
import com.bisayaspeak.ai.R

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
                direction = direction,
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

            UsageStatusCard(
                isPremiumUser = isPremiumUser,
                usageStatus = usageStatus,
                onUpgrade = onNavigateToUpgrade
            )

            ActionButtons(
                direction = direction,
                isTranslating = uiState is TranslatorUiState.Loading,
                isPremiumUser = isPremiumUser,
                canUseFreeQuota = canUseTranslate,
                onSwap = viewModel::swapDirection,
                onTranslate = { viewModel.translate(isPremiumUser) },
                onUpgrade = onNavigateToUpgrade
            )

            val canPlayBisaya = direction == TranslationDirection.JA_TO_CEB && translatedText.isNotBlank()
            ResultCard(
                text = translatedText,
                direction = direction,
                onCopy = {
                    if (translatedText.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(translatedText))
                        Toast.makeText(
                            context,
                            context.getString(R.string.translator_copy_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                isSpeakEnabled = canPlayBisaya,
                onSpeak = {
                    if (canPlayBisaya) {
                        voiceService.stop()
                        voiceService.speak(
                            text = translatedText,
                            cue = GeminiVoiceCue.TRANSLATOR_SWIFT
                        )
                    }
                }
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
private fun UsageStatusCard(
    isPremiumUser: Boolean,
    usageStatus: TranslatorUsageStatus?,
    onUpgrade: () -> Unit
) {
    if (isPremiumUser || usageStatus == null) return
    val remaining = (usageStatus.maxCount - usageStatus.usedCount).coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1828)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.translator_usage_title),
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.translator_usage_remaining,
                    remaining,
                    usageStatus.maxCount
                ),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.translator_usage_reset),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            if (!usageStatus.canUse) {
                Text(
                    text = stringResource(R.string.translator_limit_reached_label),
                    color = Color(0xFFF87171),
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text(
                        text = stringResource(R.string.translator_upgrade_cta),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    text: String,
    direction: TranslationDirection,
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
                        text = stringResource(
                            if (direction == TranslationDirection.JA_TO_CEB)
                                R.string.translator_direction_ja_ceb
                            else R.string.translator_direction_ceb_ja
                        ),
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
private fun ActionButtons(
    direction: TranslationDirection,
    isTranslating: Boolean,
    isPremiumUser: Boolean,
    canUseFreeQuota: Boolean,
    onSwap: () -> Unit,
    onTranslate: () -> Unit,
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF0D1828),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.translator_swap_label),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(
                            if (direction == TranslationDirection.JA_TO_CEB)
                                R.string.translator_direction_ja_ceb
                            else R.string.translator_direction_ceb_ja
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onSwap) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.translator_swap_desc),
                        tint = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00C896), Color(0xFF0EB5E0))
                    )
                )
                .let { base ->
                    if (isTranslating) base else base
                },
            contentAlignment = Alignment.Center
        ) {
            if (isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else if (!isPremiumUser && !canUseFreeQuota) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.translator_limit_reached_label),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onUpgrade) {
                        Text(
                            text = stringResource(R.string.translator_upgrade_cta),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                TextButton(onClick = onTranslate, enabled = !isTranslating) {
                    Text(
                        text = stringResource(R.string.translator_translate_button),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    text: String,
    direction: TranslationDirection,
    onCopy: () -> Unit,
    isSpeakEnabled: Boolean,
    onSpeak: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .background(Color(0xFF16253C), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val header = stringResource(R.string.translator_result_label)
        val placeholder = stringResource(R.string.translator_result_placeholder)

        SelectableResultText(
            label = header,
            text = text,
            placeholder = placeholder
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSpeakEnabled) {
                IconButton(onClick = onSpeak) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.translator_tap_to_play_bisaya),
                        tint = Color.White
                    )
                }
            }
            IconButton(
                onClick = onCopy,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.translator_copy_desc),
                    tint = if (text.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f)
                )
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
private fun SelectableResultText(
    label: String,
    text: String,
    placeholder: String
) {
    val textColor = Color.White
    val selectionHighlight = Color.White.copy(alpha = 0.25f)
    val content = remember(label, text, placeholder) {
        buildString {
            append("【")
            append(label)
            append("】")
            append("\n\n")
            append(if (text.isBlank()) placeholder else text)
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        factory = { context ->
            TextView(context).apply {
                setTextIsSelectable(true)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setLineSpacing(0f, 1.3f)
                setTextColor(textColor.toArgb())
                highlightColor = selectionHighlight.toArgb()
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false
                    override fun onDestroyActionMode(mode: ActionMode) {}
                }
            }
        },
        update = { view ->
            view.text = content
            view.setTextColor(textColor.toArgb())
        }
    )
}
