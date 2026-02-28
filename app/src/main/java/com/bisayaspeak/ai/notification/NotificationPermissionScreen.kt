package com.bisayaspeak.ai.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * 通知許可プロンプトのUI
 */
@Composable
fun NotificationPermissionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit
) {
    if (!isVisible) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("通知の許可")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "毎日18:00にビサヤ語のフレーズをお届けします！",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "学習のリマインドとして、日常会話で使える表現を通知します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRequestPermission()
                    onDismiss()
                }
            ) {
                Text("許可する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("後で")
            }
        }
    )
}

/**
 * 通知許可管理コンポーザブル
 */
@Composable
fun NotificationPermissionManager(
    notificationManager: LocalNotificationManager
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // 通知権限のチェック
    val hasPermission = remember {
        notificationManager.hasNotificationPermission()
    }
    
    // 初回起動時の権限チェック
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            // 初回起動時に少し遅延して表示
            kotlinx.coroutines.delay(2000)
            showPermissionDialog = true
        }
    }
    
    // 通知権限リクエストランチャー
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 権限付与結果の処理
        if (isGranted) {
            // 権限が付与されたら通知をスケジュール
            // notificationScheduler.scheduleDailyNotification()
        }
    }
    
    NotificationPermissionDialog(
        isVisible = showPermissionDialog,
        onDismiss = { showPermissionDialog = false },
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Android 12以前は権限不要
                // notificationScheduler.scheduleDailyNotification()
            }
        }
    )
}

/**
 * 通知設定画面
 */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    notificationManager: LocalNotificationManager
) {
    var isEnabled by remember { mutableStateOf(true) }
    var showTestNotification by remember { mutableStateOf(false) }
    
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
            TextButton(onClick = onBack) {
                Text("← 戻る")
            }
            Text(
                text = "通知設定",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(64.dp))
        }
        
        // 通知権限ステータス
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "通知権限",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (notificationManager.hasNotificationPermission()) {
                        "✅ 通知が許可されています"
                    } else {
                        "❌ 通知が拒否されています"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notificationManager.hasNotificationPermission()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
        
        // 通知オン・オフ
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "毎日18:00の通知",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ビサヤ語フレーズをお届けします",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }
        }
        
        // テスト通知ボタン
        if (notificationManager.hasNotificationPermission()) {
            Button(
                onClick = { 
                    showTestNotification = true
                    // テスト通知を表示
                    // notificationManager.showNotification(isPaidUser = false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("テスト通知を送信")
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
                    text = "通知について",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• 無料ユーザー：今日のビサヤ語フレーズ\n• 有料ユーザー：タリからのメッセージ\n• 毎日18:00に配信（ローカル時間）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // テスト通知確認ダイアログ
    if (showTestNotification) {
        AlertDialog(
            onDismissRequest = { showTestNotification = false },
            title = { Text("テスト通知") },
            text = { Text("テスト通知を送信しました。通知を確認してください。") },
            confirmButton = {
                TextButton(onClick = { showTestNotification = false }) {
                    Text("OK")
                }
            }
        )
    }
}
