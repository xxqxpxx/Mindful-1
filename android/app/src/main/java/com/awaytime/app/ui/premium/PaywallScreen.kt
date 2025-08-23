package com.awaytime.app.ui.premium

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.ProductDetails
import com.awaytime.app.service.*
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    triggeredByFeature: PremiumFeature? = null,
    onDismiss: () -> Unit,
    subscriptionService: SubscriptionService = run {
        val context = LocalContext.current
        remember { SubscriptionService(context) }
    }
) {
    val context = LocalContext.current
    var selectedProduct by remember { mutableStateOf<ProductDetails?>(null) }
    var showingPurchaseConfirmation by remember { mutableStateOf(false) }
    var purchaseSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Pre-select yearly product
    LaunchedEffect(subscriptionService.availableProducts) {
        if (selectedProduct == null && subscriptionService.availableProducts.isNotEmpty()) {
            selectedProduct = subscriptionService.getYearlyProduct()
        }
    }

    // Listen for purchase results
    LaunchedEffect(Unit) {
        subscriptionService.purchaseResultFlow.collect { result ->
            when (result) {
                is SubscriptionService.PurchaseResult.Success -> {
                    purchaseSuccess = true
                }

                is SubscriptionService.PurchaseResult.Error -> {
                    subscriptionService.errorMessage = result.message
                }

                else -> {
                    // Handle other cases if needed
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AwayTimeColors.primary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            subscriptionService.restorePurchases()
                        }
                    ) {
                        Text(
                            text = "Restore",
                            color = AwayTimeColors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AwayTimeColors.primary.copy(alpha = 0.1f),
                            AwayTimeColors.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    HeaderSection(triggeredByFeature = triggeredByFeature)
                }

                // Features
                item {
                    FeaturesSection(triggeredByFeature = triggeredByFeature)
                }

                // Pricing
                item {
                    PricingSection(
                        subscriptionService = subscriptionService,
                        selectedProduct = selectedProduct,
                        onProductSelected = { selectedProduct = it }
                    )
                }

                // Purchase button
                item {
                    PurchaseSection(
                        subscriptionService = subscriptionService,
                        selectedProduct = selectedProduct,
                        onPurchase = { product ->
                            val activity = context as? Activity
                            if (activity != null) {
                                // Launch actual purchase flow
                                coroutineScope.launch {
                                    try {
                                        val result = subscriptionService.purchase(activity, product)
                                        when (result) {
                                            is SubscriptionService.PurchaseResult.Success -> {
                                                purchaseSuccess = true
                                            }
                                            is SubscriptionService.PurchaseResult.Error -> {
                                                subscriptionService.errorMessage = result.message
                                            }
                                            is SubscriptionService.PurchaseResult.UserCancelled -> {
                                                // User cancelled, no action needed
                                            }
                                            is SubscriptionService.PurchaseResult.Pending -> {
                                                // Purchase is pending, show appropriate message
                                                subscriptionService.errorMessage = "Purchase is pending. Please check back later."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        subscriptionService.errorMessage = "Purchase failed: ${e.message}"
                                    }
                                }
                            }
                        }
                    )
                }

                // Footer
                item {
                    FooterSection()
                }
            }
        }
    }

    // Success dialog
    if (purchaseSuccess) {
        AlertDialog(
            onDismissRequest = { purchaseSuccess = false },
            title = {
                Text("Purchase Successful! 🎉")
            },
            text = {
                Text("Welcome to Awaytime Premium! All features are now unlocked.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseSuccess = false
                        onDismiss()
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }

    // Error dialog
    subscriptionService.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { subscriptionService.clearError() },
            title = {
                Text("Error")
            },
            text = {
                Text(error)
            },
            confirmButton = {
                TextButton(
                    onClick = { subscriptionService.clearError() }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(triggeredByFeature: PremiumFeature?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App icon with premium badge
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AwayTimeColors.primary,
                                AwayTimeColors.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Premium badge
            Box(
                modifier = Modifier
                    .offset(x = 25.dp, y = (-25).dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Gold
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Unlock Awaytime Premium",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (triggeredByFeature != null) {
                    "Get access to ${triggeredByFeature.displayName} and more premium features"
                } else {
                    "Take control of your digital wellness with advanced features"
                },
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeaturesSection(triggeredByFeature: PremiumFeature?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Premium Features",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(280.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                listOf(
                    PremiumFeature.MULTIPLE_APP_GROUPS,
                    PremiumFeature.ADVANCED_ANALYTICS,
                    PremiumFeature.CUSTOM_THEMES,
                    PremiumFeature.EXPORT_DATA
                )
            ) { feature ->
                PremiumFeatureCard(
                    feature = feature,
                    isHighlighted = feature == triggeredByFeature
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    feature: PremiumFeature,
    isHighlighted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                AwayTimeColors.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isHighlighted) {
            androidx.compose.foundation.BorderStroke(2.dp, AwayTimeColors.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = getFeatureIcon(feature),
                contentDescription = null,
                tint = if (isHighlighted) AwayTimeColors.primary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = feature.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = feature.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun PricingSection(
    subscriptionService: SubscriptionService,
    selectedProduct: ProductDetails?,
    onProductSelected: (ProductDetails) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose Your Plan",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            subscriptionService.getYearlyProduct()?.let { yearlyProduct ->
                PricingOptionCard(
                    product = yearlyProduct,
                    isSelected = selectedProduct?.productId == yearlyProduct.productId,
                    savings = subscriptionService.getYearlySavings(),
                    onSelect = { onProductSelected(yearlyProduct) }
                )
            }

            subscriptionService.getMonthlyProduct()?.let { monthlyProduct ->
                PricingOptionCard(
                    product = monthlyProduct,
                    isSelected = selectedProduct?.productId == monthlyProduct.productId,
                    savings = null,
                    onSelect = { onProductSelected(monthlyProduct) }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun PricingOptionCard(
    product: ProductDetails,
    isSelected: Boolean,
    savings: String?,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AwayTimeColors.primary else MaterialTheme.colorScheme.outline.copy(
                alpha = 0.5f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    savings?.let { savingsText ->
                        Surface(
                            color = Color.Green,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Save $savingsText",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${product.localizedPrice} per ${product.subscriptionPeriod}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (product.isYearly) {
                    getMonthlyEquivalent(product)?.let { monthlyEquivalent ->
                        Text(
                            text = "Just $monthlyEquivalent per month",
                            fontSize = 12.sp,
                            color = AwayTimeColors.primary
                        )
                    }
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) AwayTimeColors.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PurchaseSection(
    subscriptionService: SubscriptionService,
    selectedProduct: ProductDetails?,
    onPurchase: (ProductDetails) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                selectedProduct?.let { product ->
                    onPurchase(product)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedProduct != null && !subscriptionService.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = AwayTimeColors.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (subscriptionService.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = getPurchaseButtonText(subscriptionService, selectedProduct),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Free trial info
        selectedProduct?.let { product ->
            val trialInfo = getTrialInfo(product)
            if (trialInfo.isNotEmpty()) {
                Text(
                    text = trialInfo,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FooterSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "• Cancel anytime in Play Store",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "• Subscription automatically renews unless cancelled",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextButton(
                onClick = {
                    // Open terms of service
                }
            ) {
                Text(
                    text = "Terms of Service",
                    fontSize = 12.sp,
                    color = AwayTimeColors.primary
                )
            }

            TextButton(
                onClick = {
                    // Open privacy policy
                }
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 12.sp,
                    color = AwayTimeColors.primary
                )
            }
        }
    }
}

// Helper functions
private fun getFeatureIcon(feature: PremiumFeature): ImageVector {
    return when (feature) {
        PremiumFeature.MULTIPLE_APP_GROUPS -> Icons.Default.FolderOpen
        PremiumFeature.ADVANCED_ANALYTICS -> Icons.Default.Analytics
        PremiumFeature.CUSTOM_THEMES -> Icons.Default.Palette
        PremiumFeature.EXPORT_DATA -> Icons.Default.Download
        else -> Icons.Default.Star
    }
}

private fun getPurchaseButtonText(
    subscriptionService: SubscriptionService,
    selectedProduct: ProductDetails?
): String {
    return when {
        selectedProduct == null -> "Select a Plan"
        subscriptionService.isLoading -> "Processing..."
        else -> "Start Premium - ${selectedProduct.localizedPrice}/${selectedProduct.subscriptionPeriod}"
    }
}

private fun getTrialInfo(product: ProductDetails): String {
    // Check if product has a free trial
    val offerDetails = product.subscriptionOfferDetails?.firstOrNull()
    val pricingPhases = offerDetails?.pricingPhases?.pricingPhaseList

    // Look for a free trial phase (price = 0)
    val trialPhase = pricingPhases?.find { it.priceAmountMicros == 0L }

    return if (trialPhase != null) {
        val period = trialPhase.billingPeriod
        val trialLength = when {
            period.contains("P7D") -> "7-day"
            period.contains("P14D") -> "14-day"
            period.contains("P1M") -> "1-month"
            else -> "trial"
        }
        "Start your $trialLength free trial"
    } else {
        ""
    }
}

private fun getMonthlyEquivalent(product: ProductDetails): String? {
    if (!product.isYearly) return null

    val yearlyPrice =
        product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
    if (yearlyPrice == null) return null

    val monthlyPrice = yearlyPrice / 12
    val formattedPrice =
        product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

    // This is a simplified calculation - in a real app you'd want proper currency formatting
    return formattedPrice?.let { price ->
        val currency = price.replace(Regex("[0-9.,]"), "")
        val monthlyAmount = (yearlyPrice / 12.0 / 1_000_000).toString().take(4)
        "$currency$monthlyAmount"
    }
}

// MARK: - Premium Feature Gate

@ExperimentalMaterial3Api
@Composable
fun PremiumFeatureGate(
    feature: PremiumFeature,
    content: @Composable () -> Unit,
    fallbackContent: @Composable () -> Unit
) {
    val subscriptionService = SubscriptionManager.getService()

    if (subscriptionService?.canUseFeature(feature) == true) {
        content()
    } else {
        fallbackContent()
    }
}