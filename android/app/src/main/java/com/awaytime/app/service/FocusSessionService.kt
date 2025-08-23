package com.awaytime.app.service

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class FocusSessionState {
    IDLE,
    RUNNING,
    BREAK,
    PAUSED,
    COMPLETED
}

data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val focusDuration: Duration = 25.minutes,
    val shortBreakDuration: Duration = 5.minutes,
    val longBreakDuration: Duration = 15.minutes,
    val sessionsUntilLongBreak: Int = 4,
    val blockedApps: Set<String> = emptySet(),
    val startTime: Long = System.currentTimeMillis(),
    val state: FocusSessionState = FocusSessionState.IDLE
)

data class FocusSessionStats(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val totalFocusTime: Duration = Duration.ZERO,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

class FocusSessionService(private val context: Context) : ViewModel() {

    private val _currentSession = MutableStateFlow<FocusSession?>(null)
    val currentSession: StateFlow<FocusSession?> = _currentSession.asStateFlow()

    private val _remainingTime = MutableStateFlow(Duration.ZERO)
    val remainingTime: StateFlow<Duration> = _remainingTime.asStateFlow()

    private val _sessionStats = MutableStateFlow(FocusSessionStats())
    val sessionStats: StateFlow<FocusSessionStats> = _sessionStats.asStateFlow()

    private val _isBlocking = MutableStateFlow(false)
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    private var sessionTimer: Job? = null
    private var currentSessionCount = 0

    init {
        loadSessionStats()
    }

    // MARK: - Session Management

    fun startFocusSession(
        name: String = "Focus Session",
        focusDuration: Duration = 25.minutes,
        blockedApps: Set<String> = getDefaultBlockedApps()
    ) {
        if (!canUseFocusSessions()) {
            println("❌ Focus Sessions require premium subscription")
            return
        }

        val session = FocusSession(
            name = name,
            focusDuration = focusDuration,
            blockedApps = blockedApps,
            state = FocusSessionState.RUNNING
        )

        _currentSession.value = session
        _isBlocking.value = true
        startSessionTimer(focusDuration)
        
        // Enable app blocking
        enableAppBlocking(blockedApps)
        
        println("🎯 Focus session started: $name (${focusDuration.inWholeMinutes} min)")
    }

    fun pauseSession() {
        sessionTimer?.cancel()
        _currentSession.value = _currentSession.value?.copy(state = FocusSessionState.PAUSED)
        _isBlocking.value = false
        disableAppBlocking()
        println("⏸️ Focus session paused")
    }

    fun resumeSession() {
        val session = _currentSession.value ?: return
        if (session.state != FocusSessionState.PAUSED) return

        _currentSession.value = session.copy(state = FocusSessionState.RUNNING)
        _isBlocking.value = true
        startSessionTimer(_remainingTime.value)
        enableAppBlocking(session.blockedApps)
        println("▶️ Focus session resumed")
    }

    fun endSession() {
        sessionTimer?.cancel()
        _currentSession.value = null
        _remainingTime.value = Duration.ZERO
        _isBlocking.value = false
        disableAppBlocking()
        
        // Update stats
        updateSessionStats(completed = false)
        println("🛑 Focus session ended")
    }

    private fun startSessionTimer(duration: Duration) {
        sessionTimer?.cancel()
        
        sessionTimer = viewModelScope.launch {
            var remaining = duration
            _remainingTime.value = remaining
            
            while (remaining > Duration.ZERO && isActive) {
                delay(1000)
                remaining -= kotlin.time.Duration.parse("1s")
                _remainingTime.value = remaining
            }
            
            if (remaining <= Duration.ZERO) {
                onSessionCompleted()
            }
        }
    }

    private fun onSessionCompleted() {
        val session = _currentSession.value ?: return
        
        when (session.state) {
            FocusSessionState.RUNNING -> {
                // Focus session completed, start break
                currentSessionCount++
                updateSessionStats(completed = true)
                startBreak()
            }
            FocusSessionState.BREAK -> {
                // Break completed, offer to start new focus session
                completeBreak()
            }
            else -> { /* Handle other states */ }
        }
    }

    private fun startBreak() {
        val session = _currentSession.value ?: return
        val isLongBreak = currentSessionCount >= session.sessionsUntilLongBreak
        val breakDuration = if (isLongBreak) session.longBreakDuration else session.shortBreakDuration
        
        _currentSession.value = session.copy(state = FocusSessionState.BREAK)
        _isBlocking.value = false
        disableAppBlocking()
        
        if (isLongBreak) {
            currentSessionCount = 0
        }
        
        startSessionTimer(breakDuration)
        println("☕ ${if (isLongBreak) "Long" else "Short"} break started (${breakDuration.inWholeMinutes} min)")
    }

    private fun completeBreak() {
        _currentSession.value = null
        _remainingTime.value = Duration.ZERO
        println("✅ Break completed! Ready for next focus session")
    }

    // MARK: - App Blocking Integration

    private fun enableAppBlocking(blockedApps: Set<String>) {
        viewModelScope.launch {
            try {
                val appBlockingService = AppBlockingService(context)
                appBlockingService.blockApps(blockedApps.toList())
                println("🔒 Enabled app blocking for ${blockedApps.size} apps")
            } catch (e: Exception) {
                println("❌ Failed to enable app blocking: ${e.message}")
            }
        }
    }

    private fun disableAppBlocking() {
        viewModelScope.launch {
            try {
                val appBlockingService = AppBlockingService(context)
                appBlockingService.unblockAllApps()
                println("🔓 Disabled all app blocking")
            } catch (e: Exception) {
                println("❌ Failed to disable app blocking: ${e.message}")
            }
        }
    }

    private fun getDefaultBlockedApps(): Set<String> {
        return setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.twitter.android",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",
            "com.linkedin.android"
        )
    }

    // MARK: - Statistics Management

    private fun updateSessionStats(completed: Boolean) {
        val currentStats = _sessionStats.value
        val newStats = currentStats.copy(
            totalSessions = currentStats.totalSessions + 1,
            completedSessions = if (completed) currentStats.completedSessions + 1 else currentStats.completedSessions,
            totalFocusTime = currentStats.totalFocusTime + (_currentSession.value?.focusDuration ?: Duration.ZERO),
            currentStreak = if (completed) currentStats.currentStreak + 1 else 0,
            longestStreak = if (completed) maxOf(currentStats.longestStreak, currentStats.currentStreak + 1) else currentStats.longestStreak
        )
        
        _sessionStats.value = newStats
        saveSessionStats(newStats)
    }

    private fun loadSessionStats() {
        // Load from SharedPreferences or database
        val prefs = context.getSharedPreferences("focus_sessions", Context.MODE_PRIVATE)
        val stats = FocusSessionStats(
            totalSessions = prefs.getInt("total_sessions", 0),
            completedSessions = prefs.getInt("completed_sessions", 0),
            totalFocusTime = Duration.parse(prefs.getString("total_focus_time", "PT0S") ?: "PT0S"),
            currentStreak = prefs.getInt("current_streak", 0),
            longestStreak = prefs.getInt("longest_streak", 0)
        )
        _sessionStats.value = stats
    }

    private fun saveSessionStats(stats: FocusSessionStats) {
        val prefs = context.getSharedPreferences("focus_sessions", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("total_sessions", stats.totalSessions)
            .putInt("completed_sessions", stats.completedSessions)
            .putString("total_focus_time", stats.totalFocusTime.toString())
            .putInt("current_streak", stats.currentStreak)
            .putInt("longest_streak", stats.longestStreak)
            .apply()
    }

    // MARK: - Premium Feature Check

    private fun canUseFocusSessions(): Boolean {
        val subscriptionService = SubscriptionManager.getService()
        return subscriptionService?.canUseFeature(PremiumFeature.FOCUS_SESSIONS) ?: false
    }

    // MARK: - Preset Sessions

    fun getPresetSessions(): List<FocusSession> {
        return listOf(
            FocusSession(
                name = "Deep Work",
                focusDuration = 50.minutes,
                shortBreakDuration = 10.minutes,
                blockedApps = getDefaultBlockedApps()
            ),
            FocusSession(
                name = "Quick Focus",
                focusDuration = 15.minutes,
                shortBreakDuration = 5.minutes,
                blockedApps = getDefaultBlockedApps()
            ),
            FocusSession(
                name = "Study Session",
                focusDuration = 30.minutes,
                shortBreakDuration = 10.minutes,
                blockedApps = setOf(
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.snapchat.android",
                    "com.reddit.frontpage"
                )
            ),
            FocusSession(
                name = "Creative Flow",
                focusDuration = 90.minutes,
                shortBreakDuration = 20.minutes,
                blockedApps = setOf(
                    "com.twitter.android",
                    "com.facebook.katana",
                    "com.linkedin.android"
                )
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimer?.cancel()
        disableAppBlocking()
    }
} 