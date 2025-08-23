package com.awaytime.app.service

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.awaytime.app.MainActivity
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Integration service that connects gamification with other app services
 */
class GamificationIntegration(private val context: Context) : ViewModel() {

    private val gamificationService = GamificationService(context)
    private val goalTrackingService = GoalTrackingService(context)
    private val usageTrackingService = UsageTrackingService(context)
    private val notificationService = NotificationService(context)

    // Event flows for UI integration
    private val _motivationalToastFlow = MutableSharedFlow<String>(replay = 0)
    val motivationalToastFlow: SharedFlow<String> = _motivationalToastFlow.asSharedFlow()

    private val _experienceGainedFlow = MutableSharedFlow<ExperienceGainedEvent>(replay = 0)
    val experienceGainedFlow: SharedFlow<ExperienceGainedEvent> =
        _experienceGainedFlow.asSharedFlow()

    private val _confettiFlow = MutableSharedFlow<Unit>(replay = 0)
    val confettiFlow: SharedFlow<Unit> = _confettiFlow.asSharedFlow()

    init {
        setupIntegrations()
    }

    private fun setupIntegrations() {
        // Setup daily achievement check
        setupDailyAchievementCheck()

        // Perform initial check
        performDailyAchievementCheck()
    }

    // MARK: - Event Handlers

    fun handleGoalAchieved() {
        viewModelScope.launch {
            gamificationService.awardExperience(ExperienceAction.GOAL_ACHIEVED)
            gamificationService.checkAchievements()

            val message = gamificationService.getContextualMessage(MessageContext.GoalAchieved)
            _motivationalToastFlow.emit(message)

            _experienceGainedFlow.emit(
                ExperienceGainedEvent(
                    points = ExperienceAction.GOAL_ACHIEVED.points,
                    action = ExperienceAction.GOAL_ACHIEVED.description
                )
            )

            println("🎯 Goal achieved - XP awarded and achievements checked")
        }
    }

    fun handleLimitRespected() {
        viewModelScope.launch {
            gamificationService.awardExperience(ExperienceAction.LIMIT_RESPECTED)

            val message = gamificationService.getContextualMessage(MessageContext.LimitReached)
            _motivationalToastFlow.emit(message)

            _experienceGainedFlow.emit(
                ExperienceGainedEvent(
                    points = ExperienceAction.LIMIT_RESPECTED.points,
                    action = ExperienceAction.LIMIT_RESPECTED.description
                )
            )

            println("✅ Usage limit respected - XP awarded")
        }
    }

    fun handleStreakUpdated(streakDays: Int) {
        viewModelScope.launch {
            gamificationService.awardExperience(ExperienceAction.STREAK_DAY)

            // Check for streak milestones
            val milestones = listOf(3, 7, 14, 30, 60, 100)
            if (milestones.contains(streakDays)) {
                gamificationService.celebrationType = CelebrationType.StreakMilestone(streakDays)
                gamificationService.showCelebration()

                val message = gamificationService.getContextualMessage(
                    MessageContext.StreakMilestone(streakDays)
                )
                _motivationalToastFlow.emit(message)
                _confettiFlow.emit(Unit)
            }

            _experienceGainedFlow.emit(
                ExperienceGainedEvent(
                    points = ExperienceAction.STREAK_DAY.points,
                    action = ExperienceAction.STREAK_DAY.description
                )
            )

            println("🔥 Streak updated to $streakDays days - XP awarded")
        }
    }

    fun handleWeeklyGoalCompleted() {
        viewModelScope.launch {
            gamificationService.awardExperience(ExperienceAction.WEEKLY_GOAL_MET)
            gamificationService.checkAchievements()

            _experienceGainedFlow.emit(
                ExperienceGainedEvent(
                    points = ExperienceAction.WEEKLY_GOAL_MET.points,
                    action = ExperienceAction.WEEKLY_GOAL_MET.description
                )
            )

            println("📅 Weekly goal completed - bonus XP awarded")
        }
    }

    // MARK: - Daily Achievement Check

    private fun setupDailyAchievementCheck() {
        viewModelScope.launch {
            while (true) {
                delay(24 * 60 * 60 * 1000) // 24 hours
                performDailyAchievementCheck()
            }
        }
    }

    private fun performDailyAchievementCheck() {
        viewModelScope.launch {
            gamificationService.checkAchievements()
            checkForApproachingLimits()
        }
    }

    private fun checkForApproachingLimits() {
        viewModelScope.launch {
            val todayUsage = usageTrackingService.getTodayUsage()
            val goals = goalTrackingService.getCurrentGoals()

            for (goal in goals) {
                val appUsage = todayUsage.find { it.appName == goal.appIdentifier }
                if (appUsage != null) {
                    val usagePercentage = appUsage.usageTimeMinutes.toDouble() / goal.dailyLimit.toDouble()

                    // Show motivational message when approaching 80% of limit
                    if (usagePercentage >= 0.8 && usagePercentage < 1.0) {
                        val message = gamificationService.getContextualMessage(
                            MessageContext.ApproachingLimit
                        )
                        _motivationalToastFlow.emit(message)
                        break
                    }
                }
            }
        }
    }

    // MARK: - Public Interface

    fun getGamificationService(): GamificationService {
        return gamificationService
    }

    fun triggerManualAchievementCheck() {
        viewModelScope.launch {
            gamificationService.checkAchievements()
        }
    }

    fun getMotivationalMessageForContext(context: MessageContext): String {
        return gamificationService.getContextualMessage(context)
    }

    fun awardBonusExperience(points: Int, reason: String) {
        viewModelScope.launch {
            // For special events or manual rewards
            gamificationService.addExperiencePoints(points)

            _experienceGainedFlow.emit(
                ExperienceGainedEvent(
                    points = points,
                    action = reason
                )
            )

            println("🌟 Bonus XP awarded: $points for $reason")
        }
    }
}

// MARK: - Data Classes

data class ExperienceGainedEvent(
    val points: Int,
    val action: String
)

// MARK: - UI Integration Components

@Composable
fun MotivationalToast(
    message: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        isVisible = true
        delay(4000) // Show for 4 seconds
        isVisible = false
        delay(300) // Wait for animation
        onDismiss()
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        ) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it },
            animationSpec = androidx.compose.animation.core.tween(300)
        ) + androidx.compose.animation.fadeOut()
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.BottomCenter
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = AwayTimeColors.primary.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "💜",
                    fontSize = 20.sp
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = androidx.compose.ui.Modifier.width(12.dp)
                )
                androidx.compose.material3.Text(
                    text = message,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ExperienceGainedAnimation(
    points: Int,
    action: String,
    onComplete: () -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = -100f,
            animationSpec = androidx.compose.animation.core.tween(2000)
        ) { value, _ ->
            offsetY = value
        }

        androidx.compose.animation.core.animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(2000)
        ) { value, _ ->
            alpha = value
        }

        onComplete()
    }

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = androidx.compose.ui.Modifier
                .offset(y = offsetY.dp)
                .alpha(alpha)
                .background(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            androidx.compose.material3.Text(
                text = "+$points XP",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = AwayTimeColors.primary
            )
            androidx.compose.material3.Text(
                text = action,
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Integration with MainActivity

/**
 * Extension functions to integrate gamification with the main activity
 */
fun MainActivity.setupGamificationIntegration() {
    val integration = GamificationIntegration(this)

    // Observe motivational toast events
    lifecycleScope.launch {
        integration.motivationalToastFlow.collect { message ->
            // Show toast in UI
            showMotivationalToast(message)
        }
    }

    // Observe experience gained events
    lifecycleScope.launch {
        integration.experienceGainedFlow.collect { event ->
            // Show XP animation
            showExperienceAnimation(event.points, event.action)
        }
    }

    // Observe confetti events
    lifecycleScope.launch {
        integration.confettiFlow.collect {
            // Trigger confetti animation
            showConfettiAnimation()
        }
    }
}

private fun MainActivity.showMotivationalToast(message: String) {
    // Implementation would depend on your toast system
    println("Showing motivational toast: $message")
}

private fun MainActivity.showExperienceAnimation(points: Int, action: String) {
    // Implementation would depend on your animation system
    println("Showing XP animation: +$points XP for $action")
}

private fun MainActivity.showConfettiAnimation() {
    // Implementation would depend on your animation system
    println("Showing confetti animation")
}