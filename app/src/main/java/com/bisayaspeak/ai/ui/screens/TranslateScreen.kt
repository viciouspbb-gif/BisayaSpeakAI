@file:OptIn(ExperimentalMaterial3Api::class)

package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.data.model.SourceLang
import com.bisayaspeak.ai.ui.viewmodel.TranslateUiState
import com.bisayaspeak.ai.ui.viewmodel.TranslateViewModel
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext as LocalAndroidContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import com.bisayaspeak.ai.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    isPremium: Boolean,
    onOpenPremiumInfo: () -> Unit,
    onOpenConversationMode: () -> Unit,
    viewModel: TranslateViewModel = viewModel()
) {
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val targetText by viewModel.targetText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalAndroidContext.current
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS初期化成功
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = { TranslateTopBar() },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            val needsScroll = maxHeight < 640.dp
            val scrollState = rememberScrollState()

            val questionSection: @Composable () -> Unit = {
                TranslationQuestionSection(
                    sourceLanguage = sourceLanguage,
                    sourceText = sourceText,
                    uiState = uiState,
                    onSwapLanguage = { viewModel.toggleSourceLanguage() },
                    onSourceTextChange = viewModel::onSourceTextChange,
                    onTranslate = viewModel::translate
                )
            }

            val mascotSection: @Composable () -> Unit = {
                TranslationMascotSection()
            }

            val bottomSection: @Composable () -> Unit = {
                TranslationResultSection(
                    targetText = targetText,
                    originalText = sourceText,
                    onCopy = { clipboardManager.setText(AnnotatedString(targetText)) },
                    onSpeak = {
                        tts.speak(targetText, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    isPremium = isPremium,
                    onOpenPremiumInfo = onOpenPremiumInfo,
                    onOpenConversationMode = onOpenConversationMode
                )
            }

            val columnModifier = if (needsScroll) {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            } else {
                Modifier.fillMaxSize()
            }

            Column(
                modifier = columnModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (needsScroll) {
                    Spacer(modifier = Modifier.height(8.dp))
                    questionSection()
                    mascotSection()
                    bottomSection()
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxWidth()
                    ) {
                        questionSection()
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        mascotSection()
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxWidth()
                    ) {
                        bottomSection()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TranslateBannerPlaceholder()
            }
        }
    }
}

@Composable
private fun ConversationModePremiumButton(
    isPremium: Boolean,
    onOpenPremiumInfo: () -> Unit,
    onOpenConversationMode: () -> Unit
) {
    val buttonColors = if (isPremium) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7B4DFF),
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFBDBDBD),
            contentColor = Color.White
        )
    }

    Button(
        onClick = {
            if (isPremium) {
                onOpenConversationMode()
            } else {
                onOpenPremiumInfo()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = buttonColors,
        shape = RoundedCornerShape(50.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "会話モード（Premium）",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = if (isPremium) "リアルタイム音声翻訳を開始" else "Premiumでリアルタイム翻訳を解放",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun TranslateTopBar() {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.bisaya_translate),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

@Composable
private fun LanguageSwitcherCard(
    sourceLanguage: SourceLang,
    onSwap: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LanguageColumn(
                title = stringResource(R.string.input_language),
                language = sourceLanguage.displayName,
                flag = sourceLanguage.flagEmoji
            )

            ElevatedButton(
                onClick = onSwap,
                shape = CircleShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    tint = Color.White,
                    contentDescription = stringResource(R.string.swap_languages),
                    modifier = Modifier.size(28.dp)
                )
            }

            LanguageColumn(
                title = stringResource(R.string.output_language),
                language = stringResource(R.string.bisaya),
                flag = "🇵🇭"
            )
        }
    }
}

@Composable
private fun LanguageColumn(
    title: String,
    language: String,
    flag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = flag,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = language,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TranslateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean
) {
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !isLoading,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
            ) { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enter_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = placeholderColor
                    )
                }
                innerTextField()
            }
        }
    }
}

@Composable
private fun TranslationQuestionSection(
    sourceLanguage: SourceLang,
    sourceText: String,
    uiState: TranslateUiState,
    onSwapLanguage: () -> Unit,
    onSourceTextChange: (String) -> Unit,
    onTranslate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LanguageSwitcherCard(
            sourceLanguage = sourceLanguage,
            onSwap = onSwapLanguage
        )

        TranslateInputField(
            value = sourceText,
            onValueChange = onSourceTextChange,
            isLoading = uiState is TranslateUiState.Loading
        )

        TranslateActionButton(
            enabled = sourceText.isNotBlank() && uiState !is TranslateUiState.Loading,
            onTranslate = onTranslate,
            isLoading = uiState is TranslateUiState.Loading
        )

        if (uiState is TranslateUiState.Error) {
            ErrorBanner(message = uiState.message)
        }
    }
}

@Composable
private fun TranslationMascotSection(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 200.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.char_owl),
            contentDescription = "Translate mascot",
            modifier = Modifier
                .heightIn(min = 60.dp, max = 200.dp)
                .aspectRatio(0.85f, matchHeightConstraintsFirst = true),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TranslationResultSection(
    targetText: String,
    originalText: String,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    isPremium: Boolean,
    onOpenPremiumInfo: () -> Unit,
    onOpenConversationMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(visible = targetText.isNotBlank()) {
            TranslateResultBubble(
                visayanText = targetText,
                originalText = originalText,
                onCopy = onCopy,
                onSpeak = onSpeak
            )
        }

        ConversationModePremiumButton(
            isPremium = isPremium,
            onOpenPremiumInfo = onOpenPremiumInfo,
            onOpenConversationMode = onOpenConversationMode
        )
    }
}

@Composable
private fun TranslateBannerPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "広告スペース（AdMob）",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF9CA3AF),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun TranslateActionButton(
    enabled: Boolean,
    onTranslate: () -> Unit,
    isLoading: Boolean
) {
    Button(
        onClick = onTranslate,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4A90E2),
            disabledContainerColor = Color(0xFF4A90E2).copy(alpha = 0.4f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(R.string.translate),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TranslateResultBubble(
    visayanText: String,
    originalText: String,
    onCopy: () -> Unit,
    onSpeak: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.translation_result),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "コピー"
                        )
                    }
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "読み上げ"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ビサヤ語",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = visayanText,
                        style = MaterialTheme.typography.titleLarge,
                        lineHeight = 28.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "元のテキスト",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = originalText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private val SourceLang.flagEmoji: String
    get() = when (this) {
        SourceLang.JAPANESE -> "🇯🇵"
        SourceLang.ENGLISH -> "🇺🇸"
    }
