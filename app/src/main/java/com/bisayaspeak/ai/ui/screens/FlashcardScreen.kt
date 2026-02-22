package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.model.PracticeItem
import com.bisayaspeak.ai.data.repository.PracticeContentRepository
import com.bisayaspeak.ai.ui.viewmodel.FlashcardUiState
import com.bisayaspeak.ai.ui.viewmodel.FlashcardViewModel
import com.bisayaspeak.ai.ui.viewmodel.PracticeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    onNavigateBack: () -> Unit,
    isProVersion: Boolean = false
) {
    val isLiteBuild = BuildConfig.IS_LITE_BUILD
    val context = LocalContext.current

    if (isLiteBuild) {
        val liteViewModel: FlashcardViewModel = viewModel()
        val uiState by liteViewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            val items = try {
                PracticeContentRepository(context.applicationContext).loadPracticeItemsV1()
            } catch (_: Exception) {
                emptyList()
            }
            liteViewModel.startLiteSession(items)
        }

        FlashcardLiteScaffold(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onToggleMeaning = { liteViewModel.toggleMeaning() },
            onNext = { liteViewModel.goToNextCard() }
        )
    } else {
        val practiceViewModel: PracticeViewModel = viewModel()
        val practiceItems by practiceViewModel.practiceItems.collectAsState()

        LegacyFlashcardList(
            items = practiceItems.filterNot { it.isPremium },
            onNavigateBack = onNavigateBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyFlashcardList(
    items: List<PracticeItem>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.flashcards),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                FlashcardItem(item = item)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardLiteScaffold(
    uiState: FlashcardUiState,
    onNavigateBack: () -> Unit,
    onToggleMeaning: () -> Unit,
    onNext: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.flashcard_today_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        when {
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.flashcard_empty_message),
                        color = Color.White
                    )
                }
            }
            uiState.isComplete -> {
                FlashcardCompletion(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onNavigateBack = onNavigateBack
                )
            }
            else -> {
                FlashcardLiteContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    uiState = uiState,
                    onToggleMeaning = onToggleMeaning,
                    onNext = onNext
                )
            }
        }
    }
}

@Composable
private fun FlashcardLiteContent(
    modifier: Modifier = Modifier,
    uiState: FlashcardUiState,
    onToggleMeaning: () -> Unit,
    onNext: () -> Unit
) {
    val currentItem = uiState.items.getOrNull(uiState.currentIndex) ?: return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.flashcard_progress_format,
                    uiState.currentIndex + 1,
                    uiState.items.size
                ),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(uiState.currentIndex) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                totalDrag += dragAmount
                                if (totalDrag < -120f) {
                                    onNext()
                                    totalDrag = 0f
                                }
                                change.consume()
                            },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f }
                        )
                    }
                    .clickable { onToggleMeaning() },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF212121)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = currentItem.bisaya,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    if (uiState.showMeaning) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentItem.japanese,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC400)
                                )
                            )
                            Text(
                                text = currentItem.english,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White
                                )
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.flashcard_tap_to_reveal),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(text = stringResource(R.string.flashcard_next_button))
            }
            Text(
                text = stringResource(R.string.flashcard_swipe_hint),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun FlashcardCompletion(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.flashcard_completion_message),
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                lineHeight = 24.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.flashcard_completion_phrase),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC400)
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(text = stringResource(R.string.flashcard_completion_back_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardItem(
    item: PracticeItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7E0EC)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE7E0EC))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.bisaya,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Text(
                text = item.pronunciation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.DarkGray
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.japanese_translation),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Text(
                        text = item.japanese,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.english_translation),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Text(
                        text = item.english,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }
            }
            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
