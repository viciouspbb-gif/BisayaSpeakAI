package com.bisayaspeak.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ui.viewmodel.PracticeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeCategoryScreen(
    onNavigateBack: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    isPremium: Boolean = false,
    viewModel: PracticeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groupedItems by viewModel.groupedItems.collectAsState()
    var showProDialog by remember { mutableStateOf(false) }
    
    // カテゴリとPremium情報をペアで保持
    val categoryInfo = remember(groupedItems) {
        groupedItems.map { (category, items) ->
            category to items.any { it.isPremium }
        }
    }
    
    // Proアップグレードダイアログ
    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            title = { Text(stringResource(R.string.unlock_pro_btn)) },
            text = { Text(stringResource(R.string.locked_toast_pro)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProDialog = false
                        onNavigateToUpgrade()
                    }
                ) {
                    Text(stringResource(R.string.upgrade_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProDialog = false }) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pronunciation",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "戻る",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categoryInfo) { (category, isPremiumCategory) ->
                CategoryCard(
                    category = category,
                    isPremiumCategory = isPremiumCategory,
                    isUserPremium = isPremium,
                    onClick = {
                        if (!isPremiumCategory || isPremium) {
                            onCategorySelected(category)
                        } else {
                            // ロックされたカテゴリをタップ
                            Toast.makeText(
                                context,
                                context.getString(R.string.locked_toast_pro),
                                Toast.LENGTH_SHORT
                            ).show()
                            showProDialog = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    isPremiumCategory: Boolean,
    isUserPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ロック状態による色変更
    val isLocked = isPremiumCategory && !isUserPremium
    val containerColor = if (isLocked) Color(0xFFB0B0B0) else Color(0xFFE7E0EC)
    val contentColor = if (isLocked) Color.White else Color.Black
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Category,
                    contentDescription = category,
                    tint = contentColor,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}
