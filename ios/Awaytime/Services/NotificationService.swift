import Foundation
import UserNotifications

@MainActor
class NotificationService: ObservableObject {
    @Published var isAuthorized = false
    @Published var notificationsEnabled = true
    
    private let center = UNUserNotificationCenter.current()
    
    init() {
        checkAuthorizationStatus()
        setupNotificationCategories()
    }
    
    // MARK: - Permission Management
    
    func requestPermission() async {
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
            isAuthorized = granted
            
            if granted {
                print("✅ Notification permission granted")
            } else {
                print("❌ Notification permission denied")
            }
        } catch {
            print("❌ Failed to request notification permission: \(error)")
        }
    }
    
    private func checkAuthorizationStatus() {
        center.getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.isAuthorized = settings.authorizationStatus == .authorized
            }
        }
    }
    
    // MARK: - Notification Categories
    
    private func setupNotificationCategories() {
        let warningCategory = UNNotificationCategory(
            identifier: "USAGE_WARNING",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        let limitCategory = UNNotificationCategory(
            identifier: "USAGE_LIMIT",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        let streakCategory = UNNotificationCategory(
            identifier: "STREAK_ACHIEVEMENT",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        center.setNotificationCategories([warningCategory, limitCategory, streakCategory])
    }
    
    // MARK: - Usage Notifications
    
    func sendWarningNotification(appGroupName: String, minutesRemaining: Int) {
        guard isAuthorized && notificationsEnabled else { return }
        
        let content = UNMutableNotificationContent()
        content.title = "Awaytime Warning 💜"
        content.body = getWarningMessage(appGroupName: appGroupName, minutesRemaining: minutesRemaining)
        content.categoryIdentifier = "USAGE_WARNING"
        content.sound = .default
        content.badge = 1
        
        // Add custom data for handling
        content.userInfo = [
            "type": "warning",
            "appGroupName": appGroupName,
            "minutesRemaining": minutesRemaining,
            "timestamp": Date().timeIntervalSince1970
        ]
        
        // Add purple accent color if available
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .active
        }
        
        let request = UNNotificationRequest(
            identifier: "warning_\(appGroupName)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        center.add(request) { error in
            if let error = error {
                print("❌ Failed to send warning notification: \(error)")
            } else {
                print("✅ Warning notification sent for \(appGroupName)")
                self.logNotification(type: "warning", appGroupName: appGroupName)
            }
        }
    }
    
    private func getWarningMessage(appGroupName: String, minutesRemaining: Int) -> String {
        let messages = [
            "You're at 80% of your daily limit for \(appGroupName). \(minutesRemaining) minutes left! 💜",
            "Almost there! \(minutesRemaining) minutes remaining for \(appGroupName) today 🕐",
            "Heads up! You have \(minutesRemaining) minutes left for \(appGroupName) 📱",
            "Time check! \(minutesRemaining) minutes remaining for \(appGroupName) today ⏰"
        ]
        return messages.randomElement() ?? messages[0]
    }
    
    func sendLimitReachedNotification(appGroupName: String) {
        guard isAuthorized && notificationsEnabled else { return }
        
        let content = UNMutableNotificationContent()
        content.title = "Time's Up! 🌙"
        content.body = getLimitReachedMessage(appGroupName: appGroupName)
        content.categoryIdentifier = "USAGE_LIMIT"
        content.sound = .default
        content.badge = 1
        
        // Add custom data for handling
        content.userInfo = [
            "type": "limit",
            "appGroupName": appGroupName,
            "timestamp": Date().timeIntervalSince1970
        ]
        
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .timeSensitive
        }
        
        let request = UNNotificationRequest(
            identifier: "limit_\(appGroupName)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        center.add(request) { error in
            if let error = error {
                print("❌ Failed to send limit notification: \(error)")
            } else {
                print("✅ Limit notification sent for \(appGroupName)")
                self.logNotification(type: "limit", appGroupName: appGroupName)
            }
        }
    }
    
    private func getLimitReachedMessage(appGroupName: String) -> String {
        let messages = [
            "You've reached your daily limit for \(appGroupName). See you tomorrow! 🌙",
            "That's a wrap for \(appGroupName) today! Time to focus on other things 🌟",
            "Daily limit reached for \(appGroupName). Great job staying mindful! 💜",
            "Time's up for \(appGroupName)! Tomorrow is a fresh start 🌅"
        ]
        return messages.randomElement() ?? messages[0]
    }
    
    func sendStreakAchievementNotification(streakDays: Int) {
        guard isAuthorized && notificationsEnabled else { return }
        
        let content = UNMutableNotificationContent()
        content.title = getStreakTitle(streakDays: streakDays)
        content.body = getStreakMessage(streakDays: streakDays)
        content.categoryIdentifier = "STREAK_ACHIEVEMENT"
        content.sound = .default
        content.badge = 1
        
        // Add custom data for handling
        content.userInfo = [
            "type": "streak",
            "streakDays": streakDays,
            "timestamp": Date().timeIntervalSince1970
        ]
        
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .active
        }
        
        let request = UNNotificationRequest(
            identifier: "streak_\(streakDays)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        center.add(request) { error in
            if let error = error {
                print("❌ Failed to send streak notification: \(error)")
            } else {
                print("✅ Streak notification sent for \(streakDays) days")
                self.logNotification(type: "streak", streakDays: streakDays)
            }
        }
    }
    
    private func getStreakTitle(streakDays: Int) -> String {
        switch streakDays {
        case 3:
            return "3-Day Streak! 🚀"
        case 7:
            return "One Week Strong! 🔥"
        case 14:
            return "Two Week Champion! 💎"
        case 30:
            return "30-Day Master! 🏆"
        case 60:
            return "60-Day Legend! 👑"
        case 100:
            return "100-Day Hero! 🎉"
        default:
            return "Streak Achievement! 🎉"
        }
    }
    
    private func getStreakMessage(streakDays: Int) -> String {
        switch streakDays {
        case 3:
            return "3 days strong! You're building momentum! 🚀"
        case 7:
            return "One week of success! You're on fire! 🔥"
        case 14:
            return "Two weeks! You're forming a real habit! 💎"
        case 30:
            return "30 days! This is becoming second nature! 🏆"
        case 60:
            return "60 days! You're a screen time master! 👑"
        case 100:
            return "100 days! Absolutely incredible! 🎉"
        default:
            return "Amazing! You've maintained your screen time goals for \(streakDays) days in a row!"
        }
    }
    
    // MARK: - Scheduled Notifications
    
    func scheduleDailyReminder() {
        guard isAuthorized && notificationsEnabled else { return }
        
        let content = UNMutableNotificationContent()
        content.title = "Daily Check-in 💜"
        content.body = "How's your screen time looking today? Check your progress in Awaytime!"
        content.sound = .default
        
        // Schedule for 8 PM daily
        var dateComponents = DateComponents()
        dateComponents.hour = 20
        dateComponents.minute = 0
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        let request = UNNotificationRequest(
            identifier: "daily_reminder",
            content: content,
            trigger: trigger
        )
        
        center.add(request) { error in
            if let error = error {
                print("❌ Failed to schedule daily reminder: \(error)")
            } else {
                print("✅ Daily reminder scheduled")
            }
        }
    }
    
    func cancelDailyReminder() {
        center.removePendingNotificationRequests(withIdentifiers: ["daily_reminder"])
    }
    
    // MARK: - Notification Management
    
    func clearAllNotifications() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
        
        // Reset badge count
        UIApplication.shared.applicationIconBadgeNumber = 0
    }
    
    func cancelNotifications(for appGroupName: String) {
        center.getPendingNotificationRequests { requests in
            let identifiersToCancel = requests
                .filter { $0.identifier.contains(appGroupName) }
                .map { $0.identifier }
            
            self.center.removePendingNotificationRequests(withIdentifiers: identifiersToCancel)
        }
    }
    
    // MARK: - Settings
    
    func updateNotificationSettings(enabled: Bool) {
        notificationsEnabled = enabled
        
        // Save to Core Data
        CoreDataManager.shared.updateUserSettings(notificationsEnabled: enabled)
        
        if !enabled {
            clearAllNotifications()
        }
    }
    
    func openNotificationSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        
        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl)
        }
    }
    
    // MARK: - Notification Analytics
    
    private func logNotification(type: String, appGroupName: String? = nil, streakDays: Int? = nil) {
        let logData: [String: Any] = [
            "type": type,
            "timestamp": Date().timeIntervalSince1970,
            "appGroupName": appGroupName ?? "",
            "streakDays": streakDays ?? 0
        ]
        
        UserDefaults.standard.set(logData, forKey: "lastNotification")
        
        // Update notification count
        let countKey = "notificationCount_\(type)"
        let currentCount = UserDefaults.standard.integer(forKey: countKey)
        UserDefaults.standard.set(currentCount + 1, forKey: countKey)
    }
    
    func getNotificationStats() -> NotificationStats {
        let warningCount = UserDefaults.standard.integer(forKey: "notificationCount_warning")
        let limitCount = UserDefaults.standard.integer(forKey: "notificationCount_limit")
        let streakCount = UserDefaults.standard.integer(forKey: "notificationCount_streak")
        
        return NotificationStats(
            warningsSent: warningCount,
            limitsSent: limitCount,
            streaksSent: streakCount,
            totalSent: warningCount + limitCount + streakCount
        )
    }
    
    // MARK: - Smart Notification Timing
    
    func scheduleSmartReminder(for appGroupName: String, at percentage: Double) {
        guard isAuthorized && notificationsEnabled else { return }
        
        // Don't send too many notifications
        if hasRecentNotification(for: appGroupName, within: 300) { // 5 minutes
            return
        }
        
        let content = UNMutableNotificationContent()
        content.title = "Mindful Moment 🧘‍♀️"
        content.body = getSmartReminderMessage(appGroupName: appGroupName, percentage: percentage)
        content.categoryIdentifier = "SMART_REMINDER"
        content.sound = .default
        
        // Schedule for immediate delivery
        let request = UNNotificationRequest(
            identifier: "smart_\(appGroupName)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        
        center.add(request) { error in
            if let error = error {
                print("❌ Failed to send smart reminder: \(error)")
            } else {
                print("✅ Smart reminder sent for \(appGroupName)")
            }
        }
    }
    
    private func getSmartReminderMessage(appGroupName: String, percentage: Double) -> String {
        let messages = [
            "You're at \(Int(percentage * 100))% of your \(appGroupName) limit. How are you feeling? 💜",
            "Quick check-in: \(Int(percentage * 100))% used for \(appGroupName). Still on track? 🎯",
            "Mindful moment: You've used \(Int(percentage * 100))% of your \(appGroupName) time today 🌟"
        ]
        return messages.randomElement() ?? messages[0]
    }
    
    private func hasRecentNotification(for appGroupName: String, within seconds: TimeInterval) -> Bool {
        guard let lastNotificationData = UserDefaults.standard.dictionary(forKey: "lastNotification"),
              let timestamp = lastNotificationData["timestamp"] as? TimeInterval,
              let lastAppGroup = lastNotificationData["appGroupName"] as? String else {
            return false
        }
        
        let timeSinceLastNotification = Date().timeIntervalSince1970 - timestamp
        return lastAppGroup == appGroupName && timeSinceLastNotification < seconds
    }
}

// MARK: - Data Models

struct NotificationStats {
    let warningsSent: Int
    let limitsSent: Int
    let streaksSent: Int
    let totalSent: Int
    
    var averagePerDay: Double {
        // Simple calculation - in a real app you'd track over time
        return Double(totalSent) / 30.0 // Assume 30 days
    }
}

// MARK: - Notification Handling

extension NotificationService: UNUserNotificationCenterDelegate {
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        
        let identifier = response.notification.request.identifier
        
        // Handle notification tap
        if identifier.contains("warning") {
            // Navigate to dashboard
            NotificationCenter.default.post(name: .navigateToDashboard, object: nil)
        } else if identifier.contains("limit") {
            // Show limit reached screen
            NotificationCenter.default.post(name: .showLimitReached, object: nil)
        } else if identifier.contains("streak") {
            // Show streak celebration
            NotificationCenter.default.post(name: .showStreakCelebration, object: nil)
        }
        
        completionHandler()
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let navigateToDashboard = Notification.Name("navigateToDashboard")
    static let showLimitReached = Notification.Name("showLimitReached")
    static let showStreakCelebration = Notification.Name("showStreakCelebration")
}