package com.bisayaspeak.ai.ui.upgrade

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.ProductDetails
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.billing.BillingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpgradeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val showPurchaseSuccess by viewModel.showPurchaseSuccess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val products by viewModel.products.collectAsState()

    val monthlyPlan = remember(products) {
        products.find { it.productId == BillingManager.PREMIUM_AI_MONTHLY_SKU }
    }
    val yearlyPlan = remember(products) {
        products.find { it.productId == BillingManager.PREMIUM_AI_YEARLY_SKU }
    }
    
    // 購入成功メッセージ表示
    LaunchedEffect(showPurchaseSuccess) {
        showPurchaseSuccess?.let { productName ->
            val message = context.getString(R.string.upgrade_purchase_success_message, productName)
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearPurchaseSuccessMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.upgrade_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PaywallHeader()

            if (monthlyPlan != null && activity != null) {
                HeroCtaButton(onClick = { viewModel.purchasePremiumAIMonthly(activity) })
            }

            SubscriptionPlanSection(
                monthlyPlan = monthlyPlan,
                yearlyPlan = yearlyPlan,
                activity = activity,
                onMonthly = { viewModel.purchasePremiumAIMonthly(it) },
                onYearly = { viewModel.purchasePremiumAIYearly(it) }
            )

            RestoreNote(
                onRestore = { viewModel.restorePurchases() }
            )
        }
    }
}

@Composable
private fun PaywallHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.upgrade_paywall_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.upgrade_paywall_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.upgrade_paywall_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeroCtaButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = stringResource(R.string.upgrade_cta_try_free),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SubscriptionPlanSection(
    monthlyPlan: ProductDetails?,
    yearlyPlan: ProductDetails?,
    activity: Activity?,
    onMonthly: (Activity) -> Unit,
    onYearly: (Activity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.upgrade_plan_section_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (monthlyPlan == null && yearlyPlan == null) {
            LoadingCard()
            return
        }

        monthlyPlan?.let { plan ->
            SubscriptionPlanCard(
                title = stringResource(R.string.upgrade_plan_monthly_title),
                priceLabel = plan.priceLabel(BillingManager.MONTHLY_TRIAL_TAG),
                trialLabel = plan.trialLabel(BillingManager.MONTHLY_TRIAL_TAG)
                    ?: stringResource(R.string.upgrade_plan_trial_default),
                badge = stringResource(R.string.upgrade_plan_badge_popular),
                highlight = true,
                onClick = { activity?.let(onMonthly) }
            )
        }

        yearlyPlan?.let { plan ->
            SubscriptionPlanCard(
                title = stringResource(R.string.upgrade_plan_yearly_title),
                priceLabel = plan.priceLabel(BillingManager.YEARLY_TRIAL_TAG),
                trialLabel = plan.trialLabel(BillingManager.YEARLY_TRIAL_TAG)
                    ?: stringResource(R.string.upgrade_plan_trial_default),
                badge = stringResource(R.string.upgrade_plan_badge_savings),
                highlight = false,
                onClick = { activity?.let(onYearly) }
            )
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    title: String,
    priceLabel: String?,
    trialLabel: String,
    badge: String,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val bgColors = if (highlight) listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)) else listOf(Color(0xFF78350F), Color(0xFFDC2626))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(bgColors))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    priceLabel?.let {
                        Text(text = it, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
                Text(
                    text = badge,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(text = trialLabel, color = Color.White.copy(alpha = 0.9f))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = stringResource(R.string.upgrade_plan_button_text),
                    color = if (highlight) Color(0xFF1E40AF) else Color(0xFFB45309),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(stringResource(R.string.upgrade_loading_message), color = Color.White)
        }
    }
}

@Composable
private fun RestoreNote(onRestore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.upgrade_restore_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.upgrade_restore_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRestore) {
                Text(stringResource(R.string.upgrade_restore_button))
            }
        }
    }
}

private fun ProductDetails.priceLabel(offerTag: String): String? {
    val offer = subscriptionOfferDetails?.firstOrNull { it.offerTags.contains(offerTag) }
        ?: subscriptionOfferDetails?.firstOrNull()
    val recurringPhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull { it.priceAmountMicros > 0 }
    return recurringPhase?.formattedPrice
}

private fun ProductDetails.trialLabel(offerTag: String): String? {
    val offer = subscriptionOfferDetails?.firstOrNull { it.offerTags.contains(offerTag) }
        ?: subscriptionOfferDetails?.firstOrNull()
    val trialPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros == 0L }
    return trialPhase?.let { phase ->
        val duration = formatPeriod(phase.billingPeriod)
        "最初の${duration}は無料"
    }
}

private fun formatPeriod(period: String): String {
    return when {
        period.contains("P7D") -> "7日間"
        period.contains("P1M") -> "1か月"
        period.contains("P1Y") -> "1年間"
        period.contains("P1W") -> "1週間"
        else -> period
    }
}
