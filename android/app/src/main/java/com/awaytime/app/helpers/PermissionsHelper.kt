package com.awaytime.app.helpers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awaytime.app.receivers.AwayTimeDeviceAdminReceiver

object PermissionsHelper {
    private const val TAG = "PermissionsHelper"
    
    fun getAndAskAdminPermission(context: Context, askIfNeeded: Boolean): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, AwayTimeDeviceAdminReceiver::class.java)
            
            val isAdmin = devicePolicyManager.isAdminActive(adminComponent)
            
            if (!isAdmin && askIfNeeded) {
                // Request device admin permission
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "Enable device admin to prevent tampering with AwayTime settings")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            
            isAdmin
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device admin permission", e)
            false
        }
    }
    
    fun getAndAskAccessibilityPermission(context: Context, askIfNeeded: Boolean): Boolean {
        return try {
            val accessibilityEnabled = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val serviceName = "${context.packageName}/.service.AwayTimeAccessibilityService"
            val isEnabled = accessibilityEnabled?.contains(serviceName) == true
            
            if (!isEnabled && askIfNeeded) {
                // Open accessibility settings
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility permission", e)
            false
        }
    }
}