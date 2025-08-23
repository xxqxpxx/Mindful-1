import Foundation
import DeviceActivity
import FamilyControls
import ManagedSettings
import UserNotifications

@MainActor
class UsageTrackingService: ObservableObject {
    @Published var isMonitoring = false
    @Published var currentUsage: TimeInterval = 0
    @Published var error: String?
    
    private let deviceActivityCenter = DeviceActivityCenter()
    private let store = ManagedSettingsStore()
    private let coreDataManager = CoreDataManager.shared
    
    // Activity names for monitoring
    private let dailyActivityName = DeviceActivityName("DailyUsageTracking")
    
    init() {
        checkMonitoringStatus()
    }
    
    // MARK: - Monitoring Control
    
    func startMonitoring(for appGroup: AppGroupEntity) {
        guard let appsData = appGroup.selectedAppsData, !appsData.isEmpty else {
            error = "No apps selected for monitoring"
            return
        }
        
        do {
            // Load the selected apps from stored data
            guard let appsData = appGroup.selectedAppsData,
                  let selection = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(appsData) as? FamilyActivitySelection else {
                error = "Failed to load selected apps"
                return
            }
            
            // Create schedule for daily monitoring
            let schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: 0, minute: 0),
                intervalEnd: DateComponents(hour: 23, minute: 59),
                repeats: true
            )
            
            // Create events for monitoring
            let events = createMonitoringEvents(for: appGroup, selection: selection)
            
            // Start monitoring with schedule and events
            try deviceActivityCenter.startMonitoring(dailyActivityName, during: schedule, events: events)
            
            isMonitoring = true
            error = nil
            
            print("✅ Started monitoring for app group: \(appGroup.name ?? "Unknown")")
            
        } catch {
            self.error = "Failed to start monitoring: \(error.localizedDescription)"
            print("❌ Monitoring error: \(error)")
        }
    }
    
    func stopMonitoring() {
        do {
            try deviceActivityCenter.stopMonitoring([dailyActivityName])
            isMonitoring = false
            print("✅ Stopped monitoring")
        } catch {
            self.error = "Failed to stop monitoring: \(error.localizedDescription)"
            print("❌ Stop monitoring error: \(error)")
        }
    }
    
    private func checkMonitoringStatus() {
        // Check if monitoring is currently active
        // Note: DeviceActivity doesn't provide a direct way to check status
        // We'll assume monitoring is active if we have stored monitoring data
        let userDefaults = UserDefaults.standard
        isMonitoring = userDefaults.bool(forKey: "isMonitoringActive")
    }
    
    // MARK: - Event Creation
    
    private func createMonitoringEvents(for appGroup: AppGroupEntity, selection: FamilyActivitySelection) -> [DeviceActivityEvent.Name: DeviceActivityEvent] {
        var events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [:]
        
        // Create warning event at 80% of limit
        let warningThreshold = TimeInterval(appGroup.dailyLimitMinutes * 60) * 0.8
        let warningEventName = DeviceActivityEvent.Name("WarningEvent_\(appGroup.id?.uuidString ?? UUID().uuidString)")
        
        let warningEvent = DeviceActivityEvent(
            applications: selection.applicationTokens,
            categories: selection.categoryTokens,
            webDomains: selection.webDomainTokens,
            threshold: DateComponents(second: Int(warningThreshold))
        )
        
        events[warningEventName] = warningEvent
        
        // Create limit reached event
        let limitThreshold = TimeInterval(appGroup.dailyLimitMinutes * 60)
        let limitEventName = DeviceActivityEvent.Name("LimitEvent_\(appGroup.id?.uuidString ?? UUID().uuidString)")
        
        let limitEvent = DeviceActivityEvent(
            applications: selection.applicationTokens,
            categories: selection.categoryTokens,
            webDomains: selection.webDomainTokens,
            threshold: DateComponents(second: Int(limitThreshold))
        )
        
        events[limitEventName] = limitEvent
        
        return events
    }
    
    // MARK: - Usage Data Retrieval
    
    func getCurrentUsage(for appGroup: AppGroupEntity) async -> TimeInterval {
        // Note: DeviceActivity doesn't provide direct usage querying
        // We'll use stored data from our monitoring extension
        return TimeInterval(coreDataManager.getTodayUsage(for: appGroup.name ?? "") * 60)
    }
    
    func updateUsageData(for appGroup: AppGroupEntity, usageMinutes: Int) {
        // This method will be called by the DeviceActivity extension
        let limitExceeded = usageMinutes >= Int(appGroup.dailyLimitMinutes)
        
        coreDataManager.saveUsageRecord(
            date: Date(),
            usageMinutes: usageMinutes,
            appGroupName: appGroup.name ?? "Unknown",
            limitExceeded: limitExceeded
        )
        
        currentUsage = TimeInterval(usageMinutes * 60)
        
        // Update UserDefaults for quick access
        UserDefaults.standard.set(usageMinutes, forKey: "todayUsage_\(appGroup.name ?? "")")
        UserDefaults.standard.set(Date(), forKey: "lastUpdated_\(appGroup.name ?? "")")
    }
    
    // MARK: - Background Monitoring Support
    
    func setupBackgroundMonitoring() {
        // Store monitoring state
        UserDefaults.standard.set(true, forKey: "isMonitoringActive")
        
        // Setup notification categories for monitoring alerts
        setupNotificationCategories()
    }
    
    private func setupNotificationCategories() {
        let center = UNUserNotificationCenter.current()
        
        // Warning notification category
        let warningCategory = UNNotificationCategory(
            identifier: "USAGE_WARNING",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        // Limit reached notification category
        let limitCategory = UNNotificationCategory(
            identifier: "USAGE_LIMIT",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        center.setNotificationCategories([warningCategory, limitCategory])
    }
    
    // MARK: - Utility Methods
    
    func formatUsageTime(_ timeInterval: TimeInterval) -> String {
        let hours = Int(timeInterval) / 3600
        let minutes = (Int(timeInterval) % 3600) / 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
    
    func getUsageProgress(for appGroup: AppGroupEntity) -> Double {
        let currentMinutes = Double(coreDataManager.getTodayUsage(for: appGroup.name ?? ""))
        let limitMinutes = Double(appGroup.dailyLimitMinutes)
        
        guard limitMinutes > 0 else { return 0 }
        return min(currentMinutes / limitMinutes, 1.0)
    }
}

// MARK: - DeviceActivity Extension Support

extension UsageTrackingService {
    // These methods support communication with the DeviceActivity extension
    
    static func handleWarningEvent(for appGroupName: String) {
        // This will be called from the DeviceActivity extension
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .usageWarningReached,
                object: nil,
                userInfo: ["appGroupName": appGroupName]
            )
        }
    }
    
    static func handleLimitEvent(for appGroupName: String) {
        // This will be called from the DeviceActivity extension
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: .usageLimitReached,
                object: nil,
                userInfo: ["appGroupName": appGroupName]
            )
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let usageWarningReached = Notification.Name("usageWarningReached")
    static let usageLimitReached = Notification.Name("usageLimitReached")
    static let usageDataUpdated = Notification.Name("usageDataUpdated")
}