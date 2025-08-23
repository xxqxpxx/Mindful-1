package com.awaytime.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.awaytime.app.R

object NotificationHelper {
    private const val FOREGROUND_CHANNEL_ID = "awaytime_foreground_services"
    private const val BLOCKING_CHANNEL_ID = "awaytime_blocking"
    private const val FOCUS_CHANNEL_ID = "awaytime_focus"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // Foreground services channel
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "AwayTime Services",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background services for app blocking and tracking"
                setShowBadge(false)
            }
            
            // Blocking notifications channel
            val blockingChannel = NotificationChannel(
                BLOCKING_CHANNEL_ID,
                "App Blocking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when apps are blocked"
            }
            
            // Focus session channel
            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                "Focus Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Focus session timers and progress"
            }
            
            notificationManager.createNotificationChannels(listOf(
                foregroundChannel,
                blockingChannel, 
                focusChannel
            ))
        }
    }
    
    fun buildForegroundServiceNotification(
        context: Context,
        title: String,
        content: String,
        iconRes: Int = R.drawable.ic_notification
    ): Notification {
        createNotificationChannels(context)
        
        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    fun buildBlockingNotification(
        context: Context,
        appName: String,
        iconRes: Int = R.drawable.ic_notification
    ): Notification {
        createNotificationChannels(context)
        
        return NotificationCompat.Builder(context, BLOCKING_CHANNEL_ID)
            .setContentTitle("App Blocked")
            .setContentText("$appName is blocked. Take a break! 💜")
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
    
    fun buildFocusSessionNotification(
        context: Context,
        title: String,
        content: String,
        timeRemaining: String,
        iconRes: Int = R.drawable.ic_notification
    ): Notification {
        createNotificationChannels(context)
        
        return NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(timeRemaining)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}