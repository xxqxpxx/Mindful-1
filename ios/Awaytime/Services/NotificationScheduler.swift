import Foundation
import UserNotifications

@MainActor
class NotificationScheduler: ObservableObject {
    private let notificationService = NotificationService()
    private let goalTrackingService = GoalTrackingService()
    
    @Published var scheduledNotifications: [ScheduledNotification] = []
    
    init() {
        loadScheduledNotifications()
    }
    
    // MARK: - Usage-Based Notifications
    
    func checkAndSendUsageNotifications(for appGroup: AppGroupEntity, currentUsage: Int) {
        let dailyLimit = Int(appGroup.dailyLimitMinutes)
        let usagePercentage = Double(currentUsage) / Double(dailyLimit)
        
        // Warning at 80%
        if usagePercentage >= 0.8 && usagePercentage < 1.0 {
            let minutesRemaining = dailyLimit - currentUsage
            notificationService.sendWarningNotification(
                appGroupName: appGroup.name,
                minutesRemaining: minutesRemaining
            )
        }
        
        // Limit reached at 100%
        if usagePercentage >= 1.0 {
            notificationService.sendLimitReachedNotification(appGroupName: appGroup.name)
        }
        
        // Smart reminders at 50% and 70%
        if usagePercentage >= 0.5 && usagePercentage < 0.55 {
            notificationService.scheduleSmartReminder(for: appGroup.name, at: usagePercentage)
        }
        
        if usagePercentage >= 0.7 && usagePercentage < 0.75 {
            notificationService.scheduleSmartReminder(for: appGroup.name, at: usagePercentage)
        }
    }
    
    // MARK: - Streak Notifications
    
    func checkAndSendStreakNotifications() {
        let currentStreak = goalTrackingService.currentStreak
        let milestones = [3, 7, 14, 30, 60, 100]
        
        if milestones.contains(currentStreak) {
            notificationService.sendStreakAchievementNotification(streakDays: currentStreak)
        }
    }
    
    // MARK: - Scheduled Notifications
    
    func scheduleDailyCheckIn(at hour: Int, minute: Int = 0) {
        let content = UNMutableNotificationContent()
        content.title = "Daily Check-in 💜"
        content.body = "How did your screen time goals go today? Check your progress in Awaytime!"
        content.categoryIdentifier = "DAILY_CHECKIN"
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        let request = UNNotificationRequest(
            identifier: "daily_checkin",
            content: content,
            trigger: trigger
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("❌ Failed to schedule daily check-in: \(error)")
            } else {
                print("✅ Daily check-in scheduled for \(hour):\(String(format: "%02d", minute))")
            }
        }
        
        let scheduledNotification = ScheduledNotification(
            id: "daily_checkin",
            type: .dailyCheckIn,
            scheduledTime: "\(hour):\(String(format: "%02d", minute))",
            isActive: true
        )
        
        addScheduledNotification(scheduledNotification)
    }
    
    func scheduleWeeklyReview(on weekday: Int, at hour: Int, minute: Int = 0) {
        let content = UNMutableNotificationContent()
        content.title = "Weekly Review 📊"
        content.body = "Time for your weekly screen time review! See how you did this week."
        content.categoryIdentifier = "WEEKLY_REVIEW"
        content.sound = .default
        
        var dateComponents = DateComponents()
        dateComponents.weekday = weekday // 1 = Sunday, 2 = Monday, etc.
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        let request = UNNotificationRequest(
            identifier: "weekly_review",
            content: content,
            trigger: trigger
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("❌ Failed to schedule weekly review: \(error)")
            } else {
                print("✅ Weekly review scheduled")
            }
        }
        
        let scheduledNotification = ScheduledNotification(
            id: "weekly_review",
            type: .weeklyReview,
            scheduledTime: getWeekdayName(weekday) + " \(hour):\(String(format: "%02d", minute))",
            isActive: true
        )
        
        addScheduledNotification(scheduledNotification)
    }
    
    func scheduleMotivationalReminders() {
        let motivationalMessages = [
            "You're building great habits! Keep it up! 💪",
            "Every mindful choice counts. You've got this! 🌟",
            "Progress, not perfection. You're doing amazing! 📈",
            "Your future self will thank you for this! 🙏"
        ]
        
        for (index, message) in motivationalMessages.enumerated() {
            let content = UNMutableNotificationContent()
            content.title = "Motivation Boost 🚀"
            content.body = message
            content.categoryIdentifier = "MOTIVATION"
            content.sound = .default
            
            // Schedule at different times throughout the week
            var dateComponents = DateComponents()
            dateComponents.weekday = (index % 7) + 1
            dateComponents.hour = 10 + (index * 2) // Spread throughout the day
            dateComponents.minute = 0
            
            let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
            
            let request = UNNotificationRequest(
                identifier: "motivation_\(index)",
                content: content,
                trigger: trigger
            )
            
            UNUserNotificationCenter.current().add(request) { error in
                if let error = error {
                    print("❌ Failed to schedule motivational reminder \(index): \(error)")
                } else {
                    print("✅ Motivational reminder \(index) scheduled")
                }
            }
        }
    }
    
    // MARK: - Notification Management
    
    func cancelScheduledNotification(_ notification: ScheduledNotification) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [notification.id])
        removeScheduledNotification(notification)
    }
    
    func cancelAllScheduledNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        scheduledNotifications.removeAll()
        saveScheduledNotifications()
    }
    
    private func addScheduledNotification(_ notification: ScheduledNotification) {
        scheduledNotifications.append(notification)
        saveScheduledNotifications()
    }
    
    private func removeScheduledNotification(_ notification: ScheduledNotification) {
        scheduledNotifications.removeAll { $0.id == notification.id }
        saveScheduledNotifications()
    }
    
    // MARK: - Persistence
    
    private func loadScheduledNotifications() {
        if let data = UserDefaults.standard.data(forKey: "scheduledNotifications"),
           let notifications = try? JSONDecoder().decode([ScheduledNotification].self, from: data) {
            scheduledNotifications = notifications
        }
    }
    
    private func saveScheduledNotifications() {
        if let data = try? JSONEncoder().encode(scheduledNotifications) {
            UserDefaults.standard.set(data, forKey: "scheduledNotifications")
        }
    }
    
    // MARK: - Utility
    
    private func getWeekdayName(_ weekday: Int) -> String {
        let weekdays = ["", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
        return weekdays[weekday]
    }
}

// MARK: - Data Models

struct ScheduledNotification: Identifiable, Codable {
    let id: String
    let type: NotificationType
    let scheduledTime: String
    let isActive: Bool
    
    enum NotificationType: String, Codable, CaseIterable {
        case dailyCheckIn = "daily_checkin"
        case weeklyReview = "weekly_review"
        case motivation = "motivation"
        case smartReminder = "smart_reminder"
        
        var displayName: String {
            switch self {
            case .dailyCheckIn:
                return "Daily Check-in"
            case .weeklyReview:
                return "Weekly Review"
            case .motivation:
                return "Motivation"
            case .smartReminder:
                return "Smart Reminder"
            }
        }
        
        var icon: String {
            switch self {
            case .dailyCheckIn:
                return "calendar"
            case .weeklyReview:
                return "chart.bar"
            case .motivation:
                return "heart.fill"
            case .smartReminder:
                return "lightbulb"
            }
        }
    }
}