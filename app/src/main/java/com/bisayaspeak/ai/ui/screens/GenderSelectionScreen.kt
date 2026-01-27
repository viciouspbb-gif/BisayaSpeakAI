package com.bisayaspeak.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.viewmodel.GenderSelectionViewModel
import com.bisayaspeak.ai.voice.GeminiVoiceCue
import com.bisayaspeak.ai.voice.GeminiVoiceService

@Composable
fun GenderSelectionScreen(
    navController: NavController,
    viewModel: GenderSelectionViewModel
) {
    val tariSpeech = stringResource(R.string.gender_selection_tari_speech)
    val tariSpeechTranslation = stringResource(R.string.gender_selection_tari_translation)

    var showTranslation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceService = remember { GeminiVoiceService(context) }
    val speakTari = remember(voiceService) {
        {
            voiceService.stop()
            voiceService.speak(
                text = tariSpeech,
                cue = GeminiVoiceCue.TALK_HIGH
            )
        }
    }
    val targetRoute = remember {
        AppRoute.RolePlayChat.route.replace("{scenarioId}", "tari_infinite_mode")
    }

    LaunchedEffect(Unit) {
        GeminiVoiceService.stopAllActive()
        speakTari()
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceService.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.char_tarsier),
                contentDescription = stringResource(R.string.gender_selection_tari_image_desc),

                modifier = Modifier
                    .size(140.dp)
                    .padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                showTranslation = true
                            },
                            onTap = {
                                showTranslation = false
                                speakTari()
                            },
                            onPress = {
                                val released = tryAwaitRelease()
                                if (released) {
                                    showTranslation = false
                                }
                            }
                        )
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tariSpeech,
                        style = MaterialTheme.typography.h6,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    AnimatedVisibility(showTranslation) {
                        Text(
                            text = tariSpeechTranslation,
                            style = MaterialTheme.typography.body2,
                            color = Color(0xFFE0E0E0),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 12.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.gender_selection_translation_hint),

                        style = MaterialTheme.typography.caption,
                        color = Color(0xFFB0B0B0),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            GenderButton(
                text = stringResource(R.string.gender_selection_button_male),

                onClick = {
                    viewModel.saveUserGender(UserGender.MALE)
                    navController.navigate(targetRoute) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenderButton(
                text = stringResource(R.string.gender_selection_button_female),

                onClick = {
                    viewModel.saveUserGender(UserGender.FEMALE)
                    navController.navigate(targetRoute) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenderButton(
                text = stringResource(R.string.gender_selection_button_other),

                onClick = {
                    viewModel.saveUserGender(UserGender.OTHER)
                    navController.navigate(targetRoute) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                    }
                }
            )
        }
    }
}

@Composable
fun GenderButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFBB86FC))
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.button
        )
    }
}