/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.models

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Represents bedtime mode configuration
 */
data class BedtimeSettings(
    val isEnabled: Boolean = false,
    val startTime: LocalTime = LocalTime.of(22, 0), // 10:00 PM
    val endTime: LocalTime = LocalTime.of(7, 0),   // 7:00 AM
    val enableDnd: Boolean = true,
    val dimNotifications: Boolean = true,
    val blockDistractions: Boolean = true,
    val windDownDurationMinutes: Int = 30,
    val allowEmergencyCalls: Boolean = true,
    val allowedApps: Set<String> = setOf(
        "com.android.dialer",
        "com.android.contacts",
        "com.android.settings",
        "android" // System apps
    ),
    val enabledDays: Set<DayOfWeek> = DayOfWeek.values().toSet()
) {
    
    /**
     * Returns true if bedtime mode should be active at the given time
     */
    fun isActiveAt(time: LocalTime): Boolean {
        if (!isEnabled) return false
        
        val currentDay = java.time.LocalDate.now().dayOfWeek
        val awayTimeDayOfWeek = DayOfWeek.fromJavaTime(currentDay)
        
        if (awayTimeDayOfWeek !in enabledDays) return false
        
        return if (startTime.isBefore(endTime)) {
            // Same day: 22:00 - 23:59
            time.isAfter(startTime) && time.isBefore(endTime)
        } else {
            // Cross midnight: 22:00 - 07:00 next day
            time.isAfter(startTime) || time.isBefore(endTime)
        }
    }
    
    /**
     * Returns true if wind down period should be active
     */
    fun isWindDownActive(time: LocalTime): Boolean {
        if (!isEnabled || windDownDurationMinutes <= 0) return false
        
        val windDownStart = startTime.minusMinutes(windDownDurationMinutes.toLong())
        return time.isAfter(windDownStart) && time.isBefore(startTime)
    }
    
    /**
     * Gets the next bedtime start time
     */
    fun getNextBedtime(): LocalTime {
        val now = LocalTime.now()
        return if (now.isBefore(startTime)) {
            startTime // Today
        } else {
            startTime // Tomorrow (caller should handle date logic)
        }
    }
    
    /**
     * Formats time for display
     */
    fun getStartTimeFormatted(): String {
        return startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    fun getEndTimeFormatted(): String {
        return endTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
}

/**
 * Days of the week for bedtime scheduling
 */
enum class DayOfWeek(val displayName: String, val shortName: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun");
    
    companion object {
        fun fromJavaTime(javaDay: java.time.DayOfWeek): DayOfWeek {
            return when (javaDay) {
                java.time.DayOfWeek.MONDAY -> MONDAY
                java.time.DayOfWeek.TUESDAY -> TUESDAY
                java.time.DayOfWeek.WEDNESDAY -> WEDNESDAY
                java.time.DayOfWeek.THURSDAY -> THURSDAY
                java.time.DayOfWeek.FRIDAY -> FRIDAY
                java.time.DayOfWeek.SATURDAY -> SATURDAY
                java.time.DayOfWeek.SUNDAY -> SUNDAY
            }
        }
        
        fun weekdays(): Set<DayOfWeek> {
            return setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        }
        
        fun weekends(): Set<DayOfWeek> {
            return setOf(SATURDAY, SUNDAY)
        }
    }
}

/**
 * Represents a scheduled blocking rule
 */
data class ScheduledBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val enabledDays: Set<DayOfWeek>,
    val blockedApps: Set<String>,
    val isEnabled: Boolean = true,
    val blockType: BlockType = BlockType.COMPLETE_BLOCK,
    val allowEmergency: Boolean = true
) {
    
    /**
     * Returns true if this scheduled block should be active now
     */
    fun isActiveNow(): Boolean {
        if (!isEnabled) return false
        
        val now = LocalTime.now()
        val currentDay = java.time.LocalDate.now().dayOfWeek
        val awayTimeDayOfWeek = DayOfWeek.fromJavaTime(currentDay)
        
        if (awayTimeDayOfWeek !in enabledDays) return false
        
        return if (startTime.isBefore(endTime)) {
            // Same day block
            now.isAfter(startTime) && now.isBefore(endTime)
        } else {
            // Cross midnight block
            now.isAfter(startTime) || now.isBefore(endTime)
        }
    }
}

/**
 * Types of blocking for scheduled rules
 */
enum class BlockType(val displayName: String) {
    COMPLETE_BLOCK("Complete Block"),
    TIME_LIMIT("Time Limit"),
    NOTIFICATION_ONLY("Notifications Only"),
    CONTENT_FILTER("Content Filter")
}

/**
 * Bedtime mode state
 */
sealed class BedtimeState {
    object Inactive : BedtimeState()
    object WindDown : BedtimeState()
    object Active : BedtimeState()
    
    fun isActive(): Boolean = this is Active
    fun isWindDown(): Boolean = this is WindDown
}