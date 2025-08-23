@file:OptIn(ExperimentalCoroutinesApi::class)

package com.awaytime.app.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.resume

enum class SubscriptionStatus {
    Unknown,
    NotSubscribed,
    Subscribed
}

class SubscriptionService(
    private val context: Context
) : PurchasesUpdatedListener,
    BillingClientStateListener {

    companion object {
        private const val TAG = "SubscriptionService"
    }
    
    // Trial integration
    private val trialManager = PremiumTrialManager.getInstance(context)

    // State properties with backing fields
    private var _subscriptionStatus = SubscriptionStatus.Unknown
    val subscriptionStatus: SubscriptionStatus
        get() = _subscriptionStatus

    private var _availableProducts = emptyList<ProductDetails>()
    val availableProducts: List<ProductDetails>
        get() = _availableProducts

    private var _isLoading = false
    val isLoading: Boolean
        get() = _isLoading

    private var _errorMessage: String? = null
    var errorMessage: String? = null
        get() = _errorMessage

    // Callback for state changes
    var onSubscriptionStateChanged: ((SubscriptionService) -> Unit)? = null

    // Coroutine scope for this service
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Purchase result flow
    private val _purchaseResultFlow = MutableSharedFlow<PurchaseResult>()
    val purchaseResultFlow: SharedFlow<PurchaseResult> = _purchaseResultFlow.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val productIds = listOf(
        "com.awaytime.premium.monthly",
        "com.awaytime.premium.yearly"
    )

    // Event flows for UI integration
    // Purchase result callbacks (replacing flows)
    var onPurchaseResult: ((PurchaseResult) -> Unit)? = null

    sealed class PurchaseResult {
        object Success : PurchaseResult()
        object UserCancelled : PurchaseResult()
        object Pending : PurchaseResult()
        data class Error(val message: String) : PurchaseResult()
    }

    init {
        startConnection()
    }
    
    fun disconnect() {
        billingClient.endConnection()
        serviceScope.cancel()
    }

    // MARK: - Billing Client Setup

    private fun startConnection() {
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            println("✅ Billing client connected")
            loadProducts()
            checkSubscriptionStatus()
        } else {
            _errorMessage = "Failed to connect to billing service: ${billingResult.debugMessage}"
            println("❌ Billing setup failed: ${billingResult.debugMessage}")
        }
    }

    override fun onBillingServiceDisconnected() {
        println("⚠️ Billing service disconnected")
        // Try to reconnect
        serviceScope.launch {
            delay(1000)
            startConnection()
        }
    }

    // MARK: - Product Loading

    private fun loadProducts() {
        if (!billingClient.isReady) {
            _errorMessage = "Billing client not ready"
            return
        }

        _isLoading = true

        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Sort products by price (monthly first, then yearly)
                _availableProducts = productDetailsList.sortedWith { product1, product2 ->
                    when {
                        product1.productId.contains("monthly") && product2.productId.contains("yearly") -> -1
                        product1.productId.contains("yearly") && product2.productId.contains("monthly") -> 1
                        else -> {
                            val price1 =
                                product1.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                                    ?: 0
                            val price2 =
                                product2.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                                    ?: 0
                            price1.compareTo(price2)
                        }
                    }
                }

                println("✅ Loaded ${_availableProducts.size} products")
            } else {
                _errorMessage = "Failed to load products: ${billingResult.debugMessage}"
                println("❌ Failed to load products: ${billingResult.debugMessage}")
            }

            _isLoading = false
            onSubscriptionStateChanged?.invoke(this)
        }
    }

    // MARK: - Purchase Handling

    suspend fun purchase(activity: Activity, productDetails: ProductDetails): PurchaseResult {
        if (!billingClient.isReady) {
            return PurchaseResult.Error("Billing client not ready")
        }

        _isLoading = true
        _errorMessage = null

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            _isLoading = false
            return PurchaseResult.Error("No offer available for this product")
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // Wait for purchase result in onPurchasesUpdated
                suspendCancellableCoroutine { continuation ->
                    val job = serviceScope.launch {
                        purchaseResultFlow.collect { result ->
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    }
                    continuation.invokeOnCancellation { job.cancel() }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _isLoading = false
                PurchaseResult.UserCancelled
            }

            else -> {
                _isLoading = false
                PurchaseResult.Error("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
                serviceScope.launch {
                    _purchaseResultFlow.emit(PurchaseResult.Success)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                serviceScope.launch {
                    _purchaseResultFlow.emit(PurchaseResult.UserCancelled)
                }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // User already owns this item
                checkSubscriptionStatus()
                serviceScope.launch {
                    _purchaseResultFlow.emit(PurchaseResult.Success)
                }
            }

            else -> {
                serviceScope.launch {
                    _purchaseResultFlow.emit(
                        PurchaseResult.Error("Purchase failed: ${billingResult.debugMessage}")
                    )
                }
            }
        }

        _isLoading = false
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't been acknowledged yet
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        println("✅ Purchase acknowledged")
                        checkSubscriptionStatus()
                    } else {
                        println("❌ Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                checkSubscriptionStatus()
            }
        }
    }

    // MARK: - Subscription Status

    private fun checkSubscriptionStatus() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchasesList.find { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            productIds.contains(purchase.products.firstOrNull())
                }

                if (activePurchase != null) {
                    // Find the product details for this purchase
                    val productDetails = _availableProducts.find { product ->
                        activePurchase.products.contains(product.productId)
                    }

                    if (productDetails != null) {
                        _subscriptionStatus = SubscriptionStatus.Subscribed
                    } else {
                        _subscriptionStatus = SubscriptionStatus.NotSubscribed
                    }
                } else {
                    _subscriptionStatus = SubscriptionStatus.NotSubscribed
                }

                println("📱 Subscription status updated: ${_subscriptionStatus}")
            } else {
                println("❌ Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    // MARK: - Restore Purchases

    fun restorePurchases() {
        _isLoading = true
        _errorMessage = null

        checkSubscriptionStatus()

        serviceScope.launch {
            delay(1000) // Give time for the check to complete
            _isLoading = false
        }
    }

    // MARK: - Premium Feature Checks

    val isPremiumActive: Boolean
        get() = _subscriptionStatus == SubscriptionStatus.Subscribed || trialManager.isTrialActive()

    fun canUseFeature(feature: PremiumFeature): Boolean {
        val hasSubscription = _subscriptionStatus == SubscriptionStatus.Subscribed
        val hasActiveTrial = trialManager.isTrialActive()
        
        return when (feature) {
            // Premium Features - Require Subscription or Active Trial
            PremiumFeature.MULTIPLE_APP_GROUPS,
            PremiumFeature.SMART_APP_CATEGORIZATION,
            PremiumFeature.SCHEDULED_LIMITS,
            PremiumFeature.ADVANCED_ANALYTICS,
            PremiumFeature.AI_INSIGHTS,
            PremiumFeature.WEEKLY_REPORTS,
            PremiumFeature.FOCUS_SESSIONS,
            PremiumFeature.ADVANCED_GOALS,
            PremiumFeature.BREAK_REMINDERS,
            PremiumFeature.WEBSITE_BLOCKING,
            PremiumFeature.EMERGENCY_OVERRIDE,
            PremiumFeature.CUSTOM_THEMES,
            PremiumFeature.ADVANCED_NOTIFICATIONS,
            PremiumFeature.EXPORT_DATA,
            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.FAMILY_SHARING -> hasSubscription || hasActiveTrial

            // Free Features - Always Available
            PremiumFeature.BASIC_USAGE_TRACKING,
            PremiumFeature.SINGLE_APP_GROUP,
            PremiumFeature.BASIC_NOTIFICATIONS -> true
        }
    }

    fun requiresPremium(feature: PremiumFeature): Boolean {
        return !canUseFeature(feature)
    }

    // MARK: - Product Information

    fun getMonthlyProduct(): ProductDetails? {
        return _availableProducts.find { it.productId.contains("monthly") }
    }

    fun getYearlyProduct(): ProductDetails? {
        return _availableProducts.find { it.productId.contains("yearly") }
    }

    fun getYearlySavings(): String? {
        val monthly = getMonthlyProduct()
        val yearly = getYearlyProduct()

        if (monthly == null || yearly == null) return null

        val monthlyPrice =
            monthly.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                ?: return null
        val yearlyPrice =
            yearly.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                ?: return null

        val monthlyYearlyPrice = monthlyPrice * 12
        val savings = monthlyYearlyPrice - yearlyPrice
        val percentage = (savings.toDouble() / monthlyYearlyPrice.toDouble()) * 100

        return String.format("%.0f%%", percentage)
    }

    // MARK: - Error Handling

    fun clearError() {
        _errorMessage = null
    }
    
    // MARK: - Trial Integration
    
    /**
     * Get trial manager instance
     */
    fun getTrialManager(): PremiumTrialManager {
        return trialManager
    }
    
    /**
     * Check if user has any form of premium access
     */
    fun hasAnyPremiumAccess(): Boolean {
        return _subscriptionStatus == SubscriptionStatus.Subscribed || trialManager.isTrialActive()
    }
    
    /**
     * Get comprehensive premium status including trial info
     */
    fun getPremiumStatusInfo(): PremiumAccessInfo {
        val hasSubscription = _subscriptionStatus == SubscriptionStatus.Subscribed
        val hasActiveTrial = trialManager.isTrialActive()
        
        return PremiumAccessInfo(
            hasSubscription = hasSubscription,
            subscriptionStatus = _subscriptionStatus,
            hasActiveTrial = hasActiveTrial,
            trialStatus = trialManager.trialStatus.value,
            trialDaysRemaining = if (hasActiveTrial) trialManager.getDaysRemaining() else 0,
            hasAnyPremiumAccess = hasSubscription || hasActiveTrial,
            shouldShowTrialPromo = !hasSubscription && !trialManager.hasUserEverHadTrial(),
            shouldShowUpgradePrompt = trialManager.shouldShowUpgradePrompt() && !hasSubscription
        )
    }
    
    /**
     * Handle trial expiration - called when trial expires
     */
    fun handleTrialExpiration() {
        // Update subscription status check
        checkSubscriptionStatus()
        
        // Track trial expiration for analytics
        trialManager.trackTrialExpiration()
        
        println("🔔 Trial expired - prompting for subscription")
    }
}



// MARK: - Product Extensions

val ProductDetails.localizedPrice: String
    get() {
        val pricingPhase =
            subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
        return pricingPhase?.formattedPrice ?: "N/A"
    }

val ProductDetails.subscriptionPeriod: String
    get() {
        val pricingPhase =
            subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
        val period = pricingPhase?.billingPeriod ?: return ""

        return when {
            period.contains("P1M") -> "month"
            period.contains("P1Y") -> "year"
            period.contains("P1W") -> "week"
            period.contains("P1D") -> "day"
            else -> "period"
        }
    }

val ProductDetails.isMonthly: Boolean
    get() = productId.contains("monthly")

val ProductDetails.isYearly: Boolean
    get() = productId.contains("yearly")

// MARK: - Global Subscription Manager

object SubscriptionManager {
    private var subscriptionService: SubscriptionService? = null

    fun initialize(context: Context) {
        subscriptionService = SubscriptionService(context)
    }

    fun getService(): SubscriptionService? {
        return subscriptionService
    }

    fun isPremiumActive(): Boolean {
        return subscriptionService?.isPremiumActive ?: false
    }

    fun canUseFeature(feature: PremiumFeature): Boolean {
        return subscriptionService?.canUseFeature(feature) ?: false
    }

    fun requiresPremium(feature: PremiumFeature): Boolean {
        return subscriptionService?.requiresPremium(feature) ?: true
    }
}

// MARK: - Data Classes

data class PremiumAccessInfo(
    val hasSubscription: Boolean,
    val subscriptionStatus: SubscriptionStatus,
    val hasActiveTrial: Boolean,
    val trialStatus: TrialStatus,
    val trialDaysRemaining: Int,
    val hasAnyPremiumAccess: Boolean,
    val shouldShowTrialPromo: Boolean,
    val shouldShowUpgradePrompt: Boolean
)