import Foundation
import SwiftUI

/// Service to validate and ensure all critical user settings and permissions are preserved
@MainActor
class SettingsValidationService: ObservableObject {
    static let shared = SettingsValidationService()
    
    @Published var lastValidationDate: Date?
    @Published var validationIssuesFound: [ValidationIssue] = []
    
    private init() {
        loadLastValidationDate()
    }
    
    struct ValidationIssue {
        let type: IssueType
        let description: String
        let severity: Severity
        let resolution: String
        
        enum IssueType {
            case missingPermission
            case corruptedData
            case missingSettings
            case inconsistentData
        }
        
        enum Severity {
            case critical
            case warning
            case info
        }
    }
    
    // MARK: - Validation Methods
    
    func performComprehensiveValidation() async {
        print("🔍 Starting comprehensive settings validation...")
        validationIssuesFound.removeAll()
        
        // 1. Validate permissions
        await validatePermissions()
        
        // 2. Validate Core Data integrity
        validateCoreDataIntegrity()
        
        // 3. Validate UserDefaults consistency
        validateUserDefaultsConsistency()
        
        // 4. Validate app group settings
        validateAppGroupSettings()
        
        // 5. Validate subscription status
        await validateSubscriptionStatus()
        
        // 6. Cross-validate data consistency
        validateDataConsistency()
        
        lastValidationDate = Date()
        saveLastValidationDate()
        
        print("✅ Comprehensive validation completed. Issues found: \(validationIssuesFound.count)")
        
        // Auto-resolve critical issues
        await autoResolveCriticalIssues()
    }
    
    private func validatePermissions() async {
        let permissionService = PermissionService()
        
        // Check if we should have permissions but don't
        let hasOnboardingCompleted = UserDefaults.standard.bool(forKey: "onboarding_completed")
        let hasActiveAppGroups = !CoreDataManager.shared.fetchAppGroups().isEmpty
        
        if hasOnboardingCompleted && hasActiveAppGroups {
            if permissionService.authorizationStatus != .approved {
                validationIssuesFound.append(ValidationIssue(
                    type: .missingPermission,
                    description: "FamilyControls permission lost after onboarding completion",
                    severity: .critical,
                    resolution: "Re-request permission from user"
                ))
            }
        }
    }
    
    private func validateCoreDataIntegrity() {
        let coreDataManager = CoreDataManager.shared
        
        if !coreDataManager.validateDataIntegrity() {
            validationIssuesFound.append(ValidationIssue(
                type: .corruptedData,
                description: "Core Data integrity check failed",
                severity: .critical,
                resolution: "Perform data recovery and backup restoration"
            ))
        }
        
        // Check for essential entities
        let userSettings = coreDataManager.getUserSettings()
        if userSettings.id == nil {
            validationIssuesFound.append(ValidationIssue(
                type: .missingSettings,
                description: "User settings entity missing or corrupted",
                severity: .critical,
                resolution: "Recreate user settings with defaults"
            ))
        }
    }
    
    private func validateUserDefaultsConsistency() {
        let criticalKeys = [
            "onboarding_completed",
            "isPremiumUser",
            "selectedAppNames"
        ]
        
        for key in criticalKeys {
            if UserDefaults.standard.object(forKey: key) == nil &&
               UserDefaults.standard.bool(forKey: "onboarding_completed") {
                validationIssuesFound.append(ValidationIssue(
                    type: .missingSettings,
                    description: "Critical UserDefaults key '\(key)' is missing",
                    severity: .warning,
                    resolution: "Restore from backup or recreate with defaults"
                ))
            }
        }
        
        // Check app group suite access
        let sharedDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        if sharedDefaults == nil {
            validationIssuesFound.append(ValidationIssue(
                type: .missingSettings,
                description: "App group UserDefaults access failed",
                severity: .critical,
                resolution: "Check app group entitlements configuration"
            ))
        }
    }
    
    private func validateAppGroupSettings() {
        let appGroups = CoreDataManager.shared.fetchAppGroups()
        
        if appGroups.isEmpty {
            validationIssuesFound.append(ValidationIssue(
                type: .missingSettings,
                description: "No app groups configured",
                severity: .warning,
                resolution: "Create default app group"
            ))
        }
        
        // Check for inconsistencies in app group data
        let activeGroups = appGroups.filter { $0.isActive }
        if activeGroups.count > 1 && !SubscriptionManager.shared.isPremiumActive() {
            validationIssuesFound.append(ValidationIssue(
                type: .inconsistentData,
                description: "Multiple active app groups without premium subscription",
                severity: .warning,
                resolution: "Deactivate extra groups or validate premium status"
            ))
        }
    }
    
    private func validateSubscriptionStatus() async {
        // Check consistency between different subscription status sources
        let userDefaultsPremium = UserDefaults.standard.bool(forKey: "isPremiumUser")
        let coreDataPremium = CoreDataManager.shared.getUserSettings().isPremium
        let subscriptionServicePremium = SubscriptionManager.shared.isPremiumActive()
        
        if userDefaultsPremium != coreDataPremium {
            validationIssuesFound.append(ValidationIssue(
                type: .inconsistentData,
                description: "Premium status inconsistent between UserDefaults and Core Data",
                severity: .warning,
                resolution: "Sync premium status across storage systems"
            ))
        }
        
        if userDefaultsPremium != subscriptionServicePremium {
            validationIssuesFound.append(ValidationIssue(
                type: .inconsistentData,
                description: "Premium status inconsistent with subscription service",
                severity: .warning,
                resolution: "Validate with StoreKit and update local status"
            ))
        }
    }
    
    private func validateDataConsistency() {
        // Cross-validate usage records with app groups
        let appGroups = CoreDataManager.shared.fetchAppGroups()
        let recentRecords = CoreDataManager.shared.getUsageRecords(days: 1)
        
        for record in recentRecords {
            let groupExists = appGroups.contains { $0.name == record.appGroupName }
            if !groupExists {
                validationIssuesFound.append(ValidationIssue(
                    type: .inconsistentData,
                    description: "Usage record references non-existent app group: \(record.appGroupName ?? "unknown")",
                    severity: .info,
                    resolution: "Clean up orphaned usage records"
                ))
            }
        }
    }
    
    // MARK: - Auto-Resolution
    
    private func autoResolveCriticalIssues() async {
        for issue in validationIssuesFound where issue.severity == .critical {
            await autoResolveIssue(issue)
        }
    }
    
    private func autoResolveIssue(_ issue: ValidationIssue) async {
        print("🔧 Auto-resolving critical issue: \(issue.description)")
        
        switch issue.type {
        case .missingPermission:
            let permissionService = PermissionService()
            await permissionService.attemptPermissionRecovery()
            
        case .corruptedData:
            CoreDataManager.shared.performDataMaintenance()
            
        case .missingSettings:
            await recreateMissingSettings()
            
        case .inconsistentData:
            await synchronizeInconsistentData()
        }
    }
    
    private func recreateMissingSettings() async {
        // Ensure user settings exist
        let _ = CoreDataManager.shared.getUserSettings()
        
        // Ensure at least one app group exists
        let appGroups = CoreDataManager.shared.fetchAppGroups()
        if appGroups.isEmpty {
            let _ = CoreDataManager.shared.createAppGroup(
                name: "My Apps",
                dailyLimitMinutes: 120,
                isActive: true
            )
        }
        
        // Restore critical UserDefaults if missing
        if !UserDefaults.standard.bool(forKey: "onboarding_completed") &&
           !appGroups.isEmpty {
            UserDefaults.standard.set(true, forKey: "onboarding_completed")
            print("✅ Restored onboarding completion status")
        }
    }
    
    private func synchronizeInconsistentData() async {
        // Sync premium status using subscription service as source of truth
        let actualPremiumStatus = SubscriptionManager.shared.isPremiumActive()
        
        UserDefaults.standard.set(actualPremiumStatus, forKey: "isPremiumUser")
        CoreDataManager.shared.updateUserSettings(isPremium: actualPremiumStatus)
        
        print("✅ Synchronized premium status to: \(actualPremiumStatus)")
    }
    
    // MARK: - Persistence
    
    private func loadLastValidationDate() {
        if let date = UserDefaults.standard.object(forKey: "lastSettingsValidation") as? Date {
            lastValidationDate = date
        }
    }
    
    private func saveLastValidationDate() {
        UserDefaults.standard.set(lastValidationDate, forKey: "lastSettingsValidation")
    }
    
    // MARK: - Public Interface
    
    func shouldPerformValidation() -> Bool {
        guard let lastValidation = lastValidationDate else { return true }
        
        // Perform validation daily
        let dayAgo = Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date()
        return lastValidation < dayAgo
    }
    
    func getValidationSummary() -> String {
        let criticalCount = validationIssuesFound.filter { $0.severity == .critical }.count
        let warningCount = validationIssuesFound.filter { $0.severity == .warning }.count
        let infoCount = validationIssuesFound.filter { $0.severity == .info }.count
        
        return "Critical: \(criticalCount), Warnings: \(warningCount), Info: \(infoCount)"
    }
}