package com.awaytime.app.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.awaytime.app.cache.DatabaseCache
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean


data class Goal(
    val id: String,
    val appIdentifier: String,
    val dailyLimit: Long,
    val currentUsage: Long
) {
    val progress: Double
        get() = currentUsage.toDouble() / dailyLimit.toDouble()
    
    val isLimitExceeded: Boolean
        get() = currentUsage >= dailyLimit
}


data class GoalStatistics(
    val currentStreak: Int,
    val longestStreak: Int,
    val successRate: Double,
    val successfulDays: Int,
    val totalDays: Int,
    val weeklyProgress: List<Boolean>
) {
    val successRatePercentage: Int
        get() = (successRate * 100).toInt()
}

data class GoalRecommendation(
    val type: RecommendationType,
    val message: String,
    val suggestion: String
) {
    enum class RecommendationType {
        EXCELLENT,
        GOOD,
        WARNING,
        EXCEEDED;

        val color: androidx.compose.ui.graphics.Color
            get() = when (this) {
                EXCELLENT -> AwayTimeColors.success
                GOOD -> AwayTimeColors.primary
                WARNING -> AwayTimeColors.warning
                EXCEEDED -> androidx.compose.ui.graphics.Color.Red
            }

        val icon: String
            get() = when (this) {
                EXCELLENT -> "star"
                GOOD -> "check_circle"
                WARNING -> "warning"
                EXCEEDED -> "cancel"
            }
    }
}

data class UsageRecord(
    val appName: String,
    val usageMinutes: Int,
    val limitMinutes: Int,
    val date: Date,
    val limitExceeded: Boolean
)

class GoalTrackingService(private val context: Context) {

    private val repository = AwayTimeRepository(context)
    private val notificationService = NotificationService(context)
    private val prefs = context.getSharedPreferences("awaytime_goals", Context.MODE_PRIVATE)
    private val cache = DatabaseCache.getInstance()
    
    // Debouncing control
    private val streakUpdateDebouncer = AtomicBoolean(false)

    var currentStreak by mutableStateOf(0)
        private set

    var longestStreak by mutableStateOf(0)
        private set

    var todayGoalMet by mutableStateOf(false)
        private set

    var weeklyProgress by mutableStateOf<List<Boolean>>(emptyList())
        private set

    init {
        loadGoalData()
    }

    // MARK: - Goal Tracking

    suspend fun checkDailyGoal(appGroup: AppGroupEntity) {
        val todayUsage = repository.getTodayUsage(appGroup.name)
        val dailyLimit = appGroup.dailyLimitMinutes

        val goalMet = todayUsage <= dailyLimit

        if (goalMet != todayGoalMet) {
            todayGoalMet = goalMet

            if (goalMet) {
                handleGoalAchieved()
            }
        }

        updateStreak()
    }

    private suspend fun handleGoalAchieved() {
        // Send achievement notification if it's a milestone
        if (shouldCelebrateStreak()) {
            notificationService.sendStreakAchievementNotification(currentStreak + 1)
        }

        // Trigger confetti animation
        // This would typically be handled through a callback or event system
        println("🎉 Goal achieved! Show confetti animation")
    }

    private suspend fun updateStreak() {
        // Debounce streak updates to prevent excessive calculations
        if (streakUpdateDebouncer.getAndSet(true)) {
            delay(5000) // Wait 5 seconds before resetting
            streakUpdateDebouncer.set(false)
            return
        }
        
        val newStreak = repository.calculateStreak()

        if (newStreak > currentStreak) {
            currentStreak = newStreak

            if (newStreak > longestStreak) {
                longestStreak = newStreak
                saveLongestStreak()
            }

            // Update repository with debouncing protection
            repository.updateStreak(newStreak)
        }

        updateWeeklyProgress()
        
        // Reset debouncer after 30 seconds
        delay(30_000)
        streakUpdateDebouncer.set(false)
    }

    private suspend fun updateWeeklyProgress() {
        val calendar = Calendar.getInstance()
        val today = Date()
        val appGroup = getCurrentAppGroup()

        // Get the last 7 days
        val progress = mutableListOf<Boolean>()

        for (dayOffset in 6 downTo 0) {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_MONTH, -dayOffset)
            val date = calendar.time

            val dayStart = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val dayEnd = Calendar.getInstance().apply {
                time = dayStart
                add(Calendar.DAY_OF_MONTH, 1)
            }.time

            val records = repository.getUsageRecordsForDateRange(dayStart, dayEnd)
            var totalUsage = 0
            for (record in records) {
                totalUsage += record.usageMinutes
            }
            val dailyLimit = appGroup?.dailyLimitMinutes ?: 120 // Default to 2 hours if no group

            val goalMet = totalUsage <= dailyLimit
            progress.add(goalMet)
        }

        weeklyProgress = progress
    }

    // MARK: - Goal Statistics

    suspend fun getGoalStatistics(): GoalStatistics {
        val calendar = Calendar.getInstance()
        val today = Date()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val thirtyDaysAgo = calendar.time

        val records = repository.getUsageRecordsForDateRange(thirtyDaysAgo, today)
        val successfulDays = records.count { !it.limitExceeded }
        val totalDays = records.size

        val successRate =
            if (totalDays > 0) successfulDays.toDouble() / totalDays.toDouble() else 0.0

        return GoalStatistics(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            successRate = successRate,
            successfulDays = successfulDays,
            totalDays = totalDays,
            weeklyProgress = weeklyProgress
        )
    }

    fun getMotivationalMessage(): String {
        val messages = listOf(
            "You're doing great! Keep it up! 💜",
            "Every small step counts towards your goal! 🌟",
            "Building healthy habits takes time. You've got this! 💪",
            "Your future self will thank you for this! 🙏",
            "Progress, not perfection! 📈",
            "You're stronger than your distractions! 🔥",
            "One day at a time, one goal at a time! ⭐",
            "Your mindful choices are creating positive change! 🌱"
        )

        return messages.random()
    }

    // MARK: - Streak Milestones

    private fun shouldCelebrateStreak(): Boolean {
        val milestones = listOf(3, 7, 14, 30, 60, 100)
        return milestones.contains(currentStreak + 1)
    }

    fun getStreakMilestoneMessage(streak: Int): String {
        return when (streak) {
            3 -> "3 days strong! You're building momentum! 🚀"
            7 -> "One week of success! You're on fire! 🔥"
            14 -> "Two weeks! You're forming a real habit! 💎"
            30 -> "30 days! This is becoming second nature! 🏆"
            60 -> "60 days! You're a screen time master! 👑"
            100 -> "100 days! Absolutely incredible! 🎉"
            else -> "Amazing streak! Keep it going! ⭐"
        }
    }

    // MARK: - Data Persistence

    private fun loadGoalData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to load from cache first
                val cachedStreak = cache.getCachedStreak()
                val userSettings = repository.getUserSettingsSync()
                
                currentStreak = cachedStreak ?: userSettings.streakCount
                longestStreak = prefs.getInt("longestStreak", 0)

                updateWeeklyProgress()

                // Check today's goal status
                getCurrentAppGroup()?.let { appGroup ->
                    checkDailyGoal(appGroup)
                }
            } catch (e: Exception) {
                println("❌ Failed to load goal data: ${e.message}")
            }
        }
    }

    private fun saveLongestStreak() {
        prefs.edit().putInt("longestStreak", longestStreak).apply()
    }

    private suspend fun getCurrentAppGroup(): AppGroupEntity? {
        return repository.getAllAppGroups().first().firstOrNull()
    }

    // MARK: - Goal Recommendations

    fun getGoalRecommendation(currentUsage: Int, currentLimit: Int): GoalRecommendation {
        val usagePercentage =
            if (currentLimit > 0) currentUsage.toDouble() / currentLimit.toDouble() else 0.0

        return when {
            usagePercentage <= 0.5 -> GoalRecommendation(
                type = GoalRecommendation.RecommendationType.EXCELLENT,
                message = "Excellent! You're using only ${(usagePercentage * 100).toInt()}% of your limit!",
                suggestion = "Consider reducing your limit by 15-30 minutes to challenge yourself further."
            )

            usagePercentage <= 0.8 -> GoalRecommendation(
                type = GoalRecommendation.RecommendationType.GOOD,
                message = "Good progress! You're at ${(usagePercentage * 100).toInt()}% of your limit.",
                suggestion = "You're on track! Try to maintain this pace for the rest of the day."
            )

            usagePercentage <= 1.0 -> GoalRecommendation(
                type = GoalRecommendation.RecommendationType.WARNING,
                message = "You're close to your limit at ${(usagePercentage * 100).toInt()}%.",
                suggestion = "Consider taking a break or switching to a different activity."
            )

            else -> GoalRecommendation(
                type = GoalRecommendation.RecommendationType.EXCEEDED,
                message = "You've exceeded your daily limit.",
                suggestion = "Tomorrow is a fresh start! Consider what led to today's usage and plan accordingly."
            )
        }
    }

    // MARK: - Usage Data Methods
    
    suspend fun getTodayUsageRecords(): List<UsageRecord> {
        val calendar = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return try {
            repository.getUsageRecordsBetween(startOfDay.timeInMillis, endOfDay.timeInMillis).first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - Utility Methods

    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60

        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun getProgressColor(usagePercentage: Float): androidx.compose.ui.graphics.Color {
        return when {
            usagePercentage <= 0.5f -> AwayTimeColors.success
            usagePercentage <= 0.8f -> AwayTimeColors.primary
            usagePercentage <= 1.0f -> AwayTimeColors.warning
            else -> androidx.compose.ui.graphics.Color.Red
        }
    }
    
    // MARK: - External API methods (for ErrorIntegration compatibility)
    
    fun saveGoal(goal: Goal) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.saveAppGroup(
                    name = goal.appIdentifier,
                    dailyLimitMinutes = goal.dailyLimit.toInt(),
                    selectedApps = listOf(goal.appIdentifier)
                )
            } catch (e: Exception) {
                throw Exception("Failed to save goal: ${e.message}")
            }
        }
    }
    
    fun getCurrentGoals(): List<Goal> {
        return try {
            kotlinx.coroutines.runBlocking {
                val appGroups = repository.getAllAppGroups().first()
                appGroups.map { appGroup ->
                    val currentUsage = repository.getTodayUsage(appGroup.name)
                    Goal(
                        id = appGroup.id,
                        appIdentifier = appGroup.name,
                        dailyLimit = appGroup.dailyLimitMinutes.toLong(),
                        currentUsage = currentUsage.toLong()
                    )
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to get current goals: ${e.message}")
        }
    }
    
    fun updateGoalProgress(goalId: String, progress: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appGroup = repository.getAppGroupById(goalId)
                if (appGroup != null) {
                    val newUsage = (appGroup.dailyLimitMinutes * progress).toInt()
                    repository.saveUsageRecord(
                        date = Date(),
                        usageMinutes = newUsage,
                        appGroupName = appGroup.name,
                        limitExceeded = newUsage >= appGroup.dailyLimitMinutes
                    )
                }
            } catch (e: Exception) {
                throw Exception("Failed to update goal progress: ${e.message}")
            }
        }
    }
}

