package com.bisayaspeak.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisayaspeak.ai.data.model.RolePlayGenre
import com.bisayaspeak.ai.data.repository.RolePlayRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePlayGenreScreen(
    onNavigateBack: () -> Unit = {},
    onGenreSelected: (RolePlayGenre) -> Unit = {}
) {
    val repository = remember { RolePlayRepository() }
    val genres = remember { repository.getAllGenres() }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF5F6F7)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("実践ロールプレイ") },
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
                text = "ジャンルを選択",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "AIとロールプレイで実践的な会話力を身につけましょう",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            genres.forEach { genre ->
                GenreCard(
                    genre = genre,
                    icon = getGenreIcon(genre.id),
                    onClick = { onGenreSelected(genre) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GenreCard(
    genre: RolePlayGenre,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(60.dp)
                .background(genre.accentColor, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(18.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = genre.accentColor,
            modifier = Modifier.size(36.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = genre.titleJa,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Text(
                text = genre.description,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "${genre.scenes.size}シーン",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

fun getGenreIcon(genreId: String): ImageVector {
    return when (genreId) {
        "travel" -> Icons.Outlined.Flight
        "daily_life" -> Icons.Outlined.Home
        "shopping" -> Icons.Outlined.ShoppingCart
        "dining" -> Icons.Outlined.Restaurant
        "dating" -> Icons.Outlined.Favorite
        "friends" -> Icons.Outlined.People
        "hotel" -> Icons.Outlined.Hotel
        "trouble" -> Icons.Outlined.Warning
        "business" -> Icons.Outlined.Work
        else -> Icons.Outlined.Home
    }
}
