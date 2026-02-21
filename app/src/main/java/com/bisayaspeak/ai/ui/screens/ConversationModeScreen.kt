@file:OptIn(ExperimentalMaterial3Api::class)

package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.data.model.ConversationMode
import com.bisayaspeak.ai.R

private enum class ConversationSpeaker {
    JAPANESE,
    BISAYAN
}

private data class ConversationMessage(
    val speaker: ConversationSpeaker,
    val transcript: String,
    val translation: String
)

@Composable
fun ConversationModeScreen(
    isPremium: Boolean,
    onOpenPremiumInfo: () -> Unit,
    onStartFreeTalk: () -> Unit,
    onStartRolePlay: () -> Unit,
    onBack: () -> Unit
) {
    val sampleHistory = listOf(
        ConversationMessage(
            speaker = ConversationSpeaker.JAPANESE,
            transcript = stringResource(R.string.conversation_mode_sample_question),
            translation = stringResource(R.string.conversation_mode_sample_question_translation)
        ),
        ConversationMessage(
            speaker = ConversationSpeaker.BISAYAN,
            transcript = stringResource(R.string.conversation_mode_sample_answer_transcript),
            translation = stringResource(R.string.conversation_mode_sample_answer_translation)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversation_mode_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoicePipelineRow(isPremium = isPremium)

                ConversationHistoryCard(
                    messages = sampleHistory,
                    modifier = Modifier.weight(1f)
                )

                ActionButtons(
                    isPremium = isPremium,
                    onStartFreeTalk = onStartFreeTalk,
                    onStartRolePlay = onStartRolePlay
                )
            }

            if (!isPremium) {
                PremiumLockOverlay(onOpenPremiumInfo = onOpenPremiumInfo)
            }
        }
    }
}

@Composable
private fun VoicePipelineRow(isPremium: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PipelineCard(
            title = stringResource(R.string.conversation_mode_pipeline_title_ja_ceb),
            description = stringResource(R.string.conversation_mode_pipeline_desc_ja_ceb),
            accentColor = Color(0xFF4A90E2),
            locked = !isPremium,
            steps = listOf("🎙️", "STT", "Translate", "🔊")
        )
        PipelineCard(
            title = stringResource(R.string.conversation_mode_pipeline_title_ceb_ja),
            description = stringResource(R.string.conversation_mode_pipeline_desc_ceb_ja),
            accentColor = Color(0xFF8BC34A),
            locked = !isPremium,
            steps = listOf("🎙️", "STT", "Translate", "🔊")
        )
    }
}

@Composable
private fun PipelineCard(
    title: String,
    description: String,
    accentColor: Color,
    locked: Boolean,
    steps: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (locked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                steps.forEach { step ->
                    Text(step)
                }
            }
        }
    }
}

@Composable
private fun ConversationHistoryCard(
    messages: List<ConversationMessage>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                stringResource(R.string.conversation_mode_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ConversationBubble(message)
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(message: ConversationMessage) {
    val bubbleColor = if (message.speaker == ConversationSpeaker.JAPANESE) {
        Color(0xFFE3F2FD)
    } else {
        Color(0xFFE8F5E9)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bubbleColor, shape = RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = if (message.speaker == ConversationSpeaker.JAPANESE) {
                stringResource(R.string.japanese)
            } else {
                stringResource(R.string.bisaya)
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = message.transcript, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun ActionButtons(
    isPremium: Boolean,
    onStartFreeTalk: () -> Unit,
    onStartRolePlay: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onStartFreeTalk,
            enabled = isPremium,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7B4DFF)
            )
        ) {
            Icon(imageVector = Icons.Filled.Chat, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.conversation_mode_button_free_talk))
        }

        Button(
            onClick = onStartRolePlay,
            enabled = isPremium,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00A99D)
            )
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.conversation_mode_button_roleplay))
        }
    }
}

@Composable
private fun BoxScope.PremiumLockOverlay(onOpenPremiumInfo: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF7043))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.conversation_mode_premium_banner_title), fontWeight = FontWeight.SemiBold)
            }
            Text(stringResource(R.string.conversation_mode_premium_banner_desc))
            TextButton(onClick = onOpenPremiumInfo) {
                Text(stringResource(R.string.conversation_mode_premium_banner_cta))
            }
        }
    }
}
