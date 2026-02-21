package com.bisayaspeak.ai.billing

import com.android.billingclient.api.ProductDetails

/**
 * 共通のサブスク Offer 取得ロジック
 */
fun ProductDetails.findOffer(
    basePlanId: String?,
    offerTag: String? = null
): ProductDetails.SubscriptionOfferDetails? {
    val offers = subscriptionOfferDetails.orEmpty()
    if (offers.isEmpty()) return null

    val candidates = basePlanId?.let { id ->
        offers.filter { it.basePlanId == id }.takeIf { it.isNotEmpty() }
    } ?: offers

    if (candidates.isEmpty()) return null

    return offerTag?.let { tag ->
        candidates.firstOrNull { it.offerTags.contains(tag) }
    } ?: candidates.firstOrNull()
}

fun ProductDetails.findRecurringPhase(
    basePlanId: String?,
    offerTag: String? = null
): ProductDetails.PricingPhase? {
    val offer = findOffer(basePlanId, offerTag)
    return offer?.pricingPhases?.pricingPhaseList?.lastOrNull { it.priceAmountMicros > 0 }
}

fun ProductDetails.findTrialPhase(
    basePlanId: String?,
    offerTag: String? = null
): ProductDetails.PricingPhase? {
    val offer = findOffer(basePlanId, offerTag)
    return offer?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros == 0L }
}

fun ProductDetails.recurringPriceLabel(
    basePlanId: String?,
    offerTag: String? = null
): String? = findRecurringPhase(basePlanId, offerTag)?.formattedPrice

fun ProductDetails.trialDuration(
    basePlanId: String?,
    offerTag: String? = null
): String? = findTrialPhase(basePlanId, offerTag)?.billingPeriod?.let(::formatBillingPeriod)

fun formatBillingPeriod(period: String): String = when {
    period.contains("P7D", ignoreCase = true) -> "7日間"
    period.contains("P1W", ignoreCase = true) -> "1週間"
    period.contains("P1M", ignoreCase = true) -> "1か月"
    period.contains("P1Y", ignoreCase = true) -> "1年間"
    else -> period
}
