package com.awaytime.app.core

import android.content.Context
import com.awaytime.app.service.NotificationService

/**
 * Core notification functionality that manages all app notifications
 * and provides a unified interface for the app's notification features.
 */
class AwayTimeNotifications(private val context: Context) {
    
    private val notificationService = NotificationService(context)
    
    fun sendUsageWarning(appGroupName: String, minutesRemaining: Int) {
        notificationService.sendWarningNotification(appGroupName, minutesRemaining)
    }
    
    fun sendLimitReached(appGroupName: String) {
        notificationService.sendLimitReachedNotification(appGroupName)
    }
    
    fun sendStreakAchievement(streakDays: Int) {
        notificationService.sendStreakAchievementNotification(streakDays)
    }
    
    fun sendMotivationalMessage(message: String) {
        notificationService.showMotivationalNotification(message)
    }
    
    fun scheduleDailyReminder() {
        notificationService.scheduleDailyReminder()
    }
    
    fun clearAllNotifications() {
        notificationService.clearAllNotifications()
    }
    
    fun areNotificationsEnabled(): Boolean {
        return notificationService.areNotificationsEnabled()
    }
    
    fun getNotificationStats(): NotificationStats {
        val stats = notificationService.getNotificationStats()
        return NotificationStats(
            warningsSent = stats.warningsSent,
            limitsSent = stats.limitsSent,
            streaksSent = stats.streaksSent,
            totalSent = stats.totalSent
        )
    }
}

data class NotificationStats(
    val warningsSent: Int,
    val limitsSent: Int, 
    val streaksSent: Int,
    val totalSent: Int
)