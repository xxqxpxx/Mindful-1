package com.awaytime.app.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit

/**
 * A precise countdown executor that provides accurate timing for focus sessions.
 * Adapted from Mindful's PreciseCountDownExecutor.
 */
class PreciseCountDownExecutor(
    private val duration: Long,
    private val interval: Long = 1L,
    private val timeUnit: TimeUnit = TimeUnit.SECONDS,
    private val onTick: (elapsedTime: Long) -> Unit,
    private val onFinish: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0
    private var isRunning = false
    private var isCancelled = false
    
    private val durationMs = timeUnit.toMillis(duration)
    private val intervalMs = timeUnit.toMillis(interval)
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCancelled) return
            
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime
            val elapsedInOriginalUnit = timeUnit.convert(elapsedTime, TimeUnit.MILLISECONDS)
            
            if (elapsedTime >= durationMs) {
                // Timer finished
                isRunning = false
                onFinish()
            } else {
                // Continue timer
                onTick(elapsedInOriginalUnit)
                handler.postDelayed(this, intervalMs)
            }
        }
    }
    
    /**
     * Starts the countdown timer
     */
    fun start() {
        if (isRunning) return
        
        isRunning = true
        isCancelled = false
        startTime = System.currentTimeMillis()
        handler.post(timerRunnable)
    }
    
    /**
     * Cancels the countdown timer
     */
    fun cancel() {
        isRunning = false
        isCancelled = true
        handler.removeCallbacks(timerRunnable)
    }
    
    /**
     * Returns whether the timer is currently running
     */
    fun isRunning(): Boolean = isRunning
}