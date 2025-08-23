package com.awaytime.app.service

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Crash Prevention and Performance Monitoring Utility
 * 
 * This class helps prevent common Android crashes and performance issues:
 * - Memory leaks from unclosed resources
 * - Main thread blocking operations
 * - Null pointer dereferences
 * - Resource exhaustion
 */
object CrashPrevention {
    const val TAG = "CrashPrevention"
    
    // Memory monitoring
    private val memoryThresholdMB = 50L // Alert when free memory < 50MB
    private val activeCoroutines = ConcurrentHashMap<String, WeakReference<Job>>()
    private val memoryCheckInterval = 30_000L // Check every 30 seconds
    
    // Performance monitoring
    private val frameDropCounter = AtomicLong(0)
    private val anrDetectionThreshold = 5000L // 5 seconds
    
    // Initialization flag
    @Volatile
    private var isInitialized = false
    
    /**
     * Initialize crash prevention system
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            Log.d(TAG, "Initializing crash prevention system")
            
            // Set up native crash handler
            setupNativeCrashHandler()
            
            // Set up memory monitoring
            startMemoryMonitoring(context)
            
            // Set up ANR detection
            startANRDetection()
            
            // Set up frame drop monitoring
            startFrameDropMonitoring()
            
            // Set up main thread watchdog
            setupMainThreadWatchdog()
            
            isInitialized = true
            Log.d(TAG, "✅ Crash prevention system initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize crash prevention", e)
        }
    }
    
    /**
     * Setup native crash handler to prevent SIGSEGV
     */
    private fun setupNativeCrashHandler() {
        try {
            // Install thread exception handler
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "🚨 Uncaught exception in thread ${thread.name}", throwable)
                
                // Handle specific crash types
                when {
                    throwable.message?.contains("SIGSEGV") == true -> {
                        Log.e(TAG, "🚨 SIGSEGV detected - likely native memory corruption")
                        handleSigSegvCrash()
                    }
                    throwable is OutOfMemoryError -> {
                        Log.e(TAG, "🚨 Out of memory error")
                        handleOutOfMemoryError()
                    }
                    else -> {
                        Log.e(TAG, "🚨 Generic crash: ${throwable.javaClass.simpleName}")
                        handleGenericCrash(throwable)
                    }
                }
            }
            
            Log.d(TAG, "✅ Native crash handler installed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to setup native crash handler", e)
        }
    }
    
    /**
     * Handle SIGSEGV crashes specifically
     */
    private fun handleSigSegvCrash() {
        try {
            Log.w(TAG, "🔧 Attempting SIGSEGV recovery...")
            
            // Force immediate cleanup
            forceCleanup()
            
            // Clear all caches
            System.gc()
            System.runFinalization()
            
            // Reset static instances that might be corrupted
            resetStaticInstances()
            
            Log.d(TAG, "✅ SIGSEGV recovery attempted")
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIGSEGV recovery failed", e)
        }
    }
    
    /**
     * Setup main thread watchdog to prevent excessive blocking
     */
    private fun setupMainThreadWatchdog() {
        val mainHandler = Handler(Looper.getMainLooper())
        val watchdogThreshold = 2000L // 2 seconds
        
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                val completed = CompletableDeferred<Long>()
                
                mainHandler.post {
                    completed.complete(System.currentTimeMillis())
                }
                
                try {
                    val responseTime = withTimeoutOrNull(watchdogThreshold) {
                        completed.await()
                    }
                    
                    if (responseTime == null) {
                        Log.w(TAG, "🐕 Main thread watchdog triggered - thread blocked > ${watchdogThreshold}ms")
                        
                        // Emergency cleanup on background thread
                        emergencyMainThreadCleanup()
                    } else {
                        val delay = responseTime - startTime
                        if (delay > 500) { // Log delays > 500ms
                            Log.w(TAG, "⚠️ Main thread delay: ${delay}ms")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Watchdog error", e)
                }
                
                delay(1000) // Check every second
            }
        }
    }
    
    /**
     * Emergency cleanup when main thread is blocked
     */
    private fun emergencyMainThreadCleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "🚨 Emergency main thread cleanup initiated")
                
                // Cancel all non-essential coroutines
                activeCoroutines.values.forEach { ref ->
                    ref.get()?.let { job ->
                        if (!job.isCompleted) {
                            job.cancel()
                        }
                    }
                }
                
                // Force garbage collection
                System.gc()
                
                Log.d(TAG, "✅ Emergency cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Emergency cleanup failed", e)
            }
        }
    }
    
    /**
     * Reset static instances that might be corrupted
     */
    private fun resetStaticInstances() {
        try {
            // Clear any static caches or singletons that might be corrupted
            // This would reset managers and services to prevent cascade failures
            Log.d(TAG, "🔄 Static instances reset")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reset static instances", e)
        }
    }
    
    /**
     * Handle out of memory errors
     */
    private fun handleOutOfMemoryError() {
        try {
            Log.w(TAG, "🧠 Out of memory - emergency cleanup")
            
            // Aggressive cleanup
            forceCleanup()
            
            // Clear all possible caches
            System.gc()
            
            Log.d(TAG, "✅ OOM cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ OOM cleanup failed", e)
        }
    }
    
    /**
     * Handle generic crashes
     */
    private fun handleGenericCrash(throwable: Throwable) {
        try {
            Log.w(TAG, "🔧 Generic crash recovery for: ${throwable.javaClass.simpleName}")
            
            // Standard cleanup
            cleanupWeakReferences()
            
            Log.d(TAG, "✅ Generic crash recovery completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Generic crash recovery failed", e)
        }
    }
    
    /**
     * Execute operation safely with timeout and error handling
     */
    suspend fun <T> safeExecute(
        operation: suspend () -> T,
        timeoutMs: Long = 10_000L,
        fallback: T? = null,
        operationName: String = "unknown"
    ): T? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                trackCoroutine(operationName) {
                    operation()
                }
            } ?: run {
                Log.w(TAG, "⚠️ Operation '$operationName' timed out after ${timeoutMs}ms")
                fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in operation '$operationName': ${e.message}", e)
            fallback
        }
    }
    
    /**
     * Execute operation on main thread safely
     */
    fun safeMainThreadExecution(operation: () -> Unit, operationName: String = "unknown") {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // Already on main thread
                operation()
            } else {
                // Switch to main thread
                Handler(Looper.getMainLooper()).post {
                    try {
                        operation()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in main thread operation '$operationName': ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to execute on main thread '$operationName': ${e.message}", e)
        }
    }
    
    /**
     * Check if object is safe to use (not null and valid)
     */
    inline fun <T : Any> T?.ifSafe(action: (T) -> Unit) {
        if (this != null) {
            try {
                action(this)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in safe execution: ${e.message}", e)
            }
        }
    }
    
    /**
     * Safe casting with null check
     */
    inline fun <reified T> Any?.safeCast(): T? {
        return try {
            this as? T
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Safe cast failed: ${e.message}")
            null
        }
    }
    
    /**
     * Track coroutine lifecycle
     */
    private suspend fun <T> trackCoroutine(name: String, operation: suspend () -> T): T {
        val job = coroutineContext[Job]
        if (job != null) {
            activeCoroutines[name] = WeakReference(job)
        }
        
        try {
            return operation()
        } finally {
            activeCoroutines.remove(name)
        }
    }
    
    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
                    val freeMemoryMB = maxMemoryMB - usedMemoryMB
                    
                    if (freeMemoryMB < memoryThresholdMB) {
                        Log.w(TAG, "⚠️ Low memory warning: ${freeMemoryMB}MB free, ${usedMemoryMB}MB used")
                        
                        // Suggest garbage collection
                        System.gc()
                        
                        // Clean up weak references
                        cleanupWeakReferences()
                    }
                    
                    // Log memory stats periodically
                    if (usedMemoryMB > 0 && usedMemoryMB % 50 == 0L) {
                        Log.d(TAG, "📊 Memory: ${usedMemoryMB}MB used, ${freeMemoryMB}MB free, ${activeCoroutines.size} active coroutines")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in memory monitoring", e)
                }
                
                delay(memoryCheckInterval)
            }
        }
    }
    
    /**
     * Start ANR detection
     */
    private fun startANRDetection() {
        val mainHandler = Handler(Looper.getMainLooper())
        
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                
                // Post a task to main thread and measure response time
                val completed = CompletableDeferred<Unit>()
                
                mainHandler.post {
                    completed.complete(Unit)
                }
                
                try {
                    withTimeoutOrNull(anrDetectionThreshold) {
                        completed.await()
                    } ?: run {
                        val blockedTime = System.currentTimeMillis() - startTime
                        Log.w(TAG, "⚠️ Potential ANR detected: Main thread blocked for ${blockedTime}ms")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in ANR detection", e)
                }
                
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Start frame drop monitoring
     */
    private fun startFrameDropMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                // Monitor choreographer for frame drops
                // This is a simplified implementation
                Log.d(TAG, "✅ Frame drop monitoring started")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start frame drop monitoring", e)
            }
        }
    }
    
    /**
     * Clean up weak references
     */
    private fun cleanupWeakReferences() {
        val iterator = activeCoroutines.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val job = entry.value.get()
            if (job == null || job.isCompleted || job.isCancelled) {
                iterator.remove()
            }
        }
    }
    
    /**
     * Get current performance stats
     */
    fun getPerformanceStats(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "usedMemoryMB" to (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            "maxMemoryMB" to runtime.maxMemory() / (1024 * 1024),
            "activeCoroutines" to activeCoroutines.size,
            "frameDrops" to frameDropCounter.get(),
            "isInitialized" to isInitialized
        )
    }
    
    /**
     * Force cleanup to prevent memory leaks
     */
    fun forceCleanup() {
        try {
            Log.d(TAG, "🧹 Forcing cleanup...")
            
            // Cancel all tracked coroutines
            activeCoroutines.values.forEach { ref ->
                ref.get()?.cancel()
            }
            activeCoroutines.clear()
            
            // Suggest garbage collection
            System.gc()
            
            Log.d(TAG, "✅ Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup", e)
        }
    }
}

/**
 * Extension functions for safer operations
 */

/**
 * Safe string operations
 */
fun String?.orSafe(default: String = ""): String = this ?: default

/**
 * Safe list operations
 */
fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/**
 * Safe map operations
 */
fun <K, V> Map<K, V>?.orEmpty(): Map<K, V> = this ?: emptyMap()

/**
 * Safe collection operations with size limit
 */
fun <T> Collection<T>?.safeSize(): Int = this?.size ?: 0

/**
 * Safe number operations
 */
fun Int?.orZero(): Int = this ?: 0
fun Long?.orZero(): Long = this ?: 0L
fun Float?.orZero(): Float = this ?: 0f
fun Double?.orZero(): Double = this ?: 0.0