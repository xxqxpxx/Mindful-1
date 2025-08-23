package com.awaytime.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awaytime.app.cache.DatabaseCache
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.service.AppBlockingService
import com.awaytime.app.service.BlockingStats
import com.awaytime.app.service.BlockingStatus
import com.awaytime.app.service.NotificationService
import com.awaytime.app.service.UsageTrackingService
import com.awaytime.app.integration.MindfulFeaturesIntegration
import com.awaytime.app.service.EnhancedServiceManager
import com.awaytime.app.service.EnhancedServiceStatus
import com.awaytime.app.service.BedtimeManager
import com.awaytime.app.models.BedtimeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

data class DashboardUiState(
    val todayUsageMinutes: Int = 0,
    val dailyLimitMinutes: Int = 120, // Default 2 hours
    val streakDays: Int = 0,
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val currentAppGroup: AppGroupEntity? = null,
    val isLoading: Boolean = false,
    // Enhanced features state
    val enhancedServiceStatus: EnhancedServiceStatus? = null,
    val isFocusSessionActive: Boolean = false,
    val focusSessionTimeRemaining: String = "",
    val enhancedBlockingEnabled: Boolean = false,
    val contentFilteringEnabled: Boolean = false
) {
    val usageProgress: Float
        get() = if (dailyLimitMinutes > 0 && todayUsageMinutes >= 0) {
            (todayUsageMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceAtMost(1.0f)
        } else 0f

    val timeRemainingMinutes: Int
        get() = maxOf(0, dailyLimitMinutes - todayUsageMinutes)

    val timeRemainingText: String
        get() {
            val hours = timeRemainingMinutes / 60
            val minutes = timeRemainingMinutes % 60

            return if (hours > 0) {
                "${hours}h ${minutes}m left today"
            } else {
                "${minutes}m left today"
            }
        }
}

class DashboardViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val repository = AwayTimeRepository(context)
    private val usageTrackingService = UsageTrackingService(context)
    private val notificationService = NotificationService(context)
    private val appBlockingService = AppBlockingService(context)
    private val prefs = context.getSharedPreferences("awaytime", Context.MODE_PRIVATE)
    private val cache = DatabaseCache.getInstance()
    
    // Enhanced features integration
    private val mindfulIntegration = MindfulFeaturesIntegration(context, repository)
    private val enhancedServiceManager = EnhancedServiceManager.getInstance(context, repository)
    private val bedtimeManager = BedtimeManager.getInstance(context)
    
    // Job for managing real-time usage tracking
    private var usageUpdateJob: Job? = null
    
    // Debouncing controls
    private val streakUpdateDebouncer = AtomicBoolean(false)
    private val usageQueryDebouncer = AtomicBoolean(false)

    init {
        loadUserData()
        observeAppGroups()
        observeUserSettings()
        startRealTimeUsageTracking()
        
        // Initialize enhanced features
        initializeEnhancedFeatures()
        
        // Additional check for app groups after a short delay to handle initialization race conditions
        viewModelScope.launch {
            delay(3000) // Wait 3 seconds
            val currentAppGroups = repository.getActiveAppGroups().first()
            if (_uiState.value.currentAppGroup == null && currentAppGroups.isNotEmpty()) {
                println("📱 Found app group after initialization delay, refreshing...")
                setCurrentAppGroup(currentAppGroups.first())
            }
        }
    }

    private fun observeAppGroups() {
        viewModelScope.launch {
            repository.getActiveAppGroups().collect { appGroups ->
                val currentGroup = appGroups.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    currentAppGroup = currentGroup,
                    dailyLimitMinutes = currentGroup?.dailyLimitMinutes ?: 120
                )

                // Load today's usage for the current group
                currentGroup?.let { group ->
                    loadTodayUsage(group.name)
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeUserSettings() {
        viewModelScope.launch {
            // Use cached user settings for reactive updates
            cache.userSettings
                .filterNotNull()
                .debounce(500) // Debounce updates by 500ms
                .flowOn(Dispatchers.IO)
                .collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        isPremium = settings.isPremium,
                        streakDays = settings.streakCount
                    )
                }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load user settings from cache first, then database
                val userSettings = repository.getUserSettingsSync()

                // Calculate current streak with debouncing
                val streak = if (!streakUpdateDebouncer.getAndSet(true)) {
                    val calculatedStreak = repository.calculateStreak()
                    repository.updateStreak(calculatedStreak)
                    
                    // Reset debouncer after 30 seconds
                    delay(30_000)
                    streakUpdateDebouncer.set(false)
                    
                    calculatedStreak
                } else {
                    // Use cached streak if recent calculation happened
                    cache.getCachedStreak() ?: userSettings.streakCount
                }

                _uiState.value = _uiState.value.copy(
                    isPremium = userSettings.isPremium,
                    streakDays = streak,
                    isLoading = false
                )

            } catch (e: Exception) {
                println("❌ Failed to load user data: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadTodayUsage(appGroupName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Debounce usage queries to prevent excessive database calls
            if (usageQueryDebouncer.getAndSet(true)) {
                delay(1000) // Wait 1 second before allowing another query
                usageQueryDebouncer.set(false)
                return@launch
            }
            
            try {
                val todayUsage = repository.getTodayUsage(appGroupName)
                // Ensure usage is never negative - if -1 or negative, default to 0
                val validUsage = if (todayUsage < 0) 0 else todayUsage
                _uiState.value = _uiState.value.copy(todayUsageMinutes = validUsage)
                
                // Only log occasionally to reduce log spam
                if (validUsage % 5 == 0 || validUsage == 0) {
                    println("📊 Usage update: $validUsage minutes for '$appGroupName'")
                }
            } catch (e: Exception) {
                println("❌ Failed to load today's usage: ${e.message}")
                _uiState.value = _uiState.value.copy(todayUsageMinutes = 0)
            } finally {
                // Reset debouncer after 2 seconds
                delay(2000)
                usageQueryDebouncer.set(false)
            }
        }
    }

    fun updateDailyLimit(minutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(dailyLimitMinutes = minutes)

            // Update the current app group in database
            _uiState.value.currentAppGroup?.let { appGroup ->
                val updatedGroup = appGroup.copy(dailyLimitMinutes = minutes)
                repository.updateAppGroup(updatedGroup)
            }

            // Also save to SharedPreferences for backward compatibility
            prefs.edit().putInt("dailyLimit", minutes).apply()
        }
    }

    @Deprecated("Use Mindful architecture instead - app selection is managed through AppGroupEntity")
    fun updateSelectedApps(apps: List<String>) {
        // Method deprecated - app selection now managed through AppGroupEntity in database
        println("⚠️ updateSelectedApps is deprecated. Use AppGroupEntity-based app selection instead.")
    }

    fun saveUsageRecord(usageMinutes: Int, limitExceeded: Boolean) {
        viewModelScope.launch {
            _uiState.value.currentAppGroup?.let { appGroup ->
                repository.saveUsageRecord(
                    date = Date(),
                    usageMinutes = usageMinutes,
                    appGroupName = appGroup.name,
                    limitExceeded = limitExceeded
                )

                // Block apps if limit exceeded
                if (limitExceeded) {
                    appBlockingService.blockApps(appGroup)
                }

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    todayUsageMinutes = usageMinutes,
                    isBlocked = limitExceeded
                )

                // Recalculate streak with debouncing
                if (!streakUpdateDebouncer.getAndSet(true)) {
                    val newStreak = repository.calculateStreak()
                    repository.updateStreak(newStreak)
                    
                    // Reset debouncer after 30 seconds
                    viewModelScope.launch {
                        delay(30_000)
                        streakUpdateDebouncer.set(false)
                    }
                }
            }
        }
    }

    fun updatePremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            repository.updatePremiumStatus(isPremium)
            _uiState.value = _uiState.value.copy(isPremium = isPremium)
        }
    }

    fun refreshData() {
        loadUserData()
    }

    // MARK: - App Group Management

    fun setCurrentAppGroup(appGroup: AppGroupEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentAppGroup = appGroup,
                dailyLimitMinutes = appGroup.dailyLimitMinutes
            )

            loadTodayUsage(appGroup.name)
        }
    }

    // MARK: - Usage Tracking Integration

    fun startUsageTracking() {
        viewModelScope.launch {
            try {
                usageTrackingService.startMonitoring()
                println("✅ Usage tracking started")
            } catch (e: Exception) {
                println("❌ Failed to start usage tracking: ${e.message}")
            }
        }
    }

    fun stopUsageTracking() {
        usageTrackingService.stopMonitoring()
    }

    fun isUsageTrackingActive(): Boolean {
        return usageTrackingService.isMonitoringActive()
    }

    fun requestNotificationPermission() {
        notificationService.requestNotificationPermission()
    }

    // MARK: - App Blocking Integration

    fun getBlockingStatus(): BlockingStatus? {
        return viewModelScope.async {
            _uiState.value.currentAppGroup?.let { appGroup ->
                appBlockingService.getBlockingStatus(appGroup)
            }
        }.getCompleted()
    }

    fun requestEmergencyOverride() {
        viewModelScope.launch {
            _uiState.value.currentAppGroup?.let { appGroup ->
                appBlockingService.requestEmergencyOverride(appGroup, 5) // 5 minutes
            }
        }
    }

    fun unblockApps() {
        viewModelScope.launch {
            _uiState.value.currentAppGroup?.let { appGroup ->
                appBlockingService.unblockApps(appGroup)
                _uiState.value = _uiState.value.copy(isBlocked = false)
            }
        }
    }

    fun getBlockingStats(): BlockingStats {
        return appBlockingService.getBlockingStats()
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return appBlockingService.isAccessibilityServiceEnabled()
    }

    fun checkAndApplyBlocking() {
        viewModelScope.launch {
            appBlockingService.checkAndApplyBlocking()
        }
    }

    // MARK: - Real-time Usage Tracking

    private fun startRealTimeUsageTracking() {
        usageUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Start the usage tracking service
                usageTrackingService.startMonitoring()
                
                // Set up periodic updates every 60 seconds (reduced frequency)
                kotlinx.coroutines.delay(5000) // Initial delay
                
                while (true) {
                    updateCurrentUsage()
                    kotlinx.coroutines.delay(60_000) // Update every minute instead of 30 seconds
                }
            } catch (e: Exception) {
                println("❌ Real-time usage tracking failed: ${e.message}")
            }
        }
    }

    private suspend fun updateCurrentUsage() {
        try {
            _uiState.value.currentAppGroup?.let { appGroup ->
                // Get current usage from the tracking service
                val rawUsage = usageTrackingService.getCurrentUsage(appGroup.name)
                // Ensure usage is never negative - if -1 or negative, default to 0
                val currentUsage = if (rawUsage < 0) 0 else rawUsage
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    todayUsageMinutes = currentUsage,
                    isBlocked = currentUsage >= appGroup.dailyLimitMinutes
                )
                
                // Check if we need to trigger blocking
                if (currentUsage >= appGroup.dailyLimitMinutes && !_uiState.value.isBlocked) {
                    // Trigger blocking
                    appBlockingService.blockApps(appGroup)
                    
                    // Send notification
                    notificationService.sendLimitReachedNotification(appGroup.name)
                    
                    // Save usage record
                    repository.saveUsageRecord(
                        date = Date(),
                        usageMinutes = currentUsage,
                        appGroupName = appGroup.name,
                        limitExceeded = true
                    )
                } else if (currentUsage >= (appGroup.dailyLimitMinutes * 0.8).toInt()) {
                    // Send warning notification at 80%
                    val remainingMinutes = appGroup.dailyLimitMinutes - currentUsage
                    notificationService.sendWarningNotification(appGroup.name, remainingMinutes)
                }
                
                // Reduce log frequency to every 5 minutes of usage change
                if (currentUsage % 5 == 0) {
                    println("📊 Usage updated: $currentUsage minutes for ${appGroup.name}")
                }
            } ?: run {
                // No app group exists - just show 0 usage safely
                _uiState.value = _uiState.value.copy(
                    todayUsageMinutes = 0,
                    isBlocked = false
                )
                println("⚠️ No app group found, showing 0 usage")
            }
        } catch (e: Exception) {
            println("❌ Failed to update current usage: ${e.message}")
            // On error, ensure we don't show negative values
            _uiState.value = _uiState.value.copy(
                todayUsageMinutes = 0,
                isBlocked = false
            )
        }
    }

    fun forceRefreshUsage() {
        viewModelScope.launch {
            updateCurrentUsage()
        }
    }
    
    // MARK: - Enhanced Features Integration
    
    private fun initializeEnhancedFeatures() {
        viewModelScope.launch {
            try {
                mindfulIntegration.initializeEnhancedFeatures()
                updateEnhancedServiceStatus()
                
                // Start periodic status updates
                startEnhancedServiceStatusUpdates()
            } catch (e: Exception) {
                println("❌ Failed to initialize enhanced features: ${e.message}")
            }
        }
    }
    
    private fun startEnhancedServiceStatusUpdates() {
        viewModelScope.launch {
            while (true) {
                try {
                    updateEnhancedServiceStatus()
                    delay(30_000) // Update every 30 seconds
                } catch (e: Exception) {
                    println("❌ Failed to update enhanced service status: ${e.message}")
                    delay(60_000) // Wait longer if error
                }
            }
        }
    }
    
    private fun updateEnhancedServiceStatus() {
        val status = enhancedServiceManager.getServiceStatus()
        val bedtimeState = bedtimeManager.bedtimeState.value
        val isBedtimeActive = bedtimeState is BedtimeState.Active || bedtimeState is BedtimeState.WindDown
        
        val enhancedStatus = EnhancedServiceStatus(
            vpnServiceRunning = status.vpnServiceRunning,
            notificationServiceRunning = status.notificationServiceRunning,
            focusSessionActive = status.focusSessionActive,
            accessibilityServiceRunning = status.accessibilityServiceRunning,
            bedtimeActive = isBedtimeActive
        )
        _uiState.value = _uiState.value.copy(
            enhancedServiceStatus = enhancedStatus,
            isFocusSessionActive = enhancedStatus.focusSessionActive
        )
    }
    
    fun enableEnhancedAppBlocking(enableVpn: Boolean = true) {
        viewModelScope.launch {
            try {
                val selectedApps = _uiState.value.currentAppGroup?.getSelectedApps()?.toSet() ?: emptySet()
                if (enableVpn) {
                    mindfulIntegration.enableEnhancedAppBlocking(selectedApps)
                } else {
                    enhancedServiceManager.updateBlockedApps(selectedApps)
                }
                
                _uiState.value = _uiState.value.copy(enhancedBlockingEnabled = true)
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to enable enhanced blocking: ${e.message}")
            }
        }
    }
    
    fun disableEnhancedAppBlocking() {
        viewModelScope.launch {
            try {
                enhancedServiceManager.updateBlockedApps(emptySet())
                enhancedServiceManager.stopInternetBlocking()
                
                _uiState.value = _uiState.value.copy(enhancedBlockingEnabled = false)
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to disable enhanced blocking: ${e.message}")
            }
        }
    }
    
    fun enableContentFiltering() {
        viewModelScope.launch {
            try {
                mindfulIntegration.enableContentFiltering()
                _uiState.value = _uiState.value.copy(contentFilteringEnabled = true)
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to enable content filtering: ${e.message}")
            }
        }
    }
    
    fun startFocusSession(durationMinutes: Int = 25) {
        viewModelScope.launch {
            try {
                val selectedApps = _uiState.value.currentAppGroup?.getSelectedApps()?.toSet() ?: emptySet()
                mindfulIntegration.startFocusSession(
                    durationMinutes = durationMinutes,
                    blockedApps = selectedApps
                )
                
                _uiState.value = _uiState.value.copy(
                    isFocusSessionActive = true,
                    focusSessionTimeRemaining = "${durationMinutes}m"
                )
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to start focus session: ${e.message}")
            }
        }
    }
    
    fun stopFocusSession() {
        viewModelScope.launch {
            try {
                mindfulIntegration.stopFocusSession(successful = true)
                _uiState.value = _uiState.value.copy(
                    isFocusSessionActive = false,
                    focusSessionTimeRemaining = ""
                )
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to stop focus session: ${e.message}")
            }
        }
    }
    
    fun applySocialMediaPreset() {
        viewModelScope.launch {
            try {
                mindfulIntegration.applySocialMediaPreset()
                _uiState.value = _uiState.value.copy(
                    enhancedBlockingEnabled = true,
                    contentFilteringEnabled = true
                )
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to apply social media preset: ${e.message}")
            }
        }
    }
    
    fun startProductivitySession(durationMinutes: Int = 25) {
        viewModelScope.launch {
            try {
                mindfulIntegration.startProductivitySession(durationMinutes)
                _uiState.value = _uiState.value.copy(
                    isFocusSessionActive = true,
                    focusSessionTimeRemaining = "${durationMinutes}m",
                    enhancedBlockingEnabled = true
                )
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to start productivity session: ${e.message}")
            }
        }
    }
    
    fun emergencyDisableAll() {
        viewModelScope.launch {
            try {
                mindfulIntegration.emergencyDisableAll()
                _uiState.value = _uiState.value.copy(
                    enhancedBlockingEnabled = false,
                    contentFilteringEnabled = false,
                    isFocusSessionActive = false,
                    focusSessionTimeRemaining = ""
                )
                updateEnhancedServiceStatus()
            } catch (e: Exception) {
                println("❌ Failed to emergency disable: ${e.message}")
            }
        }
    }
    
    fun getEnhancedServiceStatus(): EnhancedServiceStatus? {
        return _uiState.value.enhancedServiceStatus
    }
    
    // Debug method to clear all old app groups
    fun clearAllOldAppGroups() {
        viewModelScope.launch {
            try {
                repository.clearAllAppGroups()
                _uiState.value = _uiState.value.copy(
                    currentAppGroup = null
                )
                println("✅ Cleared all old app groups from database")
            } catch (e: Exception) {
                println("❌ Failed to clear app groups: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up usage tracking
        usageTrackingService.stopMonitoring()
        // Cancel the usage update job
        usageUpdateJob?.cancel()
    }
}