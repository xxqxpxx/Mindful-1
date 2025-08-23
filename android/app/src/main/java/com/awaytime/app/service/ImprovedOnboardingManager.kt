package com.awaytime.app.service

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Improved onboarding manager with better permission handling
 */
class ImprovedOnboardingManager(application: Application) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    private val permissionService = PermissionService(context)
    private val prefs = context.getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)

    var currentStep by mutableStateOf(OnboardingStep.WELCOME)
        private set

    var isCompleted by mutableStateOf(false)
        private set

    // App selection
    var selectedApps by mutableStateOf<List<String>>(emptyList())
        private set

    // Goal setting
    var selectedGoal by mutableStateOf<GoalPreset?>(null)
        private set

    var customGoalMinutes by mutableStateOf(120)
        private set

    // Permission states - observed from permission service
    val usageStatsPermissionStatus: PermissionStatus
        get() = permissionService.usageStatsPermissionStatus

    val accessibilityPermissionStatus: PermissionStatus
        get() = permissionService.accessibilityPermissionStatus

    val notificationPermissionStatus: PermissionStatus
        get() = if (permissionService.hasNotificationPermission()) PermissionStatus.GRANTED else PermissionStatus.DENIED

    // Simplified onboarding steps - permissions are requested contextually
    val simplifiedSteps = listOf(
        OnboardingStep.WELCOME,
        OnboardingStep.FEATURES,
        OnboardingStep.APP_SELECTION,
        OnboardingStep.GOAL_SETTING,
        OnboardingStep.COMPLETION
    )

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
        
        // Set up permission status change listener
        permissionService.onPermissionStatusChanged = { 
            // Trigger recomposition when permissions change
            checkPermissionStatuses()
        }
    }

    // MARK: - Navigation

    val canSkip: Boolean
        get() = currentStep != OnboardingStep.COMPLETION

    val canGoBack: Boolean
        get() = currentStep != OnboardingStep.WELCOME

    val canGoNext: Boolean
        get() = when (currentStep) {
            OnboardingStep.WELCOME, OnboardingStep.FEATURES -> true
            OnboardingStep.APP_SELECTION -> selectedApps.isNotEmpty()
            OnboardingStep.GOAL_SETTING -> selectedGoal != null
            OnboardingStep.COMPLETION -> true
            else -> true
        }

    val nextButtonTitle: String
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> "Get Started"
            OnboardingStep.FEATURES -> "Continue"
            OnboardingStep.APP_SELECTION -> if (selectedApps.isEmpty()) "Select Apps" else "Set Goals"
            OnboardingStep.GOAL_SETTING -> if (selectedGoal != null) "Almost Done" else "Choose Goal"
            OnboardingStep.COMPLETION -> "Start Using Awaytime"
            else -> "Next"
        }

    fun startOnboarding() {
        currentStep = OnboardingStep.WELCOME
        println("🚀 Improved onboarding started")
    }

    fun goNext() {
        val currentIndex = simplifiedSteps.indexOf(currentStep)
        if (currentIndex < simplifiedSteps.size - 1) {
            currentStep = simplifiedSteps[currentIndex + 1]
        } else if (currentStep == OnboardingStep.COMPLETION) {
            completeOnboarding()
        }
    }

    fun goBack() {
        val currentIndex = simplifiedSteps.indexOf(currentStep)
        if (currentIndex > 0) {
            currentStep = simplifiedSteps[currentIndex - 1]
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

        println("✅ Improved onboarding completed")
    }

    // MARK: - Permission Management

    private fun checkPermissionStatuses() {
        viewModelScope.launch {
            permissionService.updatePermissionStatuses()
        }
    }

    // These methods will be called from UI when user is ready for permissions
    fun requestUsageStatsPermissionWhenReady(onRequest: () -> Unit) {
        if (!permissionService.hasUsageStatsPermission()) {
            onRequest()
        }
    }

    fun requestAccessibilityPermissionWhenReady(onRequest: () -> Unit) {
        if (!permissionService.hasAccessibilityPermission()) {
            onRequest()
        }
    }

    fun requestNotificationPermissionWhenReady(onRequest: () -> Unit) {
        if (!permissionService.hasNotificationPermission()) {
            onRequest()
        }
    }

    // MARK: - App Selection
    
    var isLoadingApps by mutableStateOf(false)
        private set

    // Navigation callback - to be set by the onboarding UI
    var onNavigateToAppSelection: (() -> Unit)? = null
    
    fun showAppSelection() {
        if (isLoadingApps) {
            println("⚠️ FIXED: Already loading apps, preventing multiple navigation")
            return
        }
        
        try {
            println("🔄 FIXED: Triggering navigation to app selection")
            onNavigateToAppSelection?.invoke()
            println("✅ FIXED: App selection navigation callback executed")
        } catch (e: Exception) {
            println("❌ FIXED: Error in app selection navigation: ${e.message}")
        }
    }
    
    // FIXED: Load selected apps with better error handling and timeout
    fun loadSelectedAppsFromService() {
        if (isLoadingApps) {
            println("⚠️ FIXED: Already loading apps, skipping")
            return
        }
        
        viewModelScope.launch {
            try {
                isLoadingApps = true
                println("🔄 FIXED: Loading selected apps safely...")
                
                // Load with timeout protection
                kotlinx.coroutines.withTimeout(5000L) {
                    // Using clean Mindful architecture - get from database
                    val repository = com.awaytime.app.data.repository.AwayTimeRepository(context)
                    val appGroups = repository.getAllAppGroupsSync()
                    val selectedAppNames = appGroups.flatMap { it.getSelectedApps() }
                    
                    if (selectedAppNames.isNotEmpty()) {
                        selectedApps = selectedAppNames
                        println("✅ FIXED: Loaded ${selectedApps.size} selected apps")
                    } else {
                        // Quick fallback without expensive loading
                        selectedApps = listOf("Instagram", "YouTube", "TikTok", "Facebook", "Twitter")
                        println("📱 FIXED: Using quick fallback apps")
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                println("⚠️ FIXED: App loading timed out, using defaults")
                selectedApps = listOf("Social Media", "Entertainment", "Games")
            } catch (e: Exception) {
                println("❌ FIXED: Error loading apps: ${e.message}")
                selectedApps = listOf("Social Media", "Entertainment", "Games")
            } finally {
                isLoadingApps = false
                println("✅ FIXED: App loading completed")
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

    // MARK: - Permission Status Helpers

    val hasBasicPermissions: Boolean
        get() = permissionService.hasUsageStatsPermission()

    val hasAllPermissions: Boolean
        get() = permissionService.hasUsageStatsPermission() && 
                permissionService.hasAccessibilityPermission() &&
                permissionService.hasNotificationPermission()

    val canTrackUsage: Boolean
        get() = permissionService.hasUsageStatsPermission()

    val canBlockApps: Boolean
        get() = permissionService.hasAccessibilityPermission()

    val canSendNotifications: Boolean
        get() = permissionService.hasNotificationPermission()

    // MARK: - Static Methods

    companion object {
        fun hasCompletedOnboarding(context: Context): Boolean {
            val prefs = context.getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)
            return prefs.getBoolean("hasCompletedOnboarding", false)
        }

        fun resetOnboarding(context: Context) {
            val prefs = context.getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                remove("hasCompletedOnboarding")
                remove("selectedApps")
                remove("dailyGoalMinutes")
                remove("dailyGoalTitle")
                remove("onboardingCompletedDate")
                apply()
            }

            println("🔄 Improved onboarding reset")
        }

        fun getSelectedApps(context: Context): Set<String> {
            val prefs = context.getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)
            return prefs.getStringSet("selectedApps", emptySet()) ?: emptySet()
        }

        fun getDailyGoalMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)
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

    fun trackPermissionRequested(permission: String, context: String) {
        // In production, this would send to analytics
        println("📊 Permission requested: $permission in context: $context")
    }

    fun trackPermissionGranted(permission: String) {
        // In production, this would send to analytics
        println("📊 Permission granted: $permission")
    }

    fun trackPermissionDenied(permission: String) {
        // In production, this would send to analytics
        println("📊 Permission denied: $permission")
    }
}

// MARK: - Improved Onboarding Coordinator

class ImprovedOnboardingCoordinator(private val context: Context) {
    var showOnboarding by mutableStateOf(false)
        private set

    init {
        checkOnboardingStatus()
    }

    fun checkOnboardingStatus() {
        showOnboarding = !ImprovedOnboardingManager.hasCompletedOnboarding(context)
    }

    fun onboardingCompleted() {
        showOnboarding = false
    }

    fun resetAndShowOnboarding() {
        ImprovedOnboardingManager.resetOnboarding(context)
        showOnboarding = true
    }
}

// MARK: - Extension Functions

fun Context.shouldShowImprovedOnboarding(): Boolean {
    return !ImprovedOnboardingManager.hasCompletedOnboarding(this)
}

fun Context.getImprovedOnboardingData(): OnboardingData {
    val prefs = getSharedPreferences("onboarding_v2", Context.MODE_PRIVATE)
    return OnboardingData(
        selectedApps = prefs.getStringSet("selectedApps", emptySet())?.toList() ?: emptyList(),
        dailyGoalMinutes = prefs.getInt("dailyGoalMinutes", 120),
        dailyGoalTitle = prefs.getString("dailyGoalTitle", "Balanced") ?: "Balanced",
        completedDate = prefs.getLong("onboardingCompletedDate", 0L)
    )
}