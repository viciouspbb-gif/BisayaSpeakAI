package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.viewmodel.AiExplanation
import com.bisayaspeak.ai.ui.viewmodel.DictionaryLanguage
import com.bisayaspeak.ai.ui.viewmodel.DictionaryUiState
import com.bisayaspeak.ai.ui.viewmodel.DictionaryViewModel
import com.bisayaspeak.ai.ui.viewmodel.TranslationCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    isProVersion: Boolean = false,
    viewModel: DictionaryViewModel = viewModel(factory = DictionaryViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.dictionary_title), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(R.string.dictionary_subtitle),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF01060F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF01060F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF01060F), Color(0xFF081A2E))
                    )
                )
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExploreSection(
                state = uiState,
                onQueryChange = viewModel::updateQuery,
                onSubmit = viewModel::submitExploration,
                onPlayBisaya = viewModel::speakBisaya
            )
        }
    }
}

@Composable
private fun ExploreSection(
    state: DictionaryUiState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPlayBisaya: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.dictionary_query_placeholder), color = Color(0xFF7E8DA8)) },
            leadingIcon = {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF38BDF8))
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF1F2937)
            )
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6))
        ) {
            Text(
                text = if (state.isLoading) {
                    stringResource(R.string.dictionary_button_searching)
                } else {
                    stringResource(R.string.dictionary_button_explore)
                },
                fontWeight = FontWeight.Bold
            )
        }

        state.errorMessage?.let { message ->
            ErrorCard(message)
        }

        AnimatedVisibility(visible = state.candidates.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.candidates) { candidate ->
                    CandidateCard(candidate, onPlayBisaya)
                }
            }
        }

        state.explanation?.let { explanation ->
            ExplanationCard(explanation)
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: TranslationCandidate,
    onPlayBisaya: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2E))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    candidate.bisaya,
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onPlayBisaya(candidate.bisaya) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = stringResource(R.string.dictionary_play_bisaya_desc),
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
            Text(candidate.japanese, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(candidate.english, color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            InfoPill(stringResource(R.string.dictionary_politeness_label, candidate.politeness))
            InfoPill(stringResource(R.string.dictionary_situation_label, candidate.situation))
            Text(candidate.nuance, color = Color(0xFFD1D9E6), fontSize = 13.sp)
            Text(candidate.tip, color = Color(0xFF93E6C8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color(0x3322C55E)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color(0xFF22C55E),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ExplanationCard(explanation: AiExplanation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.dictionary_explanation_title), color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
            Text(explanation.summary, color = Color.White)
            Text(stringResource(R.string.dictionary_usage_title), color = Color(0xFF38BDF8), fontSize = 13.sp)
            Text(explanation.usage, color = Color(0xFFD7E0F5))
            if (explanation.relatedPhrases.isNotEmpty()) {
                Text(stringResource(R.string.dictionary_related_title), color = Color(0xFF22C55E), fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    explanation.relatedPhrases.forEach { phrase ->
                        Text("・$phrase", color = Color(0xFFA5B4FC))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun languageLabel(language: DictionaryLanguage): String = when (language) {
    DictionaryLanguage.JAPANESE -> stringResource(R.string.japanese)
    DictionaryLanguage.BISAYA -> stringResource(R.string.bisaya)
    DictionaryLanguage.ENGLISH -> stringResource(R.string.english)
    DictionaryLanguage.UNKNOWN -> stringResource(R.string.dictionary_language_unknown)
}
