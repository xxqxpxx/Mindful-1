package com.awaytime.app.models

import android.service.notification.StatusBarNotification
import android.app.Notification as AndroidNotification

/**
 * Represents a captured notification for batching and history.
 * Adapted from Mindful's notification model.
 */
data class AwayTimeNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isBatched: Boolean = false
) {
    companion object {
        fun fromStatusBarNotification(sbn: StatusBarNotification): AwayTimeNotification {
            val notification = sbn.notification
            return AwayTimeNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = sbn.packageName, // TODO: Get actual app name
                title = notification.extras.getString(AndroidNotification.EXTRA_TITLE) ?: "",
                content = notification.extras.getString(AndroidNotification.EXTRA_TEXT) ?: "",
                timestamp = sbn.postTime,
                isRead = false,
                isBatched = true
            )
        }
    }
}