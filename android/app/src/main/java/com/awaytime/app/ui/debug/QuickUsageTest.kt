package com.awaytime.app.ui.debug

import android.content.Context
import com.awaytime.app.service.ImprovedUsageTrackingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Quick test utility to verify usage tracking is working
 */
class QuickUsageTest(private val context: Context) {
    
    private val usageTracker = ImprovedUsageTrackingManager(context)
    
    suspend fun runQuickTest(): UsageTestResult = withContext(Dispatchers.IO) {
        val result = UsageTestResult()
        
        try {
            // Test 1: Check permissions
            result.hasUsagePermission = usageTracker.hasUsageStatsPermission()
            result.isUserUnlocked = usageTracker.isUserUnlocked()
            result.canAccessUsage = usageTracker.testUsageAccess()
            
            println("🧪 Quick Usage Test Results:")
            println("   📋 Has Usage Permission: ${result.hasUsagePermission}")
            println("   🔓 User Unlocked: ${result.isUserUnlocked}")
            println("   ✅ Can Access Usage: ${result.canAccessUsage}")
            
            if (result.hasUsagePermission && result.isUserUnlocked && result.canAccessUsage) {
                // Test 2: Get user apps
                val userApps = usageTracker.getUserInstalledApps()
                result.userAppsCount = userApps.size
                println("   📱 User Apps Found: ${result.userAppsCount}")
                
                // Test 3: Get usage for common apps
                val commonApps = listOf(
                    "com.android.chrome",
                    "com.instagram.android", 
                    "com.whatsapp",
                    "com.facebook.katana",
                    "com.twitter.android",
                    "com.google.android.youtube",
                    "com.spotify.music",
                    "com.netflix.mediaclient"
                )
                
                val installedCommonApps = commonApps.filter { userApps.containsKey(it) }
                println("   🎯 Testing usage for ${installedCommonApps.size} common apps")
                
                if (installedCommonApps.isNotEmpty()) {
                    val usageData = usageTracker.getTodayUsageEvents(installedCommonApps)
                    result.appsWithUsage = usageData.filter { it.value > 0 }.size
                    result.totalUsageMinutes = usageData.values.sum() / (1000 * 60)
                    
                    println("   📊 Apps with usage today: ${result.appsWithUsage}")
                    println("   ⏱️ Total usage: ${result.totalUsageMinutes} minutes")
                    
                    // Show individual app usage
                    usageData.forEach { (packageName, usageMs) ->
                        val usageMinutes = (usageMs / (1000 * 60)).toInt()
                        if (usageMinutes > 0) {
                            val appName = userApps[packageName] ?: packageName
                            println("     📱 $appName: ${usageTracker.formatUsageTime(usageMinutes)}")
                        }
                    }
                } else {
                    println("   ⚠️ No common apps installed for testing")
                }
                
                // Test 4: Test with all user apps (limited to first 50)
                val allAppsTest = userApps.keys.take(50)
                val allUsageData = usageTracker.getTodayUsageEvents(allAppsTest)
                val appsWithAnyUsage = allUsageData.filter { it.value > 0 }
                
                result.totalAppsWithUsage = appsWithAnyUsage.size
                println("   📈 Total apps with usage (from ${allAppsTest.size} tested): ${result.totalAppsWithUsage}")
                
                result.isWorking = true
                
            } else {
                println("   ❌ Cannot test usage - permissions not available")
                result.isWorking = false
            }
            
        } catch (e: Exception) {
            println("   ❌ Test failed: ${e.message}")
            result.error = e.message
            result.isWorking = false
        }
        
        return@withContext result
    }
    
    suspend fun testSpecificApps(packageNames: List<String>): Map<String, Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!usageTracker.hasUsageStatsPermission()) {
                println("❌ No usage permission for specific app test")
                return@withContext emptyMap()
            }
            
            val usageData = usageTracker.getTodayUsageEvents(packageNames)
            val result = mutableMapOf<String, Int>()
            
            println("🧪 Testing specific apps:")
            usageData.forEach { (packageName, usageMs) ->
                val usageMinutes = (usageMs / (1000 * 60)).toInt()
                result[packageName] = usageMinutes
                if (usageMinutes > 0) {
                    println("   📱 $packageName: ${usageTracker.formatUsageTime(usageMinutes)}")
                }
            }
            
            result
        } catch (e: Exception) {
            println("❌ Specific app test failed: ${e.message}")
            emptyMap()
        }
    }
}

data class UsageTestResult(
    var hasUsagePermission: Boolean = false,
    var isUserUnlocked: Boolean = false,
    var canAccessUsage: Boolean = false,
    var userAppsCount: Int = 0,
    var appsWithUsage: Int = 0,
    var totalUsageMinutes: Long = 0,
    var totalAppsWithUsage: Int = 0,
    var isWorking: Boolean = false,
    var error: String? = null
) {
    fun getSummary(): String {
        return if (isWorking) {
            "✅ Usage tracking is working! Found $totalAppsWithUsage apps with usage today (${totalUsageMinutes}m total)"
        } else {
            "❌ Usage tracking not working: ${error ?: "Unknown error"}"
        }
    }
}