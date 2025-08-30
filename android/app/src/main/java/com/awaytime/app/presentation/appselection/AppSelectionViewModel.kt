package com.awaytime.app.presentation.appselection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.awaytime.app.data.paging.AppPagingSource
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.domain.usecase.CreateAppGroupUseCase
import com.awaytime.app.domain.model.AppInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Paging version of AppSelectionViewModel using Paging 3 library
 * Provides efficient lazy loading of installed apps with search capabilities
 */
class AppSelectionViewModel(
    private val context: Context,
    private val createAppGroupUseCase: CreateAppGroupUseCase
) : ViewModel() {

    init {
        println("🎯 AppSelectionViewModel: Initializing")
    }

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected apps state
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /**
     * Paging flow for installed apps - automatically updates when search query changes
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val appsPagingFlow: Flow<PagingData<AppInfo>> = searchQuery
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 5,
                    enablePlaceholders = false,
                    initialLoadSize = 20
                ),
                pagingSourceFactory = { 
                    AppPagingSource(context = context, searchQuery = query)
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    /**
     * Derived state for UI convenience
     */
    val hasSelectedApps: StateFlow<Boolean> = selectedApps
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val selectedAppCount: StateFlow<Int> = selectedApps
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggle app selection
     */
    fun toggleAppSelection(packageName: String) {
        val currentSelection = _selectedApps.value.toMutableSet()
        if (currentSelection.contains(packageName)) {
            currentSelection.remove(packageName)
        } else {
            currentSelection.add(packageName)
        }
        _selectedApps.value = currentSelection
    }

    /**
     * Clear all selected apps
     */
    fun clearSelection() {
        _selectedApps.value = emptySet()
    }

    /**
     * Create app group from selected apps
     */
    fun createAppGroup(name: String, dailyLimitMinutes: Int = 120) {
        viewModelScope.launch {
            val currentSelectedApps = _selectedApps.value
            if (currentSelectedApps.isEmpty()) {
                _error.value = "Please select at least one app"
                return@launch
            }

            _isSaving.value = true
            _error.value = null

            createAppGroupUseCase(
                name = name.ifBlank { "App Group" },
                selectedApps = currentSelectedApps.toList(),
                dailyLimitMinutes = dailyLimitMinutes
            ).fold(
                onSuccess = { _ ->
                    _isSaving.value = false
                    _selectedApps.value = emptySet() // Clear selection after successful save
                    _saveSuccess.value = true
                },
                onFailure = { error ->
                    _isSaving.value = false
                    _error.value = error.message ?: "Failed to create app group"
                }
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear save success flag
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}