package com.bisayaspeak.ai.ui.missions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class MissionScenario(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionListScreen(
    onNavigateBack: () -> Unit,
    onMissionSelect: (String) -> Unit
) {
    val missions = listOf(
        MissionScenario(
            id = "market_bargain",
            title = "市場で値切り",
            description = "現地の市場で安く買ってみよう",
            icon = Icons.Default.ShoppingCart
        ),
        MissionScenario(
            id = "staff_instruction",
            title = "新人指示トレーニング",
            description = "新人スタッフに3つのタスクを伝えよう",
            icon = Icons.Default.LocalTaxi
        ),
        MissionScenario(
            id = "jealousy",
            title = "嫉妬の弁明ミッション",
            description = "拗ねた恋人を安心させよう",
            icon = Icons.Default.Restaurant
        ),
        MissionScenario(
            id = "making_up",
            title = "親友への謝罪作戦",
            description = "親友に謝るミッション",
            icon = Icons.Default.Restaurant
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI ミッション選択") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(missions) { mission ->
                MissionCard(
                    mission = mission,
                    onClick = { onMissionSelect(mission.id) }
                )
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: MissionScenario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = mission.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = mission.title, style = MaterialTheme.typography.titleMedium)
                Text(text = mission.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
