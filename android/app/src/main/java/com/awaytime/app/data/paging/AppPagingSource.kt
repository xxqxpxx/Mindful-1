package com.awaytime.app.data.paging

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awaytime.app.models.InstalledApp
import com.awaytime.app.service.MindfulAppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OPTIMIZED PagingSource that integrates with the high-performance MindfulAppManager
 * 
 * KEY FEATURES:
 * - Integrates with optimized MindfulAppManager
 * - Uses lazy loading and memory management
 * - Proper error handling and cancellation support
 * - Efficient search filtering
 */
class AppPagingSource(
    private val context: Context,
    private val searchQuery: String = ""
) : PagingSource<Int, InstalledApp>() {

    private val appManager = MindfulAppManager.getInstance(context)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, InstalledApp> {
        return try {
            val currentPage = params.key ?: 0
            
            withContext(Dispatchers.IO) {
                // Use the optimized app manager to load the page
                val result = appManager.loadAppsPage(
                    page = currentPage,
                    forceRefresh = false,
                    searchQuery = searchQuery
                )
                
                when (result) {
                    is com.awaytime.app.models.AppResult.Success -> {
                        val apps = result.data
                        val totalPages = appManager.getTotalPages(searchQuery)
                        
                        println("📄 Loaded page $currentPage: ${apps.size} apps (search: '$searchQuery')")
                        
                        LoadResult.Page(
                            data = apps,
                            prevKey = if (currentPage == 0) null else currentPage - 1,
                            nextKey = if (currentPage >= totalPages - 1) null else currentPage + 1
                        )
                    }
                    is com.awaytime.app.models.AppResult.Error -> {
                        println("❌ Error loading apps page: ${result.exception.message}")
                        LoadResult.Error(result.exception)
                    }
                    is com.awaytime.app.models.AppResult.Loading -> {
                        // This shouldn't happen with the optimized manager, but handle it
                        LoadResult.Error(Exception("Unexpected loading state"))
                    }
                }
            }
        } catch (exception: Exception) {
            println("❌ Error in AppPagingSource: ${exception.message}")
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, InstalledApp>): Int? {
        // Return the page closest to the anchor position
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
