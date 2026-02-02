package com.bisayaspeak.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.viewmodel.ListeningViewModel
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonResultScreen(
    correctCount: Int,
    totalQuestions: Int,
    clearedLevel: Int,
    leveledUp: Boolean,
    onNavigateHome: () -> Unit,
    onPracticeAgain: () -> Unit,
    viewModel: ListeningViewModel? = null
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }

    val normalizedTotal = totalQuestions.coerceAtLeast(1)
    val passed = leveledUp

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        )
    )
    val progress = (correctCount / normalizedTotal.toFloat()).coerceIn(0f, 1f)

    DisposableEffect(Unit) {
        Log.d("LessonResultScreen", "Displayed result screen")
        onDispose { voiceService.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.lesson_result_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPracticeAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = stringResource(R.string.lesson_result_repeat_button),
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(R.string.back_to_home),
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // ★共通のバナー広告部品を使用
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBanner(adUnitId = AdUnitIds.BANNER_MAIN)
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .background(gradient)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InteractiveResultText(
                                text = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_bisaya_passed
                                    } else {
                                        R.string.lesson_result_bisaya_failed
                                    }
                                ),
                                translation = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_translation_passed
                                    } else {
                                        R.string.lesson_result_translation_failed
                                    }
                                ),
                                voiceService = voiceService,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Image(
                                painter = painterResource(id = R.drawable.char_owl),
                                contentDescription = "Result mascot",
                                modifier = Modifier.size(160.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.lesson_result_correct_label), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$correctCount / $totalQuestions",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = stringResource(R.string.lesson_result_level_label, clearedLevel))
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            InteractiveResultText(
                                text = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_bisaya_level_passed
                                    } else {
                                        R.string.lesson_result_bisaya_level_failed
                                    }
                                ),
                                translation = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_level_translation_passed
                                    } else {
                                        R.string.lesson_result_level_translation_failed
                                    }
                                ),
                                voiceService = voiceService,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            InteractiveResultText(
                                text = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_bisaya_encourage_passed
                                    } else {
                                        R.string.lesson_result_bisaya_encourage_failed
                                    }
                                ),
                                translation = stringResource(
                                    if (passed) {
                                        R.string.lesson_result_encourage_translation_passed
                                    } else {
                                        R.string.lesson_result_encourage_translation_failed
                                    }
                                ),
                                voiceService = voiceService,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveResultText(
    text: String,
    translation: String,
    voiceService: GeminiVoiceService?,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    var showTranslation by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.pointerInput(text, translation) {
            detectTapGestures(
                onPress = {
                    showTranslation = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        showTranslation = false
                    }
                },
                onTap = {
                    voiceService?.stop()
                    voiceService?.speak(text)
                }
            )
        }
    ) {
        Text(
            text = if (showTranslation && translation.isNotBlank()) translation else text,
            style = style
        )
    }
}
