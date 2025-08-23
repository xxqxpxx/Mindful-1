package com.awaytime.app.core

import android.content.Context
import com.awaytime.app.service.AppBlockingService
import com.awaytime.app.service.PermissionService

/**
 * Core blocking functionality that manages app blocking
 * and provides a unified interface for the app's blocking features.
 */
class AwayTimeBlocker(private val context: Context) {
    
    private val blockingService = AppBlockingService(context)
    private val permissionService = PermissionService(context)
    
    suspend fun blockApps(packageNames: List<String>) {
        if (!permissionService.hasAccessibilityPermission()) {
            throw SecurityException("Accessibility permission not granted")
        }
        blockingService.blockApps(packageNames)
    }
    
    suspend fun unblockApps(packageNames: List<String>) {
        blockingService.unblockApps(packageNames)
    }
    
    suspend fun unblockAllApps() {
        blockingService.unblockAllApps()
    }
    
    fun getBlockedApps(): Set<String> {
        return blockingService.getBlockedApps()
    }
    
    fun isAppBlocked(packageName: String): Boolean {
        return blockingService.isAppBlocked(packageName)
    }
    
    fun isBlockingEnabled(): Boolean {
        return permissionService.hasAccessibilityPermission() && 
               blockingService.isBlockingActive()
    }
    
    suspend fun setScheduledBlock(packageNames: List<String>, durationMinutes: Int) {
        blockingService.setScheduledBlock(packageNames, durationMinutes)
    }
    
    fun clearScheduledBlocks() {
        blockingService.clearScheduledBlocks()
    }
}