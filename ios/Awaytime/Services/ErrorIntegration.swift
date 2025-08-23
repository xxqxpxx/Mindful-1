import SwiftUI
import Combine

/// Integration service that connects error handling with other app services
@MainActor
class ErrorIntegration: ObservableObject {
    private let errorService = GlobalErrorHandler.shared.getErrorService()
    private let usageTrackingService = UsageTrackingService()
    private let appBlockingService = AppBlockingService()
    private let goalTrackingService = GoalTrackingService()
    private let coreDataManager = CoreDataManager.shared
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupErrorIntegration()
        startErrorMonitoring()
    }
    
    private func setupErrorIntegration() {
        // Monitor usage tracking errors
        NotificationCenter.default.publisher(for: .usageTrackingError)
            .sink { [weak self] notification in
                if let error = notification.object as? Error {
                    self?.errorService.handleDeviceActivityError(error)
                }
            }
            .store(in: &cancellables)
        
        // Monitor blocking errors
        NotificationCenter.default.publisher(for: .appBlockingError)
            .sink { [weak self] notification in
                if let userInfo = notification.userInfo,
                   let appName = userInfo["appName"] as? String {
                    self?.errorService.handleBlockingError(appName: appName)
                }
            }
            .store(in: &cancellables)
        
        // Monitor permission changes
        NotificationCenter.default.publisher(for: .permissionStatusChanged)
            .sink { [weak self] notification in
                self?.handlePermissionStatusChange(notification)
            }
            .store(in: &cancellables)
        
        // Monitor data corruption
        NotificationCenter.default.publisher(for: .dataCorruptionDetected)
            .sink { [weak self] _ in
                self?.errorService.handleDataCorruption()
            }
            .store(in: &cancellables)
        
        // Monitor network errors
        NotificationCenter.default.publisher(for: .networkError)
            .sink { [weak self] _ in
                self?.errorService.handleNetworkError()
            }
            .store(in: &cancellables)
    }
    
    private func handlePermissionStatusChange(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let permissionType = userInfo["permissionType"] as? String,
              let isGranted = userInfo["isGranted"] as? Bool else { return }
        
        let type: ErrorHandlingService.PermissionType
        switch permissionType {
        case "familyControls":
            type = .familyControls
        case "notifications":
            type = .notifications
        case "screenTime":
            type = .screenTime
        default:
            return
        }
        
        if !isGranted {
            errorService.handlePermissionError(type, denied: false)
        }
    }
    
    private func startErrorMonitoring() {
        errorService.startErrorMonitoring()
        
        // Additional monitoring for specific services
        monitorUsageTracking()
        monitorAppBlocking()
        monitorDataIntegrity()
    }
    
    private func monitorUsageTracking() {
        Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            self?.checkUsageTrackingHealth()
        }
    }
    
    private func checkUsageTrackingHealth() {
        Task {
            do {
                _ = try await usageTrackingService.getCurrentUsage()
            } catch {
                errorService.handleDeviceActivityError(error)
            }
        }
    }
    
    private func monitorAppBlocking() {
        Timer.scheduledTimer(withTimeInterval: 120, repeats: true) { [weak self] _ in
            self?.checkAppBlockingHealth()
        }
    }
    
    private func checkAppBlockingHealth() {
        Task {
            do {
                try await appBlockingService.validateBlockingStatus()
            } catch {
                errorService.handleFamilyControlsError(error)
            }
        }
    }
    
    private func monitorDataIntegrity() {
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { [weak self] _ in
            self?.checkDataIntegrity()
        }
    }
    
    private func checkDataIntegrity() {
        do {
            // Test basic data operations
            _ = try coreDataManager.fetchSelectedApps()
            _ = try coreDataManager.fetchGoals()
            _ = try coreDataManager.fetchUsageRecords(for: Date())
        } catch {
            errorService.handleStorageError(error)
        }
    }
    
    // MARK: - Public Interface
    
    func getErrorService() -> ErrorHandlingService {
        return errorService
    }
    
    func reportError(_ error: Error, context: String) {
        print("🚨 Error reported from \(context): \(error.localizedDescription)")
        errorService.handleUnknownError(error)
    }
    
    func reportPermissionError(_ type: ErrorHandlingService.PermissionType, denied: Bool = true) {
        errorService.handlePermissionError(type, denied: denied)
    }
    
    func reportBlockingFailure(for appName: String) {
        errorService.handleBlockingError(appName: appName)
    }
    
    func reportDataCorruption() {
        errorService.handleDataCorruption()
    }
    
    func reportNetworkError() {
        errorService.handleNetworkError()
    }
}

// MARK: - Error-Aware Service Wrappers

/// Wrapper for UsageTrackingService with error handling
class SafeUsageTrackingService {
    private let usageService = UsageTrackingService()
    private let errorIntegration = ErrorIntegration()
    
    func getCurrentUsage() async -> [AppUsageData] {
        do {
            return try await usageService.getCurrentUsage()
        } catch {
            errorIntegration.reportError(error, context: "UsageTracking")
            return []
        }
    }
    
    func getTodayUsage() -> [AppUsageData] {
        do {
            return usageService.getTodayUsage()
        } catch {
            errorIntegration.reportError(error, context: "UsageTracking")
            return []
        }
    }
    
    func startMonitoring() {
        do {
            try usageService.startMonitoring()
        } catch {
            errorIntegration.reportError(error, context: "UsageTracking")
        }
    }
}

/// Wrapper for AppBlockingService with error handling
class SafeAppBlockingService {
    private let blockingService = AppBlockingService()
    private let errorIntegration = ErrorIntegration()
    
    func blockApps(_ apps: [String]) async {
        do {
            try await blockingService.blockApps(apps)
        } catch {
            // Report specific app blocking failures
            for app in apps {
                errorIntegration.reportBlockingFailure(for: app)
            }
        }
    }
    
    func unblockApps(_ apps: [String]) async {
        do {
            try await blockingService.unblockApps(apps)
        } catch {
            errorIntegration.reportError(error, context: "AppBlocking")
        }
    }
    
    func initializeBlocking() async {
        do {
            try await blockingService.initializeBlocking()
        } catch {
            errorIntegration.reportError(error, context: "AppBlocking")
        }
    }
}

/// Wrapper for GoalTrackingService with error handling
class SafeGoalTrackingService {
    private let goalService = GoalTrackingService()
    private let errorIntegration = ErrorIntegration()
    
    func saveGoal(_ goal: Goal) {
        do {
            try goalService.saveGoal(goal)
        } catch {
            errorIntegration.reportError(error, context: "GoalTracking")
        }
    }
    
    func getCurrentGoals() -> [Goal] {
        do {
            return try goalService.getCurrentGoals()
        } catch {
            errorIntegration.reportError(error, context: "GoalTracking")
            return []
        }
    }
    
    func updateGoalProgress(_ goalId: String, progress: Double) {
        do {
            try goalService.updateGoalProgress(goalId, progress: progress)
        } catch {
            errorIntegration.reportError(error, context: "GoalTracking")
        }
    }
}

// MARK: - Error Recovery Helpers

extension ErrorIntegration {
    
    /// Attempt to recover from common error scenarios
    func attemptAutoRecovery(for error: ErrorHandlingService.AwayTimeError) -> Bool {
        switch error {
        case .blockingFailed:
            return attemptBlockingRecovery()
        case .deviceActivityError, .familyControlsError:
            return attemptSystemServiceRecovery()
        case .storageError:
            return attemptStorageRecovery()
        default:
            return false
        }
    }
    
    private func attemptBlockingRecovery() -> Bool {
        Task {
            do {
                try await appBlockingService.initializeBlocking()
                errorService.clearError()
                return true
            } catch {
                return false
            }
        }
        return false
    }
    
    private func attemptSystemServiceRecovery() -> Bool {
        Task {
            do {
                // Try to restart monitoring
                try usageTrackingService.startMonitoring()
                errorService.clearError()
                return true
            } catch {
                return false
            }
        }
        return false
    }
    
    private func attemptStorageRecovery() -> Bool {
        do {
            // Try to compact Core Data store
            try coreDataManager.compactStore()
            errorService.clearError()
            return true
        } catch {
            return false
        }
    }
}

// MARK: - Error Analytics

extension ErrorIntegration {
    
    func trackErrorMetrics() {
        // In production, this would send error metrics to analytics
        let errorCounts = getErrorCounts()
        print("📊 Error metrics: \(errorCounts)")
    }
    
    private func getErrorCounts() -> [String: Int] {
        // This would track error frequencies
        return [
            "permission_errors": 0,
            "blocking_errors": 0,
            "data_errors": 0,
            "network_errors": 0,
            "unknown_errors": 0
        ]
    }
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let usageTrackingError = Notification.Name("usageTrackingError")
    static let appBlockingError = Notification.Name("appBlockingError")
    static let permissionStatusChanged = Notification.Name("permissionStatusChanged")
    static let dataCorruptionDetected = Notification.Name("dataCorruptionDetected")
    static let networkError = Notification.Name("networkError")
}

// MARK: - Error Context

struct ErrorContext {
    let service: String
    let operation: String
    let timestamp: Date
    let additionalInfo: [String: Any]
    
    init(service: String, operation: String, additionalInfo: [String: Any] = [:]) {
        self.service = service
        self.operation = operation
        self.timestamp = Date()
        self.additionalInfo = additionalInfo
    }
}

// MARK: - Error Reporting Helper

class ErrorReporter {
    static let shared = ErrorReporter()
    private let errorIntegration = ErrorIntegration()
    
    private init() {}
    
    func report(_ error: Error, context: ErrorContext) {
        let contextString = "\(context.service).\(context.operation)"
        errorIntegration.reportError(error, context: contextString)
        
        // Log additional context
        print("🔍 Error context: \(context)")
    }
    
    func reportPermissionDenied(_ type: ErrorHandlingService.PermissionType) {
        errorIntegration.reportPermissionError(type, denied: true)
    }
    
    func reportPermissionRevoked(_ type: ErrorHandlingService.PermissionType) {
        errorIntegration.reportPermissionError(type, denied: false)
    }
}