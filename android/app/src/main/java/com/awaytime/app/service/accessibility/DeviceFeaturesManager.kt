package com.awaytime.app.service.accessibility

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.awaytime.app.models.Wellbeing

/**
 * Manages blocking of device features like Settings access.
 * Adapted from Mindful's DeviceFeaturesManager.
 */
class DeviceFeaturesManager(
    private val context: Context,
    private val blockedContentGoBack: () -> Unit
) {
    companion object {
        private const val TAG = "DeviceFeaturesManager"
    }
    
    fun blockFeatures(packageName: String, node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            when (packageName) {
                "com.android.settings" -> blockSettingsAccess(node, wellbeing)
                else -> {
                    Log.d(TAG, "No specific blocking rules for package: $packageName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking features for package: $packageName", e)
        }
    }
    
    private fun blockSettingsAccess(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            if (wellbeing.tamperProtectionEnabled || wellbeing.parentalControlsActive) {
                // Block access to specific settings that could disable AwayTime
                val blockedSettingsKeywords = listOf(
                    "accessibility",
                    "device administrators", 
                    "device admin",
                    "apps",
                    "application manager",
                    "permissions"
                )
                
                for (keyword in blockedSettingsKeywords) {
                    if (containsTextIgnoreCase(node, keyword)) {
                        Log.d(TAG, "Blocking access to sensitive settings: $keyword")
                        blockedContentGoBack()
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking settings access", e)
        }
    }
    
    private fun containsTextIgnoreCase(node: AccessibilityNodeInfo, searchText: String): Boolean {
        try {
            // Check current node text
            val nodeText = node.text?.toString()?.lowercase()
            if (nodeText?.contains(searchText.lowercase()) == true) {
                return true
            }
            
            // Check content description
            val contentDesc = node.contentDescription?.toString()?.lowercase()
            if (contentDesc?.contains(searchText.lowercase()) == true) {
                return true
            }
            
            // Recursively check child nodes
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let {
                    if (containsTextIgnoreCase(it, searchText)) {
                        return true
                    }
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking text in node", e)
            return false
        }
    }
}