import SwiftUI
import FamilyControls
import DeviceActivity

/// Comprehensive error handling service for Awaytime
@MainActor
class ErrorHandlingService: ObservableObject {
    @Published var currentError: AwayTimeError?
    @Published var showingErrorAlert = false
    @Published var showingRecoveryOptions = false
    
    private let notificationService = NotificationService()
    
    // MARK: - Error Types
    
    enum AwayTimeError: LocalizedError, Identifiable {
        case permissionDenied(PermissionType)
        case permissionRevoked(PermissionType)
        case blockingFailed(String)
        case dataCorruption
        case networkUnavailable
        case subscriptionError(String)
        case deviceActivityError(String)
        case familyControlsError(String)
        case storageError(String)
        case unknownError(String)
        
        var id: String {
            switch self {
            case .permissionDenied(let type): return "permission_denied_\(type.rawValue)"
            case .permissionRevoked(let type): return "permission_revoked_\(type.rawValue)"
            case .blockingFailed: return "blocking_failed"
            case .dataCorruption: return "data_corruption"
            case .networkUnavailable: return "network_unavailable"
            case .subscriptionError: return "subscription_error"
            case .deviceActivityError: return "device_activity_error"
            case .familyControlsError: return "family_controls_error"
            case .storageError: return "storage_error"
            case .unknownError: return "unknown_error"
            }
        }
        
        var errorDescription: String? {
            switch self {
            case .permissionDenied(let type):
                return "Permission Required"
            case .permissionRevoked(let type):
                return "Permission Lost"
            case .blockingFailed:
                return "App Blocking Failed"
            case .dataCorruption:
                return "Data Issue Detected"
            case .networkUnavailable:
                return "No Internet Connection"
            case .subscriptionError:
                return "Subscription Issue"
            case .deviceActivityError:
                return "Monitoring Issue"
            case .familyControlsError:
                return "System Integration Issue"
            case .storageError:
                return "Storage Issue"
            case .unknownError:
                return "Unexpected Error"
            }
        }
        
        var failureReason: String? {
            switch self {
            case .permissionDenied(let type):
                return getPermissionDeniedMessage(for: type)
            case .permissionRevoked(let type):
                return getPermissionRevokedMessage(for: type)
            case .blockingFailed(let appName):
                return "Unable to block \(appName). The app may still be accessible."
            case .dataCorruption:
                return "Some of your data appears to be corrupted. We can help restore it."
            case .networkUnavailable:
                return "Some features require an internet connection to work properly."
            case .subscriptionError(let message):
                return message
            case .deviceActivityError(let message):
                return "Usage tracking may not work correctly: \(message)"
            case .familyControlsError(let message):
                return "App blocking may not work correctly: \(message)"
            case .storageError(let message):
                return "Unable to save your data: \(message)"
            case .unknownError(let message):
                return message
            }
        }
        
        var recoverySuggestion: String? {
            switch self {
            case .permissionDenied(let type):
                return getPermissionRecoverySuggestion(for: type)
            case .permissionRevoked(let type):
                return "Please re-grant permission in Settings to continue using Awaytime."
            case .blockingFailed:
                return "Try restarting the app or check your Screen Time settings."
            case .dataCorruption:
                return "We can reset your data to fix this issue. Your app selections will be preserved."
            case .networkUnavailable:
                return "Check your internet connection and try again."
            case .subscriptionError:
                return "Please check your subscription status or contact support."
            case .deviceActivityError:
                return "Try restarting the app or re-granting Screen Time permissions."
            case .familyControlsError:
                return "Check your Screen Time settings and ensure Awaytime has proper permissions."
            case .storageError:
                return "Free up some storage space or restart the app."
            case .unknownError:
                return "Please try restarting the app. If the problem persists, contact support."
            }
        }
        
        var severity: ErrorSeverity {
            switch self {
            case .permissionDenied, .permissionRevoked:
                return .critical
            case .blockingFailed, .deviceActivityError, .familyControlsError:
                return .high
            case .dataCorruption, .storageError:
                return .medium
            case .networkUnavailable, .subscriptionError:
                return .low
            case .unknownError:
                return .medium
            }
        }
        
        var icon: String {
            switch self {
            case .permissionDenied, .permissionRevoked:
                return "🔒"
            case .blockingFailed:
                return "🚫"
            case .dataCorruption, .storageError:
                return "💾"
            case .networkUnavailable:
                return "📡"
            case .subscriptionError:
                return "💳"
            case .deviceActivityError, .familyControlsError:
                return "⚙️"
            case .unknownError:
                return "⚠️"
            }
        }
    }
    
    enum PermissionType: String, CaseIterable {
        case familyControls = "family_controls"
        case notifications = "notifications"
        case screenTime = "screen_time"
        
        var displayName: String {
            switch self {
            case .familyControls: return "Screen Time"
            case .notifications: return "Notifications"
            case .screenTime: return "Screen Time Access"
            }
        }
    }
    
    enum ErrorSeverity {
        case low, medium, high, critical
        
        var color: Color {
            switch self {
            case .low: return .blue
            case .medium: return .orange
            case .high: return .red
            case .critical: return .purple
            }
        }
    }
    
    // MARK: - Error Handling Methods
    
    func handleError(_ error: AwayTimeError) {
        currentError = error
        showingErrorAlert = true
        
        // Log error for debugging
        logError(error)
        
        // Send analytics if needed
        trackError(error)
        
        // Show notification for critical errors
        if error.severity == .critical {
            sendCriticalErrorNotification(error)
        }
        
        print("🚨 Error handled: \(error.errorDescription ?? "Unknown")")
    }
    
    func handlePermissionError(_ type: PermissionType, denied: Bool = true) {
        let error: AwayTimeError = denied ? .permissionDenied(type) : .permissionRevoked(type)
        handleError(error)
    }
    
    func handleBlockingError(appName: String) {
        handleError(.blockingFailed(appName))
    }
    
    func handleDataCorruption() {
        handleError(.dataCorruption)
    }
    
    func handleNetworkError() {
        handleError(.networkUnavailable)
    }
    
    func handleSubscriptionError(_ message: String) {
        handleError(.subscriptionError(message))
    }
    
    func handleDeviceActivityError(_ error: Error) {
        let message = error.localizedDescription
        handleError(.deviceActivityError(message))
    }
    
    func handleFamilyControlsError(_ error: Error) {
        let message = error.localizedDescription
        handleError(.familyControlsError(message))
    }
    
    func handleStorageError(_ error: Error) {
        let message = error.localizedDescription
        handleError(.storageError(message))
    }
    
    func handleUnknownError(_ error: Error) {
        let message = error.localizedDescription
        handleError(.unknownError(message))
    }
    
    // MARK: - Recovery Actions
    
    func performRecoveryAction(for error: AwayTimeError) {
        switch error {
        case .permissionDenied(let type), .permissionRevoked(let type):
            openPermissionSettings(for: type)
            
        case .blockingFailed:
            attemptBlockingRecovery()
            
        case .dataCorruption:
            showingRecoveryOptions = true
            
        case .networkUnavailable:
            // No action needed, user should check connection
            break
            
        case .subscriptionError:
            openSubscriptionSettings()
            
        case .deviceActivityError, .familyControlsError:
            attemptSystemRecovery()
            
        case .storageError:
            showStorageGuidance()
            
        case .unknownError:
            restartApp()
        }
    }
    
    private func openPermissionSettings(for type: PermissionType) {
        switch type {
        case .familyControls, .screenTime:
            if let settingsUrl = URL(string: "App-prefs:SCREEN_TIME") {
                UIApplication.shared.open(settingsUrl)
            }
        case .notifications:
            if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(settingsUrl)
            }
        }
    }
    
    private func attemptBlockingRecovery() {
        // Try to re-initialize blocking service
        Task {
            do {
                let blockingService = AppBlockingService()
                try await blockingService.initializeBlocking()
                clearError()
                print("✅ Blocking recovery successful")
            } catch {
                print("❌ Blocking recovery failed: \(error)")
            }
        }
    }
    
    private func attemptSystemRecovery() {
        // Try to re-request permissions
        Task {
            do {
                try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
                clearError()
                print("✅ System recovery successful")
            } catch {
                print("❌ System recovery failed: \(error)")
            }
        }
    }
    
    private func openSubscriptionSettings() {
        // Open subscription management
        if let url = URL(string: "https://apps.apple.com/account/subscriptions") {
            UIApplication.shared.open(url)
        }
    }
    
    private func showStorageGuidance() {
        // This would show a guide on freeing up storage
        print("💾 Storage guidance shown")
    }
    
    private func restartApp() {
        // This would trigger app restart flow
        print("🔄 App restart requested")
    }
    
    // MARK: - Data Recovery
    
    func performDataReset() {
        let coreDataManager = CoreDataManager.shared
        
        // Backup essential data
        let selectedApps = coreDataManager.fetchSelectedApps()
        let currentGoals = coreDataManager.fetchGoals()
        
        // Clear corrupted data
        coreDataManager.clearAllData()
        
        // Restore essential data
        for app in selectedApps {
            coreDataManager.saveSelectedApp(app)
        }
        
        for goal in currentGoals {
            coreDataManager.saveGoal(goal)
        }
        
        clearError()
        print("🔄 Data reset completed successfully")
    }
    
    func performFullReset() {
        let coreDataManager = CoreDataManager.shared
        coreDataManager.clearAllData()
        
        // Reset user defaults
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: "hasCompletedOnboarding")
        defaults.removeObject(forKey: "gamificationData")
        defaults.removeObject(forKey: "userPreferences")
        
        clearError()
        print("🔄 Full reset completed")
    }
    
    // MARK: - Error Utilities
    
    func clearError() {
        currentError = nil
        showingErrorAlert = false
        showingRecoveryOptions = false
    }
    
    private func logError(_ error: AwayTimeError) {
        let timestamp = Date().formatted()
        let logEntry = "[\(timestamp)] ERROR: \(error.id) - \(error.errorDescription ?? "Unknown")"
        
        // In production, this would go to a proper logging service
        print("📝 \(logEntry)")
    }
    
    private func trackError(_ error: AwayTimeError) {
        // In production, this would send to analytics
        print("📊 Error tracked: \(error.id)")
    }
    
    private func sendCriticalErrorNotification(_ error: AwayTimeError) {
        notificationService.sendNotification(
            title: "Awaytime Needs Attention",
            message: error.errorDescription ?? "Please open the app to resolve an issue.",
            channelId: "critical_errors"
        )
    }
    
    // MARK: - Permission Helper Messages
    
    private func getPermissionDeniedMessage(for type: PermissionType) -> String {
        switch type {
        case .familyControls:
            return "Awaytime needs Screen Time permission to track your app usage and help you stay focused."
        case .notifications:
            return "Notifications help you stay on track with gentle reminders and achievement celebrations."
        case .screenTime:
            return "Screen Time access is required for Awaytime to monitor and limit your app usage."
        }
    }
    
    private func getPermissionRevokedMessage(for type: PermissionType) -> String {
        switch type {
        case .familyControls:
            return "Screen Time permission was revoked. Awaytime can't track usage without this permission."
        case .notifications:
            return "Notification permission was disabled. You won't receive usage alerts or achievement notifications."
        case .screenTime:
            return "Screen Time access was removed. App blocking and usage tracking won't work."
        }
    }
    
    private func getPermissionRecoverySuggestion(for type: PermissionType) -> String {
        switch type {
        case .familyControls:
            return "Go to Settings > Screen Time > Content & Privacy Restrictions and enable Awaytime."
        case .notifications:
            return "Go to Settings > Notifications > Awaytime and enable notifications."
        case .screenTime:
            return "Go to Settings > Screen Time and ensure Awaytime has proper access."
        }
    }
}

// MARK: - Error Monitoring Extension

extension ErrorHandlingService {
    /// Monitor for common error conditions
    func startErrorMonitoring() {
        // Monitor permission status changes
        Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.checkPermissionStatus()
        }
        
        // Monitor system health
        Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            self?.checkSystemHealth()
        }
    }
    
    private func checkPermissionStatus() {
        Task {
            let authStatus = AuthorizationCenter.shared.authorizationStatus
            if authStatus != .approved {
                handlePermissionError(.familyControls, denied: false)
            }
        }
    }
    
    private func checkSystemHealth() {
        // Check if core services are working
        let coreDataManager = CoreDataManager.shared
        
        do {
            _ = try coreDataManager.fetchSelectedApps()
        } catch {
            handleStorageError(error)
        }
    }
}

// MARK: - Global Error Handler

class GlobalErrorHandler {
    static let shared = GlobalErrorHandler()
    private let errorService = ErrorHandlingService()
    
    private init() {
        setupGlobalErrorHandling()
    }
    
    private func setupGlobalErrorHandling() {
        // Catch unhandled exceptions
        NSSetUncaughtExceptionHandler { exception in
            GlobalErrorHandler.shared.handleException(exception)
        }
    }
    
    private func handleException(_ exception: NSException) {
        let error = AwayTimeError.unknownError(exception.reason ?? "Unknown exception")
        errorService.handleError(error)
    }
    
    func getErrorService() -> ErrorHandlingService {
        return errorService
    }
}