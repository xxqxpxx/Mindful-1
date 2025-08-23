import Foundation
import FamilyControls
import ManagedSettings

@MainActor
class AppSelectionService: ObservableObject {
    @Published var selectedApps: FamilyActivitySelection = FamilyActivitySelection()
    @Published var isLoading = false
    @Published var error: String?
    
    private let store = ManagedSettingsStore()
    
    init() {
        loadSelectedApps()
    }
    
    // MARK: - App Selection Management
    
    func updateSelectedApps(_ selection: FamilyActivitySelection) {
        selectedApps = selection
        
        // Debug logging matching Android functionality
        let appCount = selection.applicationTokens.count
        let categoryCount = selection.categoryTokens.count
        let webCount = selection.webDomainTokens.count
        
        print("🎯 Apps updated: \(appCount) apps, \(categoryCount) categories, \(webCount) websites selected")
        
        saveSelectedApps()
    }
    
    func clearSelectedApps() {
        let previousCount = selectedAppCount
        selectedApps = FamilyActivitySelection()
        print("🗑️ Cleared \(previousCount) selected apps")
        saveSelectedApps()
    }
    
    // MARK: - Persistence
    
    private func saveSelectedApps() {
        // Save to UserDefaults for persistence
        if let data = try? NSKeyedArchiver.archivedData(withRootObject: selectedApps, requiringSecureCoding: false) {
            UserDefaults.standard.set(data, forKey: "selectedApps")
        }
        
        // Update the dashboard view model
        let appNames = getSelectedAppNames()
        UserDefaults.standard.set(appNames, forKey: "selectedAppNames")
    }
    
    private func loadSelectedApps() {
        if let data = UserDefaults.standard.data(forKey: "selectedApps"),
           let selection = try? NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(data) as? FamilyActivitySelection {
            selectedApps = selection
        }
    }
    
    // MARK: - App Information
    
    func getSelectedAppNames() -> [String] {
        var appNames: [String] = []
        
        // Get application names (this is simplified - in reality, FamilyControls provides tokens, not names)
        for token in selectedApps.applicationTokens {
            // FamilyControls doesn't provide direct access to app names for privacy
            // We'll use a generic description that shows the count
            appNames.append("App \(appNames.count + 1)")
        }
        
        // Add category descriptions
        for token in selectedApps.categoryTokens {
            appNames.append("Category \(appNames.count + 1)")
        }
        
        // Add web domain descriptions  
        for token in selectedApps.webDomainTokens {
            appNames.append("Website \(appNames.count + 1)")
        }
        
        // Add category names
        for token in selectedApps.categoryTokens {
            appNames.append("App Category")
        }
        
        return appNames
    }
    
    var selectedAppCount: Int {
        return selectedApps.applicationTokens.count + selectedApps.categoryTokens.count
    }
    
    var hasSelectedApps: Bool {
        return selectedAppCount > 0
    }
    
    // MARK: - App Group Management
    
    func createAppGroup(name: String) -> AppGroup? {
        guard hasSelectedApps else { 
            print("❌ Cannot create app group: No apps selected")
            return nil 
        }
        
        let appGroup = AppGroup(
            id: UUID(),
            name: name,
            applicationTokens: selectedApps.applicationTokens,
            categoryTokens: selectedApps.categoryTokens,
            dailyLimitMinutes: 120, // Default 2 hours
            isActive: true
        )
        
        print("✅ Created app group '\(name)' with \(selectedAppCount) items")
        
        return appGroup
    }
}

// MARK: - App Group Model

struct AppGroup: Identifiable, Codable {
    let id: UUID
    let name: String
    let applicationTokens: Set<ApplicationToken>
    let categoryTokens: Set<ActivityCategoryToken>
    let dailyLimitMinutes: Int
    let isActive: Bool
    
    var displayName: String {
        return name.isEmpty ? "My Apps" : name
    }
    
    var appCount: Int {
        return applicationTokens.count + categoryTokens.count
    }
}