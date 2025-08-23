package com.awaytime.app.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * Throttler for both regular and suspend functions to prevent excessive calls
 */
class Throttler(
    private val delayMillis: Long = 300L,
) {
    private val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
    private var lastExecutionTime: Long = 0
    
    // For suspend functions
    private val mutex = Mutex()
    private val lastAsyncExecutionTime = AtomicLong(0L)

    fun submit(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()

        // Execute the action immediately if enough time has passed since the last execution
        if (currentTime - lastExecutionTime >= delayMillis) {
            action()
            lastExecutionTime = currentTime
        } else {
            // If the time hasn't passed, delay the execution
            val timeRemaining = delayMillis - (currentTime - lastExecutionTime)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                action()
                lastExecutionTime = System.currentTimeMillis()
            }, timeRemaining)
        }
    }
    
    /**
     * Suspend function throttling
     */
    suspend fun <T> throttle(action: suspend () -> T): T {
        return mutex.withLock {
            val currentTime = System.currentTimeMillis()
            val lastTime = lastAsyncExecutionTime.get()
            
            if (currentTime - lastTime >= delayMillis) {
                lastAsyncExecutionTime.set(currentTime)
                action()
            } else {
                val waitTime = delayMillis - (currentTime - lastTime)
                delay(waitTime)
                lastAsyncExecutionTime.set(System.currentTimeMillis())
                action()
            }
        }
    }
}
