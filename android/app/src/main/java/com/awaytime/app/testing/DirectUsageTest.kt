/*
package com.awaytime.app.testing

import android.content.Context
import com.awaytime.app.service.ImprovedUsageTrackingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

*/
/**
 * Simple direct test for usage tracking without requiring app groups
 *//*

object DirectUsageTest {
    
    suspend fun testUsageTracking(context: Context): String = withContext(Dispatchers.IO) {
        val usageTracker = ImprovedUsageTrackingManager(context)
        val result = StringBuilder()
        
        try {
            result.appendLine("🧪 Direct Usage Tracking Test")
            result.appendLine("=" * 40)
            
            // Check permissions
            val hasPermission = usageTracker.hasUsageStatsPermission()
            val isUnlocked = usageTracker.isUserUnlocked()
            val canAccess = usageTracker.testUsageAccess()
            
            result.appendLine("📋 Has Usage Permission: $hasPermission")
            result.appendLine("🔓 User Unlocked: $isUnlocked") 
            result.appendLine("✅ Can Access Usage: $canAccess")
            result.appendLine()
            
            if (hasPermission && isUnlocked && canAccess) {
                // Get user apps
                val userApps = usageTracker.getUserInstalledApps()
                result.appendLine("📱 User Apps Found: ${userApps.size}")
                
                // Test common apps that are likely to be installed
                val commonApps = listOf(
                    "com.android.chrome",
                    "com.google.android.chrome",
                    "com.instagram.android",
                    "com.whatsapp",
                    "com.facebook.katana",
                    "com.google.android.youtube",
                    "com.spotify.music",
                    "com.netflix.mediaclient",
                    "com.twitter.android",
                    "com.snapchat.android",
                    "com.tiktok.musically",
                    "com.discord"
                )
                
                val installedCommonApps = commonApps.filter { userApps.containsKey(it) }
                result.appendLine("🎯 Common Apps Installed: ${installedCommonApps.size}")
                
                if (installedCommonApps.isNotEmpty()) {
                    result.appendLine()
                    result.appendLine("📊 Today's Usage Data:")
                    result.appendLine("-" * 30)
                    
                    val usageData = usageTracker.getTodayUsageEvents(installedCommonApps)
                    var totalMinutes = 0L
                    var appsWithUsage = 0
                    
                    usageData.forEach { (packageName, usageMs) ->
                        val minutes = (usageMs / (1000 * 60)).toInt()
                        if (minutes > 0) {
                            val appName = userApps[packageName] ?: packageName
                            result.appendLine("📱 $appName: ${usageTracker.formatUsageTime(minutes)}")
                            totalMinutes += minutes
                            appsWithUsage++
                        }
                    }
                    
                    result.appendLine()
                    result.appendLine("📈 Summary:")
                    result.appendLine("   Apps with usage: $appsWithUsage")
                    result.appendLine("   Total usage: ${usageTracker.formatUsageTime(totalMinutes.toInt())}")
                    
                    if (totalMinutes > 0) {
                        result.appendLine()
                        result.appendLine("✅ SUCCESS: Usage tracking is working!")
                        result.appendLine("   The system can track app usage correctly.")
                    } else {
                        result.appendLine()
                        result.appendLine("⚠️ No usage data found today.")
                        result.appendLine("   Try using some apps first, then test again.")
                    }
                    
                } else {
                    result.appendLine()
                    result.appendLine("⚠️ No common apps found for testing.")
                    result.appendLine("   Testing with first 10 user apps...")
                    
                    val firstTenApps = userApps.keys.take(10)
                    val usageData = usageTracker.getTodayUsageEvents(firstTenApps)
                    val appsWithUsage = usageData.filter { it.value > 0 }
                    
                    if (appsWithUsage.isNotEmpty()) {
                        result.appendLine("📊 Found usage in ${appsWithUsage.size} apps:")
                        appsWithUsage.forEach { (packageName, usageMs) ->
                            val minutes = (usageMs / (1000 * 60)).toInt()
                            val appName = userApps[packageName] ?: packageName
                            result.appendLine("   📱 $appName: ${usageTracker.formatUsageTime(minutes)}")
                        }
                        result.appendLine("✅ Usage tracking is working!")
                    } else {
                        result.appendLine("⚠️ No usage found in any apps today.")
                    }
                }
                
            } else {
                result.appendLine()
                result.appendLine("❌ FAILED: Cannot access usage data")
                if (!hasPermission) {
                    result.appendLine("   → Grant usage access permission in Settings")
                }
                if (!isUnlocked) {
                    result.appendLine("   → Device must be unlocked")
                }
                if (!canAccess) {
                    result.appendLine("   → Usage access test failed")
                }
            }
            
        } catch (e: Exception) {
            result.appendLine()
            result.appendLine("❌ ERROR: ${e.message}")
            e.printStackTrace()
        }
        
        val resultString = result.toString()
        println(resultString) // Also log to console
        return@withContext resultString
    }
    
    */
/**
     * Quick test that just checks if usage tracking is working
     *//*

    suspend fun quickCheck(context: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val usageTracker = ImprovedUsageTrackingManager(context)
            
            if (!usageTracker.hasUsageStatsPermission()) {
                println("❌ Quick check failed: No usage permission")
                return@withContext false
            }
            
            val userApps = usageTracker.getUserInstalledApps()
            if (userApps.isEmpty()) {
                println("❌ Quick check failed: No user apps found")
                return@withContext false
            }
            
            // Test with first 20 apps
            val testApps = userApps.keys.take(20)
            val usageData = usageTracker.getTodayUsageEvents(testApps)
            val hasUsage = usageData.any { it.value > 0 }
            
            println("✅ Quick check: ${if (hasUsage) "WORKING" else "NO USAGE DATA"}")
            return@withContext true // System is working, even if no usage data
            
        } catch (e: Exception) {
            println("❌ Quick check failed: ${e.message}")
            return@withContext false
        }
    }
}*/
