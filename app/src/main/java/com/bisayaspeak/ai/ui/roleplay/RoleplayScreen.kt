package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// データモデル
data class RoleplayScenario(val id: String, val title: String, val description: String, val requiredLevel: Int, val iconEmoji: String)

// シナリオデータ
val roleplayScenarios = listOf(
    RoleplayScenario("rp_airport", "マクタン空港にて", "到着ゲートを出て、タクシー乗り場への行き方を係員に尋ねましょう。", 1, "✈️"),
    RoleplayScenario("rp_taxi", "タクシー移動", "行き先を伝えて、メーターを使ってもらうよう交渉します。", 2, "🚕"),
    RoleplayScenario("rp_hotel", "ホテルチェックイン", "予約の名前を伝え、Wi-Fiのパスワードを聞き出します。", 3, "🏨")
)

@Composable
fun RoleplayListScreen(userCurrentLevel: Int, onScenarioClick: (RoleplayScenario) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp)) {
        Text("AI ロールプレイ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(roleplayScenarios) { scenario ->
                // ★ここがポイント：強制ロック解除
                RoleplayCard(scenario = scenario, isLocked = false, onClick = { onScenarioClick(scenario) })
            }
        }
    }
}

@Composable
fun RoleplayCard(scenario: RoleplayScenario, isLocked: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isLocked) Color(0xFF2C2C2C) else Color(0xFF3E4158)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(if (isLocked) Color.Gray else Color(0xFF00C853), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (isLocked) Icon(Icons.Default.Lock, "Locked", tint = Color.White) else Text(scenario.iconEmoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f).alpha(if (isLocked) 0.5f else 1f)) {
                Text("Lv ${scenario.requiredLevel}: ${scenario.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(scenario.description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}