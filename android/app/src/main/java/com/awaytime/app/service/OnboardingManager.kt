package com.awaytime.app.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Manager for the onboarding flow
 */
class OnboardingManager(application: Application) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext

    var currentStep by mutableStateOf(OnboardingStep.WELCOME)
        private set

    var isCompleted by mutableStateOf(false)
        private set

    // Permission states
    var usageStatsPermissionStatus by mutableStateOf(PermissionStatus.NOT_DETERMINED)
        private set

    var accessibilityPermissionStatus by mutableStateOf(PermissionStatus.NOT_DETERMINED)
        private set

    var notificationPermissionStatus by mutableStateOf(PermissionStatus.NOT_DETERMINED)
        private set

    var overlayPermissionStatus by mutableStateOf(PermissionStatus.NOT_DETERMINED)
        private set

    // App selection
    var selectedApps by mutableStateOf<List<String>>(emptyList())
        private set

    // Goal setting
    var selectedGoal by mutableStateOf<GoalPreset?>(null)
        private set

    var customGoalMinutes by mutableStateOf(120)
        private set

    private val permissionService = PermissionService(context)
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    private val trialManager = PremiumTrialManager.getInstance(context)

    val presetGoals = listOf(
        GoalPreset(
            title = "Light User",
            minutes = 60,
            description = "Perfect for minimal usage",
            emoji = "🌱"
        ),
        GoalPreset(
            title = "Balanced",
            minutes = 120,
            description = "Healthy daily balance",
            emoji = "⚖️"
        ),
        GoalPreset(
            title = "Moderate",
            minutes = 180,
            description = "Room for work and leisure",
            emoji = "📚"
        ),
        GoalPreset(
            title = "Flexible",
            minutes = 240,
            description = "More time for productivity",
            emoji = "💼"
        )
    )

    init {
        checkPermissionStatuses()
    }

    // MARK: - Navigation

    val canSkip: Boolean
        get() = currentStep != OnboardingStep.PERMISSIONS && currentStep != OnboardingStep.COMPLETION

    val canGoBack: Boolean
        get() = currentStep != OnboardingStep.WELCOME

    val canGoNext: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME, OnboardingStep.FEATURES, OnboardingStep.PREMIUM_TRIAL -> true
            OnboardingStep.PERMISSIONS -> usageStatsPermissionStatus == PermissionStatus.GRANTED
            OnboardingStep.APP_SELECTION -> selectedApps.isNotEmpty()
            OnboardingStep.GOAL_SETTING -> selectedGoal != null
            OnboardingStep.COMPLETION -> true
        }

    val nextButtonTitle: String
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> "Get Started"
            OnboardingStep.FEATURES -> "Continue"
            OnboardingStep.PREMIUM_TRIAL -> if (trialManager.isTrialActive()) "Continue" else "Start Free Trial"
            OnboardingStep.PERMISSIONS -> if (usageStatsPermissionStatus == PermissionStatus.GRANTED) "Next" else "Grant Permissions"
            OnboardingStep.APP_SELECTION -> if (selectedApps.isEmpty()) "Select Apps" else "Set Goals"
            OnboardingStep.GOAL_SETTING -> if (selectedGoal != null) "Almost Done" else "Choose Goal"
            OnboardingStep.COMPLETION -> "Start Using Awaytime"
        }

    val hasUsageStatsPermission: Boolean
        get() = usageStatsPermissionStatus == PermissionStatus.GRANTED

    fun startOnboarding() {
        currentStep = OnboardingStep.WELCOME
        println("🚀 Onboarding started")
    }

    fun goNext() {
        when (currentStep) {
            OnboardingStep.WELCOME -> {
                currentStep = OnboardingStep.FEATURES
            }

            OnboardingStep.FEATURES -> {
                currentStep = OnboardingStep.PREMIUM_TRIAL
            }

            OnboardingStep.PREMIUM_TRIAL -> {
                // Activate trial if not already active
                if (!trialManager.isTrialActive() && !trialManager.hasUserEverHadTrial()) {
                    trialManager.activateTrial()
                }
                currentStep = OnboardingStep.PERMISSIONS
            }

            OnboardingStep.PERMISSIONS -> {
                if (usageStatsPermissionStatus == PermissionStatus.GRANTED) {
                    currentStep = OnboardingStep.APP_SELECTION
                } else {
                    requestUsageStatsPermission()
                }
            }

            OnboardingStep.APP_SELECTION -> {
                if (selectedApps.isNotEmpty()) {
                    currentStep = OnboardingStep.GOAL_SETTING
                }
            }

            OnboardingStep.GOAL_SETTING -> {
                if (selectedGoal != null) {
                    currentStep = OnboardingStep.COMPLETION
                }
            }

            OnboardingStep.COMPLETION -> {
                completeOnboarding()
            }
        }
    }

    fun goBack() {
        when (currentStep) {
            OnboardingStep.WELCOME -> { /* Can't go back */
            }

            OnboardingStep.FEATURES -> currentStep = OnboardingStep.WELCOME
            OnboardingStep.PREMIUM_TRIAL -> currentStep = OnboardingStep.FEATURES
            OnboardingStep.PERMISSIONS -> currentStep = OnboardingStep.PREMIUM_TRIAL
            OnboardingStep.APP_SELECTION -> currentStep = OnboardingStep.PERMISSIONS
            OnboardingStep.GOAL_SETTING -> currentStep = OnboardingStep.APP_SELECTION
            OnboardingStep.COMPLETION -> currentStep = OnboardingStep.GOAL_SETTING
        }
    }

    fun skipOnboarding() {
        setDefaultConfiguration()
        completeOnboarding()
    }

    fun completeOnboarding() {
        // Save onboarding completion
        prefs.edit().putBoolean("hasCompletedOnboarding", true).apply()

        // Save user selections
        saveOnboardingData()

        // Mark as completed
        isCompleted = true

        println("✅ Onboarding completed")
    }

    // MARK: - Permissions

    private fun checkPermissionStatuses() {
        viewModelScope.launch {
            usageStatsPermissionStatus = if (permissionService.hasUsageStatsPermission()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_DETERMINED
            }

            accessibilityPermissionStatus = if (permissionService.hasAccessibilityPermission()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_DETERMINED
            }

            notificationPermissionStatus = if (permissionService.hasNotificationPermission()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_DETERMINED
            }

            overlayPermissionStatus = if (permissionService.hasOverlayPermission()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_DETERMINED
            }
        }
    }

    fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            // Update status (user will need to manually grant)
            usageStatsPermissionStatus = PermissionStatus.NOT_DETERMINED

            println("📱 Usage Stats permission requested")
        } catch (e: Exception) {
            usageStatsPermissionStatus = PermissionStatus.DENIED
            println("❌ Failed to request Usage Stats permission: ${e.message}")
        }
    }

    fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            accessibilityPermissionStatus = PermissionStatus.NOT_DETERMINED

            println("♿ Accessibility permission requested")
        } catch (e: Exception) {
            accessibilityPermissionStatus = PermissionStatus.DENIED
            println("❌ Failed to request Accessibility permission: ${e.message}")
        }
    }

    fun requestNotificationPermission() {
        // For Android 13+, we would request notification permission
        // For now, we'll mark as granted since it's not strictly required
        notificationPermissionStatus = PermissionStatus.GRANTED

        println("🔔 Notification permission requested")
    }

    fun requestOverlayPermission() {
        try {
            permissionService.requestOverlayPermission()
            overlayPermissionStatus = PermissionStatus.NOT_DETERMINED
            println("🎨 Overlay permission requested")
        } catch (e: Exception) {
            overlayPermissionStatus = PermissionStatus.DENIED
            println("❌ Failed to request Overlay permission: ${e.message}")
        }
    }

    // MARK: - App Selection
    
    var isLoadingApps by mutableStateOf(false)
        private set

    // Navigation callback - to be set by the onboarding UI
    var onNavigateToAppSelection: (() -> Unit)? = null
    
    fun showAppSelection() {
        if (isLoadingApps) return // Prevent multiple concurrent loads
        
        // Navigate to the full app selection screen
        onNavigateToAppSelection?.invoke()
        
        println("🔄 Navigating to app selection screen...")
    }
    
    // This method can be called when returning from the app selection screen
    // to load the selected apps into the onboarding flow
    fun loadSelectedAppsFromService() {
        if (isLoadingApps) return
        
        viewModelScope.launch {
            try {
                isLoadingApps = true
                println("🔄 Loading selected apps from service...")
                
                // Get the saved selected apps from database (Mindful architecture)
                val repository = com.awaytime.app.data.repository.AwayTimeRepository(context)
                val appGroups = repository.getAllAppGroupsSync()
                val selectedAppNames = appGroups.flatMap { it.getSelectedApps() }
                
                if (selectedAppNames.isNotEmpty()) {
                    selectedApps = selectedAppNames
                    println("✅ Loaded ${selectedApps.size} selected apps: $selectedApps")
                } else {
                    // If no apps selected, use popular apps as fallback
                    val popularApps = listOf("Instagram", "Facebook", "YouTube")
                        .take(3) // Take top 3 popular apps as fallback
                    
                    selectedApps = popularApps
                    println("📱 Using fallback apps: ${selectedApps.size} apps")
                }
            } catch (e: Exception) {
                println("❌ Error loading apps: ${e.message}")
                // Fallback to default apps
                selectedApps = listOf("Social Media", "Entertainment", "Games")
            } finally {
                isLoadingApps = false
            }
        }
    }

    fun addSelectedApp(appName: String) {
        selectedApps = selectedApps + appName
    }

    fun removeSelectedApp(appName: String) {
        selectedApps = selectedApps - appName
    }

    // MARK: - Goal Setting

    fun selectGoal(goal: GoalPreset) {
        selectedGoal = goal
        println("🎯 Goal selected: ${goal.title} (${goal.minutes} min)")
    }

    fun selectCustomGoal() {
        selectedGoal = GoalPreset(
            title = "Custom",
            minutes = customGoalMinutes,
            description = "Your personalized limit",
            emoji = "🎨"
        )
        println("🎯 Custom goal selected: $customGoalMinutes min")
    }

    fun increaseCustomGoal() {
        customGoalMinutes = minOf(customGoalMinutes + 30, 480)
    }

    fun decreaseCustomGoal() {
        customGoalMinutes = maxOf(customGoalMinutes - 30, 30)
    }
    
    // MARK: - Premium Trial Integration
    
    /**
     * Get trial manager for accessing trial information
     */
    fun getTrialManager(): PremiumTrialManager {
        return trialManager
    }
    
    /**
     * Start premium trial manually (for users who want to start it)
     */
    fun startPremiumTrial(): Boolean {
        return if (!trialManager.hasUserEverHadTrial()) {
            trialManager.activateTrial()
            println("🎁 Premium trial started during onboarding!")
            true
        } else {
            false
        }
    }
    
    /**
     * Skip trial activation (user chooses not to start trial)
     */
    fun skipTrial() {
        println("⏭️ User skipped premium trial activation")
        // Continue to next step without activating trial
        currentStep = OnboardingStep.PERMISSIONS
    }
    
    /**
     * Check if trial should be promoted to user
     */
    fun shouldPromoteTrial(): Boolean {
        return !trialManager.hasUserEverHadTrial() && !trialManager.isTrialActive()
    }
    
    /**
     * Get trial promotion message
     */
    fun getTrialPromotionMessage(): String {
        return when {
            trialManager.isTrialActive() -> {
                val daysLeft = trialManager.getDaysRemaining()
                "Your premium trial is active! $daysLeft days remaining to explore all features."
            }
            shouldPromoteTrial() -> {
                "Start your 7-day free premium trial and unlock all features including multiple app groups, advanced analytics, and more!"
            }
            else -> {
                "Premium features available with subscription."
            }
        }
    }

    // MARK: - Data Management

    private fun setDefaultConfiguration() {
        // Set default goal if none selected
        if (selectedGoal == null) {
            selectGoal(presetGoals[1]) // Balanced (2 hours) default
        }

        // Set default apps if none selected
        if (selectedApps.isEmpty()) {
            selectedApps = listOf("Social Media", "Entertainment")
        }

        println("⚙️ Default configuration set")
    }

    private fun saveOnboardingData() {
        with(prefs.edit()) {
            // Save selected apps
            putStringSet("selectedApps", selectedApps.toSet())

            // Save goal
            selectedGoal?.let { goal ->
                putInt("dailyGoalMinutes", goal.minutes)
                putString("dailyGoalTitle", goal.title)
            }

            // Save completion date
            putLong("onboardingCompletedDate", System.currentTimeMillis())

            apply()
        }

        println("💾 Onboarding data saved")
    }

    // MARK: - Static Methods

    companion object {
        fun hasCompletedOnboarding(context: Context): Boolean {
            val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            return prefs.getBoolean("hasCompletedOnboarding", false)
        }

        fun resetOnboarding(context: Context) {
            val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                remove("hasCompletedOnboarding")
                remove("selectedApps")
                remove("dailyGoalMinutes")
                remove("dailyGoalTitle")
                remove("onboardingCompletedDate")
                apply()
            }

            println("🔄 Onboarding reset")
        }

        fun getSelectedApps(context: Context): Set<String> {
            val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            return prefs.getStringSet("selectedApps", emptySet()) ?: emptySet()
        }

        fun getDailyGoalMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            return prefs.getInt("dailyGoalMinutes", 120) // Default 2 hours
        }
    }

    // MARK: - Analytics

    fun trackOnboardingStep(step: OnboardingStep) {
        // In production, this would send to analytics
        println("📊 Onboarding step: $step")
    }

    fun trackOnboardingCompletion(duration: Long) {
        // In production, this would send to analytics
        println("📊 Onboarding completed in ${duration}ms")
    }

    fun trackOnboardingSkipped(at: OnboardingStep) {
        // In production, this would send to analytics
        println("📊 Onboarding skipped at: $at")
    }
}

// MARK: - Data Classes and Enums

enum class OnboardingStep {
    WELCOME,
    FEATURES,
    PREMIUM_TRIAL,
    PERMISSIONS,
    APP_SELECTION,
    GOAL_SETTING,
    COMPLETION
}

data class GoalPreset(
    val title: String,
    val minutes: Int,
    val description: String,
    val emoji: String
)

// MARK: - Onboarding Coordinator

class OnboardingCoordinator(private val context: Context) {
    var showOnboarding by mutableStateOf(false)
        private set

    init {
        checkOnboardingStatus()
    }

    fun checkOnboardingStatus() {
        showOnboarding = !OnboardingManager.hasCompletedOnboarding(context)
    }

    fun onboardingCompleted() {
        showOnboarding = false
    }

    fun resetAndShowOnboarding() {
        OnboardingManager.resetOnboarding(context)
        showOnboarding = true
    }
}

// MARK: - Permission Status Enum for Onboarding
// Using PermissionStatus from PermissionService.kt

// MARK: - Extension Functions

fun Context.shouldShowOnboarding(): Boolean {
    return !OnboardingManager.hasCompletedOnboarding(this)
}

fun Context.getOnboardingData(): OnboardingData {
    val prefs = getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    return OnboardingData(
        selectedApps = prefs.getStringSet("selectedApps", emptySet())?.toList() ?: emptyList(),
        dailyGoalMinutes = prefs.getInt("dailyGoalMinutes", 120),
        dailyGoalTitle = prefs.getString("dailyGoalTitle", "Balanced") ?: "Balanced",
        completedDate = prefs.getLong("onboardingCompletedDate", 0L)
    )
}

data class OnboardingData(
    val selectedApps: List<String>,
    val dailyGoalMinutes: Int,
    val dailyGoalTitle: String,
    val completedDate: Long
)