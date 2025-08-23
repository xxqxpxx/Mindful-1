package com.awaytime.app.service

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Service manager for structured concurrency and proper coroutine scope management
 * Manages all app services with proper lifecycle-aware coroutine scopes
 */
class ServiceManager private constructor(private val context: Context) : DefaultLifecycleObserver {
    
    companion object {
        @Volatile
        private var INSTANCE: ServiceManager? = null
        
        fun getInstance(context: Context): ServiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceManager(context.applicationContext).also { 
                    INSTANCE = it
                    ProcessLifecycleOwner.get().lifecycle.addObserver(it)
                }
            }
        }
    }
    
    // Supervised scope for all app services
    private val applicationScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.Default + 
        CoroutineExceptionHandler { _, exception ->
            println("❌ ServiceManager exception: ${exception.message}")
            exception.printStackTrace()
        }
    )
    
    // Service instances managed by this manager
    private val services = ConcurrentHashMap<String, Any>()
    private val serviceJobs = ConcurrentHashMap<String, Job>()
    
    // Service state management
    private val _serviceStates = MutableSharedFlow<ServiceState>(replay = 10)
    val serviceStates: SharedFlow<ServiceState> = _serviceStates.asSharedFlow()
    
    // Background processing scope for long-running operations
    private val backgroundScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.IO + 
        CoroutineExceptionHandler { _, exception ->
            println("❌ Background operation exception: ${exception.message}")
        }
    )
    
    init {
        startPeriodicMaintenance()
    }
    
    /**
     * Get or create a service with proper scope management
     */
    fun <T> getService(serviceFactory: () -> T): T {
        val serviceName = serviceFactory.javaClass.simpleName
        
        @Suppress("UNCHECKED_CAST")
        return services.getOrPut(serviceName) {
            println("🔧 Creating service: $serviceName")
            val service = serviceFactory()
            
            // Emit service creation event
            applicationScope.launch {
                _serviceStates.emit(ServiceState.Created(serviceName))
            }
            
            service
        } as T
    }
    
    /**
     * Start a background service with proper error handling and lifecycle management
     */
    fun startBackgroundService(
        serviceName: String,
        interval: Long = 5.minutes.inWholeMilliseconds,
        operation: suspend CoroutineScope.() -> Unit
    ) {
        // Cancel existing job if running
        serviceJobs[serviceName]?.cancel()
        
        val job = backgroundScope.launch {
            _serviceStates.emit(ServiceState.Started(serviceName))
            
            try {
                while (isActive) {
                    try {
                        operation()
                        _serviceStates.emit(ServiceState.OperationCompleted(serviceName))
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation
                    } catch (e: Exception) {
                        println("❌ Background service $serviceName operation failed: ${e.message}")
                        _serviceStates.emit(ServiceState.Error(serviceName, e.message ?: "Unknown error"))
                    }
                    
                    delay(interval)
                }
            } finally {
                _serviceStates.emit(ServiceState.Stopped(serviceName))
            }
        }
        
        serviceJobs[serviceName] = job
    }
    
    /**
     * Stop a background service
     */
    fun stopBackgroundService(serviceName: String) {
        serviceJobs[serviceName]?.cancel()
        serviceJobs.remove(serviceName)
        
        applicationScope.launch {
            _serviceStates.emit(ServiceState.Stopped(serviceName))
        }
    }
    
    /**
     * Execute a one-time background operation with proper error handling
     */
    fun executeBackground(
        operationName: String,
        timeoutMs: Long = 30_000L,
        operation: suspend CoroutineScope.() -> Unit
    ) {
        backgroundScope.launch {
            try {
                _serviceStates.emit(ServiceState.OperationStarted(operationName))
                
                withTimeout(timeoutMs) {
                    operation()
                }
                
                _serviceStates.emit(ServiceState.OperationCompleted(operationName))
            } catch (e: TimeoutCancellationException) {
                println("⏰ Operation $operationName timed out after ${timeoutMs}ms")
                _serviceStates.emit(ServiceState.Timeout(operationName))
            } catch (e: CancellationException) {
                println("🚫 Operation $operationName cancelled")
                _serviceStates.emit(ServiceState.Cancelled(operationName))
            } catch (e: Exception) {
                println("❌ Operation $operationName failed: ${e.message}")
                _serviceStates.emit(ServiceState.Error(operationName, e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Create a flow that emits values from a background operation
     */
    fun <T> createBackgroundFlow(
        flowName: String,
        producer: suspend FlowCollector<T>.() -> Unit
    ): Flow<T> = flow {
        producer()
    }
        .flowOn(Dispatchers.IO)
        .catch { exception ->
            println("❌ Background flow $flowName error: ${exception.message}")
            _serviceStates.emit(ServiceState.Error(flowName, exception.message ?: "Flow error"))
        }
        .onStart {
            _serviceStates.emit(ServiceState.FlowStarted(flowName))
        }
        .onCompletion {
            _serviceStates.emit(ServiceState.FlowCompleted(flowName))
        }
    
    /**
     * Initialize core services with proper scopes
     */
    fun initializeCoreServices() {
        executeBackground("initialize_core_services") {
            // Usage Tracking Service
            val usageTrackingService = getService { UsageTrackingService(context) }
            
            // Start background usage monitoring
            startBackgroundService(
                serviceName = "usage_monitoring",
                interval = 1.minutes.inWholeMilliseconds
            ) {
                try {
                    usageTrackingService.updateUsageData()
                } catch (e: Exception) {
                    println("❌ Usage monitoring failed: ${e.message}")
                }
            }
            
            // Goal Tracking Service
            val goalTrackingService = getService { GoalTrackingService(context) }
            
            // Data Export Service (on-demand, no background processing needed)
            getService { DataExportService(context) }
            
            // Performance Monitor
            val performanceMonitor = com.awaytime.app.utils.PerformanceMonitor
            performanceMonitor.startMonitoring()
            
            // Database Cache
            val databaseCache = com.awaytime.app.cache.DatabaseCache.getInstance()
            
            // Start cache cleanup
            startBackgroundService(
                serviceName = "cache_cleanup",
                interval = 10.minutes.inWholeMilliseconds
            ) {
                databaseCache.cleanupExpiredEntries()
            }
            
            println("✅ Core services initialized successfully")
        }
    }
    
    /**
     * Start periodic maintenance tasks
     */
    private fun startPeriodicMaintenance() {
        // Database cleanup every hour
        startBackgroundService(
            serviceName = "database_cleanup",
            interval = 60.minutes.inWholeMilliseconds
        ) {
            try {
                val repository = com.awaytime.app.data.repository.AwayTimeRepository(context)
                repository.cleanupOldRecords()
                println("🧹 Database cleanup completed")
            } catch (e: Exception) {
                println("❌ Database cleanup failed: ${e.message}")
            }
        }
        
        // Memory monitoring every 5 minutes
        startBackgroundService(
            serviceName = "memory_monitoring",
            interval = 5.minutes.inWholeMilliseconds
        ) {
            try {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
                
                if (memoryUsagePercent > 80f) {
                    println("⚠️ High memory usage: ${"%.1f".format(memoryUsagePercent)}%")
                    
                    // Trigger garbage collection
                    System.gc()
                    
                    // Clear non-essential caches
                    val cache = com.awaytime.app.cache.DatabaseCache.getInstance()
                    cache.cleanupExpiredEntries()
                }
                
            } catch (e: Exception) {
                println("❌ Memory monitoring failed: ${e.message}")
            }
        }
        
        // Performance reporting every 30 minutes
        startBackgroundService(
            serviceName = "performance_reporting",
            interval = 30.minutes.inWholeMilliseconds
        ) {
            try {
                val performanceMonitor = com.awaytime.app.utils.PerformanceMonitor
                performanceMonitor.forceReport()
            } catch (e: Exception) {
                println("❌ Performance reporting failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get service status for debugging
     */
    fun getServiceStatus(): Map<String, ServiceStatus> {
        return services.keys.associateWith { serviceName ->
            val hasJob = serviceJobs.containsKey(serviceName)
            val jobActive = serviceJobs[serviceName]?.isActive ?: false
            
            ServiceStatus(
                serviceName = serviceName,
                isActive = hasJob && jobActive,
                hasBackgroundJob = hasJob,
                vpnServiceRunning = false,
                notificationServiceRunning = false,
                focusSessionActive = false,
                accessibilityServiceRunning = false
            )
        }
    }
    
    /**
     * Shutdown all services gracefully
     */
    fun shutdown() {
        println("🔄 Shutting down ServiceManager...")
        
        // Cancel all background jobs
        serviceJobs.values.forEach { it.cancel() }
        serviceJobs.clear()
        
        // Cancel main scopes
        applicationScope.cancel()
        backgroundScope.cancel()
        
        // Clear services
        services.clear()
        
        println("✅ ServiceManager shutdown complete")
    }
    
    // Lifecycle observer methods
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        println("🟢 App foregrounded - resuming active services")
        
        // Resume any paused services
        applicationScope.launch {
            _serviceStates.emit(ServiceState.AppForegrounded)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        println("🔴 App backgrounded - optimizing services")
        
        // Reduce frequency of non-critical background tasks
        applicationScope.launch {
            _serviceStates.emit(ServiceState.AppBackgrounded)
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        shutdown()
    }
}

// Service state events
sealed class ServiceState(val serviceName: String) {
    data class Created(val name: String) : ServiceState(name)
    data class Started(val name: String) : ServiceState(name)
    data class Stopped(val name: String) : ServiceState(name)
    data class OperationStarted(val name: String) : ServiceState(name)
    data class OperationCompleted(val name: String) : ServiceState(name)
    data class FlowStarted(val name: String) : ServiceState(name)
    data class FlowCompleted(val name: String) : ServiceState(name)
    data class Error(val name: String, val message: String) : ServiceState(name)
    data class Timeout(val name: String) : ServiceState(name)
    data class Cancelled(val name: String) : ServiceState(name)
    object AppForegrounded : ServiceState("app")
    object AppBackgrounded : ServiceState("app")
}

// Service status data class
data class ServiceStatus(
    val serviceName: String,
    val isActive: Boolean,
    val hasBackgroundJob: Boolean,
    val vpnServiceRunning: Boolean,
    val notificationServiceRunning: Boolean,
    val focusSessionActive: Boolean,
    val accessibilityServiceRunning: Boolean
)
