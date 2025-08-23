/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awaytime.app.models.BedtimeSettings
import com.awaytime.app.models.BedtimeState
import com.awaytime.app.models.DayOfWeek
import com.awaytime.app.service.BedtimeManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class BedtimeViewModel(private val context: Context) : ViewModel() {
    
    private val bedtimeManager = BedtimeManager.getInstance(context)
    
    // State flows from bedtime manager
    val bedtimeSettings: StateFlow<BedtimeSettings> = bedtimeManager.bedtimeSettings
    val bedtimeState: StateFlow<BedtimeState> = bedtimeManager.bedtimeState
    
    /**
     * Updates whether bedtime mode is enabled
     */
    fun updateIsEnabled(enabled: Boolean) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(isEnabled = enabled)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates bedtime start time
     */
    fun updateStartTime(startTime: LocalTime) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(startTime = startTime)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates bedtime end time
     */
    fun updateEndTime(endTime: LocalTime) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(endTime = endTime)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates enabled days for bedtime
     */
    fun updateEnabledDays(days: Set<DayOfWeek>) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(enabledDays = days)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates wind down duration in minutes
     */
    fun updateWindDownDuration(minutes: Int) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(windDownDurationMinutes = minutes)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates Do Not Disturb setting
     */
    fun updateEnableDnd(enabled: Boolean) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(enableDnd = enabled)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates notification dimming setting
     */
    fun updateDimNotifications(enabled: Boolean) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(dimNotifications = enabled)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates distraction blocking setting
     */
    fun updateBlockDistractions(enabled: Boolean) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(blockDistractions = enabled)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates emergency calls setting
     */
    fun updateAllowEmergency(enabled: Boolean) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(allowEmergencyCalls = enabled)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Updates allowed apps during bedtime
     */
    fun updateAllowedApps(allowedApps: Set<String>) {
        val currentSettings = bedtimeSettings.value
        val newSettings = currentSettings.copy(allowedApps = allowedApps)
        bedtimeManager.updateBedtimeSettings(newSettings)
    }
    
    /**
     * Triggers emergency override
     */
    fun emergencyOverride() {
        viewModelScope.launch {
            bedtimeManager.emergencyOverride()
        }
    }
    
    /**
     * Manually activates bedtime mode (for testing/immediate activation)
     */
    fun activateBedtimeMode() {
        viewModelScope.launch {
            bedtimeManager.activateBedtimeMode()
        }
    }
    
    /**
     * Manually deactivates bedtime mode
     */
    fun deactivateBedtimeMode() {
        viewModelScope.launch {
            bedtimeManager.deactivateBedtimeMode()
        }
    }
    
    /**
     * Activates wind down mode
     */
    fun activateWindDown() {
        viewModelScope.launch {
            bedtimeManager.activateWindDown()
        }
    }
    
    /**
     * Checks if an app is allowed during bedtime
     */
    fun isAppAllowedDuringBedtime(packageName: String): Boolean {
        return bedtimeManager.isAppAllowedDuringBedtime(packageName)
    }
}