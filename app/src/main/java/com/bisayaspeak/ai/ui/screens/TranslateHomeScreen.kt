package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.data.repository.ConversationRepository
import kotlinx.coroutines.launch

@Composable
fun TranslateHomeScreen() {
    val repository = remember { ConversationRepository() }
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "翻訳（日本語 ↔ ビサヤ語）",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("テキストを入力") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (inputText.isNotEmpty()) {
                    isLoading = true
                    coroutineScope.launch {
                        val result = repository.translateToVisayan(inputText)
                        translatedText = result.getOrElse { "翻訳に失敗しました" }
                        isLoading = false
                    }
                }
            }
        ) {
            Text("翻訳")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            if (translatedText.isNotEmpty()) {
                Text(
                    text = "翻訳結果：",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = translatedText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
