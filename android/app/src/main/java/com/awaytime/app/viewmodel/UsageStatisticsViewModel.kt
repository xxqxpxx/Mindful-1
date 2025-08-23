package com.awaytime.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.data.entity.UsageRecordEntity
import com.awaytime.app.data.paging.UsageRecordPagingSource
import com.awaytime.app.data.paging.UsageRecordPagingSourceFactory
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.service.DataExportService
import com.awaytime.app.service.ExportFormat
import com.awaytime.app.service.ExportRequest
import com.awaytime.app.service.ExportDataType
import com.awaytime.app.ui.statistics.DateRange
import com.awaytime.app.ui.statistics.UsageAnalyticsData
import com.awaytime.app.ui.statistics.UsageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.Duration.Companion.days

/**
 * ViewModel for Usage Statistics screen with Paging3 and Flow-based state management
 * Implements proper reactive programming patterns for large datasets
 */
class UsageStatisticsViewModel(private val context: Context) : ViewModel() {

    private val repository = AwayTimeRepository(context)
    private val database = AwayTimeDatabase.getDatabase(context)
    private val dataExportService = DataExportService(context)
    
    // UI State
    private val _selectedFilter = MutableStateFlow(UsageFilter.LAST_30_DAYS)
    val selectedFilter: StateFlow<UsageFilter> = _selectedFilter.asStateFlow()
    
    private val _selectedAppGroup = MutableStateFlow<String?>(null)
    val selectedAppGroup: StateFlow<String?> = _selectedAppGroup.asStateFlow()
    
    private val _dateRange = MutableStateFlow(getDateRangeForFilter(UsageFilter.LAST_30_DAYS))
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()
    
    private val _analyticsData = MutableStateFlow<UsageAnalyticsData?>(null)
    val analyticsData: StateFlow<UsageAnalyticsData?> = _analyticsData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Paging flow for usage records - reactive to filter changes
     */
    val usageRecordsPagingFlow: Flow<PagingData<UsageRecordEntity>> = combine(
        selectedFilter,
        selectedAppGroup,
        dateRange
    ) { filter, appGroup, range ->
        createPagingSourceForCurrentFilters(filter, appGroup, range)
    }.flatMapLatest { pagingSource ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 10,
                enablePlaceholders = false,
                initialLoadSize = 100
            ),
            pagingSourceFactory = { pagingSource }
        ).flow
    }.cachedIn(viewModelScope)
    
    init {
        loadAnalyticsData()
    }
    
    /**
     * Update the selected filter and refresh data
     */
    fun updateFilter(filter: UsageFilter) {
        viewModelScope.launch {
            _selectedFilter.value = filter
            _dateRange.value = getDateRangeForFilter(filter)
            loadAnalyticsData()
        }
    }
    
    /**
     * Update the app group filter
     */
    fun updateAppGroupFilter(appGroup: String?) {
        viewModelScope.launch {
            _selectedAppGroup.value = appGroup
            loadAnalyticsData()
        }
    }
    
    /**
     * Update the date range directly
     */
    fun updateDateRange(range: DateRange) {
        viewModelScope.launch {
            _dateRange.value = range
            loadAnalyticsData()
        }
    }
    
    /**
     * Refresh analytics data
     */
    fun refreshAnalytics() {
        loadAnalyticsData()
    }
    
    /**
     * Export current statistics
     */
    fun exportStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportRequest = ExportRequest(
                    dataTypes = setOf(ExportDataType.USAGE_RECORDS, ExportDataType.ANALYTICS_SUMMARY),
                    format = ExportFormat.CSV,
                    dateRange = com.awaytime.app.service.DateRange(
                        startDate = _dateRange.value.start,
                        endDate = _dateRange.value.end
                    )
                )
                
                val result = dataExportService.exportData(exportRequest)
                
                if (result.success) {
                    // Share the exported file
                    dataExportService.shareExportFile(result, ExportFormat.CSV)
                } else {
                    _error.value = result.error ?: "Export failed"
                }
            } catch (e: Exception) {
                _error.value = "Export error: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Load analytics data based on current filters
     */
    private fun loadAnalyticsData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentDateRange = _dateRange.value
                val currentAppGroup = _selectedAppGroup.value
                
                // Load usage records for analytics calculation
                val usageRecords = if (currentAppGroup != null) {
                    database.usageRecordDao().getUsageRecordsForGroupPaged(
                        appGroupName = currentAppGroup,
                        startDate = currentDateRange.start.time,
                        endDate = currentDateRange.end.time,
                        limit = 10000, // Get enough for analytics
                        offset = 0
                    )
                } else {
                    database.usageRecordDao().getUsageRecordsForDateRangePaged(
                        startDate = currentDateRange.start.time,
                        endDate = currentDateRange.end.time,
                        limit = 10000, // Get enough for analytics
                        offset = 0
                    )
                }
                
                // Calculate analytics
                val analytics = calculateAnalytics(usageRecords, currentDateRange)
                _analyticsData.value = analytics
                
            } catch (e: Exception) {
                _error.value = "Failed to load analytics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create paging source based on current filters
     */
    private fun createPagingSourceForCurrentFilters(
        filter: UsageFilter,
        appGroup: String?,
        range: DateRange
    ): UsageRecordPagingSource {
        return if (appGroup != null) {
            UsageRecordPagingSourceFactory.createForAppGroup(
                dao = database.usageRecordDao(),
                appGroupName = appGroup,
                dateRange = range.start to range.end
            )
        } else {
            UsageRecordPagingSourceFactory.createForDateRange(
                dao = database.usageRecordDao(),
                startDate = range.start,
                endDate = range.end,
                onlySuccessful = false
            )
        }
    }
    
    /**
     * Get date range for a given filter
     */
    private fun getDateRangeForFilter(filter: UsageFilter): DateRange {
        val endDate = Date()
        val startDate = when (filter) {
            UsageFilter.LAST_7_DAYS -> Date(endDate.time - 7.days.inWholeMilliseconds)
            UsageFilter.LAST_30_DAYS -> Date(endDate.time - 30.days.inWholeMilliseconds)
            UsageFilter.LAST_90_DAYS -> Date(endDate.time - 90.days.inWholeMilliseconds)
            UsageFilter.ALL_TIME -> Date(0) // Unix epoch
        }
        return DateRange(startDate, endDate)
    }
    
    /**
     * Calculate analytics from usage records
     */
    private suspend fun calculateAnalytics(
        records: List<UsageRecordEntity>,
        dateRange: DateRange
    ): UsageAnalyticsData = withContext(Dispatchers.Default) {
        
        if (records.isEmpty()) {
            return@withContext UsageAnalyticsData(
                totalUsageMinutes = 0,
                avgDailyUsage = 0,
                successRate = 0,
                mostUsedApp = "None",
                streakDays = 0
            )
        }
        
        // Calculate total usage
        val totalUsageMinutes = records.sumOf { it.usageMinutes }
        
        // Calculate average daily usage
        val daysDiff = maxOf(1, ((dateRange.end.time - dateRange.start.time) / (24 * 60 * 60 * 1000)).toInt())
        val avgDailyUsage = totalUsageMinutes / daysDiff
        
        // Calculate success rate (percentage of records where limit was not exceeded)
        val successfulRecords = records.count { !it.limitExceeded }
        val successRate = if (records.isNotEmpty()) {
            (successfulRecords * 100) / records.size
        } else 0
        
        // Find most used app
        val mostUsedApp = records
            .groupBy { it.appGroupName }
            .maxByOrNull { (_, groupRecords) -> groupRecords.sumOf { it.usageMinutes } }
            ?.key ?: "None"
        
        // Calculate current streak (simplified - would need more complex logic for accurate streak)
        val streakDays = try {
            repository.calculateStreak()
        } catch (e: Exception) {
            0
        }
        
        UsageAnalyticsData(
            totalUsageMinutes = totalUsageMinutes,
            avgDailyUsage = avgDailyUsage,
            successRate = successRate,
            mostUsedApp = mostUsedApp,
            streakDays = streakDays
        )
    }
    
    /**
     * Get available app groups for filtering
     */
    fun getAvailableAppGroups(): Flow<List<String>> {
        return repository.getAllAppGroups()
            .map { appGroups -> appGroups.map { it.name } }
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * Get usage summary for a specific date range
     */
    fun getUsageSummary(
        startDate: Date,
        endDate: Date
    ): Flow<UsageSummary> = flow {
        try {
            val records = database.usageRecordDao().getUsageRecordsForDateRangePaged(
                startDate = startDate.time,
                endDate = endDate.time,
                limit = 10000,
                offset = 0
            )
            
            val summary = UsageSummary(
                totalMinutes = records.sumOf { it.usageMinutes },
                totalDays = records.groupBy { 
                    Date(it.date).let { date ->
                        "${date.year}-${date.month}-${date.date}"
                    }
                }.size,
                averageMinutesPerDay = if (records.isNotEmpty()) {
                    records.sumOf { it.usageMinutes } / maxOf(1, records.groupBy { 
                        Date(it.date).let { date ->
                            "${date.year}-${date.month}-${date.date}"
                        }
                    }.size)
                } else 0,
                mostActiveDay = records
                    .groupBy { Date(it.date).let { date -> "${date.year}-${date.month}-${date.date}" } }
                    .maxByOrNull { (_, dayRecords) -> dayRecords.sumOf { it.usageMinutes } }
                    ?.key ?: "None",
                successfulDays = records.count { !it.limitExceeded }
            )
            
            emit(summary)
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get detailed app usage breakdown
     */
    fun getAppUsageBreakdown(): Flow<List<AppUsageBreakdown>> = flow {
        try {
            val currentRange = _dateRange.value
            val records = database.usageRecordDao().getUsageRecordsForDateRangePaged(
                startDate = currentRange.start.time,
                endDate = currentRange.end.time,
                limit = 10000,
                offset = 0
            )
            
            val breakdown = records
                .groupBy { it.appGroupName }
                .map { (appName, appRecords) ->
                    AppUsageBreakdown(
                        appName = appName,
                        totalMinutes = appRecords.sumOf { it.usageMinutes },
                        averageMinutesPerDay = appRecords.sumOf { it.usageMinutes } / maxOf(1, 
                            appRecords.groupBy { 
                                Date(it.date).let { date -> "${date.year}-${date.month}-${date.date}" }
                            }.size
                        ),
                        daysUsed = appRecords.groupBy { 
                            Date(it.date).let { date -> "${date.year}-${date.month}-${date.date}" }
                        }.size,
                        limitExceededCount = appRecords.count { it.limitExceeded }
                    )
                }
                .sortedByDescending { it.totalMinutes }
            
            emit(breakdown)
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
}

// Data classes for usage statistics
data class UsageSummary(
    val totalMinutes: Int,
    val totalDays: Int,
    val averageMinutesPerDay: Int,
    val mostActiveDay: String,
    val successfulDays: Int
)

data class AppUsageBreakdown(
    val appName: String,
    val totalMinutes: Int,
    val averageMinutesPerDay: Int,
    val daysUsed: Int,
    val limitExceededCount: Int
)
