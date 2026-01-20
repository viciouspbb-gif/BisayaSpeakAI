package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.ui.viewmodel.DojoListeningViewModel
import com.bisayaspeak.ai.voice.EnvironmentVolumePreset
import com.bisayaspeak.ai.voice.Soundscape
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun TariDojoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DojoListeningViewModel = viewModel(factory = DojoListeningViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        delay(200)
        viewModel.startTraining()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF111827),
                        Color(0xFF0B1120)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            DojoHeader(onNavigateBack)
            Spacer(modifier = Modifier.height(16.dp))
            ProgressSection(state.currentQuestionIndex, state.totalQuestions, state.correctCount)
            Spacer(modifier = Modifier.height(18.dp))
            ReactionPanel(state.feedbackMessage, state.expression, state.isTtsPlaying)
            Spacer(modifier = Modifier.height(18.dp))
            QuestionDeckSection(state.currentQuestionIndex + 1, state.currentQuestion?.meaning)
            Spacer(modifier = Modifier.height(12.dp))
            EnvironmentSelectors(
                selectedSoundscape = state.selectedSoundscape,
                onSoundscapeSelected = viewModel::selectSoundscape,
                preset = state.volumePreset,
                onPresetSelected = viewModel::setVolumePreset
            )
            Spacer(modifier = Modifier.height(20.dp))
            OptionBoard(
                options = state.currentOptions,
                selected = state.selectedAnswer,
                isEnabled = state.isAnswerEnabled,
                onPreview = viewModel::previewOption,
                onAnswer = viewModel::submitAnswer
            )
            Spacer(modifier = Modifier.height(16.dp))
            InstructionBanner(isAnswerEnabled = state.isAnswerEnabled)
            Spacer(modifier = Modifier.height(24.dp))
            if (state.finished) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::retry
                ) {
                    Text("もう一度修行する")
                }
            } else {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(state.feedbackMessage ?: "タリの指示を待とう", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PsychologyAlt,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun DojoHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "戻る", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "タリ道場",
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "修行2：空手チョップ・リスニング",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFBFDBFE))
            )
        }
    }
}

@Composable
private fun ProgressSection(currentIndex: Int, total: Int, correctCount: Int) {
    val questionNumber = (currentIndex + 1).coerceAtLeast(1).coerceAtMost(total)
    val progress = if (total == 0) 0f else questionNumber / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "第 $questionNumber / $total 問", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "正解 $correctCount", color = Color(0xFF34D399))
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = Color(0xFFEB6F4A),
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun ReactionPanel(message: String?, expression: com.bisayaspeak.ai.ui.viewmodel.TariExpression, isSpeaking: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1F2937).copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = expression.emoji, fontSize = 32.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(expression.caption, color = Color(0xFFFEF08A), fontWeight = FontWeight.SemiBold)
                AnimatedContent(targetState = message ?: "耳を澄ませ…") { target ->
                    Text(target, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
            if (isSpeaking) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFCD34D))
            }
        }
    }
}

@Composable
private fun QuestionDeckSection(questionNumber: Int, meaning: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111827),
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF433422))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "修行メモ", color = Color(0xFFF1E3C2), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = meaning ?: "意味ヒントはタリの声を聞いてから…",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "問題 #$questionNumber", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun EnvironmentSelectors(
    selectedSoundscape: Soundscape,
    onSoundscapeSelected: (Soundscape) -> Unit,
    preset: EnvironmentVolumePreset,
    onPresetSelected: (EnvironmentVolumePreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text("環境音", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Soundscape.values().forEach { soundscape ->
                val selected = soundscape == selectedSoundscape
                AssistChip(
                    onClick = { onSoundscapeSelected(soundscape) },
                    label = { Text(soundscape.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) Color(0xFF433422) else Color.White.copy(alpha = 0.08f),
                        labelColor = if (selected) Color(0xFFF1E3C2) else Color(0xFFE5E7EB)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("雑音レベル", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnvironmentVolumePreset.values().forEach { presetOption ->
                val selected = presetOption == preset
                AssistChip(
                    onClick = { onPresetSelected(presetOption) },
                    label = { Text(presetOption.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) Color(0xFF8B5CF6).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun OptionBoard(
    options: List<String>,
    selected: String?,
    isEnabled: Boolean,
    onPreview: (String) -> Unit,
    onAnswer: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEachIndexed { index, option ->
            val isChosen = selected == option
            OptionCard(
                index = index,
                text = option,
                selected = isChosen,
                enabled = isEnabled || isChosen,
                onPreview = { onPreview(option) },
                onAnswer = { onAnswer(option) }
            )
        }
    }
}

@Composable
private fun OptionCard(
    index: Int,
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onPreview: () -> Unit,
    onAnswer: () -> Unit
) {
    val background = when {
        selected && enabled -> Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF0EA5E9)))
        selected && !enabled -> Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFF4ADE80)))
        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                detectTapGestures(
                    onTap = { if (enabled) onPreview() },
                    onDoubleTap = { if (enabled) onAnswer() }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .border(2.dp, Color.White.copy(alpha = if (selected) 0.8f else 0.2f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("選択肢 ${index + 1}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("単タップで試聴 / ダブルタップで回答", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InstructionBanner(isAnswerEnabled: Boolean) {
    val text = if (isAnswerEnabled) {
        "タリの声と環境音を真似して、正しいフレーズを聞き分けよう！"
    } else {
        "タリの号令待ち…環境音に集中して耳を澄ませるべし"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAC29A).copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isAnswerEnabled) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                contentDescription = null,
                tint = Color(0xFF8B4513)
            )
            Text(text = text, color = Color(0xFF4B2E16), fontWeight = FontWeight.Bold)
        }
    }
}
