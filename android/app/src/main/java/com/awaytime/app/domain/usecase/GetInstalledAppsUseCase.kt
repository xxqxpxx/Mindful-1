package com.awaytime.app.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.awaytime.app.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
/**
 * Mindful Use Case: Get all installed apps from device
 * Clean architecture implementation following Mindful patterns
 */
class GetInstalledAppsUseCase(
    private val context: Context
) {
    private val packageManager = context.packageManager
    
    suspend operator fun invoke(): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000L) { // 10 second timeout
                val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
                
                val apps = launcherApps.mapNotNull { resolveInfo ->
                    try {
                        val packageName = resolveInfo.activityInfo.packageName
                        
                        // Skip our own app and system apps
                        if (packageName == context.packageName || isSystemApp(packageName)) {
                            return@mapNotNull null
                        }
                        
                        val appName = try {
                            resolveInfo.loadLabel(packageManager).toString()
                        } catch (e: Exception) {
                            packageName.substringAfterLast(".").replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                            }
                        }
                        
                        if (appName.isBlank()) return@mapNotNull null
                        
                        AppInfo(
                            packageName = packageName,
                            appName = appName,
                            isSystemApp = false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
                
                Result.success(apps)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isSystemApp(packageName: String): Boolean {
        val systemApps = setOf(
            "android", "com.android.systemui", "com.android.settings",
            "com.android.launcher", "com.android.inputmethod",
            "com.google.android.gms", "com.google.android.gsf"
        )
        return systemApps.any { packageName.startsWith(it) }
    }
}