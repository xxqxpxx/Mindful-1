package com.awaytime.app.memory

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Enterprise Memory Management Integration Service
 * 
 * Provides seamless integration of the memory management system into the AwayTime app.
 * Handles initialization, configuration, and lifecycle management.
 * 
 * @author Senior Android Engineer
 * @version 1.0.0
 */
class MemoryManagementIntegration private constructor() {
    
    companion object {
        private const val TAG = "MemoryManagementIntegration"
        
        @Volatile
        private var instance: MemoryManagementIntegration? = null
        
        fun getInstance(): MemoryManagementIntegration {
            return instance ?: synchronized(this) {
                instance ?: MemoryManagementIntegration().also { instance = it }
            }
        }
    }
    
    private var memorySystem: MemoryManagementSystem? = null
    private var isInitialized = false
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Initialize the memory management system
     */
    fun initialize(application: Application) {
        if (isInitialized) {
            Log.w(TAG, "Memory management already initialized")
            return
        }
        
        try {
            // Initialize the enterprise memory management system
            memorySystem = EnterpriseMemoryManager.getInstance()
            memorySystem?.initialize(application)
            
            // Configure default pools for common objects
            setupDefaultPools()
            
            // Configure memory thresholds and alerts
            setupMemoryThresholds()
            
            // Start leak detection for critical components
            setupLeakDetection()
            
            isInitialized = true
            Log.i(TAG, "✅ Memory management integration initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize memory management integration", e)
            throw e
        }
    }
    
    /**
     * Get the memory management system instance
     */
    fun getMemorySystem(): MemoryManagementSystem? = memorySystem
    
    /**
     * Get system health metrics
     */
    fun getSystemHealth(): StateFlow<MemorySystemHealth>? = memorySystem?.systemHealth
    
    /**
     * Get detailed memory metrics
     */
    fun getMetrics(): StateFlow<MemoryMetrics>? = memorySystem?.metrics
    
    /**
     * Perform manual memory optimization
     */
    suspend fun optimizeMemory(urgency: OptimizationUrgency = OptimizationUrgency.NORMAL) {
        memorySystem?.optimizeMemory(urgency)
    }
    
    /**
     * Generate a comprehensive health report
     */
    suspend fun generateHealthReport(): MemoryHealthReport? {
        return memorySystem?.generateHealthReport()
    }
    
    /**
     * Setup default object pools for common AwayTime objects
     */
    private fun setupDefaultPools() {
        val poolManager = memorySystem?.objectPoolManager ?: return
        
        try {
            // StringBuilder pool for text processing
            val stringBuilderPool = PoolBuilders.stringBuilderPool(
                poolId = "awaytime_stringbuilder",
                minSize = 10,
                maxSize = 50,
                initialCapacity = 512
            )
            poolManager.createPool(stringBuilderPool)
            
            // ByteArray pool for data processing
            val byteArrayPool = PoolBuilders.byteArrayPool(
                poolId = "awaytime_bytearrays",
                arraySize = 2048,
                minSize = 5,
                maxSize = 20
            )
            poolManager.createPool(byteArrayPool)
            
            // List pool for collections
            val listPool = PoolBuilders.listPool<Any>(
                poolId = "awaytime_lists",
                minSize = 10,
                maxSize = 30,
                initialCapacity = 32
            )
            poolManager.createPool(listPool)
            
            // Map pool for data structures
            val mapPool = PoolBuilders.mapPool<String, Any>(
                poolId = "awaytime_maps",
                minSize = 5,
                maxSize = 15,
                initialCapacity = 16
            )
            poolManager.createPool(mapPool)
            
            Log.d(TAG, "✅ Default object pools configured")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up default pools", e)
        }
    }
    
    /**
     * Configure memory monitoring thresholds
     */
    private fun setupMemoryThresholds() {
        val monitor = memorySystem?.memoryMonitor ?: return
        
        try {
            // High memory usage threshold
            val highMemoryThreshold = MemoryThreshold(
                id = "awaytime_high_memory",
                name = "High Memory Usage",
                type = ThresholdType.MEMORY_USAGE,
                value = 200.0, // 200MB
                hysteresis = 0.1,
                enabled = true,
                actions = listOf(
                    ThresholdAction.OptimizeMemory,
                    ThresholdAction.SendAlert
                )
            )
            monitor.registerThreshold(highMemoryThreshold)
            
            // Heap usage threshold
            val heapThreshold = MemoryThreshold(
                id = "awaytime_heap_usage",
                name = "Heap Usage",
                type = ThresholdType.HEAP_USAGE,
                value = 0.8, // 80% of heap
                hysteresis = 0.1,
                enabled = true,
                actions = listOf(
                    ThresholdAction.TriggerGC,
                    ThresholdAction.ClearCaches
                )
            )
            monitor.registerThreshold(heapThreshold)
            
            // GC pressure threshold
            val gcThreshold = MemoryThreshold(
                id = "awaytime_gc_pressure",
                name = "GC Pressure",
                type = ThresholdType.GC_FREQUENCY,
                value = 10.0, // More than 10 GCs in monitoring window
                hysteresis = 0.1,
                enabled = true,
                actions = listOf(
                    ThresholdAction.OptimizeMemory,
                    ThresholdAction.SendAlert
                )
            )
            monitor.registerThreshold(gcThreshold)
            
            // Memory leak threshold
            val leakThreshold = MemoryThreshold(
                id = "awaytime_leak_count",
                name = "Memory Leak Count",
                type = ThresholdType.LEAK_COUNT,
                value = 3.0, // More than 3 leaks
                hysteresis = 0.1,
                enabled = true,
                actions = listOf(
                    ThresholdAction.SendAlert,
                    ThresholdAction.CustomAction {
                        Log.w(TAG, "🚨 Memory leak threshold exceeded!")
                        // Could trigger automatic leak cleanup here
                    }
                )
            )
            monitor.registerThreshold(leakThreshold)
            
            Log.d(TAG, "✅ Memory thresholds configured")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up memory thresholds", e)
        }
    }
    
    /**
     * Setup leak detection for critical AwayTime components
     */
    private fun setupLeakDetection() {
        val leakDetector = memorySystem?.leakDetector ?: return
        
        try {
            // Create automatic leak tracker
            val autoTracker = AutomaticLeakTracker(leakDetector)
            
            // Store reference to tracker for lifecycle components to use
            AwayTimeMemoryUtils.setAutoLeakTracker(autoTracker)
            
            Log.d(TAG, "✅ Leak detection configured")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up leak detection", e)
        }
    }
    
    /**
     * Shutdown the memory management system
     */
    fun shutdown() {
        try {
            memorySystem?.shutdown()
            memorySystem = null
            isInitialized = false
            Log.i(TAG, "✅ Memory management integration shut down")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during memory management shutdown", e)
        }
    }
}

/**
 * Utility class for easy access to memory management features throughout the app
 */
object AwayTimeMemoryUtils {
    
    private var autoLeakTracker: AutomaticLeakTracker? = null
    
    internal fun setAutoLeakTracker(tracker: AutomaticLeakTracker) {
        autoLeakTracker = tracker
    }
    
    /**
     * Get a StringBuilder from the pool
     */
    fun getStringBuilder(): StringBuilder? {
        return try {
            val memorySystem = MemoryManagementIntegration.getInstance().getMemorySystem()
            val pool = memorySystem?.objectPoolManager?.getPool("awaytime_stringbuilder", StringBuilder::class)
            pool?.acquire()
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to get StringBuilder from pool", e)
            StringBuilder() // Fallback to regular creation
        }
    }
    
    /**
     * Return a StringBuilder to the pool
     */
    fun releaseStringBuilder(sb: StringBuilder) {
        try {
            val memorySystem = MemoryManagementIntegration.getInstance().getMemorySystem()
            val pool = memorySystem?.objectPoolManager?.getPool("awaytime_stringbuilder", StringBuilder::class)
            pool?.release(sb)
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to release StringBuilder to pool", e)
        }
    }
    
    /**
     * Get a List from the pool
     */
    fun <T> getList(): MutableList<T>? {
        return try {
            val memorySystem = MemoryManagementIntegration.getInstance().getMemorySystem()
            @Suppress("UNCHECKED_CAST")
            val pool = memorySystem?.objectPoolManager?.getPool("awaytime_lists", MutableList::class) as? ObjectPool<MutableList<T>>
            pool?.acquire()
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to get List from pool", e)
            mutableListOf() // Fallback to regular creation
        }
    }
    
    /**
     * Return a List to the pool
     */
    fun <T> releaseList(list: MutableList<T>) {
        try {
            val memorySystem = MemoryManagementIntegration.getInstance().getMemorySystem()
            @Suppress("UNCHECKED_CAST")
            val pool = memorySystem?.objectPoolManager?.getPool("awaytime_lists", MutableList::class) as? ObjectPool<MutableList<T>>
            pool?.release(list)
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to release List to pool", e)
        }
    }
    
    /**
     * Track an Activity for memory leaks
     */
    fun trackActivity(activity: Any) {
        autoLeakTracker?.trackActivity(activity)
    }
    
    /**
     * Track a Fragment for memory leaks
     */
    fun trackFragment(fragment: Any) {
        autoLeakTracker?.trackFragment(fragment)
    }
    
    /**
     * Track a Service for memory leaks
     */
    fun trackService(service: Any) {
        autoLeakTracker?.trackService(service)
    }
    
    /**
     * Track a ViewModel for memory leaks
     */
    fun trackViewModel(viewModel: Any) {
        autoLeakTracker?.trackViewModel(viewModel)
    }
    
    /**
     * Track any object for memory leaks
     */
    fun trackObject(obj: Any, identifier: String? = null) {
        try {
            val memorySystem = MemoryManagementIntegration.getInstance().getMemorySystem()
            if (identifier != null) {
                memorySystem?.leakDetector?.registerTrackingTarget(obj, identifier)
            } else {
                memorySystem?.leakDetector?.trackObject(obj)
            }
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to track object", e)
        }
    }
    
    /**
     * Get current memory metrics
     */
    fun getCurrentMetrics(): MemoryMetrics? {
        return try {
            MemoryManagementIntegration.getInstance().getMetrics()?.value
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to get memory metrics", e)
            null
        }
    }
    
    /**
     * Get current system health
     */
    fun getSystemHealth(): MemorySystemHealth? {
        return try {
            MemoryManagementIntegration.getInstance().getSystemHealth()?.value
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to get system health", e)
            null
        }
    }
    
    /**
     * Force memory optimization
     */
    suspend fun optimizeMemory() {
        try {
            MemoryManagementIntegration.getInstance().optimizeMemory(OptimizationUrgency.HIGH)
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to optimize memory", e)
        }
    }
    
    /**
     * Generate comprehensive memory health report
     */
    suspend fun generateMemoryReport(): MemoryHealthReport? {
        return try {
            MemoryManagementIntegration.getInstance().generateHealthReport()
        } catch (e: Exception) {
            Log.w("AwayTimeMemoryUtils", "Failed to generate memory report", e)
            null
        }
    }
    
    /**
     * Convenient extension for using StringBuilder from pool
     */
    inline fun <R> withStringBuilder(block: (StringBuilder) -> R): R {
        val sb = getStringBuilder() ?: StringBuilder() // Use fallback if pool returns null
        return try {
            block(sb)
        } finally {
            releaseStringBuilder(sb)
        }
    }
    
    /**
     * Convenient extension for using List from pool
     */
    inline fun <T, R> withList(block: (MutableList<T>) -> R): R {
        val list = getList<T>()
        return try {
            block(list ?: mutableListOf())
        } finally {
            if (list != null) releaseList(list)
        }
    }
}

/**
 * Application lifecycle callbacks for memory management
 */
class MemoryManagementLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    
    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
        AwayTimeMemoryUtils.trackActivity(activity)
    }
    
    override fun onActivityStarted(activity: android.app.Activity) {
        // No-op
    }
    
    override fun onActivityResumed(activity: android.app.Activity) {
        // Could trigger memory optimization here if needed
    }
    
    override fun onActivityPaused(activity: android.app.Activity) {
        // Could suggest GC here to free up memory
        System.gc()
    }
    
    override fun onActivityStopped(activity: android.app.Activity) {
        // No-op
    }
    
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {
        // No-op
    }
    
    override fun onActivityDestroyed(activity: android.app.Activity) {
        // Activity should be garbage collected soon
        // The leak detector will automatically detect if it doesn't
    }
}

/**
 * Extension functions for easier integration
 */

/**
 * Extension to track any object for leaks
 */
fun Any.trackForLeaks(identifier: String? = null) {
    AwayTimeMemoryUtils.trackObject(this, identifier)
}

/**
 * Extension to track Activities
 */
fun android.app.Activity.trackForLeaks() {
    AwayTimeMemoryUtils.trackActivity(this)
}

/**
 * Extension to track Fragments
 */
fun androidx.fragment.app.Fragment.trackForLeaks() {
    AwayTimeMemoryUtils.trackFragment(this)
}

/**
 * Extension to track Services
 */
fun android.app.Service.trackForLeaks() {
    AwayTimeMemoryUtils.trackService(this)
}
