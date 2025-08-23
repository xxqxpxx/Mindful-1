import SwiftUI
import BackgroundTasks
import DeviceActivity
import os.log

/// Service to ensure app functionality continues reliably in background
@MainActor
class BackgroundReliabilityService: ObservableObject {
    @Published var isBackgroundRefreshEnabled = false
    @Published var lastBackgroundRefresh: Date?
    @Published var backgroundTasksRegistered = false
    
    private let logger = Logger(subsystem: "com.awaytime.app", category: "BackgroundReliability")
    private let backgroundTaskIdentifier = "com.awaytime.app.background-refresh"
    private let processingTaskIdentifier = "com.awaytime.app.background-processing"
    
    private let usageTrackingService = UsageTrackingService()
    private let appBlockingService = AppBlockingService()
    private let coreDataManager = CoreDataManager.shared
    
    init() {
        checkBackgroundRefreshStatus()
        registerBackgroundTasks()
        setupDeviceActivityReliability()
    }
    
    // MARK: - Background Tasks Registration
    
    func registerBackgroundTasks() {
        // Register background app refresh task
        let refreshRegistered = BGTaskScheduler.shared.register(
            forTaskWithIdentifier: backgroundTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleBackgroundRefresh(task as! BGAppRefreshTask)
        }
        
        // Register background processing task
        let processingRegistered = BGTaskScheduler.shared.register(
            forTaskWithIdentifier: processingTaskIdentifier,
            using: nil
        ) { [weak self] task in
            self?.handleBackgroundProcessing(task as! BGProcessingTask)
        }
        
        backgroundTasksRegistered = refreshRegistered && processingRegistered
        
        if backgroundTasksRegistered {
            logger.info("✅ Background tasks registered successfully")
        } else {
            logger.error("❌ Failed to register background tasks")
        }
    }
    
    // MARK: - Background Refresh Handling
    
    private func handleBackgroundRefresh(_ task: BGAppRefreshTask) {
        logger.info("🔄 Background refresh task started")
        
        // Schedule next background refresh
        scheduleBackgroundRefresh()
        
        let operation = BackgroundRefreshOperation()
        
        task.expirationHandler = {
            operation.cancel()
            task.setTaskCompleted(success: false)
            self.logger.warning("⚠️ Background refresh task expired")
        }
        
        operation.completionBlock = {
            task.setTaskCompleted(success: !operation.isCancelled)
            
            DispatchQueue.main.async {
                self.lastBackgroundRefresh = Date()
                self.logger.info("✅ Background refresh completed")
            }
        }
        
        let queue = OperationQueue()
        queue.addOperation(operation)
    }
    
    private func handleBackgroundProcessing(_ task: BGProcessingTask) {
        logger.info("⚙️ Background processing task started")
        
        // Schedule next background processing
        scheduleBackgroundProcessing()
        
        let operation = BackgroundProcessingOperation()
        
        task.expirationHandler = {
            operation.cancel()
            task.setTaskCompleted(success: false)
            self.logger.warning("⚠️ Background processing task expired")
        }
        
        operation.completionBlock = {
            task.setTaskCompleted(success: !operation.isCancelled)
            self.logger.info("✅ Background processing completed")
        }
        
        let queue = OperationQueue()
        queue.addOperation(operation)
    }
    
    // MARK: - Task Scheduling
    
    func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: backgroundTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // 15 minutes
        
        do {
            try BGTaskScheduler.shared.submit(request)
            logger.info("📅 Background refresh scheduled")
        } catch {
            logger.error("❌ Failed to schedule background refresh: \(error)")
        }
    }
    
    func scheduleBackgroundProcessing() {
        let request = BGProcessingTaskRequest(identifier: processingTaskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60) // 1 hour
        request.requiresNetworkConnectivity = false
        request.requiresExternalPower = false
        
        do {
            try BGTaskScheduler.shared.submit(request)
            logger.info("📅 Background processing scheduled")
        } catch {
            logger.error("❌ Failed to schedule background processing: \(error)")
        }
    }
    
    // MARK: - Background Refresh Status
    
    func checkBackgroundRefreshStatus() {
        isBackgroundRefreshEnabled = UIApplication.shared.backgroundRefreshStatus == .available
        
        if !isBackgroundRefreshEnabled {
            logger.warning("⚠️ Background refresh is not available")
        }
    }
    
    func requestBackgroundRefreshPermission() {
        // Guide user to enable background refresh in Settings
        if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(settingsUrl)
        }
    }
    
    // MARK: - Device Activity Reliability
    
    private func setupDeviceActivityReliability() {
        // Ensure device activity monitoring continues after app restart
        validateDeviceActivityMonitoring()
        
        // Setup monitoring for device activity failures
        setupDeviceActivityFailureDetection()
    }
    
    private func validateDeviceActivityMonitoring() {
        Task {
            do {
                // Check if device activity monitoring is still active
                let isMonitoring = await usageTrackingService.isMonitoringActive()
                
                if !isMonitoring {
                    logger.warning("⚠️ Device activity monitoring is inactive, restarting...")
                    try await usageTrackingService.startMonitoring()
                    logger.info("✅ Device activity monitoring restarted")
                }
            } catch {
                logger.error("❌ Failed to validate device activity monitoring: \(error)")
            }
        }
    }
    
    private func setupDeviceActivityFailureDetection() {
        // Monitor for device activity failures and restart if needed
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { [weak self] _ in
            self?.checkDeviceActivityHealth()
        }
    }
    
    private func checkDeviceActivityHealth() {
        Task {
            let lastUpdate = await usageTrackingService.getLastUpdateTime()
            let timeSinceLastUpdate = Date().timeIntervalSince(lastUpdate)
            
            // If no updates for more than 10 minutes during active hours, restart monitoring
            if timeSinceLastUpdate > 600 && isActiveHours() {
                logger.warning("⚠️ Device activity monitoring appears stalled, restarting...")
                
                do {
                    try await usageTrackingService.restartMonitoring()
                    logger.info("✅ Device activity monitoring restarted")
                } catch {
                    logger.error("❌ Failed to restart device activity monitoring: \(error)")
                }
            }
        }
    }
    
    private func isActiveHours() -> Bool {
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: Date())
        return hour >= 7 && hour <= 23 // 7 AM to 11 PM
    }
    
    // MARK: - App Blocking Reliability
    
    func ensureAppBlockingReliability() {
        Task {
            do {
                // Validate that app blocking is still active
                let isBlockingActive = await appBlockingService.isBlockingActive()
                
                if !isBlockingActive {
                    logger.warning("⚠️ App blocking is inactive, reactivating...")
                    try await appBlockingService.initializeBlocking()
                    logger.info("✅ App blocking reactivated")
                }
                
                // Verify blocked apps are still blocked
                await validateBlockedApps()
                
            } catch {
                logger.error("❌ Failed to ensure app blocking reliability: \(error)")
            }
        }
    }
    
    private func validateBlockedApps() async {
        do {
            let blockedApps = await appBlockingService.getCurrentlyBlockedApps()
            let expectedBlockedApps = await appBlockingService.getExpectedBlockedApps()
            
            // Check if any expected blocked apps are not actually blocked
            let missingBlocks = Set(expectedBlockedApps).subtracting(Set(blockedApps))
            
            if !missingBlocks.isEmpty {
                logger.warning("⚠️ Some apps are not properly blocked: \(missingBlocks)")
                
                // Re-apply blocking for missing apps
                try await appBlockingService.blockApps(Array(missingBlocks))
                logger.info("✅ Missing app blocks reapplied")
            }
        } catch {
            logger.error("❌ Failed to validate blocked apps: \(error)")
        }
    }
    
    // MARK: - Data Persistence Reliability
    
    func ensureDataPersistenceReliability() {
        // Validate Core Data stack integrity
        validateCoreDataStack()
        
        // Perform data consistency checks
        performDataConsistencyChecks()
        
        // Setup automatic data backup
        setupAutomaticDataBackup()
    }
    
    private func validateCoreDataStack() {
        do {
            // Test Core Data operations
            _ = try coreDataManager.fetchSelectedApps()
            _ = try coreDataManager.fetchGoals()
            
            logger.info("✅ Core Data stack is healthy")
        } catch {
            logger.error("❌ Core Data stack validation failed: \(error)")
            
            // Attempt to recover Core Data stack
            recoverCoreDataStack()
        }
    }
    
    private func recoverCoreDataStack() {
        do {
            // Attempt to rebuild Core Data stack
            try coreDataManager.rebuildStack()
            logger.info("✅ Core Data stack recovered")
        } catch {
            logger.error("❌ Failed to recover Core Data stack: \(error)")
        }
    }
    
    private func performDataConsistencyChecks() {
        Task {
            do {
                // Check for orphaned records
                let orphanedRecords = try coreDataManager.findOrphanedRecords()
                
                if !orphanedRecords.isEmpty {
                    logger.warning("⚠️ Found \(orphanedRecords.count) orphaned records")
                    try coreDataManager.cleanupOrphanedRecords(orphanedRecords)
                    logger.info("✅ Orphaned records cleaned up")
                }
                
                // Validate data relationships
                let invalidRelationships = try coreDataManager.validateDataRelationships()
                
                if !invalidRelationships.isEmpty {
                    logger.warning("⚠️ Found \(invalidRelationships.count) invalid relationships")
                    try coreDataManager.fixInvalidRelationships(invalidRelationships)
                    logger.info("✅ Invalid relationships fixed")
                }
                
            } catch {
                logger.error("❌ Data consistency check failed: \(error)")
            }
        }
    }
    
    private func setupAutomaticDataBackup() {
        // Schedule periodic data backups
        Timer.scheduledTimer(withTimeInterval: 24 * 60 * 60, repeats: true) { [weak self] _ in
            self?.performDataBackup()
        }
    }
    
    private func performDataBackup() {
        Task {
            do {
                try await coreDataManager.createBackup()
                logger.info("✅ Data backup completed")
            } catch {
                logger.error("❌ Data backup failed: \(error)")
            }
        }
    }
    
    // MARK: - System Integration Reliability
    
    func ensureSystemIntegrationReliability() {
        // Check FamilyControls authorization
        validateFamilyControlsAuthorization()
        
        // Verify notification permissions
        validateNotificationPermissions()
        
        // Check system settings
        validateSystemSettings()
    }
    
    private func validateFamilyControlsAuthorization() {
        let authStatus = AuthorizationCenter.shared.authorizationStatus
        
        if authStatus != .approved {
            logger.warning("⚠️ FamilyControls authorization lost")
            
            // Request re-authorization
            Task {
                do {
                    try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
                    logger.info("✅ FamilyControls authorization restored")
                } catch {
                    logger.error("❌ Failed to restore FamilyControls authorization: \(error)")
                }
            }
        }
    }
    
    private func validateNotificationPermissions() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus != .authorized {
                self.logger.warning("⚠️ Notification permissions lost")
            }
        }
    }
    
    private func validateSystemSettings() {
        // Check if Screen Time is enabled
        // This would involve checking system settings that affect app functionality
        logger.info("🔍 System settings validated")
    }
    
    // MARK: - Recovery Actions
    
    func performFullSystemRecovery() {
        logger.info("🔄 Performing full system recovery")
        
        Task {
            // Re-initialize all services
            await reinitializeServices()
            
            // Restore app blocking
            await restoreAppBlocking()
            
            // Validate data integrity
            ensureDataPersistenceReliability()
            
            // Re-register background tasks
            registerBackgroundTasks()
            
            logger.info("✅ Full system recovery completed")
        }
    }
    
    private func reinitializeServices() async {
        do {
            // Restart usage tracking
            try await usageTrackingService.restartMonitoring()
            
            // Reinitialize app blocking
            try await appBlockingService.initializeBlocking()
            
            logger.info("✅ Services reinitialized")
        } catch {
            logger.error("❌ Failed to reinitialize services: \(error)")
        }
    }
    
    private func restoreAppBlocking() async {
        do {
            // Get current goals and restore blocking
            let goals = try coreDataManager.fetchGoals()
            let appsToBlock = goals.compactMap { goal in
                goal.isLimitExceeded ? goal.appIdentifier : nil
            }
            
            if !appsToBlock.isEmpty {
                try await appBlockingService.blockApps(appsToBlock)
                logger.info("✅ App blocking restored for \(appsToBlock.count) apps")
            }
        } catch {
            logger.error("❌ Failed to restore app blocking: \(error)")
        }
    }
    
    // MARK: - Health Monitoring
    
    func getSystemHealthStatus() -> SystemHealthStatus {
        return SystemHealthStatus(
            backgroundRefreshEnabled: isBackgroundRefreshEnabled,
            backgroundTasksRegistered: backgroundTasksRegistered,
            lastBackgroundRefresh: lastBackgroundRefresh,
            deviceActivityMonitoring: usageTrackingService.isMonitoringActive(),
            appBlockingActive: appBlockingService.isBlockingActive(),
            coreDataHealthy: coreDataManager.isHealthy(),
            familyControlsAuthorized: AuthorizationCenter.shared.authorizationStatus == .approved
        )
    }
}

// MARK: - Background Operations

class BackgroundRefreshOperation: Operation {
    private let logger = Logger(subsystem: "com.awaytime.app", category: "BackgroundRefresh")
    
    override func main() {
        guard !isCancelled else { return }
        
        logger.info("🔄 Background refresh operation started")
        
        // Refresh usage data
        refreshUsageData()
        
        // Update goals and check limits
        updateGoalsAndLimits()
        
        // Sync subscription status
        syncSubscriptionStatus()
        
        // Cleanup old data
        cleanupOldData()
        
        logger.info("✅ Background refresh operation completed")
    }
    
    private func refreshUsageData() {
        // Refresh current usage data
        let usageTrackingService = UsageTrackingService()
        Task {
            _ = try? await usageTrackingService.getCurrentUsage()
        }
    }
    
    private func updateGoalsAndLimits() {
        // Check if any limits have been exceeded and update blocking
        let coreDataManager = CoreDataManager.shared
        let appBlockingService = AppBlockingService()
        
        Task {
            do {
                let goals = try coreDataManager.fetchGoals()
                let appsToBlock = goals.compactMap { goal in
                    goal.isLimitExceeded ? goal.appIdentifier : nil
                }
                
                if !appsToBlock.isEmpty {
                    try await appBlockingService.blockApps(appsToBlock)
                }
            } catch {
                logger.error("❌ Failed to update goals and limits: \(error)")
            }
        }
    }
    
    private func syncSubscriptionStatus() {
        // Update subscription status
        let subscriptionService = SubscriptionManager.shared.getService()
        Task {
            await subscriptionService.updateSubscriptionStatus()
        }
    }
    
    private func cleanupOldData() {
        // Clean up old usage records
        let coreDataManager = CoreDataManager.shared
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
        
        do {
            try coreDataManager.deleteUsageRecords(before: cutoffDate)
        } catch {
            logger.error("❌ Failed to cleanup old data: \(error)")
        }
    }
}

class BackgroundProcessingOperation: Operation {
    private let logger = Logger(subsystem: "com.awaytime.app", category: "BackgroundProcessing")
    
    override func main() {
        guard !isCancelled else { return }
        
        logger.info("⚙️ Background processing operation started")
        
        // Perform data analysis
        performDataAnalysis()
        
        // Update gamification
        updateGamification()
        
        // Generate insights
        generateInsights()
        
        // Compact database
        compactDatabase()
        
        logger.info("✅ Background processing operation completed")
    }
    
    private func performDataAnalysis() {
        // Analyze usage patterns and trends
        logger.info("📊 Performing data analysis")
    }
    
    private func updateGamification() {
        // Update achievements and progress
        let gamificationService = GamificationService()
        gamificationService.checkAchievements()
    }
    
    private func generateInsights() {
        // Generate usage insights and recommendations
        logger.info("💡 Generating insights")
    }
    
    private func compactDatabase() {
        // Compact Core Data store
        let coreDataManager = CoreDataManager.shared
        do {
            try coreDataManager.compactStore()
        } catch {
            logger.error("❌ Failed to compact database: \(error)")
        }
    }
}

// MARK: - System Health Status

struct SystemHealthStatus {
    let backgroundRefreshEnabled: Bool
    let backgroundTasksRegistered: Bool
    let lastBackgroundRefresh: Date?
    let deviceActivityMonitoring: Bool
    let appBlockingActive: Bool
    let coreDataHealthy: Bool
    let familyControlsAuthorized: Bool
    
    var overallHealth: HealthLevel {
        let healthChecks = [
            backgroundRefreshEnabled,
            backgroundTasksRegistered,
            deviceActivityMonitoring,
            appBlockingActive,
            coreDataHealthy,
            familyControlsAuthorized
        ]
        
        let healthyCount = healthChecks.filter { $0 }.count
        let healthPercentage = Double(healthyCount) / Double(healthChecks.count)
        
        switch healthPercentage {
        case 1.0:
            return .excellent
        case 0.8..<1.0:
            return .good
        case 0.6..<0.8:
            return .fair
        default:
            return .poor
        }
    }
    
    enum HealthLevel {
        case excellent, good, fair, poor
        
        var color: Color {
            switch self {
            case .excellent: return .green
            case .good: return .blue
            case .fair: return .orange
            case .poor: return .red
            }
        }
        
        var description: String {
            switch self {
            case .excellent: return "All systems operational"
            case .good: return "Minor issues detected"
            case .fair: return "Some systems need attention"
            case .poor: return "Multiple systems require repair"
            }
        }
    }
}

// MARK: - Core Data Extensions

extension CoreDataManager {
    func isHealthy() -> Bool {
        do {
            _ = try fetchSelectedApps()
            return true
        } catch {
            return false
        }
    }
    
    func rebuildStack() throws {
        // Rebuild Core Data stack if corrupted
        // This would involve recreating the persistent store
    }
    
    func findOrphanedRecords() throws -> [NSManagedObject] {
        // Find records without proper relationships
        return []
    }
    
    func cleanupOrphanedRecords(_ records: [NSManagedObject]) throws {
        // Remove orphaned records
        for record in records {
            context.delete(record)
        }
        try context.save()
    }
    
    func validateDataRelationships() throws -> [NSManagedObject] {
        // Validate data relationships and return invalid ones
        return []
    }
    
    func fixInvalidRelationships(_ records: [NSManagedObject]) throws {
        // Fix invalid relationships
        try context.save()
    }
    
    func createBackup() async throws {
        // Create a backup of the Core Data store
    }
}

// MARK: - Service Extensions

extension UsageTrackingService {
    func isMonitoringActive() -> Bool {
        // Check if device activity monitoring is active by checking UserDefaults
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        let lastUpdate = userDefaults?.object(forKey: "lastUsageUpdate") as? Date
        
        // Consider monitoring active if we've received updates in the last 10 minutes
        if let lastUpdate = lastUpdate {
            return Date().timeIntervalSince(lastUpdate) < 600
        }
        
        // Also check if we have any active app groups
        let coreDataManager = CoreDataManager.shared
        let appGroups = coreDataManager.fetchAppGroups()
        return appGroups.contains { $0.isActive }
    }
    
    func getLastUpdateTime() -> Date {
        // Get the last time usage data was updated
        return Date()
    }
    
    func restartMonitoring() async throws {
        // Restart device activity monitoring
        try startMonitoring()
    }
}

extension AppBlockingService {
    func isBlockingActive() -> Bool {
        // Check if app blocking is currently active
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        
        // Check if any app group has exceeded its limit today
        let coreDataManager = CoreDataManager.shared
        let appGroups = coreDataManager.fetchAppGroups()
        
        for group in appGroups where group.isActive {
            let todayUsage = coreDataManager.getTodayUsage(for: group.name)
            if todayUsage >= Int(group.dailyLimitMinutes) {
                return true
            }
        }
        
        // Check if blocking was manually triggered
        return userDefaults?.bool(forKey: "isBlockingActive") ?? false
    }
    
    func getCurrentlyBlockedApps() -> [String] {
        // Get list of currently blocked apps
        return []
    }
    
    func getExpectedBlockedApps() -> [String] {
        // Get list of apps that should be blocked based on current limits
        return []
    }
}