package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.bisayaspeak.ai.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    billingManager: BillingManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isPremium by billingManager.isPremium.collectAsState()
    val products by billingManager.products.collectAsState()
    
    // デバッグログ
    LaunchedEffect(products) {
        android.util.Log.d("PremiumScreen", "Products loaded: ${products.size}")
        products.forEach { product ->
            android.util.Log.d("PremiumScreen", "Product: ${product.productId}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレミアムプラン") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B4513),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C1810),
                            Color(0xFF1A0F0A)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPremium) {
                // プレミアム会員の場合
                PremiumActiveCard()
            } else {
                // 非会員の場合
                PremiumBenefitsSection()
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // プラン選択
                Text(
                    text = "プランを選択",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (products.isEmpty()) {
                    // 商品が読み込まれていない場合
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3E2723)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "プランを読み込み中...",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Google Play Consoleで商品IDを設定してください",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    products.forEach { product ->
                        PremiumPlanCard(
                            product = product,
                            onPurchase = {
                                billingManager.launchPurchaseFlow(
                                    context as Activity,
                                    product
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 購入復元ボタン
                TextButton(
                    onClick = {
                        billingManager.restorePurchases { success ->
                            // 復元結果を表示
                        }
                    }
                ) {
                    Text(
                        text = "購入を復元",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumBenefitsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3E2723)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFFD700)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "プレミアム特典",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumBenefitItem(
                icon = Icons.Default.CheckCircle,
                title = "無制限の発音診断",
                description = "1日の回数制限なし"
            )
            
            PremiumBenefitItem(
                icon = Icons.Default.VolumeUp,
                title = "リスニング問題追加",
                description = "各レベル40問以上"
            )
            
            PremiumBenefitItem(
                icon = Icons.Default.Chat,
                title = "AI会話無制限",
                description = "回数制限なしで会話練習"
            )
            
            PremiumBenefitItem(
                icon = Icons.Default.Block,
                title = "広告なし",
                description = "快適な学習体験"
            )
            
            PremiumBenefitItem(
                icon = Icons.Default.Book,
                title = "追加フレーズ",
                description = "日常会話フレーズ100以上"
            )
        }
    }
}

@Composable
fun PremiumBenefitItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PremiumPlanCard(
    product: ProductDetails,
    onPurchase: () -> Unit
) {
    val offerDetails = product.subscriptionOfferDetails?.firstOrNull()
    val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
    val price = pricingPhase?.formattedPrice ?: ""
    val billingPeriod = pricingPhase?.billingPeriod ?: ""
    
    val isYearly = product.productId.contains("yearly")
    val planName = if (isYearly) "年間プラン" else "月間プラン"
    val savings = if (isYearly) "2ヶ月分お得！" else ""
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isYearly) Color(0xFFD2691E) else Color(0xFF5D4037)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = planName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (savings.isNotEmpty()) {
                        Text(
                            text = savings,
                            fontSize = 14.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = price,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "購入する",
                    color = if (isYearly) Color(0xFFD2691E) else Color(0xFF5D4037),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumActiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "プレミアム会員",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "すべての機能をご利用いただけます",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
