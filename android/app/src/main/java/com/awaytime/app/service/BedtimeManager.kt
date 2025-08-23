/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.awaytime.app.models.BedtimeSettings
import com.awaytime.app.models.BedtimeState
import com.awaytime.app.models.DayOfWeek
import com.awaytime.app.models.ScheduledBlock
import com.awaytime.app.receivers.BedtimeReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

/**
 * Manages bedtime mode and scheduled blocking rules
 */
class BedtimeManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "BedtimeManager"
        private const val PREFS_NAME = "bedtime_settings"
        private const val KEY_BEDTIME_SETTINGS = "bedtime_settings"
        private const val KEY_SCHEDULED_BLOCKS = "scheduled_blocks"
        
        // Alarm request codes
        private const val BEDTIME_START_REQUEST_CODE = 1001
        private const val BEDTIME_END_REQUEST_CODE = 1002
        private const val WIND_DOWN_REQUEST_CODE = 1003
        
        @Volatile
        private var INSTANCE: BedtimeManager? = null
        
        fun getInstance(context: Context): BedtimeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BedtimeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val gson = Gson()
    
    // State management
    private val _bedtimeState = MutableStateFlow<BedtimeState>(BedtimeState.Inactive)
    val bedtimeState: StateFlow<BedtimeState> = _bedtimeState.asStateFlow()
    
    private val _bedtimeSettings = MutableStateFlow(loadBedtimeSettings())
    val bedtimeSettings: StateFlow<BedtimeSettings> = _bedtimeSettings.asStateFlow()
    
    private val _scheduledBlocks = MutableStateFlow(loadScheduledBlocks())
    val scheduledBlocks: StateFlow<List<ScheduledBlock>> = _scheduledBlocks.asStateFlow()
    
    init {
        // Check current state
        updateBedtimeState()
        
        // Schedule alarms for bedtime
        scheduleNextBedtimeAlarms()
    }
    
    /**
     * Updates bedtime settings
     */
    fun updateBedtimeSettings(settings: BedtimeSettings) {
        _bedtimeSettings.value = settings
        saveBedtimeSettings(settings)
        
        // Reschedule alarms
        cancelBedtimeAlarms()
        scheduleNextBedtimeAlarms()
        
        // Update current state
        updateBedtimeState()
        
        Log.d(TAG, "Bedtime settings updated: ${settings.isEnabled}")
    }
    
    /**
     * Adds or updates a scheduled block
     */
    fun updateScheduledBlock(block: ScheduledBlock) {
        val currentBlocks = _scheduledBlocks.value.toMutableList()
        val existingIndex = currentBlocks.indexOfFirst { it.id == block.id }
        
        if (existingIndex >= 0) {
            currentBlocks[existingIndex] = block
        } else {
            currentBlocks.add(block)
        }
        
        _scheduledBlocks.value = currentBlocks
        saveScheduledBlocks(currentBlocks)
        
        Log.d(TAG, "Scheduled block updated: ${block.name}")
    }
    
    /**
     * Removes a scheduled block
     */
    fun removeScheduledBlock(blockId: String) {
        val currentBlocks = _scheduledBlocks.value.toMutableList()
        currentBlocks.removeAll { it.id == blockId }
        
        _scheduledBlocks.value = currentBlocks
        saveScheduledBlocks(currentBlocks)
        
        Log.d(TAG, "Scheduled block removed: $blockId")
    }
    
    /**
     * Activates bedtime mode
     */
    fun activateBedtimeMode() {
        val settings = _bedtimeSettings.value
        if (!settings.isEnabled) return
        
        _bedtimeState.value = BedtimeState.Active
        
        // Apply bedtime restrictions
        if (settings.blockDistractions) {
            applyBedtimeBlocking(settings)
        }
        
        // Enable DND if configured
        if (settings.enableDnd) {
            enableDoNotDisturb(true)
        }
        
        // Dim notifications
        if (settings.dimNotifications) {
            // Implementation would depend on notification service
            Log.d(TAG, "Dimming notifications for bedtime")
        }
        
        Log.d(TAG, "Bedtime mode activated")
    }
    
    /**
     * Deactivates bedtime mode
     */
    fun deactivateBedtimeMode() {
        _bedtimeState.value = BedtimeState.Inactive
        
        // Remove bedtime restrictions
        removeBedtimeBlocking()
        
        // Disable DND
        enableDoNotDisturb(false)
        
        Log.d(TAG, "Bedtime mode deactivated")
    }
    
    /**
     * Activates wind down mode
     */
    fun activateWindDown() {
        val settings = _bedtimeSettings.value
        if (!settings.isEnabled || settings.windDownDurationMinutes <= 0) return
        
        _bedtimeState.value = BedtimeState.WindDown
        
        // Apply gentle restrictions during wind down
        // This could include dimming, limiting notifications, etc.
        Log.d(TAG, "Wind down mode activated")
    }
    
    /**
     * Gets currently active scheduled blocks
     */
    fun getActiveScheduledBlocks(): List<ScheduledBlock> {
        return _scheduledBlocks.value.filter { it.isActiveNow() }
    }
    
    /**
     * Checks if an app is allowed during bedtime
     */
    fun isAppAllowedDuringBedtime(packageName: String): Boolean {
        val settings = _bedtimeSettings.value
        val state = _bedtimeState.value
        
        if (!state.isActive()) return true
        if (!settings.blockDistractions) return true
        
        // Always allow emergency apps
        if (settings.allowEmergencyCalls && isEmergencyApp(packageName)) {
            return true
        }
        
        // Check allowed apps list
        return packageName in settings.allowedApps
    }
    
    /**
     * Forces bedtime mode off (emergency override)
     */
    fun emergencyOverride() {
        deactivateBedtimeMode()
        
        // Temporarily disable bedtime for 1 hour
        val settings = _bedtimeSettings.value.copy(isEnabled = false)
        updateBedtimeSettings(settings)
        
        // Schedule re-enable after 1 hour
        scheduleReEnableBedtime()
        
        Log.d(TAG, "Emergency override activated - bedtime disabled for 1 hour")
    }
    
    private fun updateBedtimeState() {
        val settings = _bedtimeSettings.value
        val now = LocalTime.now()
        
        when {
            !settings.isEnabled -> _bedtimeState.value = BedtimeState.Inactive
            settings.isWindDownActive(now) -> _bedtimeState.value = BedtimeState.WindDown
            settings.isActiveAt(now) -> _bedtimeState.value = BedtimeState.Active
            else -> _bedtimeState.value = BedtimeState.Inactive
        }
    }
    
    private fun scheduleNextBedtimeAlarms() {
        val settings = _bedtimeSettings.value
        if (!settings.isEnabled) return
        
        try {
            // Schedule wind down alarm
            if (settings.windDownDurationMinutes > 0) {
                val windDownTime = settings.startTime.minusMinutes(settings.windDownDurationMinutes.toLong())
                scheduleAlarm(windDownTime, WIND_DOWN_REQUEST_CODE, "WIND_DOWN")
            }
            
            // Schedule bedtime start alarm
            scheduleAlarm(settings.startTime, BEDTIME_START_REQUEST_CODE, "START")
            
            // Schedule bedtime end alarm
            scheduleAlarm(settings.endTime, BEDTIME_END_REQUEST_CODE, "END")
            
            Log.d(TAG, "Bedtime alarms scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule bedtime alarms", e)
        }
    }
    
    private fun scheduleAlarm(time: LocalTime, requestCode: Int, action: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minute)
        calendar.set(Calendar.SECOND, 0)
        
        // If time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val intent = Intent(context, BedtimeReceiver::class.java).apply {
            putExtra("action", action)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    
    private fun cancelBedtimeAlarms() {
        val requestCodes = listOf(
            BEDTIME_START_REQUEST_CODE,
            BEDTIME_END_REQUEST_CODE,
            WIND_DOWN_REQUEST_CODE
        )
        
        requestCodes.forEach { requestCode ->
            val intent = Intent(context, BedtimeReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    private fun applyBedtimeBlocking(settings: BedtimeSettings) {
        // Get all installed apps except allowed ones
        val allApps = getAllInstalledApps()
        val blockedApps = allApps.filter { it !in settings.allowedApps }.toSet()
        
        // Apply blocking through enhanced service manager
        try {
            val enhancedServiceManager = EnhancedServiceManager.getInstance(context, 
                com.awaytime.app.data.repository.AwayTimeRepository(context))
            enhancedServiceManager.updateBlockedApps(blockedApps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply bedtime blocking", e)
        }
    }
    
    private fun removeBedtimeBlocking() {
        try {
            val enhancedServiceManager = EnhancedServiceManager.getInstance(context,
                com.awaytime.app.data.repository.AwayTimeRepository(context))
            enhancedServiceManager.updateBlockedApps(emptySet())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove bedtime blocking", e)
        }
    }
    
    private fun enableDoNotDisturb(enable: Boolean) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val interruptionFilter = if (enable) {
                    android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(interruptionFilter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle DND", e)
        }
    }
    
    private fun isEmergencyApp(packageName: String): Boolean {
        val emergencyApps = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.android.phone",
            "com.google.android.dialer",
            "android"
        )
        return packageName in emergencyApps
    }
    
    private fun getAllInstalledApps(): List<String> {
        return try {
            val packageManager = context.packageManager
            packageManager.getInstalledApplications(0)
                .map { it.packageName }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed apps", e)
            emptyList()
        }
    }
    
    private fun scheduleReEnableBedtime() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 1) // Re-enable after 1 hour
        
        val intent = Intent(context, BedtimeReceiver::class.java).apply {
            putExtra("action", "RE_ENABLE")
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    
    private fun loadBedtimeSettings(): BedtimeSettings {
        return try {
            val json = prefs.getString(KEY_BEDTIME_SETTINGS, null)
            if (json != null) {
                gson.fromJson(json, BedtimeSettings::class.java)
            } else {
                BedtimeSettings() // Default settings
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bedtime settings", e)
            BedtimeSettings()
        }
    }
    
    private fun saveBedtimeSettings(settings: BedtimeSettings) {
        try {
            val json = gson.toJson(settings)
            prefs.edit().putString(KEY_BEDTIME_SETTINGS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bedtime settings", e)
        }
    }
    
    private fun loadScheduledBlocks(): List<ScheduledBlock> {
        return try {
            val json = prefs.getString(KEY_SCHEDULED_BLOCKS, null)
            if (json != null) {
                val type = object : TypeToken<List<ScheduledBlock>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load scheduled blocks", e)
            emptyList()
        }
    }
    
    private fun saveScheduledBlocks(blocks: List<ScheduledBlock>) {
        try {
            val json = gson.toJson(blocks)
            prefs.edit().putString(KEY_SCHEDULED_BLOCKS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save scheduled blocks", e)
        }
    }
}