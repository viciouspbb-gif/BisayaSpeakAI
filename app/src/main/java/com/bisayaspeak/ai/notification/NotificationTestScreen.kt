package com.bisayaspeak.ai.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisayaspeak.ai.BuildConfig
import javax.inject.Inject

/**
 * 通知テスト画面
 */
@Composable
fun NotificationTestScreen(
    onBack: () -> Unit,
    notificationTestViewModel: NotificationTestViewModel = hiltViewModel()
) {
    val testResults by notificationTestViewModel.testResults.collectAsState()
    val resetStatus by notificationTestViewModel.resetStatus.collectAsState()
    val isResetting by notificationTestViewModel.isResetting.collectAsState()
    val currentInfo by notificationTestViewModel.currentInfo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "通知テスト",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(onClick = onBack) {
                Text("戻る")
            }
        }
        
        // デバッグ用通知テスト（debugビルドのみ）
        if (BuildConfig.DEBUG) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "デバッグ用通知",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 1分後のデバッグ通知
                    Button(
                        onClick = { notificationTestViewModel.scheduleDebugNotification(1) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔧 1分後に通知をテスト")
                    }
                    
                    // 2分後のデバッグ通知
                    Button(
                        onClick = { notificationTestViewModel.scheduleDebugNotification(2) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔧 2分後に通知をテスト")
                    }
                }
            }
        }
        
        // 現在の状態情報
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "現在の状態",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentInfo,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // テストボタン群
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "通知テスト",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 平日・無料ユーザー
                Button(
                    onClick = { notificationTestViewModel.sendNotification("平日・無料ユーザー") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("平日・無料ユーザー通知")
                }
                
                // 平日・有料ユーザー
                Button(
                    onClick = { notificationTestViewModel.sendNotification("平日・有料ユーザー") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("平日・有料ユーザー通知")
                }
                
                // 土曜日・無料ユーザー
                Button(
                    onClick = { notificationTestViewModel.sendNotification("土曜日・無料ユーザー") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("土曜日・無料ユーザー通知")
                }
                
                // 土曜日・有料ユーザー
                Button(
                    onClick = { notificationTestViewModel.sendNotification("土曜日・有料ユーザー") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("土曜日・有料ユーザー通知")
                }
                
                // 全パターンテスト
                OutlinedButton(
                    onClick = { notificationTestViewModel.sendAllNotifications() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("全パターンテスト実行")
                }
                
                // 強制リセットボタン
                Button(
                    onClick = { notificationTestViewModel.rebuildSchedule() },
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
            }
        }
        
        // テスト結果
        if (testResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "テスト結果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = testResults,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Button(
                        onClick = { notificationTestViewModel.clearResults() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("クリア")
                    }
                }
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
        
        // 説明
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "通知パターン説明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        appendLine("• 平日・無料: フレーズ + 「タリと練習してみよう！」")
                        appendLine("• 平日・有料: 「フレーズ」って、今言いたくなっちゃった。+ 「今日はどんな会話する？」")
                        appendLine("• 土曜・無料: 「タリと自由に会話してみる？ プレミアムプランで可能です。」")
                        appendLine("• 土曜・有料: 「今日はタリと（恋愛/ビジネス）会話してみる？ 最初に話しかけてね。」")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
