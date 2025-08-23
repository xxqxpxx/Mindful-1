package com.awaytime.app.data.repository

import android.content.Context
import com.awaytime.app.cache.DatabaseCache
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.entity.UsageRecordEntity
import com.awaytime.app.data.entity.UserSettingsEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Date

class AwayTimeRepository(context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private val database = AwayTimeDatabase.getDatabase(context)
    private val usageRecordDao = database.usageRecordDao()
    private val appGroupDao = database.appGroupDao()
    private val userSettingsDao = database.userSettingsDao()
    
    // Cache manager for minimizing database access
    private val cache = DatabaseCache.getInstance()

    // MARK: - Usage Records

    fun getAllUsageRecords(): Flow<List<UsageRecordEntity>> = usageRecordDao.getAllUsageRecords()

    suspend fun saveUsageRecord(
        date: Date,
        usageMinutes: Int,
        appGroupName: String,
        limitExceeded: Boolean,
        pickupCount: Int = 0
    ) = withContext(ioDispatcher) {
        try {
            val record = UsageRecordEntity(
                date = date,
                usageMinutes = usageMinutes,
                appGroupName = appGroupName,
                limitExceeded = limitExceeded,
                pickupCount = pickupCount
            )
            
            withTimeoutOrNull(3000) {
                usageRecordDao.insertUsageRecord(record)
            } ?: run {
                println("⚠️ Timeout saving usage record for $appGroupName")
            }
            
            // Invalidate related caches
            cache.invalidateUsageData(appGroupName)
            if (!limitExceeded) {
                // Streak may have changed if goal was met
                cache.invalidateStreak()
            }
        } catch (e: Exception) {
            println("❌ Error saving usage record for $appGroupName: ${e.message}")
        }
    }

    suspend fun getTodayUsage(appGroupName: String): Int = withContext(ioDispatcher) {
        try {
            // Try to get from cache first
            cache.getCachedTodayUsage(appGroupName)?.let {
                return@withContext it
            }
            
            val calendar = Calendar.getInstance()
            val startOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = calendar.apply {
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis

            val usage = withTimeoutOrNull(5000) {
                usageRecordDao.getTotalUsageForDay(startOfDay, endOfDay, appGroupName) ?: 0
            } ?: 0
            
            // Cache the result
            cache.cacheTodayUsage(appGroupName, usage)
            usage
        } catch (e: Exception) {
            println("❌ Error getting today's usage for $appGroupName: ${e.message}")
            0
        }
    }

    suspend fun getUsageRecordsForDateRange(
        startDate: Date,
        endDate: Date
    ): List<UsageRecordEntity> {
        return usageRecordDao.getUsageRecordsForDateRange(startDate.time, endDate.time)
    }

    suspend fun getWeeklyUsage(appGroupName: String): List<UsageRecordEntity> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = calendar.timeInMillis

        return usageRecordDao.getUsageRecordsForGroup(appGroupName, weekAgo)
    }

    // MARK: - App Groups

    fun getAllAppGroups(): Flow<List<AppGroupEntity>> = appGroupDao.getAllAppGroups()

    fun getActiveAppGroups(): Flow<List<AppGroupEntity>> = appGroupDao.getActiveAppGroups()

    suspend fun saveAppGroup(
        name: String,
        dailyLimitMinutes: Int,
        selectedApps: List<String>
    ): AppGroupEntity {
        val selectedAppsJson = com.google.gson.Gson().toJson(selectedApps)
        val appGroup = AppGroupEntity(
            name = name,
            dailyLimitMinutes = dailyLimitMinutes,
            selectedAppsJson = selectedAppsJson
        )
        appGroupDao.insertAppGroup(appGroup)
        return appGroup
    }

    suspend fun updateAppGroup(appGroup: AppGroupEntity) {
        appGroupDao.updateAppGroup(appGroup)
    }

    suspend fun deleteAppGroup(appGroup: AppGroupEntity) {
        appGroupDao.deleteAppGroup(appGroup)
    }

    suspend fun clearAllAppGroups() {
        appGroupDao.deleteAllAppGroups()
        // Clear related caches
        cache.invalidateAppGroups()
        // Also clear usage data since it's tied to app groups
        cache.invalidateAll()
    }

    suspend fun getAppGroupById(id: String): AppGroupEntity? {
        return appGroupDao.getAppGroupById(id)
    }

    suspend fun getActiveAppGroupCount(): Int {
        return try {
            withTimeoutOrNull(3000) {
                appGroupDao.getActiveAppGroupCount()
            } ?: 0
        } catch (e: Exception) {
            println("❌ Error getting active app group count: ${e.message}")
            0
        }
    }

    // MARK: - User Settings

    fun getUserSettings(): Flow<UserSettingsEntity?> = userSettingsDao.getUserSettings().flowOn(ioDispatcher)

    suspend fun getUserSettingsSync(): UserSettingsEntity = withContext(ioDispatcher) {
        // Try to get from cache first
        cache.getCachedUserSettings()?.let {
            return@withContext it
        }
        
        // Get from database and cache the result
        val settings = userSettingsDao.getOrCreateUserSettings()
        cache.cacheUserSettings(settings)
        settings
    }

    suspend fun updatePremiumStatus(isPremium: Boolean) = withContext(ioDispatcher) {
        // Ensure settings exist first
        val settings = getUserSettingsSync()
        userSettingsDao.updatePremiumStatus(isPremium)
        
        // Update cache
        cache.invalidateUserSettings()
        cache.cacheUserSettings(settings.copy(isPremium = isPremium))
    }

    suspend fun updateStreak(streakCount: Int) = withContext(ioDispatcher) {
        // Get cached streak to avoid unnecessary updates
        val cachedStreak = cache.getCachedStreak()
        if (cachedStreak == streakCount) {
            return@withContext
        }
        
        // Ensure settings exist first
        val settings = getUserSettingsSync()
        userSettingsDao.updateStreak(streakCount, Date().time)
        
        // Update cache
        cache.cacheStreak(streakCount)
        cache.invalidateUserSettings()
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        // Ensure settings exist first
        val settings = getUserSettingsSync()
        userSettingsDao.updateNotificationsEnabled(enabled)
        
        // Update cache
        cache.invalidateUserSettings()
        cache.cacheUserSettings(settings.copy(notificationsEnabled = enabled))
    }

    // MARK: - Utility Methods

    suspend fun calculateStreak(): Int = withContext(ioDispatcher) {
        try {
            // Try to get from cache first
            cache.getCachedStreak()?.let {
                return@withContext it
            }
            
            // Use cached streak calculation with timeout
            val streak = withTimeoutOrNull(2000) {
                calculateStreakOptimized()
            } ?: 0
            
            // Cache the result
            cache.cacheStreak(streak)
            streak
        } catch (e: Exception) {
            println("❌ Error calculating streak: ${e.message}")
            0
        }
    }
    
    private suspend fun calculateStreakOptimized(): Int {
        try {
            // Use the new optimized query that gets consecutive dates with success status
            val streakData = usageRecordDao.getStreakCalculationData()
            
            if (streakData.isEmpty()) {
                return 0
            }

            // Calculate consecutive successful days from today backwards
            var streak = 0
            for (dayData in streakData) {
                if (dayData.count > 0) {
                    // This day was successful (had records with limitExceeded = 0)
                    streak++
                } else {
                    // Break streak on first unsuccessful day
                    break
                }
            }

            return streak
        } catch (e: Exception) {
            println("⚠️ Optimized streak calculation failed, falling back to legacy method")
            return calculateStreakLegacy()
        }
    }
    
    private suspend fun calculateStreakLegacy(): Int {
        val calendar = Calendar.getInstance()
        val thirtyDaysAgo = calendar.apply {
            add(Calendar.DAY_OF_MONTH, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Get all successful dates in one query
        val successfulDates = usageRecordDao.getSuccessfulDatesOptimized(thirtyDaysAgo)
        
        if (successfulDates.isEmpty()) {
            return 0
        }

        // Convert to date set for efficient lookup
        val successfulDateSet = successfulDates.map { result ->
            Calendar.getInstance().apply {
                timeInMillis = result.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSet()

        // Calculate streak going backwards from today
        var streak = 0
        val currentCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check each day going backwards for up to 30 days
        repeat(30) {
            val dayStart = currentCal.timeInMillis
            
            if (successfulDateSet.contains(dayStart)) {
                streak++
                currentCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                return@repeat // Break streak
            }
        }

        return streak
    }

    suspend fun cleanupOldRecords() {
        // Keep only last 90 days of records
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -90)
        val cutoffDate = calendar.timeInMillis

        usageRecordDao.deleteOldRecords(cutoffDate)
    }
    
    // MARK: - Data Recovery
    
    suspend fun attemptDataRecovery(context: Context): Boolean {
        return try {
            val backupService = com.awaytime.app.service.DataBackupService(context)
            
            // Check if we have any data in the database
            val appGroupCount = getActiveAppGroupCount()
            val userSettings = getUserSettingsSync()
            
            if (appGroupCount == 0 && !backupService.hasRecentBackup()) {
                println("⚠️ No data found and no backup available")
                return false
            }
            
            if (appGroupCount == 0 && backupService.hasRecentBackup()) {
                println("🔄 No app groups found, attempting restore from backup...")
                return backupService.restoreFromBackup()
            }
            
            println("✅ Data appears to be intact")
            return true
            
        } catch (e: Exception) {
            println("❌ Data recovery attempt failed: ${e.message}")
            false
        }
    }
    
    suspend fun getUsageRecordsBetween(startTime: Long, endTime: Long): kotlinx.coroutines.flow.Flow<List<com.awaytime.app.service.UsageRecord>> {
        return kotlinx.coroutines.flow.flow { 
            val records = usageRecordDao.getUsageRecordsForDateRange(startTime, endTime)
            val usageRecords = records.map { entity ->
                com.awaytime.app.service.UsageRecord(
                    appName = entity.appGroupName,
                    usageMinutes = entity.usageMinutes,
                    limitMinutes = 0, // Would need to get from app group
                    date = entity.getDateAsDate(),
                    limitExceeded = entity.limitExceeded
                )
            }
            emit(usageRecords)
        }
    }
    
    // Synchronous methods for background service
    suspend fun getAllAppGroupsSync(): List<AppGroupEntity> {
        return appGroupDao.getAllAppGroupsSync()
    }
    
    suspend fun getUsageRecordsSync(days: Int): List<UsageRecordEntity> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        val startTime = calendar.timeInMillis
        return usageRecordDao.getUsageRecordsForDateRange(startTime, System.currentTimeMillis())
    }
}