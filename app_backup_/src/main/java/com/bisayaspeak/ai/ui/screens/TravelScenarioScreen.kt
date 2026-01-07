package com.bisayaspeak.ai.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bisayaspeak.ai.data.model.LearningLevel

data class TravelScenario(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isFree: Boolean = false  // 無料で利用可能かどうか
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScenarioScreen(
    onNavigateBack: () -> Unit = {},
    onScenarioSelected: (TravelScenario) -> Unit = {}
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    val scenarios = listOf(
        TravelScenario(
            id = "airport",
            title = "空港",
            description = "チェックイン、荷物預け、搭乗手続き",
            icon = Icons.Outlined.Flight,
            accentColor = Color(0xFF3C8DFF),
            isFree = true  // 空港のみ無料
        ),
        TravelScenario(
            id = "hotel",
            title = "ホテル",
            description = "予約、チェックイン、ルームサービス",
            icon = Icons.Outlined.Hotel,
            accentColor = Color(0xFF55C27A),
            isFree = false
        ),
        TravelScenario(
            id = "restaurant",
            title = "レストラン",
            description = "注文、支払い、おすすめ料理",
            icon = Icons.Outlined.Restaurant,
            accentColor = Color(0xFFFFC84D),
            isFree = false
        ),
        TravelScenario(
            id = "taxi",
            title = "タクシー",
            description = "行き先指示、料金交渉、道案内",
            icon = Icons.Outlined.DirectionsCar,
            accentColor = Color(0xFF9C6BFF),
            isFree = false
        ),
        TravelScenario(
            id = "trouble",
            title = "トラブル",
            description = "道に迷った、物を失くした、助けを求める",
            icon = Icons.Outlined.Warning,
            accentColor = Color(0xFFFF6B6B),
            isFree = false
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("旅行シチュエーション") },
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
                text = "シチュエーションを選択",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "実際の旅行場面を想定したロールプレイで、実践的な会話力を身につけましょう。",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            scenarios.forEach { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onClick = { onScenarioSelected(scenario) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ScenarioCard(
    scenario: TravelScenario,
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
                .background(scenario.accentColor, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(18.dp))

        Icon(
            imageVector = scenario.icon,
            contentDescription = null,
            tint = scenario.accentColor,
            modifier = Modifier.size(36.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scenario.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Text(
                text = scenario.description,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
    }
}
