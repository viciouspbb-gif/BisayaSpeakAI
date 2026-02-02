package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.ConversationMode
import com.bisayaspeak.ai.data.model.LearningLevel
import com.bisayaspeak.ai.data.model.RolePlayScenario
import com.bisayaspeak.ai.data.model.Speaker
import com.bisayaspeak.ai.ui.viewmodel.ConversationUiState
import com.bisayaspeak.ai.ui.viewmodel.ConversationViewModel
import kotlinx.coroutines.launch

/**
 * AI会話画面
 * 
 * 【仕様】
 * 1. 画面表示と同時にAIが自動で話し始める
 * 2. 会話一覧をLazyColumnで表示
 * 3. メッセージ入力欄（OutlinedTextField）
 * 4. 送信処理（テンプレートチェックなし）
 * 5. UI State処理（Loading, Error, Idle）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConversationScreen(
    scenario: RolePlayScenario?,
    mode: ConversationMode,
    level: LearningLevel,
    viewModel: ConversationViewModel = viewModel()
) {
    val turns by viewModel.conversationTurns.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }

    // 画面表示と同時にAIが自動で話し始める（1回だけ実行）
    LaunchedEffect(Unit) {
        viewModel.startConversation(
            scenario = scenario?.id ?: "",
            mode = mode,
            level = level.apiValue
        )
    }

    // 会話が追加されたら自動スクロール
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(turns.size - 1)
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = scenario?.titleJa ?: "AI会話")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(Color.White)
        ) {
            // 会話一覧（チャットリスト）
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(turns) { turn ->
                    if (turn.speaker == Speaker.USER) {
                        UserBubble(
                            text = turn.text,
                            translation = turn.translation
                        )
                    } else {
                        AIBubble(
                            text = turn.text,
                            translation = turn.translation
                        )
                    }
                }
            }

            // ローディング表示
            if (uiState is ConversationUiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // エラー表示
            if (uiState is ConversationUiState.Error) {
                Text(
                    text = "エラー: ${(uiState as ConversationUiState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // メッセージ入力欄
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("メッセージを入力...") },
                    singleLine = true,
                    enabled = uiState !is ConversationUiState.Loading
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 送信ボタン
                val canSend = inputText.isNotBlank() && uiState !is ConversationUiState.Loading

                FilledIconButton(
                    onClick = {
                        if (canSend) {
                            viewModel.sendUserMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF4A90E2),
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = Color.White,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "送信"
                    )
                }
            }
        }
    }
}

/**
 * ユーザーの吹き出し（右寄せ、青背景）
 */
@Composable
private fun UserBubble(
    text: String,
    translation: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (translation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * AIの吹き出し（左寄せ、グレー背景）
 */
@Composable
private fun AIBubble(
    text: String,
    translation: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (translation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
