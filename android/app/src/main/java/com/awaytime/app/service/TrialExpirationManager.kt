package com.awaytime.app.service

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.TimeUnit

/**
 * Manages trial expiration handling, notifications, and graceful degradation
 */
class TrialExpirationManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: TrialExpirationManager? = null
        
        fun getInstance(context: Context): TrialExpirationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrialExpirationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val TRIAL_CHECK_WORK_NAME = "trial_expiration_check"
        private const val EXPIRATION_NOTIFICATION_WORK_NAME = "trial_expiration_notification"
    }
    
    private val trialManager = PremiumTrialManager.getInstance(context)
    private val notificationService = NotificationService(context)
    private val premiumFeatureManager = PremiumFeatureManager.getInstance(context)
    
    // Event flows for UI updates
    private val _trialExpiredFlow = MutableSharedFlow<TrialExpirationEvent>()
    val trialExpiredFlow: SharedFlow<TrialExpirationEvent> = _trialExpiredFlow.asSharedFlow()
    
    private val _featureAccessRevokedFlow = MutableSharedFlow<PremiumFeature>()
    val featureAccessRevokedFlow: SharedFlow<PremiumFeature> = _featureAccessRevokedFlow.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        scheduleTrialExpirationChecks()
        monitorTrialStatus()
    }
    
    // MARK: - Trial Monitoring
    
    private fun monitorTrialStatus() {
        scope.launch {
            trialManager.trialStatus.collect { status ->
                when (status) {
                    TrialStatus.EXPIRED -> handleTrialExpiration()
                    TrialStatus.ACTIVE -> scheduleExpirationReminders()
                    else -> { /* No action needed */ }
                }
            }
        }
    }
    
    private fun scheduleTrialExpirationChecks() {
        val checkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val checkWorkRequest = PeriodicWorkRequestBuilder<TrialExpirationCheckWorker>(
            6, TimeUnit.HOURS // Check every 6 hours
        )
            .setConstraints(checkConstraints)
            .addTag(TRIAL_CHECK_WORK_NAME)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                TRIAL_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                checkWorkRequest
            )
    }
    
    private fun scheduleExpirationReminders() {
        if (!trialManager.isTrialActive()) return
        
        val hoursRemaining = trialManager.getHoursRemaining()
        
        // Schedule notifications at 24 hours, 6 hours, and 1 hour before expiration
        val reminderHours = listOf(24, 6, 1)
        
        reminderHours.forEach { reminderHour ->
            if (hoursRemaining > reminderHour) {
                scheduleExpirationNotification(reminderHour)
            }
        }
    }
    
    private fun scheduleExpirationNotification(hoursBeforeExpiration: Int) {
        val delay = trialManager.getHoursRemaining() - hoursBeforeExpiration
        if (delay <= 0) return
        
        val notificationRequest = OneTimeWorkRequestBuilder<TrialExpirationNotificationWorker>()
            .setInitialDelay(delay.toLong(), TimeUnit.HOURS)
            .setInputData(
                Data.Builder()
                    .putInt("hours_remaining", hoursBeforeExpiration)
                    .build()
            )
            .addTag(EXPIRATION_NOTIFICATION_WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueue(notificationRequest)
    }
    
    // MARK: - Expiration Handling
    
    private suspend fun handleTrialExpiration() {
        try {
            println("⏰ Handling trial expiration...")
            
            // Send expiration event
            _trialExpiredFlow.emit(TrialExpirationEvent.TrialExpired)
            
            // Revoke access to premium features
            revokePremiumFeatureAccess()
            
            // Show expiration notification
            showTrialExpiredNotification()
            
            // Schedule upgrade prompts
            scheduleUpgradePrompts()
            
            // Track expiration for analytics
            trialManager.trackTrialExpiration()
            
            println("✅ Trial expiration handling completed")
            
        } catch (e: Exception) {
            println("❌ Error handling trial expiration: ${e.message}")
        }
    }
    
    private suspend fun revokePremiumFeatureAccess() {
        val premiumFeatures = listOf(
            PremiumFeature.MULTIPLE_APP_GROUPS,
            PremiumFeature.ADVANCED_ANALYTICS,
            PremiumFeature.SMART_APP_CATEGORIZATION,
            PremiumFeature.SCHEDULED_LIMITS,
            PremiumFeature.FOCUS_SESSIONS,
            PremiumFeature.CUSTOM_THEMES,
            PremiumFeature.EXPORT_DATA,
            PremiumFeature.CLOUD_SYNC
        )
        
        premiumFeatures.forEach { feature ->
            _featureAccessRevokedFlow.emit(feature)
            println("🔒 Revoked access to ${feature.displayName}")
        }
    }
    
    // MARK: - Notifications
    
    private fun showTrialExpiredNotification() {
        notificationService.showTrialExpiredNotification()
    }
    
    fun showTrialReminderNotification(hoursRemaining: Int) {
        val message = when (hoursRemaining) {
            24 -> "Your premium trial expires in 1 day"
            6 -> "Your premium trial expires in 6 hours"
            1 -> "Your premium trial expires in 1 hour"
            else -> "Your premium trial is ending soon"
        }
        
        notificationService.showTrialReminderNotification(message, hoursRemaining)
    }
    
    // MARK: - Upgrade Prompts
    
    private fun scheduleUpgradePrompts() {
        // Schedule gentle upgrade prompts after trial expiration
        val promptDelays = listOf(1, 24, 72) // 1 hour, 1 day, 3 days after expiration
        
        promptDelays.forEach { delayHours ->
            scheduleUpgradePrompt(delayHours)
        }
    }
    
    private fun scheduleUpgradePrompt(delayHours: Int) {
        val promptRequest = OneTimeWorkRequestBuilder<UpgradePromptWorker>()
            .setInitialDelay(delayHours.toLong(), TimeUnit.HOURS)
            .setInputData(
                Data.Builder()
                    .putInt("prompt_delay_hours", delayHours)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(promptRequest)
    }
    
    // MARK: - Graceful Degradation
    
    /**
     * Handle feature access gracefully when trial expires
     */
    suspend fun handleFeatureAccessRevocation(feature: PremiumFeature) {
        when (feature) {
            PremiumFeature.MULTIPLE_APP_GROUPS -> {
                // Keep only the first app group, disable others
                handleMultipleAppGroupsRevocation()
            }
            PremiumFeature.ADVANCED_ANALYTICS -> {
                // Show basic analytics only
                println("📊 Switched to basic analytics view")
            }
            PremiumFeature.SMART_APP_CATEGORIZATION -> {
                // Disable auto-categorization
                println("🏷️ Smart categorization disabled")
            }
            PremiumFeature.SCHEDULED_LIMITS -> {
                // Keep current limits but disable scheduling
                handleScheduledLimitsRevocation()
            }
            else -> {
                println("🔒 Feature ${feature.displayName} access revoked")
            }
        }
    }
    
    private suspend fun handleMultipleAppGroupsRevocation() {
        try {
            // This would integrate with your repository to keep only the first app group
            println("📱 Keeping only the first app group, disabling others")
            _trialExpiredFlow.emit(TrialExpirationEvent.AppGroupsLimited)
        } catch (e: Exception) {
            println("❌ Error handling app groups revocation: ${e.message}")
        }
    }
    
    private suspend fun handleScheduledLimitsRevocation() {
        try {
            // Convert scheduled limits to basic limits
            println("⏰ Converting scheduled limits to basic daily limits")
            _trialExpiredFlow.emit(TrialExpirationEvent.ScheduledLimitsDisabled)
        } catch (e: Exception) {
            println("❌ Error handling scheduled limits revocation: ${e.message}")
        }
    }
    
    // MARK: - User Communication
    
    fun getExpirationMessage(): String {
        return when {
            trialManager.isTrialExpired() -> {
                "Your 7-day premium trial has expired. Upgrade to continue enjoying premium features like multiple app groups, advanced analytics, and more."
            }
            trialManager.isTrialEnded() -> {
                "Premium features are now locked. Start a subscription to unlock them again."
            }
            else -> {
                "Premium trial status unknown."
            }
        }
    }
    
    fun getUpgradeCallToAction(): String {
        return "Upgrade to Premium to restore all features and continue your digital wellness journey."
    }
    
    // MARK: - Cleanup
    
    fun cleanup() {
        scope.cancel()
        WorkManager.getInstance(context).cancelAllWorkByTag(TRIAL_CHECK_WORK_NAME)
        WorkManager.getInstance(context).cancelAllWorkByTag(EXPIRATION_NOTIFICATION_WORK_NAME)
    }
}

// MARK: - Trial Expiration Events

sealed class TrialExpirationEvent {
    object TrialExpired : TrialExpirationEvent()
    object AppGroupsLimited : TrialExpirationEvent()
    object ScheduledLimitsDisabled : TrialExpirationEvent()
    data class FeatureRevoked(val feature: PremiumFeature) : TrialExpirationEvent()
}

// MARK: - Worker Classes

class TrialExpirationCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val trialManager = PremiumTrialManager.getInstance(applicationContext)
            val expirationManager = TrialExpirationManager.getInstance(applicationContext)
            
            if (trialManager.isTrialExpired() && !trialManager.isTrialEnded()) {
                trialManager.endTrial()
                println("🔄 Trial expired and marked as ended")
            }
            
            Result.success()
        } catch (e: Exception) {
            println("❌ Trial expiration check failed: ${e.message}")
            Result.retry()
        }
    }
}

class TrialExpirationNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val hoursRemaining = inputData.getInt("hours_remaining", 0)
            val expirationManager = TrialExpirationManager.getInstance(applicationContext)
            
            expirationManager.showTrialReminderNotification(hoursRemaining)
            
            Result.success()
        } catch (e: Exception) {
            println("❌ Trial reminder notification failed: ${e.message}")
            Result.failure()
        }
    }
}

class UpgradePromptWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val delayHours = inputData.getInt("prompt_delay_hours", 1)
            val notificationService = NotificationService(applicationContext)
            
            // Show upgrade prompt notification
            notificationService.showUpgradePromptNotification(delayHours)
            
            Result.success()
        } catch (e: Exception) {
            println("❌ Upgrade prompt failed: ${e.message}")
            Result.failure()
        }
    }
}

// MARK: - Extension Functions

fun Context.getTrialExpirationManager(): TrialExpirationManager {
    return TrialExpirationManager.getInstance(this)
}