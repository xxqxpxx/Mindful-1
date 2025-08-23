package com.awaytime.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.awaytime.app.helpers.PermissionsHelper
import kotlinx.coroutines.*

/**
 * Monitors the accessibility service state and provides notifications when it's disabled
 */
class AccessibilityServiceMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "AccessibilityMonitor"
        private const val CHECK_INTERVAL_MS = 5000L // Check every 5 seconds
        
        @Volatile
        private var instance: AccessibilityServiceMonitor? = null
        
        fun getInstance(context: Context): AccessibilityServiceMonitor {
            return instance ?: synchronized(this) {
                instance ?: AccessibilityServiceMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val _isServiceEnabled = MutableLiveData<Boolean>()
    val isServiceEnabled: LiveData<Boolean> = _isServiceEnabled
    
    private val _serviceStateMessage = MutableLiveData<String>()
    val serviceStateMessage: LiveData<String> = _serviceStateMessage
    
    private var monitoringJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Broadcast receiver for service state changes
    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.awaytime.app.ACCESSIBILITY_SERVICE_STATE_CHANGED" -> {
                    val isConnected = intent.getBooleanExtra("is_connected", false)
                    val blockedCount = intent.getIntExtra("blocked_packages_count", 0)
                    
                    _isServiceEnabled.postValue(isConnected)
                    
                    val message = if (isConnected) {
                        "Accessibility service active - $blockedCount apps blocked"
                    } else {
                        "Accessibility service disconnected"
                    }
                    _serviceStateMessage.postValue(message)
                    
                    Log.d(TAG, "Service state changed: $message")
                }
            }
        }
    }
    
    // Callback for when service needs to be re-enabled
    var onServiceDisabled: (() -> Unit)? = null
    var onServiceEnabled: (() -> Unit)? = null
    
    fun startMonitoring() {
        Log.d(TAG, "Starting accessibility service monitoring")
        
        // Register broadcast receiver
        val filter = IntentFilter("com.awaytime.app.ACCESSIBILITY_SERVICE_STATE_CHANGED")
        context.registerReceiver(serviceStateReceiver, filter)
        
        // Start periodic monitoring
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkServiceState()
                delay(CHECK_INTERVAL_MS)
            }
        }
        
        // Initial check
        checkServiceState()
    }
    
    fun stopMonitoring() {
        Log.d(TAG, "Stopping accessibility service monitoring")
        
        try {
            context.unregisterReceiver(serviceStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister receiver", e)
        }
        
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    private fun checkServiceState() {
        val isSystemEnabled = PermissionsHelper.getAndAskAccessibilityPermission(context, false)
        val isInstanceRunning = AwayTimeAccessibilityService.isServiceRunning()
        val isFullyEnabled = isSystemEnabled && isInstanceRunning
        
        handler.post {
            val previousState = _isServiceEnabled.value ?: false
            _isServiceEnabled.value = isFullyEnabled
            
            val message = when {
                !isSystemEnabled -> "Accessibility service disabled in system settings"
                !isInstanceRunning -> "Accessibility service not running"
                isFullyEnabled -> "Accessibility service active"
                else -> "Accessibility service in unknown state"
            }
            _serviceStateMessage.value = message

            if (!isFullyEnabled) {
                val notificationService = NotificationService(context)
                notificationService.sendAccessibilityServiceDisabledNotification()
            } else {
                val notificationService = NotificationService(context)
                notificationService.dismissAccessibilityServiceNotification()
            }
            
            // Trigger callbacks on state changes
            if (previousState != isFullyEnabled) {
                if (isFullyEnabled) {
                    onServiceEnabled?.invoke()
                    Log.d(TAG, "Service enabled callback triggered")
                } else {
                    onServiceDisabled?.invoke()
                    Log.d(TAG, "Service disabled callback triggered")
                }
            }
        }
        
        Log.d(TAG, "Service state check - System: $isSystemEnabled, Instance: $isInstanceRunning")
    }
    
    fun isServiceCurrentlyEnabled(): Boolean {
        return PermissionsHelper.getAndAskAccessibilityPermission(context, false) && 
               AwayTimeAccessibilityService.isServiceRunning()
    }
    
    fun getServiceStatusMessage(): String {
        return when {
            !PermissionsHelper.getAndAskAccessibilityPermission(context, false) -> 
                "Please enable the accessibility service in Settings to block apps"
            !AwayTimeAccessibilityService.isServiceRunning() -> 
                "Accessibility service is enabled but not running - please restart the app"
            else -> 
                "Accessibility service is active and ready"
        }
    }
    
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }
    
    // Force a manual check (useful for UI refresh)
    fun forceCheck() {
        checkServiceState()
    }
}