/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.awaytime.app.adapters.WellbeingAdapter
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.helpers.SharedPrefsHelper
import com.awaytime.app.models.AwayTimeNotification
import com.awaytime.app.models.NotificationSettings
import com.awaytime.app.utils.ServiceBinder
import com.awaytime.app.utils.SmartCacheBox
import com.awaytime.app.utils.Throttler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Service that listens to notifications and manages batching for distracting apps.
 * Adapted from Mindful's sophisticated notification management system.
 */
class AwayTimeNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "AwayTime.NotificationListener"
        
        @Volatile
        private var instance: AwayTimeNotificationListenerService? = null
        
        @Synchronized
        fun getInstance(): AwayTimeNotificationListenerService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }

    private val binder = ServiceBinder(this@AwayTimeNotificationListenerService)
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val pendingNotifications: MutableList<AwayTimeNotification> = mutableListOf()
    private val cachedPendingIntents: SmartCacheBox<String, PendingIntent> = SmartCacheBox(
        maxSize = 100,
        maxAgeMs = 24 * 60 * 60 * 1000L // 24 hours
    )

    private val throttler: Throttler = Throttler(5 * 1000L) // Every 5 seconds
    private var settings: NotificationSettings = NotificationSettings()
    private var isListenerActive = false
    
    private lateinit var repository: AwayTimeRepository
    private lateinit var wellbeingAdapter: WellbeingAdapter

    override fun onCreate() {
        super.onCreate()
        synchronized(this) {
            instance = this
        }
        
        // Initialize repository and adapter
        repository = AwayTimeRepository(applicationContext)
        wellbeingAdapter = WellbeingAdapter(applicationContext, repository)
        
        // Load notification settings
        loadNotificationSettings()
        
        Log.d(TAG, "Notification listener service created")
    }

    /**
     * Returns the pending intent for the provided key if found, otherwise null
     */
    fun getPendingIntentForKey(key: String): PendingIntent? = cachedPendingIntents.get(key)

    /**
     * Gets all pending notifications that have been batched
     */
    fun getPendingNotifications(): List<AwayTimeNotification> = pendingNotifications.toList()

    /**
     * Delivers all batched notifications immediately
     */
    fun deliverBatchedNotifications() {
        executorService.submit {
            try {
                Log.d(TAG, "Delivering ${pendingNotifications.size} batched notifications")
                
                // For now, just clear the pending notifications
                // In a full implementation, we would re-post them or show a summary
                pendingNotifications.clear()
                
                Log.d(TAG, "Batched notifications delivered")
            } catch (e: Exception) {
                Log.e(TAG, "Error delivering batched notifications", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeNotificationListenerService, e)
            }
        }
    }

    override fun onListenerConnected() {
        isListenerActive = true
        Log.d(TAG, "Notification listener CONNECTED")
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        isListenerActive = false
        Log.d(TAG, "Notification listener DISCONNECTED")

        super.onListenerDisconnected()
        // Try to rebind again
        runCatching {
            val listener = ComponentName(this, AwayTimeNotificationListenerService::class.java)
            requestRebind(listener)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isListenerActive) return
        
        val packageName = sbn.packageName
        try {
            // Skip system notifications, our own notifications, non-clearable, group summaries, ongoing
            val isGroupSummary = sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0
            if (packageName == this.packageName || !sbn.isClearable || isGroupSummary || sbn.isOngoing) {
                return
            }

            // Check if this app's notifications should be batched
            val shouldBatch = shouldBatchNotification(packageName)
            
            if (shouldBatch) {
                // Cancel the notification immediately
                cancelNotification(sbn.key)
                Log.d(TAG, "Batched notification from $packageName")
            }

            // Process notification if batching is enabled or we're storing all notifications
            if (settings.batchingEnabled && (shouldBatch || settings.storeNonBatchedToo)) {
                executorService.submit {
                    processNotificationInBackground(sbn, shouldBatch)
                }
            }
        } catch (e: Exception) {
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
            Log.e(TAG, "Error processing notification from $packageName", e)
        }
        super.onNotificationPosted(sbn)
    }

    private fun shouldBatchNotification(packageName: String): Boolean {
        // Check if app is in batched apps list
        if (settings.batchedApps.contains(packageName)) {
            return true
        }
        
        // Check if auto-batching during focus sessions is enabled
        if (settings.autoBatchDuringFocus) {
            // TODO: Check if we're currently in a focus session
            // For now, check if the app is blocked by the wellbeing settings
            val wellbeing = wellbeingAdapter.getCurrentWellbeing()
            if (wellbeing.blockedApps.contains(packageName)) {
                return true
            }
        }
        
        return false
    }

    private fun processNotificationInBackground(sbn: StatusBarNotification, isBatched: Boolean) {
        try {
            Log.d(TAG, "Processing notification from ${sbn.packageName}")

            // Create notification object
            val notification = AwayTimeNotification.fromStatusBarNotification(sbn).copy(
                isRead = !isBatched,
                isBatched = isBatched
            )
            
            // Skip if title or content is empty
            if (notification.title.isEmpty() || notification.content.isEmpty()) return

            // Cache the pending intent for later access
            sbn.notification.contentIntent?.let { 
                cachedPendingIntents.put(notification.key, it) 
            }
            
            // Add to pending notifications if batched
            if (isBatched) {
                synchronized(pendingNotifications) {
                    pendingNotifications.add(notification)
                }
            }

            // Save to database with throttling
            throttler.submit { saveNotificationsToDatabase() }
        } catch (e: Exception) {
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
            Log.e(TAG, "Failed to process notification in background", e)
        }
    }

    private fun saveNotificationsToDatabase() {
        if (pendingNotifications.isEmpty()) return
        
        coroutineScope.launch {
            try {
                // TODO: Save notifications to our database
                // For now, just log the count
                Log.d(TAG, "Would save ${pendingNotifications.size} notifications to database")
                
                // In a full implementation, we would save to our Room database
                // repository.saveNotifications(pendingNotifications)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notifications to database", e)
                SharedPrefsHelper.insertCrashLogToPrefs(this@AwayTimeNotificationListenerService, e)
            }
        }
    }

    fun updateNotificationSettings(newSettings: NotificationSettings) {
        settings = newSettings
        saveNotificationSettings()
        Log.d(TAG, "Notification settings updated: $settings")
    }
    
    private fun loadNotificationSettings() {
        try {
            val prefs = getSharedPreferences("notification_settings", MODE_PRIVATE)
            val batchedAppsString = prefs.getString("batched_apps", "") ?: ""
            val batchedApps = if (batchedAppsString.isNotEmpty()) {
                batchedAppsString.split(",").toSet()
            } else {
                emptySet()
            }
            
            settings = NotificationSettings(
                batchedApps = batchedApps,
                batchingEnabled = prefs.getBoolean("batching_enabled", false),
                storeNonBatchedToo = prefs.getBoolean("store_non_batched", false),
                showSummaries = prefs.getBoolean("show_summaries", true),
                batchDeliveryIntervalMinutes = prefs.getInt("batch_delivery_interval", 60),
                autoBatchDuringFocus = prefs.getBoolean("auto_batch_during_focus", true)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notification settings", e)
        }
    }
    
    private fun saveNotificationSettings() {
        try {
            val prefs = getSharedPreferences("notification_settings", MODE_PRIVATE)
            prefs.edit()
                .putString("batched_apps", settings.batchedApps.joinToString(","))
                .putBoolean("batching_enabled", settings.batchingEnabled)
                .putBoolean("store_non_batched", settings.storeNonBatchedToo)
                .putBoolean("show_summaries", settings.showSummaries)
                .putInt("batch_delivery_interval", settings.batchDeliveryIntervalMinutes)
                .putBoolean("auto_batch_during_focus", settings.autoBatchDuringFocus)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification settings", e)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == ServiceBinder.ACTION_BIND_TO_AWAYTIME) {
            binder
        } else {
            super.onBind(intent)
        }
    }

    override fun onDestroy() {
        synchronized(this) {
            instance = null
        }
        
        saveNotificationsToDatabase()
        executorService.shutdown()
        Log.d(TAG, "Notification listener service destroyed")
        super.onDestroy()
    }
}