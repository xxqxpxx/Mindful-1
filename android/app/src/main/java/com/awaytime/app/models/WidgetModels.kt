/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.models

import java.time.LocalTime

/**
 * Widget configuration and data models
 */
data class WidgetConfiguration(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: WidgetType,
    val isEnabled: Boolean = true,
    val updateIntervalMinutes: Int = 15,
    val showProgressBar: Boolean = true,
    val showAppIcons: Boolean = true,
    val selectedApps: Set<String> = emptySet(),
    val compactMode: Boolean = false,
    val theme: WidgetTheme = WidgetTheme.SYSTEM
)

/**
 * Types of widgets available
 */
enum class WidgetType(val displayName: String, val description: String) {
    USAGE_SUMMARY("Usage Summary", "Shows today's screen time and progress"),
    TOP_APPS("Top Apps", "Displays most used apps today"),
    FOCUS_SESSION("Focus Session", "Quick focus session controls"),
    BEDTIME_STATUS("Bedtime Status", "Shows bedtime mode status and controls"),
    STREAK_COUNTER("Streak Counter", "Displays current goal streak"),
    QUICK_TOGGLE("Quick Toggle", "One-tap app blocking controls")
}

/**
 * Widget theme options
 */
enum class WidgetTheme(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    COLORFUL("Colorful"),
    MINIMAL("Minimal")
}

/**
 * Widget data for usage summary widget
 */
data class UsageSummaryWidgetData(
    val todayUsageMinutes: Int,
    val dailyLimitMinutes: Int,
    val streakDays: Int,
    val isLimitExceeded: Boolean,
    val topApps: List<AppUsageInfo>,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val usageProgress: Float
        get() = if (dailyLimitMinutes > 0) {
            (todayUsageMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceAtMost(1.0f)
        } else 0f
    
    val timeRemainingMinutes: Int
        get() = maxOf(0, dailyLimitMinutes - todayUsageMinutes)
    
    fun getUsageText(): String {
        val hours = todayUsageMinutes / 60
        val minutes = todayUsageMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
    
    fun getLimitText(): String {
        val hours = dailyLimitMinutes / 60
        val minutes = dailyLimitMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * App usage information for widgets
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val iconData: ByteArray? = null
) {
    fun getUsageText(): String {
        val hours = usageMinutes / 60
        val minutes = usageMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppUsageInfo

        if (packageName != other.packageName) return false
        if (appName != other.appName) return false
        if (usageMinutes != other.usageMinutes) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + usageMinutes
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Focus session widget data
 */
data class FocusSessionWidgetData(
    val isActive: Boolean,
    val sessionType: String = "Work",
    val timeRemainingMinutes: Int = 0,
    val totalSessionMinutes: Int = 25,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalSessionMinutes > 0) {
            1f - (timeRemainingMinutes.toFloat() / totalSessionMinutes.toFloat())
        } else 0f
    
    fun getTimeRemainingText(): String {
        val hours = timeRemainingMinutes / 60
        val minutes = timeRemainingMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}

/**
 * Bedtime widget data
 */
data class BedtimeWidgetData(
    val isActive: Boolean,
    val isWindDown: Boolean,
    val nextBedtime: LocalTime?,
    val nextWakeup: LocalTime?,
    val isEnabled: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getStatusText(): String {
        return when {
            isActive -> "Sleep Mode Active"
            isWindDown -> "Wind Down Period"
            isEnabled && nextBedtime != null -> "Next: ${formatTime(nextBedtime)}"
            else -> "Disabled"
        }
    }
    
    private fun formatTime(time: LocalTime): String {
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val period = if (time.hour < 12) "AM" else "PM"
        return "${hour}:${time.minute.toString().padStart(2, '0')} $period"
    }
}

/**
 * Quick toggle widget data
 */
data class QuickToggleWidgetData(
    val isBlockingEnabled: Boolean,
    val blockedAppsCount: Int,
    val vpnActive: Boolean,
    val focusActive: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getStatusText(): String {
        return when {
            isBlockingEnabled && blockedAppsCount > 0 -> "$blockedAppsCount apps blocked"
            focusActive -> "Focus session active"
            else -> "Tap to enable blocking"
        }
    }
}