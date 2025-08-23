package com.awaytime.app.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AwayTimeFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server for push notifications
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Handle FCM messages here
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            handleNotificationMessage(it)
        }
    }
    
    private fun sendTokenToServer(token: String) {
        // Store token locally for potential server communication
        val prefs = getSharedPreferences("awaytime_firebase", MODE_PRIVATE)
        prefs.edit()
            .putString("fcm_token", token)
            .putLong("token_updated", System.currentTimeMillis())
            .apply()
        
        Log.d(TAG, "FCM token stored locally: $token")
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val message = data["message"]
        
        when (type) {
            "motivation" -> {
                // Show motivational notification
                val notificationService = NotificationService(this)
                notificationService.showMotivationalNotification(
                    message ?: "Keep up the great work! 💜"
                )
            }
            "streak_reminder" -> {
                // Show streak reminder
                val notificationService = NotificationService(this)
                notificationService.showStreakReminder()
            }
            "weekly_review" -> {
                // Show weekly review notification
                val notificationService = NotificationService(this)
                notificationService.showWeeklyReviewNotification()
            }
        }
    }
    
    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        val title = notification.title ?: "Awaytime"
        val body = notification.body ?: ""
        
        val notificationService = NotificationService(this)
        notificationService.showCustomNotification(title, body)
    }
}