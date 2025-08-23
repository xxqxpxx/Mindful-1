package com.awaytime.app.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.data.entity.AppGroupEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

// Data class for blocking schedule
data class BlockingSchedule(
    val appGroupName: String,
    val startTime: Calendar,
    val endTime: Calendar,
    val daysOfWeek: Set<Int>,
    val isEnabled: Boolean = true
)


enum class BlockingStatus {
    ALLOWED,
    BLOCKED_LIMIT_REACHED,
    BLOCKED_SCHEDULED,
    SHOULD_BLOCK_LIMIT,
    SHOULD_BLOCK_SCHEDULED
}

data class BlockingState(
    val isBlocking: Boolean,
    val blockedAppGroups: List<String>,
    val blockingSchedule: BlockingSchedule?
)

data class BlockingEvent(
    val appGroupName: String,
    val action: String, // "blocked", "unblocked", "emergency_override"
    val timestamp: Long,
    val duration: Long? = null
)

data class BlockingStats(
    val totalBlockingEvents: Int,
    val emergencyOverrides: Int,
    val averageBlockDuration: Long,
    val currentlyBlockedGroups: Int
) {
    fun getFormattedAverageBlockDuration(): String {
        val hours = averageBlockDuration / (1000 * 60 * 60)
        val minutes = (averageBlockDuration % (1000 * 60 * 60)) / (1000 * 60)

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

class AppBlockingService(private val context: Context) {

    companion object {
        private const val TAG = "AppBlockingService"
        private const val PREFS_NAME = "app_blocking_prefs"
        private const val KEY_IS_BLOCKING = "is_blocking"
        private const val KEY_BLOCKED_GROUPS = "blocked_groups"
        private const val KEY_BLOCKING_SCHEDULE = "blocking_schedule"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Service dependencies
    private val notificationService = NotificationService(context)
    private val serviceMonitor = AccessibilityServiceMonitor.getInstance(context)
    
    // Repository for data access
    private val repository = com.awaytime.app.data.repository.AwayTimeRepository(context)

    // State properties with backing fields
    private var _isBlocking = false
    val isBlocking: Boolean
        get() = _isBlocking

    private var _blockedAppGroups = emptyList<String>()
    val blockedAppGroups: List<String>
        get() = _blockedAppGroups

    private var _blockingSchedule: BlockingSchedule? = null
    val blockingSchedule: BlockingSchedule?
        get() = _blockingSchedule

    // Callback for state changes
    var onBlockingStateChanged: ((AppBlockingService) -> Unit)? = null

    init {
        loadBlockingState()
        setupServiceMonitoring()
    }

    // MARK: - Initialization
    
    fun initializeBlocking() {
        Log.d(TAG, "Initializing app blocking service")
        
        // Load saved blocking state
        loadBlockingState()
        
        // Start monitoring accessibility service
        serviceMonitor.startMonitoring()
        
        // Check if accessibility service is running
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Accessibility service not enabled - app blocking may not work properly")
            notificationService.sendAccessibilityServiceDisabledNotification()
        } else {
            Log.d(TAG, "Accessibility service is running")
            
            // Restore blocked packages if there are any active blocks
            if (_isBlocking && _blockedAppGroups.isNotEmpty()) {
                Log.d(TAG, "Restoring blocked apps: ${_blockedAppGroups.size} groups")
                restoreBlockedAppsFromState()
            }
        }
        
        Log.d(TAG, "App blocking service initialized successfully")
    }
    
    private fun setupServiceMonitoring() {
        serviceMonitor.onServiceDisabled = {
            Log.w(TAG, "Accessibility service was disabled - notifying user")
            notificationService.sendAccessibilityServiceDisabledNotification()
            
            // Update internal state but don't clear saved state
            // so we can restore when service comes back
            _isBlocking = false
        }
        
        serviceMonitor.onServiceEnabled = {
            Log.d(TAG, "Accessibility service was re-enabled - restoring blocked apps")
            
            // Restore blocked apps from saved state
            if (_blockedAppGroups.isNotEmpty()) {
                scope.launch {
                    restoreBlockedAppsFromState()
                }
            }
        }
    }
    
    private fun restoreBlockedAppsFromState() {
        scope.launch {
            restoreBlockedAppsFromStateInternal()
        }
    }
    
    private suspend fun restoreBlockedAppsFromStateInternal() {
        try {
            // Get all app groups and restore blocking for saved groups
            val allAppGroups = repository.getAllAppGroupsSync()
            
            for (groupName in _blockedAppGroups) {
                val appGroup = allAppGroups.find { it.name == groupName }
                if (appGroup != null) {
                    val accessibilityService = AwayTimeAccessibilityService.getInstance()
                    if (accessibilityService != null) {
                        accessibilityService.setBlockedPackages(appGroup.getSelectedApps().toSet())
                        Log.d(TAG, "Restored blocking for group: $groupName")
                    }
                }
            }
            
            _isBlocking = _blockedAppGroups.isNotEmpty()
            Log.d(TAG, "Successfully restored ${_blockedAppGroups.size} blocked app groups")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore blocked apps from state", e)
        }
    }

    // MARK: - App Blocking Control

    suspend fun blockApps(appGroup: AppGroupEntity) {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()

        if (accessibilityService != null) {
            // Set blocked packages in accessibility service
            accessibilityService.setBlockedPackages(appGroup.getSelectedApps().toSet())

            // Update state
            _isBlocking = true
            _blockedAppGroups = _blockedAppGroups + appGroup.name

            // Save state
            saveBlockingState()

            // Send notification
            notificationService.sendLimitReachedNotification(appGroup.name)

            // Log event
            logBlockingEvent(appGroup.name, "blocked")

            println("✅ Apps blocked for group: ${appGroup.name}")
        } else {
            println("❌ Accessibility service not available for blocking")
        }
    }
    
    // Overloaded method for List<String>
    suspend fun blockApps(packageNames: List<String>) {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()

        if (accessibilityService != null) {
            // Set blocked packages in accessibility service
            accessibilityService.setBlockedPackages(packageNames.toSet())

            // Update state
            _isBlocking = true
            _blockedAppGroups = _blockedAppGroups + "manual_block"

            // Save state
            saveBlockingState()

            // Log event
            logBlockingEvent("manual_block", "blocked")

            println("✅ Apps blocked: ${packageNames.joinToString()}")
        } else {
            println("❌ Accessibility service not available for blocking")
        }
    }

    suspend fun unblockApps(appGroup: AppGroupEntity) {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()

        if (accessibilityService != null) {
            // Remove blocked packages from accessibility service
            accessibilityService.removeBlockedPackages(appGroup.getSelectedApps().toSet())

            // Update state
            _blockedAppGroups = _blockedAppGroups.filter { it != appGroup.name }
            _isBlocking = _blockedAppGroups.isNotEmpty()

            // Save state
            saveBlockingState()

            // Log event
            logBlockingEvent(appGroup.name, "unblocked")

            println("✅ Apps unblocked for group: ${appGroup.name}")
        } else {
            println("❌ Accessibility service not available for unblocking")
        }
    }
    
    // Overloaded method for List<String>
    suspend fun unblockApps(packageNames: List<String>) {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()

        if (accessibilityService != null) {
            // Remove blocked packages from accessibility service
            accessibilityService.removeBlockedPackages(packageNames.toSet())

            // Update state
            _blockedAppGroups = _blockedAppGroups.filter { it != "manual_block" }
            _isBlocking = _blockedAppGroups.isNotEmpty()

            // Save state
            saveBlockingState()

            // Log event
            logBlockingEvent("manual_block", "unblocked")

            println("✅ Apps unblocked: ${packageNames.joinToString()}")
        } else {
            println("❌ Accessibility service not available for unblocking")
        }
    }

    fun unblockAllApps() {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()

        if (accessibilityService != null) {
            accessibilityService.clearBlockedPackages()

            // Update state
            _isBlocking = false
            _blockedAppGroups = emptyList()

            // Save state
            saveBlockingState()

            println("✅ All apps unblocked")
        }
    }

    // MARK: - Scheduled Blocking

    fun scheduleBlocking(appGroup: AppGroupEntity, schedule: BlockingSchedule) {
        _blockingSchedule = schedule

        _blockingSchedule = schedule
        saveBlockingState()
        println("✅ Blocking scheduled for ${appGroup.name}")
    }

    fun cancelScheduledBlocking() {
        _blockingSchedule = null
        saveBlockingState()
        println("✅ Scheduled blocking cancelled")
    }

    // MARK: - Blocking Status

    fun isAppGroupBlocked(appGroupName: String): Boolean {
        return _blockedAppGroups.contains(appGroupName)
    }

    suspend fun getBlockingStatus(appGroup: AppGroupEntity): BlockingStatus {
        val isCurrentlyBlocked = isAppGroupBlocked(appGroup.name)
        val todayUsage = repository.getTodayUsage(appGroup.name).toLong()
        val dailyLimit = appGroup.dailyLimitMinutes.toLong()

        return when {
            isCurrentlyBlocked -> BlockingStatus.BLOCKED_LIMIT_REACHED
            todayUsage >= dailyLimit -> BlockingStatus.SHOULD_BLOCK_LIMIT
            _blockingSchedule?.isActiveNow() == true -> BlockingStatus.SHOULD_BLOCK_SCHEDULED
            else -> BlockingStatus.ALLOWED
        }
    }

    // MARK: - Emergency Override

    fun requestEmergencyOverride(appGroup: AppGroupEntity, durationMinutes: Int = 5) {
        // Temporarily unblock apps and schedule re-blocking
        CoroutineScope(Dispatchers.Main).launch {
            unblockApps(appGroup)
            
            // Schedule re-blocking after the override period
            kotlinx.coroutines.delay(durationMinutes * 60 * 1000L)
            blockApps(appGroup)
        }

        // Log emergency override
        logBlockingEvent(appGroup.name, "emergency_override", durationMinutes * 60 * 1000L)

        println("✅ Emergency override granted for ${appGroup.name} for $durationMinutes minutes")
    }

    // MARK: - Automatic Blocking Check

    suspend fun checkAndApplyBlocking() {
        val appGroups = repository.getActiveAppGroups().first()
        for (appGroup in appGroups) {
            val status = getBlockingStatus(appGroup)

            when (status) {
                BlockingStatus.SHOULD_BLOCK_LIMIT,
                BlockingStatus.SHOULD_BLOCK_SCHEDULED -> {
                    if (!isAppGroupBlocked(appGroup.name)) {
                        blockApps(appGroup)
                    }
                }

                BlockingStatus.ALLOWED -> {
                    if (isAppGroupBlocked(appGroup.name)) {
                        unblockApps(appGroup)
                    }
                }

                else -> {
                    // Already in correct state
                }
            }
        }
    }

    // MARK: - Blocking Analytics

    fun getBlockingStats(): BlockingStats {
        val logs = getBlockingLogs()
        val totalBlocks = logs.count { it.action == "blocked" }
        val totalOverrides = logs.count { it.action == "emergency_override" }
        val averageBlockDuration = calculateAverageBlockDuration(logs)

        return BlockingStats(
            totalBlockingEvents = totalBlocks,
            emergencyOverrides = totalOverrides,
            averageBlockDuration = averageBlockDuration,
            currentlyBlockedGroups = _blockedAppGroups.size
        )
    }

    // MARK: - Data Persistence

    private fun saveBlockingState() {
        prefs.edit()
            .putBoolean(KEY_IS_BLOCKING, _isBlocking)
            .putStringSet(KEY_BLOCKED_GROUPS, _blockedAppGroups.toSet())
            .apply()

        // Save blocking schedule if exists
        _blockingSchedule?.let { schedule ->
            prefs.edit()
                .putInt("schedule_start_hour", schedule.startTime.get(Calendar.HOUR_OF_DAY))
                .putInt("schedule_start_minute", schedule.startTime.get(Calendar.MINUTE))
                .putInt("schedule_end_hour", schedule.endTime.get(Calendar.HOUR_OF_DAY))
                .putInt("schedule_end_minute", schedule.endTime.get(Calendar.MINUTE))
                .putBoolean("schedule_repeats", schedule.isEnabled)
                .apply()
        } ?: run {
            prefs.edit()
                .remove("schedule_start_hour")
                .remove("schedule_start_minute")
                .remove("schedule_end_hour")
                .remove("schedule_end_minute")
                .remove("schedule_repeats")
                .apply()
        }
    }

    private fun loadBlockingState() {
        _isBlocking = prefs.getBoolean(KEY_IS_BLOCKING, false)
        _blockedAppGroups =
            prefs.getStringSet(KEY_BLOCKED_GROUPS, emptySet())?.toList() ?: emptyList()

        // Load blocking schedule if exists
        if (prefs.contains("schedule_start_hour")) {
            // Load saved days of week
            val savedDays = prefs.getStringSet("schedule_days", emptySet()) ?: emptySet()
            val daysOfWeek = savedDays.mapNotNull { dayString ->
                try {
                    dayString.toInt()
                } catch (e: Exception) {
                    null
                }
            }.toSet()
            
            _blockingSchedule = BlockingSchedule(
                appGroupName = prefs.getString("schedule_app_group", "My Apps") ?: "My Apps",
                startTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, prefs.getInt("schedule_start_hour", 0))
                    set(Calendar.MINUTE, prefs.getInt("schedule_start_minute", 0))
                },
                endTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, prefs.getInt("schedule_end_hour", 23))
                    set(Calendar.MINUTE, prefs.getInt("schedule_end_minute", 0))
                },
                daysOfWeek = daysOfWeek,
                isEnabled = prefs.getBoolean("schedule_repeats", false)
            )
        }
    }

    private fun logBlockingEvent(appGroupName: String, action: String, duration: Long? = null) {
        val event = BlockingEvent(
            appGroupName = appGroupName,
            action = action,
            timestamp = System.currentTimeMillis(),
            duration = duration
        )

        val logs = getBlockingLogs().toMutableList()
        logs.add(event)

        // Keep only last 100 events
        if (logs.size > 100) {
            logs.removeAt(0)
        }

        // Save logs (simplified - in a real app you'd use Room database)
        val logsJson = com.google.gson.Gson().toJson(logs)
        prefs.edit().putString("blocking_logs", logsJson).apply()
    }

    private fun getBlockingLogs(): List<BlockingEvent> {
        val logsJson = prefs.getString("blocking_logs", "[]") ?: "[]"
        return try {
            com.google.gson.Gson().fromJson(logsJson, Array<BlockingEvent>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateAverageBlockDuration(logs: List<BlockingEvent>): Long {
        val blockEvents = logs.filter { it.action == "blocked" }
        val unblockEvents = logs.filter { it.action == "unblocked" }

        var totalDuration = 0L
        var pairCount = 0

        for (blockEvent in blockEvents) {
            val unblockEvent = unblockEvents.find {
                it.appGroupName == blockEvent.appGroupName &&
                        it.timestamp > blockEvent.timestamp
            }

            if (unblockEvent != null) {
                totalDuration += unblockEvent.timestamp - blockEvent.timestamp
                pairCount++
            }
        }

        return if (pairCount > 0) totalDuration / pairCount else 0L
    }

    // MARK: - Utility Methods

    fun isAccessibilityServiceEnabled(): Boolean {
        return AwayTimeAccessibilityService.isServiceRunning()
    }

    fun getBlockedAppsCount(): Int {
        return AwayTimeAccessibilityService.getInstance()?.getBlockedPackages()?.size ?: 0
    }

    fun clearAllBlocking() {
        val accessibilityService = AwayTimeAccessibilityService.getInstance()
        if (accessibilityService != null) {
            accessibilityService.clearBlockedPackages()
            _isBlocking = false
            _blockedAppGroups = emptyList()
            saveBlockingState()
            logBlockingEvent("ALL", "cleared")
            println("✅ All blocking cleared")
        }
    }
    
    // MARK: - Service Lifecycle Management
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up AppBlockingService")
        serviceMonitor.stopMonitoring()
        scope.cancel()
    }
    
    // MARK: - Missing Methods for Core Integration
    
    fun getBlockedApps(): Set<String> {
        return AwayTimeAccessibilityService.getInstance()?.getBlockedPackages() ?: emptySet()
    }
    
    fun isAppBlocked(packageName: String): Boolean {
        return AwayTimeAccessibilityService.getInstance()?.getBlockedPackages()?.contains(packageName) ?: false
    }
    
    fun isBlockingActive(): Boolean {
        return _isBlocking && AwayTimeAccessibilityService.isServiceRunning()
    }
    
    suspend fun setScheduledBlock(packageNames: List<String>, durationMinutes: Int) {
        val endTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, durationMinutes)
        }
        
        val schedule = BlockingSchedule(
            appGroupName = "scheduled",
            startTime = Calendar.getInstance(),
            endTime = endTime,
            daysOfWeek = setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)),
            isEnabled = true
        )
        
        _blockingSchedule = schedule
        blockApps(packageNames)
        
        // Schedule unblocking
        scope.launch {
            delay(durationMinutes * 60 * 1000L)
            unblockApps(packageNames)
            _blockingSchedule = null
        }
    }
    
    fun clearScheduledBlocks() {
        _blockingSchedule = null
        saveBlockingState()
    }
    
}

// Extension method for BlockingSchedule
fun BlockingSchedule.isActiveNow(): Boolean {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    
    // Check if today is in the schedule
    if (!daysOfWeek.contains(currentDayOfWeek)) {
        return false
    }
    
    val currentTimeMinutes = currentHour * 60 + currentMinute
    val startTimeMinutes = startTime.get(Calendar.HOUR_OF_DAY) * 60 + startTime.get(Calendar.MINUTE)
    val endTimeMinutes = endTime.get(Calendar.HOUR_OF_DAY) * 60 + endTime.get(Calendar.MINUTE)
    
    return if (startTimeMinutes <= endTimeMinutes) {
        // Same day schedule
        currentTimeMinutes in startTimeMinutes..endTimeMinutes
    } else {
        // Schedule crosses midnight
        currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes
    }
}