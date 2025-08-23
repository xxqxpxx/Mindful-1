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
 * Usage summary widget showing today's screen time and progress
 */
class UsageSummaryWidget : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "UsageSummaryWidget"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "Updating ${appWidgetIds.size} usage summary widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        
        scope.launch {
            try {
                // Force update widget data
                widgetManager.updateAllWidgetData()
                
                // Update each widget
                appWidgetIds.forEach { widgetId ->
                    updateWidget(context, appWidgetManager, widgetId, widgetManager)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update usage summary widgets", e)
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
            val views = RemoteViews(context.packageName, R.layout.widget_usage_summary)
            val data = widgetManager.usageSummaryData.value
            
            if (data != null) {
                // Update usage text
                views.setTextViewText(R.id.tv_usage_time, data.getUsageText())
                views.setTextViewText(R.id.tv_daily_limit, "of ${data.getLimitText()}")
                
                // Update progress bar
                val progressPercentage = (data.usageProgress * 100).toInt()
                views.setProgressBar(R.id.progress_usage, 100, progressPercentage, false)
                
                // Update streak
                views.setTextViewText(R.id.tv_streak, "${data.streakDays} day streak")
                
                // Set progress bar color based on usage
                val progressColor = when {
                    data.isLimitExceeded -> Color.RED
                    data.usageProgress > 0.8f -> Color.parseColor("#FF9800") // Orange
                    else -> Color.parseColor("#4CAF50") // Green
                }
                views.setInt(R.id.progress_usage, "setProgressTint", progressColor)
                
                // Update top apps if available
                if (data.topApps.isNotEmpty()) {
                    val topApp = data.topApps.first()
                    views.setTextViewText(R.id.tv_top_app, topApp.appName)
                    views.setTextViewText(R.id.tv_top_app_time, topApp.getUsageText())
                    views.setViewVisibility(R.id.layout_top_app, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.layout_top_app, android.view.View.GONE)
                }
                
                // Update last updated time
                val timeAgo = getTimeAgo(data.lastUpdated)
                views.setTextViewText(R.id.tv_last_updated, "Updated $timeAgo")
                
            } else {
                // Show loading or error state
                views.setTextViewText(R.id.tv_usage_time, "Loading...")
                views.setTextViewText(R.id.tv_daily_limit, "")
                views.setProgressBar(R.id.progress_usage, 100, 0, false)
                views.setTextViewText(R.id.tv_streak, "Calculating...")
                views.setViewVisibility(R.id.layout_top_app, android.view.View.GONE)
                views.setTextViewText(R.id.tv_last_updated, "")
            }
            
            // Set click intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            // Update the widget
            appWidgetManager.updateAppWidget(widgetId, views)
            
            Log.d(TAG, "Updated usage summary widget $widgetId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update usage summary widget $widgetId", e)
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
        Log.d(TAG, "Usage summary widget enabled")
        
        // Start widget manager updates
        WidgetManager.getInstance(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Usage summary widget disabled")
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Deleted ${appWidgetIds.size} usage summary widgets")
        
        val widgetManager = WidgetManager.getInstance(context)
        appWidgetIds.forEach { widgetId ->
            widgetManager.removeWidgetConfiguration(widgetId.toString())
        }
    }
}