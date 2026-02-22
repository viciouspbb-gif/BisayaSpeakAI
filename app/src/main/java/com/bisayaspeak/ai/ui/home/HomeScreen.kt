package com.bisayaspeak.ai.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.ads.AdsPolicy

// --- 必須定義 ---
enum class FeatureId {
    AI_CHAT,
    ROLE_PLAY,
    LISTENING,
    PRONUNCIATION,
    AI_TRANSLATOR,
    TRANSLATE,
    ADVANCED_ROLE_PLAY,
    FLASHCARDS,
    ACCOUNT,
    UPGRADE
}

data class FeatureItem(
    val id: FeatureId,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isLocked: Boolean = false
)

// --- メイン画面 ---
@Composable
fun HomeScreen(
    homeStatus: HomeStatus? = null,
    isLiteBuild: Boolean = false,
    isPremiumPlan: Boolean = false,
    isProUnlocked: Boolean = false,
    onStartLearning: () -> Unit,
    onClickFeature: (FeatureId) -> Unit,
    onClickProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isTariComingSoon = !BuildConfig.DEBUG

    // デバッグビルドでは強制的にプレミアムとして扱う
    val effectivePremiumPlan = if (BuildConfig.DEBUG) true else isPremiumPlan
    val effectiveProUnlocked = if (BuildConfig.DEBUG) true else isProUnlocked

    var showProDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // ヘッダー
        HomeHeader(onClickProfile)

        Spacer(modifier = Modifier.height(16.dp))

        // 学習セクション（称号・レッスン導線）
        LearningSection(
            status = homeStatus,
            onStartLearning = onStartLearning
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PRO機能セクション
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pro_feature_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { showProDialog = true }) {
                Text(text = stringResource(R.string.home_pro_dialog_action))
            }
        }

        // PRO機能リスト
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AI翻訳機
            ProFeatureItem(
                title = stringResource(R.string.home_feature_ai_translator_title),
                subtitle = stringResource(R.string.home_feature_ai_translator_subtitle),
                icon = Icons.Default.Translate,
                color = Color(0xFFD4A017),
                onClick = { onClickFeature(FeatureId.AI_TRANSLATOR) },
                modifier = Modifier.weight(1f)
            )

            // タリと散歩道
            ProFeatureItem(
                title = stringResource(R.string.home_feature_tari_walk_title),
                subtitle = stringResource(R.string.home_feature_tari_walk_subtitle),
                icon = Icons.Default.ViewList,
                color = MaterialTheme.colorScheme.primary,
                onClick = { 
                    if (effectivePremiumPlan || effectiveProUnlocked) {
                        onClickFeature(FeatureId.ROLE_PLAY)
                    } else {
                        showProDialog = true
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // カミングスーン（リリース） / タリ道場（デバッグ）
            val dojoTitle = stringResource(R.string.home_feature_dojo_title_master)
            val dojoSubtitle = stringResource(R.string.home_feature_dojo_subtitle_master)
            val dojoIllustration = R.drawable.taridoujo

            ProFeatureItem(
                title = dojoTitle,
                subtitle = dojoSubtitle,
                icon = Icons.Default.Psychology,
                color = Color(0xFFCD7F32),
                illustration = dojoIllustration,
                onClick = { onClickFeature(FeatureId.UPGRADE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 広告バナー
        if (AdsPolicy.areAdsEnabled) {
            AdMobBanner(adUnitId = AdUnitIds.BANNER_MAIN)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showProDialog) {
        ProDialog(
            onDismiss = { showProDialog = false },
            onViewPlans = {
                showProDialog = false
                onClickFeature(FeatureId.UPGRADE)
            }
        )
    }
}

@Composable
fun HomeHeader(onClickProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_header_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700)
            )
            Text(
                text = "LearnBisaya",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }
        IconButton(onClick = onClickProfile) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.home_profile_icon_desc),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LearningSection(
    status: HomeStatus?,
    onStartLearning: () -> Unit
) {
    val level = status?.currentLevel ?: 1
    val honorTitle = status?.honorTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_honor_unlock_prompt)
    val honorNickname = status?.honorNickname?.takeIf { it.isNotBlank() }
    val progress = status?.progressToNextLevel ?: 0f
    val nextLessonMessage = status?.let {
        when {
            it.lessonsRemainingToNext <= 0 -> stringResource(R.string.home_honor_reached)
            else -> stringResource(
                R.string.home_honor_next_xp,
                it.lessonsRemainingToNext
            )
        }
    } ?: stringResource(R.string.home_honor_unlock_hint)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // 1. 青いカード（フクロウ画像）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5856D6))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_honor_level_label, level),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = honorTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    honorNickname?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        trackColor = Color.White.copy(alpha = 0.2f),
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextLessonMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // フクロウの画像表示 (正しいリソースID: char_owl)
                Image(
                    painter = painterResource(id = R.drawable.char_owl),
                    contentDescription = stringResource(R.string.home_owl_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )
            }
        }

        // 2. 緑のカード（タルシエ画像）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { onStartLearning() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2ECC71))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_start_learning_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.home_start_learning_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_start_learning_cta),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // タルシエの画像表示 (正しいリソースID: char_tarsier)
                Image(
                    painter = painterResource(id = R.drawable.char_tarsier),
                    contentDescription = stringResource(R.string.home_tarsier_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
fun ProFeatureItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    illustration: Int? = null
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 10.sp,
                    maxLines = 2
                )
                if (illustration != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(id = illustration),
                        contentDescription = stringResource(R.string.home_pro_feature_illustration_desc),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProDialog(
    onDismiss: () -> Unit,
    onViewPlans: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_pro_dialog_title)) },
        text = { Text(stringResource(R.string.home_pro_dialog_message)) },
        confirmButton = {
            Button(onClick = onViewPlans) {
                Text(stringResource(R.string.home_pro_dialog_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}