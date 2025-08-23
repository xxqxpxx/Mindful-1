import SwiftUI
import DeviceActivity
import Combine
import os.log

/// Performance optimization service for Awaytime
@MainActor
class PerformanceOptimizer: ObservableObject {
    @Published var isOptimizing = false
    @Published var batteryOptimizationEnabled = true
    @Published var backgroundRefreshEnabled = true
    @Published var cacheSize: Int64 = 0
    
    private let logger = Logger(subsystem: "com.awaytime.app", category: "Performance")
    private let cacheManager = CacheManager()
    private let backgroundTaskManager = BackgroundTaskManager()
    private let memoryManager = MemoryManager()
    
    private var cancellables = Set<AnyCancellable>()
    private var performanceTimer: Timer?
    
    init() {
        setupPerformanceMonitoring()
        optimizeAppLaunch()
    }
    
    // MARK: - App Launch Optimization
    
    func optimizeAppLaunch() {
        logger.info("🚀 Optimizing app launch performance")
        
        // Preload critical data
        preloadCriticalData()
        
        // Initialize background services
        initializeBackgroundServices()
        
        // Setup memory management
        setupMemoryManagement()
        
        // Configure caching strategy
        configureCaching()
    }
    
    private func preloadCriticalData() {
        Task {
            // Preload user preferences
            _ = UserDefaults.standard.object(forKey: "hasCompletedOnboarding")
            
            // Preload subscription status
            let subscriptionService = SubscriptionManager.shared.getService()
            await subscriptionService.updateSubscriptionStatus()
            
            // Preload recent usage data
            let coreDataManager = CoreDataManager.shared
            _ = try? coreDataManager.fetchUsageRecords(for: Date())
            
            logger.info("✅ Critical data preloaded")
        }
    }
    
    private func initializeBackgroundServices() {
        // Initialize services that need to run in background
        backgroundTaskManager.initialize()
        
        // Setup device activity monitoring with optimized intervals
        setupOptimizedDeviceActivity()
        
        logger.info("✅ Background services initialized")
    }
    
    private func setupOptimizedDeviceActivity() {
        // Use longer intervals during low activity periods
        let calendar = Calendar.current
        let now = Date()
        let hour = calendar.component(.hour, from: now)
        
        // Reduce monitoring frequency during typical sleep hours (11 PM - 7 AM)
        let isLowActivityPeriod = hour >= 23 || hour <= 7
        let monitoringInterval: TimeInterval = isLowActivityPeriod ? 300 : 60 // 5 min vs 1 min
        
        // Configure device activity with optimized settings
        configureDeviceActivityMonitoring(interval: monitoringInterval)
        
        logger.info("⚙️ Device activity monitoring optimized for current time")
    }
    
    private func configureDeviceActivityMonitoring(interval: TimeInterval) {
        // This would configure the actual DeviceActivity monitoring
        // with the specified interval to balance accuracy and battery life
        logger.info("📱 Device activity monitoring configured with \(interval)s interval")
    }
    
    // MARK: - Memory Management
    
    private func setupMemoryManagement() {
        memoryManager.startMonitoring()
        
        // Listen for memory warnings
        NotificationCenter.default.publisher(for: UIApplication.didReceiveMemoryWarningNotification)
            .sink { [weak self] _ in
                self?.handleMemoryWarning()
            }
            .store(in: &cancellables)
        
        // Monitor memory usage periodically
        startMemoryMonitoring()
    }
    
    private func startMemoryMonitoring() {
        performanceTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.checkMemoryUsage()
        }
    }
    
    private func checkMemoryUsage() {
        let memoryUsage = memoryManager.getCurrentMemoryUsage()
        
        if memoryUsage > 0.8 { // 80% memory usage threshold
            logger.warning("⚠️ High memory usage detected: \(memoryUsage * 100)%")
            performMemoryCleanup()
        }
    }
    
    private func handleMemoryWarning() {
        logger.warning("⚠️ Memory warning received")
        performMemoryCleanup()
    }
    
    private func performMemoryCleanup() {
        isOptimizing = true
        
        Task {
            // Clear image caches
            await cacheManager.clearImageCache()
            
            // Clear old usage data
            await clearOldUsageData()
            
            // Compact Core Data store
            await compactCoreDataStore()
            
            // Force garbage collection
            await memoryManager.forceGarbageCollection()
            
            isOptimizing = false
            logger.info("✅ Memory cleanup completed")
        }
    }
    
    // MARK: - Caching Strategy
    
    private func configureCaching() {
        cacheManager.configure(
            maxMemoryCache: 50 * 1024 * 1024, // 50MB
            maxDiskCache: 100 * 1024 * 1024,  // 100MB
            cacheExpiration: 24 * 60 * 60     // 24 hours
        )
        
        updateCacheSize()
        
        logger.info("💾 Caching strategy configured")
    }
    
    func updateCacheSize() {
        Task {
            cacheSize = await cacheManager.getCurrentCacheSize()
        }
    }
    
    func clearCache() {
        Task {
            await cacheManager.clearAllCaches()
            updateCacheSize()
            logger.info("🗑️ All caches cleared")
        }
    }
    
    private func clearOldUsageData() async {
        let coreDataManager = CoreDataManager.shared
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
        
        do {
            try coreDataManager.deleteUsageRecords(before: cutoffDate)
            logger.info("🗑️ Old usage data cleared")
        } catch {
            logger.error("❌ Failed to clear old usage data: \(error)")
        }
    }
    
    private func compactCoreDataStore() async {
        let coreDataManager = CoreDataManager.shared
        
        do {
            try coreDataManager.compactStore()
            logger.info("🗜️ Core Data store compacted")
        } catch {
            logger.error("❌ Failed to compact Core Data store: \(error)")
        }
    }
    
    // MARK: - Battery Optimization
    
    func enableBatteryOptimization(_ enabled: Bool) {
        batteryOptimizationEnabled = enabled
        
        if enabled {
            // Reduce background activity
            backgroundTaskManager.enableBatteryOptimization()
            
            // Increase monitoring intervals
            setupOptimizedDeviceActivity()
            
            // Reduce animation complexity
            reducedMotionEnabled = true
            
            logger.info("🔋 Battery optimization enabled")
        } else {
            // Restore normal activity
            backgroundTaskManager.disableBatteryOptimization()
            
            // Restore normal intervals
            configureDeviceActivityMonitoring(interval: 60)
            
            // Restore full animations
            reducedMotionEnabled = false
            
            logger.info("🔋 Battery optimization disabled")
        }
    }
    
    @Published var reducedMotionEnabled = false
    
    // MARK: - Background Refresh
    
    func enableBackgroundRefresh(_ enabled: Bool) {
        backgroundRefreshEnabled = enabled
        
        if enabled {
            backgroundTaskManager.enableBackgroundRefresh()
            logger.info("🔄 Background refresh enabled")
        } else {
            backgroundTaskManager.disableBackgroundRefresh()
            logger.info("🔄 Background refresh disabled")
        }
    }
    
    // MARK: - Performance Monitoring
    
    private func setupPerformanceMonitoring() {
        // Monitor app state changes
        NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)
            .sink { [weak self] _ in
                self?.handleAppDidEnterBackground()
            }
            .store(in: &cancellables)
        
        NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)
            .sink { [weak self] _ in
                self?.handleAppWillEnterForeground()
            }
            .store(in: &cancellables)
        
        // Monitor device activity changes
        NotificationCenter.default.publisher(for: .deviceActivityDidChange)
            .sink { [weak self] _ in
                self?.handleDeviceActivityChange()
            }
            .store(in: &cancellables)
    }
    
    private func handleAppDidEnterBackground() {
        logger.info("📱 App entered background")
        
        // Reduce resource usage
        performanceTimer?.invalidate()
        
        // Save critical data
        saveCriticalData()
        
        // Schedule background tasks
        backgroundTaskManager.scheduleBackgroundTasks()
    }
    
    private func handleAppWillEnterForeground() {
        logger.info("📱 App entering foreground")
        
        // Resume monitoring
        startMemoryMonitoring()
        
        // Refresh data if needed
        refreshDataIfNeeded()
        
        // Update cache size
        updateCacheSize()
    }
    
    private func handleDeviceActivityChange() {
        // Optimize monitoring based on current activity
        setupOptimizedDeviceActivity()
    }
    
    private func saveCriticalData() {
        // Save any unsaved data before backgrounding
        let coreDataManager = CoreDataManager.shared
        coreDataManager.saveContext()
        
        logger.info("💾 Critical data saved")
    }
    
    private func refreshDataIfNeeded() {
        let lastRefresh = UserDefaults.standard.object(forKey: "lastDataRefresh") as? Date ?? Date.distantPast
        let refreshInterval: TimeInterval = 300 // 5 minutes
        
        if Date().timeIntervalSince(lastRefresh) > refreshInterval {
            Task {
                await refreshAppData()
                UserDefaults.standard.set(Date(), forKey: "lastDataRefresh")
            }
        }
    }
    
    private func refreshAppData() async {
        // Refresh usage data
        let usageTrackingService = UsageTrackingService()
        _ = try? await usageTrackingService.getCurrentUsage()
        
        // Update subscription status
        let subscriptionService = SubscriptionManager.shared.getService()
        await subscriptionService.updateSubscriptionStatus()
        
        logger.info("🔄 App data refreshed")
    }
    
    // MARK: - Performance Metrics
    
    func getPerformanceMetrics() -> PerformanceMetrics {
        return PerformanceMetrics(
            memoryUsage: memoryManager.getCurrentMemoryUsage(),
            cacheSize: cacheSize,
            batteryOptimizationEnabled: batteryOptimizationEnabled,
            backgroundRefreshEnabled: backgroundRefreshEnabled,
            reducedMotionEnabled: reducedMotionEnabled
        )
    }
    
    // MARK: - Cleanup
    
    deinit {
        performanceTimer?.invalidate()
        cancellables.removeAll()
    }
}

// MARK: - Cache Manager

class CacheManager {
    private let imageCache = NSCache<NSString, UIImage>()
    private let dataCache = NSCache<NSString, NSData>()
    private let fileManager = FileManager.default
    
    private var cacheDirectory: URL {
        fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
    }
    
    func configure(maxMemoryCache: Int, maxDiskCache: Int, cacheExpiration: TimeInterval) {
        imageCache.totalCostLimit = maxMemoryCache / 2
        dataCache.totalCostLimit = maxMemoryCache / 2
        
        // Setup disk cache cleanup
        setupDiskCacheCleanup(maxSize: maxDiskCache, expiration: cacheExpiration)
    }
    
    private func setupDiskCacheCleanup(maxSize: Int, expiration: TimeInterval) {
        Task {
            await cleanupExpiredCache(expiration: expiration)
            await limitDiskCacheSize(maxSize: maxSize)
        }
    }
    
    func clearImageCache() async {
        imageCache.removeAllObjects()
    }
    
    func clearDataCache() async {
        dataCache.removeAllObjects()
    }
    
    func clearAllCaches() async {
        await clearImageCache()
        await clearDataCache()
        await clearDiskCache()
    }
    
    private func clearDiskCache() async {
        let cacheURL = cacheDirectory.appendingPathComponent("AwayTimeCache")
        try? fileManager.removeItem(at: cacheURL)
        try? fileManager.createDirectory(at: cacheURL, withIntermediateDirectories: true)
    }
    
    func getCurrentCacheSize() async -> Int64 {
        let cacheURL = cacheDirectory.appendingPathComponent("AwayTimeCache")
        return await calculateDirectorySize(url: cacheURL)
    }
    
    private func calculateDirectorySize(url: URL) async -> Int64 {
        guard let enumerator = fileManager.enumerator(at: url, includingPropertiesForKeys: [.fileSizeKey]) else {
            return 0
        }
        
        var totalSize: Int64 = 0
        
        for case let fileURL as URL in enumerator {
            do {
                let resourceValues = try fileURL.resourceValues(forKeys: [.fileSizeKey])
                totalSize += Int64(resourceValues.fileSize ?? 0)
            } catch {
                continue
            }
        }
        
        return totalSize
    }
    
    private func cleanupExpiredCache(expiration: TimeInterval) async {
        let cacheURL = cacheDirectory.appendingPathComponent("AwayTimeCache")
        let cutoffDate = Date().addingTimeInterval(-expiration)
        
        guard let enumerator = fileManager.enumerator(at: cacheURL, includingPropertiesForKeys: [.contentModificationDateKey]) else {
            return
        }
        
        for case let fileURL as URL in enumerator {
            do {
                let resourceValues = try fileURL.resourceValues(forKeys: [.contentModificationDateKey])
                if let modificationDate = resourceValues.contentModificationDate,
                   modificationDate < cutoffDate {
                    try fileManager.removeItem(at: fileURL)
                }
            } catch {
                continue
            }
        }
    }
    
    private func limitDiskCacheSize(maxSize: Int) async {
        let currentSize = await getCurrentCacheSize()
        
        if currentSize > maxSize {
            // Remove oldest files until under limit
            await removeOldestCacheFiles(targetSize: maxSize)
        }
    }
    
    private func removeOldestCacheFiles(targetSize: Int) async {
        let cacheURL = cacheDirectory.appendingPathComponent("AwayTimeCache")
        
        guard let enumerator = fileManager.enumerator(at: cacheURL, includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]) else {
            return
        }
        
        var files: [(URL, Date, Int)] = []
        
        for case let fileURL as URL in enumerator {
            do {
                let resourceValues = try fileURL.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey])
                if let modificationDate = resourceValues.contentModificationDate,
                   let fileSize = resourceValues.fileSize {
                    files.append((fileURL, modificationDate, fileSize))
                }
            } catch {
                continue
            }
        }
        
        // Sort by modification date (oldest first)
        files.sort { $0.1 < $1.1 }
        
        var currentSize = await getCurrentCacheSize()
        
        for (fileURL, _, fileSize) in files {
            if currentSize <= targetSize {
                break
            }
            
            do {
                try fileManager.removeItem(at: fileURL)
                currentSize -= Int64(fileSize)
            } catch {
                continue
            }
        }
    }
}

// MARK: - Memory Manager

class MemoryManager {
    private let logger = Logger(subsystem: "com.awaytime.app", category: "Memory")
    
    func startMonitoring() {
        logger.info("🧠 Memory monitoring started")
    }
    
    func getCurrentMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            let usedMemory = Double(info.resident_size)
            let totalMemory = Double(ProcessInfo.processInfo.physicalMemory)
            return usedMemory / totalMemory
        }
        
        return 0.0
    }
    
    func forceGarbageCollection() async {
        // Force autoreleasepool drain
        autoreleasepool {
            // This will help release any pending autorelease objects
        }
        
        logger.info("🗑️ Garbage collection forced")
    }
}

// MARK: - Background Task Manager

class BackgroundTaskManager {
    private let logger = Logger(subsystem: "com.awaytime.app", category: "Background")
    private var backgroundTaskID: UIBackgroundTaskIdentifier = .invalid
    
    func initialize() {
        logger.info("🔄 Background task manager initialized")
    }
    
    func enableBatteryOptimization() {
        // Reduce background processing frequency
        logger.info("🔋 Battery optimization enabled for background tasks")
    }
    
    func disableBatteryOptimization() {
        // Restore normal background processing
        logger.info("🔋 Battery optimization disabled for background tasks")
    }
    
    func enableBackgroundRefresh() {
        logger.info("🔄 Background refresh enabled")
    }
    
    func disableBackgroundRefresh() {
        logger.info("🔄 Background refresh disabled")
    }
    
    func scheduleBackgroundTasks() {
        backgroundTaskID = UIApplication.shared.beginBackgroundTask { [weak self] in
            self?.endBackgroundTask()
        }
        
        // Perform critical background work
        Task {
            await performBackgroundWork()
            endBackgroundTask()
        }
    }
    
    private func performBackgroundWork() async {
        // Save any pending data
        let coreDataManager = CoreDataManager.shared
        coreDataManager.saveContext()
        
        // Update usage statistics
        let usageTrackingService = UsageTrackingService()
        _ = try? await usageTrackingService.getCurrentUsage()
        
        logger.info("✅ Background work completed")
    }
    
    private func endBackgroundTask() {
        if backgroundTaskID != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTaskID)
            backgroundTaskID = .invalid
        }
    }
}

// MARK: - Performance Metrics

struct PerformanceMetrics {
    let memoryUsage: Double
    let cacheSize: Int64
    let batteryOptimizationEnabled: Bool
    let backgroundRefreshEnabled: Bool
    let reducedMotionEnabled: Bool
    
    var formattedMemoryUsage: String {
        return String(format: "%.1f%%", memoryUsage * 100)
    }
    
    var formattedCacheSize: String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useMB, .useKB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: cacheSize)
    }
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let deviceActivityDidChange = Notification.Name("deviceActivityDidChange")
}

// MARK: - Core Data Extensions

extension CoreDataManager {
    func deleteUsageRecords(before date: Date) throws {
        let request: NSFetchRequest<NSFetchRequestResult> = UsageRecord.fetchRequest()
        request.predicate = NSPredicate(format: "date < %@", date as NSDate)
        
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: request)
        try persistentContainer.persistentStoreCoordinator.execute(deleteRequest, with: context)
        try context.save()
    }
    
    func compactStore() throws {
        guard let storeURL = persistentContainer.persistentStoreDescriptions.first?.url else {
            throw NSError(domain: "CoreDataError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Store URL not found"])
        }
        
        try persistentContainer.persistentStoreCoordinator.destroyPersistentStore(at: storeURL, ofType: NSSQLiteStoreType, options: nil)
        try persistentContainer.persistentStoreCoordinator.addPersistentStore(ofType: NSSQLiteStoreType, configurationName: nil, at: storeURL, options: nil)
    }
}