package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.data.model.LearningLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectionScreen(
    onLevelSelected: (LearningLevel) -> Unit = {},
    onAIConversation: (LearningLevel) -> Unit = {},
    onListening: (LearningLevel) -> Unit = {},
    onPremium: () -> Unit = {},
    onAccountInfo: () -> Unit = {},
    isPremium: Boolean = false,
    aiConversationCount: Int = 0,
    onShowAd: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // --- ヘッダー ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "レベルを選択",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onAccountInfo) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // セクション：会話・練習
        SectionTitle("会話・練習")

        MenuCard(
            title = "AI会話モード",
            icon = Icons.Outlined.Chat,
            onClick = { onAIConversation(LearningLevel.BEGINNER) }
        )

        MenuCard(
            title = "リスニング練習",
            icon = Icons.Outlined.GraphicEq,
            onClick = {
                onListening(LearningLevel.BEGINNER)
            }
        )

        MenuCard(
            title = "発音練習",
            icon = Icons.Outlined.Mic,
            onClick = {
                onLevelSelected(LearningLevel.BEGINNER)
            }
        )

        Spacer(Modifier.height(28.dp))

        // セクション：レベル別学習
        SectionTitle("レベル別学習")

        MenuCard(
            title = "初級",
            icon = Icons.Outlined.Chat,
            onClick = { onLevelSelected(LearningLevel.BEGINNER) }
        )

        MenuCard(
            title = "中級",
            icon = Icons.Outlined.Chat,
            onClick = { onLevelSelected(LearningLevel.INTERMEDIATE) }
        )

        MenuCard(
            title = "上級",
            icon = Icons.Outlined.Chat,
            onClick = { onLevelSelected(LearningLevel.ADVANCED) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
