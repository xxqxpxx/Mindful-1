package com.awaytime.app.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.awaytime.app.adapters.WellbeingAdapter
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.enums.PlatformFeatures
import com.awaytime.app.models.FocusSession
import com.awaytime.app.models.NotificationSettings
import com.awaytime.app.models.SessionType
import com.awaytime.app.utils.ServiceBinder

/**
 * Central manager for all enhanced AwayTime services integrated from Mindful.
 * Coordinates VPN, notification management, focus sessions, and accessibility services.
 */
class EnhancedServiceManager(
    private val context: Context,
    private val repository: AwayTimeRepository
) {
    companion object {
        private const val TAG = "EnhancedServiceManager"
        
        @Volatile
        private var instance: EnhancedServiceManager? = null
        
        @Synchronized
        fun getInstance(context: Context, repository: AwayTimeRepository): EnhancedServiceManager {
            if (instance == null) {
                instance = EnhancedServiceManager(context, repository)
            }
            return instance!!
        }
    }
    
    private val wellbeingAdapter = WellbeingAdapter(context, repository)
    
    // Service state tracking
    private var isVpnServiceRunning = false
    private var isNotificationServiceRunning = false
    private var isFocusSessionActive = false
    
    /**
     * Initializes all enhanced services based on current app configuration
     */
    fun initializeServices() {
        try {
            Log.d(TAG, "Initializing enhanced services")
            
            // Sync current app groups with wellbeing settings
            wellbeingAdapter.syncAppGroupsToWellbeing()
            
            // Initialize notification helper channels
            com.awaytime.app.utils.NotificationHelper.createNotificationChannels(context)
            
            Log.d(TAG, "Enhanced services initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize enhanced services", e)
        }
    }
    
    /**
     * Starts VPN-based internet blocking for specified apps
     */
    fun startInternetBlocking(blockedApps: Set<String>) {
        try {
            if (blockedApps.isEmpty()) {
                stopInternetBlocking()
                return
            }
            
            val intent = Intent(context, AwayTimeVpnService::class.java).apply {
                action = ServiceBinder.ACTION_START_AWAYTIME_SERVICE
                putExtra("blocked_apps", blockedApps.toTypedArray())
            }
            
            context.startService(intent)
            isVpnServiceRunning = true
            
            Log.d(TAG, "Internet blocking started for ${blockedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start internet blocking", e)
        }
    }
    
    /**
     * Stops VPN-based internet blocking
     */
    fun stopInternetBlocking() {
        try {
            val intent = Intent(context, AwayTimeVpnService::class.java)
            context.stopService(intent)
            isVpnServiceRunning = false
            
            Log.d(TAG, "Internet blocking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop internet blocking", e)
        }
    }
    
    /**
     * Updates blocked apps for VPN service
     */
    fun updateBlockedApps(blockedApps: Set<String>) {
        try {
            wellbeingAdapter.addBlockedApps(blockedApps)
            
            val vpnService = AwayTimeVpnService.getInstance()
            if (vpnService != null) {
                vpnService.updateBlockedApps(blockedApps)
            } else if (blockedApps.isNotEmpty()) {
                startInternetBlocking(blockedApps)
            }
            
            Log.d(TAG, "Blocked apps updated: ${blockedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update blocked apps", e)
        }
    }
    
    /**
     * Enables platform feature blocking (shorts, reels, etc.)
     */
    fun enableContentFiltering(features: Set<PlatformFeatures>) {
        try {
            wellbeingAdapter.updatePlatformFeatures(features)
            Log.d(TAG, "Content filtering enabled for ${features.size} features")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable content filtering", e)
        }
    }
    
    /**
     * Enables website blocking
     */
    fun enableWebsiteBlocking(blockedSites: Set<String>, blockNsfw: Boolean = false) {
        try {
            wellbeingAdapter.updateWebsiteBlocking(blockedSites, blockNsfw)
            Log.d(TAG, "Website blocking enabled for ${blockedSites.size} sites, NSFW: $blockNsfw")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable website blocking", e)
        }
    }
    
    /**
     * Starts a focus session with timer and app blocking
     */
    fun startFocusSession(
        durationMinutes: Int = 0, // 0 = infinite
        sessionType: SessionType = SessionType.FOCUS,
        distractingApps: Set<String>,
        enableDnd: Boolean = true,
        batchNotifications: Boolean = true
    ): String {
        try {
            val sessionId = System.currentTimeMillis().toString()
            val session = FocusSession(
                id = sessionId,
                sessionType = sessionType,
                toggleDnd = enableDnd,
                startTimeMsEpoch = System.currentTimeMillis(),
                durationSecs = durationMinutes * 60,
                distractingApps = distractingApps,
                isActive = true,
                batchNotifications = batchNotifications
            )
            
            val intent = Intent(context, AwayTimeFocusSessionService::class.java).apply {
                action = AwayTimeFocusSessionService.ACTION_START_FOCUS_SESSION
                putExtra(AwayTimeFocusSessionService.EXTRA_FOCUS_SESSION, session.toString())
            }
            
            context.startService(intent)
            isFocusSessionActive = true
            
            Log.d(TAG, "Focus session started: $sessionType for ${durationMinutes}min")
            return sessionId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start focus session", e)
            return ""
        }
    }
    
    /**
     * Stops the current focus session
     */
    fun stopFocusSession(isSuccessful: Boolean = true) {
        try {
            val intent = Intent(context, AwayTimeFocusSessionService::class.java).apply {
                action = AwayTimeFocusSessionService.ACTION_STOP_FOCUS_SESSION
                putExtra(AwayTimeFocusSessionService.EXTRA_SESSION_SUCCESSFUL, isSuccessful)
            }
            
            context.startService(intent)
            isFocusSessionActive = false
            
            Log.d(TAG, "Focus session stopped: successful=$isSuccessful")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop focus session", e)
        }
    }
    
    /**
     * Configures notification batching
     */
    fun configureNotificationBatching(
        batchedApps: Set<String>,
        enableBatching: Boolean = true,
        autoBatchDuringFocus: Boolean = true
    ) {
        try {
            val notificationService = AwayTimeNotificationListenerService.getInstance()
            if (notificationService != null) {
                val settings = NotificationSettings(
                    batchedApps = batchedApps,
                    batchingEnabled = enableBatching,
                    autoBatchDuringFocus = autoBatchDuringFocus,
                    storeNonBatchedToo = true,
                    showSummaries = true
                )
                notificationService.updateNotificationSettings(settings)
            }
            
            Log.d(TAG, "Notification batching configured: ${batchedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure notification batching", e)
        }
    }
    
    /**
     * Gets the current service status
     */
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            serviceName = "EnhancedServiceManager",
            isActive = true,
            hasBackgroundJob = isFocusSessionActive,
            vpnServiceRunning = AwayTimeVpnService.isServiceRunning(),
            notificationServiceRunning = AwayTimeNotificationListenerService.isServiceRunning(),
            focusSessionActive = AwayTimeFocusSessionService.isServiceRunning(),
            accessibilityServiceRunning = AwayTimeAccessibilityService.isServiceRunning()
        )
    }
    
    /**
     * Applies quick blocking preset for social media apps
     */
    fun applySocialMediaBlockingPreset() {
        val socialMediaApps = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.snapchat.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.reddit.frontpage"
        )
        
        val socialMediaFeatures = setOf(
            PlatformFeatures.INSTAGRAM_REELS,
            PlatformFeatures.INSTAGRAM_EXPLORE,
            PlatformFeatures.YOUTUBE_SHORTS,
            PlatformFeatures.TIKTOK_FOR_YOU,
            PlatformFeatures.FACEBOOK_REELS,
            PlatformFeatures.REDDIT_SHORTS
        )
        
        updateBlockedApps(socialMediaApps)
        enableContentFiltering(socialMediaFeatures)
        configureNotificationBatching(socialMediaApps)
        
        Log.d(TAG, "Social media blocking preset applied")
    }
    
    /**
     * Applies productivity focus preset
     */
    fun applyProductivityFocusPreset(durationMinutes: Int = 25) {
        val distractingApps = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.youtube.android",
            "com.twitter.android",
            "com.reddit.frontpage"
        )
        
        startFocusSession(
            durationMinutes = durationMinutes,
            sessionType = SessionType.WORK,
            distractingApps = distractingApps,
            enableDnd = true,
            batchNotifications = true
        )
        
        Log.d(TAG, "Productivity focus preset applied for ${durationMinutes}min")
    }
    
    /**
     * Emergency stop all blocking services
     */
    fun emergencyStopAllServices() {
        try {
            stopInternetBlocking()
            stopFocusSession(false)
            // Clear all blocked apps
            val currentWellbeing = wellbeingAdapter.getCurrentWellbeing()
            val clearedWellbeing = currentWellbeing.copy(blockedApps = emptySet())
            com.awaytime.app.helpers.SharedPrefsHelper.getSetWellBeingSettings(context, clearedWellbeing)
            
            Log.d(TAG, "Emergency stop completed - all blocking disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emergency stop services", e)
        }
    }
}

/**
 * Data class representing the current status of all enhanced services
 */
data class EnhancedServiceStatus(
    val vpnServiceRunning: Boolean,
    val notificationServiceRunning: Boolean,
    val focusSessionActive: Boolean,
    val accessibilityServiceRunning: Boolean,
    val bedtimeActive: Boolean = false
) {
    fun allServicesRunning(): Boolean {
        return vpnServiceRunning && notificationServiceRunning && 
               focusSessionActive && accessibilityServiceRunning
    }
    
    fun anyServiceRunning(): Boolean {
        return vpnServiceRunning || notificationServiceRunning || 
               focusSessionActive || accessibilityServiceRunning || bedtimeActive
    }
}