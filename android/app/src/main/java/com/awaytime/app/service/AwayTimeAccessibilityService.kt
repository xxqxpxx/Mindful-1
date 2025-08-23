/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.awaytime.app.R
import com.awaytime.app.core.AppConstants.FACEBOOK_PACKAGE
import com.awaytime.app.core.AppConstants.INSTAGRAM_PACKAGE
import com.awaytime.app.core.AppConstants.REDDIT_PACKAGE
import com.awaytime.app.core.AppConstants.SETTINGS_PACKAGE
import com.awaytime.app.core.AppConstants.SNAPCHAT_PACKAGE
import com.awaytime.app.core.AppConstants.YOUTUBE_PACKAGE
import com.awaytime.app.enums.PlatformFeatures
import com.awaytime.app.helpers.PermissionsHelper
import com.awaytime.app.helpers.SharedPrefsHelper
import com.awaytime.app.models.Wellbeing
import com.awaytime.app.receivers.DeviceAppsChangedReceiver
import com.awaytime.app.service.accessibility.BrowserManager
import com.awaytime.app.service.accessibility.DeviceFeaturesManager
import com.awaytime.app.service.accessibility.ShortsPlatformManager
import com.awaytime.app.service.accessibility.TrackingManager
import com.awaytime.app.utils.ThreadUtils
import com.awaytime.app.utils.Throttler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * An AccessibilityService that monitors app usage and blocks access to specified content based on user settings.
 * Adapted from Mindful's robust implementation.
 */
class AwayTimeAccessibilityService : AccessibilityService(), OnSharedPreferenceChangeListener {
    companion object {
        private const val TAG = "AwayTime.AccessibilityService"

        const val ACTION_PERFORM_HOME_PRESS = "com.awaytime.app.action.performHomePress"
        const val ACTION_MIDNIGHT_ACCESSIBILITY_RESET = "com.awaytime.app.action.midnightAccessibilityReset"
        const val ACTION_TAMPER_PROTECTION_CHANGED = "com.awaytime.app.action.tamperProtectionChanged"

        // Set of desired events which will be processed
        private val desiredEvents = setOf(
            TYPE_WINDOWS_CHANGED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_VIEW_SCROLLED
        )

        private val browserPackages = mutableSetOf<String>()
        private val shortsPlatformPackages = mutableSetOf<String>()
        private val devicePlatformPackages = mutableSetOf<String>()
        
        @Volatile
        private var instance: AwayTimeAccessibilityService? = null
        
        @Synchronized
        fun getInstance(): AwayTimeAccessibilityService? {
            return try {
                instance?.takeIf { !it.isDestroyed }
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing service instance", e)
                null
            }
        }
        
        @Synchronized
        fun isServiceRunning(): Boolean {
            return try {
                instance?.let { !it.isDestroyed } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking service state", e)
                false
            }
        }
    }

    // Fixed thread pool for parallel event processing
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    private val throttler: Throttler = Throttler(500L)
    private val deviceAppsChangedReceiver: DeviceAppsChangedReceiver =
        DeviceAppsChangedReceiver(onAppsChanged = { refreshServiceConfig() })

    // Managers
    private lateinit var shortsPlatformManager: ShortsPlatformManager
    private lateinit var browserManager: BrowserManager
    private lateinit var deviceFeaturesManager: DeviceFeaturesManager
    private lateinit var trackingManager: TrackingManager

    private var wellbeing = Wellbeing()
    
    @Volatile
    private var isDestroyed = false

    override fun onCreate() {
        super.onCreate()
        synchronized(this) {
            isDestroyed = false
            instance = this
        }
        
        trackingManager = TrackingManager(context = this)
        deviceFeaturesManager = DeviceFeaturesManager(
            context = this,
            blockedContentGoBack = this::goBackWithToast
        )
        shortsPlatformManager = ShortsPlatformManager(
            context = this,
            blockedContentGoBack = this::goBackWithToast
        )
        browserManager = BrowserManager(
            context = this,
            shortsPlatformManager = shortsPlatformManager,
            blockedContentGoBack = this::goBackWithToast
        )

        // Register shared prefs listener and load data
        SharedPrefsHelper.registerUnregisterListenerToListenablePrefs(this, true, this)
        wellbeing = SharedPrefsHelper.getSetWellBeingSettings(this, null)

        // Register listener for install and uninstall events
        deviceAppsChangedReceiver.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MIDNIGHT_ACCESSIBILITY_RESET -> {
                shortsPlatformManager.resetShortsScreenTime()
                Log.d(TAG, "onStartCommand: Midnight reset completed")
            }

            ACTION_TAMPER_PROTECTION_CHANGED -> {
                Log.d(TAG, "onStartCommand: Tamper protection changed")
                refreshServiceConfig()
            }

            ACTION_PERFORM_HOME_PRESS -> {
                Log.d(TAG, "onStartCommand: Pressing home button")
                goBackWithToast(GLOBAL_ACTION_HOME)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        refreshServiceConfig()
        trackingManager.stopManualTracking()
        Log.d(TAG, "onServiceConnected: Accessibility service started successfully")
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            // If not desired event or executor is shutdown, then just return
            if (!desiredEvents.contains(event.eventType) || executorService.isShutdown) return

            executorService.submit {
                // Determine package and event source node
                val eventPackageName = event.packageName.toString()
                val node = if (eventPackageName == REDDIT_PACKAGE) event.source
                else rootInActiveWindow ?: event.source

                node?.let {
                    // Broadcast event
                    trackingManager.onNewEvent("${it.packageName}")

                    // Only process if any of the content is blocked
                    if (shouldBlockContent()) {
                        processEventInBackground(
                            packageName = eventPackageName,
                            node = it,
                            wellBeing = wellbeing.copy()
                        )
                    }
                }
            }

        } catch (ignored: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent", ignored)
        }
    }

    /**
     * Processes accessibility event in background thread instead of main thread.
     *
     * @param packageName The package name of the app generating the event.
     * @param node        The accessibility node representing the UI element currently in focus.
     */
    private fun processEventInBackground(
        packageName: String,
        node: AccessibilityNodeInfo,
        wellBeing: Wellbeing,
    ) {
        try {
            when (packageName) {
                in devicePlatformPackages ->
                    deviceFeaturesManager.blockFeatures(packageName, node, wellBeing)

                in shortsPlatformPackages ->
                    shortsPlatformManager.blockDistraction(packageName, node, wellBeing)

                in browserPackages ->
                    browserManager.blockDistraction(packageName, node, wellBeing)
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "processEventInBackground: Failed to process accessibility event in background",
                e
            )
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
        }
    }

    /**
     * Determines whether content should be blocked based on the current settings.
     *
     * @return `true` if content should be blocked based on the current settings,
     * `false` otherwise.
     */
    private fun shouldBlockContent(): Boolean {
        return wellbeing.blockedFeatures.isNotEmpty() ||
                wellbeing.blockedWebsites.isNotEmpty() ||
                wellbeing.nsfwWebsites.isNotEmpty() ||
                wellbeing.blockNsfwSites ||
                wellbeing.blockedApps.isNotEmpty() // Added support for blocked apps
    }

    /**
     * Performs the back action and shows a toast message indicating that the content is blocked.
     */
    private fun goBackWithToast(customAction: Int? = null) {
        throttler.submit {
            ThreadUtils.runOnMainThread {
                // Perform the back action (can be done on background thread)
                performGlobalAction(customAction ?: GLOBAL_ACTION_BACK)

                // Post Toast to main thread
                Toast.makeText(
                    this@AwayTimeAccessibilityService,
                    getString(R.string.blocked_content_message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Updates the service info with the latest settings and registered packages.
     */
    private fun refreshServiceConfig() {
        try {
            // Using hashset to avoid duplicates
            browserPackages.clear()
            devicePlatformPackages.clear()
            shortsPlatformPackages.clear()
            val pm = packageManager

            // Check admin and add settings to blocked packages
            if (PermissionsHelper.getAndAskAdminPermission(this, false)) {
                devicePlatformPackages.add(SETTINGS_PACKAGE)
            }

            // Fetch installed browser packages
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
            pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL).forEach {
                browserPackages.add(it.activityInfo.packageName)
            }

            // Add blocked apps to appropriate categories
            wellbeing.blockedApps.forEach { packageName ->
                shortsPlatformPackages.add(packageName)
            }

            wellbeing.blockedFeatures.forEach { feature ->
                when (feature) {
                    /// Instagram
                    PlatformFeatures.INSTAGRAM_REELS,
                    PlatformFeatures.INSTAGRAM_EXPLORE,
                        -> shortsPlatformPackages.add(INSTAGRAM_PACKAGE)

                    // Snapchat
                    PlatformFeatures.SNAPCHAT_SPOTLIGHT,
                    PlatformFeatures.SNAPCHAT_DISCOVER,
                        -> shortsPlatformPackages.add(SNAPCHAT_PACKAGE)

                    // Facebook
                    PlatformFeatures.FACEBOOK_REELS ->
                        shortsPlatformPackages.add(FACEBOOK_PACKAGE)

                    // Reddit
                    PlatformFeatures.REDDIT_SHORTS ->
                        shortsPlatformPackages.add(REDDIT_PACKAGE)

                    // Youtube
                    PlatformFeatures.YOUTUBE_SHORTS -> {
                        // Add official package
                        shortsPlatformPackages.add(YOUTUBE_PACKAGE)

                        // Now add other unofficial clients
                        val ytIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                        pm.queryIntentActivities(ytIntent, PackageManager.MATCH_ALL)
                            .filterNot { browserPackages.contains(it.activityInfo.packageName) }
                            .forEach {
                                shortsPlatformPackages.add(it.activityInfo.packageName)
                            }
                    }
                    
                    // TikTok
                    PlatformFeatures.TIKTOK_FOR_YOU ->
                        shortsPlatformPackages.add("com.zhiliaoapp.musically")
                    
                    // Twitter/X
                    PlatformFeatures.TWITTER_TIMELINE ->
                        shortsPlatformPackages.add("com.twitter.android")
                    
                    // General features
                    PlatformFeatures.INFINITE_SCROLL,
                    PlatformFeatures.AUTO_PLAY_VIDEOS -> {
                        // These are handled by the accessibility service logic
                        // No specific package blocking needed
                    }
                }
            }

            // Load nsfw website domains if needed
            if (wellbeing.blockNsfwSites) BrowserManager.initializeNsfwDomains()
            else BrowserManager.clearNsfwDomains()

            Log.d(
                TAG, "refreshServiceConfig: Accessibility service config updated successfully: " +
                        "\n settings: $wellbeing" +
                        "\n device platforms: $devicePlatformPackages" +
                        "\n short platforms: $shortsPlatformPackages" +
                        "\n browsers: $browserPackages"
            )
        } catch (e: Exception) {
            Log.e(TAG, "refreshServiceInfo: Failed to refresh service info", e)
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, changedKey: String?) {
        changedKey?.let { key ->
            if (key == SharedPrefsHelper.PREF_KEY_WELLBEING_SETTINGS) {
                Log.d(TAG, "OnSharedPrefsChanged: Key changed = $changedKey")
                wellbeing = SharedPrefsHelper.getSetWellBeingSettings(this, null)
                refreshServiceConfig()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        synchronized(this) {
            isDestroyed = true
            instance = null
        }
        
        try {
            executorService.shutdownNow()
            trackingManager.startManualTracking()

            // Unregister prefs listener and receiver
            deviceAppsChangedReceiver.unRegister(this)
            SharedPrefsHelper.registerUnregisterListenerToListenablePrefs(this, false, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error during service destruction", e)
        }

        Log.d(TAG, "onDestroy: Accessibility service destroyed")
        super.onDestroy()
    }

    // Legacy methods for compatibility with existing AwayTime code
    fun setBlockedPackages(packages: Set<String>) {
        wellbeing = wellbeing.copy(blockedApps = packages)
        SharedPrefsHelper.getSetWellBeingSettings(this, wellbeing)
        refreshServiceConfig()
        Log.d(TAG, "Updated blocked packages: ${packages.size} apps")
    }

    fun getBlockedPackages(): Set<String> = wellbeing.blockedApps

    fun clearBlockedPackages() {
        wellbeing = wellbeing.copy(blockedApps = emptySet())
        SharedPrefsHelper.getSetWellBeingSettings(this, wellbeing)
        refreshServiceConfig()
        Log.d(TAG, "Cleared all blocked packages")
    }

    fun removeBlockedPackages(packagesToRemove: Set<String>) {
        val currentBlocked = wellbeing.blockedApps
        val newBlocked = currentBlocked - packagesToRemove
        wellbeing = wellbeing.copy(blockedApps = newBlocked)
        SharedPrefsHelper.getSetWellBeingSettings(this, wellbeing)
        refreshServiceConfig()
        Log.d(TAG, "Removed ${packagesToRemove.size} packages from blocked list")
    }
}