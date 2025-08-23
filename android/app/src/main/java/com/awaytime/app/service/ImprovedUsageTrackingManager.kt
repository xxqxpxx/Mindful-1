package com.awaytime.app.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Improved Usage Tracking Manager that follows Android best practices
 * Based on official Android documentation for UsageStatsManager
 */
class ImprovedUsageTrackingManager(private val context: Context) {
    
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    
    // Cache for user-installed apps to avoid repeated queries
    private val userAppsCache = ConcurrentHashMap<String, Boolean>()
    private var lastCacheUpdate = 0L
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes
    
    companion object {
        private const val TAG = "ImprovedUsageTracking"
    }
    
    // MARK: - Permission Checks
    
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            println("❌ Error checking usage stats permission: ${e.message}")
            false
        }
    }
    
    fun isUserUnlocked(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                userManager.isUserUnlocked
            } catch (e: Exception) {
                println("❌ Error checking user unlock status: ${e.message}")
                true // Assume unlocked if we can't check
            }
        } else {
            true // Not applicable for older versions
        }
    }
    
    fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            println("❌ Failed to open usage access settings: ${e.message}")
        }
    }
    
    // MARK: - App Filtering
    
    private fun isUserInstalledApp(packageName: String): Boolean {
        // Check cache first
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate > cacheValidityMs) {
            userAppsCache.clear()
            lastCacheUpdate = currentTime
        }
        
        return userAppsCache.getOrPut(packageName) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            } catch (e: Exception) {
                false
            }
        }
    }
    
    fun getUserInstalledApps(): Map<String, String> {
        val appInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appInfoMap = HashMap<String, String>()
        
        for (appInfo in appInfos) {
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }
                appInfoMap[appInfo.packageName] = appName
            }
        }
        
        return appInfoMap
    }
    
    // MARK: - Usage Data Retrieval
    
    fun getTodayUsageStats(packageNames: List<String>): Map<String, Long> {
        if (!hasUsageStatsPermission()) {
            println("❌ No usage stats permission")
            return emptyMap()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isUserUnlocked()) {
            println("❌ Device locked, cannot query usage stats")
            return emptyMap()
        }
        
        return try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            
            // Start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            if (usageStats == null) {
                println("❌ UsageStats query returned null")
                return emptyMap()
            }
            
            val result = mutableMapOf<String, Long>()
            
            for (packageName in packageNames) {
                if (isUserInstalledApp(packageName)) {
                    val stats = usageStats.find { it.packageName == packageName }
                    val usageTime = stats?.totalTimeInForeground ?: 0L
                    result[packageName] = usageTime
                    
                    if (usageTime > 0) {
                        println("📊 $packageName: ${usageTime / (1000 * 60)} minutes")
                    }
                }
            }
            
            result
            
        } catch (e: SecurityException) {
            println("❌ Security exception accessing usage stats: ${e.message}")
            emptyMap()
        } catch (e: Exception) {
            println("❌ Error getting usage stats: ${e.message}")
            emptyMap()
        }
    }
    
    fun getTodayUsageEvents(packageNames: List<String>): Map<String, Long> {
        return com.awaytime.app.service.EmergencyMode.safeExecuteWithEmergencyCheck(
            operation = {
                if (!hasUsageStatsPermission()) {
                    println("❌ No usage stats permission for events")
                    return@safeExecuteWithEmergencyCheck emptyMap()
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isUserUnlocked()) {
                    println("❌ Device locked, cannot query usage events")
                    return@safeExecuteWithEmergencyCheck emptyMap()
                }
                
                // Limit to maximum 5 packages to prevent excessive processing
                val limitedPackageNames = packageNames.take(5)
                
                getTodayUsageEventsInternal(limitedPackageNames)
            },
            fallback = emptyMap(),
            operationName = "get_today_usage_events",
            requiresNativeOperations = true
        )
    }
    
    private fun getTodayUsageEventsInternal(packageNames: List<String>): Map<String, Long> {
        return try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            
            // Start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            
            if (usageEvents == null) {
                println("❌ Usage events query returned null, falling back to stats")
                return getTodayUsageStats(packageNames)
            }
            
            val appUsageMap = mutableMapOf<String, Long>()
            val appSessionMap = mutableMapOf<String, Long>()
            
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                
                val packageName = event.packageName
                if (packageNames.contains(packageName) && isUserInstalledApp(packageName)) {
                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            // App moved to foreground
                            appSessionMap[packageName] = event.timeStamp
                        }
                        
                        @Suppress("DEPRECATION")
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            // App moved to foreground (deprecated but still used on older devices)
                            appSessionMap[packageName] = event.timeStamp
                        }
                        
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            // App moved to background
                            val sessionStart = appSessionMap[packageName]
                            if (sessionStart != null) {
                                val sessionDuration = event.timeStamp - sessionStart
                                appUsageMap[packageName] = (appUsageMap[packageName] ?: 0) + sessionDuration
                                appSessionMap.remove(packageName)
                            }
                        }
                        
                        @Suppress("DEPRECATION")
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            // App moved to background (deprecated but still used on older devices)
                            val sessionStart = appSessionMap[packageName]
                            if (sessionStart != null) {
                                val sessionDuration = event.timeStamp - sessionStart
                                appUsageMap[packageName] = (appUsageMap[packageName] ?: 0) + sessionDuration
                                appSessionMap.remove(packageName)
                            }
                        }
                    }
                }
            }
            
            // Handle apps still in foreground
            val currentTime = System.currentTimeMillis()
            for ((packageName, sessionStartTime) in appSessionMap) {
                val sessionDuration = currentTime - sessionStartTime
                appUsageMap[packageName] = (appUsageMap[packageName] ?: 0) + sessionDuration
            }
            
            // Log results
            for ((packageName, usageTime) in appUsageMap) {
                if (usageTime > 0) {
                    println("📱 $packageName: ${usageTime / (1000 * 60)} minutes (events)")
                }
            }
            
            appUsageMap
            
        } catch (e: SecurityException) {
            println("❌ Security exception accessing usage events: ${e.message}")
            getTodayUsageStats(packageNames) // Fallback
        } catch (e: Exception) {
            println("❌ Error getting usage events: ${e.message}")
            getTodayUsageStats(packageNames) // Fallback
        }
    }
    
    // MARK: - Convenience Methods
    
    fun getTotalUsageMinutes(packageNames: List<String>): Int {
        return com.awaytime.app.service.EmergencyMode.safeExecuteWithEmergencyCheck(
            operation = {
                val usageMap = getTodayUsageEvents(packageNames)
                val totalUsageMs = usageMap.values.sum()
                (totalUsageMs / (1000 * 60)).toInt()
            },
            fallback = 0,
            operationName = "get_total_usage_minutes",
            requiresNativeOperations = true
        )
    }
    
    fun getUsageForPackage(packageName: String): Int {
        val usageMap = getTodayUsageEvents(listOf(packageName))
        val usageMs = usageMap[packageName] ?: 0L
        return (usageMs / (1000 * 60)).toInt()
    }
    
    fun getDetailedUsageStats(packageNames: List<String>, days: Int = 7): Map<String, List<UsageStats>> {
        if (!hasUsageStatsPermission()) {
            return emptyMap()
        }
        
        return try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            
            calendar.add(Calendar.DAY_OF_MONTH, -days)
            val startTime = calendar.timeInMillis
            
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            if (usageStats == null) {
                return emptyMap()
            }
            
            packageNames.associateWith { packageName ->
                usageStats.filter { it.packageName == packageName && isUserInstalledApp(packageName) }
            }
            
        } catch (e: Exception) {
            println("❌ Error getting detailed usage stats: ${e.message}")
            emptyMap()
        }
    }
    
    // MARK: - Utility Methods
    
    fun formatUsageTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "0m"
        }
    }
    
    fun getUsageProgress(currentMinutes: Int, limitMinutes: Int): Float {
        if (limitMinutes <= 0) return 0f
        return (currentMinutes.toFloat() / limitMinutes.toFloat()).coerceAtMost(1.0f)
    }
    
    // MARK: - Debug Methods
    
    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "hasPermission" to hasUsageStatsPermission(),
            "isUserUnlocked" to isUserUnlocked(),
            "androidVersion" to Build.VERSION.SDK_INT,
            "cacheSize" to userAppsCache.size,
            "lastCacheUpdate" to Date(lastCacheUpdate)
        )
    }
    
    fun testUsageAccess(): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.HOUR, -1)
            val startTime = calendar.timeInMillis
            
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            usageStats != null
        } catch (e: Exception) {
            println("❌ Usage access test failed: ${e.message}")
            false
        }
    }
}