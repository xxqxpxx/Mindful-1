package com.awaytime.app.models

/**
 * Represents a focus session with metadata like duration, type, state, and apps to block.
 * Adapted from Mindful's FocusSession model.
 */
data class FocusSession(
    /**
     * Unique identifier for this session
     */
    val id: String = "",
    
    /**
     * Type of focus session (study, work, break, etc.)
     */
    val sessionType: SessionType = SessionType.FOCUS,
    
    /**
     * Flag indicating whether to start DND mode
     */
    val toggleDnd: Boolean = false,
    
    /**
     * Start time in milliseconds since epoch for this focus session
     */
    val startTimeMsEpoch: Long = 0L,
    
    /**
     * Duration of the session in seconds. 0 = infinite session
     */
    val durationSecs: Int = 0,
    
    /**
     * Set of app package names identified as distracting apps to block
     */
    val distractingApps: Set<String> = emptySet(),
    
    /**
     * Whether this session is currently active
     */
    val isActive: Boolean = false,
    
    /**
     * Optional label for this session
     */
    val label: String = "",
    
    /**
     * Whether to enable notification batching during this session
     */
    val batchNotifications: Boolean = true
) {
    /**
     * Calculates remaining time in seconds for finite sessions
     */
    fun getRemainingTimeSeconds(): Long {
        if (durationSecs <= 0) return -1 // Infinite session
        
        val elapsedMs = System.currentTimeMillis() - startTimeMsEpoch
        val elapsedSecs = elapsedMs / 1000L
        return maxOf(0L, durationSecs - elapsedSecs)
    }
    
    /**
     * Calculates elapsed time in seconds
     */
    fun getElapsedTimeSeconds(): Long {
        val elapsedMs = System.currentTimeMillis() - startTimeMsEpoch
        return elapsedMs / 1000L
    }
    
    /**
     * Returns true if this is a finite session (has duration > 0)
     */
    fun isFinite(): Boolean = durationSecs > 0
    
    /**
     * Returns true if the session has completed (for finite sessions)
     */
    fun isCompleted(): Boolean {
        return if (isFinite()) {
            getRemainingTimeSeconds() <= 0
        } else {
            false
        }
    }
}

enum class SessionType {
    FOCUS,
    STUDY,
    WORK,
    CREATIVE,
    MEDITATION,
    BREAK,
    CUSTOM
}