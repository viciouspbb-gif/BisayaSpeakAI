package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.LessonResult
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.mock.MockQuizRepository
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
import com.bisayaspeak.ai.utils.MistakeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    level: LearningLevel,
    onNavigateBack: () -> Unit = {},
    onQuizStart: () -> Unit = {},
    onQuizComplete: () -> Unit = {},
    onLessonFinished: (LessonResult) -> Unit = {},
    isPremium: Boolean = false,
    navController: NavHostController,
    isReviewMode: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val usageRepository = remember { UsageRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    
    // セッション管理
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    
    val repository = remember { MockQuizRepository() }
    val isLiteBuild = BuildConfig.IS_LITE_BUILD
    val mistakeIds by MistakeManager.mistakeIds.collectAsState()
    val questions = remember(level, isLiteBuild, isReviewMode, mistakeIds) {
        if (isReviewMode) {
            repository.getAllQuestions()
                .filter { it.id in mistakeIds }
                .shuffled()
        } else {
            val source = if (isLiteBuild) {
                repository.getLiteQuizSet(totalQuestions = 10)
            } else {
                repository.getQuestionsByLevel(level)
            }
            source.shuffled().take(10)
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var hasStarted by remember { mutableStateOf(false) }
    var completionReported by remember { mutableStateOf(false) }
    var lessonReported by remember { mutableStateOf(false) }
    val total = questions.size

    val current = questions.getOrNull(currentIndex)
    val currentCorrectAnswer = current?.options?.getOrNull(current.correctIndex)
    
    fun handleLessonCompletion() {
        if (lessonReported) return
        lessonReported = true
        val baseXp = if (isReviewMode) 0 else correctCount * 10
        val bonus = if (!isReviewMode && correctCount == total && total == 10) 50 else 0
        val xp = baseXp + bonus
        val leveledUp = !isReviewMode && correctCount >= 8
        val result = LessonResult(
            correctCount = correctCount,
            totalQuestions = total,
            xpEarned = xp,
            leveledUp = leveledUp
        )
        coroutineScope.launch {
            val currentLevel = usageRepository.getCurrentLevel().first()
            var clearedLevel = currentLevel
            if (!isReviewMode) {
                usageRepository.addXP(result.xpEarned)
                if (result.leveledUp) {
                    usageRepository.incrementLevel()
                    clearedLevel += 1
                }
            }
            onLessonFinished(result)
            navController.navigate("result_screen/${result.correctCount}/${result.xpEarned}/$clearedLevel") {
                popUpTo(AppRoute.Quiz.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(total, hasStarted) {
        if (!hasStarted && total > 0) {
            sessionManager.startSession()
            onQuizStart()
            hasStarted = true
        }
    }

    // セッション完了時の広告表示（統一ルール：1セット完了 = 1回広告）
    LaunchedEffect(current) {
        if (current == null && !completionReported && hasStarted) {
            completionReported = true
            android.util.Log.d("QuizScreen", "Quiz completed, showing ad")
            sessionManager.onSessionComplete(activity) {
                onQuizComplete()
            }
        }
    }
    
    // 中断時の広告表示（統一ルール：中断 = 1回広告）
    DisposableEffect(Unit) {
        onDispose {
            if (hasStarted && current != null) {
                android.util.Log.d("QuizScreen", "Quiz interrupted, showing ad")
                sessionManager.onSessionInterrupted(activity)
            }
        }
    }
    
    // バックボタン処理
    BackHandler {
        if (hasStarted && current != null) {
            sessionManager.onSessionInterrupted(activity) {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "クイズ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        bottomBar = {
            QuizBottomBar(
                showFeedback = showFeedback,
                hasSelection = selectedIndex != null,
                isLastQuestion = currentIndex >= total - 1,
                isCorrect = showFeedback && selectedIndex == current?.correctIndex,
                correctAnswer = currentCorrectAnswer.orEmpty(),
                isPremium = isPremium,
                onAction = {
                    if (current == null) return@QuizBottomBar
                    if (!showFeedback) {
                        if (selectedIndex == null) return@QuizBottomBar
                        showFeedback = true
                        if (selectedIndex == current.correctIndex) {
                            correctCount++
                            MistakeManager.removeMistake(current.id)
                        } else {
                            MistakeManager.addMistake(current.id)
                        }
                    } else {
                        if (currentIndex < total - 1) {
                            currentIndex++
                            selectedIndex = null
                            showFeedback = false
                        } else {
                            handleLessonCompletion()
                            currentIndex = total
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isReviewMode && total == 0) {
                        "復習する問題はありません！\n通常のレッスンやクイズで間違えると、ここに溜まっていきます。"
                    } else {
                        "結果画面へ移動しています..."
                    },
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val needsScroll = maxHeight < 620.dp
                val scrollState = rememberScrollState()
                val columnModifier = if (needsScroll) {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                } else {
                    Modifier.fillMaxSize()
                }

                Column(
                    modifier = columnModifier,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuizProgressHeader(
                        currentIndex = currentIndex,
                        total = total
                    )

                    val questionContent: @Composable () -> Unit = {
                        QuestionAndMascotSection(
                            question = current.questionJa,
                            visayan = current.questionVisayan
                        )
                    }

                    val answersContent: @Composable () -> Unit = {
                        AnswerOptionsSection(
                            options = current.options,
                            selectedIndex = selectedIndex,
                            correctIndex = current.correctIndex,
                            showFeedback = showFeedback,
                            onOptionSelected = { index ->
                                if (!showFeedback) {
                                    selectedIndex = index
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (needsScroll) {
                        questionContent()
                        answersContent()
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxWidth()
                        ) {
                            questionContent()
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxWidth()
                        ) {
                            answersContent()
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionAndMascotSection(
    question: String,
    visayan: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuestionCard(
            question = question,
            visayan = visayan,
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
        )

        Image(
            painter = painterResource(id = R.drawable.char_owl),
            contentDescription = "Quiz mascot",
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .heightIn(min = 60.dp, max = 180.dp)
                .aspectRatio(0.8f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(24.dp)),
            alignment = Alignment.Center
        )
    }
}

@Composable
private fun QuizProgressHeader(
    currentIndex: Int,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "問題 ${currentIndex + 1} / $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(
            progress = (currentIndex + 1f) / total.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun QuestionCard(
    question: String,
    visayan: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            val primarySize = when {
                question.length > 160 -> 18.sp
                question.length > 120 -> 20.sp
                else -> 22.sp
            }
            val secondarySize = if (visayan.length > 100) 16.sp else 18.sp
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = primarySize,
                lineHeight = (primarySize.value + 4).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = visayan,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = secondarySize,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnswerOptionsSection(
    options: List<String>,
    selectedIndex: Int?,
    correctIndex: Int,
    showFeedback: Boolean,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val isCorrect = index == correctIndex
                val backgroundColor = when {
                    showFeedback && isCorrect -> Color(0xFFE8F5E9)
                    showFeedback && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                    isSelected -> Color(0xFFE3F2FD)
                    else -> Color(0xFFF5F6F7)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(enabled = !showFeedback) {
                            onOptionSelected(index)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun QuizBottomBar(
    showFeedback: Boolean,
    hasSelection: Boolean,
    isLastQuestion: Boolean,
    isCorrect: Boolean,
    correctAnswer: String,
    isPremium: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showFeedback) {
            ResultFeedbackPanel(
                isCorrect = isCorrect,
                correctAnswer = correctAnswer,
                isLastQuestion = isLastQuestion,
                onNext = onAction
            )
        } else {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = hasSelection,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "回答を確認する",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isPremium) {
                SmartAdBanner(isPremium = false)
            }
        }
    }
}

@Composable
private fun ResultFeedbackPanel(
    isCorrect: Boolean,
    correctAnswer: String,
    isLastQuestion: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCorrect) Color(0xFFE8F7ED) else Color(0xFFFFE8E5)
    val accentColor = if (isCorrect) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val encouragement = if (isCorrect) "正解！ナイス！" else "不正解...次はヒントを使ってみよう！"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isCorrect) {
                Image(
                    painter = painterResource(id = R.drawable.char_tarsier),
                    contentDescription = "Happy tarsier",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isCorrect) "正解！" else "不正解",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = accentColor
                )
                Text(
                    text = encouragement,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor.copy(alpha = 0.9f)
                )
                if (!isCorrect && correctAnswer.isNotBlank()) {
                    Text(
                        text = "正解: $correctAnswer",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            val label = if (isLastQuestion) "結果を見る" else "次の問題へ"
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
