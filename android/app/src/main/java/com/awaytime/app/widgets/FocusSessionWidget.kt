/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.awaytime.app.MainActivity
import com.awaytime.app.R
import com.awaytime.app.service.WidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Focus session widget for quick focus controls
 */
class FocusSessionWidget : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "FocusSessionWidget"
        const val ACTION_START_FOCUS = "com.awaytime.widget.START_FOCUS"
        const val ACTION_STOP_FOCUS = "com.awaytime.widget.STOP_FOCUS"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Updating ${appWidgetIds.size} focus session widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        
        scope.launch {
            try {
                widgetManager.updateAllWidgetData()
                
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, appWidgetManager, widgetId, widgetManager)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update focus session widgets", e)
            }
        }
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        widgetManager: WidgetManager
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_focus_session)
            val data = widgetManager.focusSessionData.value
            
            if (data != null) {
                if (data.isActive) {
                    // Active session UI
                    views.setTextViewText(R.id.tv_session_status, "Focus Session Active")
                    views.setTextViewText(R.id.tv_session_type, data.sessionType)
                    views.setTextViewText(R.id.tv_time_remaining, data.getTimeRemainingText())
                    
                    // Update progress
                    val progressPercentage = (data.progress * 100).toInt()
                    views.setProgressBar(R.id.progress_session, 100, progressPercentage, false)
                    views.setViewVisibility(R.id.progress_session, android.view.View.VISIBLE)
                    
                    // Show stop button
                    views.setTextViewText(R.id.btn_action, "Stop Session")
                    views.setInt(R.id.btn_action, "setBackgroundColor", Color.parseColor("#F44336"))
                    
                    // Set stop action
                    val stopIntent = Intent(context, FocusSessionWidget::class.java).apply {
                        action = ACTION_STOP_FOCUS
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    val stopPendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId * 2 + 1,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_action, stopPendingIntent)
                    
                } else {
                    // Inactive session UI
                    views.setTextViewText(R.id.tv_session_status, "Ready for Focus")
                    views.setTextViewText(R.id.tv_session_type, "25 min Pomodoro")
                    views.setTextViewText(R.id.tv_time_remaining, "Tap to start")
                    
                    // Hide progress
                    views.setViewVisibility(R.id.progress_session, android.view.View.GONE)
                    
                    // Show start button
                    views.setTextViewText(R.id.btn_action, "Start Focus")
                    views.setInt(R.id.btn_action, "setBackgroundColor", Color.parseColor("#4CAF50"))
                    
                    // Set start action
                    val startIntent = Intent(context, FocusSessionWidget::class.java).apply {
                        action = ACTION_START_FOCUS
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    val startPendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId * 2,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_action, startPendingIntent)
                }
                
                // Update last updated time
                val timeAgo = getTimeAgo(data.lastUpdated)
                views.setTextViewText(R.id.tv_last_updated, "Updated $timeAgo")
                
            } else {
                // Loading state
                views.setTextViewText(R.id.tv_session_status, "Loading...")
                views.setTextViewText(R.id.tv_session_type, "")
                views.setTextViewText(R.id.tv_time_remaining, "")
                views.setViewVisibility(R.id.progress_session, android.view.View.GONE)
                views.setTextViewText(R.id.btn_action, "Loading...")
                views.setTextViewText(R.id.tv_last_updated, "")
            }
            
            // Set container click to open app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)
            
            appWidgetManager.updateAppWidget(widgetId, views)
            Log.d(TAG, "Updated focus session widget $widgetId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update focus session widget $widgetId", e)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_START_FOCUS -> {
                Log.d(TAG, "Starting focus session from widget")
                startFocusSession(context)
            }
            ACTION_STOP_FOCUS -> {
                Log.d(TAG, "Stopping focus session from widget")
                stopFocusSession(context)
            }
        }
    }
    
    private fun startFocusSession(context: Context) {
        scope.launch {
            try {
                // Start focus session through enhanced service manager
                val enhancedServiceManager = com.awaytime.app.service.EnhancedServiceManager.getInstance(
                    context, 
                    com.awaytime.app.data.repository.AwayTimeRepository(context)
                )
                
                // This would ideally call a method to start focus session
                // For now, we'll just update the widget
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.FOCUS_SESSION)
                
                Log.d(TAG, "Focus session started from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start focus session from widget", e)
            }
        }
    }
    
    private fun stopFocusSession(context: Context) {
        scope.launch {
            try {
                // Stop focus session through enhanced service manager
                val enhancedServiceManager = com.awaytime.app.service.EnhancedServiceManager.getInstance(
                    context,
                    com.awaytime.app.data.repository.AwayTimeRepository(context)
                )
                
                // This would ideally call a method to stop focus session
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.FOCUS_SESSION)
                
                Log.d(TAG, "Focus session stopped from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop focus session from widget", e)
            }
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMinutes = (now - timestamp) / (1000 * 60)
        
        return when {
            diffMinutes < 1 -> "just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Focus session widget enabled")
        WidgetManager.getInstance(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Focus session widget disabled")
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Deleted ${appWidgetIds.size} focus session widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        appWidgetIds.forEach { widgetId ->
            widgetManager.removeWidgetConfiguration(widgetId.toString())
        }
    }
}