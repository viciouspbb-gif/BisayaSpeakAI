package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.FeedbackDetail
import com.bisayaspeak.ai.data.model.PronunciationData
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.viewmodel.RecordingViewModel

/**
 * 診断結果表示画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: PronunciationData,
    onBackClick: () -> Unit,
    onRetry: () -> Unit
) {
    // 発音練習の状態が他のセッションに残らないようにする
    val recordingViewModel: RecordingViewModel = viewModel()

    DisposableEffect(Unit) {
        onDispose {
            recordingViewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.try_again_button), fontSize = 18.sp)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // スコアゲージ（コンパクト版）
            item {
                MascotRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            item {
                ScoreGaugeCard(
                    score = result.score.toDouble(),
                    rating = getRating(result.score),
                    word = ""
                )
            }
            
            // 総合フィードバック
            item {
                OverallFeedbackCard(
                    overall = result.feedback,
                    rating = getRating(result.score)
                )
            }
            
            // 詳細フィードバック見出し＋各項目（個別アコーディオン）
            item {
                Text(
                    text = stringResource(R.string.detailed_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 項目の表示順を統一: ピッチ, タイミング, 音量, 全体評価, アドバイス
            val orderedAspects = listOf("Pitch", "Timing", "Volume", "Overall", "Advice")
            val sortedDetails = result.detailedFeedback.sortedWith(compareBy(
                { detail ->
                    val index = orderedAspects.indexOf(detail.aspect)
                    if (index == -1) Int.MAX_VALUE else index
                },
                { detail -> detail.aspect }
            ))

            items(sortedDetails) { detail ->
                FeedbackDetailCard(detail = detail)
            }
            
            // Tips
            item {
                TipsCard(tips = result.tips)
            }
        }
    }
}

@Composable
private fun MascotRow(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.char_owl),
            contentDescription = "Bisaya learning owl",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.char_tarsier),
            contentDescription = "Bisaya learning tarsier",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 詳細分析アコーディオン
 */
@Composable
fun DetailedFeedbackAccordion(details: List<FeedbackDetail>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.detailed_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.forEach { detail ->
                        FeedbackDetailCard(detail = detail)
                    }
                }
            }
        }
    }
}

/**
 * スコアゲージカード
 */
@Composable
fun ScoreGaugeCard(
    score: Double,
    rating: String,
    word: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // アニメーション付きスコアゲージ
            AnimatedScoreGauge(score = score)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 評価バッジ
            RatingBadge(rating = rating)
        }
    }
}

/**
 * アニメーション付きスコアゲージ
 */
@Composable
fun AnimatedScoreGauge(score: Double) {
    var animatedScore by remember { mutableStateOf(0f) }
    
    LaunchedEffect(score) {
        animate(
            initialValue = 0f,
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedScore = value
        }
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // 背景円
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            
            // 背景円
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // スコア円（グラデーション色）
            val sweepAngle = (animatedScore / 100f) * 270f
            val color = when {
                animatedScore >= 90 -> Color(0xFF4CAF50)
                animatedScore >= 80 -> Color(0xFF2196F3)
                animatedScore >= 70 -> Color(0xFFFFC107)
                animatedScore >= 60 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
            
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // スコアテキスト
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    score >= 90 -> stringResource(R.string.excellent)
                    score >= 80 -> stringResource(R.string.good)
                    score >= 70 -> stringResource(R.string.not_bad)
                    score >= 60 -> stringResource(R.string.needs_improvement)
                    else -> stringResource(R.string.keep_practicing)
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.your_score),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "/ 100",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 評価バッジ
 */
@Composable
fun RatingBadge(rating: String) {
    val (backgroundColor, textColor) = when (rating) {
        "Excellent" -> Color(0xFF4CAF50) to Color.White
        "Good" -> Color(0xFF2196F3) to Color.White
        "Fair" -> Color(0xFFFFC107) to Color.Black
        else -> Color(0xFFF44336) to Color.White
    }
    
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        color = backgroundColor
    ) {
        Text(
            text = rating,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

/**
 * 総合フィードバックカード
 */
@Composable
fun OverallFeedbackCard(overall: String, rating: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = overall,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * フィードバック詳細カード
 */
@Composable
fun FeedbackDetailCard(detail: FeedbackDetail) {
    var expanded by remember { mutableStateOf(false) }
    val scoreValue: Double = (detail.score ?: 0.0).toDouble()
    val scoreLabel = when {
        scoreValue >= 70.0 -> "良好"
        scoreValue >= 50.0 -> "普通"
        else -> "要改善"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (detail.aspect) {
                        "Pitch" -> Icons.Default.MusicNote
                        "Timing" -> Icons.Default.Timer
                        "Volume" -> Icons.Default.VolumeUp
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.aspect,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    detail.score?.let { score ->
                        Text(
                            text = "${score.toInt()}点（$scoreLabel）",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                score >= 70 -> Color(0xFF4CAF50)
                                score >= 50 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail.comment,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tipsカード
 */
@Composable
fun TipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.tips),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57C00)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            fontSize = 14.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 比較詳細カード
 */
@Composable
fun ComparisonCard(
    userDuration: Double,
    refDuration: Double,
    userPitch: Double,
    refPitch: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "詳細比較",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ComparisonRow(
                label = "発話時間",
                userValue = String.format("%.2f秒", userDuration),
                refValue = String.format("%.2f秒", refDuration)
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            ComparisonRow(
                label = "ピッチ",
                userValue = String.format("%.1f Hz", userPitch),
                refValue = String.format("%.1f Hz", refPitch)
            )
        }
    }
}

@Composable
fun ComparisonRow(label: String, userValue: String, refValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "あなた: $userValue", fontSize = 14.sp)
            Text(text = "参照: $refValue", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun getRating(score: Int): String {
    return when {
        score >= 90 -> "優秀"
        score >= 70 -> "良好"
        score >= 50 -> "普通"
        else -> "要改善"
    }
}
