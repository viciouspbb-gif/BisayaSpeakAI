package com.bisayaspeak.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.BuildConfig

@Immutable
data class HomeUiState(
    val todayXp: Int = 0,
    val todayGoalXp: Int = 100
)

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
    uiState: HomeUiState,
    onClickFeature: (FeatureId) -> Unit,
    onClickProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiteBuild = BuildConfig.IS_LITE_BUILD

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
                icon = Icons.Filled.Chat,
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
            .background(Color.Black)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // タイトル（中央寄せ）
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

        // 機能カードグリッド（2列レイアウト）
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLiteBuild) 1 else 2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = if (isLiteBuild) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(features) { feature ->
                FeatureCard(
                    item = feature,
                    onClick = { onClickFeature(feature.id) }
                )
            }
        }
        
        if (!isLiteBuild) {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    item: FeatureItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = !item.isLocked) { 
                if (!item.isLocked) onClick() 
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isLocked) Color(0xFFB0B0B0) else Color(0xFFE7E0EC)
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
                    tint = Color.White,
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
                    color = if (item.isLocked) Color.White else Color.Unspecified
                )
            )
            
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (item.isLocked) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            )
        }
    }
}
