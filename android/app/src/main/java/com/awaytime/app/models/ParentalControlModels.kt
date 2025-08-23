/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.models

import java.time.LocalTime
import java.util.Date

/**
 * Parental control configuration
 */
data class ParentalControlSettings(
    val isEnabled: Boolean = false,
    val parentalPin: String = "",
    val supervisionLevel: SupervisionLevel = SupervisionLevel.MODERATE,
    val allowParentalOverride: Boolean = true,
    val requirePinForAppAccess: Boolean = false,
    val requirePinForSettingsChange: Boolean = true,
    val maxDailyScreenTime: Int = 120, // minutes
    val maxSessionTime: Int = 30, // minutes
    val breakIntervalMinutes: Int = 15,
    val blockedTimeSlots: List<TimeSlot> = emptyList(),
    val allowedApps: Set<String> = getDefaultAllowedApps(),
    val restrictedApps: Set<String> = emptySet(),
    val contentRestrictions: ContentRestrictions = ContentRestrictions(),
    val emergencyContacts: List<String> = emptyList(),
    val childProfileName: String = "Child",
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultAllowedApps(): Set<String> {
            return setOf(
                "com.android.dialer",
                "com.android.contacts",
                "com.android.settings",
                "com.android.camera",
                "com.google.android.apps.photos",
                "com.android.calculator2",
                "android" // System apps
            )
        }
    }
}

/**
 * Supervision levels for parental controls
 */
enum class SupervisionLevel(val displayName: String, val description: String) {
    STRICT("Strict", "Heavy restrictions, minimal app access"),
    MODERATE("Moderate", "Balanced restrictions with supervised access"),
    LIGHT("Light", "Basic time limits and content filtering"),
    CUSTOM("Custom", "Custom configuration by parent")
}

/**
 * Time slot for blocking periods
 */
data class TimeSlot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val activeDays: Set<DayOfWeek>,
    val isEnabled: Boolean = true,
    val allowEmergencyApps: Boolean = true
) {
    /**
     * Checks if current time falls within this time slot
     */
    fun isActiveNow(): Boolean {
        if (!isEnabled) return false
        
        val now = LocalTime.now()
        val currentDay = java.time.LocalDate.now().dayOfWeek
        val awayTimeDayOfWeek = DayOfWeek.fromJavaTime(currentDay)
        
        if (awayTimeDayOfWeek !in activeDays) return false
        
        return if (startTime.isBefore(endTime)) {
            // Same day slot
            now.isAfter(startTime) && now.isBefore(endTime)
        } else {
            // Cross midnight slot
            now.isAfter(startTime) || now.isBefore(endTime)
        }
    }
}

/**
 * Content restrictions configuration
 */
data class ContentRestrictions(
    val blockInappropriateContent: Boolean = true,
    val blockSocialMedia: Boolean = false,
    val blockGames: Boolean = false,
    val blockVideoStreaming: Boolean = false,
    val blockOnlineShopping: Boolean = true,
    val blockWebBrowser: Boolean = false,
    val allowedWebsites: Set<String> = emptySet(),
    val blockedWebsites: Set<String> = emptySet(),
    val ageRating: AgeRating = AgeRating.TEEN,
    val keywordBlacklist: Set<String> = emptySet(),
    val safeSearchEnabled: Boolean = true
)

/**
 * Age rating for content filtering
 */
enum class AgeRating(val displayName: String, val minAge: Int) {
    CHILD("Child (4-8)", 4),
    TWEEN("Tween (9-12)", 9),
    TEEN("Teen (13-17)", 13),
    MATURE("Mature (18+)", 18)
}

/**
 * Parental override request
 */
data class ParentalOverrideRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val requestedApp: String,
    val requestReason: String,
    val requestedDuration: Int, // minutes
    val requestTime: Long = System.currentTimeMillis(),
    val status: OverrideStatus = OverrideStatus.PENDING,
    val parentResponse: String = "",
    val approvedDuration: Int = 0,
    val responseTime: Long? = null
)

/**
 * Override request status
 */
enum class OverrideStatus(val displayName: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    DENIED("Denied"),
    EXPIRED("Expired")
}

/**
 * Usage session tracking for parental controls
 */
data class ChildUsageSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val appPackageName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val wasTerminatedByParent: Boolean = false,
    val wasTerminatedByTimeLimit: Boolean = false,
    val parentApprovalUsed: Boolean = false
) {
    val isActive: Boolean get() = endTime == null
    
    fun getDurationMinutes(): Int {
        val end = endTime ?: System.currentTimeMillis()
        return ((end - startTime) / (1000 * 60)).toInt()
    }
}

/**
 * Daily summary for parental dashboard
 */
data class ChildDailySummary(
    val date: Date,
    val totalScreenTimeMinutes: Int,
    val sessionCount: Int,
    val topApps: List<AppUsageInfo>,
    val overrideRequestsCount: Int,
    val timeSlotViolations: Int,
    val longestSessionMinutes: Int,
    val averageSessionMinutes: Int,
    val behaviorScore: Int // 0-100 based on compliance
) {
    fun getBehaviorDescription(): String {
        return when {
            behaviorScore >= 90 -> "Excellent behavior"
            behaviorScore >= 80 -> "Good behavior"
            behaviorScore >= 70 -> "Fair behavior"
            behaviorScore >= 60 -> "Needs improvement"
            else -> "Poor behavior"
        }
    }
}

/**
 * Parental control event for logging
 */
data class ParentalControlEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val eventType: ParentalEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackageName: String? = null,
    val details: String,
    val parentAction: Boolean = false
)

/**
 * Types of parental control events
 */
enum class ParentalEventType(val displayName: String) {
    APP_BLOCKED("App Blocked"),
    TIME_LIMIT_REACHED("Time Limit Reached"),
    BREAK_ENFORCED("Break Enforced"),
    OVERRIDE_REQUESTED("Override Requested"),
    OVERRIDE_APPROVED("Override Approved"),
    OVERRIDE_DENIED("Override Denied"),
    SETTINGS_CHANGED("Settings Changed"),
    INAPPROPRIATE_CONTENT_BLOCKED("Inappropriate Content Blocked"),
    TIME_SLOT_VIOLATION("Time Slot Violation"),
    EMERGENCY_ACCESS("Emergency Access")
}

/**
 * Parental dashboard statistics
 */
data class ParentalDashboardStats(
    val childName: String,
    val todayScreenTime: Int,
    val weeklyAverage: Int,
    val complianceRate: Float, // 0-1
    val currentStreak: Int,
    val pendingRequests: Int,
    val recentEvents: List<ParentalControlEvent>,
    val weeklyTrend: List<Int>, // 7 days of screen time
    val topConcerns: List<String>,
    val lastActive: Long
) {
    fun getComplianceDescription(): String {
        return when {
            complianceRate >= 0.9f -> "Excellent compliance"
            complianceRate >= 0.8f -> "Good compliance"
            complianceRate >= 0.7f -> "Fair compliance"
            else -> "Needs attention"
        }
    }
}