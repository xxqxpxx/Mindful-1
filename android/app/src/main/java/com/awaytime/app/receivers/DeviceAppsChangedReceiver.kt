package com.awaytime.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Receives notifications when apps are installed or uninstalled.
 * Adapted from Mindful's DeviceAppsChangedReceiver.
 */
class DeviceAppsChangedReceiver(
    private val onAppsChanged: () -> Unit
) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DeviceAppsChangedReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    Log.d(TAG, "App changed: ${intent.action} for package: $packageName")
                    onAppsChanged()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling app change", e)
        }
    }
    
    fun register(context: Context) {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            context.registerReceiver(this, filter)
            Log.d(TAG, "DeviceAppsChangedReceiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering DeviceAppsChangedReceiver", e)
        }
    }
    
    fun unRegister(context: Context) {
        try {
            context.unregisterReceiver(this)
            Log.d(TAG, "DeviceAppsChangedReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering DeviceAppsChangedReceiver", e)
        }
    }
}