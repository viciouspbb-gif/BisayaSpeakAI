package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.billing.PremiumStatusProvider
import com.bisayaspeak.ai.ui.viewmodel.AiExplanation
import com.bisayaspeak.ai.ui.viewmodel.DictionaryLanguage
import com.bisayaspeak.ai.ui.viewmodel.DictionaryMode
import com.bisayaspeak.ai.ui.viewmodel.DictionaryTalkEvent
import com.bisayaspeak.ai.ui.viewmodel.DictionaryUiState
import com.bisayaspeak.ai.ui.viewmodel.DictionaryViewModel
import com.bisayaspeak.ai.ui.viewmodel.TalkResponse
import com.bisayaspeak.ai.ui.viewmodel.TalkStatus
import com.bisayaspeak.ai.ui.viewmodel.TalkUsageStatus
import com.bisayaspeak.ai.ui.viewmodel.TranslationCandidate
import com.bisayaspeak.ai.util.findActivityOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: DictionaryViewModel = viewModel(factory = DictionaryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPremiumUser by PremiumStatusProvider.isPremiumUser.collectAsState()
    val context = LocalContext.current
    val latestPremiumState = rememberUpdatedState(isPremiumUser)

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.talkEvents.collect { event ->
            when (event) {
                is DictionaryTalkEvent.RequireAd -> {
                    if (latestPremiumState.value) {
                        viewModel.onAdResult(true)
                    } else {
                        val activity = context.findActivityOrNull()
                        if (activity != null) {
                            AdManager.showInterstitialWithTimeout(activity, timeoutMs = 3_000L) {
                                AdManager.loadInterstitial(activity.applicationContext)
                                viewModel.onAdResult(true)
                            }
                        } else {
                            viewModel.onAdResult(true)
                        }
                    }
                }

                is DictionaryTalkEvent.ReachedLimit -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.dictionary_talk_limit_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.dictionary_title), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.dictionary_subtitle),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF01060F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF01060F)
    ) { padding ->
        val baseModifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF01060F), Color(0xFF081A2E))
                )
            )
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 16.dp)

        when (uiState.mode) {
            DictionaryMode.EXPLORE -> {
                Column(
                    modifier = baseModifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ModeToggle(
                        current = uiState.mode,
                        onModeChange = viewModel::setMode
                    )

                    ExploreSection(
                        state = uiState,
                        onQueryChange = viewModel::updateQuery,
                        onSubmit = viewModel::submitExploration,
                        onPlayBisaya = viewModel::speakBisaya
                    )
                }
            }

            DictionaryMode.TALK -> {
                TalkSection(
                    modifier = baseModifier,
                    state = uiState,
                    hasMicPermission = hasMicPermission,
                    isPremiumUser = isPremiumUser,
                    talkUsageStatus = uiState.talkUsageStatus,
                    onModeToggle = viewModel::setMode,
                    onMicTapStart = {
                        if (hasMicPermission) {
                            viewModel.startPushToTalk()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onMicTapStop = { viewModel.stopPushToTalk(true) },
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onReplayLast = viewModel::replayLastTranslation,
                    onNavigateToUpgrade = onNavigateToUpgrade
                )
            }
        }
    }
}

@Composable
private fun ModeToggle(current: DictionaryMode, onModeChange: (DictionaryMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x1FFFFFFF),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(
                text = stringResource(R.string.dictionary_mode_explore),
                selected = current == DictionaryMode.EXPLORE,
                onClick = { onModeChange(DictionaryMode.EXPLORE) }
            )
            ModeChip(
                text = stringResource(R.string.dictionary_mode_talk),
                selected = current == DictionaryMode.TALK,
                onClick = { onModeChange(DictionaryMode.TALK) }
            )
        }
    }
}

@Composable
private fun RowScope.ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (selected) 6.dp else 0.dp,
        color = if (selected) Color(0xFF0EA5E9) else Color.Transparent,
        border = if (selected) null else ButtonDefaults.outlinedButtonBorder
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else Color(0xFF9BB5CA),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExploreSection(
    state: DictionaryUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPlayBisaya: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.dictionary_query_placeholder), color = Color(0xFF7E8DA8)) },
            leadingIcon = {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF38BDF8))
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF1F2937)
            )
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6))
        ) {
            Text(
                text = if (state.isLoading) {
                    stringResource(R.string.dictionary_button_searching)
                } else {
                    stringResource(R.string.dictionary_button_explore)
                },
                fontWeight = FontWeight.Bold
            )
        }

        state.errorMessage?.let { message ->
            ErrorCard(message)
        }

        AnimatedVisibility(visible = state.candidates.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.candidates) { candidate ->
                    CandidateCard(candidate, onPlayBisaya)
                }
            }
        }

        state.explanation?.let { explanation ->
            ExplanationCard(explanation)
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: TranslationCandidate,
    onPlayBisaya: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2E))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    candidate.bisaya,
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onPlayBisaya(candidate.bisaya) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = stringResource(R.string.dictionary_play_bisaya_desc),
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
            Text(candidate.japanese, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(candidate.english, color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            InfoPill(stringResource(R.string.dictionary_politeness_label, candidate.politeness))
            InfoPill(stringResource(R.string.dictionary_situation_label, candidate.situation))
            Text(candidate.nuance, color = Color(0xFFD1D9E6), fontSize = 13.sp)
            Text(candidate.tip, color = Color(0xFF93E6C8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color(0x3322C55E)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color(0xFF22C55E),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ExplanationCard(explanation: AiExplanation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
private fun TalkSection(
    modifier: Modifier,
    state: DictionaryUiState,
    hasMicPermission: Boolean,
    isPremiumUser: Boolean,
    talkUsageStatus: TalkUsageStatus?,
    onModeToggle: (DictionaryMode) -> Unit,
    onMicTapStart: () -> Unit,
    onMicTapStop: () -> Unit,
    onRequestPermission: () -> Unit,
    onReplayLast: () -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxHeight()) {
        ModeToggle(current = DictionaryMode.TALK, onModeChange = onModeToggle)
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isPremiumUser && talkUsageStatus != null) {
                TalkUsageCard(status = talkUsageStatus, onUpgrade = onNavigateToUpgrade)
            }
            TalkStatusCard(state.talkStatus, state.isManualRecording)

            state.talkResponse?.let { response ->
                TalkResultCard(
                    response = response,
                    onReplay = onReplayLast
                )
            }

            state.errorMessage?.let { ErrorCard(it) }

            if (state.talkHistory.isNotEmpty()) {
                Text(stringResource(R.string.dictionary_recent_talks), color = Color(0xFF9CA3AF), fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.talkHistory.forEach { history ->
                        TalkHistoryRow(history)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isBusy = state.talkStatus is TalkStatus.Processing || state.talkStatus is TalkStatus.Speaking
        val canUseQuota = isPremiumUser || talkUsageStatus !is TalkUsageStatus.Blocked
        val isMicEnabled = hasMicPermission && !isBusy && canUseQuota
        ManualMicButton(
            isRecording = state.isManualRecording,
            enabled = isMicEnabled || state.isManualRecording,
            isBusy = isBusy,
            onClick = {
                when {
                    !hasMicPermission -> onRequestPermission()
                    state.isManualRecording -> onMicTapStop()
                    !canUseQuota -> onNavigateToUpgrade()
                    else -> onMicTapStart()
                }
            }
        )

        if (!hasMicPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            PermissionHint(onRequestPermission)
        }

        if (!isPremiumUser && talkUsageStatus is TalkUsageStatus.Blocked) {
            Spacer(modifier = Modifier.height(12.dp))
            TalkLimitCta(onUpgrade = onNavigateToUpgrade)
        }
    }
}

@Composable
private fun TalkUsageCard(status: TalkUsageStatus, onUpgrade: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152032))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.dictionary_talk_usage_title),
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.dictionary_talk_usage_remaining,
                    status.remaining,
                    status.maxCount
                ),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.dictionary_talk_usage_reset),
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp
            )
            when (status) {
                is TalkUsageStatus.NeedsAdBeforeUse -> {
                    Text(
                        text = stringResource(R.string.dictionary_talk_usage_ad_notice),
                        color = Color(0xFFFB923C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                is TalkUsageStatus.Blocked -> {
                    Text(
                        text = stringResource(R.string.dictionary_talk_limit_title),
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text(stringResource(R.string.dictionary_talk_upgrade_button), fontWeight = FontWeight.Bold)
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun TalkLimitCta(onUpgrade: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF3F1F2B)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.dictionary_talk_limit_cta),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
            ) {
                Text(stringResource(R.string.dictionary_talk_upgrade_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TalkStatusCard(status: TalkStatus, isManualRecording: Boolean) {
    val label: String
    val statusColor: Color
    when (status) {
        is TalkStatus.Error -> {
            label = status.message
            statusColor = Color(0xFFF87171)
        }
        TalkStatus.Idle -> {
            label = if (isManualRecording) {
                stringResource(R.string.dictionary_status_idle_manual)
            } else {
                stringResource(R.string.dictionary_status_idle_tap)
            }
            statusColor = Color(0xFF94A3B8)
        }
        TalkStatus.Listening -> {
            label = if (isManualRecording) {
                stringResource(R.string.dictionary_status_listening_manual)
            } else {
                stringResource(R.string.dictionary_status_listening_auto)
            }
            statusColor = if (isManualRecording) Color(0xFFFB923C) else Color(0xFF34D399)
        }
        TalkStatus.Processing -> {
            label = stringResource(R.string.dictionary_status_processing)
            statusColor = Color(0xFFFCD34D)
        }
        TalkStatus.Speaking -> {
            label = stringResource(R.string.dictionary_status_speaking)
            statusColor = Color(0xFF6366F1)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = statusColor.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TalkResultCard(
    response: TalkResponse,
    onReplay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1825))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.dictionary_detected_language, languageLabel(response.detectedLanguage)),
                color = Color(0xFF60A5FA),
                fontSize = 12.sp
            )
            Text(stringResource(R.string.dictionary_input_label), color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(response.sourceText, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.dictionary_translation_label), color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(
                response.translatedText,
                color = Color(0xFFBAE6FD),
                fontWeight = FontWeight.Bold
            )
            if (response.romanized.isNotBlank()) {
                Text(response.romanized, color = Color(0xFFFB923C), fontSize = 12.sp)
            }
            Text(stringResource(R.string.dictionary_explanation_label), color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(response.explanation, color = Color(0xFFFDE68A))
            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text(stringResource(R.string.dictionary_retranslate_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TalkHistoryRow(response: TalkResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(languageLabel(response.detectedLanguage), color = Color(0xFF60A5FA), fontSize = 11.sp)
            Text(response.sourceText, color = Color.White, fontWeight = FontWeight.Medium)
            Text(response.translatedText, color = Color(0xFFA7F3D0), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun languageLabel(language: DictionaryLanguage): String = when (language) {
    DictionaryLanguage.JAPANESE -> stringResource(R.string.japanese)
    DictionaryLanguage.BISAYA -> stringResource(R.string.bisaya)
    DictionaryLanguage.ENGLISH -> stringResource(R.string.english)
    DictionaryLanguage.UNKNOWN -> stringResource(R.string.dictionary_language_unknown)
}

@Composable
private fun ManualMicButton(
    isRecording: Boolean,
    enabled: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    val baseColor = if (isRecording) Color(0xFF7C3AED) else Color(0xFF1E293B)
    val label = when {
        isRecording -> stringResource(R.string.dictionary_mic_label_recording)
        isBusy -> stringResource(R.string.dictionary_mic_label_busy)
        else -> stringResource(R.string.dictionary_mic_label_idle)
    }
    val subLabel = when {
        isRecording -> stringResource(R.string.dictionary_mic_sub_label_recording)
        isBusy -> stringResource(R.string.dictionary_mic_sub_label_busy)
        else -> stringResource(R.string.dictionary_mic_sub_label_idle)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(baseColor.copy(alpha = if (enabled) 1f else 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subLabel,
                color = Color(0xFFCBD5F5),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun PermissionHint(onRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.dictionary_permission_title),
            color = Color(0xFFF87171),
            fontSize = 13.sp
        )
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.dictionary_permission_button), fontWeight = FontWeight.Bold)
        }
    }
}

