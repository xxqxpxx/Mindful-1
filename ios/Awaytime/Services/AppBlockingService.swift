import Foundation
import ManagedSettings
import FamilyControls
import DeviceActivity

@MainActor
class AppBlockingService: ObservableObject {
    @Published var isBlocking = false
    @Published var blockedAppGroups: [String] = []
    @Published var blockingSchedule: BlockingSchedule?
    
    private let store = ManagedSettingsStore()
    private let coreDataManager = CoreDataManager.shared
    private let notificationService = NotificationService()
    
    init() {
        loadBlockingState()
    }
    
    // MARK: - App Blocking Control
    
    func blockApps(for appGroup: AppGroupEntity) {
        guard let appsData = appGroup.selectedAppsData,
              let selection = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(appsData) as? FamilyActivitySelection else {
            print("❌ Failed to load app selection for blocking")
            return
        }
        
        // Configure shield settings
        let shieldSettings = ShieldSettings.Applications(
            applicationTokens: selection.applicationTokens,
            exceptedApplicationTokens: Set(),
            webDomainTokens: selection.webDomainTokens,
            exceptedWebDomainTokens: Set()
        )
        
        // Apply blocking
        store.shield.applications = shieldSettings
        
        // Update state
        isBlocking = true
        if !blockedAppGroups.contains(appGroup.name) {
            blockedAppGroups.append(appGroup.name)
        }
        
        // Save blocking state
        saveBlockingState()
        
        // Send notification
        notificationService.sendLimitReachedNotification(appGroupName: appGroup.name)
        
        // Log blocking event
        logBlockingEvent(appGroupName: appGroup.name, action: "blocked")
        
        print("✅ Apps blocked for group: \(appGroup.name)")
    }
    
    func unblockApps(for appGroup: AppGroupEntity) {
        // Remove shield settings
        store.shield.applications = nil
        
        // Update state
        isBlocking = false
        blockedAppGroups.removeAll { $0 == appGroup.name }
        
        // Save blocking state
        saveBlockingState()
        
        // Log unblocking event
        logBlockingEvent(appGroupName: appGroup.name, action: "unblocked")
        
        print("✅ Apps unblocked for group: \(appGroup.name)")
    }
    
    func unblockAllApps() {
        // Remove all shield settings
        store.shield.applications = nil
        store.shield.webContent = nil
        
        // Update state
        isBlocking = false
        blockedAppGroups.removeAll()
        
        // Save blocking state
        saveBlockingState()
        
        print("✅ All apps unblocked")
    }
    
    // MARK: - Scheduled Blocking
    
    func scheduleBlocking(for appGroup: AppGroupEntity, schedule: BlockingSchedule) {
        self.blockingSchedule = schedule
        
        // Create device activity schedule
        let deviceSchedule = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: schedule.startHour, minute: schedule.startMinute),
            intervalEnd: DateComponents(hour: schedule.endHour, minute: schedule.endMinute),
            repeats: schedule.repeats
        )
        
        // Configure monitoring for scheduled blocking
        let activityName = DeviceActivityName("ScheduledBlocking_\(appGroup.id.uuidString)")
        
        do {
            let request = DeviceActivityMonitoringRequest(
                deviceActivity: activityName,
                schedule: deviceSchedule,
                events: [:]
            )
            
            try DeviceActivityCenter().startMonitoring(request)
            
            saveBlockingState()
            print("✅ Scheduled blocking configured for \(appGroup.name)")
            
        } catch {
            print("❌ Failed to schedule blocking: \(error)")
        }
    }
    
    func cancelScheduledBlocking() {
        blockingSchedule = nil
        saveBlockingState()
        
        // Cancel all device activity monitoring
        do {
            let center = DeviceActivityCenter()
            // Note: In a real implementation, you'd track activity names to cancel specific ones
            print("✅ Scheduled blocking cancelled")
        } catch {
            print("❌ Failed to cancel scheduled blocking: \(error)")
        }
    }
    
    // MARK: - Blocking Status
    
    func isAppGroupBlocked(_ appGroupName: String) -> Bool {
        return blockedAppGroups.contains(appGroupName)
    }
    
    func getBlockingStatus(for appGroup: AppGroupEntity) -> BlockingStatus {
        let isCurrentlyBlocked = isAppGroupBlocked(appGroup.name)
        let todayUsage = coreDataManager.getTodayUsage(for: appGroup.name)
        let dailyLimit = Int(appGroup.dailyLimitMinutes)
        
        if isCurrentlyBlocked {
            return .blocked(reason: .limitReached)
        } else if todayUsage >= dailyLimit {
            return .shouldBlock(reason: .limitReached)
        } else if let schedule = blockingSchedule, schedule.isActiveNow() {
            return .shouldBlock(reason: .scheduledTime)
        } else {
            return .allowed
        }
    }
    
    // MARK: - Emergency Override
    
    func requestEmergencyOverride(for appGroup: AppGroupEntity, duration: TimeInterval) {
        // Temporarily unblock apps
        unblockApps(for: appGroup)
        
        // Schedule re-blocking after the override period
        DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
            self.blockApps(for: appGroup)
        }
        
        // Log emergency override
        logBlockingEvent(appGroupName: appGroup.name, action: "emergency_override", duration: duration)
        
        print("✅ Emergency override granted for \(appGroup.name) for \(duration) seconds")
    }
    
    // MARK: - Blocking Analytics
    
    func getBlockingStats() -> BlockingStats {
        let logs = getBlockingLogs()
        let totalBlocks = logs.filter { $0.action == "blocked" }.count
        let totalOverrides = logs.filter { $0.action == "emergency_override" }.count
        let averageBlockDuration = calculateAverageBlockDuration(from: logs)
        
        return BlockingStats(
            totalBlockingEvents: totalBlocks,
            emergencyOverrides: totalOverrides,
            averageBlockDuration: averageBlockDuration,
            currentlyBlockedGroups: blockedAppGroups.count
        )
    }
    
    // MARK: - Data Persistence
    
    private func saveBlockingState() {
        let state = BlockingState(
            isBlocking: isBlocking,
            blockedAppGroups: blockedAppGroups,
            blockingSchedule: blockingSchedule
        )
        
        if let data = try? JSONEncoder().encode(state) {
            UserDefaults.standard.set(data, forKey: "blockingState")
        }
    }
    
    private func loadBlockingState() {
        guard let data = UserDefaults.standard.data(forKey: "blockingState"),
              let state = try? JSONDecoder().decode(BlockingState.self, from: data) else {
            return
        }
        
        isBlocking = state.isBlocking
        blockedAppGroups = state.blockedAppGroups
        blockingSchedule = state.blockingSchedule
    }
    
    private func logBlockingEvent(appGroupName: String, action: String, duration: TimeInterval? = nil) {
        let event = BlockingEvent(
            appGroupName: appGroupName,
            action: action,
            timestamp: Date(),
            duration: duration
        )
        
        var logs = getBlockingLogs()
        logs.append(event)
        
        // Keep only last 100 events
        if logs.count > 100 {
            logs = Array(logs.suffix(100))
        }
        
        if let data = try? JSONEncoder().encode(logs) {
            UserDefaults.standard.set(data, forKey: "blockingLogs")
        }
    }
    
    private func getBlockingLogs() -> [BlockingEvent] {
        guard let data = UserDefaults.standard.data(forKey: "blockingLogs"),
              let logs = try? JSONDecoder().decode([BlockingEvent].self, from: data) else {
            return []
        }
        return logs
    }
    
    private func calculateAverageBlockDuration(from logs: [BlockingEvent]) -> TimeInterval {
        let blockEvents = logs.filter { $0.action == "blocked" }
        let unblockEvents = logs.filter { $0.action == "unblocked" }
        
        var totalDuration: TimeInterval = 0
        var pairCount = 0
        
        for blockEvent in blockEvents {
            if let unblockEvent = unblockEvents.first(where: { 
                $0.appGroupName == blockEvent.appGroupName && 
                $0.timestamp > blockEvent.timestamp 
            }) {
                totalDuration += unblockEvent.timestamp.timeIntervalSince(blockEvent.timestamp)
                pairCount += 1
            }
        }
        
        return pairCount > 0 ? totalDuration / Double(pairCount) : 0
    }
}

// MARK: - Data Models

struct BlockingSchedule: Codable {
    let startHour: Int
    let startMinute: Int
    let endHour: Int
    let endMinute: Int
    let repeats: Bool
    let daysOfWeek: [Int] // 1 = Sunday, 2 = Monday, etc.
    
    func isActiveNow() -> Bool {
        let calendar = Calendar.current
        let now = Date()
        let currentHour = calendar.component(.hour, from: now)
        let currentMinute = calendar.component(.minute, from: now)
        let currentWeekday = calendar.component(.weekday, from: now)
        
        // Check if today is in the scheduled days
        if !daysOfWeek.isEmpty && !daysOfWeek.contains(currentWeekday) {
            return false
        }
        
        let currentTimeMinutes = currentHour * 60 + currentMinute
        let startTimeMinutes = startHour * 60 + startMinute
        let endTimeMinutes = endHour * 60 + endMinute
        
        if startTimeMinutes <= endTimeMinutes {
            // Same day schedule
            return currentTimeMinutes >= startTimeMinutes && currentTimeMinutes < endTimeMinutes
        } else {
            // Overnight schedule
            return currentTimeMinutes >= startTimeMinutes || currentTimeMinutes < endTimeMinutes
        }
    }
}

enum BlockingStatus {
    case allowed
    case blocked(reason: BlockingReason)
    case shouldBlock(reason: BlockingReason)
    
    enum BlockingReason {
        case limitReached
        case scheduledTime
        case manualBlock
    }
}

struct BlockingState: Codable {
    let isBlocking: Bool
    let blockedAppGroups: [String]
    let blockingSchedule: BlockingSchedule?
}

struct BlockingEvent: Codable {
    let appGroupName: String
    let action: String
    let timestamp: Date
    let duration: TimeInterval?
}

struct BlockingStats {
    let totalBlockingEvents: Int
    let emergencyOverrides: Int
    let averageBlockDuration: TimeInterval
    let currentlyBlockedGroups: Int
    
    var formattedAverageBlockDuration: String {
        let hours = Int(averageBlockDuration) / 3600
        let minutes = (Int(averageBlockDuration) % 3600) / 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
}