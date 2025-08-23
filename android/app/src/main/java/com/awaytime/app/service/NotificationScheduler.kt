package com.awaytime.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.awaytime.app.data.entity.AppGroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

data class ScheduledNotification(
    val id: String,
    val type: NotificationType,
    val scheduledTime: String,
    val isActive: Boolean
) {
    enum class NotificationType {
        DAILY_CHECKIN,
        WEEKLY_REVIEW,
        MOTIVATION,
        SMART_REMINDER;

        val displayName: String
            get() = when (this) {
                DAILY_CHECKIN -> "Daily Check-in"
                WEEKLY_REVIEW -> "Weekly Review"
                MOTIVATION -> "Motivation"
                SMART_REMINDER -> "Smart Reminder"
            }

        val icon: String
            get() = when (this) {
                DAILY_CHECKIN -> "calendar_today"
                WEEKLY_REVIEW -> "bar_chart"
                MOTIVATION -> "favorite"
                SMART_REMINDER -> "lightbulb"
            }
            }
}

class NotificationScheduler(private val context: Context) {

    private val notificationService = NotificationService(context)
    private val goalTrackingService = GoalTrackingService(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("notification_scheduler", Context.MODE_PRIVATE)

    var scheduledNotifications by mutableStateOf<List<ScheduledNotification>>(emptyList())
        private set

    init {
        loadScheduledNotifications()
    }

    // MARK: - Usage-Based Notifications

    fun checkAndSendUsageNotifications(appGroup: AppGroupEntity, currentUsage: Int) {
        val dailyLimit = appGroup.dailyLimitMinutes
        val usagePercentage = currentUsage.toFloat() / dailyLimit.toFloat()

        // Warning at 80%
        if (usagePercentage >= 0.8f && usagePercentage < 1.0f) {
            val minutesRemaining = dailyLimit - currentUsage
            notificationService.sendWarningNotification(appGroup.name, minutesRemaining)
        }

        // Limit reached at 100%
        if (usagePercentage >= 1.0f) {
            notificationService.sendLimitReachedNotification(appGroup.name)
        }

        // Smart reminders at 50% and 70%
        if (usagePercentage >= 0.5f && usagePercentage < 0.55f) {
            notificationService.scheduleSmartReminder(appGroup.name, usagePercentage)
        }

        if (usagePercentage >= 0.7f && usagePercentage < 0.75f) {
            notificationService.scheduleSmartReminder(appGroup.name, usagePercentage)
        }
    }

    // MARK: - Streak Notifications

    fun checkAndSendStreakNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentStreak = goalTrackingService.currentStreak
            val milestones = listOf(3, 7, 14, 30, 60, 100)

            if (milestones.contains(currentStreak)) {
                notificationService.sendStreakAchievementNotification(currentStreak)
            }
        }
    }

    // MARK: - Scheduled Notifications

    fun scheduleDailyCheckIn(hour: Int, minute: Int = 0) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DAILY_CHECKIN"
            putExtra("title", "Daily Check-in 💜")
            putExtra(
                "message",
                "How did your screen time goals go today? Check your progress in Awaytime!"
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // If the time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        val scheduledNotification = ScheduledNotification(
            id = "daily_checkin",
            type = ScheduledNotification.NotificationType.DAILY_CHECKIN,
            scheduledTime = String.format("%02d:%02d", hour, minute),
            isActive = true
        )

        addScheduledNotification(scheduledNotification)
        println("✅ Daily check-in scheduled for $hour:${String.format("%02d", minute)}")
    }

    fun scheduleWeeklyReview(weekday: Int, hour: Int, minute: Int = 0) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "WEEKLY_REVIEW"
            putExtra("title", "Weekly Review 📊")
            putExtra(
                "message",
                "Time for your weekly screen time review! See how you did this week."
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, weekday) // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, etc.
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // If the time has passed this week, schedule for next week
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7, // Weekly
            pendingIntent
        )

        val weekdayName = getWeekdayName(weekday)
        val scheduledNotification = ScheduledNotification(
            id = "weekly_review",
            type = ScheduledNotification.NotificationType.WEEKLY_REVIEW,
            scheduledTime = "$weekdayName ${String.format("%02d:%02d", hour, minute)}",
            isActive = true
        )

        addScheduledNotification(scheduledNotification)
        println("✅ Weekly review scheduled for $weekdayName $hour:${String.format("%02d", minute)}")
    }

    fun scheduleMotivationalReminders() {
        val motivationalMessages = listOf(
            "You're building great habits! Keep it up! 💪",
            "Every mindful choice counts. You've got this! 🌟",
            "Progress, not perfection. You're doing amazing! 📈",
            "Your future self will thank you for this! 🙏"
        )

        motivationalMessages.forEachIndexed { index, message ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "MOTIVATION"
                putExtra("title", "Motivation Boost 🚀")
                putExtra("message", message)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2000 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, (index % 7) + 1)
                set(Calendar.HOUR_OF_DAY, 10 + (index * 2)) // Spread throughout the day
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)

                // If the time has passed this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Weekly
                pendingIntent
            )

            println("✅ Motivational reminder $index scheduled")
        }
    }

    // MARK: - Notification Management

    fun cancelScheduledNotification(notification: ScheduledNotification) {
        val requestCode = when (notification.type) {
            ScheduledNotification.NotificationType.DAILY_CHECKIN -> 1001
            ScheduledNotification.NotificationType.WEEKLY_REVIEW -> 1002
            ScheduledNotification.NotificationType.MOTIVATION -> 2000
            ScheduledNotification.NotificationType.SMART_REMINDER -> 3000
        }

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        removeScheduledNotification(notification)
        println("✅ Cancelled scheduled notification: ${notification.id}")
    }

    fun cancelAllScheduledNotifications() {
        scheduledNotifications.forEach { notification ->
            cancelScheduledNotification(notification)
        }
        scheduledNotifications = emptyList()
        saveScheduledNotifications()
    }

    private fun addScheduledNotification(notification: ScheduledNotification) {
        scheduledNotifications = scheduledNotifications + notification
        saveScheduledNotifications()
    }

    private fun removeScheduledNotification(notification: ScheduledNotification) {
        scheduledNotifications = scheduledNotifications.filter { it.id != notification.id }
        saveScheduledNotifications()
    }

    // MARK: - Persistence

    private fun loadScheduledNotifications() {
        // Simple implementation using SharedPreferences
        // In a real app, you might use Room database
        val notificationCount = prefs.getInt("notification_count", 0)
        val notifications = mutableListOf<ScheduledNotification>()

        for (i in 0 until notificationCount) {
            val id = prefs.getString("notification_${i}_id", "") ?: ""
            val type = prefs.getString("notification_${i}_type", "") ?: ""
            val time = prefs.getString("notification_${i}_time", "") ?: ""
            val isActive = prefs.getBoolean("notification_${i}_active", false)

            if (id.isNotEmpty() && type.isNotEmpty()) {
                val notificationType = try {
                    ScheduledNotification.NotificationType.valueOf(type)
                } catch (e: Exception) {
                    continue
                }

                notifications.add(
                    ScheduledNotification(
                        id = id,
                        type = notificationType,
                        scheduledTime = time,
                        isActive = isActive
                    )
                )
            }
        }

        scheduledNotifications = notifications
    }

    private fun saveScheduledNotifications() {
        val editor = prefs.edit()
        editor.putInt("notification_count", scheduledNotifications.size)

        scheduledNotifications.forEachIndexed { index, notification ->
            editor.putString("notification_${index}_id", notification.id)
            editor.putString("notification_${index}_type", notification.type.name)
            editor.putString("notification_${index}_time", notification.scheduledTime)
            editor.putBoolean("notification_${index}_active", notification.isActive)
        }

        editor.apply()
    }

    // MARK: - Utility

    private fun getWeekdayName(weekday: Int): String {
        return when (weekday) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Unknown"
        }
    }
}