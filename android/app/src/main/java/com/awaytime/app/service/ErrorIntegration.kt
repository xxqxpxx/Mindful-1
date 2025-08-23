package com.awaytime.app.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Integration service that connects error handling with other app services
 */
class ErrorIntegration(private val context: Context) : ViewModel() {

    private val errorService = ErrorHandlingService(context)
    private val usageTrackingService = UsageTrackingService(context)
    private val appBlockingService = AppBlockingService(context)
    private val goalTrackingService = GoalTrackingService(context)
    private val permissionService = PermissionService(context)

    // Event flows for error reporting
    private val _errorReportFlow = MutableSharedFlow<ErrorReport>(replay = 0)
    val errorReportFlow: SharedFlow<ErrorReport> = _errorReportFlow.asSharedFlow()

    init {
        setupErrorIntegration()
        startErrorMonitoring()
    }

    private fun setupErrorIntegration() {
        // Initialize global error handler
        GlobalErrorHandler.initialize(context)

        // Start error monitoring
        errorService.startErrorMonitoring()
    }

    private fun startErrorMonitoring() {
        // Monitor usage tracking health
        monitorUsageTracking()

        // Monitor app blocking health
        monitorAppBlocking()

        // Monitor permission status
        monitorPermissions()

        // Monitor data integrity
        monitorDataIntegrity()
    }

    private fun monitorUsageTracking() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // Check every minute
                checkUsageTrackingHealth()
            }
        }
    }

    private fun checkUsageTrackingHealth() {
        viewModelScope.launch {
            try {
                usageTrackingService.getCurrentUsage()
            } catch (e: Exception) {
                errorService.handleUsageStatsError(e)
                reportError(e, "UsageTracking", "getCurrentUsage")
            }
        }
    }

    private fun monitorAppBlocking() {
        viewModelScope.launch {
            while (true) {
                delay(120000) // Check every 2 minutes
                checkAppBlockingHealth()
            }
        }
    }

    private fun checkAppBlockingHealth() {
        viewModelScope.launch {
            try {
                appBlockingService.validateBlockingStatus()
            } catch (e: Exception) {
                errorService.handleAccessibilityError(e)
                reportError(e, "AppBlocking", "validateBlockingStatus")
            }
        }
    }

    private fun monitorPermissions() {
        viewModelScope.launch {
            while (true) {
                delay(300000) // Check every 5 minutes instead of 30 seconds
                checkPermissionStatus()
            }
        }
    }

    private fun checkPermissionStatus() {
        viewModelScope.launch {
            try {
                if (!permissionService.hasUsageStatsPermission()) {
                    errorService.handlePermissionError(
                        ErrorHandlingService.PermissionType.USAGE_STATS,
                        denied = false
                    )
                }

                if (!permissionService.hasAccessibilityPermission()) {
                    errorService.handlePermissionError(
                        ErrorHandlingService.PermissionType.ACCESSIBILITY,
                        denied = false
                    )
                }

                // Only check notification permission, skip overlay permission 
                // as it's not critical for core functionality
                if (!permissionService.hasNotificationPermission()) {
                    // Don't show error notifications for non-critical permissions
                    println("⚠️ Notification permission not granted, but app can continue")
                }
            } catch (e: Exception) {
                reportError(e, "PermissionMonitoring", "checkPermissionStatus")
            }
        }
    }

    private fun monitorDataIntegrity() {
        viewModelScope.launch {
            while (true) {
                delay(300000) // Check every 5 minutes
                checkDataIntegrity()
            }
        }
    }

    private fun checkDataIntegrity() {
        viewModelScope.launch {
            try {
                // Test basic data operations
                val prefs = context.getSharedPreferences("awaytime_prefs", Context.MODE_PRIVATE)
                prefs.getString("test_key", null) // Test read access

                goalTrackingService.getCurrentGoals() // Test goal data
                usageTrackingService.getTodayUsage() // Test usage data
            } catch (e: Exception) {
                errorService.handleStorageError(e)
                reportError(e, "DataIntegrity", "checkDataIntegrity")
            }
        }
    }

    // MARK: - Public Interface

    fun getErrorService(): ErrorHandlingService {
        return errorService
    }

    fun reportError(error: Exception, service: String, operation: String) {
        val errorReport = ErrorReport(
            error = error,
            service = service,
            operation = operation,
            timestamp = System.currentTimeMillis(),
            context = getErrorContext()
        )

        viewModelScope.launch {
            _errorReportFlow.emit(errorReport)
        }

        println("🚨 Error reported from $service.$operation: ${error.message}")
        errorService.handleUnknownError(error)
    }

    fun reportPermissionError(type: ErrorHandlingService.PermissionType, denied: Boolean = true) {
        errorService.handlePermissionError(type, denied)
    }

    fun reportBlockingFailure(appName: String) {
        errorService.handleBlockingError(appName)
    }

    fun reportDataCorruption() {
        errorService.handleDataCorruption()
    }

    fun reportNetworkError() {
        errorService.handleNetworkError()
    }

    private fun getErrorContext(): Map<String, String> {
        return mapOf(
            "app_version" to getAppVersion(),
            "android_version" to android.os.Build.VERSION.RELEASE,
            "device_model" to android.os.Build.MODEL,
            "available_memory" to getAvailableMemory(),
            "battery_level" to getBatteryLevel()
        )
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getAvailableMemory(): String {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            "${memoryInfo.availMem / 1024 / 1024} MB"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getBatteryLevel(): String {
        return try {
            val batteryManager =
                context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel =
                batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            "$batteryLevel%"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // MARK: - Auto Recovery

    fun attemptAutoRecovery(error: ErrorHandlingService.AwayTimeError): Boolean {
        return when (error) {
            is ErrorHandlingService.AwayTimeError.BlockingFailed -> {
                attemptBlockingRecovery()
            }

            is ErrorHandlingService.AwayTimeError.UsageStatsError,
            is ErrorHandlingService.AwayTimeError.AccessibilityError -> {
                attemptSystemServiceRecovery()
            }

            is ErrorHandlingService.AwayTimeError.StorageError -> {
                attemptStorageRecovery()
            }

            else -> false
        }
    }

    private fun attemptBlockingRecovery(): Boolean {
        return try {
            viewModelScope.launch {
                appBlockingService.initializeBlocking()
                errorService.clearError()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun attemptSystemServiceRecovery(): Boolean {
        return try {
            viewModelScope.launch {
                // Try to restart monitoring
                usageTrackingService.startMonitoring()
                errorService.clearError()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun attemptStorageRecovery(): Boolean {
        return try {
            // Try to clear temporary data
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp_")) {
                    file.delete()
                }
            }
            errorService.clearError()
            true
        } catch (e: Exception) {
            false
        }
    }

    // MARK: - Error Analytics

    fun trackErrorMetrics() {
        viewModelScope.launch {
            val errorCounts = getErrorCounts()
            println("📊 Error metrics: $errorCounts")

            // In production, this would send to analytics service
        }
    }

    private fun getErrorCounts(): Map<String, Int> {
        // This would track error frequencies from persistent storage
        return mapOf(
            "permission_errors" to 0,
            "blocking_errors" to 0,
            "data_errors" to 0,
            "network_errors" to 0,
            "unknown_errors" to 0
        )
    }
}

// MARK: - Error-Aware Service Wrappers

/**
 * Wrapper for UsageTrackingService with error handling
 */
class SafeUsageTrackingService(private val context: Context) {
    private val usageService = UsageTrackingService(context)
    private val errorIntegration = ErrorIntegration(context)

    suspend fun getCurrentUsage(): List<AppUsageData> {
        return try {
            usageService.getCurrentUsage()
        } catch (e: Exception) {
            errorIntegration.reportError(e, "UsageTracking", "getCurrentUsage")
            emptyList()
        }
    }

    fun getTodayUsage(): List<AppUsageData> {
        return try {
            usageService.getTodayUsage()
        } catch (e: Exception) {
            errorIntegration.reportError(e, "UsageTracking", "getTodayUsage")
            emptyList()
        }
    }

    fun startMonitoring() {
        try {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                usageService.startMonitoring()
            }
        } catch (e: Exception) {
            errorIntegration.reportError(e, "UsageTracking", "startMonitoring")
        }
    }
}

/**
 * Wrapper for AppBlockingService with error handling
 */
class SafeAppBlockingService(private val context: Context) {
    private val blockingService = AppBlockingService(context)
    private val errorIntegration = ErrorIntegration(context)

    suspend fun blockApps(apps: List<String>) {
        try {
            blockingService.blockApps(apps)
        } catch (e: Exception) {
            // Report specific app blocking failures
            apps.forEach { app ->
                errorIntegration.reportBlockingFailure(app)
            }
        }
    }

    suspend fun unblockApps(apps: List<String>) {
        try {
            blockingService.unblockApps(apps)
        } catch (e: Exception) {
            errorIntegration.reportError(e, "AppBlocking", "unblockApps")
        }
    }

    suspend fun initializeBlocking() {
        try {
            blockingService.initializeBlocking()
        } catch (e: Exception) {
            errorIntegration.reportError(e, "AppBlocking", "initializeBlocking")
        }
    }
}

/**
 * Wrapper for GoalTrackingService with error handling
 */
class SafeGoalTrackingService(private val context: Context) {
    private val goalService = GoalTrackingService(context)
    private val errorIntegration = ErrorIntegration(context)

    fun saveGoal(goal: Goal) {
        try {
            goalService.saveGoal(goal)
        } catch (e: Exception) {
            errorIntegration.reportError(e, "GoalTracking", "saveGoal")
        }
    }

    fun getCurrentGoals(): List<Goal> {
        return try {
            goalService.getCurrentGoals()
        } catch (e: Exception) {
            errorIntegration.reportError(e, "GoalTracking", "getCurrentGoals")
            emptyList()
        }
    }

    fun updateGoalProgress(goalId: String, progress: Double) {
        try {
            goalService.updateGoalProgress(goalId, progress)
        } catch (e: Exception) {
            errorIntegration.reportError(e, "GoalTracking", "updateGoalProgress")
        }
    }
}

// MARK: - Data Classes

data class ErrorReport(
    val error: Exception,
    val service: String,
    val operation: String,
    val timestamp: Long,
    val context: Map<String, String>
)

data class ErrorContext(
    val service: String,
    val operation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalInfo: Map<String, Any> = emptyMap()
)

// MARK: - Error Reporter

object ErrorReporter {
    private var errorIntegration: ErrorIntegration? = null

    fun initialize(context: Context) {
        errorIntegration = ErrorIntegration(context)
    }

    fun report(error: Exception, context: ErrorContext) {
        errorIntegration?.reportError(error, context.service, context.operation)

        // Log additional context
        println("🔍 Error context: $context")
    }

    fun reportPermissionDenied(type: ErrorHandlingService.PermissionType) {
        errorIntegration?.reportPermissionError(type, denied = true)
    }

    fun reportPermissionRevoked(type: ErrorHandlingService.PermissionType) {
        errorIntegration?.reportPermissionError(type, denied = false)
    }

    fun reportBlockingFailure(appName: String) {
        errorIntegration?.reportBlockingFailure(appName)
    }

    fun reportDataCorruption() {
        errorIntegration?.reportDataCorruption()
    }

    fun reportNetworkError() {
        errorIntegration?.reportNetworkError()
    }
}

// MARK: - Extension Functions for AppBlockingService

fun AppBlockingService.validateBlockingStatus() {
    // This would check if the accessibility service is running
    // and if blocking is working correctly
    val isServiceRunning = isAccessibilityServiceEnabled()
    if (!isServiceRunning) {
        throw Exception("Accessibility service is not running")
    }
}

fun AppBlockingService.isAccessibilityServiceEnabled(): Boolean {
    // Implementation would check if the accessibility service is enabled
    val accessibilityService = AwayTimeAccessibilityService.getInstance()
    return accessibilityService != null
}