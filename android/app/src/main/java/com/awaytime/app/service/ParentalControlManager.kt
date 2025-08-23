/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.time.LocalTime
import java.util.*

/**
 * Manages parental control features and restrictions
 */
class ParentalControlManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ParentalControlManager"
        private const val PREFS_NAME = "parental_controls"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_OVERRIDE_REQUESTS = "override_requests"
        private const val KEY_USAGE_SESSIONS = "usage_sessions"
        private const val KEY_EVENTS = "events"
        private const val PIN_SALT = "awaytime_parental_salt"
        
        @Volatile
        private var INSTANCE: ParentalControlManager? = null
        
        fun getInstance(context: Context): ParentalControlManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ParentalControlManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = AwayTimeRepository(context)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // State flows
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ParentalControlSettings> = _settings.asStateFlow()
    
    private val _overrideRequests = MutableStateFlow(loadOverrideRequests())
    val overrideRequests: StateFlow<List<ParentalOverrideRequest>> = _overrideRequests.asStateFlow()
    
    private val _currentSessions = MutableStateFlow<Map<String, ChildUsageSession>>(emptyMap())
    val currentSessions: StateFlow<Map<String, ChildUsageSession>> = _currentSessions.asStateFlow()
    
    private val _dashboardStats = MutableStateFlow<ParentalDashboardStats?>(null)
    val dashboardStats: StateFlow<ParentalDashboardStats?> = _dashboardStats.asStateFlow()
    
    private var sessionCheckJob: Job? = null
    
    init {
        startSessionMonitoring()
        updateDashboardStats()
    }
    
    /**
     * Updates parental control settings
     */
    fun updateSettings(newSettings: ParentalControlSettings) {
        _settings.value = newSettings.copy(lastModified = System.currentTimeMillis())
        saveSettings(_settings.value)
        
        // Log the change
        logEvent(ParentalControlEvent(
            eventType = ParentalEventType.SETTINGS_CHANGED,
            details = "Parental control settings updated",
            parentAction = true
        ))
        
        Log.d(TAG, "Parental control settings updated")
    }
    
    /**
     * Validates parental PIN
     */
    fun validatePin(enteredPin: String): Boolean {
        val currentSettings = _settings.value
        if (currentSettings.parentalPin.isEmpty()) return true
        
        val hashedPin = hashPin(enteredPin)
        return hashedPin == currentSettings.parentalPin
    }
    
    /**
     * Sets new parental PIN
     */
    fun setParentalPin(newPin: String): Boolean {
        return try {
            val hashedPin = hashPin(newPin)
            val updatedSettings = _settings.value.copy(parentalPin = hashedPin)
            updateSettings(updatedSettings)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set parental PIN", e)
            false
        }
    }
    
    /**
     * Checks if app access is allowed based on parental controls
     */
    fun isAppAccessAllowed(packageName: String): Boolean {
        val settings = _settings.value
        if (!settings.isEnabled) return true
        
        // Always allow emergency apps
        if (isEmergencyApp(packageName)) return true
        
        // Check if app is in allowed list
        if (packageName in settings.allowedApps) return true
        
        // Check if app is explicitly restricted
        if (packageName in settings.restrictedApps) return false
        
        // Check active time slots
        if (isInBlockedTimeSlot()) {
            logEvent(ParentalControlEvent(
                eventType = ParentalEventType.TIME_SLOT_VIOLATION,
                appPackageName = packageName,
                details = "App accessed during blocked time slot"
            ))
            return false
        }
        
        // Check daily screen time limit
        if (hasExceededDailyLimit()) {
            logEvent(ParentalControlEvent(
                eventType = ParentalEventType.TIME_LIMIT_REACHED,
                appPackageName = packageName,
                details = "Daily screen time limit exceeded"
            ))
            return false
        }
        
        // Check session time limit
        if (hasExceededSessionLimit(packageName)) {
            logEvent(ParentalControlEvent(
                eventType = ParentalEventType.TIME_LIMIT_REACHED,
                appPackageName = packageName,
                details = "Session time limit exceeded"
            ))
            return false
        }
        
        return true
    }
    
    /**
     * Starts a usage session for an app
     */
    fun startUsageSession(packageName: String) {
        if (!_settings.value.isEnabled) return
        
        val session = ChildUsageSession(
            appPackageName = packageName,
            startTime = System.currentTimeMillis()
        )
        
        val currentSessions = _currentSessions.value.toMutableMap()
        currentSessions[packageName] = session
        _currentSessions.value = currentSessions
        
        Log.d(TAG, "Started usage session for $packageName")
    }
    
    /**
     * Ends a usage session for an app
     */
    fun endUsageSession(packageName: String, terminatedByParent: Boolean = false) {
        val currentSessions = _currentSessions.value.toMutableMap()
        val session = currentSessions[packageName] ?: return
        
        val endedSession = session.copy(
            endTime = System.currentTimeMillis(),
            wasTerminatedByParent = terminatedByParent
        )
        
        // Save to persistent storage
        saveUsageSession(endedSession)
        
        // Remove from current sessions
        currentSessions.remove(packageName)
        _currentSessions.value = currentSessions
        
        Log.d(TAG, "Ended usage session for $packageName (${endedSession.getDurationMinutes()}m)")
    }
    
    /**
     * Requests parental override for an app
     */
    fun requestParentalOverride(
        appPackageName: String,
        reason: String,
        requestedDuration: Int
    ): String {
        val request = ParentalOverrideRequest(
            requestedApp = appPackageName,
            requestReason = reason,
            requestedDuration = requestedDuration
        )
        
        val currentRequests = _overrideRequests.value.toMutableList()
        currentRequests.add(request)
        _overrideRequests.value = currentRequests
        saveOverrideRequests(currentRequests)
        
        logEvent(ParentalControlEvent(
            eventType = ParentalEventType.OVERRIDE_REQUESTED,
            appPackageName = appPackageName,
            details = "Override requested: $reason"
        ))
        
        Log.d(TAG, "Override request created for $appPackageName")
        return request.id
    }
    
    /**
     * Responds to a parental override request
     */
    fun respondToOverrideRequest(
        requestId: String,
        approved: Boolean,
        approvedDuration: Int = 0,
        response: String = ""
    ) {
        val currentRequests = _overrideRequests.value.toMutableList()
        val requestIndex = currentRequests.indexOfFirst { it.id == requestId }
        
        if (requestIndex >= 0) {
            val updatedRequest = currentRequests[requestIndex].copy(
                status = if (approved) OverrideStatus.APPROVED else OverrideStatus.DENIED,
                parentResponse = response,
                approvedDuration = if (approved) approvedDuration else 0,
                responseTime = System.currentTimeMillis()
            )
            
            currentRequests[requestIndex] = updatedRequest
            _overrideRequests.value = currentRequests
            saveOverrideRequests(currentRequests)
            
            logEvent(ParentalControlEvent(
                eventType = if (approved) ParentalEventType.OVERRIDE_APPROVED else ParentalEventType.OVERRIDE_DENIED,
                appPackageName = updatedRequest.requestedApp,
                details = "Override ${if (approved) "approved" else "denied"}: $response",
                parentAction = true
            ))
            
            Log.d(TAG, "Override request $requestId ${if (approved) "approved" else "denied"}")
        }
    }
    
    /**
     * Gets daily summary for child usage
     */
    suspend fun getDailySummary(date: Date): ChildDailySummary {
        return withContext(Dispatchers.IO) {
            try {
                val sessions = getUsageSessionsForDate(date)
                val totalScreenTime = sessions.sumOf { it.getDurationMinutes() }
                val sessionCount = sessions.size
                val topApps = getTopAppsFromSessions(sessions)
                val overrideRequests = getOverrideRequestsForDate(date)
                val timeSlotViolations = getTimeSlotViolationsForDate(date)
                val longestSession = sessions.maxOfOrNull { it.getDurationMinutes() } ?: 0
                val averageSession = if (sessionCount > 0) totalScreenTime / sessionCount else 0
                val behaviorScore = calculateBehaviorScore(sessions, overrideRequests.size, timeSlotViolations)
                
                ChildDailySummary(
                    date = date,
                    totalScreenTimeMinutes = totalScreenTime,
                    sessionCount = sessionCount,
                    topApps = topApps,
                    overrideRequestsCount = overrideRequests.size,
                    timeSlotViolations = timeSlotViolations,
                    longestSessionMinutes = longestSession,
                    averageSessionMinutes = averageSession,
                    behaviorScore = behaviorScore
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate daily summary", e)
                ChildDailySummary(
                    date = date,
                    totalScreenTimeMinutes = 0,
                    sessionCount = 0,
                    topApps = emptyList(),
                    overrideRequestsCount = 0,
                    timeSlotViolations = 0,
                    longestSessionMinutes = 0,
                    averageSessionMinutes = 0,
                    behaviorScore = 0
                )
            }
        }
    }
    
    /**
     * Forces break time for the child
     */
    fun forceBreakTime(durationMinutes: Int) {
        val settings = _settings.value
        if (!settings.isEnabled) return
        
        // End all current sessions
        _currentSessions.value.keys.forEach { packageName ->
            endUsageSession(packageName, terminatedByParent = true)
        }
        
        logEvent(ParentalControlEvent(
            eventType = ParentalEventType.BREAK_ENFORCED,
            details = "Break time enforced for $durationMinutes minutes",
            parentAction = true
        ))
        
        // Schedule break end (this would integrate with notification system)
        scheduleBreakEnd(durationMinutes)
        
        Log.d(TAG, "Break time enforced for $durationMinutes minutes")
    }
    
    /**
     * Gets parental dashboard statistics
     */
    private fun updateDashboardStats() {
        scope.launch {
            try {
                val settings = _settings.value
                if (!settings.isEnabled) return@launch
                
                val today = Date()
                val dailySummary = getDailySummary(today)
                val weeklyData = getWeeklyScreenTimeData()
                val pendingRequestsCount = _overrideRequests.value.count { it.status == OverrideStatus.PENDING }
                val recentEvents = getRecentEvents(10)
                val topConcerns = generateTopConcerns(dailySummary, recentEvents)
                
                val stats = ParentalDashboardStats(
                    childName = settings.childProfileName,
                    todayScreenTime = dailySummary.totalScreenTimeMinutes,
                    weeklyAverage = weeklyData.average().toInt(),
                    complianceRate = calculateComplianceRate(weeklyData),
                    currentStreak = calculateComplianceStreak(),
                    pendingRequests = pendingRequestsCount,
                    recentEvents = recentEvents,
                    weeklyTrend = weeklyData,
                    topConcerns = topConcerns,
                    lastActive = getLastActivityTime()
                )
                
                _dashboardStats.value = stats
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update dashboard stats", e)
            }
        }
    }
    
    // Private helper methods
    
    private fun startSessionMonitoring() {
        sessionCheckJob?.cancel()
        sessionCheckJob = scope.launch {
            while (isActive) {
                try {
                    checkSessionLimits()
                    enforceBreakTime()
                    updateDashboardStats()
                    delay(30_000) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in session monitoring", e)
                    delay(60_000)
                }
            }
        }
    }
    
    private fun checkSessionLimits() {
        val settings = _settings.value
        if (!settings.isEnabled) return
        
        _currentSessions.value.forEach { (packageName, session) ->
            val sessionDuration = session.getDurationMinutes()
            
            if (sessionDuration >= settings.maxSessionTime) {
                endUsageSession(packageName, terminatedByParent = false)
                
                logEvent(ParentalControlEvent(
                    eventType = ParentalEventType.TIME_LIMIT_REACHED,
                    appPackageName = packageName,
                    details = "Session time limit reached (${sessionDuration}m)"
                ))
            }
        }
    }
    
    private fun enforceBreakTime() {
        val settings = _settings.value
        if (!settings.isEnabled || settings.breakIntervalMinutes <= 0) return
        
        // Check if break is needed based on continuous usage
        val totalContinuousTime = _currentSessions.value.values.sumOf { it.getDurationMinutes() }
        
        if (totalContinuousTime >= settings.breakIntervalMinutes) {
            // Force a break
            forceBreakTime(5) // 5-minute mandatory break
        }
    }
    
    private fun isInBlockedTimeSlot(): Boolean {
        val settings = _settings.value
        return settings.blockedTimeSlots.any { it.isActiveNow() }
    }
    
    private fun hasExceededDailyLimit(): Boolean {
        // This would check against today's total usage
        // Implementation would involve checking stored usage sessions
        return false // Placeholder
    }
    
    private fun hasExceededSessionLimit(packageName: String): Boolean {
        val session = _currentSessions.value[packageName] ?: return false
        val settings = _settings.value
        return session.getDurationMinutes() >= settings.maxSessionTime
    }
    
    private fun isEmergencyApp(packageName: String): Boolean {
        val emergencyApps = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.android.phone",
            "android"
        )
        return packageName in emergencyApps
    }
    
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPin = "$pin$PIN_SALT"
        val hash = digest.digest(saltedPin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun scheduleBreakEnd(durationMinutes: Int) {
        // This would schedule a notification or alarm to end the break
        // Implementation depends on notification system
    }
    
    private fun calculateBehaviorScore(
        sessions: List<ChildUsageSession>,
        overrideRequestsCount: Int,
        timeSlotViolations: Int
    ): Int {
        // Basic behavior scoring algorithm
        var score = 100
        
        // Deduct for excessive usage
        val totalTime = sessions.sumOf { it.getDurationMinutes() }
        val settings = _settings.value
        if (totalTime > settings.maxDailyScreenTime) {
            score -= ((totalTime - settings.maxDailyScreenTime) / 10).coerceAtMost(30)
        }
        
        // Deduct for override requests
        score -= (overrideRequestsCount * 5).coerceAtMost(20)
        
        // Deduct for time slot violations
        score -= (timeSlotViolations * 10).coerceAtMost(30)
        
        return score.coerceAtLeast(0)
    }
    
    private fun calculateComplianceRate(weeklyData: List<Int>): Float {
        val settings = _settings.value
        val compliantDays = weeklyData.count { it <= settings.maxDailyScreenTime }
        return compliantDays.toFloat() / weeklyData.size.toFloat()
    }
    
    private fun calculateComplianceStreak(): Int {
        // This would calculate consecutive days of compliance
        // Implementation would involve checking historical data
        return 0 // Placeholder
    }
    
    private fun getWeeklyScreenTimeData(): List<Int> {
        // This would get screen time data for the past 7 days
        // Implementation would involve database queries
        return listOf(60, 45, 30, 75, 50, 80, 40) // Placeholder
    }
    
    private fun getRecentEvents(limit: Int): List<ParentalControlEvent> {
        return loadEvents().take(limit)
    }
    
    private fun generateTopConcerns(
        dailySummary: ChildDailySummary,
        recentEvents: List<ParentalControlEvent>
    ): List<String> {
        val concerns = mutableListOf<String>()
        
        if (dailySummary.behaviorScore < 70) {
            concerns.add("Poor behavior compliance")
        }
        
        if (dailySummary.overrideRequestsCount > 3) {
            concerns.add("Excessive override requests")
        }
        
        if (dailySummary.timeSlotViolations > 0) {
            concerns.add("Time slot violations")
        }
        
        val contentBlocks = recentEvents.count { it.eventType == ParentalEventType.INAPPROPRIATE_CONTENT_BLOCKED }
        if (contentBlocks > 0) {
            concerns.add("Inappropriate content accessed")
        }
        
        return concerns
    }
    
    private fun getLastActivityTime(): Long {
        return _currentSessions.value.values.maxOfOrNull { it.startTime } ?: System.currentTimeMillis()
    }
    
    // Data persistence methods
    
    private fun loadSettings(): ParentalControlSettings {
        return try {
            val json = prefs.getString(KEY_SETTINGS, null)
            if (json != null) {
                gson.fromJson(json, ParentalControlSettings::class.java)
            } else {
                ParentalControlSettings()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load parental control settings", e)
            ParentalControlSettings()
        }
    }
    
    private fun saveSettings(settings: ParentalControlSettings) {
        try {
            val json = gson.toJson(settings)
            prefs.edit().putString(KEY_SETTINGS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save parental control settings", e)
        }
    }
    
    private fun loadOverrideRequests(): List<ParentalOverrideRequest> {
        return try {
            val json = prefs.getString(KEY_OVERRIDE_REQUESTS, null)
            if (json != null) {
                val type = object : TypeToken<List<ParentalOverrideRequest>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load override requests", e)
            emptyList()
        }
    }
    
    private fun saveOverrideRequests(requests: List<ParentalOverrideRequest>) {
        try {
            val json = gson.toJson(requests)
            prefs.edit().putString(KEY_OVERRIDE_REQUESTS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save override requests", e)
        }
    }
    
    private fun saveUsageSession(session: ChildUsageSession) {
        try {
            val sessions = loadUsageSessions().toMutableList()
            sessions.add(session)
            
            // Keep only last 30 days
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            val filteredSessions = sessions.filter { it.startTime > cutoffTime }
            
            val json = gson.toJson(filteredSessions)
            prefs.edit().putString(KEY_USAGE_SESSIONS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save usage session", e)
        }
    }
    
    private fun loadUsageSessions(): List<ChildUsageSession> {
        return try {
            val json = prefs.getString(KEY_USAGE_SESSIONS, null)
            if (json != null) {
                val type = object : TypeToken<List<ChildUsageSession>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load usage sessions", e)
            emptyList()
        }
    }
    
    private fun logEvent(event: ParentalControlEvent) {
        try {
            val events = loadEvents().toMutableList()
            events.add(0, event) // Add to beginning
            
            // Keep only last 100 events
            val limitedEvents = events.take(100)
            
            val json = gson.toJson(limitedEvents)
            prefs.edit().putString(KEY_EVENTS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log parental control event", e)
        }
    }
    
    private fun loadEvents(): List<ParentalControlEvent> {
        return try {
            val json = prefs.getString(KEY_EVENTS, null)
            if (json != null) {
                val type = object : TypeToken<List<ParentalControlEvent>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load events", e)
            emptyList()
        }
    }
    
    private fun getUsageSessionsForDate(date: Date): List<ChildUsageSession> {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)
        
        return loadUsageSessions().filter { session ->
            session.startTime >= startOfDay && session.startTime < endOfDay
        }
    }
    
    private fun getOverrideRequestsForDate(date: Date): List<ParentalOverrideRequest> {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)
        
        return _overrideRequests.value.filter { request ->
            request.requestTime >= startOfDay && request.requestTime < endOfDay
        }
    }
    
    private fun getTimeSlotViolationsForDate(date: Date): Int {
        val events = loadEvents().filter { event ->
            event.eventType == ParentalEventType.TIME_SLOT_VIOLATION
        }
        
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L)
        
        return events.count { event ->
            event.timestamp >= startOfDay && event.timestamp < endOfDay
        }
    }
    
    private fun getTopAppsFromSessions(sessions: List<ChildUsageSession>): List<AppUsageInfo> {
        return sessions
            .groupBy { it.appPackageName }
            .map { (packageName, appSessions) ->
                val totalMinutes = appSessions.sumOf { it.getDurationMinutes() }
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    usageMinutes = totalMinutes
                )
            }
            .sortedByDescending { it.usageMinutes }
            .take(5)
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
}