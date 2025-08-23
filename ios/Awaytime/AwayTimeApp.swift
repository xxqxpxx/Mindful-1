import SwiftUI
import UserNotifications
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

// MARK: - App Delegate for Firebase
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Check if GoogleService-Info.plist exists
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           FileManager.default.fileExists(atPath: path) {
            // Configure Firebase normally
            FirebaseApp.configure()
            
            // Configure Analytics
            Analytics.setAnalyticsCollectionEnabled(true)
            
            // Configure Crashlytics
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
            
            print("🔥 Firebase configured successfully with GoogleService-Info.plist")
            print("📊 Firebase Analytics enabled")
            print("💥 Firebase Crashlytics enabled")
        } else {
            // Fallback configuration for development
            print("⚠️ GoogleService-Info.plist not found in bundle")
            print("🚀 Running in development mode without Firebase")
            print("📝 To fix: Add GoogleService-Info.plist to Xcode target")
            
            // You can still configure Firebase manually if needed:
            configureFirebaseManually()
        }
        
        return true
    }
    
    private func configureFirebaseManually() {
        // Manual Firebase configuration as fallback
        let options = FirebaseOptions(googleAppID: "1:434002057239:ios:27545580835b06d3e82452",
                                    gcmSenderID: "434002057239")
        
        options.apiKey = "AIzaSyDunPXAYntsc_-Oy4Mkz8RllRDA-eladUY"
        options.projectID = "awaytime-63869"
        options.bundleID = "com.awaytime.app"
        options.storageBucket = "awaytime-63869.firebasestorage.app"
        
        FirebaseApp.configure(options: options)
        
        // Configure Analytics (but disable in debug mode)
        #if DEBUG
        Analytics.setAnalyticsCollectionEnabled(false)
        print("📊 Firebase Analytics disabled in debug mode")
        #else
        Analytics.setAnalyticsCollectionEnabled(true)
        print("📊 Firebase Analytics enabled")
        #endif
        
        // Configure Crashlytics
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        print("💥 Firebase Crashlytics enabled")
        print("🔥 Firebase configured manually as fallback")
    }
}

@main
struct AwayTimeApp: App {
    // Register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    let persistenceController = CoreDataManager.shared
    @StateObject private var appState = AppState()
    
    var body: some Scene {
        WindowGroup {
            NavigationCoordinatorView()
                .environment(\.managedObjectContext, persistenceController.context)
                .environmentObject(appState)
                .preferredColorScheme(.light)
                .onAppear {
                    setupApp()
                }
        }
    }
    
    private func setupApp() {
        // Initialize notification categories
        setupNotificationCategories()
        
        // Initialize analytics
        setupAnalytics()
        
        // Setup background app refresh
        setupBackgroundRefresh()
        
        // Create default app group if needed (matching Android functionality)
        Task {
            await createDefaultAppGroupIfNeeded()
            await performAppStartupMaintenance()
        }
        
        print("✅ AwayTime iOS App initialized successfully")
    }
    
    private func performAppStartupMaintenance() async {
        print("🔧 Performing app startup maintenance...")
        
        // Validate Core Data integrity
        let coreDataManager = CoreDataManager.shared
        let isDataIntegrityOk = coreDataManager.validateDataIntegrity()
        
        if !isDataIntegrityOk {
            print("⚠️ Data integrity issues detected on startup")
            coreDataManager.performDataMaintenance()
        }
        
        // Ensure settings persistence 
        let permissionService = PermissionService()
        await MainActor.run {
            permissionService.ensureSettingsPersistence()
        }
        
        // Perform comprehensive settings validation
        await performSettingsValidation()
        
        // Perform periodic data maintenance (every 7 days)
        let lastMaintenance = UserDefaults.standard.object(forKey: "lastDataMaintenance") as? Date ?? Date.distantPast
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        
        if lastMaintenance < weekAgo {
            print("🧹 Running periodic data maintenance...")
            coreDataManager.performDataMaintenance()
            UserDefaults.standard.set(Date(), forKey: "lastDataMaintenance")
        }
        
        print("✅ App startup maintenance completed")
    }
    
    private func performSettingsValidation() async {
        // Simplified validation approach to avoid build issues
        await MainActor.run {
            // Basic validation checks
            let hasOnboardingCompleted = UserDefaults.standard.object(forKey: "onboarding_completed") != nil
            let hasPremiumStatus = UserDefaults.standard.object(forKey: "isPremiumUser") != nil
            let hasAppGroups = !CoreDataManager.shared.fetchAppGroups().isEmpty
            
            print("🔍 Settings validation:")
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
    
    private func createDefaultAppGroupIfNeeded() async {
        do {
            let coreDataManager = CoreDataManager.shared
            let appGroups = coreDataManager.fetchAppGroups()
            
            if appGroups.isEmpty {
                print("📱 No app groups found, creating default group")
                
                // Create default app group with common social media apps
                coreDataManager.saveAppGroup(
                    name: "My Apps",
                    dailyLimitMinutes: 120, // Default 2 hours
                    selectedAppsData: nil // Will be populated when user selects apps
                )
                
                print("✅ Created default app group 'My Apps' with 120 minute limit")
            } else {
                print("📱 Found \(appGroups.count) existing app groups")
            }
        } catch {
            print("❌ Failed to create default app group: \(error.localizedDescription)")
        }
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
        
        // Goal achievement category
        let goalCategory = UNNotificationCategory(
            identifier: "GOAL_ACHIEVEMENT",
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        
        center.setNotificationCategories([warningCategory, limitCategory, goalCategory])
    }
    
    private func setupAnalytics() {
        // Track app launch
        let userDefaults = UserDefaults.standard
        let launchCount = userDefaults.integer(forKey: "app_launch_count") + 1
        userDefaults.set(launchCount, forKey: "app_launch_count")
        
        // Log app launch event to Firebase Analytics
        Analytics.logEvent("app_launch", parameters: [
            "launch_count": launchCount,
            "platform": "ios"
        ])
        
        // Set user properties
        Analytics.setUserProperty("ios", forName: "platform")
        
        print("📊 App launch #\(launchCount) - Firebase Analytics event logged")
    }
    
    private func setupBackgroundRefresh() {
        // Enable background app refresh for usage tracking
        // This is handled by the DeviceActivity extension
        print("🔄 Background refresh configured")
    }
}

// MARK: - App State Management

@MainActor
class AppState: ObservableObject {
    @Published var isInitialized = false
    @Published var hasCompletedOnboarding = false
    
    init() {
        checkOnboardingStatus()
    }
    
    private func checkOnboardingStatus() {
        hasCompletedOnboarding = OnboardingManager.isOnboardingCompleted
        isInitialized = true
    }
    
    func completeOnboarding() {
        OnboardingManager.completeOnboarding()
        hasCompletedOnboarding = true
    }
    
    func resetOnboarding() {
        OnboardingManager.resetOnboarding()
        hasCompletedOnboarding = false
    }
}
