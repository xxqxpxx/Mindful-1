package com.awaytime.app.data.paging

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.awaytime.app.domain.model.AppInfo
import com.awaytime.app.domain.usecase.GetInstalledAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PagingSource for loading installed apps with search functionality
 * Uses the existing GetInstalledAppsUseCase and applies pagination and filtering
 */
class AppPagingSource(
    private val context: Context,
    private val searchQuery: String = ""
) : PagingSource<Int, AppInfo>() {

    private val getInstalledAppsUseCase = GetInstalledAppsUseCase(context)
    private var allApps: List<AppInfo>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AppInfo> {
        println("🎯 AppPagingSource: Loading page ${params.key ?: 0}, size=${params.loadSize}")
        return try {
            val currentPage = params.key ?: 0
            val pageSize = params.loadSize
            
            withContext(Dispatchers.IO) {
                // Load all apps once if not already loaded
                if (allApps == null) {
                    println("🎯 AppPagingSource: Loading all apps for the first time")
                    val result = getInstalledAppsUseCase()
                    result.fold(
                        onSuccess = { apps -> 
                            println("🎯 AppPagingSource: Loaded ${apps.size} apps successfully")
                            allApps = apps 
                        },
                        onFailure = { 
                            println("❌ AppPagingSource: Failed to load apps: ${it.message}")
                            return@withContext LoadResult.Error(it) 
                        }
                    )
                }
                
                // Filter apps based on search query
                val filteredApps = allApps!!.filter { app ->
                    if (searchQuery.isBlank()) {
                        true
                    } else {
                        app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                // Apply pagination
                val startIndex = currentPage * pageSize
                val endIndex = minOf(startIndex + pageSize, filteredApps.size)
                
                val pageData = if (startIndex < filteredApps.size) {
                    filteredApps.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                
                println("🎯 AppPagingSource: Returning page $currentPage with ${pageData.size} apps")
                LoadResult.Page(
                    data = pageData,
                    prevKey = if (currentPage == 0) null else currentPage - 1,
                    nextKey = if (endIndex >= filteredApps.size) null else currentPage + 1
                )
            }
        } catch (exception: Exception) {
            println("❌ AppPagingSource: Exception during load: ${exception.message}")
            exception.printStackTrace()
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, AppInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
