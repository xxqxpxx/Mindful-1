package com.awaytime.app.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionStatus {
    NOT_REQUESTED,
    NOT_DETERMINED,
    DENIED,
    GRANTED
}

class PermissionService(private val context: Context) {
    
    // State properties with backing fields
    private var _usageStatsPermissionStatus = PermissionStatus.NOT_DETERMINED
    val usageStatsPermissionStatus: PermissionStatus
        get() = _usageStatsPermissionStatus

    private var _accessibilityPermissionStatus = PermissionStatus.NOT_DETERMINED
    val accessibilityPermissionStatus: PermissionStatus
        get() = _accessibilityPermissionStatus

    private var _overlayPermissionStatus = PermissionStatus.NOT_DETERMINED
    val overlayPermissionStatus: PermissionStatus
        get() = _overlayPermissionStatus

    private var _isRequestingPermission = false
    val isRequestingPermission: Boolean
        get() = _isRequestingPermission

    private var _permissionError: String? = null
    val permissionError: String?
        get() = _permissionError

    // Callback for state changes
    var onPermissionStatusChanged: ((PermissionService) -> Unit)? = null

    init {
        loadPermissionStates() // Load saved states first
        updatePermissionStatuses()
    }

    // MARK: - Permission Status Checks

    fun updatePermissionStatuses() {
        val oldUsageStats = _usageStatsPermissionStatus
        val oldAccessibility = _accessibilityPermissionStatus
        val oldOverlay = _overlayPermissionStatus

        _usageStatsPermissionStatus = if (hasUsageStatsPermission()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }

        _accessibilityPermissionStatus = if (hasAccessibilityPermission() && isAccessibilityServiceRunning()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }

        _overlayPermissionStatus = if (hasOverlayPermission()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }

        // Save permission states to prevent loss
        savePermissionStates()

        // Notify if status changed
        if (oldUsageStats != _usageStatsPermissionStatus || oldAccessibility != _accessibilityPermissionStatus || oldOverlay != _overlayPermissionStatus) {
            println("🔍 Permission status changed - Usage: $oldUsageStats -> $_usageStatsPermissionStatus, Accessibility: $oldAccessibility -> $_accessibilityPermissionStatus, Overlay: $oldOverlay -> $_overlayPermissionStatus")
            onPermissionStatusChanged?.invoke(this)
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: run {
                    println("⚠️ AppOpsManager is null")
                    return false
                }
                
            val packageName = context.packageName
            if (packageName.isNullOrBlank()) {
                println("⚠️ Package name is null or blank")
                return false
            }
            
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            val hasPermission = mode == AppOpsManager.MODE_ALLOWED
            println("🔍 Usage stats permission check: ${if (hasPermission) "GRANTED" else "DENIED"}")
            return hasPermission
        } catch (e: Exception) {
            println("❌ Error checking usage stats permission: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: SecurityException) {
            println("❌ Security exception checking usage stats permission: ${e.message}")
            false
        } catch (e: Throwable) {
            println("❌ Unexpected error checking usage stats permission: ${e.message}")
            false
        }
    }

    fun hasAccessibilityPermission(): Boolean {
        return try {
            val contentResolver = context.contentResolver
                ?: run {
                    println("⚠️ ContentResolver is null")
                    return false
                }
            
            val accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )

            if (accessibilityEnabled == 1) {
                val services = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                
                // Log for debugging
                println("🔍 Enabled accessibility services: $services")
                
                // Null check for services string
                if (services.isNullOrBlank()) {
                    println("⚠️ Accessibility services string is null or blank")
                    return false
                }
                
                val packageName = context.packageName
                if (packageName.isNullOrBlank()) {
                    println("⚠️ Package name is null or blank")
                    return false
                }
                
                // Check multiple possible service name formats with null safety
                val possibleServiceNames = listOf(
                    "$packageName/.service.AwayTimeAccessibilityService",
                    "$packageName/com.awaytime.app.service.AwayTimeAccessibilityService",
                    "com.awaytime.app.service.AwayTimeAccessibilityService",
                    "$packageName:service/AwayTimeAccessibilityService",
                    "com.awaytime.app/.service.AwayTimeAccessibilityService"
                ).filter { it.isNotBlank() } // Filter out any blank names
                
                val hasPermission = possibleServiceNames.any { serviceName ->
                    try {
                        val isEnabled = services.contains(serviceName)
                        if (isEnabled) {
                            println("✅ Found accessibility service with name: $serviceName")
                        }
                        isEnabled
                    } catch (e: Exception) {
                        println("❌ Error checking service name '$serviceName': ${e.message}")
                        false
                    }
                }
                
                // Cache the permission state for more stable checking with error handling
                try {
                    val prefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
                    if (hasPermission) {
                        prefs.edit().putBoolean("accessibility_granted", true)
                            .putLong("accessibility_last_check", System.currentTimeMillis())
                            .apply()
                    }
                } catch (e: Exception) {
                    println("⚠️ Error caching accessibility permission state: ${e.message}")
                }
                
                return hasPermission
            }

            // Check cached state if settings are disabled (might be temporary)
            try {
                val prefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
                val cachedGranted = prefs.getBoolean("accessibility_granted", false)
                val lastCheck = prefs.getLong("accessibility_last_check", 0)
                val isRecentCheck = System.currentTimeMillis() - lastCheck < 30_000 // 30 seconds
                
                if (cachedGranted && isRecentCheck) {
                    println("🔍 Using cached accessibility permission state (granted)")
                    return true
                }
            } catch (e: Exception) {
                println("⚠️ Error reading cached accessibility permission state: ${e.message}")
            }

            return false
        } catch (e: SecurityException) {
            println("❌ Security exception checking accessibility permission: ${e.message}")
            false
        } catch (e: Exception) {
            println("❌ Error checking accessibility permission: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Throwable) {
            println("❌ Unexpected error checking accessibility permission: ${e.message}")
            false
        }
    }

    fun isAccessibilityServiceRunning(): Boolean {
        return try {
            val serviceInstance = AwayTimeAccessibilityService.getInstance()
            val isRunning = serviceInstance != null
            println("🔍 Accessibility service instance check: ${if (isRunning) "RUNNING" else "NOT RUNNING"}")
            isRunning
        } catch (e: Exception) {
            println("❌ Error checking accessibility service instance: ${e.message}")
            false
        }
    }

    fun hasNotificationPermission(): Boolean {
        // For API level 33+ (Android 13), we need to check notification permission
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // For older versions, notification permission is granted by default
            true
        }
    }

    fun hasOverlayPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true // Permission not required for older Android versions
        }
    }

    // MARK: - Permission Properties

    val isFullyAuthorized: Boolean
        get() = usageStatsPermissionStatus == PermissionStatus.GRANTED &&
                accessibilityPermissionStatus == PermissionStatus.GRANTED
                // Note: We don't check overlay permission here as it's not critical for core functionality

    val needsUsageStatsPermission: Boolean
        get() = usageStatsPermissionStatus != PermissionStatus.GRANTED

    val needsAccessibilityPermission: Boolean
        get() = accessibilityPermissionStatus != PermissionStatus.GRANTED

    val needsAnyPermission: Boolean
        get() = needsUsageStatsPermission || needsAccessibilityPermission || needsOverlayPermission

    val needsOverlayPermission: Boolean
        get() = !hasOverlayPermission()

    // MARK: - Permission Requests

    fun requestOverlayPermission() {
        try {
            _isRequestingPermission = true
            _permissionError = null

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }

        } catch (e: Exception) {
            _permissionError = "Failed to open overlay permission settings: ${e.message}"
        } finally {
            _isRequestingPermission = false
        }
    }

    fun requestUsageStatsPermission() {
        try {
            _isRequestingPermission = true
            _permissionError = null

            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        } catch (e: Exception) {
            _permissionError = "Failed to open usage stats settings: ${e.message}"
        } finally {
            _isRequestingPermission = false
        }
    }

    fun requestAccessibilityPermission() {
        try {
            _isRequestingPermission = true
            _permissionError = null

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        } catch (e: Exception) {
            _permissionError = "Failed to open accessibility settings: ${e.message}"
        } finally {
            _isRequestingPermission = false
        }
    }

    // MARK: - Permission Guidance

    fun getUsageStatsGuidanceText(): String {
        return when (usageStatsPermissionStatus) {
            PermissionStatus.NOT_REQUESTED,
            PermissionStatus.NOT_DETERMINED ->
                "Awaytime needs access to usage statistics to track your app usage and help you stay within your limits."

            PermissionStatus.DENIED ->
                "Please enable usage access for Awaytime in Settings > Apps > Special access > Usage access."

            PermissionStatus.GRANTED ->
                "Great! Usage stats permission is granted."
        }
    }

    fun getAccessibilityGuidanceText(): String {
        return when (accessibilityPermissionStatus) {
            PermissionStatus.NOT_REQUESTED,
            PermissionStatus.NOT_DETERMINED ->
                "Awaytime needs accessibility access to block apps when you reach your daily limit."

            PermissionStatus.DENIED ->
                "Please enable accessibility service for Awaytime in Settings > Accessibility > Awaytime."

            PermissionStatus.GRANTED ->
                "Great! Accessibility permission is granted."
        }
    }

    fun getUsageStatsStatusText(): String {
        return when (usageStatsPermissionStatus) {
            PermissionStatus.NOT_REQUESTED -> "Permission not requested"
            PermissionStatus.NOT_DETERMINED -> "Permission pending"
            PermissionStatus.DENIED -> "Permission denied"
            PermissionStatus.GRANTED -> "Permission granted"
        }
    }

    fun getAccessibilityStatusText(): String {
        return when (accessibilityPermissionStatus) {
            PermissionStatus.NOT_REQUESTED -> "Permission not requested"
            PermissionStatus.NOT_DETERMINED -> "Permission pending"
            PermissionStatus.DENIED -> "Permission denied"
            PermissionStatus.GRANTED -> "Permission granted"
        }
    }

    fun getOverlayGuidanceText(): String {
        return when (overlayPermissionStatus) {
            PermissionStatus.NOT_REQUESTED,
            PermissionStatus.NOT_DETERMINED ->
                "Awaytime needs permission to display over other apps to show blocking screens effectively."

            PermissionStatus.DENIED ->
                "Please enable the 'Display over other apps' permission for Awaytime in your system settings."

            PermissionStatus.GRANTED ->
                "Great! 'Display over other apps' permission is granted."
        }
    }

    fun getOverlayStatusText(): String {
        return when (overlayPermissionStatus) {
            PermissionStatus.NOT_REQUESTED -> "Permission not requested"
            PermissionStatus.NOT_DETERMINED -> "Permission pending"
            PermissionStatus.DENIED -> "Permission denied"
            PermissionStatus.GRANTED -> "Permission granted"
        }
    }
    
    // MARK: - Permission State Persistence
    
    private fun savePermissionStates() {
        try {
            val prefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("usage_stats_status", _usageStatsPermissionStatus.name)
                .putString("accessibility_status", _accessibilityPermissionStatus.name)
                .putString("overlay_status", _overlayPermissionStatus.name)
                .putLong("last_permission_save", System.currentTimeMillis())
                .putInt("permission_save_version", 1)
                .apply()
        } catch (e: Exception) {
            println("⚠️ Failed to save permission states: ${e.message}")
        }
    }
    
    private fun loadPermissionStates() {
        try {
            val prefs = context.getSharedPreferences("awaytime_permissions", Context.MODE_PRIVATE)
            val lastSave = prefs.getLong("last_permission_save", 0)
            
            // Only load if we have recent saves (within last 24 hours)
            if (System.currentTimeMillis() - lastSave < 24 * 60 * 60 * 1000) {
                val usageStatsStatus = prefs.getString("usage_stats_status", null)
                val accessibilityStatus = prefs.getString("accessibility_status", null)
                val overlayStatus = prefs.getString("overlay_status", null)
                
                usageStatsStatus?.let { 
                    _usageStatsPermissionStatus = PermissionStatus.valueOf(it)
                }
                accessibilityStatus?.let {
                    _accessibilityPermissionStatus = PermissionStatus.valueOf(it)
                }
                overlayStatus?.let {
                    _overlayPermissionStatus = PermissionStatus.valueOf(it)
                }
                
                println("✅ Loaded permission states from storage")
            }
        } catch (e: Exception) {
            println("⚠️ Failed to load permission states, using defaults: ${e.message}")
            // Keep default values if loading fails
        }
    }
}