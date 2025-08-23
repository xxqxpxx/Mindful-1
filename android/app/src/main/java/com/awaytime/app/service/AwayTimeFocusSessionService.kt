/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.awaytime.app.R
import com.awaytime.app.adapters.WellbeingAdapter
import com.awaytime.app.core.AppConstants
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.helpers.SharedPrefsHelper
import com.awaytime.app.models.FocusSession
import com.awaytime.app.models.NotificationSettings
import com.awaytime.app.utils.NotificationHelper
import com.awaytime.app.utils.NotificationTimer
import com.awaytime.app.utils.ServiceBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

/**
 * Service that manages focus sessions with timers, DND integration, and app blocking.
 * Adapted from Mindful's comprehensive focus session system.
 */
class AwayTimeFocusSessionService : Service() {
    companion object {
        private const val TAG = "AwayTime.FocusSessionService"
        
        @Volatile
        private var instance: AwayTimeFocusSessionService? = null
        
        @Synchronized
        fun getInstance(): AwayTimeFocusSessionService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
        
        const val ACTION_START_FOCUS_SESSION = "com.awaytime.app.action.START_FOCUS_SESSION"
        const val ACTION_UPDATE_FOCUS_SESSION = "com.awaytime.app.action.UPDATE_FOCUS_SESSION"
        const val ACTION_STOP_FOCUS_SESSION = "com.awaytime.app.action.STOP_FOCUS_SESSION"
        
        const val EXTRA_FOCUS_SESSION = "extra_focus_session"
        const val EXTRA_SESSION_SUCCESSFUL = "extra_session_successful"
    }

    private val serviceBinder = ServiceBinder(this@AwayTimeFocusSessionService)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private lateinit var notificationTimer: NotificationTimer
    private lateinit var repository: AwayTimeRepository
    private lateinit var wellbeingAdapter: WellbeingAdapter
    
    private var currentSession: FocusSession? = null

    override fun onCreate() {
        super.onCreate()
        synchronized(this) {
            instance = this
        }
        
        repository = AwayTimeRepository(applicationContext)
        wellbeingAdapter = WellbeingAdapter(applicationContext, repository)
        
        Log.d(TAG, "Focus session service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceBinder.ACTION_START_AWAYTIME_SERVICE -> {
                return START_STICKY
            }
            ACTION_START_FOCUS_SESSION -> {
                val sessionJson = intent.getStringExtra(EXTRA_FOCUS_SESSION)
                if (sessionJson != null) {
                    try {
                        val session = com.google.gson.Gson().fromJson(sessionJson, FocusSession::class.java)
                        startFocusSession(session)
                        return START_STICKY
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse focus session", e)
                    }
                }
            }
            ACTION_UPDATE_FOCUS_SESSION -> {
                val sessionJson = intent.getStringExtra(EXTRA_FOCUS_SESSION)
                if (sessionJson != null) {
                    try {
                        val session = com.google.gson.Gson().fromJson(sessionJson, FocusSession::class.java)
                        updateFocusSession(session)
                        return START_STICKY
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse focus session", e)
                    }
                }
            }
            ACTION_STOP_FOCUS_SESSION -> {
                val isSuccessful = intent.getBooleanExtra(EXTRA_SESSION_SUCCESSFUL, false)
                stopFocusSession(isSuccessful)
            }
        }
        
        stopSelf()
        return START_NOT_STICKY
    }

    /**
     * Starts a countdown timer for a focus session. Configures notifications to show the remaining time
     * and handles DND mode if needed.
     */
    fun startFocusSession(focusSession: FocusSession) {
        try {
            if (focusSession.distractingApps.isEmpty()) {
                Log.w(TAG, "Cannot start focus session without distracting apps")
                stopSelf()
                return
            }

            currentSession = focusSession
            initializeSessionTimer(focusSession)

            // Start foreground service with timer notification
            startForeground(
                AppConstants.FOCUS_SESSION_NOTIFICATION_ID,
                notificationTimer.getInitialNotification
            )

            // Update accessibility service with blocked apps
            wellbeingAdapter.addBlockedApps(focusSession.distractingApps)
            
            // Update VPN service if running
            updateVpnService(focusSession.distractingApps)

            // Enable notification batching during focus session
            if (focusSession.batchNotifications) {
                enableNotificationBatching(focusSession.distractingApps)
            }

            // Toggle DND according to session configuration
            if (focusSession.toggleDnd) {
                toggleDoNotDisturb(true)
            }

            // Start the timer
            notificationTimer.startTimer()

            Log.d(TAG, "Focus session started successfully: ${focusSession.sessionType}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start focus session", e)
            SharedPrefsHelper.insertCrashLogToPrefs(this, e)
            stopSelf()
        }
    }

    private fun initializeSessionTimer(session: FocusSession) {
        val isFiniteSession = session.durationSecs > 0
        val elapsedTimeMs = System.currentTimeMillis() - session.startTimeMsEpoch

        val timerDuration: Long = if (isFiniteSession) {
            session.durationSecs.toLong()
        } else {
            // For infinite sessions, set to 24 hours max
            val cal = Calendar.getInstance()
            cal.add(Calendar.HOUR, 24)
            cal.timeInMillis / 1000L
        }

        // Create pending intents for notifications
        val ongoingIntent = createFocusSessionPendingIntent("ongoing")
        val finishedIntent = createFocusSessionPendingIntent("finished")

        notificationTimer = NotificationTimer(
            context = this,
            ongoingPendingIntent = ongoingIntent,
            finishedPendingIntent = finishedIntent,
            isFinite = isFiniteSession,
            title = getString(R.string.focus_session_active_title),
            timerDurationSeconds = timerDuration,
            alreadyElapsedTimeSecond = max(0, elapsedTimeMs / 1000L),
            notificationId = AppConstants.FOCUS_SESSION_NOTIFICATION_ID,
            notificationChannelId = "awaytime_focus",
            onTicked = { remainingTime ->
                formatTimerText(remainingTime, isFiniteSession, session.sessionType.name)
            },
            onFinished = { 
                getString(R.string.focus_session_completed_message)
            },
            onDispose = { 
                stopSelf() 
            },
        )
    }
    
    private fun formatTimerText(timeSeconds: Int, isFinite: Boolean, sessionType: String): String {
        val hours = timeSeconds / 3600
        val minutes = (timeSeconds % 3600) / 60
        val seconds = timeSeconds % 60
        
        val timeString = when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
        
        return if (isFinite) {
            getString(R.string.focus_session_countdown_format, sessionType, timeString)
        } else {
            getString(R.string.focus_session_countup_format, sessionType, timeString)
        }
    }
    
    private fun createFocusSessionPendingIntent(type: String): PendingIntent {
        val intent = Intent(this, com.awaytime.app.MainActivity::class.java).apply {
            putExtra("focus_session_action", type)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        
        return PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateFocusSession(session: FocusSession) {
        currentSession = session
        
        // Update blocked apps in accessibility service
        wellbeingAdapter.removeBlockedApps(currentSession?.distractingApps ?: emptySet())
        wellbeingAdapter.addBlockedApps(session.distractingApps)
        
        // Update VPN service
        updateVpnService(session.distractingApps)
        
        Log.d(TAG, "Focus session updated: ${session.distractingApps.size} apps")
    }

    fun stopFocusSession(isSuccessful: Boolean) {
        currentSession?.let { session ->
            // Disable DND if it was enabled
            if (session.toggleDnd) {
                toggleDoNotDisturb(false)
            }
            
            // Remove blocked apps
            wellbeingAdapter.removeBlockedApps(session.distractingApps)
            
            // Update VPN service
            updateVpnService(emptySet())
            
            // Disable notification batching
            if (session.batchNotifications) {
                disableNotificationBatching()
            }
            
            // Save session to database
            saveFocusSessionResult(session, isSuccessful)
        }

        // Stop the timer with appropriate message
        val message = if (isSuccessful) {
            getString(R.string.focus_session_completed_message)
        } else {
            getString(R.string.focus_session_cancelled_message)
        }
        
        if (::notificationTimer.isInitialized) {
            notificationTimer.forceDisposeTimer(message)
        }
        
        Log.d(TAG, "Focus session stopped: successful=$isSuccessful")
    }
    
    private fun updateVpnService(blockedApps: Set<String>) {
        val vpnService = AwayTimeVpnService.getInstance()
        if (vpnService != null) {
            vpnService.updateBlockedApps(blockedApps)
        } else if (blockedApps.isNotEmpty()) {
            // Start VPN service if needed
            val intent = Intent(this, AwayTimeVpnService::class.java).apply {
                action = ServiceBinder.ACTION_START_AWAYTIME_SERVICE
                putExtra("blocked_apps", blockedApps.toTypedArray())
            }
            startService(intent)
        }
    }
    
    private fun enableNotificationBatching(distractingApps: Set<String>) {
        val notificationService = AwayTimeNotificationListenerService.getInstance()
        if (notificationService != null) {
            val currentSettings = NotificationSettings(
                batchedApps = distractingApps,
                batchingEnabled = true,
                autoBatchDuringFocus = true
            )
            notificationService.updateNotificationSettings(currentSettings)
        }
    }
    
    private fun disableNotificationBatching() {
        val notificationService = AwayTimeNotificationListenerService.getInstance()
        if (notificationService != null) {
            // Deliver any pending notifications
            notificationService.deliverBatchedNotifications()
            
            // Disable batching
            val currentSettings = NotificationSettings(
                batchedApps = emptySet(),
                batchingEnabled = false
            )
            notificationService.updateNotificationSettings(currentSettings)
        }
    }
    
    private fun toggleDoNotDisturb(enable: Boolean) {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val interruptionFilter = if (enable) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(interruptionFilter)
                Log.d(TAG, "DND toggled: $enable")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle DND", e)
        }
    }
    
    private fun saveFocusSessionResult(session: FocusSession, isSuccessful: Boolean) {
        coroutineScope.launch {
            try {
                // TODO: Save focus session result to database
                // repository.saveFocusSessionResult(session, isSuccessful)
                Log.d(TAG, "Focus session result saved: successful=$isSuccessful")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save focus session result", e)
            }
        }
    }

    override fun onDestroy() {
        synchronized(this) {
            instance = null
        }
        
        // Clean up current session
        currentSession?.let { session ->
            wellbeingAdapter.removeBlockedApps(session.distractingApps)
            updateVpnService(emptySet())
            
            if (session.toggleDnd) {
                toggleDoNotDisturb(false)
            }
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Focus session service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == ServiceBinder.ACTION_BIND_TO_AWAYTIME) {
            serviceBinder
        } else null
    }
}