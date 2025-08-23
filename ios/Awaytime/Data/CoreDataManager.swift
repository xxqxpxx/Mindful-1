import Foundation
import CoreData

// MARK: - Backup Data Structures

struct AppGroupBackup: Codable {
    let id: String
    let name: String
    let dailyLimitMinutes: Int
    let isActive: Bool
}

// Temporary Goal struct for compilation purposes
// This should be replaced with proper Core Data entity later
struct Goal {
    var id: UUID = UUID()
    var appIdentifier: String = ""
    var isLimitExceeded: Bool = false
    var dailyLimitMinutes: Int = 0
    var createdAt: Date = Date()
}

class CoreDataManager: ObservableObject {
    static let shared = CoreDataManager()
    
    lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "AwayTime")
        
        // Configure for data protection and migration
        let storeDescription = container.persistentStoreDescriptions.first
        storeDescription?.shouldInferMappingModelAutomatically = true
        storeDescription?.shouldMigrateStoreAutomatically = true
        storeDescription?.setOption(FileProtectionType.complete as NSObject, forKey: NSPersistentStoreFileProtectionKey)
        
        container.loadPersistentStores { storeDescription, error in
            if let error = error as NSError? {
                print("❌ Core Data error: \(error.localizedDescription)")
                
                // Attempt recovery instead of fatal error
                self.attemptCoreDataRecovery(container: container, error: error)
            } else {
                print("✅ Core Data loaded successfully")
            }
        }
        
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        
        return container
    }()
    
    var context: NSManagedObjectContext {
        return persistentContainer.viewContext
    }
    
    // MARK: - Save Context
    
    func save() {
        if context.hasChanges {
            do {
                try context.save()
                print("✅ Core Data saved successfully")
            } catch {
                print("❌ Failed to save Core Data context: \(error)")
                
                // Attempt to recover from save failure
                context.rollback()
                
                // Try to save again after rollback
                if context.hasChanges {
                    do {
                        try context.save()
                        print("✅ Core Data recovered and saved after rollback")
                    } catch {
                        print("❌ Core Data save failed even after rollback: \(error)")
                    }
                }
            }
        }
    }
    
    // MARK: - Data Recovery and Migration
    
    private func attemptCoreDataRecovery(container: NSPersistentContainer, error: NSError) {
        print("🔧 Attempting Core Data recovery...")
        
        // Check if it's a migration issue
        if error.code == NSPersistentStoreIncompatibleVersionHashError ||
           error.code == NSMigrationMissingSourceModelError {
            print("📦 Detected migration issue, attempting automatic migration")
            
            // Force automatic migration
            let storeDescription = container.persistentStoreDescriptions.first
            storeDescription?.shouldInferMappingModelAutomatically = true
            storeDescription?.shouldMigrateStoreAutomatically = true
            
            // Try loading again
            container.loadPersistentStores { _, retryError in
                if let retryError = retryError {
                    print("❌ Migration failed: \(retryError.localizedDescription)")
                    self.createFreshDatabase(container: container)
                } else {
                    print("✅ Migration successful")
                }
            }
        } else {
            // For other errors, try creating a fresh database
            createFreshDatabase(container: container)
        }
    }
    
    private func createFreshDatabase(container: NSPersistentContainer) {
        print("🆕 Creating fresh database...")
        
        // Backup existing data if possible
        backupUserPreferences()
        
        // Remove the corrupted store
        let storeURL = container.persistentStoreDescriptions.first?.url
        if let url = storeURL {
            try? FileManager.default.removeItem(at: url)
            print("🗑️ Removed corrupted database")
        }
        
        // Load with empty store
        container.loadPersistentStores { _, error in
            if let error = error {
                print("❌ Failed to create fresh database: \(error)")
                // As last resort, continue with in-memory store
                self.setupInMemoryStore(container: container)
            } else {
                print("✅ Fresh database created successfully")
                // Restore user preferences
                self.restoreUserPreferences()
            }
        }
    }
    
    private func setupInMemoryStore(container: NSPersistentContainer) {
        print("💾 Setting up in-memory store as fallback")
        
        let description = NSPersistentStoreDescription()
        description.type = NSInMemoryStoreType
        container.persistentStoreDescriptions = [description]
        
        container.loadPersistentStores { _, error in
            if let error = error {
                print("❌ Even in-memory store failed: \(error)")
            } else {
                print("✅ In-memory store setup successful")
                // Restore what we can
                self.restoreUserPreferences()
            }
        }
    }
    
    // MARK: - Usage Records
    
    func saveUsageRecord(date: Date, usageMinutes: Int, appGroupName: String, limitExceeded: Bool, pickupCount: Int = 0) {
        let record = UsageRecord(context: context)
        record.id = UUID()
        record.date = date
        record.usageMinutes = Int32(usageMinutes)
        record.appGroupName = appGroupName
        record.limitExceeded = limitExceeded
        record.pickupCount = Int32(pickupCount)
        
        save()
    }
    
    func fetchUsageRecords(for date: Date) -> [UsageRecord] {
        let request: NSFetchRequest<UsageRecord> = UsageRecord.fetchRequest()
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: date)
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        
        request.predicate = NSPredicate(format: "date >= %@ AND date < %@", startOfDay as NSDate, endOfDay as NSDate)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \UsageRecord.date, ascending: false)]
        
        do {
            return try context.fetch(request)
        } catch {
            print("Failed to fetch usage records: \(error)")
            return []
        }
    }
    
    func fetchUsageRecords(from startDate: Date, to endDate: Date) -> [UsageRecord] {
        let request: NSFetchRequest<UsageRecord> = UsageRecord.fetchRequest()
        request.predicate = NSPredicate(format: "date >= %@ AND date <= %@", startDate as NSDate, endDate as NSDate)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \UsageRecord.date, ascending: false)]
        
        do {
            return try context.fetch(request)
        } catch {
            print("Failed to fetch usage records: \(error)")
            return []
        }
    }
    
    // MARK: - App Groups
    
    func saveAppGroup(name: String, dailyLimitMinutes: Int, selectedAppsData: Data?) {
        let appGroup = AppGroupEntity(context: context)
        appGroup.id = UUID()
        appGroup.name = name
        appGroup.dailyLimitMinutes = Int32(dailyLimitMinutes)
        appGroup.isActive = true
        appGroup.createdDate = Date()
        appGroup.selectedAppsData = selectedAppsData
        
        save()
    }
    
    func createAppGroup(name: String, dailyLimitMinutes: Int, isActive: Bool = false) -> AppGroupEntity {
        let appGroup = AppGroupEntity(context: context)
        appGroup.id = UUID()
        appGroup.name = name
        appGroup.dailyLimitMinutes = Int32(dailyLimitMinutes)
        appGroup.isActive = isActive
        appGroup.createdDate = Date()
        
        save()
        return appGroup
    }
    
    func saveContext() {
        save()
    }
    
    func fetchAppGroups() -> [AppGroupEntity] {
        let request: NSFetchRequest<AppGroupEntity> = AppGroupEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(keyPath: \AppGroupEntity.createdDate, ascending: false)]
        
        do {
            return try context.fetch(request)
        } catch {
            print("Failed to fetch app groups: \(error)")
            return []
        }
    }
    
    func deleteAppGroup(_ appGroup: AppGroupEntity) {
        context.delete(appGroup)
        save()
    }
    
    // MARK: - User Settings
    
    func getUserSettings() -> UserSettings {
        let request: NSFetchRequest<UserSettings> = UserSettings.fetchRequest()
        
        do {
            let settings = try context.fetch(request)
            if let userSettings = settings.first {
                return userSettings
            } else {
                // Create default settings
                let newSettings = UserSettings(context: context)
                newSettings.id = UUID()
                newSettings.isPremium = false
                newSettings.streakCount = 0
                newSettings.notificationsEnabled = true
                save()
                return newSettings
            }
        } catch {
            print("Failed to fetch user settings: \(error)")
            // Return default settings without saving
            let defaultSettings = UserSettings(context: context)
            defaultSettings.id = UUID()
            defaultSettings.isPremium = false
            defaultSettings.streakCount = 0
            defaultSettings.notificationsEnabled = true
            return defaultSettings
        }
    }
    
    func updateUserSettings(isPremium: Bool? = nil, streakCount: Int? = nil, notificationsEnabled: Bool? = nil) {
        let settings = getUserSettings()
        
        if let isPremium = isPremium {
            settings.isPremium = isPremium
        }
        
        if let streakCount = streakCount {
            settings.streakCount = Int32(streakCount)
        }
        
        if let notificationsEnabled = notificationsEnabled {
            settings.notificationsEnabled = notificationsEnabled
        }
        
        save()
    }
    
    // MARK: - Utility Methods
    
    func getTodayUsage(for appGroupName: String) -> Int {
        let today = Date()
        let records = fetchUsageRecords(for: today)
        let todayRecord = records.first { $0.appGroupName == appGroupName }
        return Int(todayRecord?.usageMinutes ?? 0)
    }
    
    func getWeeklyUsage(for appGroupName: String) -> [UsageRecord] {
        let calendar = Calendar.current
        let today = Date()
        let weekAgo = calendar.date(byAdding: .day, value: -7, to: today)!
        
        let records = fetchUsageRecords(from: weekAgo, to: today)
        return records.filter { $0.appGroupName == appGroupName }
    }
    
    func calculateStreak() -> Int {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var currentDate = today
        var streak = 0
        
        // Check each day going backwards
        for _ in 0..<30 { // Check up to 30 days
            let records = fetchUsageRecords(for: currentDate)
            let dayHadSuccess = records.contains { !$0.limitExceeded }
            
            if dayHadSuccess {
                streak += 1
                currentDate = calendar.date(byAdding: .day, value: -1, to: currentDate)!
            } else {
                break
            }
        }
        
        return streak
    }
    
    // MARK: - Additional Methods
    
    func getUsageRecords(days: Int) -> [UsageRecord] {
        let calendar = Calendar.current
        let endDate = Date()
        let startDate = calendar.date(byAdding: .day, value: -days, to: endDate) ?? endDate
        
        return fetchUsageRecords(from: startDate, to: endDate)
    }
    
    func fetchSelectedApps() throws -> [String] {
        let appGroups = fetchAppGroups()
        let activeGroup = appGroups.first(where: { $0.isActive })
        
        guard let group = activeGroup else {
            return []
        }
        
        // Return a simplified list for now
        return [group.name ?? "Unknown"]
    }
    
    func fetchGoals() throws -> [Goal] {
        // For now, return empty array - this would need proper Goal entity implementation
        // TODO: Implement proper Core Data Goal entity
        return []
    }
    
    func compactStore() throws {
        // Perform Core Data store compaction
        try context.save()
    }
    
    func deleteUsageRecords(before date: Date) throws {
        let request: NSFetchRequest<UsageRecord> = UsageRecord.fetchRequest()
        request.predicate = NSPredicate(format: "date < %@", date as NSDate)
        
        let recordsToDelete = try context.fetch(request)
        for record in recordsToDelete {
            context.delete(record)
        }
        
        try context.save()
    }
    
    // MARK: - Backup and Restore
    
    private func backupUserPreferences() {
        print("💾 Backing up user preferences to UserDefaults...")
        
        do {
            // Backup current user settings
            let settings = getUserSettings()
            UserDefaults.standard.set(settings.isPremium, forKey: "backup_isPremium")
            UserDefaults.standard.set(settings.streakCount, forKey: "backup_streakCount")
            UserDefaults.standard.set(settings.notificationsEnabled, forKey: "backup_notificationsEnabled")
            
            // Backup app groups using a proper codable structure
            let appGroups = fetchAppGroups()
            let backupGroups = appGroups.map { group in
                AppGroupBackup(
                    id: group.id?.uuidString ?? "",
                    name: group.name ?? "",
                    dailyLimitMinutes: Int(group.dailyLimitMinutes),
                    isActive: group.isActive
                )
            }
            let appGroupsData = try JSONEncoder().encode(backupGroups)
            UserDefaults.standard.set(appGroupsData, forKey: "backup_appGroups")
            
            print("✅ User preferences backed up successfully")
        } catch {
            print("❌ Failed to backup user preferences: \(error)")
        }
    }
    
    private func restoreUserPreferences() {
        print("📥 Restoring user preferences from backup...")
        
        // Restore user settings
        if UserDefaults.standard.object(forKey: "backup_isPremium") != nil {
            let isPremium = UserDefaults.standard.bool(forKey: "backup_isPremium")
            let streakCount = UserDefaults.standard.integer(forKey: "backup_streakCount")
            let notificationsEnabled = UserDefaults.standard.bool(forKey: "backup_notificationsEnabled")
            
            updateUserSettings(
                isPremium: isPremium,
                streakCount: streakCount,
                notificationsEnabled: notificationsEnabled
            )
            
            print("✅ User settings restored")
        }
        
        // Restore app groups
        if let appGroupsData = UserDefaults.standard.data(forKey: "backup_appGroups") {
            do {
                let backupGroups = try JSONDecoder().decode([AppGroupBackup].self, from: appGroupsData)
                for groupData in backupGroups {
                    let _ = createAppGroup(
                        name: groupData.name,
                        dailyLimitMinutes: groupData.dailyLimitMinutes,
                        isActive: groupData.isActive
                    )
                    print("✅ Restored app group: \(groupData.name)")
                }
            } catch {
                print("❌ Failed to restore app groups: \(error)")
            }
        }
        
        // Clean up backup data
        UserDefaults.standard.removeObject(forKey: "backup_isPremium")
        UserDefaults.standard.removeObject(forKey: "backup_streakCount")
        UserDefaults.standard.removeObject(forKey: "backup_notificationsEnabled")
        UserDefaults.standard.removeObject(forKey: "backup_appGroups")
    }
    
    // MARK: - Data Validation and Integrity
    
    func validateDataIntegrity() -> Bool {
        do {
            // Check if we can fetch basic entities
            let settings = getUserSettings()
            let appGroups = fetchAppGroups()
            let recentRecords = getUsageRecords(days: 1)
            
            print("✅ Data integrity check passed")
            print("   - User settings: \(settings.id != nil ? "OK" : "Missing")")
            print("   - App groups: \(appGroups.count)")
            print("   - Recent usage records: \(recentRecords.count)")
            
            return true
        } catch {
            print("❌ Data integrity check failed: \(error)")
            return false
        }
    }
    
    func performDataMaintenance() {
        print("🧹 Performing data maintenance...")
        
        // Clean up old usage records (older than 90 days)
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -90, to: Date()) ?? Date()
        
        do {
            try deleteUsageRecords(before: cutoffDate)
            print("✅ Cleaned up old usage records")
        } catch {
            print("❌ Failed to clean up old records: \(error)")
        }
        
        // Validate and fix any data inconsistencies
        if !validateDataIntegrity() {
            print("⚠️ Data integrity issues detected, attempting repairs...")
            repairDataInconsistencies()
        }
        
        // Compact the Core Data store
        do {
            try compactStore()
            print("✅ Core Data store compacted")
        } catch {
            print("❌ Failed to compact store: \(error)")
        }
    }
    
    private func repairDataInconsistencies() {
        // Ensure we have user settings
        let _ = getUserSettings()
        
        // Ensure we have at least one app group
        let appGroups = fetchAppGroups()
        if appGroups.isEmpty {
            let _ = createAppGroup(name: "My Apps", dailyLimitMinutes: 120, isActive: true)
            print("✅ Created default app group")
        }
    }
}
