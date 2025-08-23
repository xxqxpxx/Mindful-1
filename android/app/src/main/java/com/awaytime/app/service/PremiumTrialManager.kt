package com.awaytime.app.service

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Manages the 7-day premium trial for new users
 */
class PremiumTrialManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: PremiumTrialManager? = null
        
        fun getInstance(context: Context): PremiumTrialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PremiumTrialManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        const val TRIAL_DURATION_DAYS = 7
        private const val PREFS_NAME = "premium_trial_prefs"
        private const val KEY_TRIAL_START_TIME = "trial_start_time"
        private const val KEY_TRIAL_ACTIVATED = "trial_activated"
        private const val KEY_TRIAL_ENDED = "trial_ended"
        private const val KEY_NEW_USER_DETECTED = "new_user_detected"
        private const val KEY_FIRST_APP_LAUNCH = "first_app_launch"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val onboardingPrefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    
    // Trial state flows
    private val _trialStatus = MutableStateFlow(getTrialStatus())
    val trialStatus: StateFlow<TrialStatus> = _trialStatus.asStateFlow()
    
    private val _daysRemaining = MutableStateFlow(getDaysRemaining())
    val daysRemaining: StateFlow<Int> = _daysRemaining.asStateFlow()
    
    private val _hoursRemaining = MutableStateFlow(getHoursRemaining())
    val hoursRemaining: StateFlow<Int> = _hoursRemaining.asStateFlow()
    
    init {
        detectAndActivateTrialForNewUser()
        updateTrialState()
    }
    
    // MARK: - Trial Status Detection
    
    private fun detectAndActivateTrialForNewUser() {
        val isFirstLaunch = !prefs.getBoolean(KEY_FIRST_APP_LAUNCH, false)
        val hasCompletedOnboarding = onboardingPrefs.getBoolean("hasCompletedOnboarding", false)
        val trialAlreadyActivated = prefs.getBoolean(KEY_TRIAL_ACTIVATED, false)
        
        if (isFirstLaunch) {
            markFirstLaunch()
        }
        
        // Activate trial for new users who haven't had a trial yet
        if (isNewUser() && !trialAlreadyActivated) {
            activateTrial()
            println("🎁 Premium trial activated for new user!")
        }
    }
    
    private fun isNewUser(): Boolean {
        // A user is considered new if:
        // 1. This is their first app launch, OR
        // 2. They haven't completed onboarding yet
        val isFirstLaunch = !prefs.getBoolean(KEY_FIRST_APP_LAUNCH, false)
        val hasCompletedOnboarding = onboardingPrefs.getBoolean("hasCompletedOnboarding", false)
        
        return isFirstLaunch || !hasCompletedOnboarding
    }
    
    private fun markFirstLaunch() {
        prefs.edit()
            .putBoolean(KEY_FIRST_APP_LAUNCH, true)
            .putLong("first_launch_timestamp", System.currentTimeMillis())
            .apply()
        println("📱 First app launch detected and marked")
    }
    
    // MARK: - Trial Activation and Management
    
    fun activateTrial() {
        if (!isTrialActivated()) {
            val currentTime = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_TRIAL_START_TIME, currentTime)
                .putBoolean(KEY_TRIAL_ACTIVATED, true)
                .putBoolean(KEY_TRIAL_ENDED, false)
                .apply()
            
            updateTrialState()
            println("✅ Premium trial activated! 7 days of premium features unlocked.")
        }
    }
    
    fun endTrial() {
        prefs.edit()
            .putBoolean(KEY_TRIAL_ENDED, true)
            .apply()
        
        updateTrialState()
        println("⏰ Premium trial has ended")
    }
    
    // MARK: - Trial State Queries
    
    fun isTrialActivated(): Boolean {
        return prefs.getBoolean(KEY_TRIAL_ACTIVATED, false)
    }
    
    fun isTrialActive(): Boolean {
        if (!isTrialActivated() || isTrialEnded()) {
            return false
        }
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (startTime == 0L) return false
        
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - startTime)
        
        return elapsedDays < TRIAL_DURATION_DAYS
    }
    
    fun isTrialExpired(): Boolean {
        if (!isTrialActivated()) return false
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (startTime == 0L) return false
        
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - startTime)
        
        return elapsedDays >= TRIAL_DURATION_DAYS
    }
    
    fun isTrialEnded(): Boolean {
        return prefs.getBoolean(KEY_TRIAL_ENDED, false)
    }
    
    fun hasUserEverHadTrial(): Boolean {
        return isTrialActivated()
    }
    
    // MARK: - Trial Time Calculations
    
    fun getDaysRemaining(): Int {
        if (!isTrialActive()) return 0
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (startTime == 0L) return 0
        
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - startTime)
        
        return maxOf(0, TRIAL_DURATION_DAYS - elapsedDays.toInt())
    }
    
    fun getHoursRemaining(): Int {
        if (!isTrialActive()) return 0
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (startTime == 0L) return 0
        
        val currentTime = System.currentTimeMillis()
        val elapsedHours = TimeUnit.MILLISECONDS.toHours(currentTime - startTime)
        val totalTrialHours = TRIAL_DURATION_DAYS * 24
        
        return maxOf(0, totalTrialHours - elapsedHours.toInt())
    }
    
    fun getTrialStartDate(): Date? {
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        return if (startTime > 0) Date(startTime) else null
    }
    
    fun getTrialEndDate(): Date? {
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        return if (startTime > 0) {
            Date(startTime + TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS.toLong()))
        } else null
    }
    
    fun getTrialProgress(): Float {
        if (!isTrialActivated()) return 0f
        
        val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
        if (startTime == 0L) return 0f
        
        val currentTime = System.currentTimeMillis()
        val totalTrialTime = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS.toLong())
        val elapsedTime = currentTime - startTime
        
        return minOf(1f, elapsedTime.toFloat() / totalTrialTime.toFloat())
    }
    
    // MARK: - Trial Status Management
    
    private fun getTrialStatus(): TrialStatus {
        return when {
            !isTrialActivated() -> TrialStatus.NOT_STARTED
            isTrialEnded() -> TrialStatus.ENDED
            isTrialExpired() -> TrialStatus.EXPIRED
            isTrialActive() -> TrialStatus.ACTIVE
            else -> TrialStatus.NOT_STARTED
        }
    }
    
    private fun updateTrialState() {
        val newStatus = getTrialStatus()
        val newDaysRemaining = getDaysRemaining()
        val newHoursRemaining = getHoursRemaining()
        
        // Auto-end trial if it's expired but not marked as ended
        if (newStatus == TrialStatus.EXPIRED && !isTrialEnded()) {
            endTrial()
        }
        
        _trialStatus.value = newStatus
        _daysRemaining.value = newDaysRemaining
        _hoursRemaining.value = newHoursRemaining
    }
    
    // MARK: - Premium Feature Integration
    
    fun canAccessPremiumFeatures(): Boolean {
        return isTrialActive()
    }
    
    fun shouldShowTrialBadge(): Boolean {
        return isTrialActive()
    }
    
    fun shouldShowTrialExpiredMessage(): Boolean {
        return isTrialExpired() || isTrialEnded()
    }
    
    fun shouldShowUpgradePrompt(): Boolean {
        return shouldShowTrialExpiredMessage()
    }
    
    // MARK: - User-Friendly Messages
    
    fun getTrialStatusMessage(): String {
        return when (trialStatus.value) {
            TrialStatus.NOT_STARTED -> "Premium trial not started"
            TrialStatus.ACTIVE -> {
                val days = daysRemaining.value
                val hours = hoursRemaining.value % 24
                when {
                    days > 1 -> "$days days left in your premium trial"
                    days == 1 -> "1 day left in your premium trial"
                    hours > 1 -> "$hours hours left in your premium trial"
                    hours == 1 -> "1 hour left in your premium trial"
                    else -> "Less than an hour left in your premium trial"
                }
            }
            TrialStatus.EXPIRED -> "Your 7-day premium trial has expired"
            TrialStatus.ENDED -> "Premium trial has ended"
        }
    }
    
    fun getTrialCallToAction(): String {
        return when (trialStatus.value) {
            TrialStatus.NOT_STARTED -> "Start your free 7-day premium trial"
            TrialStatus.ACTIVE -> "Enjoying premium? Upgrade to keep these features"
            TrialStatus.EXPIRED, TrialStatus.ENDED -> "Upgrade to premium to restore these features"
        }
    }
    
    // MARK: - Analytics and Tracking
    
    fun trackTrialActivation() {
        // In production, send to analytics
        println("📊 Trial activated - New user: ${isNewUser()}")
    }
    
    fun trackTrialExpiration() {
        // In production, send to analytics
        println("📊 Trial expired - Duration: $TRIAL_DURATION_DAYS days")
    }
    
    fun trackFeatureUsageDuringTrial(feature: PremiumFeature) {
        if (isTrialActive()) {
            // In production, send to analytics
            println("📊 Premium feature used during trial: ${feature.displayName}")
        }
    }
    
    // MARK: - Development and Testing
    
    fun resetTrial() {
        prefs.edit()
            .remove(KEY_TRIAL_START_TIME)
            .remove(KEY_TRIAL_ACTIVATED)
            .remove(KEY_TRIAL_ENDED)
            .remove(KEY_NEW_USER_DETECTED)
            .remove(KEY_FIRST_APP_LAUNCH)
            .apply()
        
        updateTrialState()
        println("🔄 Trial reset for testing")
    }
    
    fun extendTrial(additionalDays: Int) {
        if (isTrialActivated()) {
            val startTime = prefs.getLong(KEY_TRIAL_START_TIME, 0L)
            val extensionMillis = TimeUnit.DAYS.toMillis(additionalDays.toLong())
            
            prefs.edit()
                .putLong(KEY_TRIAL_START_TIME, startTime - extensionMillis)
                .apply()
            
            updateTrialState()
            println("⏰ Trial extended by $additionalDays days")
        }
    }
    
    fun getTrialDebugInfo(): String {
        val startDate = getTrialStartDate()
        val endDate = getTrialEndDate()
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        return buildString {
            appendLine("=== TRIAL DEBUG INFO ===")
            appendLine("Status: ${trialStatus.value}")
            appendLine("Activated: ${isTrialActivated()}")
            appendLine("Active: ${isTrialActive()}")
            appendLine("Expired: ${isTrialExpired()}")
            appendLine("Ended: ${isTrialEnded()}")
            appendLine("New User: ${isNewUser()}")
            appendLine("Days Remaining: ${daysRemaining.value}")
            appendLine("Hours Remaining: ${hoursRemaining.value}")
            appendLine("Progress: ${(getTrialProgress() * 100).toInt()}%")
            if (startDate != null) {
                appendLine("Start Date: ${formatter.format(startDate)}")
            }
            if (endDate != null) {
                appendLine("End Date: ${formatter.format(endDate)}")
            }
            appendLine("========================")
        }
    }
    
    // MARK: - Compose Integration
    
    @Composable
    fun rememberTrialStatus(): State<TrialStatus> {
        return trialStatus.collectAsState()
    }
    
    @Composable
    fun rememberDaysRemaining(): State<Int> {
        return daysRemaining.collectAsState()
    }
    
    @Composable
    fun rememberIsTrialActive(): State<Boolean> {
        val status by rememberTrialStatus()
        return remember(status) {
            derivedStateOf { status == TrialStatus.ACTIVE }
        }
    }
    
    @Composable
    fun rememberCanAccessPremium(): State<Boolean> {
        val isTrialActive by rememberIsTrialActive()
        return remember(isTrialActive) {
            derivedStateOf { isTrialActive }
        }
    }
}

// MARK: - Trial Status Enum

enum class TrialStatus {
    NOT_STARTED,
    ACTIVE, 
    EXPIRED,
    ENDED
}

// MARK: - Trial Extensions

fun Context.getTrialManager(): PremiumTrialManager {
    return PremiumTrialManager.getInstance(this)
}

fun Context.isTrialActive(): Boolean {
    return getTrialManager().isTrialActive()
}

fun Context.canAccessPremiumFeatures(): Boolean {
    return getTrialManager().canAccessPremiumFeatures()
}