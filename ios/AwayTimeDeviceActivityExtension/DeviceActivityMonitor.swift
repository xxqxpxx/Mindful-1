import DeviceActivity
import Foundation
import UserNotifications

class DeviceActivityMonitor: DeviceActivityMonitor {
    
    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
        
        print("📱 DeviceActivity interval started for: \(activity)")
        
        // Reset daily usage tracking
        resetDailyUsage()
    }
    
    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        
        print("📱 DeviceActivity interval ended for: \(activity)")
        
        // Save end-of-day usage data
        saveDailyUsageSummary()
    }
    
    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)
        
        print("📱 DeviceActivity event reached: \(event) for activity: \(activity)")
        
        // Handle different event types
        let eventString = event.rawValue
        
        if eventString.contains("WarningEvent") {
            handleWarningEvent(eventString)
        } else if eventString.contains("LimitEvent") {
            handleLimitEvent(eventString)
        }
    }
    
    override func intervalWillStartWarning(for activity: DeviceActivityName) {
        super.intervalWillStartWarning(for: activity)
        
        print("📱 DeviceActivity interval will start warning for: \(activity)")
    }
    
    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        super.intervalWillEndWarning(for: activity)
        
        print("📱 DeviceActivity interval will end warning for: \(activity)")
    }
    
    // MARK: - Event Handlers
    
    private func handleWarningEvent(_ eventName: String) {
        // Extract app group ID from event name
        let appGroupName = extractAppGroupName(from: eventName)
        
        // Send warning notification
        sendWarningNotification(for: appGroupName)
        
        // Update usage data
        updateUsageData(for: appGroupName, isWarning: true)
        
        // Notify main app
        notifyMainApp(event: "warning", appGroupName: appGroupName)
    }
    
    private func handleLimitEvent(_ eventName: String) {
        // Extract app group ID from event name
        let appGroupName = extractAppGroupName(from: eventName)
        
        // Send limit reached notification
        sendLimitNotification(for: appGroupName)
        
        // Update usage data
        updateUsageData(for: appGroupName, isWarning: false)
        
        // Notify main app
        notifyMainApp(event: "limit", appGroupName: appGroupName)
        
        // Trigger app blocking
        triggerAppBlocking(for: appGroupName)
    }
    
    // MARK: - Notification Handling
    
    private func sendWarningNotification(for appGroupName: String) {
        let content = UNMutableNotificationContent()
        content.title = "Awaytime Warning"
        content.body = "You're at 80% of your daily limit for \(appGroupName). 15 minutes left! 💜"
        content.categoryIdentifier = "USAGE_WARNING"
        content.sound = .default
        
        let request = UNNotificationRequest(
            identifier: "warning_\(appGroupName)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("❌ Failed to send warning notification: \(error)")
            } else {
                print("✅ Warning notification sent for \(appGroupName)")
            }
        }
    }
    
    private func sendLimitNotification(for appGroupName: String) {
        let content = UNMutableNotificationContent()
        content.title = "Awaytime Limit Reached"
        content.body = "Time's up for \(appGroupName)! See you tomorrow 🌙"
        content.categoryIdentifier = "USAGE_LIMIT"
        content.sound = .default
        
        let request = UNNotificationRequest(
            identifier: "limit_\(appGroupName)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("❌ Failed to send limit notification: \(error)")
            } else {
                print("✅ Limit notification sent for \(appGroupName)")
            }
        }
    }
    
    // MARK: - Data Management
    
    private func updateUsageData(for appGroupName: String, isWarning: Bool) {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        
        // Get the daily limit for this app group
        let dailyLimitMinutes = userDefaults?.integer(forKey: "dailyLimitMinutes_\(appGroupName)") ?? 120
        
        // Calculate actual usage minutes based on event type
        let currentUsageMinutes = isWarning ? 
            Int(Double(dailyLimitMinutes) * 0.8) :  // 80% for warning
            dailyLimitMinutes                       // 100% for limit
        
        // Store usage data for main app to access
        userDefaults?.set(currentUsageMinutes, forKey: "currentUsageMinutes_\(appGroupName)")
        userDefaults?.set(Date(), forKey: "lastUpdated_\(appGroupName)")
        userDefaults?.set(isWarning ? "warning" : "limit", forKey: "lastEvent_\(appGroupName)")
        
        // Also store in the format the dashboard expects
        userDefaults?.set(currentUsageMinutes, forKey: "todayUsage_\(appGroupName)")
        
        print("📊 Usage updated for \(appGroupName): \(currentUsageMinutes) minutes (\(isWarning ? "warning" : "limit") event)")
    }
    
    private func resetDailyUsage() {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        
        // Reset all usage counters for the new day
        let keys = userDefaults?.dictionaryRepresentation().keys.filter { $0.hasPrefix("currentUsage_") }
        keys?.forEach { key in
            userDefaults?.removeObject(forKey: key)
        }
        
        print("✅ Daily usage reset")
    }
    
    private func saveDailyUsageSummary() {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        
        // Save end-of-day summary
        userDefaults?.set(Date(), forKey: "lastDaySummary")
        
        print("✅ Daily usage summary saved")
    }
    
    // MARK: - App Communication
    
    private func notifyMainApp(event: String, appGroupName: String) {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        
        // Store event for main app to process
        let eventData = [
            "type": event,
            "appGroupName": appGroupName,
            "timestamp": Date().timeIntervalSince1970
        ] as [String: Any]
        
        userDefaults?.set(eventData, forKey: "latestEvent")
        
        // Post notification for immediate processing if app is active
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinCenter(),
            CFNotificationName("com.awaytime.usage.event" as CFString),
            nil,
            nil,
            true
        )
    }
    
    private func triggerAppBlocking(for appGroupName: String) {
        // The actual app blocking is handled by ManagedSettings in the main app
        // We just signal that blocking should be activated
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        userDefaults?.set(true, forKey: "shouldBlock_\(appGroupName)")
        userDefaults?.set(Date(), forKey: "blockingStarted_\(appGroupName)")
    }
    
    // MARK: - Utility Methods
    
    private func extractAppGroupName(from eventName: String) -> String {
        // Extract app group name from event name format: "WarningEvent_UUID" or "LimitEvent_UUID"
        let components = eventName.components(separatedBy: "_")
        if components.count > 1 {
            // For now, return a simplified name. In a real implementation,
            // you'd map the UUID back to the actual app group name
            return "My Apps"
        }
        return "Unknown"
    }
}