package com.bisayaspeak.ai.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.feature.ProFeatureGate
import com.bisayaspeak.ai.ui.ads.AdMobBanner
import com.bisayaspeak.ai.ui.ads.AdUnitIds
import com.bisayaspeak.ai.ui.ads.AdsPolicy

// --- 髯滂ｽ｢郢晢ｽｻ繝ｻ・ｰ闔・･繝ｻ・ｮ陞溘ｑ・ｽ・ｾ繝ｻ・ｩ ---
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

data class ProDialogFeature(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

// --- 驛｢譎｢・ｽ・｡驛｢・ｧ繝ｻ・､驛｢譎｢・ｽ・ｳ鬨ｾ蛹・ｽｽ・ｻ鬯ｮ・ｱ繝ｻ・｢ ---
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
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 驛｢譎・ｽｼ驥・ｨ抵ｽｹ譎｢・ｽ・ｼ驛｢譎√・郢晢ｽｻ驍ｵ・ｺ繝ｻ・ｫ髯滂ｽ｢隲帷腸・ｧ驍ｵ・ｺ雋・･繝ｻ驛｢譎｢・ｽ・ｬ驛｢譎・ｽｺ蛟･・樣Δ譎｢・｣・ｰ髯具ｽｻ繝ｻ・､髯橸ｽｳ郢晢ｽｻ
    // ProFeatureGate驍ｵ・ｺ繝ｻ・ｫ髣包ｽｳ・つ髫ｴ蟷｢・ｽ・ｬ髯具ｽｹ郢晢ｽｻ
    val effectivePremiumPlan = ProFeatureGate.isProFeatureEnabled(isPremiumPlan)
    val effectiveProUnlocked = ProFeatureGate.isProFeatureEnabled(isProUnlocked)

    var showProDialog by remember { mutableStateOf(false) }
    var proDialogDescription by remember { mutableStateOf(Int.MIN_VALUE) }
    var proDialogFeatures by remember { mutableStateOf<List<ProDialogFeature>>(emptyList()) }

    val aiTranslatorDetail = remember {
        ProDialogFeature(
            icon = Icons.Default.Translate,
            titleRes = R.string.home_feature_ai_translator_title,
            descriptionRes = R.string.home_feature_ai_translator_plan
        )
    }
    val tariWalkDetail = remember {
        ProDialogFeature(
            icon = Icons.Default.ViewList,
            titleRes = R.string.home_feature_tari_walk_title,
            descriptionRes = R.string.home_feature_tari_walk_plan
        )
    }
    val dojoDetail = remember {
        ProDialogFeature(
            icon = Icons.Default.Psychology,
            titleRes = R.string.home_feature_dojo_placeholder_title,
            descriptionRes = R.string.home_feature_dojo_plan
        )
    }
    val defaultProFeatures = remember { listOf(aiTranslatorDetail, tariWalkDetail, dojoDetail) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 驛｢譎渉・･郢晢ｽ｣驛｢謨鳴驛｢譎｢・ｽ・ｼ
            HomeHeader(onClickProfile)

            Spacer(modifier = Modifier.height(16.dp))

            // 髯昴・・ｽ・ｦ鬩怜雀繝ｻ邵ｺ譎会ｽｹ・ｧ繝ｻ・ｯ驛｢・ｧ繝ｻ・ｷ驛｢譎｢・ｽ・ｧ驛｢譎｢・ｽ・ｳ郢晢ｽｻ髢ｧ・ｲ繝ｻ・ｧ繝ｻ・ｰ髯ｷ・ｿ繝ｻ・ｷ驛｢譎｢・ｽ・ｻ驛｢譎｢・ｽ・ｬ驛｢譏ｴ繝ｻ邵ｺ蟶ｷ・ｹ譎｢・ｽ・ｳ髯昴・・ｮ闌ｨ・ｽ・ｷ陞滂ｽｲ繝ｻ・ｼ郢晢ｽｻ
            LearningSection(
                status = homeStatus,
                onStartLearning = onStartLearning
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PRO髫ｶ蛹・ｽｺ・ｯ郢晢ｽｻ驛｢・ｧ繝ｻ・ｻ驛｢・ｧ繝ｻ・ｯ驛｢・ｧ繝ｻ・ｷ驛｢譎｢・ｽ・ｧ驛｢譎｢・ｽ・ｳ
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
                TextButton(onClick = {
                    proDialogDescription = R.string.home_pro_dialog_message
                    proDialogFeatures = defaultProFeatures
                    showProDialog = true
                }) {
                    Text(text = stringResource(R.string.home_pro_dialog_action))
                }
            }

            // PRO髫ｶ蛹・ｽｺ・ｯ郢晢ｽｻ驛｢譎｢・ｽ・ｪ驛｢・ｧ繝ｻ・ｹ驛｢譏ｴ繝ｻ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI鬩怜遜・ｽ・ｻ鬮ｫ・ｪ繝ｻ・ｳ髫ｶ謔ｶ繝ｻ
                ProFeatureItem(
                    title = stringResource(R.string.home_feature_ai_translator_title),
                    subtitle = stringResource(R.string.home_feature_ai_translator_subtitle),
                    planDescription = stringResource(R.string.home_feature_ai_translator_plan),
                    icon = Icons.Default.Translate,
                    color = Color(0xFFD4A017),
                    isLocked = false,
                    onClick = { onClickFeature(FeatureId.AI_TRANSLATOR) },
                    modifier = Modifier.weight(1f)
                )

                // 驛｢・ｧ繝ｻ・ｿ驛｢譎｢・ｽ・ｪ驍ｵ・ｺ繝ｻ・ｨ髫ｰ・ｨ繝ｻ・｣髮弱・・ｽ・ｩ鬯ｩ霈斐・
                val isTariLocked = !(effectivePremiumPlan || effectiveProUnlocked)
                ProFeatureItem(
                    title = stringResource(R.string.home_feature_tari_walk_title),
                    subtitle = stringResource(R.string.home_feature_tari_walk_subtitle),
                    planDescription = stringResource(R.string.home_feature_tari_walk_plan),
                    icon = Icons.Default.ViewList,
                    color = MaterialTheme.colorScheme.primary,
                    isLocked = isTariLocked,
                    onClick = {
                        if (isTariLocked) {
                            proDialogDescription = R.string.home_feature_tari_walk_plan
                            proDialogFeatures = listOf(tariWalkDetail, aiTranslatorDetail, dojoDetail)
                            showProDialog = true
                        } else {
                            onClickFeature(FeatureId.ROLE_PLAY)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    lockedHint = stringResource(R.string.home_feature_tari_walk_locked_hint)
                )

                // 鬯ｩ諷閉ｧ繝ｻ・ｰ繝ｻ・ｴ驍ｵ・ｺ繝ｻ・ｯ髯晢ｽｶ繝ｻ・ｸ驍ｵ・ｺ繝ｻ・ｫ髮九・鞫ｩ繝ｻ蜻ｵ蜿峨・・ｭ鬮ｯ・ｦ繝ｻ・ｨ鬩穂ｼ夲ｽｽ・ｺ
                val dojoTitle = stringResource(R.string.home_feature_dojo_placeholder_title)
                val dojoSubtitle = stringResource(R.string.home_feature_dojo_placeholder_message)
                val dojoIllustration = R.drawable.char_owl

                ProFeatureItem(
                    title = dojoTitle,
                    subtitle = dojoSubtitle,
                    planDescription = stringResource(R.string.home_feature_dojo_plan),
                    icon = Icons.Default.Psychology,
                    color = Color(0xFFCD7F32),
                    illustration = dojoIllustration,
                    isLocked = true,
                    onClick = {
                        proDialogDescription = R.string.home_feature_dojo_plan
                        proDialogFeatures = listOf(dojoDetail, aiTranslatorDetail, tariWalkDetail)
                        showProDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    lockedHint = stringResource(R.string.home_feature_dojo_locked_hint)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 髯溷ｼｱ繝ｻ騾ｶ・ｸ驛｢譎√・郢晢ｽｪ驛｢譎｢・ｽ・ｼ

            Spacer(modifier = Modifier.height(80.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) { data ->
            Snackbar(
                action = {
                    TextButton(onClick = { onClickFeature(FeatureId.UPGRADE) }) {
                        Text(text = stringResource(R.string.home_pro_dialog_action))
                    }
                }
            ) {
                Text(text = data.visuals.message)
            }
        }
    }

    if (showProDialog) {
        ProDialog(
            onDismiss = { showProDialog = false },
            onViewPlans = {
                showProDialog = false
                onClickFeature(FeatureId.UPGRADE)
            },
            description = if (proDialogDescription == Int.MIN_VALUE) null else proDialogDescription,
            features = proDialogFeatures.ifEmpty { defaultProFeatures }
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
        // 1. 称号バー（フクロウ先生の進捗カード）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5856D6))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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

        // 2. レッスン開始カード
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

                // 驛｢・ｧ繝ｻ・ｿ驛｢譎｢・ｽ・ｫ驛｢・ｧ繝ｻ・ｷ驛｢・ｧ繝ｻ・ｨ驍ｵ・ｺ繝ｻ・ｮ鬨ｾ蛹・ｽｽ・ｻ髯ｷ雋櫁㊥繝ｻ・｡繝ｻ・ｨ鬩穂ｼ夲ｽｽ・ｺ (髮弱・・ｽ・｣驍ｵ・ｺ陷会ｽｱ繝ｻ讓抵ｽｹ譎｢・ｽ・ｪ驛｢・ｧ繝ｻ・ｽ驛｢譎｢・ｽ・ｼ驛｢・ｧ繝ｻ・ｹID: char_tarsier)
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
    planDescription: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    illustration: Int? = null,
    isLocked: Boolean = false,
    lockedHint: String? = null
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = if (isLocked) 0.8f else 0.95f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 12.sp,
                        maxLines = 2
                    )
                    Text(
                        text = planDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2
                    )
                    if (isLocked && !lockedHint.isNullOrBlank()) {
                        Text(
                            text = lockedHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 2
                        )
                    }
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
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.home_feature_locked_icon_desc),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProDialog(
    onDismiss: () -> Unit,
    onViewPlans: () -> Unit,
    description: Int?,
    features: List<ProDialogFeature>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_pro_dialog_title)) },
        text = {
            val messageRes = description ?: R.string.home_pro_dialog_message
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(feature.titleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(feature.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
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