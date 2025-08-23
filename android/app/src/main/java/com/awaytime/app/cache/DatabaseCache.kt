package com.awaytime.app.cache

import com.awaytime.app.data.entity.UserSettingsEntity
import com.awaytime.app.data.entity.AppGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache manager to reduce database queries and improve performance
 * Implements TTL-based cache invalidation and reactive updates
 */
class DatabaseCache private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseCache? = null
        
        // Cache TTL constants (in milliseconds)
        private const val USER_SETTINGS_TTL = 30_000L // 30 seconds
        private const val APP_GROUPS_TTL = 60_000L    // 1 minute
        private const val STREAK_TTL = 120_000L       // 2 minutes
        private const val USAGE_DATA_TTL = 15_000L    // 15 seconds
        
        fun getInstance(): DatabaseCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseCache().also { INSTANCE = it }
            }
        }
    }
    
    // Internal data class for cache entries
    internal data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMs
    }
    
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val mutex = Mutex()
    
    // Reactive StateFlows for cached data
    private val _userSettings = MutableStateFlow<UserSettingsEntity?>(null)
    val userSettings: StateFlow<UserSettingsEntity?> = _userSettings.asStateFlow()
    
    private val _activeAppGroups = MutableStateFlow<List<AppGroupEntity>>(emptyList())
    val activeAppGroups: StateFlow<List<AppGroupEntity>> = _activeAppGroups.asStateFlow()
    
    private val _streakCount = MutableStateFlow(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()
    
    // User Settings Cache
    suspend fun getCachedUserSettings(): UserSettingsEntity? = mutex.withLock {
        val cached = cache["user_settings"] as? CacheEntry<UserSettingsEntity>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else null
    }
    
    suspend fun cacheUserSettings(settings: UserSettingsEntity): Unit = mutex.withLock {
        cache["user_settings"] = CacheEntry(settings, System.currentTimeMillis(), USER_SETTINGS_TTL)
        _userSettings.value = settings
    }
    
    // App Groups Cache
    suspend fun getCachedActiveAppGroups(): List<AppGroupEntity>? = mutex.withLock {
        val cached = cache["active_app_groups"] as? CacheEntry<List<AppGroupEntity>>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else null
    }
    
    suspend fun cacheActiveAppGroups(appGroups: List<AppGroupEntity>): Unit = mutex.withLock {
        cache["active_app_groups"] = CacheEntry(appGroups, System.currentTimeMillis(), APP_GROUPS_TTL)
        _activeAppGroups.value = appGroups
    }
    
    // Streak Cache
    suspend fun getCachedStreak(): Int? = mutex.withLock {
        val cached = cache["streak_count"] as? CacheEntry<Int>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else null
    }
    
    suspend fun cacheStreak(streak: Int): Unit = mutex.withLock {
        cache["streak_count"] = CacheEntry(streak, System.currentTimeMillis(), STREAK_TTL)
        _streakCount.value = streak
    }
    
    // Usage Data Cache
    suspend fun getCachedTodayUsage(appGroupName: String): Int? = mutex.withLock {
        val key = "usage_$appGroupName"
        val cached = cache[key] as? CacheEntry<Int>
        return if (cached != null && !cached.isExpired) {
            cached.data
        } else null
    }
    
    suspend fun cacheTodayUsage(appGroupName: String, usage: Int): Unit = mutex.withLock {
        val key = "usage_$appGroupName"
        cache[key] = CacheEntry(usage, System.currentTimeMillis(), USAGE_DATA_TTL)
    }
    
    // Cache invalidation methods
    suspend fun invalidateUserSettings(): Unit = mutex.withLock {
        cache.remove("user_settings")
        _userSettings.value = null
    }
    
    suspend fun invalidateAppGroups(): Unit = mutex.withLock {
        cache.remove("active_app_groups")
        _activeAppGroups.value = emptyList()
    }
    
    suspend fun invalidateStreak(): Unit = mutex.withLock {
        cache.remove("streak_count")
        _streakCount.value = 0
    }
    
    suspend fun invalidateUsageData(appGroupName: String): Unit = mutex.withLock {
        cache.remove("usage_$appGroupName")
    }
    
    suspend fun invalidateAll(): Unit = mutex.withLock {
        cache.clear()
        _userSettings.value = null
        _activeAppGroups.value = emptyList()
        _streakCount.value = 0
    }
    
    // Cache statistics for debugging
    fun getCacheStats(): Map<String, Any?> {
        return mapOf(
            "totalEntries" to cache.size,
            "keys" to cache.keys.toList(),
            "userSettingsExpired" to isEntryExpired("user_settings"),
            "appGroupsExpired" to isEntryExpired("active_app_groups"),
            "streakExpired" to isEntryExpired("streak_count")
        )
    }
    
    private fun isEntryExpired(key: String): Boolean? {
        val entry = cache[key] as? CacheEntry<*>
        return entry?.isExpired
    }
    
    // Periodic cleanup of expired entries
    suspend fun cleanupExpiredEntries(): Unit = mutex.withLock {
        val expiredKeys = cache.entries
            .filter { (it.value as CacheEntry<*>).isExpired }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            println("🧹 Cleaned up ${expiredKeys.size} expired cache entries")
        }
    }
}
