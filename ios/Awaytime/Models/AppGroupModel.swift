import Foundation
import FamilyControls

// MARK: - App Group Model

struct AppGroupModel: Identifiable, Codable {
    let id: UUID
    let name: String
    let dailyLimitMinutes: Int
    let isActive: Bool
    let createdDate: Date
    
    // Note: FamilyControls tokens cannot be directly encoded/decoded
    // We'll store them separately in the AppSelectionService
    
    var displayName: String {
        return name.isEmpty ? "My Apps" : name
    }
    
    var dailyLimitText: String {
        let hours = dailyLimitMinutes / 60
        let minutes = dailyLimitMinutes % 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
    
    init(id: UUID = UUID(), name: String, dailyLimitMinutes: Int = 120, isActive: Bool = true) {
        self.id = id
        self.name = name
        self.dailyLimitMinutes = dailyLimitMinutes
        self.isActive = isActive
        self.createdDate = Date()
    }
}

// MARK: - App Group Manager

@MainActor
class AppGroupManager: ObservableObject {
    @Published var appGroups: [AppGroupModel] = []
    @Published var currentAppGroup: AppGroupModel?
    
    private let userDefaults = UserDefaults.standard
    private let appGroupsKey = "savedAppGroups"
    private let currentGroupKey = "currentAppGroup"
    
    init() {
        loadAppGroups()
    }
    
    // MARK: - Persistence
    
    func saveAppGroups() {
        if let data = try? JSONEncoder().encode(appGroups) {
            userDefaults.set(data, forKey: appGroupsKey)
        }
        
        if let currentGroup = currentAppGroup,
           let data = try? JSONEncoder().encode(currentGroup) {
            userDefaults.set(data, forKey: currentGroupKey)
        }
    }
    
    func loadAppGroups() {
        if let data = userDefaults.data(forKey: appGroupsKey),
           let groups = try? JSONDecoder().decode([AppGroupModel].self, from: data) {
            appGroups = groups
        }
        
        if let data = userDefaults.data(forKey: currentGroupKey),
           let group = try? JSONDecoder().decode(AppGroupModel.self, from: data) {
            currentAppGroup = group
        }
    }
    
    // MARK: - App Group Management
    
    func addAppGroup(_ group: AppGroupModel) {
        appGroups.append(group)
        
        // Set as current group if it's the first one
        if currentAppGroup == nil {
            currentAppGroup = group
        }
        
        saveAppGroups()
    }
    
    func updateAppGroup(_ group: AppGroupModel) {
        if let index = appGroups.firstIndex(where: { $0.id == group.id }) {
            appGroups[index] = group
            
            // Update current group if it's the same
            if currentAppGroup?.id == group.id {
                currentAppGroup = group
            }
            
            saveAppGroups()
        }
    }
    
    func deleteAppGroup(_ group: AppGroupModel) {
        appGroups.removeAll { $0.id == group.id }
        
        // Clear current group if it was deleted
        if currentAppGroup?.id == group.id {
            currentAppGroup = appGroups.first
        }
        
        saveAppGroups()
    }
    
    func setCurrentAppGroup(_ group: AppGroupModel) {
        currentAppGroup = group
        saveAppGroups()
    }
    
    // MARK: - Utility
    
    var hasAppGroups: Bool {
        return !appGroups.isEmpty
    }
    
    var activeAppGroups: [AppGroupModel] {
        return appGroups.filter { $0.isActive }
    }
}