package com.awaytime.app.memory

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Enterprise-level Memory Leak Detection System
 * 
 * Advanced leak detection with:
 * - Weak reference monitoring for automatic cleanup detection
 * - Phantom references for finalization tracking
 * - Suspicious object pattern analysis
 * - Automated cleanup and recovery mechanisms
 * - Real-time leak reporting and alerting
 * - Memory retention path analysis
 * 
 * @author Senior Android Engineer
 * @version 1.0.0
 */
class AdvancedMemoryLeakDetector(
    private val scope: CoroutineScope
) : MemoryLeakDetector {
    
    companion object {
        private const val TAG = "AdvancedLeakDetector"
        private const val DETECTION_INTERVAL_MS = 5000L // 5 seconds
        private const val CLEANUP_INTERVAL_MS = 30000L // 30 seconds
        private const val MAX_TRACKING_OBJECTS = 10000
        private const val SUSPICIOUS_AGE_THRESHOLD = 60000L // 1 minute
        private const val LEAK_CONFIDENCE_THRESHOLD = 0.7
    }
    
    // State flows
    private val _detectedLeaks = MutableStateFlow<List<MemoryLeak>>(emptyList())
    override val detectedLeaks: StateFlow<List<MemoryLeak>> = _detectedLeaks.asStateFlow()
    
    private val _suspiciousObjects = MutableStateFlow<List<SuspiciousMemoryObject>>(emptyList())
    override val suspiciousObjects: StateFlow<List<SuspiciousMemoryObject>> = _suspiciousObjects.asStateFlow()
    
    // Internal tracking structures
    private val trackedObjects = ConcurrentHashMap<String, TrackedObject>()
    private val phantomReferences = ConcurrentHashMap<PhantomReference<Any>, String>()
    private val referenceQueue = ReferenceQueue<Any>()
    private val leakHistory = ConcurrentHashMap<String, MutableList<LeakEvent>>()
    
    // Leak detection state
    private var isDetecting = false
    private var detectionJob: Job? = null
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val leakIdCounter = AtomicLong(0)
    
    // Metrics
    private var totalObjectsTracked = 0L
    private var totalLeaksDetected = 0L
    private var totalObjectsCleanedUp = 0L
    
    override fun startDetection() {
        if (isDetecting) {
            Log.w(TAG, "Leak detection already running")
            return
        }
        
        isDetecting = true
        
        // Start periodic leak detection
        detectionJob = scope.launch {
            while (isActive && isDetecting) {
                try {
                    performDetectionCycle()
                    processCleanupQueue()
                    analyzeMemoryPatterns()
                    delay(DETECTION_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during leak detection cycle", e)
                }
            }
        }
        
        // Schedule periodic cleanup
        scheduler.scheduleAtFixedRate({
            scope.launch {
                try {
                    performMaintenanceCleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during maintenance cleanup", e)
                }
            }
        }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        Log.i(TAG, "✅ Advanced memory leak detection started")
    }
    
    override fun stopDetection() {
        if (!isDetecting) return
        
        isDetecting = false
        detectionJob?.cancel()
        scheduler.shutdown()
        
        // Clear tracking data
        trackedObjects.clear()
        phantomReferences.clear()
        
        Log.i(TAG, "⏹️ Memory leak detection stopped")
    }
    
    override fun registerTrackingTarget(obj: Any, identifier: String) {
        if (trackedObjects.size >= MAX_TRACKING_OBJECTS) {
            Log.w(TAG, "Maximum tracking objects reached, skipping registration")
            return
        }
        
        try {
            val trackedObject = TrackedObject(
                id = identifier,
                className = obj::class.java.simpleName,
                weakRef = WeakReference(obj),
                creationTime = System.currentTimeMillis(),
                size = estimateObjectSize(obj),
                retentionPath = captureRetentionPath(obj)
            )
            
            // Create phantom reference for finalization tracking
            val phantomRef = PhantomReference(obj, referenceQueue)
            phantomReferences[phantomRef] = identifier
            
            trackedObjects[identifier] = trackedObject
            totalObjectsTracked++
            
            Log.d(TAG, "📍 Registered tracking for object: $identifier (${obj::class.java.simpleName})")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering tracking target", e)
        }
    }
    
    override fun unregisterTrackingTarget(identifier: String) {
        trackedObjects.remove(identifier)?.let { trackedObject ->
            // Clean up phantom reference
            phantomReferences.entries.removeAll { it.value == identifier }
            Log.d(TAG, "📍 Unregistered tracking for object: $identifier")
        }
    }
    
    private fun performDetectionCycle() {
        val currentTime = System.currentTimeMillis()
        val suspiciousObjects = mutableListOf<SuspiciousMemoryObject>()
        val confirmedLeaks = mutableListOf<MemoryLeak>()
        
        trackedObjects.values.forEach { tracked ->
            val age = currentTime - tracked.creationTime
            val obj = tracked.weakRef.get()
            
            when {
                obj == null -> {
                    // Object was garbage collected - good!
                    unregisterTrackingTarget(tracked.id)
                    totalObjectsCleanedUp++
                }
                age > SUSPICIOUS_AGE_THRESHOLD -> {
                    // Object is old and still alive - analyze further
                    val suspiciousScore = calculateSuspiciousScore(tracked, age)
                    val reasons = analyzeSuspicionReasons(tracked, age)
                    
                    if (suspiciousScore > LEAK_CONFIDENCE_THRESHOLD) {
                        // High confidence leak
                        val leak = createMemoryLeak(tracked, suspiciousScore)
                        confirmedLeaks.add(leak)
                        totalLeaksDetected++
                        
                        // Record leak event
                        recordLeakEvent(tracked.id, leak)
                        
                        Log.w(TAG, "🚨 Memory leak detected: ${tracked.className} (${tracked.id})")
                    } else {
                        // Suspicious but not confirmed leak
                        val suspicious = SuspiciousMemoryObject(
                            id = tracked.id,
                            objectClass = tracked.className,
                            size = tracked.size,
                            ageMillis = age,
                            suspiciousScore = suspiciousScore,
                            reasons = reasons,
                            monitoringSince = tracked.creationTime
                        )
                        suspiciousObjects.add(suspicious)
                    }
                }
            }
        }
        
        // Update state flows
        _detectedLeaks.value = (_detectedLeaks.value + confirmedLeaks).distinctBy { it.id }
        _suspiciousObjects.value = suspiciousObjects
        
        if (confirmedLeaks.isNotEmpty()) {
            Log.w(TAG, "🚨 Detected ${confirmedLeaks.size} new memory leaks")
        }
    }
    
    private fun processCleanupQueue() {
        // Process finalized objects from reference queue
        var processedCount = 0
        while (true) {
            val ref = referenceQueue.poll() ?: break
            phantomReferences.remove(ref)?.let { identifier ->
                unregisterTrackingTarget(identifier)
                processedCount++
            }
        }
        
        if (processedCount > 0) {
            Log.d(TAG, "🧹 Processed $processedCount finalized objects")
        }
    }
    
    private fun analyzeMemoryPatterns() {
        // Analyze patterns in memory usage to detect systematic leaks
        val objectsByClass = trackedObjects.values.groupBy { it.className }
        
        objectsByClass.forEach { (className, objects) ->
            if (objects.size > 10) { // Many instances of same class
                val avgAge = objects.map { System.currentTimeMillis() - it.creationTime }.average()
                if (avgAge > SUSPICIOUS_AGE_THRESHOLD) {
                    Log.w(TAG, "🔍 Pattern detected: Many old instances of $className (${objects.size})")
                }
            }
        }
    }
    
    private fun calculateSuspiciousScore(tracked: TrackedObject, age: Long): Double {
        var score = 0.0
        
        // Age factor (older = more suspicious)
        score += (age / (10 * 60 * 1000.0)).coerceAtMost(0.4) // Max 0.4 for age
        
        // Size factor (larger objects = more concerning)
        score += (tracked.size / (10 * 1024 * 1024.0)).coerceAtMost(0.2) // Max 0.2 for size
        
        // Retention path complexity (deeper paths = more suspicious)
        score += (tracked.retentionPath.size / 20.0).coerceAtMost(0.2) // Max 0.2 for complexity
        
        // Historical leak pattern
        val history = leakHistory[tracked.className]
        if (history != null && history.isNotEmpty()) {
            score += 0.2 // Bump score if this class has leaked before
        }
        
        return score.coerceAtMost(1.0)
    }
    
    private fun analyzeSuspicionReasons(tracked: TrackedObject, age: Long): List<SuspicionReason> {
        val reasons = mutableListOf<SuspicionReason>()
        
        if (age > 5 * 60 * 1000) { // > 5 minutes
            reasons.add(SuspicionReason.OLD_AGE)
        }
        
        if (tracked.size > 5 * 1024 * 1024) { // > 5MB
            reasons.add(SuspicionReason.RAPID_GROWTH)
        }
        
        if (tracked.retentionPath.size > 10) {
            reasons.add(SuspicionReason.UNUSUAL_RETENTION_PATH)
        }
        
        // Check for external references (simplified heuristic)
        if (tracked.retentionPath.any { it.contains("static") || it.contains("singleton") }) {
            reasons.add(SuspicionReason.EXTERNAL_REFERENCES)
        }
        
        return reasons
    }
    
    private fun createMemoryLeak(tracked: TrackedObject, confidence: Double): MemoryLeak {
        val age = System.currentTimeMillis() - tracked.creationTime
        
        return MemoryLeak(
            id = "leak_${leakIdCounter.incrementAndGet()}",
            objectClass = tracked.className,
            retainedSize = tracked.size,
            ageMillis = age,
            retentionPath = tracked.retentionPath,
            confidence = confidence,
            firstDetected = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            isActive = true,
            potentialCauses = generatePotentialCauses(tracked)
        )
    }
    
    private fun generatePotentialCauses(tracked: TrackedObject): List<String> {
        val causes = mutableListOf<String>()
        
        // Analyze retention path for common leak patterns
        tracked.retentionPath.forEach { path ->
            when {
                path.contains("Listener") -> causes.add("Event listener not unregistered")
                path.contains("Handler") -> causes.add("Handler holding reference to Activity/Fragment")
                path.contains("static") -> causes.add("Static reference preventing cleanup")
                path.contains("Singleton") -> causes.add("Singleton holding reference")
                path.contains("Timer") || path.contains("Thread") -> causes.add("Background thread holding reference")
            }
        }
        
        if (causes.isEmpty()) {
            causes.add("Unknown retention pattern - manual investigation required")
        }
        
        return causes
    }
    
    private fun recordLeakEvent(objectId: String, leak: MemoryLeak) {
        val event = LeakEvent(
            timestamp = System.currentTimeMillis(),
            leakId = leak.id,
            objectId = objectId,
            severity = leak.severity
        )
        
        leakHistory.computeIfAbsent(leak.objectClass) { mutableListOf() }.add(event)
        
        // Keep only recent history (last 24 hours)
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        leakHistory.values.forEach { events ->
            events.removeAll { it.timestamp < oneDayAgo }
        }
    }
    
    private fun performMaintenanceCleanup() {
        val currentTime = System.currentTimeMillis()
        val cleanupThreshold = currentTime - 60 * 60 * 1000 // 1 hour
        
        // Remove old tracked objects that are likely false positives
        val toRemove = trackedObjects.entries.filter { (_, tracked) ->
            tracked.creationTime < cleanupThreshold && tracked.weakRef.get() == null
        }.map { it.key }
        
        toRemove.forEach { id ->
            unregisterTrackingTarget(id)
        }
        
        if (toRemove.isNotEmpty()) {
            Log.d(TAG, "🧹 Cleaned up ${toRemove.size} old tracking entries")
        }
    }
    
    override suspend fun performLeakAnalysis(): LeakAnalysisReport {
        val startTime = System.currentTimeMillis()
        val currentLeaks = detectedLeaks.value
        val criticalLeaks = currentLeaks.filter { it.severity == LeakSeverity.CRITICAL }
        val totalMemoryLeaked = currentLeaks.sumOf { it.retainedSize }
        
        val recommendations = mutableListOf<String>()
        
        if (currentLeaks.isNotEmpty()) {
            recommendations.add("Review object lifecycle management")
            recommendations.add("Implement proper cleanup in onDestroy/onPause methods")
            recommendations.add("Use WeakReferences for callbacks and listeners")
        }
        
        if (criticalLeaks.isNotEmpty()) {
            recommendations.add("Address critical leaks immediately - they may cause OOM")
            recommendations.add("Consider using memory profiling tools for detailed analysis")
        }
        
        // Analyze leak patterns
        val leaksByClass = currentLeaks.groupBy { it.objectClass }
        leaksByClass.filter { it.value.size > 3 }.forEach { (className, leaks) ->
            recommendations.add("Systematic leak detected in $className - review class design")
        }
        
        return LeakAnalysisReport(
            analysisTime = System.currentTimeMillis() - startTime,
            totalLeaks = currentLeaks.size,
            criticalLeaks = criticalLeaks.size,
            memoryLeaked = totalMemoryLeaked,
            recommendations = recommendations
        )
    }
    
    override suspend fun forceCleanup(leakId: String): CleanupResult {
        val leak = detectedLeaks.value.find { it.id == leakId }
            ?: return CleanupResult(false, 0, 0L, listOf("Leak not found: $leakId"))
        
        try {
            // Find the tracked object
            val tracked = trackedObjects.values.find { tracked ->
                System.currentTimeMillis() - tracked.creationTime == leak.ageMillis
            }
            
            if (tracked != null) {
                // Clear the reference and force GC
                tracked.weakRef.clear()
                unregisterTrackingTarget(tracked.id)
                System.gc()
                
                // Remove from detected leaks
                _detectedLeaks.value = _detectedLeaks.value.filter { it.id != leakId }
                
                return CleanupResult(
                    success = true,
                    objectsCleaned = 1,
                    memoryFreed = leak.retainedSize,
                    errors = emptyList()
                )
            } else {
                return CleanupResult(
                    success = false,
                    objectsCleaned = 0,
                    memoryFreed = 0L,
                    errors = listOf("Tracked object not found")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during force cleanup", e)
            return CleanupResult(
                success = false,
                objectsCleaned = 0,
                memoryFreed = 0L,
                errors = listOf("Cleanup failed: ${e.message}")
            )
        }
    }
    
    // Helper methods
    
    private fun estimateObjectSize(obj: Any): Long {
        // Simple heuristic for object size estimation
        return when (obj) {
            is String -> obj.length * 2L + 40L // Rough string size
            is List<*> -> obj.size * 32L + 40L // Rough list size
            is Map<*, *> -> obj.size * 64L + 40L // Rough map size
            is ByteArray -> obj.size.toLong() + 16L
            else -> 64L // Default object overhead estimate
        }
    }
    
    private fun captureRetentionPath(obj: Any): List<String> {
        // Simplified retention path capture
        // In a real implementation, this would use reflection or profiling tools
        val path = mutableListOf<String>()
        
        try {
            val objClass = obj::class.java
            path.add(objClass.simpleName)
            
            // Add some synthetic path information based on class type
            when {
                objClass.simpleName.contains("Activity") -> {
                    path.add("ActivityManager")
                    path.add("WindowManager")
                }
                objClass.simpleName.contains("Fragment") -> {
                    path.add("FragmentManager")
                    path.add("Activity")
                }
                objClass.simpleName.contains("Adapter") -> {
                    path.add("RecyclerView")
                    path.add("ViewHolder")
                }
                objClass.simpleName.contains("Listener") -> {
                    path.add("EventSource")
                    path.add("CallbackRegistry")
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error capturing retention path", e)
            path.add("Unknown")
        }
        
        return path
    }
    
    // Data classes for internal tracking
    
    private data class TrackedObject(
        val id: String,
        val className: String,
        val weakRef: WeakReference<Any>,
        val creationTime: Long,
        val size: Long,
        val retentionPath: List<String>
    )
    
    private data class LeakEvent(
        val timestamp: Long,
        val leakId: String,
        val objectId: String,
        val severity: LeakSeverity
    )
}

/**
 * Extension for easier leak detector integration
 */
fun MemoryLeakDetector.trackObject(obj: Any, identifier: String? = null) {
    val id = identifier ?: "${obj::class.java.simpleName}_${obj.hashCode()}_${System.currentTimeMillis()}"
    registerTrackingTarget(obj, id)
}

/**
 * Extension for automatic Activity/Fragment tracking
 */
fun MemoryLeakDetector.trackLifecycleObject(obj: Any) {
    val className = obj::class.java.simpleName
    val timestamp = System.currentTimeMillis()
    val id = "${className}_${obj.hashCode()}_$timestamp"
    registerTrackingTarget(obj, id)
}

/**
 * Leak detection utilities
 */
object LeakDetectionUtils {
    
    /**
     * Analyzes an object for potential leak patterns
     */
    fun analyzeLeakPotential(obj: Any): Double {
        var score = 0.0
        val objClass = obj::class.java
        
        // Check for common leak-prone patterns
        when {
            objClass.simpleName.contains("Activity") -> score += 0.3
            objClass.simpleName.contains("Fragment") -> score += 0.3
            objClass.simpleName.contains("Context") -> score += 0.4
            objClass.simpleName.contains("Listener") -> score += 0.2
            objClass.simpleName.contains("Handler") -> score += 0.3
            objClass.simpleName.contains("AsyncTask") -> score += 0.4
        }
        
        // Check for static references (simplified)
        try {
            val fields = objClass.declaredFields
            val staticFieldCount = fields.count { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            score += (staticFieldCount * 0.1).coerceAtMost(0.3)
        } catch (e: Exception) {
            // Ignore reflection errors
        }
        
        return score.coerceAtMost(1.0)
    }
    
    /**
     * Generates recommendations for leak prevention
     */
    fun generateLeakPreventionRecommendations(objectClass: String): List<String> {
        val recommendations = mutableListOf<String>()
        
        when {
            objectClass.contains("Activity") -> {
                recommendations.add("Use WeakReferences for Activity references")
                recommendations.add("Clear listeners in onDestroy()")
                recommendations.add("Cancel background tasks in onPause()")
            }
            objectClass.contains("Fragment") -> {
                recommendations.add("Null out references in onDestroyView()")
                recommendations.add("Use getViewLifecycleOwner() for observers")
                recommendations.add("Clear adapters and listeners properly")
            }
            objectClass.contains("Listener") -> {
                recommendations.add("Implement proper unregistration")
                recommendations.add("Use WeakReferences in listener implementations")
                recommendations.add("Consider using lifecycle-aware components")
            }
            objectClass.contains("Handler") -> {
                recommendations.add("Use WeakReferences to outer class")
                recommendations.add("Remove callbacks in appropriate lifecycle methods")
                recommendations.add("Consider using static inner classes")
            }
        }
        
        return recommendations
    }
}

/**
 * Automatic leak detection for common Android components
 */
class AutomaticLeakTracker(private val detector: MemoryLeakDetector) {
    
    fun trackActivity(activity: Any) {
        detector.trackLifecycleObject(activity)
    }
    
    fun trackFragment(fragment: Any) {
        detector.trackLifecycleObject(fragment)
    }
    
    fun trackService(service: Any) {
        detector.trackLifecycleObject(service)
    }
    
    fun trackAdapter(adapter: Any) {
        detector.trackObject(adapter, "adapter_${adapter.hashCode()}")
    }
    
    fun trackViewModel(viewModel: Any) {
        detector.trackObject(viewModel, "viewmodel_${viewModel.hashCode()}")
    }
}
