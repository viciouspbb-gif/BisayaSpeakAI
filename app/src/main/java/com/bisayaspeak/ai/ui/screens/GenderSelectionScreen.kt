package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.ui.roleplay.RoleplayChatViewModel
import com.bisayaspeak.ai.voice.GeminiVoiceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GenderSelectionScreen(
    navController: NavController,
    viewModel: RoleplayChatViewModel
) {
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }
    val scope = rememberCoroutineScope()

    var selectedGender by remember { mutableStateOf<UserGender?>(null) }

    DisposableEffect(Unit) {
        onDispose { voiceService.stop() }
    }

    LaunchedEffect(Unit) {
        delay(500)
        voiceService.speak("Maayong buntag! Lalaki ba ka? Babae? O sekreto? ...Kay dinhi magdepende ang atong agianan.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How should I call you?") },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        },
        backgroundColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colors.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Maayong buntag! Lalaki ba ka? Babae? O sekreto?",
                style = MaterialTheme.typography.h6,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "...Kay dinhi magdepende ang atong agianan.",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            GenderOptionItem(
                label = "Lalaki (Male)",
                subtext = "Call me Sir",
                isSelected = selectedGender == UserGender.MALE,
                onClick = {
                    selectedGender = UserGender.MALE
                    voiceService.speak("Lalaki ko.")
                    scope.launch {
                        delay(1000)
                        viewModel.saveUserGender(UserGender.MALE)
                        voiceService.stop()
                        navController.navigate("roleplay_chat") {
                            popUpTo("gender_selection") { inclusive = true }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            GenderOptionItem(
                label = "Babae (Female)",
                subtext = "Call me Ma'am",
                isSelected = selectedGender == UserGender.FEMALE,
                onClick = {
                    selectedGender = UserGender.FEMALE
                    voiceService.speak("Babae ko.")
                    scope.launch {
                        delay(1000)
                        viewModel.saveUserGender(UserGender.FEMALE)
                        voiceService.stop()
                        navController.navigate("roleplay_chat") {
                            popUpTo("gender_selection") { inclusive = true }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            GenderOptionItem(
                label = "Sekreto (Secret)",
                subtext = "Just Friend/Boss",
                isSelected = selectedGender == UserGender.SECRET,
                onClick = {
                    selectedGender = UserGender.SECRET
                    voiceService.speak("Sekreto lang.")
                    scope.launch {
                        delay(1000)
                        viewModel.saveUserGender(UserGender.SECRET)
                        voiceService.stop()
                        navController.navigate("roleplay_chat") {
                            popUpTo("gender_selection") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GenderOptionItem(
    label: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.2f) else MaterialTheme.colors.surface
    val borderColor = if (isSelected) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        border = BorderStroke(2.dp, borderColor),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
    }
}