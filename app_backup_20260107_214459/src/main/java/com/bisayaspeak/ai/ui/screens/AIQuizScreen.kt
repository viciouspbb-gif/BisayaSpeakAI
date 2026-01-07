package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

data class QuizQuestion(
    val id: Int,
    val type: QuizType,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

enum class QuizType {
    LISTENING,
    GRAMMAR,
    PRONUNCIATION,
    VOCABULARY
}

data class QuizResult(
    val totalScore: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val weakPoints: List<String>,
    val aiAdvice: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIQuizScreen(
    onNavigateBack: () -> Unit = {}
) {
    var quizState by remember { mutableStateOf<QuizState>(QuizState.Introduction) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var quizResult by remember { mutableStateOf<QuizResult?>(null) }

    // サンプル問題（実際はAIが生成）
    val questions = remember {
        listOf(
            QuizQuestion(
                id = 1,
                type = QuizType.VOCABULARY,
                question = "「こんにちは」をビサヤ語で何と言いますか？",
                options = listOf("Maayong buntag", "Maayong hapon", "Kumusta", "Salamat"),
                correctAnswer = 2,
                explanation = "「Kumusta」は一般的な挨拶で、「こんにちは」や「元気ですか？」の意味です。"
            ),
            QuizQuestion(
                id = 2,
                type = QuizType.GRAMMAR,
                question = "次の文の空欄に入る適切な単語は？\n「Ako ___ estudyante.」",
                options = listOf("kay", "usa", "ang", "og"),
                correctAnswer = 1,
                explanation = "「usa」は「一つの」という意味で、「Ako usa ka estudyante」で「私は学生です」となります。"
            ),
            QuizQuestion(
                id = 3,
                type = QuizType.VOCABULARY,
                question = "「ありがとう」をビサヤ語で何と言いますか？",
                options = listOf("Kumusta", "Salamat", "Palihug", "Oo"),
                correctAnswer = 1,
                explanation = "「Salamat」は「ありがとう」を意味します。"
            )
        )
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AIクイズ") },
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
            when (quizState) {
                is QuizState.Introduction -> {
                    QuizIntroductionView(
                        onStart = { quizState = QuizState.Quiz }
                    )
                }
                is QuizState.Quiz -> {
                    if (currentQuestionIndex < questions.size) {
                        QuizQuestionView(
                            question = questions[currentQuestionIndex],
                            currentIndex = currentQuestionIndex,
                            totalQuestions = questions.size,
                            selectedAnswer = selectedAnswer,
                            showFeedback = showFeedback,
                            onAnswerSelected = { answer ->
                                selectedAnswer = answer
                                showFeedback = true
                                if (answer == questions[currentQuestionIndex].correctAnswer) {
                                    correctCount++
                                }
                            },
                            onNext = {
                                if (currentQuestionIndex < questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedAnswer = null
                                    showFeedback = false
                                } else {
                                    // クイズ終了
                                    quizResult = QuizResult(
                                        totalScore = (correctCount.toFloat() / questions.size * 100).toInt(),
                                        correctCount = correctCount,
                                        totalQuestions = questions.size,
                                        weakPoints = listOf("文法の基礎", "語彙の強化"),
                                        aiAdvice = "基本的な挨拶や単語は理解していますが、文法の理解を深めることで、より自然な会話ができるようになります。日常会話の練習を増やすことをおすすめします。"
                                    )
                                    quizState = QuizState.Result
                                }
                            }
                        )
                    }
                }
                is QuizState.Result -> {
                    quizResult?.let { result ->
                        QuizResultView(
                            result = result,
                            onClose = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

sealed class QuizState {
    object Introduction : QuizState()
    object Quiz : QuizState()
    object Result : QuizState()
}

@Composable
fun QuizIntroductionView(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Quiz,
            contentDescription = null,
            tint = Color(0xFFD2691E),
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "AIが生成するパーソナライズドクイズ",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "あなたの学習履歴をもとに、AIが最適な問題を自動生成します。",
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
                text = "出題形式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Spacer(Modifier.height(8.dp))
            Text("• 並べ替え問題（リスニング）", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 穴埋め問題（文法）", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 音声問題（発音）", fontSize = 14.sp, color = Color(0xFF666666))
            Text("• 選択式（語彙）", fontSize = 14.sp, color = Color(0xFF666666))
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
                text = "クイズを開始",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun QuizQuestionView(
    question: QuizQuestion,
    currentIndex: Int,
    totalQuestions: Int,
    selectedAnswer: Int?,
    showFeedback: Boolean,
    onAnswerSelected: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        // 進捗表示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "問題 ${currentIndex + 1} / $totalQuestions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Text(
                text = question.type.name,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / totalQuestions,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFD2691E)
        )

        Spacer(Modifier.height(24.dp))

        // 問題文
        Text(
            text = question.question,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF222222),
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(24.dp))

        // 選択肢
        question.options.forEachIndexed { index, option ->
            AnswerOption(
                text = option,
                index = index,
                isSelected = selectedAnswer == index,
                isCorrect = index == question.correctAnswer,
                showFeedback = showFeedback,
                onClick = { if (!showFeedback) onAnswerSelected(index) }
            )
            Spacer(Modifier.height(12.dp))
        }

        // フィードバック
        if (showFeedback) {
            Spacer(Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selectedAnswer == question.correctAnswer) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedAnswer == question.correctAnswer) Icons.Outlined.CheckCircle else Icons.Outlined.Close,
                        contentDescription = null,
                        tint = if (selectedAnswer == question.correctAnswer) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedAnswer == question.correctAnswer) "正解！" else "不正解",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedAnswer == question.correctAnswer) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = question.explanation,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2691E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentIndex < totalQuestions - 1) "次の問題" else "結果を見る",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showFeedback: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        showFeedback && isCorrect -> Color(0xFFE8F5E9)
        showFeedback && isSelected && !isCorrect -> Color(0xFFFFEBEE)
        isSelected -> Color(0xFFE3F2FD)
        else -> Color(0xFFF5F6F7)
    }

    val borderColor = when {
        showFeedback && isCorrect -> Color(0xFF4CAF50)
        showFeedback && isSelected && !isCorrect -> Color(0xFFF44336)
        isSelected -> Color(0xFF3C8DFF)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !showFeedback, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when {
                            showFeedback && isCorrect -> Color(0xFF4CAF50)
                            showFeedback && isSelected && !isCorrect -> Color(0xFFF44336)
                            isSelected -> Color(0xFF3C8DFF)
                            else -> Color(0xFFCCCCCC)
                        },
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ('A' + index).toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = text,
                fontSize = 16.sp,
                color = Color(0xFF222222)
            )
        }
    }
}

@Composable
fun QuizResultView(
    result: QuizResult,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // スコア表示
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
                text = "クイズ完了！",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${result.correctCount} / ${result.totalQuestions} 問正解",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${result.totalScore}点",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD2691E)
            )
        }

        Spacer(Modifier.height(16.dp))

        // 弱点分析
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "弱点分析",
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

        // AIアドバイス
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .background(Color(0xFFFFF8E1), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "AIからのアドバイス",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = result.aiAdvice,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )
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
