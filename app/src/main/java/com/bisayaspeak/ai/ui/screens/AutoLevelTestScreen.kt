package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LevelTestResult(
    val level: String,
    val grammarScore: Int,
    val vocabularyScore: Int,
    val coherenceScore: Int,
    val weakPoints: List<String>,
    val advice: String,
    val recommendedMenu: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLevelTestScreen(
    onNavigateBack: () -> Unit = {}
) {
    var testState by remember { mutableStateOf<TestState>(TestState.Introduction) }
    var currentQuestion by remember { mutableStateOf(1) }
    var userAnswer by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<LevelTestResult?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("レベル自動判定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD2691E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (testState) {
                is TestState.Introduction -> {
                    IntroductionView(
                        onStart = { testState = TestState.Testing }
                    )
                }
                is TestState.Testing -> {
                    TestingView(
                        currentQuestion = currentQuestion,
                        totalQuestions = 5,
                        userAnswer = userAnswer,
                        onAnswerChange = { userAnswer = it },
                        onNext = {
                            if (currentQuestion < 5) {
                                currentQuestion++
                                userAnswer = ""
                            } else {
                                testState = TestState.Analyzing
                                // TODO: AI分析処理
                                // 仮の結果を設定
                                testResult = LevelTestResult(
                                    level = "中級",
                                    grammarScore = 75,
                                    vocabularyScore = 80,
                                    coherenceScore = 70,
                                    weakPoints = listOf("過去形の使い方", "複雑な文構造"),
                                    advice = "基本的な文法は理解していますが、過去形や複雑な文の構造に課題があります。日常会話の練習を増やすことをおすすめします。",
                                    recommendedMenu = listOf(
                                        "中級AI会話",
                                        "文法強化クイズ",
                                        "リスニング練習（中級）"
                                    )
                                )
                                testState = TestState.Result
                            }
                        }
                    )
                }
                is TestState.Analyzing -> {
                    AnalyzingView()
                }
                is TestState.Result -> {
                    testResult?.let { result ->
                        ResultView(
                            result = result,
                            onClose = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

sealed class TestState {
    object Introduction : TestState()
    object Testing : TestState()
    object Analyzing : TestState()
    object Result : TestState()
}

@Composable
fun IntroductionView(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Psychology,
            contentDescription = null,
            tint = Color(0xFFD2691E),
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AIがあなたのレベルを診断",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "3〜5問の質問に答えることで、AIがあなたの現在のビサヤ語レベルを判定します。",
            fontSize = 16.sp,
            color = Color(0xFF666666)
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F6F7), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "評価項目",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Spacer(Modifier.height(8.dp))
            Text("• 文法の正確さ", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 語彙の豊富さ", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 会話の一貫性", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 返答の適切さ", fontSize = 14.sp, color = Color(0xFF666666))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD2691E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "テストを開始",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun TestingView(
    currentQuestion: Int,
    totalQuestions: Int,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "質問 $currentQuestion / $totalQuestions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Text(
                text = "${(currentQuestion.toFloat() / totalQuestions * 100).toInt()}%",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = currentQuestion.toFloat() / totalQuestions,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFD2691E)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "AIからの質問:",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "あなたの趣味について教えてください。（ビサヤ語で回答）",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF222222)
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = userAnswer,
            onValueChange = onAnswerChange,
            label = { Text("あなたの回答") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = userAnswer.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD2691E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentQuestion < totalQuestions) "次へ" else "診断完了",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun AnalyzingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color(0xFFD2691E)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "AIが分析中...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "あなたの回答を詳しく分析しています",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun ResultView(
    result: LevelTestResult,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // レベル判定結果
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF55C27A),
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "あなたのレベル",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )

            Text(
                text = result.level,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD2691E)
            )
        }

        Spacer(Modifier.height(16.dp))

        // 詳細スコア
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "詳細スコア",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ScoreItem("文法", result.grammarScore)
            ScoreItem("語彙", result.vocabularyScore)
            ScoreItem("一貫性", result.coherenceScore)
        }

        Spacer(Modifier.height(16.dp))

        // 苦手ポイント
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "改善ポイント",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            result.weakPoints.forEach { point ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("• ", color = Color(0xFFD2691E))
                    Text(point, fontSize = 14.sp, color = Color(0xFF666666))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // アドバイス
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color(0xFFFFF8E1), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "学習アドバイス",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = result.advice,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // おすすめメニュー
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "おすすめ学習メニュー",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            result.recommendedMenu.forEach { menu ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .background(Color(0xFFD2691E), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(menu, fontSize = 14.sp, color = Color(0xFF444444))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD2691E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "完了",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = Color(0xFF666666))
            Text("$score / 100", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFD2691E)
        )
    }
}
