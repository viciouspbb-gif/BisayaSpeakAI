package com.bisayaspeak.ai.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ads.AdMobBanner // ★共通部品をインポート

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonResultScreen(
    correctCount: Int,
    totalQuestions: Int,
    earnedXP: Int,
    clearedLevel: Int,
    leveledUp: Boolean,
    onNavigateHome: () -> Unit,
    onPracticeAgain: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val normalizedTotal = totalQuestions.coerceAtLeast(1)
    val accuracy = (correctCount / normalizedTotal.toFloat()).coerceIn(0f, 1f)
    val owlMessage = when {
        totalQuestions > 0 && correctCount == totalQuestions ->
            "見事じゃ！完璧な出来栄えじゃな！この調子で次も頼むぞ！"
        accuracy >= 0.8f ->
            "おしいのう！あと少しで全問正解じゃったのに。次は満点を目指してみるのじゃ！"
        accuracy >= 0.5f ->
            "むぅ、もう少しでレベルアップじゃ！諦めずに再挑戦するのじゃ！"
        else ->
            "まだまだ修行が必要じゃな。しっかり復習して出直してくるのじゃ！"
    }
    val passed = leveledUp
    val levelStatusTitle = if (passed) "次のレベルが解放されたぞ！" else "正解率80%以上で挑戦するのじゃ！"
    val levelStatusBody = if (passed) "この調子でさらに腕を磨くのじゃ！" else "落ち着いて復習し、も一度フクロウ先生と挑むのじゃ！"

    DisposableEffect(context) {
        var localTts: TextToSpeech? = null
        localTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                localTts?.setPitch(0.7f)
                localTts?.setSpeechRate(0.9f)
            }
        }
        tts = localTts
        onDispose {
            localTts?.stop()
            localTts?.shutdown()
            tts = null
        }
    }

    LaunchedEffect(owlMessage, tts) {
        tts?.speak(owlMessage, TextToSpeech.QUEUE_FLUSH, null, "ResultMessage")
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        )
    )
    val progress = (correctCount / normalizedTotal.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "レッスン結果", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPracticeAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "同じレッスンを再挑戦",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "ホームに戻る",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // ★共通のバナー広告部品を使用
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner()
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .background(gradient)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (passed) "Great Job!" else "Nice Try!",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Image(
                                painter = painterResource(id = R.drawable.char_owl),
                                contentDescription = "Result mascot",
                                modifier = Modifier.size(160.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "正解数", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$correctCount / $totalQuestions",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "レベル $clearedLevel")
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "獲得XP", fontWeight = FontWeight.SemiBold)
                            Text(text = "+$earnedXP XP", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE3E3E3))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = if (passed) "次のレベルが解放されました！" else "あと少しでレベルアップ！",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (passed) "引き続き学習を続けましょう。" else "もう一度挑戦してみましょう。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}