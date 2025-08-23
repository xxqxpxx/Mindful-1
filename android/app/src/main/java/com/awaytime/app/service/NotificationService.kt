package com.awaytime.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.awaytime.app.MainActivity
import com.awaytime.app.R
import com.awaytime.app.ui.theme.AwayTimeColors

class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_USAGE_WARNINGS = "usage_warnings"
        private const val CHANNEL_USAGE_LIMITS = "usage_limits"
        private const val CHANNEL_ACHIEVEMENTS = "achievements"
        private const val CHANNEL_REMINDERS = "reminders"
        private const val CHANNEL_SYSTEM_ALERTS = "system_alerts"
        private const val CHANNEL_PREMIUM_TRIAL = "premium_trial"

        private const val NOTIFICATION_WARNING_ID = 1001
        private const val NOTIFICATION_LIMIT_ID = 1002
        private const val NOTIFICATION_STREAK_ID = 1003
        private const val NOTIFICATION_REMINDER_ID = 1004
        private const val NOTIFICATION_ACCESSIBILITY_ID = 1005
        private const val NOTIFICATION_TRIAL_REMINDER_ID = 1006
        private const val NOTIFICATION_TRIAL_EXPIRED_ID = 1007
        private const val NOTIFICATION_UPGRADE_PROMPT_ID = 1008
    }

    init {
        createNotificationChannels()
    }

    // MARK: - Channel Setup

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_USAGE_WARNINGS,
                    "Usage Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when approaching usage limits"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_USAGE_LIMITS,
                    "Usage Limits",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when usage limits are reached"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Streak achievements and milestones"
                    enableVibration(false)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Daily check-in reminders"
                    enableVibration(false)
                    setShowBadge(false)
                },

                NotificationChannel(
                    CHANNEL_SYSTEM_ALERTS,
                    "System Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important system notifications for app functionality"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    CHANNEL_PREMIUM_TRIAL,
                    "Premium Trial",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Premium trial reminders and expiration notifications"
                    enableVibration(false)
                    setShowBadge(true)
                }
            )

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    // MARK: - Usage Notifications

    fun sendWarningNotification(appGroupName: String, minutesRemaining: Int) {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "dashboard",
                "notification_type" to "warning",
                "app_group" to appGroupName
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_USAGE_WARNINGS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for now
            .setContentTitle("Awaytime Warning 💜")
            .setContentText(getWarningMessage(appGroupName, minutesRemaining))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getWarningMessage(appGroupName, minutesRemaining))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_WARNING_ID, notification)
            println("✅ Warning notification sent for $appGroupName")
            logNotification("warning", appGroupName)
        }
    }

    private fun getWarningMessage(appGroupName: String, minutesRemaining: Int): String {
        val messages = listOf(
            "You're at 80% of your daily limit for $appGroupName. $minutesRemaining minutes left! 💜",
            "Almost there! $minutesRemaining minutes remaining for $appGroupName today 🕐",
            "Heads up! You have $minutesRemaining minutes left for $appGroupName 📱",
            "Time check! $minutesRemaining minutes remaining for $appGroupName today ⏰"
        )
        return messages.random()
    }

    fun sendLimitReachedNotification(appGroupName: String) {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "dashboard",
                "notification_type" to "limit",
                "app_group" to appGroupName
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_USAGE_LIMITS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time's Up! 🌙")
            .setContentText(getLimitReachedMessage(appGroupName))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getLimitReachedMessage(appGroupName))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_LIMIT_ID, notification)
            println("✅ Limit notification sent for $appGroupName")
            logNotification("limit", appGroupName)
        }
    }

    private fun getLimitReachedMessage(appGroupName: String): String {
        val messages = listOf(
            "You've reached your daily limit for $appGroupName. See you tomorrow! 🌙",
            "That's a wrap for $appGroupName today! Time to focus on other things 🌟",
            "Daily limit reached for $appGroupName. Great job staying mindful! 💜",
            "Time's up for $appGroupName! Tomorrow is a fresh start 🌅"
        )
        return messages.random()
    }

    fun sendStreakAchievementNotification(streakDays: Int) {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "dashboard",
                "notification_type" to "streak",
                "show_streak" to true,
                "streak_days" to streakDays
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getStreakTitle(streakDays))
            .setContentText(getStreakMessage(streakDays))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getStreakMessage(streakDays))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.success.hashCode())
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_STREAK_ID, notification)
            println("✅ Streak notification sent for $streakDays days")
            logNotification("streak", streakDays = streakDays)
        }
    }

    private fun getStreakTitle(streakDays: Int): String {
        return when (streakDays) {
            3 -> "3-Day Streak! 🚀"
            7 -> "One Week Strong! 🔥"
            14 -> "Two Week Champion! 💎"
            30 -> "30-Day Master! 🏆"
            60 -> "60-Day Legend! 👑"
            100 -> "100-Day Hero! 🎉"
            else -> "Streak Achievement! 🎉"
        }
    }

    private fun getStreakMessage(streakDays: Int): String {
        return when (streakDays) {
            3 -> "3 days strong! You're building momentum! 🚀"
            7 -> "One week of success! You're on fire! 🔥"
            14 -> "Two weeks! You're forming a real habit! 💎"
            30 -> "30 days! This is becoming second nature! 🏆"
            60 -> "60 days! You're a screen time master! 👑"
            100 -> "100 days! Absolutely incredible! 🎉"
            else -> "Amazing! You've maintained your screen time goals for $streakDays days in a row!"
        }
    }

    // MARK: - Scheduled Notifications

    fun scheduleDailyReminder() {
        // This would typically use AlarmManager for precise scheduling
        // For now, we'll create a simple reminder notification

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Check-in 💜")
            .setContentText("How's your screen time looking today? Check your progress in Awaytime!")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .build()

        // Note: In a real implementation, you'd use AlarmManager to schedule this
        // For now, we'll just show it immediately as an example
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_REMINDER_ID, notification)
            println("✅ Daily reminder scheduled")
        }
    }

    // MARK: - Notification Management

    fun clearAllNotifications() {
        notificationManager.cancelAll()
        println("✅ All notifications cleared")
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelNotificationsForAppGroup(appGroupName: String) {
        // Cancel specific notifications for an app group
        // In a more sophisticated implementation, you'd track notification IDs per app group
        notificationManager.cancel(NOTIFICATION_WARNING_ID)
        notificationManager.cancel(NOTIFICATION_LIMIT_ID)
    }

    // MARK: - Permission Checking

    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun requestNotificationPermission() {
        // On Android 13+, you need to request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // This would typically be handled in an Activity
            println("⚠️ Notification permission should be requested in Activity")
        }
    }

    // MARK: - Utility Methods

    fun createMainAppIntent(extraData: Map<String, Any> = emptyMap()): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            extraData.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                }
            }
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // MARK: - Notification Analytics

    private fun logNotification(
        type: String,
        appGroupName: String? = null,
        streakDays: Int? = null
    ) {
        val prefs = context.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

        // Log the notification
        prefs.edit()
            .putString("last_notification_type", type)
            .putString("last_notification_app_group", appGroupName ?: "")
            .putInt("last_notification_streak", streakDays ?: 0)
            .putLong("last_notification_timestamp", System.currentTimeMillis())
            .apply()

        // Update notification count
        val countKey = "notification_count_$type"
        val currentCount = prefs.getInt(countKey, 0)
        prefs.edit().putInt(countKey, currentCount + 1).apply()
    }

    fun getNotificationStats(): NotificationStats {
        val prefs = context.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

        val warningCount = prefs.getInt("notification_count_warning", 0)
        val limitCount = prefs.getInt("notification_count_limit", 0)
        val streakCount = prefs.getInt("notification_count_streak", 0)

        return NotificationStats(
            warningsSent = warningCount,
            limitsSent = limitCount,
            streaksSent = streakCount,
            totalSent = warningCount + limitCount + streakCount
        )
    }

    // MARK: - Smart Notification Timing

    fun scheduleSmartReminder(appGroupName: String, percentage: Float) {
        if (!areNotificationsEnabled()) return

        // Don't send too many notifications
        if (hasRecentNotification(appGroupName, 300_000)) { // 5 minutes
            return
        }

        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "dashboard",
                "notification_type" to "smart_reminder",
                "app_group" to appGroupName
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mindful Moment 🧘‍♀️")
            .setContentText(getSmartReminderMessage(appGroupName, percentage))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getSmartReminderMessage(appGroupName, percentage))
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .build()

        notificationManager.notify(NOTIFICATION_REMINDER_ID, notification)
        println("✅ Smart reminder sent for $appGroupName")
    }

    private fun getSmartReminderMessage(appGroupName: String, percentage: Float): String {
        val percentageInt = (percentage * 100).toInt()
        val messages = listOf(
            "You're at $percentageInt% of your $appGroupName limit. How are you feeling? 💜",
            "Quick check-in: $percentageInt% used for $appGroupName. Still on track? 🎯",
            "Mindful moment: You've used $percentageInt% of your $appGroupName time today 🌟"
        )
        return messages.random()
    }

    private fun hasRecentNotification(appGroupName: String, withinMillis: Long): Boolean {
        val prefs = context.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

        val lastTimestamp = prefs.getLong("last_notification_timestamp", 0)
        val lastAppGroup = prefs.getString("last_notification_app_group", "")

        val timeSinceLastNotification = System.currentTimeMillis() - lastTimestamp
        return lastAppGroup == appGroupName && timeSinceLastNotification < withinMillis
    }

    // MARK: - Generic Notification Method
    
    fun sendNotification(title: String, message: String, channelId: String, notificationId: Int = 999) {
        val intent = createMainAppIntent(
            mapOf(
                "notification_type" to "generic",
                "channel" to channelId
            )
        )

        val notification = NotificationCompat.Builder(context, getValidChannelId(channelId))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(notificationId, notification)
            println("✅ Generic notification sent: $title")
            logNotification("generic", title)
        }
    }
    
    private fun getValidChannelId(channelId: String): String {
        return when (channelId) {
            "level_up", "achievement" -> CHANNEL_ACHIEVEMENTS
            "warning" -> CHANNEL_USAGE_WARNINGS
            "limit" -> CHANNEL_USAGE_LIMITS
            "reminder" -> CHANNEL_REMINDERS
            else -> CHANNEL_REMINDERS // Default fallback
        }
    }
    
    // MARK: - Firebase Messaging Integration
    
    fun showMotivationalNotification(message: String) {
        sendNotification(
            title = "Motivation 💜",
            message = message,
            channelId = CHANNEL_REMINDERS,
            notificationId = 2001
        )
    }
    
    fun showStreakReminder() {
        sendNotification(
            title = "Keep Your Streak Going! 🔥",
            message = "Don't break your amazing progress! Check in with Awaytime.",
            channelId = CHANNEL_ACHIEVEMENTS,
            notificationId = 2002
        )
    }
    
    fun showWeeklyReviewNotification() {
        sendNotification(
            title = "Weekly Review Available 📊",
            message = "See how you did this week and celebrate your progress!",
            channelId = CHANNEL_REMINDERS,
            notificationId = 2003
        )
    }
    
    fun showCustomNotification(title: String, body: String) {
        sendNotification(
            title = title,
            message = body,
            channelId = CHANNEL_REMINDERS,
            notificationId = 2004
        )
    }
    
    // MARK: - System Notifications
    
    fun sendAccessibilityServiceDisabledNotification() {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("App Blocking Disabled ⚠️")
            .setContentText("Accessibility service was disabled. Tap to re-enable app blocking.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("The accessibility service required for app blocking has been disabled. " +
                        "Tap this notification to open settings and re-enable it to continue blocking apps."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_preferences,
                "Open Settings",
                pendingIntent
            )
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ACCESSIBILITY_ID, notification)
            android.util.Log.d("NotificationService", "Sent accessibility service disabled notification")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Failed to send accessibility notification", e)
        }
    }
    
    fun dismissAccessibilityServiceNotification() {
        notificationManager.cancel(NOTIFICATION_ACCESSIBILITY_ID)
    }
    
    // MARK: - Premium Trial Notifications
    
    /**
     * Show trial reminder notification
     */
    fun showTrialReminderNotification(message: String, hoursRemaining: Int) {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "premium",
                "notification_type" to "trial_reminder",
                "hours_remaining" to hoursRemaining
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PREMIUM_TRIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Premium Trial Reminder ⏰")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nUpgrade now to keep enjoying features like multiple app groups, advanced analytics, and more!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .addAction(
                android.R.drawable.ic_menu_info_details,
                "Upgrade Now",
                intent
            )
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_TRIAL_REMINDER_ID, notification)
            println("✅ Trial reminder notification sent: $message")
            logNotification("trial_reminder")
        }
    }
    
    /**
     * Show trial expired notification
     */
    fun showTrialExpiredNotification() {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "premium",
                "notification_type" to "trial_expired",
                "show_upgrade_prompt" to true
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PREMIUM_TRIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Premium Trial Expired 💎")
            .setContentText("Your 7-day trial has ended. Upgrade to continue using premium features!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your 7-day premium trial has expired. You can still use all basic features, but premium features like multiple app groups, advanced analytics, and custom themes are now locked.\n\nUpgrade to premium to unlock them again!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.warning.hashCode())
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Upgrade to Premium",
                intent
            )
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_TRIAL_EXPIRED_ID, notification)
            println("✅ Trial expired notification sent")
            logNotification("trial_expired")
        }
    }
    
    /**
     * Show upgrade prompt notification
     */
    fun showUpgradePromptNotification(delayHours: Int) {
        val message = when (delayHours) {
            1 -> "Missing your premium features? Upgrade now to get them back!"
            24 -> "Ready to unlock premium features again? Special upgrade offers available!"
            72 -> "Your premium features are waiting for you. Upgrade to continue your digital wellness journey!"
            else -> "Upgrade to premium and unlock all features again!"
        }
        
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "premium",
                "notification_type" to "upgrade_prompt",
                "delay_hours" to delayHours
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PREMIUM_TRIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upgrade to Premium 🌟")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nReclaim access to multiple app groups, advanced analytics, custom themes, data export, and more!")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.primary.hashCode())
            .addAction(
                android.R.drawable.ic_menu_add,
                "View Plans",
                intent
            )
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_UPGRADE_PROMPT_ID, notification)
            println("✅ Upgrade prompt notification sent (${delayHours}h delay)")
            logNotification("upgrade_prompt")
        }
    }
    
    /**
     * Show trial activation celebration notification
     */
    fun showTrialActivatedNotification() {
        val intent = createMainAppIntent(
            mapOf(
                "navigate_to" to "premium_features",
                "notification_type" to "trial_activated",
                "show_celebration" to true
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PREMIUM_TRIAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Premium Trial Started! 🎉")
            .setContentText("7 days of premium features unlocked! Explore multiple app groups, advanced analytics, and more.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Welcome to your 7-day premium trial! You now have access to:\n\n• Multiple app groups\n• Advanced analytics\n• Custom themes\n• Data export\n• Smart categorization\n• And much more!\n\nExplore all features in the app!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setColor(AwayTimeColors.success.hashCode())
            .addAction(
                android.R.drawable.ic_menu_view,
                "Explore Features",
                intent
            )
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_TRIAL_REMINDER_ID + 1, notification)
            println("✅ Trial activation notification sent")
            logNotification("trial_activated")
        }
    }
    
    /**
     * Cancel all trial-related notifications
     */
    fun cancelTrialNotifications() {
        notificationManager.cancel(NOTIFICATION_TRIAL_REMINDER_ID)
        notificationManager.cancel(NOTIFICATION_TRIAL_EXPIRED_ID)
        notificationManager.cancel(NOTIFICATION_UPGRADE_PROMPT_ID)
        notificationManager.cancel(NOTIFICATION_TRIAL_REMINDER_ID + 1) // Trial activated notification
        println("✅ All trial notifications cancelled")
    }
}

// MARK: - Data Models

data class NotificationStats(
    val warningsSent: Int,
    val limitsSent: Int,
    val streaksSent: Int,
    val totalSent: Int
) {
    val averagePerDay: Double
        get() = totalSent / 30.0 // Simple calculation - assume 30 days
}