# Android Usage Tracking Test Guide

## Quick Test Steps

### 1. Build and Install
```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Grant Permissions
1. Open the Awaytime app
2. Go to Settings > Apps > Special access > Usage access
3. Find "Awaytime" and enable it
4. Return to the app

### 3. Test Usage Tracking
1. Open some apps (like Chrome, Instagram, etc.) for a few minutes
2. Return to Awaytime
3. Check if usage data is being tracked

### 4. Debug with Test Activity
To add the test activity to your main app, add this to your navigation or main activity:

```kotlin
// Add this button to your main screen for testing
Button(
    onClick = {
        val intent = Intent(context, UsageTrackingTestActivity::class.java)
        context.startActivity(intent)
    }
) {
    Text("Test Usage Tracking")
}
```

### 5. Verify Functionality

**Expected Results:**
- ✅ Permission status shows "GRANTED"
- ✅ User unlock status shows "true"
- ✅ Usage data shows actual app usage times
- ✅ Data updates automatically every 10 seconds
- ✅ Only user-installed apps are shown (no system apps)

**Common Issues:**
- **"No usage data"**: Permission not granted or device locked
- **"Permission denied"**: Need to enable usage access in system settings
- **Empty app list**: Apps haven't been used today or permission issues

### 6. Integration Test

Test the main app functionality:

```kotlin
// In your main app, test the service
val usageService = UsageTrackingService(context)
val debugInfo = usageService.getDebugInfo()
println("Debug info: $debugInfo")

// Test getting current usage
lifecycleScope.launch {
    val usage = usageService.getCurrentUsage("Social Media")
    println("Current usage: $usage minutes")
}
```

## Troubleshooting

### Permission Issues
```bash
# Check if permission is granted via ADB
adb shell appops get com.awaytime.app GET_USAGE_STATS
# Should return: "allow" if granted
```

### Logcat Debugging
```bash
# View usage tracking logs
adb logcat | grep -E "(📊|❌|✅|🔍)"
```

### Manual Permission Grant (for testing)
```bash
# Grant permission via ADB (requires root or system app)
adb shell appops set com.awaytime.app GET_USAGE_STATS allow
```

## Expected Log Output

When working correctly, you should see logs like:
```
📊 Chrome: 15 minutes
📊 Instagram: 8 minutes  
📊 Total usage for group: 23 minutes
✅ Started usage monitoring with improved tracker
🔍 Usage stats permission check: GRANTED
```

## Performance Notes

- The improved tracker uses caching to minimize system calls
- Event-based tracking is more accurate than stats-based
- Background monitoring runs every 5 minutes
- Real-time updates occur every 30 seconds when app is active

The Android usage tracking should now work reliably! 🎉