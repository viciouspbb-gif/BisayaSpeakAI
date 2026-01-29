package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.LessonStatusManager
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds

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
    val sectionHeaders = listOf(
        1 to stringResource(R.string.level_section_1_title),
        6 to stringResource(R.string.level_section_2_title),
        11 to stringResource(R.string.level_section_3_title),
        16 to stringResource(R.string.level_section_4_title),
        21 to stringResource(R.string.level_section_5_title),
        26 to stringResource(R.string.level_section_6_title)
    ).associate { it }
    val activity = context as? Activity

    fun startLevel(level: Int) {
        if (level <= 10 || activity == null) {
            onLevelSelected(level)
        } else {
            AdManager.checkAndShowInterstitial(activity) {
                onLevelSelected(level)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.level_selection_title)) })
        },
        bottomBar = {
            Column {
                // ■ バナー広告
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner(adUnitId = AdUnitIds.BANNER_MAIN)
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
                for (level in 1..30) {
                    sectionHeaders[level]?.let { title ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    val status = LessonStatusManager.getLessonStatus(context, level, isPro)

                    item {
                        LevelButton(
                            level = level,
                            status = status,
                            onClick = {
                                when (status) {
                                    LessonStatusManager.Status.OPEN,
                                    LessonStatusManager.Status.CLEARED -> {
                                        startLevel(level)
                                    }
                                    LessonStatusManager.Status.NEED_AD -> {
                                        showAdDialogForLevel = level
                                    }
                                    LessonStatusManager.Status.LOCKED -> {
                                        android.util.Log.d("LevelSelection", "Level locked - need to clear previous level")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAdDialogForLevel != null) {
        val level = showAdDialogForLevel!!
        AlertDialog(
            onDismissRequest = { showAdDialogForLevel = null },
            title = { Text(text = stringResource(R.string.level_unlock_dialog_title, level)) },
            text = { Text(text = stringResource(R.string.level_unlock_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showAdDialogForLevel = null
                        AdManager.showRewardAd(
                            activity = context as android.app.Activity,
                            onRewardEarned = {
                                LessonStatusManager.setLessonUnlockedByAd(context, level)
                                refreshTrigger++
                                android.util.Log.d("LevelSelection", "Level unlocked by ad")
                            },
                            onAdClosed = {}
                        )
                    }
                ) {
                    Text(stringResource(R.string.watch_ad))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdDialogForLevel = null }) {
                    Text(stringResource(R.string.cancel))
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