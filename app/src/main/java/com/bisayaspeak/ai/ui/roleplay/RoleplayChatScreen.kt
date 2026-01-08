package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.ui.components.SmartAdBanner
import com.bisayaspeak.ai.ui.roleplay.RoleplayOption

// 繝・・繧ｿ繧ｯ繝ｩ繧ｹ
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayChatScreen(
    scenarioId: String,
    onBackClick: () -> Unit,
    isPremium: Boolean = false,
    viewModel: RoleplayChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(scenarioId) {
        viewModel.loadScenario(scenarioId)
    }

    var showHint by remember { mutableStateOf(false) }
    val latestAiLine: ChatMessage? = uiState.messages.lastOrNull { !it.isUser }
    val hintCandidate: RoleplayOption? = uiState.options.firstOrNull()

    val scenarioTitle = uiState.currentScenario?.title ?: "AI ロールプレイ"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scenarioTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF172554),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.char_owl),
                contentDescription = "フクロウ師匠",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = latestAiLine?.text ?: uiState.currentScenario?.initialMessage
                ?: "Maayong buntag! はじめようかの？",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            Button(
                onClick = { showHint = !showHint },
                enabled = hintCandidate != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (showHint) "ヒントを隠す" else "ヒントを見る")
            }

            if (showHint && hintCandidate != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = hintCandidate.hint ?: "ヒントはまだありません。",
                    color = Color(0xFFDBEAFE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x3322568E),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) Color(0xFF007AFF) else Color(0xFF333333)
    val textColor = Color.White

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HintPhrasePanel(
    hints: List<HintPhrase>,
    onHintSelected: (HintPhrase) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1C), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "ビサヤ語ヒント",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hints.forEach { hint ->
                HintChip(
                    hintPhrase = hint,
                    onClick = { onHintSelected(hint) }
                )
            }
        }
    }
}

@Composable
fun HintChip(
    hintPhrase: HintPhrase,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(min = 140.dp, max = 220.dp)
        ) {
            Text(
                text = hintPhrase.nativeText,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hintPhrase.translation,
                color = Color(0xFFBEBEBE),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RoleplayOptionsPanel(
    isLoading: Boolean,
    options: List<RoleplayOption>,
    revealedHintIds: Set<String>,
    onSelect: (String) -> Unit,
    onRevealHint: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        } else {
            options.forEach { option ->
                RoleplayOptionItem(
                    option = option,
                    hintRevealed = option.id in revealedHintIds,
                    onSelect = onSelect,
                    onRevealHint = onRevealHint
                )
            }
        }
    }
}

@Composable
fun RoleplayOptionItem(
    option: RoleplayOption,
    hintRevealed: Boolean,
    onSelect: (String) -> Unit,
    onRevealHint: (String) -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = { onSelect(option.id) },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF007AFF),
            contentColor = Color.White
        )
    ) {
        Text(
            text = option.text,
            fontSize = 16.sp
        )
    }

    option.hint?.let { hintText ->
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            onClick = { onRevealHint(option.id) },
            enabled = !hintRevealed,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2C),
                contentColor = Color.White,
                disabledContainerColor = Color(0x552C2C2C),
                disabledContentColor = Color(0xFFB0B0B0)
            )
        ) {
            Text(
                text = if (hintRevealed) "Hint表示済み" else "Hintを表示",
                fontSize = 15.sp
            )
        }

        if (hintRevealed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = Color(0x3329B6F6),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = hintText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
