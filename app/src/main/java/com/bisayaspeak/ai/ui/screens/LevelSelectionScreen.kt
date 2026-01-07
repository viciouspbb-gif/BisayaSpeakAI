package com.bisayaspeak.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ads.AdMobBanner // ★共通部品をインポート

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectionScreen(
    onLevelSelected: (Int) -> Unit,
    isPro: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    var showAdDialogForLevel by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("レッスン選択") })
        },
        bottomBar = {
            Column {
                // ■ バナー広告 (引数なしで呼び出す！)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner()
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { padding ->
        key(refreshTrigger) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.padding(padding)
            ) {
                items(30) { index ->
                    val level = index + 1
                    val status = LessonStatusManager.getLessonStatus(context, level, isPro)

                    LevelButton(
                        level = level,
                        status = status,
                        onClick = {
                            when (status) {
                                LessonStatusManager.Status.OPEN,
                                LessonStatusManager.Status.CLEARED -> {
                                    onLevelSelected(level)
                                }
                                LessonStatusManager.Status.NEED_AD -> {
                                    showAdDialogForLevel = level
                                }
                                LessonStatusManager.Status.LOCKED -> {
                                    Toast.makeText(context, "前のレベルをクリアしてね！", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAdDialogForLevel != null) {
        val level = showAdDialogForLevel!!
        AlertDialog(
            onDismissRequest = { showAdDialogForLevel = null },
            title = { Text(text = "LV.$level を解放") },
            text = { Text(text = "短い動画広告を見て、このレッスンを解放しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        showAdDialogForLevel = null
                        AdManager.showRewardAd(
                            activity = context as android.app.Activity,
                            onRewardEarned = {
                                LessonStatusManager.setLessonUnlockedByAd(context, level)
                                refreshTrigger++
                                Toast.makeText(context, "解放しました！", Toast.LENGTH_SHORT).show()
                            },
                            onAdClosed = {}
                        )
                    }
                ) {
                    Text("見る (無料)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdDialogForLevel = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun LevelButton(
    level: Int,
    status: LessonStatusManager.Status,
    onClick: () -> Unit
) {
    val (bgColor, icon, label) = when (status) {
        LessonStatusManager.Status.LOCKED -> Triple(Color.Gray, Icons.Default.Lock, "LOCKED")
        LessonStatusManager.Status.NEED_AD -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.Videocam, "GET")
        LessonStatusManager.Status.OPEN -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.PlayArrow, "START")
        LessonStatusManager.Status.CLEARED -> Triple(MaterialTheme.colorScheme.tertiary, Icons.Default.Star, "CLEAR")
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        modifier = Modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = true
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Lv.$level",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}