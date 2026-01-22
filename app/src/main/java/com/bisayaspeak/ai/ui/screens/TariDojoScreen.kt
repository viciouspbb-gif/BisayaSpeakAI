package com.bisayaspeak.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.viewmodel.DojoListeningViewModel
import com.bisayaspeak.ai.ui.viewmodel.DojoRoundState
import com.bisayaspeak.ai.ui.viewmodel.ListeningQuestion

@Composable
fun TariDojoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DojoListeningViewModel = viewModel(factory = DojoListeningViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler { onNavigateBack() }

    val progress = if (!state.hasStarted || state.totalQuestions == 0) {
        0f
    } else {
        val current = (state.currentQuestionIndex + 1).coerceAtLeast(1).coerceAtMost(state.totalQuestions)
        current / state.totalQuestions.toFloat()
    }

    val answers = when {
        state.currentOptions.size >= 3 -> state.currentOptions.take(3)
        else -> state.currentOptions + List(3 - state.currentOptions.size) { "" }
    }

    val overlayAlpha = remember { Animatable(0f) }
    LaunchedEffect(state.roundState) {
        overlayAlpha.snapTo(0.22f)
        overlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 180))
    }

    val currentQuestionNumber = (state.currentQuestionIndex + 1).coerceAtLeast(1)
    val kataNames = listOf("一ノ型", "二ノ型", "三ノ型", "四ノ型", "五ノ型")
    val kata = kataNames.getOrElse(currentQuestionNumber - 1) { "一ノ型" }
    val statusLabel = when (state.roundState) {
        DojoRoundState.LISTENING -> "耳を澄ませ"
        DojoRoundState.ANSWERING -> "修行中"
        DojoRoundState.ANSWERED -> "修行中"
        else -> if (state.hasStarted) "修行中" else "待機"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                trackColor = Color.Transparent,
                color = Color(0xFFF97316)
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatusHeader(statusLabel = statusLabel, kata = kata, current = currentQuestionNumber, total = state.totalQuestions, streak = state.streakCount)

            val canReplay = state.roundState != DojoRoundState.IDLE && state.currentQuestion != null
            Image(
                painter = painterResource(id = R.drawable.taridoujo),
                contentDescription = "タリ",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .clickable(
                        enabled = canReplay,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.replayCurrentQuestion() },
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            when {
                !state.hasStarted -> StartButton(label = "修行開始", onClick = viewModel::startTraining)
                state.finished -> StartButton(label = "もう一度", onClick = viewModel::retry)
            }
            Spacer(modifier = Modifier.height(24.dp))

            val memoQuestion = state.currentQuestion

            if (state.roundState == DojoRoundState.FINISHED || state.finished) {
                ResultPanel(
                    correct = state.correctCount,
                    total = state.totalQuestions,
                    weaknesses = emptyList(),
                    onRetry = viewModel::retry,
                    onNext = { /* 次の修行導線：後続実装予定 */ },
                    onBackToTop = onNavigateBack
                )
            } else {
                AnswerPad(
                    answers = answers,
                    enabled = state.isAnswerEnabled && state.currentOptions.isNotEmpty(),
                    previewing = state.previewingOption,
                    answered = state.answeredOption,
                    answeredCorrect = state.answeredCorrect,
                    onPreview = viewModel::previewOption,
                    onAnswer = viewModel::submitAnswer
                )

                if (state.roundState == DojoRoundState.ANSWERED && memoQuestion != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TrainingMemo(question = memoQuestion)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha.value))
        )
    }
}

@Composable
private fun StartButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun AnswerPad(
    answers: List<String>,
    enabled: Boolean,
    previewing: String?,
    answered: String?,
    answeredCorrect: Boolean?,
    onPreview: (String) -> Unit,
    onAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        answers.forEachIndexed { index, label ->
            val isPreviewing = previewing == label && label.isNotBlank()
            val isAnsweredCard = answered == label && label.isNotBlank()
            OptionCard(
                label = label,
                index = index,
                enabled = enabled,
                isPreviewing = isPreviewing,
                isAnsweredCard = isAnsweredCard,
                answeredCorrect = answeredCorrect,
                onPreview = { onPreview(label) },
                onAnswer = { onAnswer(label) }
            )
        }
    }
}

@Composable
private fun OptionCard(
    label: String,
    index: Int,
    enabled: Boolean,
    isPreviewing: Boolean,
    isAnsweredCard: Boolean,
    answeredCorrect: Boolean?,
    onPreview: () -> Unit,
    onAnswer: () -> Unit
) {
    val interactionEnabled = enabled && label.isNotBlank() && !isAnsweredCard
    val sink by animateDpAsState(targetValue = if (isAnsweredCard) 8.dp else 0.dp, label = "sink-$index")

    val pulseScale: Float = if (isPreviewing) {
        val transition = rememberInfiniteTransition(label = "pulse-$index")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale-$index"
        ).value
    } else {
        1f
    }

    val baseScale = if (isAnsweredCard) 0.97f else 1f
    val scale = baseScale * pulseScale

    val backgroundColor = when {
        isAnsweredCard && answeredCorrect == true -> Color(0xFF0F172A).copy(alpha = 0.85f)
        isAnsweredCard && answeredCorrect == false -> Color(0xFF1E0F10).copy(alpha = 0.9f)
        else -> Color(0xFF111827)
    }

    val borderColor = when {
        isPreviewing -> Color(0xFFF97316)
        isAnsweredCard && answeredCorrect == true -> Color(0xFF34D399)
        isAnsweredCard && answeredCorrect == false -> Color(0xFFF87171)
        else -> Color.White.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = sink)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(interactionEnabled) {
                detectTapGestures(
                    onTap = { if (interactionEnabled) onPreview() },
                    onDoubleTap = { if (interactionEnabled) onAnswer() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = Brush.linearGradient(listOf(borderColor, borderColor)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "問${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = if (label.isBlank()) "…" else label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                val statusText = when {
                    !isAnsweredCard -> null
                    answeredCorrect == true -> "見事"
                    answeredCorrect == false -> "まだ甘い"
                    else -> null
                }
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (answeredCorrect == true) Color(0xFF34D399) else Color(0xFFF97316)
                    )
                } else {
                    Text(
                        text = "単タップ：試聴 / ダブルタップ：回答",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(
    correct: Int,
    total: Int,
    weaknesses: List<ListeningQuestion>,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onBackToTop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101520)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "修行結果", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = "正解：$correct / $total", color = Color.White.copy(alpha = 0.9f))
            if (weaknesses.isNotEmpty()) {
                Text(text = "弱点", color = Color.White.copy(alpha = 0.7f))
                weaknesses.take(2).forEach { question ->
                    Text(text = "・${question.phrase} (${question.meaning})", color = Color.White.copy(alpha = 0.8f))
                }
            } else {
                Text(text = "弱点なし。次の修行へ進め。", color = Color.White.copy(alpha = 0.8f))
            }
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("再修行")
            }
            OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("次の修行")
            }
            TextButton(onClick = onBackToTop, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("道場トップへ戻る", color = Color(0xFF93C5FD))
            }
        }
    }
}

@Composable
private fun TrainingMemo(question: ListeningQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101520)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("修行メモ", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.labelLarge)
            Text(question.phrase, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("解説：${question.meaning}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusHeader(statusLabel: String, kata: String, current: Int, total: Int, streak: Int) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "修行 $kata (${current.coerceAtMost(total)}/$total)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        Text(
            text = "連続正解：$streak",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}
