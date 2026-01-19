package com.bisayaspeak.ai.ui.roleplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompletionDialog(
    themeTitle: String,
    flavor: RoleplayThemeFlavor,
    goal: String,
    farewellLine: String,
    farewellTranslation: String,
    onGoHome: () -> Unit
) {
    val displayFarewell = farewellLine.ifBlank { "Daghang salamat! Tawga ko usab kung gusto nimo makig-istorya." }
    val displayFarewellJa = farewellTranslation.ifBlank { "楽しかったよ。またいつでも呼んでね。" }
    val contextLine = if (flavor == RoleplayThemeFlavor.CASUAL) {
        "今日のシチュエーション: ${themeTitle.ifBlank { "気ままトーク" }}"
    } else {
        "今日のシチュエーション: ${themeTitle.ifBlank { "特別なシーン" }}"
    }
    val goalLine = goal.ifBlank { "また新しい会話を探しにいこう。" }

    AlertDialog(
        onDismissRequest = onGoHome,
        confirmButton = {
            TextButton(onClick = onGoHome) {
                Text(text = "ホームに戻る", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = displayFarewell,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = displayFarewellJa,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = contextLine, fontWeight = FontWeight.SemiBold)
                Text(text = "タリのきょうのお願い: $goalLine", modifier = Modifier.padding(top = 6.dp))
                Spacer(modifier = Modifier.padding(top = 6.dp))
                Text(
                    text = "またタリを呼びたくなったら、この画面からいつでも戻ってきてね。",
                    textAlign = TextAlign.Start
                )
            }
        },
        dismissButton = null
    )
}
