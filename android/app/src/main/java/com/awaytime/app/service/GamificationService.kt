package com.awaytime.app.service

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val rarity: Rarity,
    val unlockedDate: Long = System.currentTimeMillis()
) {
    enum class Rarity {
        COMMON, RARE, EPIC, LEGENDARY;

        val color: Color
            get() = when (this) {
                COMMON -> Color.Gray
                RARE -> Color.Blue
                EPIC -> AwayTimeColors.primary
                LEGENDARY -> Color(0xFFFF9500) // Orange
            }

        val displayName: String
            get() = name.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        val firstSuccess = Achievement(
            id = "first_success",
            title = "First Steps",
            description = "Complete your first successful day",
            icon = "🌱",
            rarity = Rarity.COMMON
        )

        fun streak(days: Int) = Achievement(
            id = "streak_$days",
            title = "$days Day Streak",
            description = "Maintain your goals for $days consecutive days",
            icon = if (days >= 30) "🔥" else "⭐",
            rarity = when {
                days >= 30 -> Rarity.EPIC
                days >= 7 -> Rarity.RARE
                else -> Rarity.COMMON
            }
        )

        val perfectionist = Achievement(
            id = "perfectionist",
            title = "Perfectionist",
            description = "Achieve 90%+ success rate over 30 days",
            icon = "💎",
            rarity = Rarity.LEGENDARY
        )

        val consistent = Achievement(
            id = "consistent",
            title = "Consistent",
            description = "Maintain 75%+ success rate over 30 days",
            icon = "🎯",
            rarity = Rarity.EPIC
        )

        val weekendWarrior = Achievement(
            id = "weekend_warrior",
            title = "Weekend Warrior",
            description = "Stay on track during the weekend",
            icon = "🏖️",
            rarity = Rarity.RARE
        )
    }
}

data class Badge(
    val id: String,
    val title: String,
    val icon: String,
    val color: String,
    val earnedDate: Long = System.currentTimeMillis()
) {
    companion object {
        fun levelUp(level: Int) = Badge(
            id = "level_$level",
            title = "Level $level",
            icon = "⭐",
            color = "purple"
        )

        fun achievement(achievement: Achievement) = Badge(
            id = "achievement_${achievement.id}",
            title = achievement.title,
            icon = achievement.icon,
            color = achievement.rarity.displayName.lowercase()
        )
    }
}

enum class ExperienceAction(val points: Int, val description: String) {
    GOAL_ACHIEVED(50, "Goal Achieved"),
    STREAK_DAY(25, "Streak Day"),
    LIMIT_RESPECTED(20, "Limit Respected"),
    ACHIEVEMENT_UNLOCKED(100, "Achievement Unlocked"),
    WEEKLY_GOAL_MET(150, "Weekly Goal Met")
}

sealed class CelebrationType {
    object GoalAchieved : CelebrationType()
    data class LevelUp(val from: Int, val to: Int) : CelebrationType()
    data class AchievementUnlocked(val achievement: Achievement) : CelebrationType()
    data class StreakMilestone(val days: Int) : CelebrationType()
}

data class MotivationalMessage(
    val text: String,
    val type: MessageType,
    val icon: String
) {
    enum class MessageType {
        ENCOURAGEMENT, ACHIEVEMENT, INSPIRATION, PROGRESS, WISDOM
    }
}

sealed class MessageContext {
    object ApproachingLimit : MessageContext()
    object LimitReached : MessageContext()
    object GoalAchieved : MessageContext()
    data class StreakMilestone(val days: Int) : MessageContext()
}

data class GamificationData(
    val currentLevel: Int,
    val experiencePoints: Int,
    val achievements: List<Achievement>,
    val badges: List<Badge>
)

class GamificationService(private val context: Context) {

    private val goalTrackingService = GoalTrackingService(context)
    private val notificationService = NotificationService(context)
    private val prefs = context.getSharedPreferences("gamification", Context.MODE_PRIVATE)

    var currentLevel by mutableStateOf(1)
        private set

    var experiencePoints by mutableStateOf(0)
        private set

    fun addExperiencePoints(points: Int) {
        experiencePoints += points
    }

    var achievements by mutableStateOf<List<Achievement>>(emptyList())
        private set

    var badges by mutableStateOf<List<Badge>>(emptyList())
        private set

    var showingCelebration by mutableStateOf(false)
        private set

    var celebrationType by mutableStateOf<CelebrationType>(CelebrationType.GoalAchieved)

    // Experience points needed for each level
    private val levelThresholds =
        listOf(0, 100, 250, 500, 1000, 1750, 2750, 4000, 5500, 7500, 10000)

    init {
        loadGamificationData()
    }

    // MARK: - Experience Points System

    fun awardExperience(action: ExperienceAction) {
        val points = action.points
        experiencePoints += points

        // Check for level up
        val newLevel = calculateLevel(experiencePoints)
        if (newLevel > currentLevel) {
            levelUp(newLevel)
        }

        // Save progress
        saveGamificationData()

        // Show experience gained animation
        showExperienceGained(points, action)

        println("✅ Awarded $points XP for ${action.description}")
    }

    private fun calculateLevel(xp: Int): Int {
        for ((level, threshold) in levelThresholds.withIndex()) {
            if (xp < threshold) {
                return maxOf(1, level)
            }
        }
        return levelThresholds.size
    }

    private fun levelUp(newLevel: Int) {
        val oldLevel = currentLevel
        currentLevel = newLevel

        // Award level up badge
        awardBadge(Badge.levelUp(newLevel))

        // Show celebration
        celebrationType = CelebrationType.LevelUp(oldLevel, newLevel)
        showCelebration()

        // Send notification
        notificationService.sendLevelUpNotification(newLevel)

        println("🎉 Level up! Now level $newLevel")
    }

    // MARK: - Achievement System

    suspend fun checkAchievements() {
        val stats = goalTrackingService.getGoalStatistics()

        // Streak achievements
        checkStreakAchievements(stats.currentStreak)

        // Success rate achievements
        checkSuccessRateAchievements(stats.successRate)

        // Usage achievements
        checkUsageAchievements()

        // Special achievements
        checkSpecialAchievements()
    }

    private fun checkStreakAchievements(currentStreak: Int) {
        val streakMilestones = listOf(3, 7, 14, 30, 60, 100)

        for (milestone in streakMilestones) {
            if (currentStreak >= milestone) {
                val achievement = Achievement.streak(milestone)
                if (!hasAchievement(achievement)) {
                    unlockAchievement(achievement)
                }
            }
        }
    }

    private fun checkSuccessRateAchievements(successRate: Double) {
        if (successRate >= 0.9) {
            val achievement = Achievement.perfectionist
            if (!hasAchievement(achievement)) {
                unlockAchievement(achievement)
            }
        }

        if (successRate >= 0.75) {
            val achievement = Achievement.consistent
            if (!hasAchievement(achievement)) {
                unlockAchievement(achievement)
            }
        }
    }

    private suspend fun checkUsageAchievements() {
        // Check for first successful day
        val todayRecords = goalTrackingService.getTodayUsageRecords()
        if (todayRecords.any { !it.limitExceeded }) {
            val achievement = Achievement.firstSuccess
            if (!hasAchievement(achievement)) {
                unlockAchievement(achievement)
            }
        }
    }

    private suspend fun checkSpecialAchievements() {
        // Weekend warrior - successful weekend day
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            val todayRecords = goalTrackingService.getTodayUsageRecords()
            if (todayRecords.any { !it.limitExceeded }) {
                val achievement = Achievement.weekendWarrior
                if (!hasAchievement(achievement)) {
                    unlockAchievement(achievement)
                }
            }
        }
    }

    private fun unlockAchievement(achievement: Achievement) {
        achievements = achievements + achievement

        // Award experience points
        awardExperience(ExperienceAction.ACHIEVEMENT_UNLOCKED)

        // Award badge
        awardBadge(Badge.achievement(achievement))

        // Show celebration
        celebrationType = CelebrationType.AchievementUnlocked(achievement)
        showCelebration()

        // Send notification
        notificationService.sendAchievementNotification(achievement)

        println("🏆 Achievement unlocked: ${achievement.title}")
    }

    // MARK: - Badge System

    fun awardBadge(badge: Badge) {
        if (!hasBadge(badge)) {
            badges = badges + badge
            saveGamificationData()

            println("🎖️ Badge earned: ${badge.title}")
        }
    }

    private fun hasBadge(badge: Badge): Boolean {
        return badges.any { it.id == badge.id }
    }

    private fun hasAchievement(achievement: Achievement): Boolean {
        return achievements.any { it.id == achievement.id }
    }

    // MARK: - Celebration System

    fun showCelebration() {
        showingCelebration = true

        // Auto-hide after 3 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            showingCelebration = false
        }

        println("🎉 Showing celebration animation")
    }

    private fun showExperienceGained(points: Int, action: ExperienceAction) {
        println("✨ +$points XP for ${action.description}")
    }

    // MARK: - Motivational Messages

    fun getMotivationalMessage(): MotivationalMessage {
        val messages = listOf(
            MotivationalMessage(
                text = "You're building incredible self-discipline! 💪",
                type = MotivationalMessage.MessageType.ENCOURAGEMENT,
                icon = "💪"
            ),
            MotivationalMessage(
                text = "Every mindful choice is a victory! 🏆",
                type = MotivationalMessage.MessageType.ACHIEVEMENT,
                icon = "🏆"
            ),
            MotivationalMessage(
                text = "Your future self is cheering you on! 🌟",
                type = MotivationalMessage.MessageType.INSPIRATION,
                icon = "🌟"
            ),
            MotivationalMessage(
                text = "Progress over perfection - you're doing great! 📈",
                type = MotivationalMessage.MessageType.PROGRESS,
                icon = "📈"
            ),
            MotivationalMessage(
                text = "Small steps lead to big changes! 🚶‍♀️",
                type = MotivationalMessage.MessageType.WISDOM,
                icon = "🚶‍♀️"
            )
        )

        return messages.random()
    }

    fun getContextualMessage(situation: MessageContext): String {
        return when (situation) {
            is MessageContext.ApproachingLimit -> listOf(
                "You're almost at your limit! How about a quick walk? 🚶‍♀️",
                "Time for a mindful break! Your eyes will thank you 👀",
                "You've got this! Maybe try some deep breathing? 🧘‍♀️"
            ).random()

            is MessageContext.LimitReached -> listOf(
                "Great job staying mindful today! Tomorrow is a fresh start 🌅",
                "You respected your limits - that's real strength! 💪",
                "Time well spent! Enjoy your screen-free time 🌟"
            ).random()

            is MessageContext.GoalAchieved -> listOf(
                "Incredible! You crushed your goal today! 🎉",
                "Goal achieved! You're building amazing habits! 🏆",
                "Success! Your dedication is paying off! ⭐"
            ).random()

            is MessageContext.StreakMilestone -> getStreakMessage(situation.days)
        }
    }

    private fun getStreakMessage(days: Int): String {
        return when (days) {
            3 -> "3 days strong! You're building momentum! 🚀"
            7 -> "One week of success! You're on fire! 🔥"
            14 -> "Two weeks! You're forming a real habit! 💎"
            30 -> "30 days! This is becoming second nature! 🏆"
            60 -> "60 days! You're a screen time master! 👑"
            100 -> "100 days! Absolutely legendary! 🎉"
            else -> "Amazing streak! Keep the momentum going! ⭐"
        }
    }

    // MARK: - Data Persistence

    private fun saveGamificationData() {
        with(prefs.edit()) {
            putInt("currentLevel", currentLevel)
            putInt("experiencePoints", experiencePoints)
            putInt("achievementCount", achievements.size)
            putInt("badgeCount", badges.size)
            apply()
        }
    }

    private fun loadGamificationData() {
        currentLevel = prefs.getInt("currentLevel", 1)
        experiencePoints = prefs.getInt("experiencePoints", 0)
        achievements = emptyList()
        badges = emptyList()
    }

    // MARK: - Utility Methods

    fun getProgressToNextLevel(): Float {
        if (currentLevel >= levelThresholds.size) return 1f

        val currentLevelXP = levelThresholds[currentLevel - 1]
        val nextLevelXP = levelThresholds[currentLevel]
        val progressXP = experiencePoints - currentLevelXP
        val requiredXP = nextLevelXP - currentLevelXP

        return progressXP.toFloat() / requiredXP.toFloat()
    }

    fun getXPToNextLevel(): Int {
        if (currentLevel >= levelThresholds.size) return 0
        return levelThresholds[currentLevel] - experiencePoints
    }
}

// Extension functions for NotificationService
fun NotificationService.sendLevelUpNotification(level: Int) {
    sendNotification(
        title = "Level Up! 🎉",
        message = "Congratulations! You've reached level $level!",
        channelId = "level_up"
    )
}

fun NotificationService.sendAchievementNotification(achievement: Achievement) {
    sendNotification(
        title = "Achievement Unlocked! 🏆",
        message = "${achievement.icon} ${achievement.title}: ${achievement.description}",
        channelId = "achievement"
    )
}