package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.navigation.NavHostController
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.LessonResult
import com.bisayaspeak.ai.data.repository.UsageRepository
import com.bisayaspeak.ai.data.repository.mock.MockQuizRepository
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.util.PracticeSessionManager
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
    navController: NavHostController
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val usageRepository = remember { UsageRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    
    // セッション管理
    val sessionManager = remember { PracticeSessionManager(isPremium) }
    
    val repository = remember { MockQuizRepository() }
    val isLiteBuild = BuildConfig.IS_LITE_BUILD
    val questions = remember(level, isLiteBuild) {
        val source = if (isLiteBuild) {
            repository.getLiteQuizSet(totalQuestions = 10)
        } else {
            repository.getQuestionsByLevel(level)
        }
        source.shuffled().take(10)
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
    
    fun handleLessonCompletion() {
        if (lessonReported) return
        lessonReported = true
        val baseXp = correctCount * 10
        val bonus = if (correctCount == total && total == 10) 50 else 0
        val xp = baseXp + bonus
        val leveledUp = correctCount >= 8
        val result = LessonResult(
            correctCount = correctCount,
            totalQuestions = total,
            xpEarned = xp,
            leveledUp = leveledUp
        )
        coroutineScope.launch {
            val currentLevel = usageRepository.getCurrentLevel().first()
            usageRepository.addXP(result.xpEarned)
            var clearedLevel = currentLevel
            if (result.leveledUp) {
                usageRepository.incrementLevel()
                clearedLevel += 1
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
            // 広告バナー（スクロール対象外で画面下部に固定）
            SmartAdBanner(isPremium = isPremium)
        }
    ) { padding ->
        if (current == null) {
            // 最終問題処理後にナビゲーションで別画面へ遷移するため、ここでは簡易表示のみ
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "結果画面へ移動しています...",
                    color = Color.White
                )
            }
        } else {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "問題 ${currentIndex + 1} / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = current.questionJa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = current.questionVisayan,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                current.options.forEachIndexed { index, option ->
                    val isSelected = selectedIndex == index
                    val isCorrect = index == current.correctIndex

                    val backgroundColor = when {
                        showFeedback && isCorrect -> Color(0xFFE8F5E9)
                        showFeedback && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        isSelected -> Color(0xFFE3F2FD)
                        else -> Color(0xFFF5F6F7)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !showFeedback) {
                                selectedIndex = index
                                showFeedback = true
                                if (index == current.correctIndex) {
                                    correctCount++
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ("A"[0] + index).toString(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = option)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (showFeedback) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedIndex == current.correctIndex)
                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 正解/不正解の表示
                            Text(
                                text = if (selectedIndex == current.correctIndex) "正解！" else "不正解",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (selectedIndex == current.correctIndex) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )

                            // 不正解時は正解を明示
                            if (selectedIndex != current.correctIndex) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "正解は「${current.options[current.correctIndex]}」です",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            // 解説
                            current.explanationJa?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (currentIndex < total - 1) {
                                currentIndex++
                                selectedIndex = null
                                showFeedback = false
                            } else {
                                handleLessonCompletion()
                                currentIndex = total
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (currentIndex < total - 1) "次の問題" else "結果を見る",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
