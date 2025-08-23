package com.awaytime.app.memory

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * Enterprise Object Pool Management System
 * 
 * High-performance object pooling with:
 * - Thread-safe object acquisition and release
 * - Automatic pool sizing and optimization
 * - Health monitoring and metrics collection
 * - Memory-aware pool management
 * - Configurable validation and reset policies
 * - Pool warmup and preallocation strategies
 * - Lifecycle management and cleanup
 * 
 * @author Senior Android Engineer
 * @version 1.0.0
 */
class EnterpriseObjectPoolManager(
    private val scope: CoroutineScope
) : ObjectPoolManager {
    
    companion object {
        private const val TAG = "EnterprisePoolManager"
        private const val OPTIMIZATION_INTERVAL_MS = 60000L // 1 minute
        private const val HEALTH_CHECK_INTERVAL_MS = 30000L // 30 seconds
        private const val DEFAULT_POOL_SIZE = 10
        private const val MAX_POOLS = 50
    }
    
    // Pool registry
    private val pools = ConcurrentHashMap<String, EnterpriseObjectPool<*>>()
    private val poolConfigurations = ConcurrentHashMap<String, PoolConfiguration<*>>()
    
    // Metrics tracking
    private val _poolMetrics = MutableStateFlow<Map<String, PoolMetrics>>(emptyMap())
    override val poolMetrics: StateFlow<Map<String, PoolMetrics>> = _poolMetrics.asStateFlow()
    
    // Background services
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private var isInitialized = false
    
    init {
        initialize()
    }
    
    private fun initialize() {
        if (isInitialized) return
        
        // Schedule pool optimization
        scheduler.scheduleAtFixedRate({
            scope.launch {
                try {
                    optimizePools()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during pool optimization", e)
                }
            }
        }, OPTIMIZATION_INTERVAL_MS, OPTIMIZATION_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        // Schedule health checks
        scheduler.scheduleAtFixedRate({
            scope.launch {
                try {
                    performHealthChecks()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during pool health checks", e)
                }
            }
        }, HEALTH_CHECK_INTERVAL_MS, HEALTH_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        isInitialized = true
        Log.i(TAG, "✅ Enterprise Object Pool Manager initialized")
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPool(poolId: String, type: KClass<T>): ObjectPool<T> {
        val pool = pools[poolId] as? EnterpriseObjectPool<T>
        if (pool != null) {
            return pool
        }
        
        // Create default pool if not exists
        val defaultConfig = PoolConfiguration(
            poolId = poolId,
            type = type,
            factory = { createDefaultInstance(type) },
            reset = { /* no-op */ },
            validator = { true },
            minSize = 5,
            maxSize = 20,
            prealloc = 5,
            maxIdleTime = 10 * 60 * 1000 // 10 minutes
        )
        
        return createPool(defaultConfig) as ObjectPool<T>
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> createPool(config: PoolConfiguration<T>): ObjectPool<T> {
        if (pools.size >= MAX_POOLS) {
            throw IllegalStateException("Maximum number of pools reached: $MAX_POOLS")
        }
        
        if (pools.containsKey(config.poolId)) {
            Log.w(TAG, "Pool already exists: ${config.poolId}")
            return pools[config.poolId] as ObjectPool<T>
        }
        
        val pool = EnterpriseObjectPool(config, scope)
        pools[config.poolId] = pool
        poolConfigurations[config.poolId] = config
        
        Log.i(TAG, "✅ Created pool: ${config.poolId} (${config.type.simpleName})")
        return pool
    }
    
    override fun removePool(poolId: String) {
        pools.remove(poolId)?.let { pool ->
            pool.clear()
            poolConfigurations.remove(poolId)
            Log.i(TAG, "🗑️ Removed pool: $poolId")
        }
    }
    
    override suspend fun optimizePools() {
        Log.d(TAG, "🔧 Starting pool optimization")
        
        val currentMetrics = mutableMapOf<String, PoolMetrics>()
        
        pools.forEach { (poolId, pool) ->
            try {
                val metrics = pool.metrics
                currentMetrics[poolId] = metrics
                
                // Analyze pool performance and optimize
                optimizeIndividualPool(poolId, pool, metrics)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error optimizing pool: $poolId", e)
            }
        }
        
        _poolMetrics.value = currentMetrics
        Log.d(TAG, "✅ Pool optimization completed")
    }
    
    private fun <T : Any> optimizeIndividualPool(
        poolId: String,
        pool: EnterpriseObjectPool<T>,
        metrics: PoolMetrics
    ) {
        val config = poolConfigurations[poolId] ?: return
        
        // Analyze utilization and adjust pool size
        val utilizationRatio = metrics.utilizationRatio
        
        when {
            utilizationRatio > 0.9 && pool.size < config.maxSize -> {
                // High utilization - grow pool
                val growBy = minOf(5, config.maxSize - pool.size)
                pool.prealloc(growBy)
                Log.d(TAG, "📈 Grew pool $poolId by $growBy objects (utilization: ${String.format("%.2f", utilizationRatio * 100)}%)")
            }
            utilizationRatio < 0.3 && pool.size > config.minSize -> {
                // Low utilization - shrink pool
                val shrinkTo = maxOf(config.minSize, (pool.size * 0.8).toInt())
                pool.trim(shrinkTo)
                Log.d(TAG, "📉 Shrunk pool $poolId to $shrinkTo objects (utilization: ${String.format("%.2f", utilizationRatio * 100)}%)")
            }
        }
        
        // Check for performance issues
        if (metrics.averageWaitTime > 10.0) { // > 10ms average wait
            Log.w(TAG, "⚠️ High wait time in pool $poolId: ${String.format("%.2f", metrics.averageWaitTime)}ms")
        }
    }
    
    override suspend fun warmupPools() {
        Log.d(TAG, "🔥 Starting pool warmup")
        
        pools.forEach { (poolId, pool) ->
            try {
                val config = poolConfigurations[poolId]
                if (config != null) {
                    pool.prealloc(config.prealloc)
                    Log.d(TAG, "🔥 Warmed up pool: $poolId (${config.prealloc} objects)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error warming up pool: $poolId", e)
            }
        }
        
        Log.d(TAG, "✅ Pool warmup completed")
    }
    
    private suspend fun performHealthChecks() {
        pools.forEach { (poolId, pool) ->
            try {
                val health = analyzePoolHealth(pool)
                if (!health.isHealthy) {
                    Log.w(TAG, "⚠️ Pool health issue in $poolId: ${health.issues.joinToString()}")
                    
                    // Apply corrective actions
                    health.suggestedActions.forEach { action ->
                        when (action) {
                            PoolHealthAction.REBUILD_POOL -> {
                                Log.i(TAG, "🔧 Rebuilding unhealthy pool: $poolId")
                                pool.clear()
                                poolConfigurations[poolId]?.let { config ->
                                    pool.prealloc(config.prealloc)
                                }
                            }
                            PoolHealthAction.TRIM_POOL -> {
                                val config = poolConfigurations[poolId]
                                if (config != null) {
                                    pool.trim(config.minSize)
                                }
                            }
                            PoolHealthAction.VALIDATE_OBJECTS -> {
                                // Trigger validation of all objects in pool
                                pool.validateAllObjects()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking health of pool: $poolId", e)
            }
        }
    }
    
    private fun analyzePoolHealth(pool: EnterpriseObjectPool<*>): PoolHealth {
        val metrics = pool.metrics
        val issues = mutableListOf<String>()
        val actions = mutableListOf<PoolHealthAction>()
        
        // Check for excessive object creation
        if (metrics.createCount > metrics.acquireCount * 2) {
            issues.add("Excessive object creation detected")
            actions.add(PoolHealthAction.REBUILD_POOL)
        }
        
        // Check for memory leaks (objects not returned)
        val outstandingObjects = metrics.acquireCount - metrics.releaseCount
        if (outstandingObjects > pool.size * 2) {
            issues.add("Possible memory leak - too many unreturned objects")
            actions.add(PoolHealthAction.VALIDATE_OBJECTS)
        }
        
        // Check for pool overflow
        if (pool.size > pool.maxSize) {
            issues.add("Pool size exceeds maximum")
            actions.add(PoolHealthAction.TRIM_POOL)
        }
        
        return PoolHealth(
            isHealthy = issues.isEmpty(),
            issues = issues,
            suggestedActions = actions
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> createDefaultInstance(type: KClass<T>): T {
        // Simple factory for common types
        return when (type) {
            StringBuilder::class -> StringBuilder() as T
            ArrayList::class -> ArrayList<Any>() as T
            HashMap::class -> HashMap<Any, Any>() as T
            ByteArray::class -> ByteArray(1024) as T
            else -> {
                try {
                    // Try to create via no-arg constructor
                    type.java.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    throw IllegalArgumentException("Cannot create default instance of ${type.simpleName}", e)
                }
            }
        }
    }
    
    fun shutdown() {
        scheduler.shutdown()
        pools.values.forEach { it.clear() }
        pools.clear()
        poolConfigurations.clear()
        Log.i(TAG, "⏹️ Enterprise Object Pool Manager shut down")
    }
}

/**
 * High-performance thread-safe object pool implementation
 */
class EnterpriseObjectPool<T : Any>(
    private val config: PoolConfiguration<T>,
    private val scope: CoroutineScope
) : ObjectPool<T> {
    
    companion object {
        private const val TAG = "EnterpriseObjectPool"
    }
    
    // Thread-safe object storage
    private val availableObjects = ConcurrentLinkedQueue<PooledObject<T>>()
    private val allObjects = ConcurrentHashMap<T, PooledObject<T>>()
    
    // Pool state
    private val currentSize = AtomicInteger(0)
    private val peakSize = AtomicInteger(0)
    
    // Metrics
    private val acquireCount = AtomicLong(0)
    private val releaseCount = AtomicLong(0)
    private val createCount = AtomicLong(0)
    private val destroyCount = AtomicLong(0)
    private val waitTimeSum = AtomicLong(0)
    private val waitCount = AtomicLong(0)
    
    override val size: Int get() = currentSize.get()
    override val availableCount: Int get() = availableObjects.size
    val maxSize: Int get() = config.maxSize
    
    override val metrics: PoolMetrics
        get() = PoolMetrics(
            acquireCount = acquireCount.get(),
            releaseCount = releaseCount.get(),
            createCount = createCount.get(),
            destroyCount = destroyCount.get(),
            currentSize = currentSize.get(),
            peakSize = peakSize.get(),
            averageWaitTime = if (waitCount.get() > 0) waitTimeSum.get().toDouble() / waitCount.get() else 0.0
        )
    
    init {
        // Pre-allocate initial objects
        prealloc(config.prealloc)
    }
    
    override fun acquire(): T {
        val startTime = System.nanoTime()
        acquireCount.incrementAndGet()
        
        try {
            // Try to get from available objects first
            val pooledObject = availableObjects.poll()
            
            val obj = if (pooledObject != null) {
                pooledObject.obj
            } else {
                // No available objects - create new one
                createNewObject()
            }
            
            // Record wait time
            val waitTime = (System.nanoTime() - startTime) / 1_000_000.0 // Convert to milliseconds
            waitTimeSum.addAndGet(waitTime.toLong())
            waitCount.incrementAndGet()
            
            return obj
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring object from pool ${config.poolId}", e)
            throw e
        }
    }
    
    override fun release(obj: T): Boolean {
        try {
            val pooledObject = allObjects[obj] ?: return false
            
            // Validate object before returning to pool
            if (!config.validator(obj)) {
                destroyObject(obj)
                return false
            }
            
            // Reset object state
            try {
                config.reset(obj)
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting object, destroying instead", e)
                destroyObject(obj)
                return false
            }
            
            // Update timestamps and return to pool
            pooledObject.lastUsed = System.currentTimeMillis()
            availableObjects.offer(pooledObject)
            releaseCount.incrementAndGet()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing object to pool ${config.poolId}", e)
            return false
        }
    }
    
    override fun clear() {
        availableObjects.clear()
        allObjects.values.forEach { pooledObject ->
            try {
                destroyObject(pooledObject.obj)
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying object during clear", e)
            }
        }
        allObjects.clear()
        currentSize.set(0)
        Log.d(TAG, "🧹 Cleared pool ${config.poolId}")
    }
    
    override fun prealloc(count: Int) {
        var allocated = 0
        while (allocated < count && currentSize.get() < config.maxSize) {
            try {
                val obj = createNewObject()
                release(obj)
                allocated++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to pre-allocate object", e)
                break
            }
        }
        Log.d(TAG, "🔥 Pre-allocated $allocated objects for pool ${config.poolId}")
    }
    
    override fun trim(targetSize: Int) {
        val currentCount = currentSize.get()
        if (currentCount <= targetSize) return
        
        val trimCount = currentCount - targetSize
        var trimmed = 0
        
        while (trimmed < trimCount && availableObjects.isNotEmpty()) {
            val pooledObject = availableObjects.poll()
            if (pooledObject != null) {
                destroyObject(pooledObject.obj)
                trimmed++
            }
        }
        
        Log.d(TAG, "✂️ Trimmed $trimmed objects from pool ${config.poolId}")
    }
    
    private fun createNewObject(): T {
        if (currentSize.get() >= config.maxSize) {
            throw IllegalStateException("Pool ${config.poolId} has reached maximum size")
        }
        
        val obj = config.factory()
        val pooledObject = PooledObject(obj, System.currentTimeMillis())
        
        allObjects[obj] = pooledObject
        currentSize.incrementAndGet()
        createCount.incrementAndGet()
        
        // Update peak size
        val current = currentSize.get()
        peakSize.updateAndGet { peak -> maxOf(peak, current) }
        
        return obj
    }
    
    private fun destroyObject(obj: T) {
        allObjects.remove(obj)
        currentSize.decrementAndGet()
        destroyCount.incrementAndGet()
    }
    
    fun validateAllObjects() {
        val invalidObjects = mutableListOf<T>()
        
        allObjects.entries.forEach { (obj, _) ->
            if (!config.validator(obj)) {
                invalidObjects.add(obj)
            }
        }
        
        invalidObjects.forEach { obj ->
            destroyObject(obj)
            availableObjects.removeIf { it.obj == obj }
        }
        
        if (invalidObjects.isNotEmpty()) {
            Log.d(TAG, "🧹 Removed ${invalidObjects.size} invalid objects from pool ${config.poolId}")
        }
    }
    
    // Cleanup idle objects
    fun cleanupIdleObjects() {
        val now = System.currentTimeMillis()
        val idleThreshold = now - config.maxIdleTime
        
        val idleObjects = availableObjects.filter { pooledObject ->
            pooledObject.lastUsed < idleThreshold
        }
        
        idleObjects.forEach { pooledObject ->
            availableObjects.remove(pooledObject)
            destroyObject(pooledObject.obj)
        }
        
        if (idleObjects.isNotEmpty()) {
            Log.d(TAG, "🧹 Cleaned up ${idleObjects.size} idle objects from pool ${config.poolId}")
        }
    }
    
    private data class PooledObject<T>(
        val obj: T,
        var lastUsed: Long
    )
}

// Health monitoring data classes
private data class PoolHealth(
    val isHealthy: Boolean,
    val issues: List<String>,
    val suggestedActions: List<PoolHealthAction>
)

private enum class PoolHealthAction {
    REBUILD_POOL,
    TRIM_POOL,
    VALIDATE_OBJECTS
}

/**
 * Convenient pool builders for common object types
 */
object PoolBuilders {
    
    fun stringBuilderPool(
        poolId: String,
        minSize: Int = 5,
        maxSize: Int = 20,
        initialCapacity: Int = 256
    ): PoolConfiguration<StringBuilder> {
        return PoolConfiguration(
            poolId = poolId,
            type = StringBuilder::class,
            factory = { StringBuilder(initialCapacity) },
            reset = { it.clear() },
            validator = { true },
            minSize = minSize,
            maxSize = maxSize,
            prealloc = minSize,
            maxIdleTime = 10 * 60 * 1000
        )
    }
    
    fun byteArrayPool(
        poolId: String,
        arraySize: Int = 1024,
        minSize: Int = 5,
        maxSize: Int = 20
    ): PoolConfiguration<ByteArray> {
        return PoolConfiguration(
            poolId = poolId,
            type = ByteArray::class,
            factory = { ByteArray(arraySize) },
            reset = { it.fill(0) },
            validator = { it.size == arraySize },
            minSize = minSize,
            maxSize = maxSize,
            prealloc = minSize,
            maxIdleTime = 5 * 60 * 1000
        )
    }
    
    fun <T> listPool(
        poolId: String,
        minSize: Int = 5,
        maxSize: Int = 20,
        initialCapacity: Int = 16
    ): PoolConfiguration<MutableList<T>> {
        return PoolConfiguration(
            poolId = poolId,
            type = MutableList::class as KClass<MutableList<T>>,
            factory = { ArrayList<T>(initialCapacity) },
            reset = { it.clear() },
            validator = { true },
            minSize = minSize,
            maxSize = maxSize,
            prealloc = minSize,
            maxIdleTime = 10 * 60 * 1000
        )
    }
    
    fun <K, V> mapPool(
        poolId: String,
        minSize: Int = 5,
        maxSize: Int = 20,
        initialCapacity: Int = 16
    ): PoolConfiguration<MutableMap<K, V>> {
        return PoolConfiguration(
            poolId = poolId,
            type = MutableMap::class as KClass<MutableMap<K, V>>,
            factory = { HashMap<K, V>(initialCapacity) },
            reset = { it.clear() },
            validator = { true },
            minSize = minSize,
            maxSize = maxSize,
            prealloc = minSize,
            maxIdleTime = 10 * 60 * 1000
        )
    }
}

/**
 * Extension functions for easier pool usage
 */
inline fun <T, R> ObjectPool<T>.use(block: (T) -> R): R {
    val obj = acquire()
    try {
        return block(obj)
    } finally {
        release(obj)
    }
}

inline fun <T> ObjectPool<T>.borrow(block: (T) -> Unit) {
    val obj = acquire()
    try {
        block(obj)
    } finally {
        release(obj)
    }
}

/**
 * Scoped pool usage that automatically handles acquisition and release
 */
class PoolScope<T>(private val pool: ObjectPool<T>) {
    private val acquiredObjects = mutableListOf<T>()
    
    fun acquire(): T {
        val obj = pool.acquire()
        acquiredObjects.add(obj)
        return obj
    }
    
    fun release() {
        acquiredObjects.forEach { obj ->
            pool.release(obj)
        }
        acquiredObjects.clear()
    }
}

inline fun <T, R> ObjectPool<T>.scoped(block: PoolScope<T>.() -> R): R {
    val scope = PoolScope(this)
    try {
        return scope.block()
    } finally {
        scope.release()
    }
}
