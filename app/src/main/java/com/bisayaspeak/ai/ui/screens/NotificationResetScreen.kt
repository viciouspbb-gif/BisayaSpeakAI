package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisayaspeak.ai.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 通知強制リセット画面
 */
@Composable
fun NotificationResetScreen(
    onBack: () -> Unit,
    notificationResetViewModel: NotificationResetViewModel = hiltViewModel()
) {
    val resetStatus by notificationResetViewModel.resetStatus.collectAsState()
    val isResetting by notificationResetViewModel.isResetting.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "通知リセット",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = onBack) {
                Text("戻る")
            }
        }
        
        // 説明
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "通知スケジュールのリセット",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "現在の通知スケジュールをすべてキャンセルし、新しく再構築します。\n\n" +
                          "これにより、通知が正しく動作しない問題を解決できます。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // リセットボタン
        Button(
            onClick = { notificationResetViewModel.resetSchedule() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isResetting
        ) {
            if (isResetting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("リセット中...")
            } else {
                Text("🔄 通知スケジュールを再構築")
            }
        }
        
        // リセットステータス
        if (resetStatus.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "リセット結果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = resetStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (resetStatus.startsWith("✅")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
        
        // 注意事項
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "注意事項",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "• リセット後、次の18:00に通知が送信されます\n" +
                          "• 既存の通知予約はすべてキャンセルされます\n" +
                          "• アプリの再起動は必要ありません",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
