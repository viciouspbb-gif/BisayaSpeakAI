package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.data.model.RolePlayScene
import com.bisayaspeak.ai.data.model.SceneDifficulty
import com.bisayaspeak.ai.data.repository.RolePlayRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePlaySceneScreen(
    genreId: String,
    isPremium: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onSceneSelected: (RolePlayScene) -> Unit = {},
    onPremiumRequired: () -> Unit = {}
) {
    val repository = remember { RolePlayRepository() }
    val genre = remember { repository.getGenreById(genreId) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(genre?.titleJa ?: "シーン選択") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD2691E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "シーンを選択",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = genre?.description ?: "",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            genre?.scenes?.forEach { scene ->
                SceneCard(
                    scene = scene,
                    accentColor = genre.accentColor,
                    isPremium = isPremium,
                    onClick = {
                        if (scene.isFreeTrialAvailable || isPremium) {
                            onSceneSelected(scene)
                        } else {
                            onPremiumRequired()
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SceneCard(
    scene: RolePlayScene,
    accentColor: Color,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val isLocked = !scene.isFreeTrialAvailable && !isPremium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(
                if (isLocked) Color(0xFFF5F5F5) else Color.White,
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .background(
                        if (isLocked) Color(0xFFCCCCCC) else accentColor,
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scene.titleJa,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) Color(0xFF999999) else Color(0xFF222222)
                    )

                    Spacer(Modifier.width(8.dp))

                    // 無料体験バッジ
                    if (scene.isFreeTrialAvailable) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFFFE082), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CardGiftcard,
                                contentDescription = null,
                                tint = Color(0xFFD2691E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "1回無料体験",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD2691E)
                            )
                        }
                    }

                    // ロックバッジ
                    if (isLocked) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "プレミアム専用",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = scene.description,
                    fontSize = 14.sp,
                    color = if (isLocked) Color(0xFF999999) else Color(0xFF666666)
                )

                Spacer(Modifier.height(8.dp))

                Row {
                    // 難易度バッジ
                    Box(
                        modifier = Modifier
                            .background(
                                when (scene.difficulty) {
                                    SceneDifficulty.BEGINNER -> Color(0xFFE8F5E9)
                                    SceneDifficulty.INTERMEDIATE -> Color(0xFFE3F2FD)
                                    SceneDifficulty.ADVANCED -> Color(0xFFFCE4EC)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = scene.difficulty.displayName,
                            fontSize = 12.sp,
                            color = when (scene.difficulty) {
                                SceneDifficulty.BEGINNER -> Color(0xFF4CAF50)
                                SceneDifficulty.INTERMEDIATE -> Color(0xFF2196F3)
                                SceneDifficulty.ADVANCED -> Color(0xFFE91E63)
                            }
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // 所要時間
                    Text(
                        text = "約${scene.estimatedMinutes}分",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}
