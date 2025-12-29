package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons

// 修正: automirrored パッケージではなく標準の filled パッケージを使用
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.ui.components.SmartAdBanner

// データクラス
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
    val inputText by viewModel.inputText.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val messages = uiState.messages

    LaunchedEffect(scenarioId) {
        viewModel.loadScenario(scenarioId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI ロールプレイ") },

                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        // 修正: Icons.AutoMirrored.Filled.ArrowBack -> Icons.Default.ArrowBack
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
        containerColor = Color(0xFF121212),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(message)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                ChatInputBar(
                    text = inputText,
                    onTextChange = viewModel::onInputTextChange,
                    onSendClick = {
                        viewModel.sendMessage(inputText)
                        focusManager.clearFocus()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SmartAdBanner(isPremium = isPremium)
            }
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

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E1E),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("メッセージを入力...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (text.isNotBlank()) Color(0xFF00C853) else Color.Gray,
                        CircleShape
                    )
            ) {
                // 修正: Icons.AutoMirrored.Filled.Send -> Icons.Default.Send
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "送信",
                    tint = Color.White
                )
            }
        }
    }
}