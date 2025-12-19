package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.NewReleases
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ReleaseNote(
    val version: String,
    val date: String,
    val changes: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseNotesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    val releaseNotes = listOf(
        ReleaseNote(
            version = "1.7.0",
            date = "2025年11月",
            changes = listOf(
                "新しいホーム画面デザインを追加",
                "AI会話機能のレベル選択を改善",
                "リスニング練習機能を追加",
                "発音診断の精度を向上",
                "PRO専用機能セクションを追加",
                "UIデザインをApple風に統一"
            )
        ),
        ReleaseNote(
            version = "1.6.0",
            date = "2025年10月",
            changes = listOf(
                "プレミアムプラン機能を追加",
                "広告表示システムを実装",
                "AI会話の使用回数制限を追加",
                "アカウント情報画面を追加"
            )
        ),
        ReleaseNote(
            version = "1.5.0",
            date = "2025年9月",
            changes = listOf(
                "初級・中級・上級のレベル別学習を追加",
                "発音診断機能を改善",
                "学習コンテンツを拡充",
                "バグ修正とパフォーマンス改善"
            )
        ),
        ReleaseNote(
            version = "1.0.0",
            date = "2025年8月",
            changes = listOf(
                "Learn Bisaya AI 初回リリース",
                "基本的な発音練習機能",
                "AI会話機能",
                "ビサヤ語学習コンテンツ"
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更新情報") },
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
                text = "リリースノート",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Learn Bisaya AI の最新情報をお届けします",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            releaseNotes.forEach { note ->
                ReleaseNoteCard(note)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ReleaseNoteCard(note: ReleaseNote) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.NewReleases,
                contentDescription = null,
                tint = Color(0xFFD2691E),
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = "バージョン ${note.version}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
                Text(
                    text = note.date,
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            note.changes.forEach { change ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "• ",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = change,
                        fontSize = 14.sp,
                        color = Color(0xFF444444),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
