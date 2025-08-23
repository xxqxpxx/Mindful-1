package com.awaytime.app.presentation.appselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awaytime.app.domain.model.AppInfo
import com.awaytime.app.domain.usecase.CreateAppGroupUseCase
import com.awaytime.app.domain.usecase.GetInstalledAppsUseCase
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Mindful ViewModel: App Selection
 * Clean architecture ViewModel following Mindful patterns
 */
class AppSelectionViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val createAppGroupUseCase: CreateAppGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            getInstalledAppsUseCase().fold(
                onSuccess = { apps ->
                    _uiState.value = _uiState.value.copy(
                        availableApps = apps,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load apps"
                    )
                }
            )
        }
    }

    fun toggleAppSelection(packageName: String) {
        val currentState = _uiState.value
        val selectedApps = currentState.selectedApps.toMutableSet()
        
        if (selectedApps.contains(packageName)) {
            selectedApps.remove(packageName)
        } else {
            selectedApps.add(packageName)
        }
        
        _uiState.value = currentState.copy(selectedApps = selectedApps)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedApps = emptySet())
    }

    fun createAppGroup(name: String, dailyLimitMinutes: Int = 120) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isSaving = true, error = null)
            
            createAppGroupUseCase(
                name = name,
                selectedApps = currentState.selectedApps.toList(),
                dailyLimitMinutes = dailyLimitMinutes
            ).fold(
                onSuccess = { appGroup ->
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        selectedApps = emptySet(), // Clear selection after successful save
                        saveSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to create app group"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}

data class AppSelectionUiState(
    val availableApps: List<AppInfo> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val hasSelectedApps: Boolean
        get() = selectedApps.isNotEmpty()
    
    val selectedAppCount: Int
        get() = selectedApps.size
}