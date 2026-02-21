package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionDialog(
    themeTitle: String,
    flavor: RoleplayThemeFlavor,
    goal: String,
    farewellLine: String,
    farewellTranslation: String,
    farewellExplanation: String,
    sheetState: SheetState,
    onPlayFarewell: () -> Unit,
    onCopyFarewell: () -> Unit,
    onGoHome: () -> Unit
) {
    val displayFarewell = farewellLine.ifBlank { "Daghang salamat! Tawga ko usab kung gusto nimo makig-istorya." }
    val displayFarewellJa = farewellTranslation.ifBlank { "楽しかったよ。またいつでも呼んでね。" }
    val displayExplanation = farewellExplanation.ifBlank { "Bisayaでは別れ際に相手を気遣う言葉を添えるのが定番です。" }
    val contextLine = if (flavor == RoleplayThemeFlavor.CASUAL) {
        "今日のシチュエーション: ${themeTitle.ifBlank { "気ままトーク" }}"
    } else {
        "ミッション: ${themeTitle.ifBlank { "特別なシーン" }}"
    }
    val goalLine = goal.ifBlank { "また新しい会話を探しにいこう。" }
    ModalBottomSheet(
        onDismissRequest = onGoHome,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Transparent
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF0B1120), Color(0xFF172542))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "タリの別れの言葉",
                        color = Color(0xFFFFEAA7),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "最後のフレーズを声に出して、記憶に刻もう。",
                        color = Color(0xFFB5C6FF),
                        fontSize = 13.sp
                    )
                }

                FarewellCard(
                    bisayaLine = displayFarewell,
                    japaneseLine = displayFarewellJa,
                    explanation = displayExplanation,
                    onPlay = onPlayFarewell
                )

                Divider(color = Color(0x26FFFFFF))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "今日の振り返り",
                        color = Color(0xFF9AE6B4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "タリのお願いとシチュエーションを読み返して、ゴールへの理解を固めよう。",
                        color = Color(0xFFB5FFC9),
                        fontSize = 13.sp
                    )
                }

                ReflectionCard(
                    contextLine = contextLine,
                    goalLine = goalLine
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCopyFarewell
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "フレーズをコピー",
                            tint = Color(0xFF93C5FD)
                        )
                        Text(
                            text = "フレーズをコピー",
                            color = Color(0xFF93C5FD),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onGoHome,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("ホームに戻る", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FarewellCard(
    bisayaLine: String,
    japaneseLine: String,
    explanation: String,
    onPlay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152036)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Bisaya", color = Color(0xFF7DD3FC), fontSize = 13.sp)
                    Text(
                        text = bisayaLine,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "再生",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = japaneseLine,
                color = Color(0xFFFFF7D6),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = explanation,
                color = Color(0xFF9CA9D9),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ReflectionCard(
    contextLine: String,
    goalLine: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReflectionRow(label = "シーン", value = contextLine)
            ReflectionRow(label = "タリのお願い", value = goalLine)
            ReflectionRow(
                label = "学びメモ",
                value = "Bisayaの別れでは、相手を労う一言を添えると暖かい印象になります。"
            )
        }
    }
}

@Composable
private fun ReflectionRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = Color(0xFF60A5FA), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp)
    }
}
