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
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RolePlayTheme(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePlayScreen(
    onNavigateBack: () -> Unit = {},
    onThemeSelected: (RolePlayTheme) -> Unit = {}
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    val themes = listOf(
        RolePlayTheme(
            id = "food",
            title = "食べ物",
            description = "レストランでの注文、料理の感想",
            icon = Icons.Outlined.Restaurant,
            accentColor = Color(0xFFFF6B6B)
        ),
        RolePlayTheme(
            id = "shopping",
            title = "買い物",
            description = "商品選び、値段交渉、支払い",
            icon = Icons.Outlined.ShoppingCart,
            accentColor = Color(0xFF55C27A)
        ),
        RolePlayTheme(
            id = "hobby",
            title = "趣味",
            description = "好きなこと、休日の過ごし方",
            icon = Icons.Outlined.SportsEsports,
            accentColor = Color(0xFF3C8DFF)
        ),
        RolePlayTheme(
            id = "leisure",
            title = "レジャー",
            description = "観光、アクティビティ、娯楽",
            icon = Icons.Outlined.BeachAccess,
            accentColor = Color(0xFFFFC84D)
        ),
        RolePlayTheme(
            id = "work",
            title = "仕事",
            description = "職場での会話、ビジネスシーン",
            icon = Icons.Outlined.Work,
            accentColor = Color(0xFF9C6BFF)
        ),
        RolePlayTheme(
            id = "school",
            title = "学校",
            description = "授業、友達との会話、先生との対話",
            icon = Icons.Outlined.School,
            accentColor = Color(0xFF4ECDC4)
        ),
        RolePlayTheme(
            id = "friends",
            title = "友達",
            description = "日常会話、遊びの約束、雑談",
            icon = Icons.Outlined.People,
            accentColor = Color(0xFFFF8C42)
        ),
        RolePlayTheme(
            id = "travel",
            title = "旅行",
            description = "観光案内、道を尋ねる、おすすめスポット",
            icon = Icons.Outlined.Flight,
            accentColor = Color(0xFF3C8DFF)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ロールプレイ会話") },
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
                text = "テーマを選択",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "AIが様々な役を演じて、自然な会話練習をサポートします。",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            themes.forEach { theme ->
                RolePlayThemeCard(
                    theme = theme,
                    onClick = { onThemeSelected(theme) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RolePlayThemeCard(
    theme: RolePlayTheme,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(60.dp)
                .background(theme.accentColor, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(18.dp))

        Icon(
            imageVector = theme.icon,
            contentDescription = null,
            tint = theme.accentColor,
            modifier = Modifier.size(36.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Text(
                text = theme.description,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
    }
}
