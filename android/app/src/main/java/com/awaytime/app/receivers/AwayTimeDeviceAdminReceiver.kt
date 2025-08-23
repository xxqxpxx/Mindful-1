package com.awaytime.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device admin receiver for tamper protection.
 * Adapted from Mindful's DeviceAdminReceiver.
 */
class AwayTimeDeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "AwayTimeDeviceAdminReceiver"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        
        // Notify the accessibility service that tamper protection changed
        val serviceIntent = Intent("com.awaytime.app.action.tamperProtectionChanged")
        context.sendBroadcast(serviceIntent)
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device admin disabled")
        
        // Notify the accessibility service that tamper protection changed
        val serviceIntent = Intent("com.awaytime.app.action.tamperProtectionChanged")
        context.sendBroadcast(serviceIntent)
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device admin disable requested")
        return "Disabling device admin will reduce AwayTime's ability to prevent tampering with app blocking settings."
    }
}