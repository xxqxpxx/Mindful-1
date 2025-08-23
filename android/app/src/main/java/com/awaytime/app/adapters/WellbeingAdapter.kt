package com.awaytime.app.adapters

import android.content.Context
import android.util.Log
import com.awaytime.app.data.entity.AppGroupEntity
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.enums.PlatformFeatures
import com.awaytime.app.helpers.SharedPrefsHelper
import com.awaytime.app.models.Wellbeing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Adapter to bridge between our existing AwayTime app group system 
 * and Mindful's wellbeing settings for the accessibility service.
 */
class WellbeingAdapter(
    private val context: Context,
    private val repository: AwayTimeRepository
) {
    companion object {
        private const val TAG = "WellbeingAdapter"
    }
    
    /**
     * Converts our AppGroupEntity data to Mindful's Wellbeing format
     * and updates the shared preferences for the accessibility service.
     */
    fun syncAppGroupsToWellbeing() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appGroups = repository.getAllAppGroups().first()
                val wellbeing = convertAppGroupsToWellbeing(appGroups)
                
                // Save to shared preferences for accessibility service
                SharedPrefsHelper.getSetWellBeingSettings(context, wellbeing)
                
                Log.d(TAG, "Synced ${appGroups.size} app groups to wellbeing settings")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing app groups to wellbeing", e)
            }
        }
    }
    
    /**
     * Updates wellbeing settings when app groups change
     */
    fun onAppGroupsChanged(appGroups: List<AppGroupEntity>) {
        try {
            val wellbeing = convertAppGroupsToWellbeing(appGroups)
            SharedPrefsHelper.getSetWellBeingSettings(context, wellbeing)
            Log.d(TAG, "Updated wellbeing settings for ${appGroups.size} app groups")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wellbeing settings", e)
        }
    }
    
    /**
     * Adds specific apps to be blocked
     */
    fun addBlockedApps(packageNames: Set<String>) {
        try {
            val currentWellbeing = SharedPrefsHelper.getSetWellBeingSettings(context, null)
            val updatedWellbeing = currentWellbeing.copy(
                blockedApps = currentWellbeing.blockedApps + packageNames
            )
            SharedPrefsHelper.getSetWellBeingSettings(context, updatedWellbeing)
            Log.d(TAG, "Added ${packageNames.size} apps to blocked list")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding blocked apps", e)
        }
    }
    
    /**
     * Removes apps from blocked list
     */
    fun removeBlockedApps(packageNames: Set<String>) {
        try {
            val currentWellbeing = SharedPrefsHelper.getSetWellBeingSettings(context, null)
            val updatedWellbeing = currentWellbeing.copy(
                blockedApps = currentWellbeing.blockedApps - packageNames
            )
            SharedPrefsHelper.getSetWellBeingSettings(context, updatedWellbeing)
            Log.d(TAG, "Removed ${packageNames.size} apps from blocked list")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing blocked apps", e)
        }
    }
    
    /**
     * Enables or disables specific platform features (shorts, reels, etc.)
     */
    fun updatePlatformFeatures(features: Set<PlatformFeatures>) {
        try {
            val currentWellbeing = SharedPrefsHelper.getSetWellBeingSettings(context, null)
            val updatedWellbeing = currentWellbeing.copy(blockedFeatures = features)
            SharedPrefsHelper.getSetWellBeingSettings(context, updatedWellbeing)
            Log.d(TAG, "Updated platform features: ${features.size} features")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating platform features", e)
        }
    }
    
    /**
     * Enables or disables website blocking
     */
    fun updateWebsiteBlocking(blockedWebsites: Set<String>, blockNsfw: Boolean = false) {
        try {
            val currentWellbeing = SharedPrefsHelper.getSetWellBeingSettings(context, null)
            val updatedWellbeing = currentWellbeing.copy(
                blockedWebsites = blockedWebsites,
                blockNsfwSites = blockNsfw
            )
            SharedPrefsHelper.getSetWellBeingSettings(context, updatedWellbeing)
            Log.d(TAG, "Updated website blocking: ${blockedWebsites.size} sites, NSFW: $blockNsfw")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating website blocking", e)
        }
    }
    
    private fun convertAppGroupsToWellbeing(appGroups: List<AppGroupEntity>): Wellbeing {
        val blockedApps = mutableSetOf<String>()
        
        appGroups.forEach { group ->
            // Add apps from active groups to blocked list
            if (group.isActive) {
                group.getSelectedApps().forEach { packageName ->
                    blockedApps.add(packageName)
                }
            }
        }
        
        // Get current wellbeing to preserve other settings
        val currentWellbeing = SharedPrefsHelper.getSetWellBeingSettings(context, null)
        
        return currentWellbeing.copy(
            blockedApps = blockedApps,
            // Add default platform features that users commonly want to block
            blockedFeatures = currentWellbeing.blockedFeatures.ifEmpty {
                setOf(
                    PlatformFeatures.INSTAGRAM_REELS,
                    PlatformFeatures.YOUTUBE_SHORTS,
                    PlatformFeatures.TIKTOK_FOR_YOU
                )
            }
        )
    }
    
    /**
     * Gets the current wellbeing settings
     */
    fun getCurrentWellbeing(): Wellbeing {
        return SharedPrefsHelper.getSetWellBeingSettings(context, null)
    }
}