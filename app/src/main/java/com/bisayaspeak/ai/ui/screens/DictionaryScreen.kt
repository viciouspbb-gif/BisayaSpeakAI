package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.data.model.LearningContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onNavigateBack: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // サンプルデータ
    val allWords = remember {
        listOf(
            LearningContent(
                id = "dict1",
                bisaya = "Maayong buntag",
                japanese = "おはようございます",
                english = "Good morning",
                category = "挨拶",
                level = com.bisayaspeak.ai.data.model.LearningLevel.BEGINNER
            ),
            LearningContent(
                id = "dict2",
                bisaya = "Salamat",
                japanese = "ありがとう",
                english = "Thank you",
                category = "挨拶",
                level = com.bisayaspeak.ai.data.model.LearningLevel.BEGINNER
            ),
            LearningContent(
                id = "dict3",
                bisaya = "Palihug",
                japanese = "お願いします",
                english = "Please",
                category = "挨拶",
                level = com.bisayaspeak.ai.data.model.LearningLevel.BEGINNER
            )
        )
    }
    
    val filteredWords = remember(searchQuery, allWords) {
        if (searchQuery.isBlank()) {
            allWords
        } else {
            allWords.filter {
                it.bisayaText.contains(searchQuery, ignoreCase = true) ||
                it.japaneseTranslation.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dictionary",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
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
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search words...", color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 検索結果
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredWords) { word ->
                    DictionaryCard(
                        word = word,
                        onToggleFavorite = { onToggleFavorite(word.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DictionaryCard(
    word: LearningContent,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.bisayaText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = word.japaneseTranslation,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = word.pronunciation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { /* TTS */ }) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "音声再生",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "お気に入り",
                        tint = Color(0xFFFFC107)
                    )
                }
            }
        }
    }
}
