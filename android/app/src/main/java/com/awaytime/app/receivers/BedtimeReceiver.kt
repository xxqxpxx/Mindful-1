/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awaytime.app.service.BedtimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for handling bedtime-related alarms
 */
class BedtimeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BedtimeReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        Log.d(TAG, "Received bedtime action: $action")

        val bedtimeManager = BedtimeManager.getInstance(context)

        scope.launch {
            try {
                when (action) {
                    "START" -> {
                        Log.d(TAG, "Starting bedtime mode")
                        bedtimeManager.activateBedtimeMode()
                        showBedtimeNotification(context, "Bedtime mode activated")
                    }
                    
                    "END" -> {
                        Log.d(TAG, "Ending bedtime mode")
                        bedtimeManager.deactivateBedtimeMode()
                        showBedtimeNotification(context, "Good morning! Bedtime mode ended")
                    }
                    
                    "WIND_DOWN" -> {
                        Log.d(TAG, "Starting wind down")
                        bedtimeManager.activateWindDown()
                        showBedtimeNotification(context, "Wind down time - prepare for bedtime")
                    }
                    
                    "RE_ENABLE" -> {
                        Log.d(TAG, "Re-enabling bedtime after emergency override")
                        // Bedtime manager will handle re-enabling based on current time
                        showBedtimeNotification(context, "Bedtime mode re-enabled")
                    }
                    
                    else -> {
                        Log.w(TAG, "Unknown bedtime action: $action")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling bedtime action: $action", e)
            }
        }
    }

    private fun showBedtimeNotification(context: Context, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            
            // Create notification channel if needed (Android 8.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "bedtime_channel",
                    "Bedtime Mode",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for bedtime mode changes"
                    setSound(null, null) // Silent for bedtime
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Build notification
            val notification = android.app.Notification.Builder(context).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setChannelId("bedtime_channel")
                }
                setContentTitle("AwayTime Bedtime")
                setContentText(message)
                setSmallIcon(android.R.drawable.ic_lock_lock)
                setAutoCancel(true)
                setDefaults(0) // No sound/vibration for bedtime
            }.build()
            
            notificationManager.notify(1001, notification)
            Log.d(TAG, "Bedtime notification shown: $message")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show bedtime notification", e)
        }
    }
}