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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.data.model.ScriptDialogue
import com.bisayaspeak.ai.data.repository.RolePlayRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptViewerScreen(
    sceneId: String,
    onNavigateBack: () -> Unit = {},
    onStartRolePlay: () -> Unit = {},
    isPremium: Boolean = false
) {
    val repository = remember { RolePlayRepository() }
    val scriptData = remember { repository.getScriptData(sceneId) }
    val scene = remember { repository.getSceneById(sceneId) }
    // 無料台本: 空港チェックイン（airport_checkin）のみ
    val isFreeScript = scene?.isFreeTrialAvailable == true || sceneId == "airport_checkin" || sceneId == "airport"

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("台本閲覧") },
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
            // シーン情報
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = scriptData?.title ?: scene?.titleJa ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = scriptData?.description ?: scene?.description ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 台本表示条件分岐
            // 条件: (1) プレミアムユーザー または (2) 無料台本（空港のみ）
            if (isPremium || isFreeScript) {
                // 無料ユーザー向け説明（無料台本のみ）
                if (!isPremium && isFreeScript) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFFFFF8E1), RoundedCornerShape(24.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "",
                                fontSize = 24.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "無料台本",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF222222)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "このシーンは無料で台本を閲覧できます。AIとロールプレイするにはプレミアム会員登録が必要です。",
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // 台本の会話
                scriptData?.dialogues?.forEachIndexed { index, dialogue ->
                    DialogueCard(dialogue = dialogue, index = index)
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                // プレミアム限定表示
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFD2691E)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "プレミアム限定",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "このシーンの台本はプレミアム会員限定です。\nプレミアムにアップグレードして、すべての台本とAIロールプレイをお楽しみください。",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // プレミアム誘導ボタン
            Button(
                onClick = onStartRolePlay,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2691E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AIとロールプレイする（プレミアム専用）",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun DialogueCard(
    dialogue: ScriptDialogue,
    index: Int
) {
    val isAI = dialogue.speaker == "AI"
    val backgroundColor = if (isAI) Color(0xFFF5F5F5) else Color(0xFFE3F2FD)
    val speakerColor = if (isAI) Color(0xFFD2691E) else Color(0xFF2196F3)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // スピーカーアイコン
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(speakerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAI) "AI" else "You",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 会話内容
        Column(
            modifier = Modifier
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // ビサヤ語
            Text(
                text = dialogue.textBisaya,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222),
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(8.dp))

            // 日本語訳
            Text(
                text = dialogue.textJapanese,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )

            // 英語訳
            if (dialogue.textEnglish.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dialogue.textEnglish,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    lineHeight = 18.sp
                )
            }

            // 注釈
            if (!dialogue.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "💡 ${dialogue.note}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
