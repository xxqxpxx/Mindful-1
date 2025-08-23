package com.awaytime.app.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FoxMoodService(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fox_mood_prefs", Context.MODE_PRIVATE)
    
    private val _currentMood = MutableStateFlow(FoxMood.HAPPY)
    val currentMood: StateFlow<FoxMood> = _currentMood.asStateFlow()
    
    private val _usagePercent = MutableStateFlow(0f)
    val usagePercent: StateFlow<Float> = _usagePercent.asStateFlow()
    
    init {
        // Load saved mood
        val savedMood = prefs.getString("current_mood", FoxMood.HAPPY.name)
        _currentMood.value = FoxMood.valueOf(savedMood ?: FoxMood.HAPPY.name)
    }
    
    fun updateUsagePercent(percent: Float) {
        _usagePercent.value = percent
        val newMood = determineMoodFromUsage(percent)
        
        if (newMood != _currentMood.value) {
            _currentMood.value = newMood
            saveMood(newMood)
        }
    }
    
    private fun determineMoodFromUsage(percent: Float): FoxMood {
        return when {
            percent <= 40f -> FoxMood.HAPPY
            percent <= 70f -> FoxMood.SLEEPY
            percent <= 90f -> FoxMood.SAD
            else -> FoxMood.EXHAUSTED
        }
    }
    
    private fun saveMood(mood: FoxMood) {
        prefs.edit().putString("current_mood", mood.name).apply()
    }
    
    fun getMoodMessage(mood: FoxMood): String {
        return when (mood) {
            FoxMood.HAPPY -> "Great job! You're managing your screen time well."
            FoxMood.SLEEPY -> "You're doing okay, but consider taking a break soon."
            FoxMood.SAD -> "You're getting close to your limit. Time for a break?"
            FoxMood.EXHAUSTED -> "You've reached your limit. Please take a break!"
        }
    }
    
    fun getMoodColor(mood: FoxMood): Long {
        return when (mood) {
            FoxMood.HAPPY -> 0xFF4CAF50 // Green
            FoxMood.SLEEPY -> 0xFFFF9800 // Orange
            FoxMood.SAD -> 0xFF2196F3 // Blue
            FoxMood.EXHAUSTED -> 0xFF9E9E9E // Gray
        }
    }
}