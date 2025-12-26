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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    ADVANCED_ROLE_PLAY,
    ACCOUNT,
    UPGRADE
}

@Composable
fun HomeScreen(
    homeStatus: HomeStatus,
    isLiteBuild: Boolean,
    onStartLearning: () -> Unit,
    onClickFeature: (FeatureId) -> Unit,
    onClickProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val needsScroll = configuration.screenHeightDp <= 640

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
            isLocked = false // ★テスト用：ロック解除
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

        if (needsScroll) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeStatusCard(
                    homeStatus = homeStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 140.dp)
                )

                StartLearningCard(
                    onStartLearning = onStartLearning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 140.dp)
                )

                ProFeaturesSection(
                    proFeatures = proFeatures,
                    onFeatureClick = handleProFeatureClick,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HomeStatusCard(
                        homeStatus = homeStatus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 140.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    StartLearningCard(
                        onStartLearning = onStartLearning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 140.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ProFeaturesSection(
                        proFeatures = proFeatures,
                        onFeatureClick = handleProFeatureClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
    }
}

@Composable
private fun HomeStatusCard(
    homeStatus: HomeStatus,
    modifier: Modifier = Modifier
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1D4ED8),
            Color(0xFF9333EA)
        )
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "現在のレベル",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Lv ${homeStatus.currentLevel}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "累計XP ${homeStatus.totalXp}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.char_owl),
                        contentDescription = "Level mascot",
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .heightIn(max = 120.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
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
            .clickable { onStartLearning() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(ctaGradient)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "学習開始",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.char_tarsier),
                contentDescription = "Start learning mascot",
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.9f, matchHeightConstraintsFirst = true)
                    .heightIn(max = 130.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp),
                contentScale = ContentScale.Fit
            )
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
