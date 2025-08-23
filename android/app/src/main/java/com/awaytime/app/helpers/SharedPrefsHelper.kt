package com.awaytime.app.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.awaytime.app.models.Wellbeing
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object SharedPrefsHelper {
    private const val TAG = "SharedPrefsHelper"
    private const val PREFS_NAME = "awaytime_wellbeing_prefs"
    const val PREF_KEY_WELLBEING_SETTINGS = "wellbeing_settings"
    private const val PREF_KEY_CRASH_LOGS = "crash_logs"
    
    private val gson = Gson()
    
    private val listenablePrefsKeys = setOf(PREF_KEY_WELLBEING_SETTINGS)
    
    fun registerUnregisterListenerToListenablePrefs(
        context: Context,
        register: Boolean,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (register) {
                prefs.registerOnSharedPreferenceChangeListener(listener)
            } else {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering/unregistering preference listener", e)
        }
    }
    
    fun getSetWellBeingSettings(context: Context, wellbeing: Wellbeing?): Wellbeing {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        return if (wellbeing != null) {
            // Set wellbeing settings
            try {
                val json = gson.toJson(wellbeing)
                prefs.edit().putString(PREF_KEY_WELLBEING_SETTINGS, json).apply()
                wellbeing
            } catch (e: Exception) {
                Log.e(TAG, "Error saving wellbeing settings", e)
                Wellbeing()
            }
        } else {
            // Get wellbeing settings
            try {
                val json = prefs.getString(PREF_KEY_WELLBEING_SETTINGS, null)
                if (json != null) {
                    gson.fromJson(json, Wellbeing::class.java)
                } else {
                    Wellbeing()
                }
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Error parsing wellbeing settings JSON", e)
                Wellbeing()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading wellbeing settings", e)
                Wellbeing()
            }
        }
    }
    
    fun insertCrashLogToPrefs(context: Context, exception: Exception) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val timestamp = System.currentTimeMillis()
            val crashLog = "[$timestamp] ${exception.javaClass.simpleName}: ${exception.message}\n${exception.stackTraceToString()}"
            
            val existingLogs = prefs.getString(PREF_KEY_CRASH_LOGS, "") ?: ""
            val newLogs = if (existingLogs.isEmpty()) {
                crashLog
            } else {
                "$existingLogs\n\n$crashLog"
            }
            
            // Keep only last 10 crash logs to prevent excessive storage
            val logEntries = newLogs.split("\n\n")
            val limitedLogs = if (logEntries.size > 10) {
                logEntries.takeLast(10).joinToString("\n\n")
            } else {
                newLogs
            }
            
            prefs.edit().putString(PREF_KEY_CRASH_LOGS, limitedLogs).apply()
            Log.d(TAG, "Crash log saved: ${exception.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crash log", e)
        }
    }
}