package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisayaspeak.ai.data.UserGender
import com.bisayaspeak.ai.ui.navigation.AppRoute
import com.bisayaspeak.ai.ui.viewmodel.GenderSelectionViewModel

@Composable
fun GenderSelectionScreen(
    navController: NavController,
    viewModel: GenderSelectionViewModel
) {
    val targetRoute = remember {
        AppRoute.RolePlayChat.route.replace("{scenarioId}", "tari_infinite_mode")
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
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Tari",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp),
                tint = Color.White
            )

            Text(
                text = "Maayong buntag! Lalaki ba ka? Babae? O sekreto?",
                style = MaterialTheme.typography.h5,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "(おはよう！あなたは男性？女性？それともヒミツ？)",
                style = MaterialTheme.typography.body1,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            GenderButton(
                text = "Lalaki (Male)",
                onClick = {
                    viewModel.saveUserGender(UserGender.MALE)
                    navController.navigate(targetRoute) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenderButton(
                text = "Babae (Female)",
                onClick = {
                    viewModel.saveUserGender(UserGender.FEMALE)
                    navController.navigate(targetRoute) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenderButton(
                text = "Sekreto (Other)",
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