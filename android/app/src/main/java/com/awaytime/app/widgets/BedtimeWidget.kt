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
import com.awaytime.app.service.BedtimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bedtime widget showing sleep schedule and controls
 */
class BedtimeWidget : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "BedtimeWidget"
        const val ACTION_TOGGLE_BEDTIME = "com.awaytime.widget.TOGGLE_BEDTIME"
        const val ACTION_EMERGENCY_OVERRIDE = "com.awaytime.widget.EMERGENCY_OVERRIDE"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Updating ${appWidgetIds.size} bedtime widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        
        scope.launch {
            try {
                widgetManager.updateAllWidgetData()
                
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, appWidgetManager, widgetId, widgetManager)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update bedtime widgets", e)
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
            val views = RemoteViews(context.packageName, R.layout.widget_bedtime)
            val data = widgetManager.bedtimeData.value
            
            if (data != null) {
                // Update status text
                views.setTextViewText(R.id.tv_bedtime_status, data.getStatusText())
                
                // Update bedtime and wakeup times
                if (data.nextBedtime != null && data.nextWakeup != null) {
                    views.setTextViewText(R.id.tv_bedtime, formatTime(data.nextBedtime))
                    views.setTextViewText(R.id.tv_wakeup, formatTime(data.nextWakeup))
                    views.setViewVisibility(R.id.layout_times, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.layout_times, android.view.View.GONE)
                }
                
                // Update status indicator
                val statusColor = when {
                    data.isActive -> Color.parseColor("#3F51B5") // Blue
                    data.isWindDown -> Color.parseColor("#FF9800") // Orange
                    data.isEnabled -> Color.parseColor("#4CAF50") // Green
                    else -> Color.GRAY
                }
                views.setInt(R.id.indicator_status, "setBackgroundColor", statusColor)
                
                // Update action button
                when {
                    data.isActive -> {
                        views.setTextViewText(R.id.btn_action, "Emergency Override")
                        views.setInt(R.id.btn_action, "setBackgroundColor", Color.parseColor("#F44336"))
                        
                        val overrideIntent = Intent(context, BedtimeWidget::class.java).apply {
                            action = ACTION_EMERGENCY_OVERRIDE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        val overridePendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId * 2 + 1,
                            overrideIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_action, overridePendingIntent)
                    }
                    data.isWindDown -> {
                        views.setTextViewText(R.id.btn_action, "Sleep Now")
                        views.setInt(R.id.btn_action, "setBackgroundColor", Color.parseColor("#3F51B5"))
                        
                        val sleepIntent = Intent(context, BedtimeWidget::class.java).apply {
                            action = ACTION_TOGGLE_BEDTIME
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        val sleepPendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId * 2,
                            sleepIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_action, sleepPendingIntent)
                    }
                    data.isEnabled -> {
                        views.setTextViewText(R.id.btn_action, "Configure")
                        views.setInt(R.id.btn_action, "setBackgroundColor", Color.parseColor("#4CAF50"))
                        
                        // Set click to open bedtime settings
                        val configIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("navigate_to", "bedtime")
                        }
                        val configPendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId * 2,
                            configIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_action, configPendingIntent)
                    }
                    else -> {
                        views.setTextViewText(R.id.btn_action, "Enable Bedtime")
                        views.setInt(R.id.btn_action, "setBackgroundColor", Color.GRAY)
                        
                        val enableIntent = Intent(context, MainActivity::class.java).apply {
                            putExtra("navigate_to", "bedtime")
                        }
                        val enablePendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId * 2,
                            enableIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.btn_action, enablePendingIntent)
                    }
                }
                
                // Update last updated time
                val timeAgo = getTimeAgo(data.lastUpdated)
                views.setTextViewText(R.id.tv_last_updated, "Updated $timeAgo")
                
            } else {
                // Loading state
                views.setTextViewText(R.id.tv_bedtime_status, "Loading...")
                views.setViewVisibility(R.id.layout_times, android.view.View.GONE)
                views.setInt(R.id.indicator_status, "setBackgroundColor", Color.GRAY)
                views.setTextViewText(R.id.btn_action, "Loading...")
                views.setTextViewText(R.id.tv_last_updated, "")
            }
            
            // Set container click to open bedtime settings
            val appIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "bedtime")
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)
            
            appWidgetManager.updateAppWidget(widgetId, views)
            Log.d(TAG, "Updated bedtime widget $widgetId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update bedtime widget $widgetId", e)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_BEDTIME -> {
                Log.d(TAG, "Toggling bedtime mode from widget")
                toggleBedtimeMode(context)
            }
            ACTION_EMERGENCY_OVERRIDE -> {
                Log.d(TAG, "Emergency override from widget")
                emergencyOverride(context)
            }
        }
    }
    
    private fun toggleBedtimeMode(context: Context) {
        scope.launch {
            try {
                val bedtimeManager = BedtimeManager.getInstance(context)
                val currentState = bedtimeManager.bedtimeState.value
                
                if (currentState is com.awaytime.app.models.BedtimeState.Active) {
                    bedtimeManager.deactivateBedtimeMode()
                } else {
                    bedtimeManager.activateBedtimeMode()
                }
                
                // Update widget
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.BEDTIME_STATUS)
                
                Log.d(TAG, "Bedtime mode toggled from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle bedtime mode from widget", e)
            }
        }
    }
    
    private fun emergencyOverride(context: Context) {
        scope.launch {
            try {
                val bedtimeManager = BedtimeManager.getInstance(context)
                bedtimeManager.emergencyOverride()
                
                // Update widget
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.BEDTIME_STATUS)
                
                Log.d(TAG, "Emergency override activated from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate emergency override from widget", e)
            }
        }
    }
    
    private fun formatTime(time: java.time.LocalTime): String {
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val period = if (time.hour < 12) "AM" else "PM"
        return "${hour}:${time.minute.toString().padStart(2, '0')} $period"
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
        Log.d(TAG, "Bedtime widget enabled")
        WidgetManager.getInstance(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Bedtime widget disabled")
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Deleted ${appWidgetIds.size} bedtime widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        appWidgetIds.forEach { widgetId ->
            widgetManager.removeWidgetConfiguration(widgetId.toString())
        }
    }
}