package com.bisayaspeak.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
// import androidx.compose.material.icons.filled.VolumeUp // Removed for free version
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.utils.FreeTTSService
import com.bisayaspeak.ai.utils.SoundEffectPlayer
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.model.mock.ChatMessage
import com.bisayaspeak.ai.data.model.mock.MockRolePlayScenario
import com.bisayaspeak.ai.data.model.mock.MockRolePlayChoice
import com.bisayaspeak.ai.data.model.mock.MockRolePlayStep
import com.bisayaspeak.ai.data.repository.mock.MockRolePlayRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ロールプレイメニュー画面（3列グリッド）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockRolePlayMenuScreen(
    onScenarioSelected: (MockRolePlayScenario) -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    isPremium: Boolean = false,
    repository: MockRolePlayRepository = remember { MockRolePlayRepository() }
) {
    val context = LocalContext.current
    val scenarios = remember { repository.getScenarios() }
    var showPremiumDialog by remember { mutableStateOf(false) }
    
    // 無料で使えるシナリオID（3項目）
    val freeScenarioIds = setOf(
        "airport_checkin",
        "hotel_checkin",
        "restaurant_order"
    )
    
    // Premiumアップグレードダイアログ
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text(stringResource(R.string.premium_monthly_btn)) },
            text = { Text(stringResource(R.string.locked_toast_premium)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPremiumDialog = false
                        onNavigateToUpgrade()
                    }
                ) {
                    Text(stringResource(R.string.upgrade_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ロールプレイ会話") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF5F9FF),
                Color(0xFFF9FFF5)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scenarios) { scenario ->
                    val isLocked = !freeScenarioIds.contains(scenario.id) && !isPremium
                    val cardColor = when {
                        isLocked -> Color(0xFFB0B0B0)
                        scenario.id == "airport_checkin" -> Color(0xFF64B5F6)
                        scenario.id == "hotel_checkin" -> Color(0xFF81C784)
                        scenario.id == "restaurant_order" -> Color(0xFFFFB74D)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                if (isLocked) {
                                    android.util.Log.d("MockRolePlay", "Feature locked - premium required")
                                    showPremiumDialog = true
                                } else {
                                    onScenarioSelected(scenario)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    Text(
                                        text = scenario.npcIcon,
                                        fontSize = 32.sp
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = scenario.titleJa,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = scenario.level.displayName,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ロールプレイチャット画面（完全リニューアル）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockRolePlayScreen(
    scenario: MockRolePlayScenario,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    isPremium: Boolean = false,
    onPracticePronunciation: (phraseKey: String) -> Unit = {}
) {
    val context = LocalContext.current
    // val ttsService = remember { FreeTTSService(context) } // Removed for free version
    // val soundEffectPlayer = remember { SoundEffectPlayer(context) } // Removed for free version
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // DisposableEffect removed - no audio resources to clean up

    var currentStepIndex by remember { mutableStateOf(0) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var showTranslation by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val currentStep = scenario.steps.getOrNull(currentStepIndex)

    val scenarioBaseColor = when (scenario.id) {
        "airport_checkin" -> Color(0xFF64B5F6)
        "hotel_checkin" -> Color(0xFF81C784)
        "restaurant_order" -> Color(0xFFFFB74D)
        else -> MaterialTheme.colorScheme.primary
    }
    
    // Upgradeダイアログ
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUpgradeDialog = false
                onNavigateBack()
            },
            title = { Text(stringResource(R.string.upgrade_suggestion_title)) },
            text = { Text(stringResource(R.string.upgrade_suggestion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpgradeDialog = false
                        onNavigateToUpgrade()
                    }
                ) {
                    Text(stringResource(R.string.upgrade_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUpgradeDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        )
    }
    
    // 🔥 初回再生を確実に行うLaunchedEffect
    LaunchedEffect(currentStep?.id) {
        if (currentStep == null) return@LaunchedEffect

        showTranslation = false

        // NPCメッセージ追加
        val npcMessage = ChatMessage.NPCMessage(
            text = currentStep.aiLineVisayan,
            translation = currentStep.aiLineJa,
            npcName = scenario.npcName,
            npcIcon = scenario.npcIcon
        )
        chatMessages = chatMessages + npcMessage

        // 選択肢追加（毎回ランダムな並びにする）
        chatMessages = chatMessages + ChatMessage.ChoiceDisplay(currentStep.choices.shuffled())

        // スクロールを完了
        delay(150)
        listState.animateScrollToItem(chatMessages.size - 1)

        // TTS再生削除（無料版はテキストのみ）
        // delay(900)
        // ttsService.play(currentStep.aiLineVisayan)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scenario.titleJa)
                        Text(
                            "ステップ ${currentStepIndex + 1}/${scenario.steps.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chatMessages) { msg ->
                    when (msg) {

                        is ChatMessage.NPCMessage -> {
                            NPCMessageBubble(
                                message = msg,
                                showTranslation = showTranslation,
                                onToggleTranslation = { showTranslation = !showTranslation },
                                onPlayAudio = {
                                    // Audio disabled for free version
                                },
                                baseColor = scenarioBaseColor
                            )
                        }

                        is ChatMessage.UserMessage -> {
                            UserMessageBubble(
                                message = msg,
                                onPlayAudio = {
                                    // Audio disabled for free version
                                }
                            )
                        }

                        is ChatMessage.ChoiceDisplay -> {
                            if (!isProcessing) {
                                ChoiceGridDisplay(
                                    choices = msg.choices,
                                    onChoiceSelected = { choice ->
                                        if (isProcessing) return@ChoiceGridDisplay
                                        isProcessing = true

                                        scope.launch {
                                            // タップTTS削除（無料版）
                                            // ttsService.play(choice.textVisayan)
                                            delay(500)

                                            if (choice.isCorrect) {
                                                // 正解効果音削除（無料版）
                                                // soundEffectPlayer.playCorrect()

                                                // 正解メッセージを追加
                                                val userMsg = ChatMessage.UserMessage(
                                                    text = choice.textVisayan,
                                                    translation = choice.textJa
                                                )
                                                // 選択肢を削除してユーザーメッセージを追加
                                                chatMessages = chatMessages.filterNot { it is ChatMessage.ChoiceDisplay } + userMsg

                                                delay(1800)

                                                // 次のステップへ
                                                if (currentStep?.isFinal == true) {
                                                    isProcessing = false
                                                } else {
                                                    currentStepIndex++
                                                    isProcessing = false
                                                }

                                            } else {
                                                // 不正解効果音削除（無料版）
                                                // soundEffectPlayer.playIncorrect()
                                                delay(1000)
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    baseColor = scenarioBaseColor
                                )
                            }
                        }
                    }
                }

                // 完了画面
                if (currentStep?.isFinal == true &&
                    chatMessages.none { it is ChatMessage.ChoiceDisplay }
                ) {
                    item {
                        CompletionCard(
                            scenarioBaseColor = scenarioBaseColor,
                            onRestart = {
                                // 会話終了時にインタースティシャル広告を表示し、閉じたら最初から
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    AdManager.showInterstitialWithTimeout(
                                        activity = activity,
                                        timeoutMs = 3_000L,
                                        onAdClosed = {
                                            currentStepIndex = 0
                                            chatMessages = emptyList()
                                            isProcessing = false
                                            AdManager.loadInterstitial(activity.applicationContext)
                                        }
                                    )
                                } else {
                                    currentStepIndex = 0
                                    chatMessages = emptyList()
                                    isProcessing = false
                                }
                            },
                            onNavigateBack = {
                                // 会話終了時にインタースティシャル広告を表示し、閉じたらUpgrade提案
                                val activity = context as? android.app.Activity
                                if (activity != null && !isPremium) {
                                    AdManager.showInterstitialWithTimeout(
                                        activity = activity,
                                        timeoutMs = 3_000L,
                                        onAdClosed = {
                                            showUpgradeDialog = true
                                            AdManager.loadInterstitial(activity.applicationContext)
                                        }
                                    )
                                } else {
                                    onNavigateBack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NPCMessageBubble(
    message: ChatMessage.NPCMessage,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    onPlayAudio: () -> Unit,
    baseColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // NPCアイコン
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(baseColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.npcIcon,
                fontSize = 24.sp
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // NPC名
            Text(
                text = message.npcName,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(4.dp))
            
            // メッセージバブル（タップで音声再生）
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(onClick = onPlayAudio),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // ビサヤ語テキスト
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF333333)
                    )
                    
                    // 翻訳表示（ヒントボタン押下時）
                    if (showTranslation) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = message.translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // ヒントボタン（統一スタイル）
            Button(
                onClick = onToggleTranslation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showTranslation) baseColor else Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (showTranslation) Color.White else Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (showTranslation) "訳を隠す" else "💡 ヒント",
                    fontSize = 12.sp,
                    color = if (showTranslation) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun UserMessageBubble(
    message: ChatMessage.UserMessage,
    onPlayAudio: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            // ユーザー名
            Text(
                text = "あなた",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(4.dp))
            
            // メッセージバブル（タップで音声再生）
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clickable(onClick = onPlayAudio),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 4.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // ビサヤ語テキスト
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    
                    // 日本語訳（常に表示）
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        // ユーザーアイコン
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👤",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun ChoiceGridDisplay(
    choices: List<MockRolePlayChoice>,
    onChoiceSelected: (MockRolePlayChoice) -> Unit,
    baseColor: Color
) {
    var showTranslation by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "あなたの返答を選んでください",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        choices.chunked(3).forEach { rowChoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowChoices.forEach { choice ->
                    ChoiceCard(
                        choice = choice,
                        onChoiceClick = { onChoiceSelected(choice) },
                        showTranslation = showTranslation,
                        baseColor = baseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowChoices.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        // ヒントボタン（NPCバブルと同じスタイル）
        Button(
            onClick = { showTranslation = !showTranslation },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showTranslation) baseColor else Color(0xFFE0E0E0)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (showTranslation) Color.White else Color.Gray
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (showTranslation) "訳を隠す" else "💡 ヒント",
                fontSize = 12.sp,
                color = if (showTranslation) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun ChoiceCard(
    choice: MockRolePlayChoice,
    onChoiceClick: () -> Unit,
    showTranslation: Boolean,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onChoiceClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // 選択肢テキスト
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ビサヤ語テキスト
            Text(
                text = choice.textVisayan,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF333333),
                fontWeight = FontWeight.SemiBold,
                maxLines = if (showTranslation) 2 else 3,
                textAlign = TextAlign.Center
            )
            
            // 翻訳表示（ヒントボタン押下時）
            if (showTranslation) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = choice.textJa,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompletionCard(
    scenarioBaseColor: Color,
    onRestart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 64.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "シーン完了！",
                style = MaterialTheme.typography.headlineMedium,
                color = scenarioBaseColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ロールプレイお疲れさまでした！",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scenarioBaseColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("最初から")
                }
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("メニューへ")
                }
            }
        }
    }
}
