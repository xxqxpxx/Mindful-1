package com.awaytime.app.core

import android.content.Context
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.service.UsageTrackingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

/**
 * Core tracking functionality that coordinates usage monitoring
 * and provides a unified interface for the app's tracking features.
 */
class AwayTimeTracker(private val context: Context) {
    
    private val repository = AwayTimeRepository(context)
    private val usageService = UsageTrackingService(context)
    
    suspend fun startTracking() {
        usageService.startMonitoring()
    }
    
    fun stopTracking() {
        usageService.stopMonitoring()
    }
    
    suspend fun getCurrentUsageMinutes(appGroupName: String): Int {
        return repository.getTodayUsage(appGroupName)
    }
    
    fun getUsageHistory(): Flow<List<UsageHistoryItem>> {
        return repository.getAllUsageRecords().map { records ->
            records.map { record ->
                UsageHistoryItem(
                    date = record.getDateAsDate(),
                    appGroupName = record.appGroupName,
                    usageMinutes = record.usageMinutes,
                    limitExceeded = record.limitExceeded
                )
            }
        }
    }
    
    suspend fun isLimitExceeded(appGroupName: String): Boolean {
        val appGroups = repository.getAllAppGroups()
        val currentUsage = repository.getTodayUsage(appGroupName)
        
        return appGroups.map { groups ->
            val group = groups.find { it.name == appGroupName }
            group?.let { currentUsage >= it.dailyLimitMinutes } ?: false
        }.toString().toBoolean()
    }
    
    fun isMonitoring(): Boolean {
        return usageService.isMonitoringActive()
    }
}

data class UsageHistoryItem(
    val date: Date,
    val appGroupName: String,
    val usageMinutes: Int,
    val limitExceeded: Boolean
)