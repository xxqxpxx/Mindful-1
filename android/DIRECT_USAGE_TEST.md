# Direct Usage Tracking Test

## Issue Identified
From the logs, I can see:
- ✅ Usage stats permission: **GRANTED**
- ✅ Accessibility service: **RUNNING** 
- ❌ Problem: **"No app group found, showing 0 usage"**

The usage tracking system is working, but the app needs app groups to be configured first.

## Quick Test Solution

### Option 1: Test Usage Tracking Directly (Recommended)

Add this code to test usage tracking without app groups:

```kotlin
// In your MainActivity or any activity
private fun testUsageTrackingDirectly() {
    lifecycleScope.launch {
        val usageTracker = ImprovedUsageTrackingManager(this@MainActivity)
        
        // Test common apps
        val testApps = listOf(
            "com.android.chrome",
            "com.instagram.android",
            "com.whatsapp", 
            "com.facebook.katana",
            "com.google.android.youtube"
        )
        
        println("🧪 Testing usage tracking directly...")
        
        // Check permissions
        val hasPermission = usageTracker.hasUsageStatsPermission()
        println("📋 Has permission: $hasPermission")
        
        if (hasPermission) {
            // Get usage data
            val usageData = usageTracker.getTodayUsageEvents(testApps)
            var totalMinutes = 0
            
            usageData.forEach { (packageName, usageMs) ->
                val minutes = (usageMs / (1000 * 60)).toInt()
                if (minutes > 0) {
                    totalMinutes += minutes
                    println("📱 $packageName: ${minutes} minutes")
                }
            }
            
            println("📊 Total usage found: $totalMinutes minutes")
            
            if (totalMinutes > 0) {
                println("✅ Usage tracking is working!")
            } else {
                println("⚠️ No usage found - try using some apps first")
            }
        } else {
            println("❌ No usage permission")
        }
    }
}
```

### Option 2: Create a Test App Group

Add this to create a test app group:

```kotlin
// Create a test app group
private suspend fun createTestAppGroup() {
    val repository = AwayTimeRepository(this)
    
    // Create a test group with common apps
    val testGroup = AppGroup(
        name = "Test Group",
        selectedApps = listOf(
            "com.android.chrome",
            "com.instagram.android", 
            "com.whatsapp"
        ),
        dailyLimitMinutes = 120, // 2 hours
        isActive = true
    )
    
    repository.saveAppGroup(testGroup)
    println("✅ Created test app group")
}
```

### Option 3: Use the Debug Activity

The `UsageTrackingTestActivity` I created will show you exactly what's happening:

1. **Add to your main screen:**
```kotlin
Button(
    onClick = {
        val intent = Intent(this, UsageTrackingTestActivity::class.java)
        startActivity(intent)
    }
) {
    Text("Debug Usage Tracking")
}
```

2. **Or test directly in logcat:**
```bash
adb logcat | grep -E "(📊|❌|✅|🧪)"
```

## Expected Results

If usage tracking is working, you should see:
```
🧪 Testing usage tracking directly...
📋 Has permission: true
📱 com.android.chrome: 15 minutes
📱 com.instagram.android: 8 minutes
📊 Total usage found: 23 minutes
✅ Usage tracking is working!
```

## Next Steps

1. **Test directly** using Option 1 above
2. **Create app groups** in the main app UI
3. **Use some apps** for a few minutes to generate usage data
4. **Check the debug screen** to verify everything is working

The core usage tracking is working - you just need to set up app groups or test directly! 🎉