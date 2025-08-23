package com.awaytime.app

import android.app.Application
import android.content.Context
import com.awaytime.app.analytics.AnalyticsEvent
import com.awaytime.app.analytics.AnalyticsManager
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.service.*
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.content.pm.PackageManager

class AwayTimeApplication : Application() {
    
    companion object {
        private lateinit var instance: AwayTimeApplication
        
        fun getInstance(): AwayTimeApplication = instance
    }
    
    // Global services
    lateinit var analyticsManager: AnalyticsManager
        private set
    lateinit var database: AwayTimeDatabase
        private set
    lateinit var usageTrackingService: UsageTrackingService
        private set
    lateinit var goalTrackingService: GoalTrackingService
        private set
    lateinit var notificationService: NotificationService
        private set
    lateinit var subscriptionService: SubscriptionService
        private set
    lateinit var dataBackupService: DataBackupService
        private set
    lateinit var trialManager: PremiumTrialManager
        private set
    lateinit var trialExpirationManager: TrialExpirationManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            // Initialize crash prevention system first
            CrashPrevention.initialize(this)
            
            // Initialize Firebase
            initializeFirebase()
            
            // Initialize database
            initializeDatabase()
            
            // Initialize services
            initializeServices()
            
            // Setup analytics
            setupAnalytics()
            
            // Setup error reporting
            setupErrorReporting()
            
            // Setup notifications
            setupNotifications()
            
            println("✅ AwayTime Application initialized successfully")
        } catch (e: Exception) {
            println("❌ Critical error during application initialization: ${e.message}")
            e.printStackTrace()
            // Don't crash - try to continue with partial initialization
        }
    }
    
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            println("✅ Firebase initialized")
        } catch (e: Exception) {
            println("❌ Firebase initialization failed: ${e.message}")
        }
    }
    
    private fun initializeDatabase() {
        try {
            database = AwayTimeDatabase.getDatabase(this)
            println("✅ Database initialized")
        } catch (e: Exception) {
            println("❌ Failed to initialize database: ${e.message}")
            throw e // Re-throw to prevent app from continuing without database
        }
    }
    
    private fun initializeServices() {
        // Core services
        usageTrackingService = UsageTrackingService(this)
        goalTrackingService = GoalTrackingService(this)
        notificationService = NotificationService(this)
        subscriptionService = SubscriptionService(this)
        dataBackupService = DataBackupService(this)
        
        // Initialize subscription manager
        SubscriptionManager.initialize(this)
        
        // Initialize premium feature manager
        PremiumFeatureManager.getInstance(this)
        
        // Initialize trial system
        trialManager = PremiumTrialManager.getInstance(this)
        trialExpirationManager = TrialExpirationManager.getInstance(this)
        
        // Setup trial monitoring
        setupTrialMonitoring()
        
        // Schedule automatic data backup
        dataBackupService.scheduleAutoBackup()
        
        // Ensure default app group exists for usage tracking (with delay to let DB initialize)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(3000) // Give database more time to fully initialize
                createDefaultAppGroupIfNeeded()
                
                // Create initial backup after app groups are set up
                delay(2000)
                dataBackupService.createBackup()
            } catch (e: Exception) {
                println("❌ Error in app group initialization coroutine: ${e.message}")
                e.printStackTrace()
            }
        }
        
        println("✅ Core services initialized")
    }
    
    private fun setupAnalytics() {
        analyticsManager = AnalyticsManager.getInstance(this)
        analyticsManager.trackLaunchMetrics()
        println("✅ Analytics setup complete")
    }
    
    private fun setupErrorReporting() {
        // Initialize global error handler with safer approach
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                if (::analyticsManager.isInitialized) {
                    analyticsManager.trackError(throwable, "UncaughtException")
                }
                println("💥 Uncaught exception on ${thread.name}: ${throwable.message}")
                throwable.printStackTrace()
                
                // Call default handler to properly crash the app
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (e: Exception) {
                // Prevent infinite loops in error handling
                println("❌ Error in error handler: ${e.message}")
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        
        try {
            // Initialize error integration
            ErrorReporter.initialize(this)
            println("✅ Error reporting setup complete")
        } catch (e: Exception) {
            println("❌ Error reporting setup failed: ${e.message}")
        }
    }
    
    private fun setupNotifications() {
        // Notification channels are created automatically when NotificationService is instantiated
        println("✅ Notifications setup complete")
    }
    
    private fun setupTrialMonitoring() {
        try {
            // Monitor trial expiration events
            CoroutineScope(Dispatchers.IO).launch {
                trialExpirationManager.trialExpiredFlow.collect { event ->
                    when (event) {
                        is TrialExpirationEvent.TrialExpired -> {
                            println("🔔 Trial expired - handling graceful degradation")
                            handleTrialExpiration()
                        }
                        is TrialExpirationEvent.AppGroupsLimited -> {
                            println("📱 App groups limited due to trial expiration")
                        }
                        is TrialExpirationEvent.ScheduledLimitsDisabled -> {
                            println("⏰ Scheduled limits disabled due to trial expiration")
                        }
                        is TrialExpirationEvent.FeatureRevoked -> {
                            println("🔒 Feature revoked: ${event.feature.displayName}")
                        }
                    }
                }
            }
            
            // Monitor trial activation
            CoroutineScope(Dispatchers.IO).launch {
                trialManager.trialStatus.collect { status ->
                    when (status) {
                        TrialStatus.ACTIVE -> {
                            if (::notificationService.isInitialized) {
                                notificationService.showTrialActivatedNotification()
                            }
                            println("🎉 Trial activated - premium features unlocked")
                        }
                        TrialStatus.EXPIRED -> {
                            println("⏰ Trial expired - features will be limited")
                        }
                        else -> { /* No action needed */ }
                    }
                }
            }
            
            println("✅ Trial monitoring setup complete")
        } catch (e: Exception) {
            println("❌ Error setting up trial monitoring: ${e.message}")
        }
    }
    
    private suspend fun handleTrialExpiration() {
        try {
            // Notify subscription service about trial expiration
            if (::subscriptionService.isInitialized) {
                subscriptionService.handleTrialExpiration()
            }
            
            // Update premium feature manager
            val premiumFeatureManager = PremiumFeatureManager.getInstance(this)
            // The premium feature manager will automatically update when trial status changes
            
            // Track trial expiration for analytics
            if (::analyticsManager.isInitialized) {
                analyticsManager.track(
                    AnalyticsEvent.TRIAL_EXPIRED, mapOf(
                        "trial_duration_days" to PremiumTrialManager.TRIAL_DURATION_DAYS,
                        "user_type" to "trial_user"
                    )
                )
            }
            
        } catch (e: Exception) {
            println("❌ Error handling trial expiration: ${e.message}")
        }
    }
    
    private suspend fun createDefaultAppGroupIfNeeded() {
        try {
            // Add extra safety checks
            if (!::database.isInitialized) {
                println("⚠️ Database not initialized yet, skipping default app group creation")
                return
            }
            
            val repository = com.awaytime.app.data.repository.AwayTimeRepository(this)
            
            // Use a safer approach to check app group count with timeout
            val activeGroupCount = try {
                withTimeoutOrNull(5000) { // 5 second timeout
                    repository.getActiveAppGroupCount()
                } ?: run {
                    println("⚠️ Database query timed out, skipping default app group creation")
                    return
                }
            } catch (e: Exception) {
                println("⚠️ Could not check app group count: ${e.message}")
                return
            }
            
            if (activeGroupCount == 0) {
                println("📱 No app groups found, creating default group")
                
                // Get some popular apps that are commonly monitored (with safety checks)
                val commonApps = try {
                    withTimeoutOrNull(3000) { // 3 second timeout for app scanning
                        getCommonSocialMediaApps()
                    } ?: emptyList()
                } catch (e: Exception) {
                    println("⚠️ Could not scan for apps: ${e.message}")
                    emptyList<String>()
                }
                
                // Always create a basic default group, even if no apps found
                try {
                    val defaultGroup = repository.saveAppGroup(
                        name = "My Apps",
                        dailyLimitMinutes = 120, // 2 hours default
                        selectedApps = commonApps
                    )
                    if (commonApps.isNotEmpty()) {
                        println("✅ Created default app group '${defaultGroup.name}' with ${commonApps.size} apps")
                    } else {
                        println("✅ Created empty default app group '${defaultGroup.name}'")
                    }
                } catch (e: Exception) {
                    println("❌ Failed to save default app group: ${e.message}")
                }
            } else {
                println("📱 Found $activeGroupCount active app group(s)")
            }
        } catch (e: Exception) {
            println("❌ Fatal error in createDefaultAppGroupIfNeeded: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun getCommonSocialMediaApps(): List<String> {
        return try {
            val packageManager = packageManager ?: run {
                println("⚠️ PackageManager is null")
                return emptyList()
            }
            
            // Limit to most common apps to reduce risk
            val commonApps = listOf(
                "com.instagram.android",
                "com.facebook.katana", 
                "com.google.android.youtube",
                "com.whatsapp",
                "com.android.chrome"
            )
            
            // Only include apps that are actually installed (with extra safety and timeout)
            val installedApps = commonApps.mapNotNull { packageName ->
                try {
                    // Add null check and safer package info retrieval
                    val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
                    if (packageInfo != null) {
                        packageName
                    } else {
                        null
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // App not installed - this is expected
                    null
                } catch (e: Exception) {
                    // Other errors - log and skip
                    println("⚠️ Error checking package $packageName: ${e.message}")
                    null
                }
            }
            
            println("📱 Found ${installedApps.size} common apps installed: ${installedApps.joinToString(", ")}")
            installedApps
        } catch (e: Exception) {
            println("❌ Error scanning for common apps: ${e.message}")
            emptyList()
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        try {
            // Clean up services
            if (::usageTrackingService.isInitialized) {
                usageTrackingService.stopMonitoring()
            }
            if (::subscriptionService.isInitialized) {
                subscriptionService.disconnect()
            }
            if (::trialExpirationManager.isInitialized) {
                trialExpirationManager.cleanup()
            }
            
            // Force cleanup to prevent memory leaks
            CrashPrevention.forceCleanup()
            
            println("✅ Application terminated, services cleaned up")
        } catch (e: Exception) {
            println("❌ Error during application termination: ${e.message}")
        }
    }
}