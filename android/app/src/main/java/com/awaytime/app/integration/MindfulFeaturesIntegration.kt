package com.awaytime.app.integration

import android.content.Context
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.enums.PlatformFeatures
import com.awaytime.app.models.SessionType
import com.awaytime.app.service.EnhancedServiceManager

/**
 * Integration layer to easily add Mindful's enhanced features to existing AwayTime UI components.
 * This class provides simple methods that can be called from existing ViewModels and UI.
 */
class MindfulFeaturesIntegration(
    private val context: Context,
    private val repository: AwayTimeRepository
) {
    private val serviceManager = EnhancedServiceManager.getInstance(context, repository)
    
    /**
     * Initialize the enhanced features when the app starts
     * Call this from your Application.onCreate() or MainActivity.onCreate()
     */
    fun initializeEnhancedFeatures() {
        serviceManager.initializeServices()
    }
    
    /**
     * Enhanced app blocking - use this instead of the basic accessibility service
     * This enables VPN blocking + content filtering + notification batching
     */
    fun enableEnhancedAppBlocking(appPackageNames: Set<String>) {
        // Start VPN-based internet blocking
        serviceManager.startInternetBlocking(appPackageNames)
        
        // Enable notification batching for these apps
        serviceManager.configureNotificationBatching(appPackageNames)
        
        // Update accessibility service blocked apps
        serviceManager.updateBlockedApps(appPackageNames)
    }
    
    /**
     * Enable content-specific blocking (shorts, reels, etc.)
     * Can be used alongside or instead of full app blocking
     */
    fun enableContentFiltering() {
        val features = setOf(
            PlatformFeatures.INSTAGRAM_REELS,
            PlatformFeatures.INSTAGRAM_EXPLORE,
            PlatformFeatures.YOUTUBE_SHORTS,
            PlatformFeatures.TIKTOK_FOR_YOU,
            PlatformFeatures.FACEBOOK_REELS,
            PlatformFeatures.SNAPCHAT_SPOTLIGHT,
            PlatformFeatures.REDDIT_SHORTS
        )
        serviceManager.enableContentFiltering(features)
    }
    
    /**
     * Enable website blocking with optional NSFW filtering
     */
    fun enableWebsiteBlocking(blockedSites: Set<String>, blockNsfw: Boolean = false) {
        serviceManager.enableWebsiteBlocking(blockedSites, blockNsfw)
    }
    
    /**
     * Start a focus session with timer - can be integrated into your focus UI
     */
    fun startFocusSession(
        durationMinutes: Int,
        blockedApps: Set<String>,
        sessionType: SessionType = SessionType.FOCUS
    ): String {
        return serviceManager.startFocusSession(
            durationMinutes = durationMinutes,
            sessionType = sessionType,
            distractingApps = blockedApps,
            enableDnd = true,
            batchNotifications = true
        )
    }
    
    /**
     * Stop the current focus session
     */
    fun stopFocusSession(successful: Boolean = true) {
        serviceManager.stopFocusSession(successful)
    }
    
    /**
     * Apply preset for social media blocking
     * Perfect for "Block Social Media" button in your UI
     */
    fun applySocialMediaPreset() {
        serviceManager.applySocialMediaBlockingPreset()
    }
    
    /**
     * Apply productivity focus preset (Pomodoro-style)
     * Perfect for "Start Focus Session" button in your UI
     */
    fun startProductivitySession(durationMinutes: Int = 25) {
        serviceManager.applyProductivityFocusPreset(durationMinutes)
    }
    
    /**
     * Get current service status - use for UI state management
     */
    fun getServiceStatus() = serviceManager.getServiceStatus()
    
    /**
     * Emergency disable all blocking - use for "Emergency Disable" button
     */
    fun emergencyDisableAll() {
        serviceManager.emergencyStopAllServices()
    }
    
    /**
     * Check if all required permissions are granted
     * Use this to show permission request UI
     */
    fun areAllPermissionsGranted(): Boolean {
        val status = getServiceStatus()
        return status.accessibilityServiceRunning // Add other permission checks as needed
    }
    
    /**
     * Integration with existing app selection UI
     * Call this when user selects apps in your existing AppSelectionScreen
     */
    fun onAppsSelected(selectedPackages: Set<String>, enableEnhancedBlocking: Boolean = true) {
        if (enableEnhancedBlocking) {
            enableEnhancedAppBlocking(selectedPackages)
        } else {
            // Use basic blocking (your existing method)
            serviceManager.updateBlockedApps(selectedPackages)
        }
    }
    
    /**
     * Integration with existing dashboard
     * Call this to update dashboard stats with focus session data
     */
    fun getFocusSessionStats(): FocusSessionStats {
        // TODO: Implement based on your database schema
        return FocusSessionStats(
            totalSessions = 0,
            totalMinutes = 0,
            streakDays = 0,
            isSessionActive = getServiceStatus().focusSessionActive
        )
    }
    
    /**
     * Integration with existing notification preferences
     * Call this when user changes notification settings
     */
    fun updateNotificationPreferences(
        batchDuringFocus: Boolean,
        batchSelectedApps: Boolean,
        selectedApps: Set<String>
    ) {
        serviceManager.configureNotificationBatching(
            batchedApps = if (batchSelectedApps) selectedApps else emptySet(),
            enableBatching = batchDuringFocus || batchSelectedApps,
            autoBatchDuringFocus = batchDuringFocus
        )
    }
}

/**
 * Data class for focus session statistics
 */
data class FocusSessionStats(
    val totalSessions: Int,
    val totalMinutes: Int,
    val streakDays: Int,
    val isSessionActive: Boolean
)