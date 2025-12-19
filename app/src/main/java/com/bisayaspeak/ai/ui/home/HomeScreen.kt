package com.bisayaspeak.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val features = if (isLiteBuild) {
        listOf(
            FeatureItem(
                id = FeatureId.LISTENING,
                title = stringResource(R.string.listening),
                subtitle = stringResource(R.string.listening_subtitle),
                icon = Icons.Filled.Hearing,
                isLocked = false
            ),
            FeatureItem(
                id = FeatureId.QUIZ,
                title = stringResource(R.string.quiz),
                subtitle = stringResource(R.string.quiz_subtitle),
                icon = Icons.Filled.Quiz,
                isLocked = false
            ),
            FeatureItem(
                id = FeatureId.FLASHCARDS,
                title = stringResource(R.string.flashcards),
                subtitle = stringResource(R.string.flashcards_subtitle),
                icon = Icons.Filled.MenuBook,
                isLocked = false
            )
        )
        } else {
            listOf(
                FeatureItem(
                    id = FeatureId.TRANSLATE,
                    title = "Translation",
                    subtitle = "Premium限定",
                    icon = Icons.Filled.Translate,
                    isLocked = true
                ),
                FeatureItem(
                    id = FeatureId.PRONUNCIATION,
                    title = stringResource(R.string.practice),
                    subtitle = stringResource(R.string.practice_subtitle),
                    icon = Icons.Filled.Mic,
                    isLocked = false
                ),
                FeatureItem(
                    id = FeatureId.LISTENING,
                    title = stringResource(R.string.listening),
                    subtitle = stringResource(R.string.listening_subtitle),
                    icon = Icons.Filled.Hearing,
                    isLocked = false
                ),
                FeatureItem(
                    id = FeatureId.QUIZ,
                    title = stringResource(R.string.quiz),
                    subtitle = stringResource(R.string.quiz_subtitle),
                    icon = Icons.Filled.Quiz,
                    isLocked = false
                ),
                FeatureItem(
                    id = FeatureId.ROLE_PLAY,
                    title = stringResource(R.string.roleplay),
                    subtitle = stringResource(R.string.roleplay_subtitle),
                    icon = Icons.Filled.Person,
                    isLocked = false
                ),
                FeatureItem(
                    id = FeatureId.AI_CHAT,
                    title = "AI Chat",
                    subtitle = "Premium限定",
                    icon = Icons.Filled.SmartToy,
                    isLocked = true
                ),
                FeatureItem(
                    id = FeatureId.ADVANCED_ROLE_PLAY,
                    title = "Advanced RolePlay",
                    subtitle = "Premium限定",
                    icon = Icons.Filled.Psychology,
                    isLocked = true
                ),
                FeatureItem(
                    id = FeatureId.ACCOUNT,
                    title = stringResource(R.string.account),
                    subtitle = stringResource(R.string.account_subtitle),
                    icon = Icons.Filled.Person,
                    isLocked = false
                ),
                FeatureItem(
                    id = FeatureId.UPGRADE,
                    title = stringResource(R.string.upgrade_title),
                    subtitle = "Pro & Premium AI",
                    icon = Icons.Filled.Star,
                    isLocked = false
                )
            )
        }

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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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

        HomeStatusCard(homeStatus = homeStatus)

        StartLearningCard(onStartLearning = onStartLearning)

        Text(
            text = "トレーニングメニュー",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLiteBuild) 1 else 2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = if (isLiteBuild) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(features) { feature ->
                FeatureCard(
                    item = feature,
                    onClick = { onClickFeature(feature.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeStatusCard(
    homeStatus: HomeStatus
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1D4ED8),
            Color(0xFF9333EA)
        )
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

@Composable
private fun StartLearningCard(
    onStartLearning: () -> Unit
) {
    val ctaGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF34D399),
            Color(0xFF10B981)
        )
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartLearning() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(ctaGradient)
                .padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "学習を開始する",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "あなたに最適なレッスンを自動で提案します",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
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
        }
    }
}

@Composable
private fun FeatureCard(
    item: FeatureItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (item.isLocked) {
        Color(0xFF1F2937)
    } else {
        Color(0xFF111827)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = !item.isLocked) { 
                if (!item.isLocked) onClick() 
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (item.isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = if (item.isLocked) 0.6f else 0.85f)
                )
            )
        }
    }
}
