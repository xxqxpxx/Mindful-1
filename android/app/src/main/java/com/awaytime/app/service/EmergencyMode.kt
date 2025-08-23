package com.awaytime.app.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Emergency Mode - Disables potentially crash-causing operations
 * This is a temporary safety measure to prevent native crashes
 */
object EmergencyMode {
    const val TAG = "EmergencyMode"
    private const val PREFS_NAME = "emergency_mode"
    private const val KEY_EMERGENCY_ENABLED = "emergency_enabled"
    private const val KEY_FIREBASE_DISABLED = "firebase_disabled"
    private const val KEY_NATIVE_OPERATIONS_DISABLED = "native_operations_disabled"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val MAX_CRASHES_BEFORE_EMERGENCY = 2
    
    private var prefs: SharedPreferences? = null
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if we should enable emergency mode based on crash history
        val crashCount = getCrashCount()
        if (crashCount >= MAX_CRASHES_BEFORE_EMERGENCY) {
            enableEmergencyMode()
            Log.w(TAG, "🚨 Emergency mode enabled due to $crashCount previous crashes")
        }
    }
    
    fun recordCrash() {
        val currentCount = getCrashCount()
        prefs?.edit()?.putInt(KEY_CRASH_COUNT, currentCount + 1)?.apply()
        
        if (currentCount + 1 >= MAX_CRASHES_BEFORE_EMERGENCY) {
            enableEmergencyMode()
            Log.w(TAG, "🚨 Emergency mode enabled after ${currentCount + 1} crashes")
        }
    }
    
    fun enableEmergencyMode() {
        prefs?.edit()
            ?.putBoolean(KEY_EMERGENCY_ENABLED, true)
            ?.putBoolean(KEY_FIREBASE_DISABLED, true)
            ?.putBoolean(KEY_NATIVE_OPERATIONS_DISABLED, true)
            ?.apply()
        
        Log.w(TAG, "🚨 EMERGENCY MODE ENABLED - Native operations disabled")
    }
    
    fun disableEmergencyMode() {
        prefs?.edit()
            ?.putBoolean(KEY_EMERGENCY_ENABLED, false)
            ?.putBoolean(KEY_FIREBASE_DISABLED, false)
            ?.putBoolean(KEY_NATIVE_OPERATIONS_DISABLED, false)
            ?.putInt(KEY_CRASH_COUNT, 0)
            ?.apply()
        
        Log.i(TAG, "✅ Emergency mode disabled")
    }
    
    fun isEmergencyModeEnabled(): Boolean {
        return prefs?.getBoolean(KEY_EMERGENCY_ENABLED, false) ?: false
    }
    
    fun isFirebaseDisabled(): Boolean {
        return prefs?.getBoolean(KEY_FIREBASE_DISABLED, false) ?: false
    }
    
    fun isNativeOperationsDisabled(): Boolean {
        return prefs?.getBoolean(KEY_NATIVE_OPERATIONS_DISABLED, false) ?: false
    }
    
    private fun getCrashCount(): Int {
        return prefs?.getInt(KEY_CRASH_COUNT, 0) ?: 0
    }
    
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "emergencyModeEnabled" to isEmergencyModeEnabled(),
            "firebaseDisabled" to isFirebaseDisabled(),
            "nativeOperationsDisabled" to isNativeOperationsDisabled(),
            "crashCount" to getCrashCount()
        )
    }
    
    /**
     * Safe execution wrapper that respects emergency mode
     */
    inline fun <T> safeExecuteWithEmergencyCheck(
        operation: () -> T,
        fallback: T,
        operationName: String,
        requiresNativeOperations: Boolean = false,
        requiresFirebase: Boolean = false
    ): T {
        try {
            // Check emergency mode conditions
            if (isEmergencyModeEnabled()) {
                if (requiresNativeOperations && isNativeOperationsDisabled()) {
                    Log.w(TAG, "⚠️ Skipping $operationName - native operations disabled in emergency mode")
                    return fallback
                }
                
                if (requiresFirebase && isFirebaseDisabled()) {
                    Log.w(TAG, "⚠️ Skipping $operationName - Firebase disabled in emergency mode")
                    return fallback
                }
            }
            
            return operation()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in $operationName: ${e.message}", e)
            recordCrash()
            return fallback
        }
    }
}