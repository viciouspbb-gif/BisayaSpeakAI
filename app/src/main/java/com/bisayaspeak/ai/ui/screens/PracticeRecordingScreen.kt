package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bisayaspeak.ai.util.LocaleUtils
import java.util.Locale

@Composable
fun PracticeRecordingScreen() {
    val locale by LocaleUtils.localeState.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (locale.language == "ja") "練習 / 録音" else "Practice / Recording",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            android.util.Log.d("PracticeRecordingScreen", "Record tapped locale=${'$'}{locale.language}")
        }) {
            Text(if (locale.language == "ja") "録音開始" else "Start Recording")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val newLocale = if (locale.language == "ja") Locale.ENGLISH else Locale.JAPANESE
            LocaleUtils.setLocale(newLocale)
            android.util.Log.d("PracticeRecordingScreen", "Locale switched to ${'$'}newLocale")
        }) {
            Text(if (locale.language == "ja") "英語に切替" else "日本語に切替")
        }
    }
}
