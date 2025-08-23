package com.awaytime.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awaytime.app.data.dao.UsageRecordDao
import com.awaytime.app.data.entity.UsageRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * PagingSource for usage records - enables efficient loading of large historical datasets
 * Implements proper pagination for usage statistics and analytics screens
 */
class UsageRecordPagingSource(
    private val usageRecordDao: UsageRecordDao,
    private val appGroupName: String? = null,
    private val startDate: Date? = null,
    private val endDate: Date? = null,
    private val onlySuccessful: Boolean = false
) : PagingSource<Int, UsageRecordEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UsageRecordEntity> {
        return try {
            val currentPage = params.key ?: 0
            val pageSize = params.loadSize
            val offset = currentPage * pageSize

            withContext(Dispatchers.IO) {
                val records = when {
                    // Load records for specific app group
                    appGroupName != null -> {
                        if (startDate != null && endDate != null) {
                            usageRecordDao.getUsageRecordsForGroupPaged(
                                appGroupName = appGroupName,
                                startDate = startDate.time,
                                endDate = endDate.time,
                                limit = pageSize,
                                offset = offset
                            )
                        } else {
                            usageRecordDao.getUsageRecordsForGroupPaged(
                                appGroupName = appGroupName,
                                limit = pageSize,
                                offset = offset
                            )
                        }
                    }
                    
                    // Load records within date range
                    startDate != null && endDate != null -> {
                        if (onlySuccessful) {
                            usageRecordDao.getSuccessfulUsageRecordsPaged(
                                startDate = startDate.time,
                                endDate = endDate.time,
                                limit = pageSize,
                                offset = offset
                            )
                        } else {
                            usageRecordDao.getUsageRecordsForDateRangePaged(
                                startDate = startDate.time,
                                endDate = endDate.time,
                                limit = pageSize,
                                offset = offset
                            )
                        }
                    }
                    
                    // Load all records with optional success filter
                    onlySuccessful -> {
                        usageRecordDao.getAllSuccessfulUsageRecordsPaged(
                            limit = pageSize,
                            offset = offset
                        )
                    }
                    
                    // Load all records
                    else -> {
                        usageRecordDao.getAllUsageRecordsPaged(
                            limit = pageSize,
                            offset = offset
                        )
                    }
                }

                println("📄 Loaded page $currentPage: ${records.size} usage records (offset: $offset)")

                LoadResult.Page(
                    data = records,
                    prevKey = if (currentPage == 0) null else currentPage - 1,
                    nextKey = if (records.size < pageSize) null else currentPage + 1
                )
            }
        } catch (exception: Exception) {
            println("❌ Error loading usage records page: ${exception.message}")
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UsageRecordEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

/**
 * Factory for creating usage record paging sources with different configurations
 */
object UsageRecordPagingSourceFactory {
    
    fun createForAppGroup(
        dao: UsageRecordDao,
        appGroupName: String,
        dateRange: Pair<Date?, Date?>? = null
    ) = UsageRecordPagingSource(
        usageRecordDao = dao,
        appGroupName = appGroupName,
        startDate = dateRange?.first,
        endDate = dateRange?.second
    )
    
    fun createForDateRange(
        dao: UsageRecordDao,
        startDate: Date,
        endDate: Date,
        onlySuccessful: Boolean = false
    ) = UsageRecordPagingSource(
        usageRecordDao = dao,
        startDate = startDate,
        endDate = endDate,
        onlySuccessful = onlySuccessful
    )
    
    fun createForAll(
        dao: UsageRecordDao,
        onlySuccessful: Boolean = false
    ) = UsageRecordPagingSource(
        usageRecordDao = dao,
        onlySuccessful = onlySuccessful
    )
}
