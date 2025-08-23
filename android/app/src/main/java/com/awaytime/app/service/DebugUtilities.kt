package com.awaytime.app.service

import android.content.Context
import com.awaytime.app.data.repository.AwayTimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach

/**
 * Debug utilities to help troubleshoot app selection and usage tracking issues
 */
class DebugUtilities(private val context: Context) {
    
    private val repository = AwayTimeRepository(context)
    private val usageTrackingService = UsageTrackingService(context)
    // AppSelectionService removed - using clean Mindful architecture
    
    suspend fun debugAppSelection(): String {
        val debug = StringBuilder()
        debug.appendLine("🔍 DEBUG: App Selection Status")
        debug.appendLine("=" * 50)
        
        try {
            // Check app groups in database
            val appGroups = repository.getAllAppGroupsSync()
            debug.appendLine("App Groups in Database: ${appGroups.size}")
            appGroups.forEach { group ->
                debug.appendLine("  - ${group.name}: ${group.getSelectedApps().size} apps, ${group.dailyLimitMinutes}min limit")
                group.getSelectedApps().forEach { app ->
                    debug.appendLine("    * $app")
                }
            }
            
        } catch (e: Exception) {
            debug.appendLine("❌ Error during debug: ${e.message}")
        }
        
        return debug.toString()
    }
    
    suspend fun debugUsageTracking(): String {
        val debug = StringBuilder()
        debug.appendLine("📊 DEBUG: Usage Tracking Status")
        debug.appendLine("=" * 50)
        
        try {
            // Check if monitoring is active
            debug.appendLine("Monitoring Active: ${usageTrackingService.isMonitoringActive()}")
            
            // Check app groups and their usage
            val appGroups = repository.getAllAppGroups().first()
            debug.appendLine("App Groups: ${appGroups.size}")
            
            for (group in appGroups) {
                debug.appendLine("\n--- ${group.name} ---")
                debug.appendLine("Apps: ${group.getSelectedApps().joinToString(", ")}")
                debug.appendLine("Limit: ${group.dailyLimitMinutes} minutes")
                
                val todayUsage = repository.getTodayUsage(group.name)
                debug.appendLine("Today's Usage (DB): $todayUsage minutes")
                
                val realTimeUsage = usageTrackingService.getCurrentUsage(group.name)
                debug.appendLine("Real-time Usage: $realTimeUsage minutes")
                
                // Check individual app usage
                for (packageName in group.getSelectedApps()) {
                    try {
                        // Use getCurrentUsage instead of private method
                        val appUsage = usageTrackingService.getCurrentUsage(group.name)
                        debug.appendLine("  $packageName: part of group with $appUsage minutes total")
                    } catch (e: Exception) {
                        debug.appendLine("  $packageName: error getting usage - ${e.message}")
                    }
                }
            }
            
        } catch (e: Exception) {
            debug.appendLine("❌ Error during debug: ${e.message}")
        }
        
        return debug.toString()
    }
    
    suspend fun debugPermissions(): String {
        val debug = StringBuilder()
        debug.appendLine("🔐 DEBUG: Permissions Status")
        debug.appendLine("=" * 50)
        
        val permissionService = PermissionService(context)
        permissionService.updatePermissionStatuses()
        
        debug.appendLine("Usage Stats Permission: ${permissionService.hasUsageStatsPermission()}")
        debug.appendLine("Accessibility Permission: ${permissionService.hasAccessibilityPermission()}")
        debug.appendLine("Notification Permission: ${permissionService.hasNotificationPermission()}")
        debug.appendLine("Fully Authorized: ${permissionService.isFullyAuthorized}")
        debug.appendLine("Needs Any Permission: ${permissionService.needsAnyPermission}")
        
        return debug.toString()
    }
    
    suspend fun debugDatabase(): String {
        val debug = StringBuilder()
        debug.appendLine("💾 DEBUG: Database Status")
        debug.appendLine("=" * 50)
        
        try {
            // Check user settings
            val userSettings = repository.getUserSettingsSync()
            debug.appendLine("User Settings:")
            debug.appendLine("  Premium: ${userSettings.isPremium}")
            debug.appendLine("  Streak: ${userSettings.streakCount}")
            debug.appendLine("  Notifications: ${userSettings.notificationsEnabled}")
            
            // Check usage records
            val usageRecords = repository.getUsageRecordsSync(7)
            debug.appendLine("\nUsage Records (last 7 days): ${usageRecords.size}")
            usageRecords.take(5).forEach { record ->
                debug.appendLine("  ${record.getDateAsDate()}: ${record.appGroupName} - ${record.usageMinutes}min")
            }
            
        } catch (e: Exception) {
            debug.appendLine("❌ Error during debug: ${e.message}")
        }
        
        return debug.toString()
    }
    
    suspend fun fullDebugReport(): String {
        val debug = StringBuilder()
        debug.appendLine("🚀 AWAYTIME DEBUG REPORT")
        debug.appendLine("Generated: ${java.util.Date()}")
        debug.appendLine("=" * 60)
        debug.appendLine()
        
        debug.appendLine(debugPermissions())
        debug.appendLine()
        debug.appendLine(debugAppSelection())
        debug.appendLine()
        debug.appendLine(debugUsageTracking())
        debug.appendLine()
        debug.appendLine(debugDatabase())
        
        return debug.toString()
    }
    
    /**
     * Fix common issues automatically
     */
    suspend fun autoFix(): String {
        val fixes = StringBuilder()
        fixes.appendLine("🔧 AUTO-FIX RESULTS")
        fixes.appendLine("=" * 30)
        
        try {
            // Fix 1: Ensure user settings exist
            repository.getUserSettingsSync()
            fixes.appendLine("✅ User settings initialized")
            
            // Fix 2: Check app groups (Mindful architecture)
            val appGroups = repository.getAllAppGroupsSync()
            if (appGroups.isEmpty()) {
                fixes.appendLine("⚠️ No app groups found - user needs to create app groups")
            } else {
                fixes.appendLine("✅ Found ${appGroups.size} app groups")
            }
            
            // Fix 3: Start usage tracking if not active
            if (!usageTrackingService.isMonitoringActive()) {
                usageTrackingService.startMonitoring()
                fixes.appendLine("✅ Usage tracking started")
            }
            
            // Fix 4: Clean up old records
            repository.cleanupOldRecords()
            fixes.appendLine("✅ Old records cleaned up")
            
        } catch (e: Exception) {
            fixes.appendLine("❌ Auto-fix error: ${e.message}")
        }
        
        return fixes.toString()
    }
}

// Extension function for string repetition
private operator fun String.times(n: Int): String = this.repeat(n)