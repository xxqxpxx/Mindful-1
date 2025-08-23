package com.awaytime.app.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.awaytime.app.R
import java.util.concurrent.TimeUnit

/**
 * A helper class to manage countdown or count-up timers with notifications.
 * Adapted from Mindful's NotificationTimer for AwayTime focus sessions.
 */
class NotificationTimer(
    private val context: Context,
    private val ongoingPendingIntent: PendingIntent,
    private val finishedPendingIntent: PendingIntent? = null,
    private val title: String,
    private val isFinite: Boolean = true,
    private val timerDurationSeconds: Long,
    private val alreadyElapsedTimeSecond: Long = 0,
    private val notificationId: Int,
    private val notificationChannelId: String,
    private val onTicked: (progress: Int) -> String,
    private val onFinished: () -> String,
    private val onDispose: () -> Unit,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Returns the initial notification object, which can be used to start the service in the foreground.
     */
    val getInitialNotification: Notification get() = notificationBuilder.build()

    private val notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(ongoingPendingIntent)
            .setContentTitle(title)

    private val preciseCountDownExecutor: PreciseCountDownExecutor = PreciseCountDownExecutor(
        duration = timerDurationSeconds,
        interval = 1L,
        timeUnit = TimeUnit.SECONDS,
        onTick = { elapsedSeconds ->
            val totalElapsedSeconds = elapsedSeconds + alreadyElapsedTimeSecond
            val progressSeconds =
                if (isFinite) (timerDurationSeconds - totalElapsedSeconds).toInt()
                else totalElapsedSeconds.toInt()

            // Update the notification with the progress
            val contentText = onTicked.invoke(progressSeconds)
            pushNotification(contentText, progressSeconds)
        },
        onFinish = {
            // When the timer finishes, invoke the onFinished callback and show the final notification
            val contentText = onFinished.invoke()
            showFinishNotification(contentText)
        }
    )

    /**
     * Updates the notification with the current progress.
     */
    private fun pushNotification(contentText: String, progress: Int) {
        notificationBuilder.setContentText(contentText)

        // Set progress if the timer is finite
        if (isFinite) {
            notificationBuilder.setProgress(
                timerDurationSeconds.toInt(),
                progress,
                false
            )
        }

        // Notify the system to update the displayed notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Displays the final notification when the timer finishes or is stopped.
     */
    private fun showFinishNotification(contentText: String) {
        notificationBuilder
            .setContentText(contentText)
            .setOngoing(false)
            .setAutoCancel(true)
            .setProgress(0, 0, false)
            .setContentIntent(finishedPendingIntent ?: ongoingPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

        // Invoke the onDispose callback to handle any cleanup
        onDispose.invoke()

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Start the timer based on provided data.
     */
    fun startTimer() {
        preciseCountDownExecutor.start()
    }

    /**
     * Forces the timer to stop immediately and displays a notification with the reason.
     */
    fun forceDisposeTimer(reason: String) {
        // Cancel the active timer to stop further ticks
        preciseCountDownExecutor.cancel()

        // Show the final notification with the provided reason
        showFinishNotification(reason)
    }
}