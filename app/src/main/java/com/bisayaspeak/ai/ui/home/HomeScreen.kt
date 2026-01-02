package com.bisayaspeak.ai.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.R

@Immutable
data class FeatureItem(
    val id: FeatureId,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isLocked: Boolean = false
)

enum class FeatureId {
    TRANSLATE,
    PRONUNCIATION,
    LISTENING,
    QUIZ,
    FLASHCARDS,
    ROLE_PLAY,
    AI_CHAT,
    AI_TRANSLATOR,
    ADVANCED_ROLE_PLAY,
    ACCOUNT,
    UPGRADE
}

@Composable
fun HomeScreen(
    homeStatus: HomeStatus,
    isLiteBuild: Boolean,
    isPremiumPlan: Boolean,
    onStartLearning: () -> Unit,
    onClickFeature: (FeatureId) -> Unit,
    onClickProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProDialog by remember { mutableStateOf(false) }
    var showOwlAdviceDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val proFeatures = listOf(
        FeatureItem(
            id = FeatureId.PRONUNCIATION,
            title = stringResource(R.string.pro_feature_speaking),
            subtitle = "",
            icon = Icons.Filled.RecordVoiceOver,
            isLocked = true
        ),
        FeatureItem(
            id = FeatureId.QUIZ,
            title = stringResource(R.string.pro_feature_quiz),
            subtitle = "",
            icon = Icons.Filled.Quiz,
            isLocked = true
        ),
        FeatureItem(
            id = FeatureId.ROLE_PLAY,
            title = stringResource(R.string.pro_feature_roleplay),
            subtitle = "",
            icon = Icons.Filled.Psychology,
            isLocked = !isPremiumPlan
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF111827),
                        Color(0xFF0B1120)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "今日もビサヤ語を磨こう",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    text = "Learn Bisaya AI",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onClickProfile() },
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val handleProFeatureClick: (FeatureId) -> Unit = { featureId ->
            if (featureId == FeatureId.ROLE_PLAY) {
                onClickFeature(featureId)
            } else {
                showProDialog = true
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HomeStatusCard(
            homeStatus = homeStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 12.dp),
            onClick = { showOwlAdviceDialog = true }
        )

        StartLearningCard(
            onStartLearning = onStartLearning,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 16.dp)
        )

        ProFeaturesSection(
            proFeatures = proFeatures,
            onFeatureClick = handleProFeatureClick,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        MissionFeatureCard(
            title = "AI Mission Talk",
            subtitle = "ボイスファーストで交渉・恋愛・ビジネスのミッションを突破しよう。",
            icon = Icons.Outlined.Bolt,
            isPremiumPlan = isPremiumPlan,
            onClick = { onClickFeature(FeatureId.AI_CHAT) },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        MissionFeatureCard(
            title = "AI Translator",
            subtitle = "ネイティブの口語感で一瞬翻訳",
            icon = Icons.Outlined.Translate,
            isPremiumPlan = isPremiumPlan,
            onClick = {
                if (isPremiumPlan) {
                    onClickFeature(FeatureId.AI_TRANSLATOR)
                } else {
                    showProDialog = true
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BannerPlaceholder()

        if (showProDialog) {
            AlertDialog(
                onDismissRequest = { showProDialog = false },
                confirmButton = {
                    TextButton(onClick = { showProDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("PRO版限定機能") },
                text = { Text(stringResource(R.string.pro_feature_dialog_message)) }
            )
        }

        if (showOwlAdviceDialog) {
            AlertDialog(
                onDismissRequest = { showOwlAdviceDialog = false },
                confirmButton = {
                    TextButton(onClick = { showOwlAdviceDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("フクロウ先生の分析") },
                text = {
                    Text(getOwlAdvice(homeStatus))
                }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MissionFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPremiumPlan: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF071C32),
            Color(0xFF0D324D),
            Color(0xFF0F3D63)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFC857)
                    )
                    Text(
                        text = "PREMIUM FEATURE",
                        color = Color(0xFFFFC857),
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )

                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isPremiumPlan) "プレミアム開放中" else "アップグレードで解放",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isPremiumPlan) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatusCard(
    homeStatus: HomeStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1D4ED8),
            Color(0xFF9333EA)
        )
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "現在のレベル",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        AutoSizeText(
                            text = "Lv ${homeStatus.currentLevel}",
                            color = Color.White,
                            maxFontSize = 32.sp,
                            minFontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        AutoSizeText(
                            text = "累計XP ${homeStatus.totalXp}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxFontSize = 14.sp,
                            minFontSize = 8.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MascotImageBox(
                        painter = painterResource(id = R.drawable.char_owl),
                        contentDescription = "Level mascot"
                    )
                }
            }
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 12.sp,
    maxLines: Int = 1,
    fontWeight: FontWeight? = null,
    softWrap: Boolean = false
) {
    var scaledTextStyle by remember {
        mutableStateOf(
            TextStyle(
                fontSize = maxFontSize,
                fontWeight = fontWeight,
                color = color
            )
        )
    }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        style = scaledTextStyle,
        maxLines = maxLines,
        softWrap = softWrap,
        onTextLayout = { textLayoutResult ->
            if ((textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) &&
                scaledTextStyle.fontSize > minFontSize
            ) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9f)
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
private fun MascotImageBox(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .requiredSize(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

private fun getOwlAdvice(homeStatus: HomeStatus): String {
    return when {
        homeStatus.currentLevel <= 1 -> "まずは「挨拶」からじゃ！毎日コツコツやれば必ず話せるようになるぞ。"
        homeStatus.currentLevel in 2..3 -> "おっ、少し慣れてきたようじゃな！次は単語量を増やしてみようか。"
        homeStatus.totalXp >= 500 -> "経験値が貯まってきたのう。今こそリスニングで耳を鍛えて会話力を一段上げるのじゃ。"
        else -> "調子はどうじゃ？焦らず楽しむことが継続の秘訣じゃぞ！"
    }
}

@Composable
private fun BannerPlaceholder(modifier: Modifier = Modifier) {
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
private fun StartLearningCard(
    onStartLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctaGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF34D399),
            Color(0xFF10B981)
        )
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onStartLearning() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(ctaGradient)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AutoSizeText(
                            text = "学習開始",
                            color = Color.White,
                            maxFontSize = 22.sp,
                            minFontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        AutoSizeText(
                            text = "今日のおすすめレッスンからスタートしよう",
                            color = Color.White.copy(alpha = 0.85f),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        AutoSizeText(
                            text = "今すぐ学ぶ",
                            color = Color.White,
                            maxFontSize = 16.sp,
                            minFontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                MascotImageBox(
                    painter = painterResource(id = R.drawable.char_tarsier),
                    contentDescription = "Start learning mascot"
                )
            }
        }
    }
}

@Composable
private fun ProFeaturesSection(
    proFeatures: List<FeatureItem>,
    onFeatureClick: (FeatureId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.pro_feature_section_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            proFeatures.forEach { feature ->
                QuickActionButton(
                    item = feature,
                    onClick = { onFeatureClick(feature.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    item: FeatureItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .alpha(if (item.isLocked) 0.65f else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )
            }

            if (item.isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                )
            }
        }
    }
}
