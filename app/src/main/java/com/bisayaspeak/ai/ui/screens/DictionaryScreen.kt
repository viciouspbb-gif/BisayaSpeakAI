package com.bisayaspeak.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.ui.viewmodel.AiExplanation
import com.bisayaspeak.ai.ui.viewmodel.DictionaryLanguage
import com.bisayaspeak.ai.ui.viewmodel.DictionaryMode
import com.bisayaspeak.ai.ui.viewmodel.DictionaryUiState
import com.bisayaspeak.ai.ui.viewmodel.DictionaryViewModel
import com.bisayaspeak.ai.ui.viewmodel.TalkResponse
import com.bisayaspeak.ai.ui.viewmodel.TalkStatus
import com.bisayaspeak.ai.ui.viewmodel.TranslationCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    viewModel: DictionaryViewModel = viewModel(factory = DictionaryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI翻訳機", fontWeight = FontWeight.Bold)
                        Text(
                            text = "探索モード / トークモード",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
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
                        onSubmit = viewModel::submitExploration
                    )
                }
            }

            DictionaryMode.TALK -> {
                TalkSection(
                    modifier = baseModifier,
                    state = uiState,
                    hasMicPermission = hasMicPermission,
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
                    onReplayLast = viewModel::replayLastTranslation
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
                text = "探索モード",
                selected = current == DictionaryMode.EXPLORE,
                onClick = { onModeChange(DictionaryMode.EXPLORE) }
            )
            ModeChip(
                text = "トークモード",
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
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("日本語でもビサヤ語でもOK", color = Color(0xFF7E8DA8)) },
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
            Text(if (state.isLoading) "探索中…" else "探索する", fontWeight = FontWeight.Bold)
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
                    CandidateCard(candidate)
                }
            }
        }

        state.explanation?.let { explanation ->
            ExplanationCard(explanation)
        }
    }
}

@Composable
private fun CandidateCard(candidate: TranslationCandidate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2E))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(candidate.bisaya, color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(candidate.japanese, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(candidate.english, color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            InfoPill("丁寧度 ${candidate.politeness}")
            InfoPill("シチュエーション: ${candidate.situation}")
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
            Text("Tari's memo", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
            Text(explanation.summary, color = Color.White)
            Text("使い方ヒント", color = Color(0xFF38BDF8), fontSize = 13.sp)
            Text(explanation.usage, color = Color(0xFFD7E0F5))
            if (explanation.relatedPhrases.isNotEmpty()) {
                Text("関連表現", color = Color(0xFF22C55E), fontSize = 13.sp)
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
    onModeToggle: (DictionaryMode) -> Unit,
    onMicTapStart: () -> Unit,
    onMicTapStop: () -> Unit,
    onRequestPermission: () -> Unit,
    onReplayLast: () -> Unit
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
            TalkStatusCard(state.talkStatus, state.isManualRecording)

            state.talkResponse?.let { response ->
                TalkResultCard(response, onReplayLast)
            }

            state.errorMessage?.let { ErrorCard(it) }

            if (state.talkHistory.isNotEmpty()) {
                Text("最近のトーク", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.talkHistory.forEach { history ->
                        TalkHistoryRow(history)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isBusy = state.talkStatus is TalkStatus.Processing || state.talkStatus is TalkStatus.Speaking
        val isMicEnabled = hasMicPermission && !isBusy
        ManualMicButton(
            isRecording = state.isManualRecording,
            enabled = isMicEnabled || state.isManualRecording,
            isBusy = isBusy,
            onClick = {
                when {
                    !hasMicPermission -> onRequestPermission()
                    state.isManualRecording -> onMicTapStop()
                    isBusy -> Unit
                    else -> onMicTapStart()
                }
            }
        )

        if (!hasMicPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            PermissionHint(onRequestPermission)
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
            label = if (isManualRecording) "録音待機" else "待機中 (タップで録音)"
            statusColor = Color(0xFF94A3B8)
        }
        TalkStatus.Listening -> {
            label = if (isManualRecording) "録音中 (タップで翻訳)" else "リスニング中"
            statusColor = if (isManualRecording) Color(0xFFFB923C) else Color(0xFF34D399)
        }
        TalkStatus.Processing -> {
            label = "翻訳中"
            statusColor = Color(0xFFFCD34D)
        }
        TalkStatus.Speaking -> {
            label = "タリが話しています"
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
                text = "検知言語: ${languageLabel(response.detectedLanguage)}",
                color = Color(0xFF60A5FA),
                fontSize = 12.sp
            )
            Text("入力", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(response.sourceText, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("翻訳", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(response.translatedText, color = Color(0xFFBAE6FD), fontWeight = FontWeight.Bold)
            if (response.romanized.isNotBlank()) {
                Text(response.romanized, color = Color(0xFFFB923C), fontSize = 12.sp)
            }
            Text("解説", color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(response.explanation, color = Color(0xFFFDE68A))
            Button(
                onClick = onReplay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("この発話で再翻訳", fontWeight = FontWeight.Bold)
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

private fun languageLabel(language: DictionaryLanguage): String = when (language) {
    DictionaryLanguage.JAPANESE -> "日本語"
    DictionaryLanguage.BISAYA -> "ビサヤ語"
    DictionaryLanguage.ENGLISH -> "英語"
    DictionaryLanguage.UNKNOWN -> "未判定"
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
        isRecording -> "録音中…タップで翻訳する"
        isBusy -> "翻訳処理中"
        else -> "タップで録音開始"
    }
    val subLabel = when {
        isRecording -> "話し終えたらもう一度タップ"
        isBusy -> "タリが処理しています"
        else -> "タップ → 録音 / もう一度タップ → 訳＆読み上げ"
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
            text = "マイク権限が必要です",
            color = Color(0xFFF87171),
            fontSize = 13.sp
        )
        OutlinedButton(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("マイク権限を許可する", fontWeight = FontWeight.Bold)
        }
    }
}

