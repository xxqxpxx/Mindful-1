package com.awaytime.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Comprehensive error handling service for Awaytime Android
 */
class ErrorHandlingService(private val context: Context) : ViewModel() {

    var currentError by mutableStateOf<AwayTimeError?>(null)
        private set

    var showingErrorDialog by mutableStateOf(false)
        private set

    var showingRecoveryOptions by mutableStateOf(false)
        private set

    private val notificationService = NotificationService(context)

    // Event flows for UI integration
    private val _errorToastFlow = MutableSharedFlow<AwayTimeError>(replay = 0)
    val errorToastFlow: SharedFlow<AwayTimeError> = _errorToastFlow.asSharedFlow()

    // MARK: - Error Types

    sealed class AwayTimeError(
        val id: String,
        val title: String,
        val message: String,
        val recoverySuggestion: String,
        val severity: ErrorSeverity,
        val icon: String
    ) {
        data class PermissionDenied(val type: PermissionType) : AwayTimeError(
            id = "permission_denied_${type.name.lowercase()}",
            title = "Permission Required",
            message = getPermissionDeniedMessage(type),
            recoverySuggestion = getPermissionRecoverySuggestion(type),
            severity = ErrorSeverity.CRITICAL,
            icon = "🔒"
        )

        data class PermissionRevoked(val type: PermissionType) : AwayTimeError(
            id = "permission_revoked_${type.name.lowercase()}",
            title = "Permission Lost",
            message = getPermissionRevokedMessage(type),
            recoverySuggestion = "Please re-grant permission in Settings to continue using Awaytime.",
            severity = ErrorSeverity.CRITICAL,
            icon = "🔒"
        )

        data class BlockingFailed(val appName: String) : AwayTimeError(
            id = "blocking_failed",
            title = "App Blocking Failed",
            message = "Unable to block $appName. The app may still be accessible.",
            recoverySuggestion = "Try restarting the app or check your Accessibility settings.",
            severity = ErrorSeverity.HIGH,
            icon = "🚫"
        )

        object DataCorruption : AwayTimeError(
            id = "data_corruption",
            title = "Data Issue Detected",
            message = "Some of your data appears to be corrupted. We can help restore it.",
            recoverySuggestion = "We can reset your data to fix this issue. Your app selections will be preserved.",
            severity = ErrorSeverity.MEDIUM,
            icon = "💾"
        )

        object NetworkUnavailable : AwayTimeError(
            id = "network_unavailable",
            title = "No Internet Connection",
            message = "Some features require an internet connection to work properly.",
            recoverySuggestion = "Check your internet connection and try again.",
            severity = ErrorSeverity.LOW,
            icon = "📡"
        )

        data class SubscriptionError(val details: String) : AwayTimeError(
            id = "subscription_error",
            title = "Subscription Issue",
            message = details,
            recoverySuggestion = "Please check your subscription status or contact support.",
            severity = ErrorSeverity.LOW,
            icon = "💳"
        )

        data class UsageStatsError(val details: String) : AwayTimeError(
            id = "usage_stats_error",
            title = "Usage Tracking Issue",
            message = "Usage tracking may not work correctly: $details",
            recoverySuggestion = "Try restarting the app or re-granting Usage Access permissions.",
            severity = ErrorSeverity.HIGH,
            icon = "⚙️"
        )

        data class AccessibilityError(val details: String) : AwayTimeError(
            id = "accessibility_error",
            title = "Accessibility Service Issue",
            message = "App blocking may not work correctly: $details",
            recoverySuggestion = "Check your Accessibility settings and ensure Awaytime service is enabled.",
            severity = ErrorSeverity.HIGH,
            icon = "⚙️"
        )

        data class StorageError(val details: String) : AwayTimeError(
            id = "storage_error",
            title = "Storage Issue",
            message = "Unable to save your data: $details",
            recoverySuggestion = "Free up some storage space or restart the app.",
            severity = ErrorSeverity.MEDIUM,
            icon = "💾"
        )

        data class UnknownError(val details: String) : AwayTimeError(
            id = "unknown_error",
            title = "Unexpected Error",
            message = details,
            recoverySuggestion = "Please try restarting the app. If the problem persists, contact support.",
            severity = ErrorSeverity.MEDIUM,
            icon = "⚠️"
        )

        companion object {
            private fun getPermissionDeniedMessage(type: PermissionType): String {
                return when (type) {
                    PermissionType.USAGE_STATS ->
                        "Awaytime needs Usage Access permission to track your app usage and help you stay focused."

                    PermissionType.ACCESSIBILITY ->
                        "Accessibility Service permission is required for Awaytime to block apps when limits are reached."

                    PermissionType.NOTIFICATIONS ->
                        "Notifications help you stay on track with gentle reminders and achievement celebrations."

                    PermissionType.OVERLAY ->
                        "Display over other apps permission is needed to show blocking screens."
                }
            }

            private fun getPermissionRevokedMessage(type: PermissionType): String {
                return when (type) {
                    PermissionType.USAGE_STATS ->
                        "Usage Access permission was revoked. Awaytime can't track usage without this permission."

                    PermissionType.ACCESSIBILITY ->
                        "Accessibility Service was disabled. App blocking won't work without this service."

                    PermissionType.NOTIFICATIONS ->
                        "Notification permission was disabled. You won't receive usage alerts or achievement notifications."

                    PermissionType.OVERLAY ->
                        "Overlay permission was revoked. Blocking screens won't be displayed."
                }
            }

            private fun getPermissionRecoverySuggestion(type: PermissionType): String {
                return when (type) {
                    PermissionType.USAGE_STATS ->
                        "Go to Settings > Apps > Special access > Usage access and enable Awaytime."

                    PermissionType.ACCESSIBILITY ->
                        "Go to Settings > Accessibility > Awaytime and turn on the service."

                    PermissionType.NOTIFICATIONS ->
                        "Go to Settings > Apps > Awaytime > Notifications and enable them."

                    PermissionType.OVERLAY ->
                        "Go to Settings > Apps > Special access > Display over other apps and enable Awaytime."
                }
            }
        }
    }

    enum class PermissionType {
        USAGE_STATS,
        ACCESSIBILITY,
        NOTIFICATIONS,
        OVERLAY;

        val displayName: String
            get() = when (this) {
                USAGE_STATS -> "Usage Access"
                ACCESSIBILITY -> "Accessibility Service"
                NOTIFICATIONS -> "Notifications"
                OVERLAY -> "Display Over Apps"
            }
    }

    enum class ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL;

        val color: Color
            get() = when (this) {
                LOW -> Color.Blue
                MEDIUM -> Color(0xFFFF9500) // Orange
                HIGH -> Color.Red
                CRITICAL -> AwayTimeColors.primary
            }
    }

    // MARK: - Error Handling Methods

    fun handleError(error: AwayTimeError) {
        currentError = error
        showingErrorDialog = true

        // Log error for debugging
        logError(error)

        // Track error for analytics
        trackError(error)

        // Show toast for non-critical errors
        if (error.severity != ErrorSeverity.CRITICAL) {
            viewModelScope.launch {
                _errorToastFlow.emit(error)
            }
        }

        // Send notification for critical errors, but skip overlay permission errors
        if (error.severity == ErrorSeverity.CRITICAL && error.id != "permission_revoked_overlay") {
            sendCriticalErrorNotification(error)
        }

        println("🚨 Error handled: ${error.title}")
    }

    fun handlePermissionError(type: PermissionType, denied: Boolean = true) {
        val error = if (denied) {
            AwayTimeError.PermissionDenied(type)
        } else {
            AwayTimeError.PermissionRevoked(type)
        }
        handleError(error)
    }

    fun handleBlockingError(appName: String) {
        handleError(AwayTimeError.BlockingFailed(appName))
    }

    fun handleDataCorruption() {
        handleError(AwayTimeError.DataCorruption)
    }

    fun handleNetworkError() {
        handleError(AwayTimeError.NetworkUnavailable)
    }

    fun handleSubscriptionError(message: String) {
        handleError(AwayTimeError.SubscriptionError(message))
    }

    fun handleUsageStatsError(error: Exception) {
        handleError(AwayTimeError.UsageStatsError(error.message ?: "Unknown error"))
    }

    fun handleAccessibilityError(error: Exception) {
        handleError(AwayTimeError.AccessibilityError(error.message ?: "Unknown error"))
    }

    fun handleStorageError(error: Exception) {
        handleError(AwayTimeError.StorageError(error.message ?: "Unknown error"))
    }

    fun handleUnknownError(error: Exception) {
        handleError(AwayTimeError.UnknownError(error.message ?: "Unknown error"))
    }

    // MARK: - Recovery Actions

    fun performRecoveryAction(error: AwayTimeError) {
        when (error) {
            is AwayTimeError.PermissionDenied,
            is AwayTimeError.PermissionRevoked -> {
                val type = when (error) {
                    is AwayTimeError.PermissionDenied -> error.type
                    is AwayTimeError.PermissionRevoked -> error.type
                    else -> PermissionType.USAGE_STATS
                }
                openPermissionSettings(type)
            }

            is AwayTimeError.BlockingFailed -> {
                attemptBlockingRecovery()
            }

            is AwayTimeError.DataCorruption -> {
                showingRecoveryOptions = true
            }

            is AwayTimeError.NetworkUnavailable -> {
                // No action needed, user should check connection
            }

            is AwayTimeError.SubscriptionError -> {
                openSubscriptionSettings()
            }

            is AwayTimeError.UsageStatsError,
            is AwayTimeError.AccessibilityError -> {
                attemptSystemRecovery()
            }

            is AwayTimeError.StorageError -> {
                showStorageGuidance()
            }

            is AwayTimeError.UnknownError -> {
                restartApp()
            }
        }
    }

    private fun openPermissionSettings(type: PermissionType) {
        val intent = when (type) {
            PermissionType.USAGE_STATS -> {
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }

            PermissionType.ACCESSIBILITY -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }

            PermissionType.NOTIFICATIONS -> {
                Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }

            PermissionType.OVERLAY -> {
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun attemptBlockingRecovery() {
        viewModelScope.launch {
            try {
                val blockingService = AppBlockingService(context)
                blockingService.initializeBlocking()
                clearError()
                println("✅ Blocking recovery successful")
            } catch (e: Exception) {
                println("❌ Blocking recovery failed: ${e.message}")
            }
        }
    }

    private fun attemptSystemRecovery() {
        viewModelScope.launch {
            try {
                // Try to reinitialize services
                delay(1000) // Give system time
                clearError()
                println("✅ System recovery attempted")
            } catch (e: Exception) {
                println("❌ System recovery failed: ${e.message}")
            }
        }
    }

    private fun openSubscriptionSettings() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/account/subscriptions")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun showStorageGuidance() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        System.exit(0)
    }

    // MARK: - Data Recovery

    fun performDataReset() {
        viewModelScope.launch {
            try {
                // Create backup before any reset operations
                val backupService = DataBackupService(context)
                backupService.createBackup()
                
                val prefs = context.getSharedPreferences("awaytime_prefs", Context.MODE_PRIVATE)

                // Backup essential data
                val selectedApps = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()
                val currentGoals = prefs.getString("current_goals", "")

                // Clear corrupted data
                prefs.edit().clear().apply()

                // Restore essential data
                prefs.edit()
                    .putStringSet("selected_apps", selectedApps)
                    .putString("current_goals", currentGoals)
                    .apply()

                clearError()
                println("🔄 Data reset completed successfully with backup protection")
            } catch (e: Exception) {
                println("❌ Data reset failed, attempting restore from backup")
                // Attempt to restore from backup if reset fails
                try {
                    val backupService = DataBackupService(context)
                    if (backupService.restoreFromBackup()) {
                        clearError()
                        println("✅ Data restored from backup after reset failure")
                    } else {
                        handleStorageError(e)
                    }
                } catch (restoreError: Exception) {
                    handleStorageError(e)
                }
            }
        }
    }

    fun performFullReset() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("awaytime_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                // Clear other data stores
                context.deleteDatabase("awaytime_database")

                clearError()
                println("🔄 Full reset completed")
            } catch (e: Exception) {
                handleStorageError(e)
            }
        }
    }

    // MARK: - Error Utilities

    fun clearError() {
        currentError = null
        showingErrorDialog = false
        showingRecoveryOptions = false
    }

    private fun logError(error: AwayTimeError) {
        val timestamp = Date().toString()
        val logEntry = "[$timestamp] ERROR: ${error.id} - ${error.title}"

        // In production, this would go to a proper logging service
        println("📝 $logEntry")
    }

    private fun trackError(error: AwayTimeError) {
        // In production, this would send to analytics
        println("📊 Error tracked: ${error.id}")
    }

    private fun sendCriticalErrorNotification(error: AwayTimeError) {
        notificationService.sendNotification(
            title = "Awaytime Needs Attention",
            message = error.title,
            channelId = "critical_errors"
        )
    }

    // MARK: - Error Monitoring

    fun startErrorMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(300000) // Check every 5 minutes instead of 30 seconds
                checkSystemHealth()
            }
        }
    }

    private fun checkSystemHealth() {
        viewModelScope.launch {
            try {
                // Only do basic health checks - don't trigger permission error notifications
                // since those can be annoying to users
                val permissionService = PermissionService(context)

                // Just log permission status without triggering notifications
                if (!permissionService.hasUsageStatsPermission()) {
                    println("🔍 Usage stats permission not granted")
                }

                if (!permissionService.hasAccessibilityPermission()) {
                    println("🔍 Accessibility permission not granted")
                }

                // Check storage
                val prefs = context.getSharedPreferences("awaytime_prefs", Context.MODE_PRIVATE)
                prefs.getString("test_key", null) // Test read access

            } catch (e: Exception) {
                // Only handle real system errors, not permission issues
                if (e !is SecurityException) {
                    handleUnknownError(e)
                }
            }
        }
    }
}

// MARK: - Global Error Handler

object GlobalErrorHandler {
    private var errorService: ErrorHandlingService? = null

    fun initialize(context: Context) {
        errorService = ErrorHandlingService(context)
        setupGlobalErrorHandling()
    }

    private fun setupGlobalErrorHandling() {
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            errorService?.handleUnknownError(Exception(exception.message))
        }
    }

    fun getErrorService(): ErrorHandlingService? {
        return errorService
    }
}