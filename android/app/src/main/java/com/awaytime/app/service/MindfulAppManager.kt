/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 * PERFORMANCE OPTIMIZED VERSION - Fixes main thread blocking and memory issues
 */
package com.awaytime.app.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.awaytime.app.models.AppCategory
import com.awaytime.app.models.AppFilterCriteria
import com.awaytime.app.models.AppLoadingState
import com.awaytime.app.models.AppResult
import com.awaytime.app.models.AppSortBy
import com.awaytime.app.models.InstalledApp
import com.awaytime.app.utils.PerformanceMonitor
import com.awaytime.app.utils.Throttler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * HIGH-PERFORMANCE app manager with lazy loading, memory management, and main thread optimization.
 * 
 * KEY OPTIMIZATIONS:
 * 1. Lazy loading with pagination (20 apps per page)
 * 2. Background thread processing with proper yielding
 * 3. Memory-aware caching with LRU eviction
 * 4. Concurrent image loading with semaphore throttling
 * 5. Progress reporting and cancellation support
 * 6. Debounced loading to prevent excessive calls
 */
class MindfulAppManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "MindfulAppManager"
        private const val ICON_SIZE = 128 // dp
        private const val PAGE_SIZE = 20 // Load 20 apps per page
        private const val MAX_CONCURRENT_ICON_LOADS = 3 // Limit concurrent icon loading
        private const val CACHE_SIZE_MB = 32 // 32MB cache for icons
        private const val DEBOUNCE_DELAY_MS = 300L // Debounce loading requests
        
        @Volatile
        private var INSTANCE: MindfulAppManager? = null
        
        fun getInstance(context: Context): MindfulAppManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MindfulAppManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val packageManager = context.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Performance monitoring
    private val performanceMonitor = PerformanceMonitor
    private val loadingThrottler = Throttler()
    
    // Memory management
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxOf(maxMemory / 8, CACHE_SIZE_MB * 1024) // Use 1/8th of max memory or 32MB
    private val iconCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            // Estimate size: width * height * 4 bytes per pixel (ARGB)
            return bitmap.width * bitmap.height * 4
        }
    }
    
    // Concurrency control
    private val iconLoadingSemaphore = Semaphore(MAX_CONCURRENT_ICON_LOADS)
    
    // State management
    private val _loadingState = MutableStateFlow<AppLoadingState>(AppLoadingState.Idle)
    val loadingState: StateFlow<AppLoadingState> = _loadingState.asStateFlow()
    
    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()
    
    // Paged app cache
    private val appPagesCache = ConcurrentHashMap<Int, List<InstalledApp>>()
    private val appMetadataCache = ConcurrentHashMap<String, InstalledApp>()
    private var totalAppCount = AtomicInteger(0)
    private var lastLoadTime = AtomicLong(0L)
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes
    
    // Job management
    private var currentLoadingJob: Job? = null
    
    init {
        performanceMonitor.startMonitoring()
        Log.d(TAG, "MindfulAppManager initialized with optimized performance settings")
        Log.d(TAG, "Icon cache size: ${cacheSize}KB, Page size: $PAGE_SIZE")
    }
    
    /**
     * OPTIMIZED: Loads apps with pagination and lazy loading
     */
    suspend fun loadAppsPage(
        page: Int = 0, 
        forceRefresh: Boolean = false,
        searchQuery: String = ""
    ): AppResult<List<InstalledApp>> {
        return loadingThrottler.throttle {
            withContext(Dispatchers.IO) {
                try {
                    performanceMonitor.startQueryTimer()
                    
                    // Check if we need to refresh the base app list
                    val currentTime = System.currentTimeMillis()
                    val needsRefresh = forceRefresh || 
                        appMetadataCache.isEmpty() || 
                        (currentTime - lastLoadTime.get()) > cacheValidityMs
                    
                    if (needsRefresh) {
                        loadBaseAppList()
                    }
                    
                    // Get filtered and paged results
                    val pagedApps = getFilteredPagedApps(page, searchQuery)
                    
                    // Load icons for visible apps only (lazy loading)
                    loadIconsForAppsAsync(pagedApps)
                    
                    performanceMonitor.endQueryTimer()
                    performanceMonitor.trackCacheHit()
                    
                    Log.d(TAG, "Loaded page $page: ${pagedApps.size} apps (search: '$searchQuery')")
                    
                    _loadingState.value = AppLoadingState.Success(pagedApps)
                    AppResult.Success(pagedApps)
                    
                } catch (e: CancellationException) {
                    Log.d(TAG, "Loading cancelled")
                    AppResult.Error(e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading apps page", e)
                    performanceMonitor.trackCacheMiss()
                    val errorMessage = "Failed to load apps: ${e.message}"
                    _loadingState.value = AppLoadingState.Error(errorMessage, e)
                    AppResult.Error(e)
                }
            }
        }
    }
    
    /**
     * OPTIMIZED: Loads all apps with proper background processing and progress reporting
     */
    suspend fun loadInstalledApps(forceRefresh: Boolean = false): AppResult<List<InstalledApp>> {
        // Cancel any existing loading job
        currentLoadingJob?.cancel()
        
        return withContext(Dispatchers.IO) {
            try {
                currentLoadingJob = coroutineContext[Job]
                
                _loadingState.value = AppLoadingState.Loading
                _loadingProgress.value = 0f
                
                // Check cache validity
                val currentTime = System.currentTimeMillis()
                if (!forceRefresh && 
                    appMetadataCache.isNotEmpty() && 
                    (currentTime - lastLoadTime.get()) < cacheValidityMs) {
                    
                    val cachedApps = appMetadataCache.values.toList()
                    _loadingState.value = AppLoadingState.Success(cachedApps)
                    _loadingProgress.value = 1f
                    return@withContext AppResult.Success(cachedApps)
                }
                
                Log.d(TAG, "Loading all installed applications with optimization...")
                
                // Load base app list first
                loadBaseAppList()
                
                // Load all pages
                val allApps = mutableListOf<InstalledApp>()
                val totalPages = (totalAppCount.get() + PAGE_SIZE - 1) / PAGE_SIZE
                
                for (page in 0 until totalPages) {
                    if (!isActive) throw CancellationException("Loading cancelled")
                    
                    val pageApps = getFilteredPagedApps(page, "")
                    allApps.addAll(pageApps)
                    
                    _loadingProgress.value = (page + 1f) / totalPages
                    
                    // Yield to prevent blocking
                    yield()
                }
                
                // Load icons for visible apps asynchronously
                loadIconsForAppsAsync(allApps.take(PAGE_SIZE * 2)) // Load first 2 pages worth of icons
                
                _loadingProgress.value = 1f
                _loadingState.value = AppLoadingState.Success(allApps)
                
                Log.d(TAG, "Successfully loaded ${allApps.size} applications with optimization")
                AppResult.Success(allApps)
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Loading cancelled")
                AppResult.Error(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading apps", e)
                val errorMessage = "Failed to load applications: ${e.message}"
                _loadingState.value = AppLoadingState.Error(errorMessage, e)
                _loadingProgress.value = 0f
                AppResult.Error(e)
            }
        }
    }
    
    /**
     * OPTIMIZED: Loads base app metadata without heavy operations
     */
    private suspend fun loadBaseAppList() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading base app list...")
                
                // Get installed packages efficiently
                val packages = packageManager.getInstalledApplications(
                    PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
                )
                
                totalAppCount.set(packages.size)
                Log.d(TAG, "Found ${packages.size} installed packages")
                
                var processedCount = 0
                
                // Process apps in chunks to prevent blocking
                packages.chunked(50).forEach { chunk ->
                    if (!isActive) return@forEach
                    
                    chunk.forEach { applicationInfo ->
                        try {
                            if (!applicationInfo.enabled && !shouldIncludeDisabledApp(applicationInfo)) {
                                return@forEach
                            }
                            
                            // Create lightweight app metadata (no icon loading)
                            val app = createLightweightInstalledApp(applicationInfo)
                            appMetadataCache[app.packageName] = app
                            
                            processedCount++
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to process app: ${applicationInfo.packageName}", e)
                        }
                    }
                    
                    // Yield between chunks to prevent blocking
                    yield()
                }
                
                lastLoadTime.set(System.currentTimeMillis())
                Log.d(TAG, "Base app list loaded: $processedCount apps cached")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load base app list", e)
                throw e
            }
        }
    }
    
    /**
     * OPTIMIZED: Gets filtered and paged apps without heavy operations
     */
    private suspend fun getFilteredPagedApps(page: Int, searchQuery: String): List<InstalledApp> {
        return withContext(Dispatchers.Default) {
            val allApps = appMetadataCache.values.toList()
            
            // Apply search filter efficiently
            val filteredApps = if (searchQuery.isBlank()) {
                allApps
            } else {
                allApps.filter { app ->
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Sort efficiently
            val sortedApps = filteredApps.sortedBy { it.appName.lowercase() }
            
            // Apply pagination
            val startIndex = page * PAGE_SIZE
            val endIndex = minOf(startIndex + PAGE_SIZE, sortedApps.size)
            
            if (startIndex < sortedApps.size) {
                sortedApps.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * OPTIMIZED: Loads icons asynchronously with concurrency control
     */
    private fun loadIconsForAppsAsync(apps: List<InstalledApp>) {
        scope.launch {
            try {
                // Load icons concurrently but controlled
                val deferredIcons = apps.map { app ->
                    async {
                        loadAppIcon(app.packageName)
                    }
                }
                
                // Await all icon loads
                deferredIcons.awaitAll()
                
                Log.d(TAG, "Loaded icons for ${apps.size} apps")
                
            } catch (e: Exception) {
                Log.w(TAG, "Error loading icons asynchronously", e)
            }
        }
    }
    
    /**
     * OPTIMIZED: Fast app icon loading with memory management
     */
    suspend fun loadAppIcon(packageName: String): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                iconCache.get(packageName)?.let { return@withContext it }
                
                // Acquire semaphore to limit concurrent loads
                iconLoadingSemaphore.acquire()
                
                try {
                    // Double-check cache after acquiring semaphore
                    iconCache.get(packageName)?.let { return@withContext it }
                    
                    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                    val drawable = packageManager.getApplicationIcon(applicationInfo)
                    
                    val bitmap = drawableToBitmapOptimized(drawable)
                    val imageBitmap = bitmap.asImageBitmap()
                    
                    // Cache with memory management
                    iconCache.put(packageName, imageBitmap)
                    
                    Log.v(TAG, "Icon loaded and cached for: $packageName")
                    imageBitmap
                    
                } finally {
                    iconLoadingSemaphore.release()
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load icon for $packageName", e)
                null
            }
        }
    }
    
    /**
     * OPTIMIZED: Creates lightweight app without heavy operations
     */
    private fun createLightweightInstalledApp(applicationInfo: ApplicationInfo): InstalledApp {
        val packageInfo = try {
            packageManager.getPackageInfo(applicationInfo.packageName, 0)
        } catch (e: Exception) {
            null
        }
        
        val appName = try {
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            applicationInfo.packageName
        }
        
        // Don't load icon here - will be loaded lazily
        return InstalledApp(
            appName = appName,
            packageName = applicationInfo.packageName,
            icon = null, // Will be loaded lazily
            iconBitmap = null, // Will be loaded lazily
            isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            versionName = packageInfo?.versionName ?: "",
            versionCode = packageInfo?.longVersionCode ?: 0L,
            installTime = packageInfo?.firstInstallTime ?: 0L,
            lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
            targetSdk = applicationInfo.targetSdkVersion,
            isEnabled = applicationInfo.enabled,
            dataDir = applicationInfo.dataDir,
            publicSourceDir = applicationInfo.publicSourceDir,
            uid = applicationInfo.uid
        )
    }
    
    /**
     * OPTIMIZED: Bitmap conversion with memory optimization
     */
    private fun drawableToBitmapOptimized(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            // Recycle if bitmap is too large
            val bitmap = drawable.bitmap
            if (bitmap.byteCount > ICON_SIZE * ICON_SIZE * 4) {
                return Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true)
            }
            return bitmap
        }
        
        val bitmap = Bitmap.createBitmap(
            ICON_SIZE,
            ICON_SIZE,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        
        return bitmap
    }
    
    /**
     * Enhanced filtering with better performance
     */
    fun filterApps(
        apps: List<InstalledApp>,
        criteria: AppFilterCriteria
    ): List<InstalledApp> {
        return apps.asSequence() // Use sequence for better performance
            .filter { app ->
                // Search query filter
                if (criteria.searchQuery.isNotBlank() && !app.matchesSearch(criteria.searchQuery)) {
                    return@filter false
                }
                
                // Category filter
                if (criteria.category != null && AppCategory.fromPackageName(app.packageName) != criteria.category) {
                    return@filter false
                }
                
                // System app filter
                if (!criteria.showSystemApps && app.isSystemApp) {
                    return@filter false
                }
                
                // Popular apps filter
                if (criteria.showPopularOnly && !app.isPopularApp()) {
                    return@filter false
                }
                
                true
            }
            .let { filteredSequence ->
                // Apply sorting
                when (criteria.sortBy) {
                    AppSortBy.NAME -> filteredSequence.sortedBy { it.appName.lowercase() }
                    AppSortBy.PACKAGE_NAME -> filteredSequence.sortedBy { it.packageName.lowercase() }
                    AppSortBy.INSTALL_TIME -> filteredSequence.sortedByDescending { it.installTime }
                    AppSortBy.UPDATE_TIME -> filteredSequence.sortedByDescending { it.lastUpdateTime }
                    AppSortBy.SIZE -> filteredSequence.sortedBy { it.appName.lowercase() }
                }
            }
            .toList()
    }
    
    /**
     * Gets total page count for pagination
     */
    fun getTotalPages(searchQuery: String = ""): Int {
        val totalApps = if (searchQuery.isBlank()) {
            appMetadataCache.size
        } else {
            appMetadataCache.values.count { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        return (totalApps + PAGE_SIZE - 1) / PAGE_SIZE
    }
    
    /**
     * Gets popular apps efficiently
     */
    fun getPopularApps(allApps: List<InstalledApp>): List<InstalledApp> {
        return allApps.asSequence()
            .filter { it.isPopularApp() }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }
    
    /**
     * MEMORY MANAGEMENT: Clears caches and releases memory
     */
    fun clearCache() {
        appPagesCache.clear()
        appMetadataCache.clear()
        iconCache.evictAll()
        lastLoadTime.set(0L)
        totalAppCount.set(0)
        _loadingState.value = AppLoadingState.Idle
        _loadingProgress.value = 0f
        
        // Force garbage collection
        System.gc()
        
        Log.d(TAG, "All caches cleared and memory released")
    }
    
    /**
     * Gets memory usage information
     */
    fun getMemoryInfo(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        return mapOf(
            "iconCacheSize" to iconCache.size(),
            "iconCacheMemory" to "${iconCache.size() * ICON_SIZE * ICON_SIZE * 4 / 1024}KB",
            "appMetadataCount" to appMetadataCache.size,
            "totalMemory" to "${runtime.totalMemory() / 1024 / 1024}MB",
            "freeMemory" to "${runtime.freeMemory() / 1024 / 1024}MB",
            "maxMemory" to "${runtime.maxMemory() / 1024 / 1024}MB"
        )
    }
    
    /**
     * Gets cached apps without triggering a reload
     */
    fun getCachedApps(): List<InstalledApp> {
        return appMetadataCache.values.toList()
    }
    
    /**
     * Cancels current loading operation
     */
    fun cancelLoading() {
        currentLoadingJob?.cancel()
        _loadingState.value = AppLoadingState.Idle
        _loadingProgress.value = 0f
        Log.d(TAG, "Loading operation cancelled")
    }
    
    private fun shouldIncludeDisabledApp(applicationInfo: ApplicationInfo): Boolean {
        return applicationInfo.packageName in InstalledApp.POPULAR_APPS_SET
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        currentLoadingJob?.cancel()
        clearCache()
        performanceMonitor.stopMonitoring()
        Log.d(TAG, "MindfulAppManager cleaned up")
    }
}
