package com.awaytime.app.utils

import android.util.Log
import com.awaytime.app.cache.DatabaseCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility to track database queries and cache effectiveness
 * Helps identify performance bottlenecks and excessive database usage
 */
object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    
    // Query counters
    private val totalQueries = AtomicInteger(0)
    private val userSettingsQueries = AtomicInteger(0)
    private val streakCalculations = AtomicInteger(0)
    private val usageQueries = AtomicInteger(0)
    
    // Cache hit counters
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    // Performance timers
    private val queryStartTime = AtomicLong(0)
    private val totalQueryTime = AtomicLong(0)
    
    // Monitoring state
    private var isMonitoring = false
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.d(TAG, "📊 Performance monitoring started")
        
        // Start periodic reporting
        CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                delay(60_000) // Report every minute
                reportStatistics()
                
                // Reset counters every 5 minutes
                delay(300_000)
                resetCounters()
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        Log.d(TAG, "📊 Performance monitoring stopped")
    }
    
    // Query tracking methods
    fun trackUserSettingsQuery() {
        totalQueries.incrementAndGet()
        userSettingsQueries.incrementAndGet()
    }
    
    fun trackStreakCalculation() {
        totalQueries.incrementAndGet()
        streakCalculations.incrementAndGet()
    }
    
    fun trackUsageQuery() {
        totalQueries.incrementAndGet()
        usageQueries.incrementAndGet()
    }
    
    fun trackCacheHit() {
        cacheHits.incrementAndGet()
    }
    
    fun trackCacheMiss() {
        cacheMisses.incrementAndGet()
    }
    
    fun startQueryTimer() {
        queryStartTime.set(System.currentTimeMillis())
    }
    
    fun endQueryTimer() {
        val elapsed = System.currentTimeMillis() - queryStartTime.get()
        totalQueryTime.addAndGet(elapsed)
    }
    
    private fun reportStatistics() {
        val totalQueriesCount = totalQueries.get()
        val cacheHitCount = cacheHits.get()
        val cacheMissCount = cacheMisses.get()
        val cacheHitRatio = if (cacheHitCount + cacheMissCount > 0) {
            (cacheHitCount.toFloat() / (cacheHitCount + cacheMissCount)) * 100
        } else 0f
        
        val avgQueryTime = if (totalQueriesCount > 0) {
            totalQueryTime.get() / totalQueriesCount
        } else 0L
        
        Log.i(TAG, """
            📊 Performance Report (Last 60s):
            ├─ Total DB Queries: $totalQueriesCount
            ├─ User Settings: ${userSettingsQueries.get()}
            ├─ Streak Calculations: ${streakCalculations.get()}
            ├─ Usage Queries: ${usageQueries.get()}
            ├─ Cache Hits: $cacheHitCount
            ├─ Cache Misses: $cacheMissCount
            ├─ Cache Hit Ratio: ${"%.1f".format(cacheHitRatio)}%
            └─ Avg Query Time: ${avgQueryTime}ms
        """.trimIndent())
        
        // Log warnings for performance issues
        if (totalQueriesCount > 100) {
            Log.w(TAG, "⚠️ High database query count detected: $totalQueriesCount queries in 60s")
        }
        
        if (cacheHitRatio < 50f && cacheHitCount + cacheMissCount > 10) {
            Log.w(TAG, "⚠️ Low cache hit ratio: ${"%.1f".format(cacheHitRatio)}%")
        }
        
        if (avgQueryTime > 100) {
            Log.w(TAG, "⚠️ Slow average query time: ${avgQueryTime}ms")
        }
        
        // Get cache statistics
        val cache = DatabaseCache.getInstance()
        val cacheStats = cache.getCacheStats()
        Log.d(TAG, "💾 Cache Statistics: $cacheStats")
    }
    
    private fun resetCounters() {
        totalQueries.set(0)
        userSettingsQueries.set(0)
        streakCalculations.set(0)
        usageQueries.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        totalQueryTime.set(0)
        
        Log.d(TAG, "🔄 Performance counters reset")
    }
    
    /**
     * Get current performance metrics for debugging
     */
    fun getCurrentMetrics(): Map<String, Any> {
        val cacheHitCount = cacheHits.get()
        val cacheMissCount = cacheMisses.get()
        val cacheHitRatio = if (cacheHitCount + cacheMissCount > 0) {
            (cacheHitCount.toFloat() / (cacheHitCount + cacheMissCount)) * 100
        } else 0f
        
        return mapOf(
            "totalQueries" to totalQueries.get(),
            "userSettingsQueries" to userSettingsQueries.get(),
            "streakCalculations" to streakCalculations.get(),
            "usageQueries" to usageQueries.get(),
            "cacheHits" to cacheHitCount,
            "cacheMisses" to cacheMissCount,
            "cacheHitRatio" to cacheHitRatio,
            "avgQueryTime" to if (totalQueries.get() > 0) totalQueryTime.get() / totalQueries.get() else 0L,
            "isMonitoring" to isMonitoring
        )
    }
    
    /**
     * Force an immediate performance report
     */
    fun forceReport() {
        if (isMonitoring) {
            reportStatistics()
        }
    }
}
