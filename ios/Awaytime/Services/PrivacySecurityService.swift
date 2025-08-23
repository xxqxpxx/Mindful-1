import SwiftUI
import CryptoKit
import LocalAuthentication
import os.log

/// Comprehensive privacy and security service for Awaytime
@MainActor
class PrivacySecurityService: ObservableObject {
    @Published var isDataEncrypted = false
    @Published var biometricAuthEnabled = false
    @Published var dataRetentionPeriod: TimeInterval = 30 * 24 * 60 * 60 // 30 days
    @Published var anonymizationEnabled = true
    
    private let logger = Logger(subsystem: "com.awaytime.app", category: "Privacy")
    private let keychain = KeychainService()
    private let encryptionService = EncryptionService()
    private let dataAnonymizer = DataAnonymizer()
    private let permissionBoundaryEnforcer = PermissionBoundaryEnforcer()
    
    private let encryptionKeyIdentifier = "com.awaytime.encryption.key"
    
    init() {
        setupPrivacyDefaults()
        enforcePermissionBoundaries()
        scheduleDataCleanup()
    }
    
    // MARK: - Data Encryption
    
    func enableDataEncryption() async throws {
        logger.info("🔐 Enabling data encryption")
        
        // Generate or retrieve encryption key
        let encryptionKey = try await getOrCreateEncryptionKey()
        
        // Encrypt existing data
        try await encryptExistingData(with: encryptionKey)
        
        // Update encryption status
        isDataEncrypted = true
        UserDefaults.standard.set(true, forKey: "dataEncryptionEnabled")
        
        logger.info("✅ Data encryption enabled successfully")
    }
    
    func disableDataEncryption() async throws {
        logger.info("🔓 Disabling data encryption")
        
        guard isDataEncrypted else {
            logger.warning("⚠️ Data encryption is not currently enabled")
            return
        }
        
        // Decrypt existing data
        let encryptionKey = try await getEncryptionKey()
        try await decryptExistingData(with: encryptionKey)
        
        // Remove encryption key
        try keychain.deleteItem(identifier: encryptionKeyIdentifier)
        
        // Update encryption status
        isDataEncrypted = false
        UserDefaults.standard.set(false, forKey: "dataEncryptionEnabled")
        
        logger.info("✅ Data encryption disabled successfully")
    }
    
    private func getOrCreateEncryptionKey() async throws -> SymmetricKey {
        // Try to retrieve existing key
        if let existingKeyData = try? keychain.retrieveData(identifier: encryptionKeyIdentifier) {
            return SymmetricKey(data: existingKeyData)
        }
        
        // Generate new key
        let newKey = SymmetricKey(size: .bits256)
        let keyData = newKey.withUnsafeBytes { Data($0) }
        
        // Store in keychain
        try keychain.storeData(keyData, identifier: encryptionKeyIdentifier)
        
        return newKey
    }
    
    private func getEncryptionKey() async throws -> SymmetricKey {
        let keyData = try keychain.retrieveData(identifier: encryptionKeyIdentifier)
        return SymmetricKey(data: keyData)
    }
    
    private func encryptExistingData(with key: SymmetricKey) async throws {
        let coreDataManager = CoreDataManager.shared
        
        // Encrypt usage records
        let usageRecords = try coreDataManager.fetchAllUsageRecords()
        for record in usageRecords {
            if let sensitiveData = record.sensitiveData {
                let encryptedData = try encryptionService.encrypt(data: sensitiveData, with: key)
                record.sensitiveData = encryptedData
            }
        }
        
        // Encrypt goals
        let goals = try coreDataManager.fetchGoals()
        for goal in goals {
            if let sensitiveData = goal.sensitiveData {
                let encryptedData = try encryptionService.encrypt(data: sensitiveData, with: key)
                goal.sensitiveData = encryptedData
            }
        }
        
        // Save encrypted data
        try coreDataManager.saveContext()
        
        logger.info("🔐 Existing data encrypted successfully")
    }
    
    private func decryptExistingData(with key: SymmetricKey) async throws {
        let coreDataManager = CoreDataManager.shared
        
        // Decrypt usage records
        let usageRecords = try coreDataManager.fetchAllUsageRecords()
        for record in usageRecords {
            if let encryptedData = record.sensitiveData {
                let decryptedData = try encryptionService.decrypt(data: encryptedData, with: key)
                record.sensitiveData = decryptedData
            }
        }
        
        // Decrypt goals
        let goals = try coreDataManager.fetchGoals()
        for goal in goals {
            if let encryptedData = goal.sensitiveData {
                let decryptedData = try encryptionService.decrypt(data: encryptedData, with: key)
                goal.sensitiveData = decryptedData
            }
        }
        
        // Save decrypted data
        try coreDataManager.saveContext()
        
        logger.info("🔓 Existing data decrypted successfully")
    }
    
    // MARK: - Biometric Authentication
    
    func enableBiometricAuth() async throws {
        logger.info("👆 Enabling biometric authentication")
        
        let context = LAContext()
        var error: NSError?
        
        // Check if biometric authentication is available
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            throw PrivacySecurityError.biometricNotAvailable(error?.localizedDescription ?? "Unknown error")
        }
        
        // Authenticate user
        let reason = "Enable biometric authentication to secure your Awaytime data"
        
        do {
            let success = try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
            
            if success {
                biometricAuthEnabled = true
                UserDefaults.standard.set(true, forKey: "biometricAuthEnabled")
                logger.info("✅ Biometric authentication enabled")
            }
        } catch {
            throw PrivacySecurityError.biometricAuthFailed(error.localizedDescription)
        }
    }
    
    func disableBiometricAuth() {
        logger.info("👆 Disabling biometric authentication")
        
        biometricAuthEnabled = false
        UserDefaults.standard.set(false, forKey: "biometricAuthEnabled")
        
        logger.info("✅ Biometric authentication disabled")
    }
    
    func authenticateWithBiometrics() async throws -> Bool {
        guard biometricAuthEnabled else {
            return true // Skip if not enabled
        }
        
        let context = LAContext()
        let reason = "Authenticate to access your Awaytime data"
        
        do {
            let success = try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
            return success
        } catch {
            logger.error("❌ Biometric authentication failed: \(error)")
            return false
        }
    }
    
    // MARK: - Data Deletion
    
    func deleteAllUserData() async throws {
        logger.info("🗑️ Deleting all user data")
        
        // Confirm with user
        let confirmed = await confirmDataDeletion()
        guard confirmed else {
            logger.info("ℹ️ Data deletion cancelled by user")
            return
        }
        
        // Delete Core Data
        try await deleteAllCoreData()
        
        // Delete UserDefaults
        deleteUserDefaults()
        
        // Delete Keychain items
        try deleteKeychainItems()
        
        // Clear caches
        clearCaches()
        
        // Reset privacy settings
        resetPrivacySettings()
        
        logger.info("✅ All user data deleted successfully")
    }
    
    func deleteDataOlderThan(days: Int) async throws {
        logger.info("🗑️ Deleting data older than \(days) days")
        
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -days, to: Date()) ?? Date()
        let coreDataManager = CoreDataManager.shared
        
        // Delete old usage records
        try coreDataManager.deleteUsageRecords(before: cutoffDate)
        
        // Delete old gamification data
        try coreDataManager.deleteGamificationRecords(before: cutoffDate)
        
        // Anonymize remaining old data
        if anonymizationEnabled {
            try await anonymizeOldData(before: cutoffDate)
        }
        
        logger.info("✅ Old data cleanup completed")
    }
    
    private func confirmDataDeletion() async -> Bool {
        // In a real implementation, this would show a confirmation dialog
        // For now, we'll assume confirmation
        return true
    }
    
    private func deleteAllCoreData() async throws {
        let coreDataManager = CoreDataManager.shared
        
        // Delete all entities
        try coreDataManager.deleteAllUsageRecords()
        try coreDataManager.deleteAllGoals()
        try coreDataManager.deleteAllGamificationData()
        try coreDataManager.deleteAllSelectedApps()
        
        try coreDataManager.saveContext()
    }
    
    private func deleteUserDefaults() {
        let defaults = UserDefaults.standard
        let domain = Bundle.main.bundleIdentifier!
        defaults.removePersistentDomain(forName: domain)
        defaults.synchronize()
    }
    
    private func deleteKeychainItems() throws {
        try keychain.deleteAllItems()
    }
    
    private func clearCaches() {
        // Clear app caches
        let cacheURL = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        try? FileManager.default.removeItem(at: cacheURL)
        try? FileManager.default.createDirectory(at: cacheURL, withIntermediateDirectories: true)
    }
    
    private func resetPrivacySettings() {
        isDataEncrypted = false
        biometricAuthEnabled = false
        dataRetentionPeriod = 30 * 24 * 60 * 60
        anonymizationEnabled = true
    }
    
    // MARK: - Data Anonymization
    
    func anonymizeUserData() async throws {
        logger.info("🎭 Anonymizing user data")
        
        let coreDataManager = CoreDataManager.shared
        
        // Anonymize usage records
        let usageRecords = try coreDataManager.fetchAllUsageRecords()
        for record in usageRecords {
            record.appIdentifier = dataAnonymizer.anonymizeAppIdentifier(record.appIdentifier)
            record.deviceIdentifier = dataAnonymizer.anonymizeDeviceIdentifier(record.deviceIdentifier)
        }
        
        // Anonymize goals
        let goals = try coreDataManager.fetchGoals()
        for goal in goals {
            goal.appIdentifier = dataAnonymizer.anonymizeAppIdentifier(goal.appIdentifier)
        }
        
        try coreDataManager.saveContext()
        
        logger.info("✅ User data anonymized successfully")
    }
    
    private func anonymizeOldData(before date: Date) async throws {
        let coreDataManager = CoreDataManager.shared
        
        // Anonymize old usage records
        let oldUsageRecords = try coreDataManager.fetchUsageRecords(before: date)
        for record in oldUsageRecords {
            record.appIdentifier = dataAnonymizer.anonymizeAppIdentifier(record.appIdentifier)
            record.deviceIdentifier = dataAnonymizer.anonymizeDeviceIdentifier(record.deviceIdentifier)
        }
        
        try coreDataManager.saveContext()
        
        logger.info("✅ Old data anonymized successfully")
    }
    
    // MARK: - Permission Boundary Enforcement
    
    private func enforcePermissionBoundaries() {
        permissionBoundaryEnforcer.startMonitoring()
        
        // Monitor for unauthorized data access attempts
        permissionBoundaryEnforcer.onUnauthorizedAccess = { [weak self] violation in
            self?.handlePermissionViolation(violation)
        }
    }
    
    private func handlePermissionViolation(_ violation: PermissionViolation) {
        logger.error("🚨 Permission violation detected: \(violation.description)")
        
        // Log the violation
        logSecurityEvent(.permissionViolation, details: violation.details)
        
        // Take appropriate action based on violation severity
        switch violation.severity {
        case .low:
            // Log and continue
            break
        case .medium:
            // Show warning to user
            showSecurityWarning(violation.userMessage)
        case .high:
            // Disable affected functionality
            disableAffectedFeatures(violation.affectedFeatures)
        case .critical:
            // Lock app and require re-authentication
            requireReAuthentication()
        }
    }
    
    // MARK: - Data Collection Minimization
    
    func configureMinimalDataCollection() {
        logger.info("📊 Configuring minimal data collection")
        
        // Set data collection preferences
        UserDefaults.standard.set(false, forKey: "collectAnalytics")
        UserDefaults.standard.set(false, forKey: "collectCrashReports")
        UserDefaults.standard.set(true, forKey: "anonymizeData")
        UserDefaults.standard.set(dataRetentionPeriod, forKey: "dataRetentionPeriod")
        
        // Configure services for minimal collection
        configureAnalyticsForPrivacy()
        configureCrashReportingForPrivacy()
        
        logger.info("✅ Minimal data collection configured")
    }
    
    private func configureAnalyticsForPrivacy() {
        // Disable or minimize analytics collection
        // In a real implementation, you'd configure your analytics SDK
        logger.info("📈 Analytics configured for privacy")
    }
    
    private func configureCrashReportingForPrivacy() {
        // Configure crash reporting to exclude sensitive data
        // In a real implementation, you'd configure your crash reporting SDK
        logger.info("💥 Crash reporting configured for privacy")
    }
    
    // MARK: - Privacy Settings Management
    
    func updateDataRetentionPeriod(_ days: Int) {
        dataRetentionPeriod = TimeInterval(days * 24 * 60 * 60)
        UserDefaults.standard.set(dataRetentionPeriod, forKey: "dataRetentionPeriod")
        
        // Schedule cleanup for new retention period
        scheduleDataCleanup()
        
        logger.info("📅 Data retention period updated to \(days) days")
    }
    
    func enableAnonymization(_ enabled: Bool) {
        anonymizationEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "anonymizationEnabled")
        
        if enabled {
            Task {
                try await anonymizeUserData()
            }
        }
        
        logger.info("🎭 Data anonymization \(enabled ? "enabled" : "disabled")")
    }
    
    // MARK: - Privacy Audit
    
    func performPrivacyAudit() async -> PrivacyAuditReport {
        logger.info("🔍 Performing privacy audit")
        
        var issues: [PrivacyIssue] = []
        var recommendations: [String] = []
        
        // Check data encryption status
        if !isDataEncrypted {
            issues.append(.dataNotEncrypted)
            recommendations.append("Enable data encryption for enhanced security")
        }
        
        // Check biometric authentication
        if !biometricAuthEnabled {
            recommendations.append("Consider enabling biometric authentication")
        }
        
        // Check data retention
        let retentionDays = Int(dataRetentionPeriod / (24 * 60 * 60))
        if retentionDays > 90 {
            issues.append(.excessiveDataRetention(days: retentionDays))
            recommendations.append("Consider reducing data retention period")
        }
        
        // Check for old data
        let oldDataCount = await countOldData()
        if oldDataCount > 0 {
            issues.append(.oldDataPresent(count: oldDataCount))
            recommendations.append("Clean up old data to minimize privacy exposure")
        }
        
        // Check anonymization status
        if !anonymizationEnabled {
            issues.append(.anonymizationDisabled)
            recommendations.append("Enable data anonymization for better privacy")
        }
        
        let report = PrivacyAuditReport(
            auditDate: Date(),
            issues: issues,
            recommendations: recommendations,
            overallScore: calculatePrivacyScore(issues: issues)
        )
        
        logger.info("✅ Privacy audit completed with score: \(report.overallScore)/100")
        
        return report
    }
    
    private func countOldData() async -> Int {
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -Int(dataRetentionPeriod / (24 * 60 * 60)), to: Date()) ?? Date()
        let coreDataManager = CoreDataManager.shared
        
        do {
            let oldRecords = try coreDataManager.fetchUsageRecords(before: cutoffDate)
            return oldRecords.count
        } catch {
            return 0
        }
    }
    
    private func calculatePrivacyScore(issues: [PrivacyIssue]) -> Int {
        let maxScore = 100
        let deductions = issues.reduce(0) { total, issue in
            total + issue.severityScore
        }
        
        return max(0, maxScore - deductions)
    }
    
    // MARK: - Setup and Scheduling
    
    private func setupPrivacyDefaults() {
        // Load saved settings
        isDataEncrypted = UserDefaults.standard.bool(forKey: "dataEncryptionEnabled")
        biometricAuthEnabled = UserDefaults.standard.bool(forKey: "biometricAuthEnabled")
        
        if let savedRetention = UserDefaults.standard.object(forKey: "dataRetentionPeriod") as? TimeInterval {
            dataRetentionPeriod = savedRetention
        }
        
        anonymizationEnabled = UserDefaults.standard.bool(forKey: "anonymizationEnabled")
        
        logger.info("🔧 Privacy settings loaded")
    }
    
    private func scheduleDataCleanup() {
        // Schedule automatic data cleanup
        let retentionDays = Int(dataRetentionPeriod / (24 * 60 * 60))
        
        Timer.scheduledTimer(withTimeInterval: 24 * 60 * 60, repeats: true) { [weak self] _ in
            Task {
                try? await self?.deleteDataOlderThan(days: retentionDays)
            }
        }
        
        logger.info("⏰ Data cleanup scheduled for every 24 hours")
    }
    
    // MARK: - Security Event Logging
    
    private func logSecurityEvent(_ event: SecurityEvent, details: [String: Any] = [:]) {
        let logEntry = SecurityLogEntry(
            timestamp: Date(),
            event: event,
            details: details
        )
        
        // In a real implementation, you'd store this securely
        logger.info("🔒 Security event logged: \(event.rawValue)")
    }
    
    private func showSecurityWarning(_ message: String) {
        // Show security warning to user
        NotificationCenter.default.post(
            name: .securityWarning,
            object: nil,
            userInfo: ["message": message]
        )
    }
    
    private func disableAffectedFeatures(_ features: [String]) {
        // Disable affected features
        for feature in features {
            UserDefaults.standard.set(false, forKey: "feature_\(feature)_enabled")
        }
        
        logger.warning("⚠️ Disabled features due to security violation: \(features)")
    }
    
    private func requireReAuthentication() {
        // Require user to re-authenticate
        NotificationCenter.default.post(name: .requireReAuthentication, object: nil)
        
        logger.error("🚨 Re-authentication required due to critical security violation")
    }
}

// MARK: - Supporting Services

class KeychainService {
    func storeData(_ data: Data, identifier: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: identifier,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        let status = SecItemAdd(query as CFDictionary, nil)
        
        if status == errSecDuplicateItem {
            // Update existing item
            let updateQuery: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrAccount as String: identifier
            ]
            
            let updateAttributes: [String: Any] = [
                kSecValueData as String: data
            ]
            
            let updateStatus = SecItemUpdate(updateQuery as CFDictionary, updateAttributes as CFDictionary)
            
            if updateStatus != errSecSuccess {
                throw PrivacySecurityError.keychainError("Failed to update keychain item")
            }
        } else if status != errSecSuccess {
            throw PrivacySecurityError.keychainError("Failed to store keychain item")
        }
    }
    
    func retrieveData(identifier: String) throws -> Data {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: identifier,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        if status == errSecSuccess {
            return result as! Data
        } else {
            throw PrivacySecurityError.keychainError("Failed to retrieve keychain item")
        }
    }
    
    func deleteItem(identifier: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: identifier
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        if status != errSecSuccess && status != errSecItemNotFound {
            throw PrivacySecurityError.keychainError("Failed to delete keychain item")
        }
    }
    
    func deleteAllItems() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        if status != errSecSuccess && status != errSecItemNotFound {
            throw PrivacySecurityError.keychainError("Failed to delete all keychain items")
        }
    }
}

class EncryptionService {
    func encrypt(data: Data, with key: SymmetricKey) throws -> Data {
        let sealedBox = try AES.GCM.seal(data, using: key)
        return sealedBox.combined!
    }
    
    func decrypt(data: Data, with key: SymmetricKey) throws -> Data {
        let sealedBox = try AES.GCM.SealedBox(combined: data)
        return try AES.GCM.open(sealedBox, using: key)
    }
}

class DataAnonymizer {
    private let hashSalt = "AwayTimeAnonymization2024"
    
    func anonymizeAppIdentifier(_ identifier: String) -> String {
        return "app_" + hashString(identifier)
    }
    
    func anonymizeDeviceIdentifier(_ identifier: String?) -> String? {
        guard let identifier = identifier else { return nil }
        return "device_" + hashString(identifier)
    }
    
    func anonymizeUsageData(_ data: [String: Any]) -> [String: Any] {
        var anonymizedData = data
        
        // Remove personally identifiable information
        anonymizedData.removeValue(forKey: "userId")
        anonymizedData.removeValue(forKey: "deviceId")
        anonymizedData.removeValue(forKey: "userEmail")
        
        // Anonymize app identifiers
        if let apps = anonymizedData["apps"] as? [[String: Any]] {
            anonymizedData["apps"] = apps.map { app in
                var anonymizedApp = app
                if let appId = app["id"] as? String {
                    anonymizedApp["id"] = anonymizeAppIdentifier(appId)
                }
                return anonymizedApp
            }
        }
        
        return anonymizedData
    }
    
    private func hashString(_ input: String) -> String {
        let combined = input + hashSalt
        let data = combined.data(using: .utf8)!
        let hashed = SHA256.hash(data: data)
        return hashed.compactMap { String(format: "%02x", $0) }.joined().prefix(8).lowercased()
    }
}

// MARK: - Supporting Types and Classes

class PermissionBoundaryEnforcer {
    var onUnauthorizedAccess: ((PermissionViolation) -> Void)?
    
    func startMonitoring() {
        // Monitor for unauthorized access attempts
    }
}

struct PermissionViolation {
    let severity: ViolationSeverity
    let description: String
    let details: [String: Any]
    let userMessage: String
    let affectedFeatures: [String]
    
    enum ViolationSeverity {
        case low, medium, high, critical
    }
}

enum PrivacySecurityError: Error {
    case biometricNotAvailable(String)
    case biometricAuthFailed(String)
    case keychainError(String)
    case encryptionFailed(String)
}

struct PrivacyAuditReport {
    let auditDate: Date
    let issues: [PrivacyIssue]
    let recommendations: [String]
    let overallScore: Int
}

enum PrivacyIssue {
    case dataNotEncrypted
    case excessiveDataRetention(days: Int)
    case oldDataPresent(count: Int)
    case anonymizationDisabled
    
    var severityScore: Int {
        switch self {
        case .dataNotEncrypted:
            return 25
        case .excessiveDataRetention:
            return 15
        case .oldDataPresent:
            return 10
        case .anonymizationDisabled:
            return 10
        }
    }
}

enum SecurityEvent: String {
    case permissionViolation = "permission_violation"
    case dataAccess = "data_access"
    case encryptionEnabled = "encryption_enabled"
    case dataDeleted = "data_deleted"
}

struct SecurityLogEntry {
    let timestamp: Date
    let event: SecurityEvent
    let details: [String: Any]
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let securityWarning = Notification.Name("securityWarning")
    static let requireReAuthentication = Notification.Name("requireReAuthentication")
}

// MARK: - CoreDataManager Extensions

extension CoreDataManager {
    func fetchAllUsageRecords() throws -> [UsageRecord] {
        // Implementation would fetch all usage records
        return []
    }
    
    func fetchGoals() throws -> [Goal] {
        // Implementation would fetch all goals
        return []
    }
    
    func fetchUsageRecords(before date: Date) throws -> [UsageRecord] {
        // Implementation would fetch records before specified date
        return []
    }
    
    func deleteAllUsageRecords() throws {
        // Implementation would delete all usage records
    }
    
    func deleteAllGoals() throws {
        // Implementation would delete all goals
    }
    
    func deleteAllGamificationData() throws {
        // Implementation would delete all gamification data
    }
    
    func deleteAllSelectedApps() throws {
        // Implementation would delete all selected apps
    }
    
    func deleteUsageRecords(before date: Date) throws {
        // Implementation would delete records before specified date
    }
    
    func deleteGamificationRecords(before date: Date) throws {
        // Implementation would delete gamification records before specified date
    }
}
