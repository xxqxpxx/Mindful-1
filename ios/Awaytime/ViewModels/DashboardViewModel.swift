import SwiftUI
import Combine
import DeviceActivity
import FamilyControls
import Foundation // Import Foundation for CFNotificationCenter

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var isLoading = true
    @Published var todayUsageMinutes = 0
    @Published var dailyLimitMinutes = 180
    @Published var selectedApps: [FamilyActivitySelection] = []
    @Published var isPremium = false
    @Published var error: String?

    private let coreDataManager = CoreDataManager.shared
    private var refreshTimer: Timer?
    private var cancellables = Set<AnyCancellable>()
    private var darwinNotificationObserver: NSObjectProtocol? // For Darwin Notification

    var usageProgress: Double {
        guard dailyLimitMinutes > 0 else { return 0 }
        return Double(todayUsageMinutes) / Double(dailyLimitMinutes)
    }

    var timeRemainingMinutes: Int {
        return max(0, dailyLimitMinutes - todayUsageMinutes)
    }

    var timeRemainingText: String {
        let remaining = timeRemainingMinutes
        let hours = remaining / 60
        let minutes = remaining % 60

        if remaining <= 0 {
            return "Time's up!"
        } else if hours > 0 {
            return "\(hours)h \(minutes)m remaining"
        } else {
            return "\(minutes)m remaining"
        }
    }

    init() {
        loadData()
        setupDarwinNotificationObserver() // Setup observer on init
    }

    deinit {
        refreshTimer?.invalidate()
        if let observer = darwinNotificationObserver {
            NotificationCenter.default.removeObserver(observer) // Remove observer on deinit
        }
    }

    private func loadData() {
        isLoading = true
        error = nil

        Task {
            do {
                // Load current app group and settings
                await loadCurrentAppGroup()

                // Load today's usage data
                await refreshUsageData()

                // Load premium status
                loadPremiumStatus()

                // Start periodic refresh
                startPeriodicRefresh()

                isLoading = false

            } catch {
                self.error = error.localizedDescription
                isLoading = false
                print("❌ Failed to load dashboard data: \(error)")
            }
        }
    }

    private func loadCurrentAppGroup() async {
        // Get the active app group from Core Data
        let appGroups = coreDataManager.fetchAppGroups()

        if let activeGroup = appGroups.first(where: { $0.isActive }) {
            dailyLimitMinutes = Int(activeGroup.dailyLimitMinutes)

            // Load selected apps from the app group
            if let appsData = activeGroup.selectedAppsData,
               let selection = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(appsData) as? FamilyActivitySelection {
                selectedApps = [selection]
            }
        } else {
            // No active app group - use defaults
            dailyLimitMinutes = 180 // 3 hours default
            selectedApps = []
        }
    }

    @MainActor
    private func refreshUsageData() async {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        if let totalActivity = userDefaults?.value(forKey: "totalActivityDuration") as? TimeInterval {
            todayUsageMinutes = Int(totalActivity / 60)
        }

        print("📊 Usage updated: \(todayUsageMinutes) minutes")
    }

    private func loadPremiumStatus() {
        // Check premium status from UserDefaults or subscription service
        isPremium = UserDefaults.standard.bool(forKey: "isPremiumUser")
    }

    private func startPeriodicRefresh() {
        // Refresh usage data every minute when app is active
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 60.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                await self?.refreshUsageData()
            }
        }
    }

    // MARK: - Darwin Notification Observer
    private func setupDarwinNotificationObserver() {
        darwinNotificationObserver = NotificationCenter.default.addObserver(
            forName: Notification.Name("com.awaytime.usage.event"), // Darwin Notification name
            object: nil,
            queue: .main
        ) { [weak self] _ in
            print("Received Darwin Notification: com.awaytime.usage.event")
            Task { @MainActor in
                await self?.refreshUsageData() // Refresh usage data when notification is received
            }
        }
    }

    // MARK: - Public Methods

    func refreshData() {
        loadData()
    }
}