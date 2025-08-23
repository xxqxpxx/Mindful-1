# Usage Tracking Test Plan

## Issue Identified
The apps are not being tracked because:

### iOS Issues:
1. **Mock Data**: DashboardViewModel was using hardcoded values instead of real tracking data
2. **Missing Integration**: UsageTrackingService wasn't connected to the UI
3. **DeviceActivity Extension**: Not properly communicating with main app

### Android Issues:
1. **No Real-time Updates**: DashboardViewModel wasn't actively polling usage data
2. **Service Disconnection**: UsageTrackingService wasn't being called regularly

## Fixes Applied

### iOS Fixes:
1. **Updated DashboardViewModel**: Now connects to real UsageTrackingService and CoreDataManager
2. **Added Real-time Updates**: Listens for DeviceActivity extension notifications
3. **Periodic Refresh**: Updates usage data every minute when app is active
4. **Proper Data Sources**: Uses multiple data sources (Core Data, UserDefaults from extension)

### Android Fixes:
1. **Added Real-time Tracking**: DashboardViewModel now starts periodic usage updates
2. **Connected Services**: Properly integrates UsageTrackingService with UI
3. **Automatic Blocking**: Triggers app blocking when limits are reached
4. **Notifications**: Sends warning and limit notifications

## Testing Steps

### iOS Testing:
1. Open the app and grant Screen Time permissions
2. Select apps to monitor in app selection screen
3. Set a daily limit (e.g., 30 minutes for testing)
4. Use the selected apps for a few minutes
5. Return to Awaytime - usage should update automatically
6. Check that progress circle reflects actual usage

### Android Testing:
1. Open the app and grant Usage Access permission
2. Enable Accessibility Service for app blocking
3. Select apps to monitor
4. Set a short daily limit for testing
5. Use the selected apps
6. Return to Awaytime - usage should update every 30 seconds
7. Verify blocking activates when limit is reached

## Expected Behavior

### Real Usage Tracking:
- ✅ Progress circle shows actual app usage time
- ✅ Time remaining updates based on real usage
- ✅ Usage data persists across app restarts
- ✅ Background monitoring continues when app is closed

### Notifications:
- ✅ Warning notification at 80% of limit
- ✅ Limit reached notification at 100%
- ✅ Apps are blocked when limit is exceeded

### Data Accuracy:
- ✅ Usage tracking accurate within ±10%
- ✅ Multiple app usage is summed correctly
- ✅ Daily reset works at midnight
- ✅ Historical data is preserved

## Debug Information

### iOS Debug:
- Check Console.app for "📊 Usage updated" messages
- Verify UserDefaults in App Group container
- Check DeviceActivity extension logs

### Android Debug:
- Check Logcat for "📊 Usage updated" messages
- Verify SharedPreferences and Room database
- Check UsageStatsManager permissions

## Next Steps

1. **Test with Real Apps**: Use actual social media apps for realistic testing
2. **Verify Background Tracking**: Ensure tracking continues when app is backgrounded
3. **Test Edge Cases**: App switching, device restarts, time changes
4. **Performance Monitoring**: Check battery usage and performance impact