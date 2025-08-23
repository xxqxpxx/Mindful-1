package com.awaytime.app.service

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PremiumFeatureManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: PremiumFeatureManager? = null
        
        fun getInstance(context: Context): PremiumFeatureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PremiumFeatureManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)
    private val trialManager = PremiumTrialManager.getInstance(context)
    
    private val _isPremiumActive = MutableStateFlow(calculatePremiumStatus())
    val isPremiumActive: StateFlow<Boolean> = _isPremiumActive.asStateFlow()
    
    // Initialize trial monitoring
    init {
        // Monitor trial status changes
        kotlinx.coroutines.GlobalScope.launch {
            trialManager.trialStatus.collect { 
                updatePremiumStatus()
            }
        }
    }
    
    private fun calculatePremiumStatus(): Boolean {
        val hasSubscription = prefs.getBoolean("is_premium", false)
        val hasActiveTrial = trialManager.isTrialActive()
        return hasSubscription || hasActiveTrial
    }
    
    private fun updatePremiumStatus() {
        _isPremiumActive.value = calculatePremiumStatus()
    }
    
    fun canUseFeature(feature: PremiumFeature): Boolean {
        val hasSubscription = prefs.getBoolean("is_premium", false)
        val hasActiveTrial = trialManager.isTrialActive()
        return hasSubscription || hasActiveTrial
    }
    
    fun requestFeatureAccess(feature: PremiumFeature) {
        // Check if user has active trial first
        if (trialManager.isTrialActive()) {
            println("Feature ${feature.displayName} is available during your trial")
            return
        }
        
        // Show appropriate message based on trial status
        when {
            trialManager.shouldShowTrialExpiredMessage() -> {
                println("Your trial has expired. Upgrade to premium to use ${feature.displayName}")
            }
            !trialManager.hasUserEverHadTrial() -> {
                println("Start your 7-day free trial to use ${feature.displayName}")
            }
            else -> {
                println("Feature ${feature.displayName} requires premium subscription")
            }
        }
    }
    
    fun getPremiumBenefits(): List<PremiumBenefit> {
        return listOf(
            PremiumBenefit(
                feature = PremiumFeature.MULTIPLE_APP_GROUPS,
                title = "Multiple App Groups",
                description = "Create and manage multiple app groups for better organization",
                icon = "folder_plus",
                color = Color(0xFF2196F3)
            ),
            PremiumBenefit(
                feature = PremiumFeature.ADVANCED_ANALYTICS,
                title = "Advanced Analytics",
                description = "Get detailed insights into your usage patterns",
                icon = "analytics",
                color = Color(0xFF4CAF50)
            ),
            PremiumBenefit(
                feature = PremiumFeature.CUSTOM_THEMES,
                title = "Custom Themes",
                description = "Personalize your app with custom themes and colors",
                icon = "palette",
                color = Color(0xFF9C27B0)
            ),
            PremiumBenefit(
                feature = PremiumFeature.EXPORT_DATA,
                title = "Export Data",
                description = "Export your usage data for external analysis",
                icon = "download",
                color = Color(0xFFFF9800)
            ),
            PremiumBenefit(
                feature = PremiumFeature.FOCUS_SESSIONS,
                title = "Focus Sessions",
                description = "Dedicated focus time with enhanced blocking",
                icon = "timer",
                color = Color(0xFFE91E63)
            ),
            PremiumBenefit(
                feature = PremiumFeature.SMART_APP_CATEGORIZATION,
                title = "Smart Categorization",
                description = "AI-powered automatic app categorization",
                icon = "auto_awesome",
                color = Color(0xFF00BCD4)
            )
        )
    }
    
    fun getPremiumBenefitsList(): List<String> {
        return listOf(
            "Multiple App Groups",
            "Advanced Analytics", 
            "Custom Themes",
            "Export Data",
            "Focus Sessions",
            "Smart Categorization",
            "Scheduled Limits",
            "Cloud Sync"
        )
    }
    
    fun setPremiumStatus(isPremium: Boolean) {
        _isPremiumActive.value = isPremium
        prefs.edit().putBoolean("is_premium", isPremium).apply()
    }
    
    /**
     * Get premium status as a Compose state
     */
    @Composable
    fun rememberIsPremiumActive(): State<Boolean> {
        return isPremiumActive.collectAsState()
    }
    
    /**
     * Check if a feature requires premium subscription
     */
    fun requiresPremium(feature: PremiumFeature): Boolean {
        return !canUseFeature(feature)
    }
    
    /**
     * Get feature access as a Compose state
     */
    @Composable
    fun rememberCanUseFeature(feature: PremiumFeature): State<Boolean> {
        val isPremium by rememberIsPremiumActive()
        return remember(isPremium, feature) {
            derivedStateOf { canUseFeature(feature) }
        }
    }
    
    /**
     * Track premium feature usage for analytics
     */
    fun trackFeatureUsage(feature: PremiumFeature) {
        if (canUseFeature(feature)) {
            // Track if user is using feature during trial
            if (trialManager.isTrialActive()) {
                trialManager.trackFeatureUsageDuringTrial(feature)
            }
            // Analytics tracking would go here
            println("🎯 Premium feature used: ${feature.displayName}")
        }
    }
    
    /**
     * Get user-friendly feature limitation message
     */
    fun getFeatureLimitationMessage(feature: PremiumFeature): String {
        val baseMessage = when (feature) {
            PremiumFeature.MULTIPLE_APP_GROUPS -> 
                "Create unlimited app groups with Premium. Currently limited to 1 group."
            PremiumFeature.ADVANCED_ANALYTICS -> 
                "View detailed analytics and insights with Premium."
            PremiumFeature.SCHEDULED_LIMITS -> 
                "Set custom schedules for different times with Premium."
            PremiumFeature.FOCUS_SESSIONS -> 
                "Use Pomodoro-style focus sessions with Premium."
            PremiumFeature.EXPORT_DATA -> 
                "Export your usage data with Premium."
            PremiumFeature.CUSTOM_THEMES -> 
                "Customize your app appearance with Premium."
            PremiumFeature.CLOUD_SYNC -> 
                "Sync your data across devices with Premium."
            else -> 
                "This feature requires Premium subscription."
        }
        
        // Add trial-specific messaging
        return when {
            trialManager.isTrialActive() -> {
                val daysLeft = trialManager.getDaysRemaining()
                "$baseMessage (Available in your trial - $daysLeft days left)"
            }
            !trialManager.hasUserEverHadTrial() -> {
                "$baseMessage Start your 7-day free trial to try it now!"
            }
            trialManager.shouldShowTrialExpiredMessage() -> {
                "$baseMessage Your trial has expired - upgrade to continue using premium features."
            }
            else -> baseMessage
        }
    }
    
    /**
     * Enforce premium feature access with automatic paywall
     */
    fun enforceFeatureAccess(
        feature: PremiumFeature,
        onAccessGranted: () -> Unit,
        onShowPaywall: (PremiumFeature) -> Unit
    ) {
        if (canUseFeature(feature)) {
            onAccessGranted()
        } else {
            onShowPaywall(feature)
        }
    }
    
    // MARK: - Trial Integration Methods
    
    /**
     * Get trial manager instance
     */
    fun getTrialManager(): PremiumTrialManager {
        return trialManager
    }
    
    /**
     * Check if user has premium access (subscription or trial)
     */
    fun hasPremiumAccess(): Boolean {
        return canUseFeature(PremiumFeature.MULTIPLE_APP_GROUPS) // Use any premium feature as test
    }
    
    /**
     * Get premium status with trial information
     */
    fun getPremiumStatusWithTrial(): PremiumStatusInfo {
        val hasSubscription = prefs.getBoolean("is_premium", false)
        val isTrialActive = trialManager.isTrialActive()
        
        return PremiumStatusInfo(
            hasSubscription = hasSubscription,
            hasActiveTrial = isTrialActive,
            trialDaysRemaining = if (isTrialActive) trialManager.getDaysRemaining() else 0,
            trialStatus = trialManager.trialStatus.value,
            canAccessPremium = hasSubscription || isTrialActive
        )
    }
    
    /**
     * Start trial if eligible
     */
    fun startTrialIfEligible(): Boolean {
        return if (!trialManager.hasUserEverHadTrial()) {
            trialManager.activateTrial()
            true
        } else {
            false
        }
    }
}

data class PremiumBenefit(
    val feature: PremiumFeature,
    val title: String,
    val description: String,
    val icon: String,
    val color: Color
)

data class PremiumStatusInfo(
    val hasSubscription: Boolean,
    val hasActiveTrial: Boolean,
    val trialDaysRemaining: Int,
    val trialStatus: TrialStatus,
    val canAccessPremium: Boolean
)

