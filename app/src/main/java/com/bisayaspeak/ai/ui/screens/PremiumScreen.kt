package com.bisayaspeak.ai.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.billing.BillingManager
import com.bisayaspeak.ai.billing.recurringPriceLabel
import com.bisayaspeak.ai.billing.trialDuration

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
                    text = stringResource(id = R.string.upgrade_plan_section_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (products.isEmpty()) {
                    LoadingCard(onRetry = { billingManager.reloadProducts() })
                } else {
                    products.forEach { product ->
                        PremiumPlanCard(
                            product = product,
                            onPurchase = {
                                val basePlanId = BillingManager.basePlanIdFor(product.productId)
                                val offerTag = if (product.productId.contains("yearly")) {
                                    BillingManager.YEARLY_TRIAL_TAG
                                } else {
                                    BillingManager.MONTHLY_TRIAL_TAG
                                }
                                billingManager.launchPurchaseFlow(
                                    context as Activity,
                                    product,
                                    basePlanId,
                                    offerTag
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
private fun PremiumPlanCard(
    product: ProductDetails,
    onPurchase: () -> Unit
) {
    val basePlanId = BillingManager.basePlanIdFor(product.productId)
    val isYearly = product.productId.contains("yearly")
    val planName = if (isYearly) stringResource(id = R.string.upgrade_plan_yearly_title) else stringResource(id = R.string.upgrade_plan_monthly_title)
    val savings = if (isYearly) stringResource(id = R.string.upgrade_plan_badge_savings) else ""
    val offerTag = if (isYearly) BillingManager.YEARLY_TRIAL_TAG else BillingManager.MONTHLY_TRIAL_TAG
    val price = product.recurringPriceLabel(basePlanId, offerTag)
    val trial = product.trialDuration(basePlanId, offerTag)?.let { duration ->
        stringResource(id = R.string.upgrade_plan_trial_prefix, duration)
    }

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

                price?.let {
                    Text(
                        text = it,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            trial?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = Color.White.copy(alpha = 0.9f))
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
private fun LoadingCard(onRetry: () -> Unit) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFFFFD700))
            Text(
                text = stringResource(id = R.string.upgrade_loading_message),
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(id = R.string.upgrade_loading_retry_message),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(id = R.string.upgrade_reload_button))
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
