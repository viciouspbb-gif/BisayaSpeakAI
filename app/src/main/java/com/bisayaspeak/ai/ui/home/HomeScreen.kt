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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R

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
    homeStatus: Any? = null,
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

        // 学習セクション（画像あり）
        LearningSection(onStartLearning)

        Spacer(modifier = Modifier.height(24.dp))

        // PRO機能セクション
        Text(
            text = "PRO機能",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // PRO機能リスト
        ProFeaturesSection(
            context = context,
            onClickFeature = onClickFeature,
            onShowProDialog = { showProDialog = true },
            isTariComingSoon = isTariComingSoon,
            onComingSoon = {
                Toast.makeText(context, "新機能を準備中です。もう少しお待ちください！", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 広告バナー
        if (AdsPolicy.shouldShowAds(context)) {
            AdMobBanner(adUnitId = AdUnitIds.HOME_BANNER)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showProDialog) {
        ProDialog(onDismiss = { showProDialog = false })
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
                text = "今日もビサヤ語を磨こう",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Learn Bisaya AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onClickProfile) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LearningSection(onStartLearning: () -> Unit) {
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
                        text = "称号レベル Lv 25",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ジンベエザメと泳ぐ（セブの達人！）",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "累計XP 3000",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // フクロウの画像表示 (正しいリソースID: char_owl)
                Image(
                    painter = painterResource(id = R.drawable.char_owl),
                    contentDescription = null,
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
                        text = "学習開始",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "今日のおすすめレッスンからスタートしよう",
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
                            text = "今すぐ学ぶ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // タルシエの画像表示 (正しいリソースID: char_tarsier)
                Image(
                    painter = painterResource(id = R.drawable.char_tarsier),
                    contentDescription = null,
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
fun ProFeaturesSection(
    context: android.content.Context,
    onClickFeature: (FeatureId) -> Unit,
    onShowProDialog: () -> Unit,
    isTariComingSoon: Boolean,
    onComingSoon: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI翻訳機
        ProFeatureItem(
            title = "AI 翻訳機",
            subtitle = "ネイティブ翻訳",
            icon = Icons.Default.Translate,
            color = Color(0xFFD4A017),
            onClick = { onClickFeature(FeatureId.AI_TRANSLATOR) },
            modifier = Modifier.weight(1f)
        )

        // タリと散歩道
        ProFeatureItem(
            title = "タリと散歩道",
            subtitle = "自由な会話の旅",
            icon = Icons.Default.ViewList,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onClickFeature(FeatureId.ROLE_PLAY) },
            modifier = Modifier.weight(1f)
        )

        // カミングスーン（リリース） / タリ道場（デバッグ）
        val dojoTitle = if (isTariComingSoon) "カミングスーン" else "タリ道場"
        val dojoSubtitle = if (isTariComingSoon) "特別トレーニング準備中" else "実践ボイス会話"
        val dojoBadge = if (isTariComingSoon) "修行中" else null
        val dojoIllustration = if (isTariComingSoon) R.drawable.taridoujo else null

        ProFeatureItem(
            title = dojoTitle,
            subtitle = dojoSubtitle,
            icon = Icons.Default.Psychology,
            color = Color(0xFFCD7F32),
            badgeText = dojoBadge,
            illustration = dojoIllustration,
            onClick = {
                if (isTariComingSoon) {
                    onComingSoon()
                } else {
                    onClickFeature(FeatureId.AI_CHAT)
                }
            },
            modifier = Modifier.weight(1f)
        )
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
                        contentDescription = "タリ先生",
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
fun ProDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PRO版にアップグレード") },
        text = { Text("この機能はPRO版限定です。") },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}

// ==========================================
// エラー回避用のスタブ
// ==========================================

object AdUnitIds {
    const val HOME_BANNER = "ca-app-pub-3940256099942544/6300978111"
}

object AdsPolicy {
    fun shouldShowAds(context: android.content.Context): Boolean = true
}

@Composable
fun AdMobBanner(adUnitId: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Gray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text("Ad Banner", style = MaterialTheme.typography.labelSmall)
    }
}