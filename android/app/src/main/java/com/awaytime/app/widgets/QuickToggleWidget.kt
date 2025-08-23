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
 * Quick toggle widget for one-tap app blocking controls
 */
class QuickToggleWidget : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "QuickToggleWidget"
        const val ACTION_TOGGLE_BLOCKING = "com.awaytime.widget.TOGGLE_BLOCKING"
        const val ACTION_TOGGLE_VPN = "com.awaytime.widget.TOGGLE_VPN"
        const val ACTION_QUICK_FOCUS = "com.awaytime.widget.QUICK_FOCUS"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Updating ${appWidgetIds.size} quick toggle widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        
        scope.launch {
            try {
                widgetManager.updateAllWidgetData()
                
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, appWidgetManager, widgetId, widgetManager)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quick toggle widgets", e)
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
            val views = RemoteViews(context.packageName, R.layout.widget_quick_toggle)
            val data = widgetManager.quickToggleData.value
            
            if (data != null) {
                // Update main status
                views.setTextViewText(R.id.tv_main_status, data.getStatusText())
                
                // Update blocking toggle
                updateToggleButton(
                    views,
                    R.id.btn_toggle_blocking,
                    R.id.tv_blocking_label,
                    "App Blocking",
                    data.isBlockingEnabled,
                    context,
                    widgetId,
                    ACTION_TOGGLE_BLOCKING
                )
                
                // Update VPN toggle
                updateToggleButton(
                    views,
                    R.id.btn_toggle_vpn,
                    R.id.tv_vpn_label,
                    "Internet Block",
                    data.vpnActive,
                    context,
                    widgetId,
                    ACTION_TOGGLE_VPN
                )
                
                // Update focus button
                val focusColor = if (data.focusActive) {
                    Color.parseColor("#4CAF50")
                } else {
                    Color.parseColor("#2196F3")
                }
                views.setInt(R.id.btn_quick_focus, "setBackgroundColor", focusColor)
                views.setTextViewText(
                    R.id.tv_focus_label,
                    if (data.focusActive) "Stop Focus" else "Quick Focus"
                )
                
                val focusIntent = Intent(context, QuickToggleWidget::class.java).apply {
                    action = ACTION_QUICK_FOCUS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                val focusPendingIntent = PendingIntent.getBroadcast(
                    context,
                    widgetId * 10 + 3,
                    focusIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_quick_focus, focusPendingIntent)
                
                // Update service indicators
                updateServiceIndicator(views, R.id.indicator_accessibility, data.isBlockingEnabled)
                updateServiceIndicator(views, R.id.indicator_vpn, data.vpnActive)
                updateServiceIndicator(views, R.id.indicator_focus, data.focusActive)
                
                // Update last updated time
                val timeAgo = getTimeAgo(data.lastUpdated)
                views.setTextViewText(R.id.tv_last_updated, "Updated $timeAgo")
                
            } else {
                // Loading state
                views.setTextViewText(R.id.tv_main_status, "Loading...")
                views.setTextViewText(R.id.tv_blocking_label, "Loading...")
                views.setTextViewText(R.id.tv_vpn_label, "Loading...")
                views.setTextViewText(R.id.tv_focus_label, "Loading...")
                views.setTextViewText(R.id.tv_last_updated, "")
                
                // Set loading colors
                views.setInt(R.id.btn_toggle_blocking, "setBackgroundColor", Color.GRAY)
                views.setInt(R.id.btn_toggle_vpn, "setBackgroundColor", Color.GRAY)
                views.setInt(R.id.btn_quick_focus, "setBackgroundColor", Color.GRAY)
                
                updateServiceIndicator(views, R.id.indicator_accessibility, false)
                updateServiceIndicator(views, R.id.indicator_vpn, false)
                updateServiceIndicator(views, R.id.indicator_focus, false)
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
            Log.d(TAG, "Updated quick toggle widget $widgetId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update quick toggle widget $widgetId", e)
        }
    }
    
    private fun updateToggleButton(
        views: RemoteViews,
        buttonId: Int,
        labelId: Int,
        labelText: String,
        isActive: Boolean,
        context: Context,
        widgetId: Int,
        action: String
    ) {
        val color = if (isActive) {
            Color.parseColor("#4CAF50") // Green
        } else {
            Color.parseColor("#757575") // Gray
        }
        
        views.setInt(buttonId, "setBackgroundColor", color)
        views.setTextViewText(labelId, if (isActive) "$labelText ON" else "$labelText OFF")
        
        val intent = Intent(context, QuickToggleWidget::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 10 + when (action) {
                ACTION_TOGGLE_BLOCKING -> 1
                ACTION_TOGGLE_VPN -> 2
                else -> 0
            },
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(buttonId, pendingIntent)
    }
    
    private fun updateServiceIndicator(views: RemoteViews, indicatorId: Int, isActive: Boolean) {
        val color = if (isActive) {
            Color.parseColor("#4CAF50") // Green
        } else {
            Color.parseColor("#F44336") // Red
        }
        views.setInt(indicatorId, "setBackgroundColor", color)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_BLOCKING -> {
                Log.d(TAG, "Toggling app blocking from widget")
                toggleAppBlocking(context)
            }
            ACTION_TOGGLE_VPN -> {
                Log.d(TAG, "Toggling VPN from widget")
                toggleVpn(context)
            }
            ACTION_QUICK_FOCUS -> {
                Log.d(TAG, "Quick focus from widget")
                toggleQuickFocus(context)
            }
        }
    }
    
    private fun toggleAppBlocking(context: Context) {
        scope.launch {
            try {
                val enhancedServiceManager = com.awaytime.app.service.EnhancedServiceManager.getInstance(
                    context,
                    com.awaytime.app.data.repository.AwayTimeRepository(context)
                )
                
                // This would toggle the app blocking service
                // Implementation would depend on current state
                
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.QUICK_TOGGLE)
                
                Log.d(TAG, "App blocking toggled from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle app blocking from widget", e)
            }
        }
    }
    
    private fun toggleVpn(context: Context) {
        scope.launch {
            try {
                val enhancedServiceManager = com.awaytime.app.service.EnhancedServiceManager.getInstance(
                    context,
                    com.awaytime.app.data.repository.AwayTimeRepository(context)
                )
                
                // This would toggle the VPN service
                val status = enhancedServiceManager.getServiceStatus()
                if (status.vpnServiceRunning) {
                    enhancedServiceManager.stopInternetBlocking()
                } else {
                    enhancedServiceManager.startInternetBlocking(emptySet())
                }
                
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.QUICK_TOGGLE)
                
                Log.d(TAG, "VPN toggled from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle VPN from widget", e)
            }
        }
    }
    
    private fun toggleQuickFocus(context: Context) {
        scope.launch {
            try {
                // This would start/stop a quick 25-minute focus session
                // Implementation would depend on focus session service
                
                val widgetManager = WidgetManager.getInstance(context)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.QUICK_TOGGLE)
                widgetManager.updateWidget(com.awaytime.app.models.WidgetType.FOCUS_SESSION)
                
                Log.d(TAG, "Quick focus toggled from widget")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle quick focus from widget", e)
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
        Log.d(TAG, "Quick toggle widget enabled")
        WidgetManager.getInstance(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Quick toggle widget disabled")
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Deleted ${appWidgetIds.size} quick toggle widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        appWidgetIds.forEach { widgetId ->
            widgetManager.removeWidgetConfiguration(widgetId.toString())
        }
    }
}