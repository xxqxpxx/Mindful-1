package com.awaytime.app.data.dao

import androidx.room.*
import com.awaytime.app.data.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

data class DateCountResult(
    val date: Long,
    val count: Int
)

@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_records ORDER BY date DESC")
    fun getAllUsageRecords(): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_records WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    suspend fun getUsageRecordsForDateRange(startDate: Long, endDate: Long): List<UsageRecordEntity>

    @Query("SELECT * FROM usage_records WHERE date >= :startOfDay AND date < :endOfDay AND appGroupName = :appGroupName LIMIT 1")
    suspend fun getTodayUsageForGroup(
        startOfDay: Long,
        endOfDay: Long,
        appGroupName: String
    ): UsageRecordEntity?

    @Query("SELECT * FROM usage_records WHERE appGroupName = :appGroupName AND date >= :startDate ORDER BY date DESC")
    suspend fun getUsageRecordsForGroup(
        appGroupName: String,
        startDate: Long
    ): List<UsageRecordEntity>

    @Query("SELECT SUM(usageMinutes) FROM usage_records WHERE date >= :startOfDay AND date < :endOfDay AND appGroupName = :appGroupName")
    suspend fun getTotalUsageForDay(startOfDay: Long, endOfDay: Long, appGroupName: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: UsageRecordEntity)

    @Update
    suspend fun updateUsageRecord(record: UsageRecordEntity)

    @Delete
    suspend fun deleteUsageRecord(record: UsageRecordEntity)

    @Query("DELETE FROM usage_records WHERE date < :cutoffDate")
    suspend fun deleteOldRecords(cutoffDate: Long)

    // Utility queries
    @Query("SELECT COUNT(*) FROM usage_records WHERE limitExceeded = 0 AND date >= :startDate")
    suspend fun getSuccessfulDaysCount(startDate: Long): Int

    @Query("SELECT * FROM usage_records WHERE date >= :startDate AND date < :endDate AND limitExceeded = 0")
    suspend fun getSuccessfulDays(startDate: Long, endDate: Long): List<UsageRecordEntity>
    
    @Query("""
        SELECT date, COUNT(*) as count 
        FROM usage_records 
        WHERE date >= :startDate AND limitExceeded = 0 
        GROUP BY date 
        ORDER BY date DESC
    """)
    suspend fun getSuccessfulDatesOptimized(startDate: Long): List<DateCountResult>
    
    // Optimized streak calculation - returns consecutive successful dates from today backwards
    @Query("""
        WITH RECURSIVE date_range(date_key) AS (
            SELECT date(datetime('now', 'start of day')) as date_key
            UNION ALL
            SELECT date(date_key, '-1 day')
            FROM date_range
            WHERE date_key > date('now', '-30 days')
        ),
        daily_success AS (
            SELECT 
                date(datetime(date / 1000, 'unixepoch')) as date_key,
                COUNT(*) as successful_records
            FROM usage_records 
            WHERE limitExceeded = 0 
                AND date >= strftime('%s', date('now', '-30 days', 'start of day')) * 1000
            GROUP BY date(datetime(date / 1000, 'unixepoch'))
        )
        SELECT 
            dr.date_key,
            COALESCE(ds.successful_records, 0) as count,
            strftime('%s', dr.date_key) * 1000 as timestamp
        FROM date_range dr
        LEFT JOIN daily_success ds ON dr.date_key = ds.date_key
        ORDER BY dr.date_key DESC
    """)
    suspend fun getStreakCalculationData(): List<StreakCalculationResult>
    
    data class StreakCalculationResult(
        val date_key: String,
        val count: Int,
        val timestamp: Long
    )
    
    // MARK: - Paging Queries for Efficient Loading
    
    @Query("SELECT * FROM usage_records ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllUsageRecordsPaged(
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    @Query("""
        SELECT * FROM usage_records 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUsageRecordsForDateRangePaged(
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    @Query("""
        SELECT * FROM usage_records 
        WHERE appGroupName = :appGroupName 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUsageRecordsForGroupPaged(
        appGroupName: String,
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    @Query("""
        SELECT * FROM usage_records 
        WHERE appGroupName = :appGroupName 
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUsageRecordsForGroupPaged(
        appGroupName: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    @Query("""
        SELECT * FROM usage_records 
        WHERE limitExceeded = 0 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getAllSuccessfulUsageRecordsPaged(
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    @Query("""
        SELECT * FROM usage_records 
        WHERE limitExceeded = 0 
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getSuccessfulUsageRecordsPaged(
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<UsageRecordEntity>
    
    // MARK: - Count Queries for Paging
    
    @Query("SELECT COUNT(*) FROM usage_records")
    suspend fun getTotalUsageRecordsCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM usage_records 
        WHERE appGroupName = :appGroupName
    """)
    suspend fun getUsageRecordsCountForGroup(appGroupName: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM usage_records 
        WHERE date >= :startDate AND date <= :endDate
    """)
    suspend fun getUsageRecordsCountForDateRange(
        startDate: Long,
        endDate: Long
    ): Int
}
