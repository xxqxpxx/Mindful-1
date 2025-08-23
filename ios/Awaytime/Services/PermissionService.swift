import SwiftUI
import FamilyControls

@MainActor
class PermissionService: ObservableObject {
    @Published var needsPermission = false
    @Published var authorizationStatus: AuthorizationStatus = .notDetermined
    @Published var isIOSVersionSupported = true
    
    init() {
        checkIOSVersionSupport()
        updateAuthorizationStatus()
        setupPermissionMonitoring()
    }
    
    private func checkIOSVersionSupport() {
        if #available(iOS 16.0, *) {
            isIOSVersionSupported = true
        } else {
            isIOSVersionSupported = false
            print("⚠️ Full FamilyControls functionality requires iOS 16.0 or later")
        }
    }
    
    func updateAuthorizationStatus() {
        if #available(iOS 16.0, *) {
            authorizationStatus = AuthorizationCenter.shared.authorizationStatus
            needsPermission = authorizationStatus != .approved
        } else {
            // For iOS 15, we'll assume permission is needed since FamilyControls
            // has limited support and requires iOS 16+ for full functionality
            authorizationStatus = .notDetermined
            needsPermission = true
        }
    }
    
    func requestPermission() async {
        do {
            if #available(iOS 16.0, *) {
                try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            } else {
                // For iOS 15, we need to use the older API or handle differently
                // Since FamilyControls authorization is complex on iOS 15, 
                // we'll provide a fallback approach
                print("FamilyControls authorization requires iOS 16+")
                authorizationStatus = .denied
                needsPermission = true
                return
            }
            updateAuthorizationStatus()
        } catch {
            print("Failed to request authorization: \(error)")
            authorizationStatus = .denied
            needsPermission = true
        }
    }
    
    // MARK: - Permission Monitoring and Persistence
    
    private func setupPermissionMonitoring() {
        // Set up a timer to periodically check permission status
        Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { _ in
            Task { @MainActor in
                self.updateAuthorizationStatus()
            }
        }
        
        // Save initial permission status
        savePermissionStatus()
        
        // Listen for app becoming active to recheck permissions
        NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { _ in
            Task { @MainActor in
                self.updateAuthorizationStatus()
                self.validatePermissionConsistency()
            }
        }
    }
    
    private func savePermissionStatus() {
        let statusValue: Int
        switch authorizationStatus {
        case .approved:
            statusValue = 1
        case .denied:
            statusValue = 2
        case .notDetermined:
            statusValue = 0
        @unknown default:
            statusValue = 0
        }
        
        UserDefaults.standard.set(statusValue, forKey: "lastKnownAuthorizationStatus")
        UserDefaults.standard.set(Date(), forKey: "lastPermissionCheck")
        
        print("💾 Saved permission status: \(authorizationStatus)")
    }
    
    private func loadSavedPermissionStatus() -> AuthorizationStatus? {
        guard UserDefaults.standard.object(forKey: "lastKnownAuthorizationStatus") != nil else {
            return nil
        }
        
        let statusValue = UserDefaults.standard.integer(forKey: "lastKnownAuthorizationStatus")
        switch statusValue {
        case 1:
            return .approved
        case 2:
            return .denied
        default:
            return .notDetermined
        }
    }
    
    private func validatePermissionConsistency() {
        guard let savedStatus = loadSavedPermissionStatus() else { return }
        
        // Check if permission status has changed unexpectedly
        if savedStatus != authorizationStatus {
            print("⚠️ Permission status changed from \(savedStatus) to \(authorizationStatus)")
            
            // If we lost permission, inform the user
            if savedStatus == .approved && authorizationStatus != .approved {
                print("❌ Lost FamilyControls permission - user may need to re-grant")
                needsPermission = true
                
                // Post notification for UI to handle
                NotificationCenter.default.post(
                    name: .permissionStatusChanged,
                    object: nil,
                    userInfo: ["previousStatus": savedStatus, "currentStatus": authorizationStatus]
                )
            }
        }
        
        savePermissionStatus()
    }
    
    // MARK: - Permission Recovery
    
    func attemptPermissionRecovery() async {
        print("🔧 Attempting permission recovery...")
        
        updateAuthorizationStatus()
        
        if authorizationStatus != .approved {
            print("📱 Requesting permission again...")
            await requestPermission()
        }
        
        // Double-check after request
        updateAuthorizationStatus()
        
        if authorizationStatus == .approved {
            print("✅ Permission recovery successful")
        } else {
            print("❌ Permission recovery failed - manual intervention required")
        }
    }
    
    // MARK: - Settings Persistence Helper
    
    func ensureSettingsPersistence() {
        // Ensure critical app settings are backed up
        let hasOnboardingCompleted = UserDefaults.standard.object(forKey: "onboarding_completed") != nil
        let hasPremiumStatus = UserDefaults.standard.object(forKey: "isPremiumUser") != nil
        let hasAppGroups = !CoreDataManager.shared.fetchAppGroups().isEmpty
        
        print("🔍 Settings persistence check:")
        print("   - Onboarding completed: \(hasOnboardingCompleted)")
        print("   - Premium status saved: \(hasPremiumStatus)")
        print("   - App groups exist: \(hasAppGroups)")
        
        if !hasAppGroups {
            print("⚠️ No app groups found, creating default group")
            let _ = CoreDataManager.shared.createAppGroup(
                name: "My Apps",
                dailyLimitMinutes: 120,
                isActive: true
            )
        }
        
        // Trigger data integrity check
        let _ = CoreDataManager.shared.validateDataIntegrity()
    }
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let permissionStatusChanged = Notification.Name("permissionStatusChanged")
}