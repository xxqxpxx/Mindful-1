/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.models.*
import com.awaytime.app.widgets.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages widget data updates and configuration
 */
class WidgetManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "WidgetManager"
        private const val PREFS_NAME = "widget_settings"
        private const val KEY_WIDGET_CONFIGS = "widget_configs"
        private const val UPDATE_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        
        @Volatile
        private var INSTANCE: WidgetManager? = null
        
        fun getInstance(context: Context): WidgetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = AwayTimeRepository(context)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Widget data states
    private val _usageSummaryData = MutableStateFlow<UsageSummaryWidgetData?>(null)
    val usageSummaryData: StateFlow<UsageSummaryWidgetData?> = _usageSummaryData.asStateFlow()
    
    private val _focusSessionData = MutableStateFlow<FocusSessionWidgetData?>(null)
    val focusSessionData: StateFlow<FocusSessionWidgetData?> = _focusSessionData.asStateFlow()
    
    private val _bedtimeData = MutableStateFlow<BedtimeWidgetData?>(null)
    val bedtimeData: StateFlow<BedtimeWidgetData?> = _bedtimeData.asStateFlow()
    
    private val _quickToggleData = MutableStateFlow<QuickToggleWidgetData?>(null)
    val quickToggleData: StateFlow<QuickToggleWidgetData?> = _quickToggleData.asStateFlow()
    
    private var updateJob: Job? = null
    
    init {
        startPeriodicUpdates()
    }
    
    /**
     * Starts periodic widget data updates
     */
    private fun startPeriodicUpdates() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                try {
                    updateAllWidgetData()
                    updateAllWidgets()
                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic widget update", e)
                    delay(60000) // Wait 1 minute on error
                }
            }
        }
    }
    
    /**
     * Updates all widget data from repository
     */
    suspend fun updateAllWidgetData() {
        try {
            // Update usage summary data
            updateUsageSummaryData()
            
            // Update focus session data
            updateFocusSessionData()
            
            // Update bedtime data
            updateBedtimeData()
            
            // Update quick toggle data
            updateQuickToggleData()
            
            Log.d(TAG, "All widget data updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget data", e)
        }
    }
    
    private suspend fun updateUsageSummaryData() {
        try {
            val appGroups = repository.getAllAppGroupsSync()
            if (appGroups.isNotEmpty()) {
                val mainGroup = appGroups.first()
                val todayUsage = repository.getTodayUsage(mainGroup.name)
                val streak = repository.calculateStreak()
                
                // Get top apps (simplified - would need usage stats integration)
                val topApps = getTopAppsUsage(mainGroup.getSelectedApps())
                
                val data = UsageSummaryWidgetData(
                    todayUsageMinutes = todayUsage,
                    dailyLimitMinutes = mainGroup.dailyLimitMinutes,
                    streakDays = streak,
                    isLimitExceeded = todayUsage > mainGroup.dailyLimitMinutes,
                    topApps = topApps
                )
                
                _usageSummaryData.value = data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update usage summary data", e)
        }
    }
    
    private suspend fun updateFocusSessionData() {
        try {
            val enhancedServiceManager = EnhancedServiceManager.getInstance(context, repository)
            val status = enhancedServiceManager.getServiceStatus()
            
            val data = FocusSessionWidgetData(
                isActive = status.focusSessionActive,
                sessionType = "Focus",
                timeRemainingMinutes = if (status.focusSessionActive) 15 else 0, // Placeholder
                totalSessionMinutes = 25
            )
            
            _focusSessionData.value = data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update focus session data", e)
        }
    }
    
    private suspend fun updateBedtimeData() {
        try {
            val bedtimeManager = BedtimeManager.getInstance(context)
            val settings = bedtimeManager.bedtimeSettings.value
            val state = bedtimeManager.bedtimeState.value
            
            val data = BedtimeWidgetData(
                isActive = state is BedtimeState.Active,
                isWindDown = state is BedtimeState.WindDown,
                nextBedtime = if (settings.isEnabled) settings.startTime else null,
                nextWakeup = if (settings.isEnabled) settings.endTime else null,
                isEnabled = settings.isEnabled
            )
            
            _bedtimeData.value = data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update bedtime data", e)
        }
    }
    
    private suspend fun updateQuickToggleData() {
        try {
            val enhancedServiceManager = EnhancedServiceManager.getInstance(context, repository)
            val status = enhancedServiceManager.getServiceStatus()
            
            val data = QuickToggleWidgetData(
                isBlockingEnabled = status.accessibilityServiceRunning,
                blockedAppsCount = 0, // Would need to get from blocked apps list
                vpnActive = status.vpnServiceRunning,
                focusActive = status.focusSessionActive
            )
            
            _quickToggleData.value = data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update quick toggle data", e)
        }
    }
    
    private suspend fun getTopAppsUsage(selectedApps: List<String>): List<AppUsageInfo> {
        // Simplified implementation - would integrate with actual usage stats
        return selectedApps.take(3).mapIndexed { index, packageName ->
            AppUsageInfo(
                packageName = packageName,
                appName = getAppName(packageName),
                usageMinutes = (120 - index * 30) // Placeholder data
            )
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
    
    /**
     * Updates all active widgets
     */
    fun updateAllWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Update Usage Summary widgets
            updateWidgetType(appWidgetManager, UsageSummaryWidget::class.java)
            
            // Update Focus Session widgets
            updateWidgetType(appWidgetManager, FocusSessionWidget::class.java)
            
            // Update Bedtime widgets
            updateWidgetType(appWidgetManager, BedtimeWidget::class.java)
            
            // Update Quick Toggle widgets
            updateWidgetType(appWidgetManager, QuickToggleWidget::class.java)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widgets", e)
        }
    }
    
    private fun updateWidgetType(appWidgetManager: AppWidgetManager, widgetClass: Class<*>) {
        try {
            val componentName = ComponentName(context, widgetClass)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, widgetClass)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget type: ${widgetClass.simpleName}", e)
        }
    }
    
    /**
     * Saves widget configuration
     */
    fun saveWidgetConfiguration(config: WidgetConfiguration) {
        try {
            val configs = loadWidgetConfigurations().toMutableMap()
            configs[config.id] = config
            
            val json = gson.toJson(configs)
            prefs.edit().putString(KEY_WIDGET_CONFIGS, json).apply()
            
            Log.d(TAG, "Widget configuration saved: ${config.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save widget configuration", e)
        }
    }
    
    /**
     * Loads all widget configurations
     */
    fun loadWidgetConfigurations(): Map<String, WidgetConfiguration> {
        return try {
            val json = prefs.getString(KEY_WIDGET_CONFIGS, null)
            if (json != null) {
                val type = object : TypeToken<Map<String, WidgetConfiguration>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load widget configurations", e)
            emptyMap()
        }
    }
    
    /**
     * Gets configuration for a specific widget
     */
    fun getWidgetConfiguration(widgetId: String): WidgetConfiguration? {
        return loadWidgetConfigurations()[widgetId]
    }
    
    /**
     * Removes widget configuration
     */
    fun removeWidgetConfiguration(widgetId: String) {
        try {
            val configs = loadWidgetConfigurations().toMutableMap()
            configs.remove(widgetId)
            
            val json = gson.toJson(configs)
            prefs.edit().putString(KEY_WIDGET_CONFIGS, json).apply()
            
            Log.d(TAG, "Widget configuration removed: $widgetId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove widget configuration", e)
        }
    }
    
    /**
     * Forces immediate update of specific widget type
     */
    fun updateWidget(widgetType: WidgetType) {
        scope.launch {
            updateAllWidgetData()
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            when (widgetType) {
                WidgetType.USAGE_SUMMARY -> updateWidgetType(appWidgetManager, UsageSummaryWidget::class.java)
                WidgetType.FOCUS_SESSION -> updateWidgetType(appWidgetManager, FocusSessionWidget::class.java)
                WidgetType.BEDTIME_STATUS -> updateWidgetType(appWidgetManager, BedtimeWidget::class.java)
                WidgetType.QUICK_TOGGLE -> updateWidgetType(appWidgetManager, QuickToggleWidget::class.java)
                else -> updateAllWidgets()
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        updateJob?.cancel()
    }
}