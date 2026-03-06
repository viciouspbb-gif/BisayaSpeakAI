package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bisayaspeak.ai.ui.viewmodel.ListeningDataUiState
import com.bisayaspeak.ai.ui.viewmodel.ListeningDataViewModel
import kotlinx.coroutines.launch

/**
 * リスニングデータ管理画面
 * Firebaseへのデータ追加を管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningDataManagementScreen(
    navController: NavController? = null,
    viewModel: ListeningDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // タイトル
        Text(
            text = "リスニングデータ管理",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // 説明
        Text(
            text = "Firebase Realtime Databaseにリスニング練習データを追加します",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 緊急ロールバックボタン（LV32-35削除）
        Button(
            onClick = { 
                scope.launch {
                    viewModel.rollbackLevel32To35Data()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("【緊急】LV32-35データを削除（ロールバック）")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // データ状態確認ボタン
        OutlinedButton(
            onClick = { 
                scope.launch {
                    viewModel.checkCurrentDataStatus()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("現在のデータ状態を確認")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // LV32-35データ一括追加ボタン
        Button(
            onClick = { 
                scope.launch {
                    viewModel.addLevel32To35Data()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LV32-35データをFirebaseに一括追加")
        }
        
        // 通知スケジュール設定ボタン
        OutlinedButton(
            onClick = { 
                scope.launch {
                    viewModel.scheduleNotifications()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("通知スケジュールを設定")
        }
        
        // スケジュール確認ボタン
        OutlinedButton(
            onClick = { 
                scope.launch {
                    viewModel.getReleaseSchedules()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("解放スケジュールを確認")
        }
        
        // LV31データ追加ボタン
        Button(
            onClick = { 
                scope.launch {
                    viewModel.addLevel31Data()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LV31データをFirebaseに追加")
        }
        
        // すべてのデータ取得ボタン
        OutlinedButton(
            onClick = { 
                scope.launch {
                    viewModel.getAllListeningData()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("すべてのリスニングデータを取得")
        }
        
        // UI状態の表示
        val currentState = uiState
        when (currentState) {
            is ListeningDataUiState.Idle -> {
                // 何も表示しない
            }
            
            is ListeningDataUiState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = "処理中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            is ListeningDataUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = currentState.message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            is ListeningDataUiState.DataLoaded -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "データ取得成功",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "合計 ${currentState.data.size} 件のデータが見つかりました",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // 最初の5件を表示
                        currentState.data.take(5).forEachIndexed { index, item ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ID: ${item["id"]}, Level: ${item["level"]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (currentState.data.size > 5) {
                            Text(
                                text = "... 他 ${currentState.data.size - 5} 件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            is ListeningDataUiState.DataStatusLoaded -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "現在のデータ状態",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val existingIds = currentState.existingIds
                        Text(
                            text = "存在するID: ${existingIds.size}件",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // レベル別に表示
                        val level31Ids = existingIds.filter { it in 301L..310L }
                        val level32to35Ids = existingIds.filter { it in 311L..350L }
                        val otherIds = existingIds.filter { it !in 301L..350L }
                        
                        if (level31Ids.isNotEmpty()) {
                            Text(
                                text = "LV31 (301-310): ${level31Ids.size}件 - ${level31Ids.min()}〜${level31Ids.max()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (level32to35Ids.isNotEmpty()) {
                            Text(
                                text = "LV32-35 (311-350): ${level32to35Ids.size}件 - ${level32to35Ids.min()}〜${level32to35Ids.max()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (otherIds.isNotEmpty()) {
                            Text(
                                text = "その他: ${otherIds.size}件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 警告メッセージ
                        if (level32to35Ids.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ 警告：LV32-35のデータが存在します！旧バージョンアプリで全レベルが解放される可能性があります。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (level31Ids.size == 10 && level32to35Ids.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✅ 正常：LV31のみが存在します",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            is ListeningDataUiState.ReleaseSchedulesLoaded -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "解放スケジュール",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        currentState.schedules.forEach { schedule ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "LV${schedule.level}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (schedule.isReleased) "解放済み" else "未解放",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (schedule.isReleased) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            is ListeningDataUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "エラー: ${currentState.message}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // リセットボタン
        TextButton(
            onClick = { viewModel.resetUiState() }
        ) {
            Text("状態をリセット")
        }
        
        // 戻るボタン（navControllerがある場合）
        navController?.let { controller ->
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { controller.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("戻る")
            }
        }
    }
}
