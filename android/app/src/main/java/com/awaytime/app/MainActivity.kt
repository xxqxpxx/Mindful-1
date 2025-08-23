@file:OptIn(ExperimentalMaterial3Api::class)

package com.awaytime.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.awaytime.app.service.GamificationIntegration
import com.awaytime.app.service.PermissionService
 import com.awaytime.app.ui.navigation.AwayTimeNavigation
import com.awaytime.app.ui.theme.AwayTimeTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionService: PermissionService
    private lateinit var gamificationIntegration: GamificationIntegration
    
    // Permission request launchers - only used when contextually appropriate
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Notification permission granted")
            // Update permission service state
            permissionService.updatePermissionStatuses()
        } else {
            println("❌ Notification permission denied")
        }
    }
    
    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission status after user returns from settings
        permissionService.updatePermissionStatuses()
        if (permissionService.hasUsageStatsPermission()) {
            println("✅ Usage stats permission granted")
            startUsageTrackingIfReady()
        } else {
            println("❌ Usage stats permission not granted")
        }
    }
    
    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission status after user returns from settings
        permissionService.updatePermissionStatuses()
        if (permissionService.hasAccessibilityPermission()) {
            println("✅ Accessibility permission granted")
        } else {
            println("❌ Accessibility permission not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize services
        initializeServices()
        
        // Setup analytics
        setupAnalytics()
        
        // Setup UI - let onboarding handle permission requests contextually
        setContent {
            AwayTimeTheme {
                AwayTimeNavigation(
                    permissionService = permissionService,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onRequestUsageStatsPermission = ::requestUsageStatsPermission,
                    onRequestAccessibilityPermission = ::requestAccessibilityPermission
                )
            }
        }
        
        // Setup gamification integration
        setupGamificationIntegration()
    }
    
    private fun initializeServices() {
        try {
            val application = application as? AwayTimeApplication 
                ?: throw IllegalStateException("Application is not AwayTimeApplication")
            
            // Initialize emergency mode first (safety measure)
            com.awaytime.app.service.EmergencyMode.initialize(this)
            
            // Initialize crash prevention system
            com.awaytime.app.service.CrashPrevention.initialize(this)
            
            // Initialize performance optimization
            com.awaytime.app.service.PerformanceManager.initialize(this)
            
            permissionService = PermissionService(this)
            gamificationIntegration = GamificationIntegration(this)
            
            // Start usage tracking if basic permissions are available
            if (permissionService.hasUsageStatsPermission()) {
                startUsageTrackingIfReady()
            }
        } catch (e: Exception) {
            println("❌ Error initializing services: ${e.message}")
            // Continue without crashing - essential services will be recreated as needed
        }
    }
    
    private fun setupAnalytics() {
        try {
            val application = application as? AwayTimeApplication
            if (application != null) {
                application.analyticsManager.startSession()
                application.analyticsManager.trackOnboardingStep("main_activity_created", true)
            } else {
                println("⚠️ Application is not AwayTimeApplication, skipping analytics")
            }
        } catch (e: Exception) {
            println("❌ Error setting up analytics: ${e.message}")
        }
    }
    
    // Permission request methods - called contextually from onboarding or settings
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    fun requestUsageStatsPermission() {
        if (!permissionService.hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsPermissionLauncher.launch(intent)
        }
    }
    
    fun requestAccessibilityPermission() {
        if (!permissionService.hasAccessibilityPermission()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionLauncher.launch(intent)
        }
    }
    
    private fun startUsageTrackingIfReady() {
        // Only start if we have the minimum required permissions
        if (::permissionService.isInitialized && permissionService.hasUsageStatsPermission()) {
            val application = application as? AwayTimeApplication
            if (application == null) {
                println("⚠️ Application is null, cannot start usage tracking")
                return
            }
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Check data integrity and attempt recovery if needed
                    val repository = com.awaytime.app.data.repository.AwayTimeRepository(this@MainActivity)
                    val dataRecovered = withTimeoutOrNull(10_000) {
                        repository.attemptDataRecovery(this@MainActivity)
                    } ?: false
                    
                    if (!dataRecovered) {
                        println("⚠️ Data recovery failed, but continuing with app startup")
                    }
                    
                    withTimeoutOrNull(5_000) {
                        application.usageTrackingService.startMonitoring()
                    } ?: run {
                        println("⚠️ Usage tracking start timed out")
                        return@launch
                    }
                    
                    println("✅ Usage tracking started")
                } catch (e: Exception) {
                    println("❌ Failed to start usage tracking: ${e.message}")
                    try {
                        application.analyticsManager.trackError(e, "MainActivity.startUsageTracking")
                    } catch (e2: Exception) {
                        println("❌ Failed to track error: ${e2.message}")
                    }
                }
            }
        }
    }
    
    private fun setupGamificationIntegration() {
        // Observe motivational toasts
        lifecycleScope.launch {
            gamificationIntegration.motivationalToastFlow.collect { message ->
                showMotivationalToast(message)
            }
        }
        
        // Observe experience gained events
        lifecycleScope.launch {
            gamificationIntegration.experienceGainedFlow.collect { event ->
                showExperienceAnimation(event.points, event.action)
            }
        }
        
        // Observe confetti events
        lifecycleScope.launch {
            gamificationIntegration.confettiFlow.collect {
                showConfettiAnimation()
            }
        }
    }
    
    private fun showMotivationalToast(message: String) {
        // Implementation would depend on your toast system
        println("Showing motivational toast: $message")
    }
    
    private fun showExperienceAnimation(points: Int, action: String) {
        // Implementation would depend on your animation system
        println("Showing XP animation: +$points XP for $action")
    }
    
    private fun showConfettiAnimation() {
        // Implementation would depend on your animation system
        println("Showing confetti animation")
    }
    
    override fun onResume() {
        super.onResume()
        try {
            val application = application as? AwayTimeApplication
            if (application != null) {
                application.analyticsManager.resumeSession()
                
                // Notify performance manager
                com.awaytime.app.service.PerformanceManager.handleAppResumed()
                
                // Check if permissions have been granted while app was in background
                if (::permissionService.isInitialized) {
                    permissionService.updatePermissionStatuses()
                    if (permissionService.hasUsageStatsPermission() && 
                        !application.usageTrackingService.isMonitoringActive()) {
                        startUsageTrackingIfReady()
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error in onResume: ${e.message}")
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            val application = application as? AwayTimeApplication
            application?.analyticsManager?.endSession()
            
            // Notify performance manager
            com.awaytime.app.service.PerformanceManager.handleAppPaused()
        } catch (e: Exception) {
            println("❌ Error in onPause: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up is handled in Application.onTerminate()
    }
    

}