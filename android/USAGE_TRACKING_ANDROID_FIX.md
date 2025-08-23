# Android Usage Tracking Fix

## Overview
This document outlines the comprehensive fix for Android app usage tracking issues in the Awaytime app. The solution follows Android's official documentation and best practices for using the `UsageStatsManager` API.

## Problems Identified

### 1. Permission Handling Issues
- Incorrect permission checking method for Android Q+
- Missing user unlock status check for Android R+
- No proper error handling for permission failures

### 2. Usage Data Collection Issues
- Basic usage stats collection without event-based tracking
- No filtering of system apps vs user-installed apps
- Missing fallback mechanisms when APIs return null
- No caching to improve performance

### 3. Integration Issues
- Service not properly connected to improved tracking methods
- Missing debug capabilities to troubleshoot issues
- No comprehensive testing utilities

## Solutions Implemented

### 1. ImprovedUsageTrackingManager.kt
Created a new comprehensive usage tracking manager that:

**Permission Management:**
- Uses `AppOpsManager.unsafeCheckOpNoThrow()` for Android Q+ (API 29+)
- Uses deprecated `checkOpNoThrow()` for older versions with proper suppression
- Checks `UserManager.isUserUnlocked()` for Android R+ (API 30+)
- Includes proper exception handling and fallbacks

**App Filtering:**
- Filters system apps vs user-installed apps using `ApplicationInfo.FLAG_SYSTEM`
- Caches app information to improve performance
- Provides methods to get user-installed apps list

**Usage Data Collection:**
- Implements both `queryUsageStats()` and `queryEvents()` methods
- Event-based tracking for more accurate real-time usage
- Proper handling of foreground/background transitions
- Fallback from events to stats when needed

**Error Handling:**
- Comprehensive try-catch blocks for all API calls
- Proper null checks for API responses
- Graceful degradation when permissions are missing

### 2. Updated UsageTrackingService.kt
Enhanced the existing service to:

**Use Improved Tracker:**
- Integrates with `ImprovedUsageTrackingManager`
- Removes duplicate permission checking code
- Uses improved methods for all usage calculations

**Better Permission Checks:**
- Tests actual usage access with `testUsageAccess()`
- Provides clear error messages for permission issues
- Includes permission request helpers

**Enhanced Monitoring:**
- More accurate usage calculations
- Better integration with app groups
- Improved logging and debugging

### 3. Updated PermissionService.kt
Fixed permission checking to:
- Use proper Android Q+ permission checking method
- Include better error handling and logging
- Provide more accurate permission status

### 4. UsageTrackingTestActivity.kt
Created a comprehensive test activity that:

**Debug Information:**
- Shows permission status and system information
- Displays real-time usage data
- Lists user-installed apps

**Real-time Testing:**
- Auto-refreshes every 10 seconds
- Manual refresh capability
- Direct permission request button

**Usage Verification:**
- Shows today's usage for all apps
- Formats usage time properly
- Filters out zero-usage apps

## Key Features

### 1. Proper Permission Handling
```kotlin
// Android Q+ (API 29+)
val mode = appOpsManager.unsafeCheckOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS,
    Process.myUid(),
    context.packageName
)

// Older versions
@Suppress("DEPRECATION")
val mode = appOpsManager.checkOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS,
    Process.myUid(),
    context.packageName
)
```

### 2. User Unlock Check (Android R+)
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isUserUnlocked()) {
    // Cannot access usage stats when device is locked
    return null
}
```

### 3. Event-Based Usage Tracking
```kotlin
val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
// Process ACTIVITY_RESUMED/ACTIVITY_PAUSED events for accurate tracking
```

### 4. User App Filtering
```kotlin
private fun isUserInstalledApp(packageName: String): Boolean {
    val appInfo = packageManager.getApplicationInfo(packageName, 0)
    return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
}
```

## Testing Instructions

### 1. Grant Permissions
1. Build and install the app
2. Open the app and navigate to usage tracking test
3. Tap "Request Permission" to open usage access settings
4. Enable usage access for Awaytime

### 2. Verify Tracking
1. Use some apps for a few minutes
2. Return to the test screen
3. Tap "Refresh Data" to see current usage
4. Verify that usage data is accurate

### 3. Debug Information
The test screen shows:
- Permission status (granted/denied)
- User unlock status
- Android version
- Cache information
- Real usage data for all apps

## Expected Results

### ✅ Working Features
- **Permission Detection**: Correctly identifies when usage access is granted
- **Real-time Tracking**: Shows accurate usage data that updates in real-time
- **Event Processing**: Processes app foreground/background events properly
- **User App Filtering**: Only tracks user-installed apps, not system apps
- **Error Handling**: Gracefully handles permission denials and API failures
- **Performance**: Uses caching to minimize repeated system calls

### ✅ Accurate Usage Data
- Usage time matches actual app usage
- Multiple apps in a group are summed correctly
- Daily reset works at midnight
- Background tracking continues when app is closed

## Files Modified

### Core Implementation
- `android/app/src/main/java/com/awaytime/app/service/ImprovedUsageTrackingManager.kt` - **NEW**
- `android/app/src/main/java/com/awaytime/app/service/UsageTrackingService.kt` - **UPDATED**
- `android/app/src/main/java/com/awaytime/app/service/PermissionService.kt` - **UPDATED**

### Testing & Debug
- `android/app/src/main/java/com/awaytime/app/ui/debug/UsageTrackingTestActivity.kt` - **NEW**
- `android/app/src/main/AndroidManifest.xml` - **UPDATED**

### Documentation
- `android/USAGE_TRACKING_ANDROID_FIX.md` - **NEW**

## Next Steps

1. **Integration Testing**: Test with the main app UI to ensure proper integration
2. **Performance Testing**: Monitor battery usage and performance impact
3. **Edge Case Testing**: Test with device restarts, time zone changes, etc.
4. **Production Cleanup**: Remove debug activities before production release

## Troubleshooting

### Common Issues

**"No usage data available"**
- Check that usage access permission is granted in system settings
- Verify the device is unlocked (Android R+)
- Ensure you've used some apps today to generate usage data

**"Permission denied" errors**
- Open Settings > Apps > Special access > Usage access
- Find Awaytime and enable usage access
- Restart the app after granting permission

**Inaccurate usage data**
- Event-based tracking is more accurate than stats-based
- Some apps may not generate proper usage events
- System apps are filtered out and won't show usage

The Android usage tracking should now work correctly following official Android documentation! 🎉