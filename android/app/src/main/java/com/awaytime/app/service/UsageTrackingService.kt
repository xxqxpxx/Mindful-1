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
import androidx.work.*
import com.awaytime.app.data.repository.AwayTimeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class UsageTrackingService(private val context: Context) {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: throw IllegalStateException("UsageStatsManager not available")
    }
    private val repository = AwayTimeRepository(context)
    private val permissionService = PermissionService(context)
    private val improvedTracker = ImprovedUsageTrackingManager(context)
    
    // Use supervised scope for structured concurrency
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    // Flow-based reactive streams
    private val _usageUpdates = MutableSharedFlow<UsageUpdate>(replay = 1)
    val usageUpdates: SharedFlow<UsageUpdate> = _usageUpdates.asSharedFlow()
    
    private val _monitoringState = MutableStateFlow(MonitoringState.STOPPED)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    companion object {
        private const val MONITORING_WORK_NAME = "usage_monitoring_work"
        private const val UPDATE_INTERVAL_MINUTES = 5L
        private const val IMMEDIATE_UPDATE_INTERVAL_MS = 60_000L // 1 minute
    }
    
    enum class MonitoringState {
        STOPPED, STARTING, RUNNING, PAUSED, ERROR
    }
    
    data class UsageUpdate(
        val appGroupName: String,
        val usageMinutes: Int,
        val limitMinutes: Int,
        val limitExceeded: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    // MARK: - Monitoring Control

    suspend fun startMonitoring() {
        // Check permissions using improved tracker
        if (!improvedTracker.hasUsageStatsPermission()) {
            throw SecurityException("PACKAGE_USAGE_STATS permission not granted")
        }

        // Check if user is unlocked (required for Android R+)
        if (!improvedTracker.isUserUnlocked()) {
            throw SecurityException("Device must be unlocked to access usage stats")
        }

        // Test actual usage access
        if (!improvedTracker.testUsageAccess()) {
            throw SecurityException("Cannot access usage stats - permission may not be properly granted")
        }

        if (isMonitoring) {
            return
        }

        isMonitoring = true

        // Start periodic background monitoring
        startBackgroundMonitoring()

        // Start immediate monitoring loop
        startImmediateMonitoring()

        println("✅ Started usage monitoring with improved tracker")
    }

    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()

        // Cancel background work
        WorkManager.getInstance(context).cancelUniqueWork(MONITORING_WORK_NAME)

        println("✅ Stopped usage monitoring")
    }

    private fun startBackgroundMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val monitoringWork = PeriodicWorkRequestBuilder<UsageMonitoringWorker>(
            UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MONITORING_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            monitoringWork
        )
    }

    private fun startImmediateMonitoring() {
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    updateUsageData()
                    delay(TimeUnit.MINUTES.toMillis(1)) // Update every minute when app is active
                } catch (e: Exception) {
                    println("❌ Error in usage monitoring: ${e.message}")
                    delay(TimeUnit.MINUTES.toMillis(5)) // Longer delay on error
                }
            }
        }
    }

    // MARK: - Usage Data Collection

    suspend fun updateUsageData() {
        try {
            // Check permissions using improved tracker
            if (!improvedTracker.hasUsageStatsPermission()) {
                println("❌ Usage stats permission not available")
                return
            }

            // Check if user is unlocked (Android R+)
            if (!improvedTracker.isUserUnlocked()) {
                println("❌ Device is locked, cannot access usage stats")
                return
            }

            val appGroups = withTimeoutOrNull(10_000) {
                repository.getAllAppGroups().first()
            } ?: run {
                println("⚠️ Timeout getting app groups")
                return
            }

            for (appGroup in appGroups.filter { it.isActive }) {
                try {
                    val packageNames = appGroup.getSelectedApps()
                    if (packageNames.isEmpty()) {
                        println("⚠️ No apps selected for group ${appGroup.name}")
                        continue
                    }
                    
                    // Use safe execution for usage tracking operations
                    val usageMinutes = com.awaytime.app.service.CrashPrevention.safeExecute(
                        operation = { improvedTracker.getTotalUsageMinutes(packageNames) },
                        timeoutMs = 3000L,
                        fallback = 0,
                        operationName = "get_total_usage_minutes"
                    ) ?: 0
                    
                    val limitExceeded = usageMinutes >= appGroup.dailyLimitMinutes

                    println("📊 Updated usage for ${appGroup.name}: $usageMinutes minutes (limit: ${appGroup.dailyLimitMinutes})")

                    // Save usage record with crash protection
                    com.awaytime.app.service.CrashPrevention.safeExecute(
                        operation = {
                            repository.saveUsageRecord(
                                date = Date(),
                                usageMinutes = usageMinutes,
                                appGroupName = appGroup.name,
                                limitExceeded = limitExceeded
                            )
                        },
                        timeoutMs = 5000L,
                        operationName = "save_usage_record"
                    )

                    // Check for warning threshold (80%)
                    val warningThreshold = (appGroup.dailyLimitMinutes * 0.8).toInt()
                    if (usageMinutes >= warningThreshold && usageMinutes < appGroup.dailyLimitMinutes) {
                        sendWarningNotification(appGroup.name, appGroup.dailyLimitMinutes - usageMinutes)
                    }

                    // Check for limit reached
                    if (limitExceeded) {
                        sendLimitNotification(appGroup.name)
                        triggerAppBlocking(packageNames)
                    }
                } catch (e: Exception) {
                    println("❌ Error processing app group ${appGroup.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error in updateUsageData: ${e.message}")
        }
    }

    suspend fun getCurrentUsage(appGroupName: String): Int {
        return try {
            // Check permissions using improved tracker
            if (!improvedTracker.hasUsageStatsPermission()) {
                println("❌ No usage stats permission for getCurrentUsage")
                return 0
            }

            // Get the app group and calculate real-time usage with timeout
            val appGroups = withTimeoutOrNull(5000) {
                repository.getAllAppGroups().first()
            } ?: run {
                println("⚠️ Timeout getting app groups for usage")
                return 0
            }
            
            val appGroup = appGroups.find { it.name == appGroupName }
            
            if (appGroup != null) {
                val packageNames = appGroup.getSelectedApps()
                if (packageNames.isNotEmpty()) {
                    // Use improved tracker for accurate usage with timeout
                    val realTimeUsage = withTimeoutOrNull(3000) {
                        improvedTracker.getTotalUsageMinutes(packageNames)
                    } ?: run {
                        println("⚠️ Timeout getting usage stats for $appGroupName")
                        return 0
                    }
                    
                    println("📊 Real-time usage for $appGroupName: $realTimeUsage minutes")
                    return maxOf(0, realTimeUsage) // Ensure non-negative
                } else {
                    println("⚠️ No apps selected in group: $appGroupName")
                    return 0
                }
            } else {
                println("⚠️ App group not found: $appGroupName")
                val fallbackUsage = withTimeoutOrNull(2000) {
                    repository.getTodayUsage(appGroupName)
                } ?: 0
                return maxOf(0, fallbackUsage)
            }
        } catch (e: Exception) {
            println("❌ Error getting current usage: ${e.message}")
            // Fallback to repository data, ensuring non-negative
            return try {
                val fallbackUsage = withTimeoutOrNull(2000) {
                    repository.getTodayUsage(appGroupName)
                } ?: 0
                maxOf(0, fallbackUsage)
            } catch (e2: Exception) {
                println("❌ Fallback usage query also failed: ${e2.message}")
                0
            }
        }
    }

    fun getUsageStats(packageNames: List<String>, days: Int = 7): Map<String, List<UsageStats>> {
        return improvedTracker.getDetailedUsageStats(packageNames, days)
    }

    // MARK: - Notifications

    private fun sendWarningNotification(appGroupName: String, minutesRemaining: Int) {
        val intent = Intent("com.awaytime.USAGE_WARNING").apply {
            putExtra("appGroupName", appGroupName)
            putExtra("minutesRemaining", minutesRemaining)
        }
        context.sendBroadcast(intent)
    }

    private fun sendLimitNotification(appGroupName: String) {
        val intent = Intent("com.awaytime.USAGE_LIMIT").apply {
            putExtra("appGroupName", appGroupName)
        }
        context.sendBroadcast(intent)
    }

    // MARK: - App Blocking Integration

    private fun triggerAppBlocking(packageNames: List<String>) {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()
        accessibilityService?.setBlockedPackages(packageNames.toSet())

        // Store blocking state
        val prefs = context.getSharedPreferences("awaytime_blocking", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("blocked_packages", packageNames.toSet())
            .putLong("blocking_started", System.currentTimeMillis())
            .apply()
    }

    fun clearAppBlocking() {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()
        accessibilityService?.clearBlockedPackages()

        // Clear blocking state
        val prefs = context.getSharedPreferences("awaytime_blocking", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // MARK: - Utility Methods

    fun isMonitoringActive(): Boolean = isMonitoring

    fun formatUsageTime(minutes: Int): String {
        return improvedTracker.formatUsageTime(minutes)
    }

    fun getUsageProgress(currentMinutes: Int, limitMinutes: Int): Float {
        return improvedTracker.getUsageProgress(currentMinutes, limitMinutes)
    }

    // MARK: - Debug and Utility Methods

    fun getDebugInfo(): Map<String, Any> {
        return improvedTracker.getDebugInfo() + mapOf(
            "isMonitoring" to isMonitoring,
            "serviceName" to "UsageTrackingService"
        )
    }

    fun getUserInstalledApps(): Map<String, String> {
        return improvedTracker.getUserInstalledApps()
    }

    fun requestPermissions() {
        improvedTracker.requestUsageStatsPermission()
    }
    
    // MARK: - Public API methods (for external integration)
    
    suspend fun getCurrentUsage(): List<AppUsageData> {
        val appGroups = repository.getAllAppGroups().first()
        val usageDataList = mutableListOf<AppUsageData>()
        
        for (appGroup in appGroups.filter { it.isActive }) {
            val selectedApps = appGroup.getSelectedApps()
            for (packageName in selectedApps) {
                val usageMinutes = improvedTracker.getUsageForPackage(packageName)
                
                usageDataList.add(
                    AppUsageData(
                        packageName = packageName,
                        appName = getAppName(packageName),
                        usageTimeMinutes = usageMinutes,
                        lastUsed = Date(),
                        appIdentifier = appGroup.name,
                        totalTime = appGroup.dailyLimitMinutes
                    )
                )
            }
        }
        
        return usageDataList
    }
    
    fun getTodayUsage(): List<AppUsageData> {
        // Return empty list to avoid blocking main thread
        // Real usage data should be obtained via getCurrentUsage() suspend function
        return emptyList()
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }


}

// MARK: - Background Worker

class UsageMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val usageTrackingService = UsageTrackingService(applicationContext)
            usageTrackingService.updateUsageData()

            Result.success()
        } catch (e: Exception) {
            println("❌ Background usage monitoring failed: ${e.message}")
            Result.retry()
        }
    }
}

// MARK: - Usage Data Models

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val lastUsed: Date,

    val appIdentifier: String,
    val totalTime: Int
) {
}

data class DailyUsageSummary(
    val date: Date,
    val totalUsageMinutes: Int,
    val appUsageData: List<AppUsageData>,
    val limitExceeded: Boolean
)