import SwiftUI
import Combine

@MainActor
class AnalyticsViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var dailyAverage: TimeInterval = 0
    @Published var goalAchievementRate: Double = 0
    @Published var currentStreak: Int = 0
    @Published var focusScore: Double = 0
    @Published var weeklyData: [UsageDataPoint] = []
    @Published var monthlyData: [UsageDataPoint] = []
    @Published var appBreakdown: [AppUsageBreakdown] = []
    @Published var error: String?
    
    private let coreDataManager = CoreDataManager.shared
    private let goalTrackingService = GoalTrackingService()
    
    struct UsageDataPoint: Identifiable {
        let id = UUID()
        let date: Date
        let usage: TimeInterval
        let limit: TimeInterval
        let goalMet: Bool
    }
    
    struct AppUsageBreakdown: Identifiable {
        let id = UUID()
        let appName: String
        let usage: TimeInterval
        let percentage: Double
        let color: Color
    }
    
    init() {
        loadAnalyticsData()
    }
    
    func loadAnalyticsData() {
        isLoading = true
        error = nil
        
        Task {
            do {
                // Load usage records for analysis (last 30 days)
                let calendar = Calendar.current
                let endDate = Date()
                let startDate = calendar.date(byAdding: .day, value: -30, to: endDate)!
                let usageRecords = coreDataManager.fetchUsageRecords(from: startDate, to: endDate)
                
                // Calculate daily average
                await calculateDailyAverage(from: usageRecords)
                
                // Calculate goal achievement rate
                await calculateGoalAchievementRate(from: usageRecords)
                
                // Load current streak
                await loadCurrentStreak()
                
                // Calculate focus score
                await calculateFocusScore(from: usageRecords)
                
                // Generate weekly and monthly data
                await generateChartData(from: usageRecords)
                
                // Generate app breakdown
                await generateAppBreakdown(from: usageRecords)
                
                isLoading = false
                
            } catch {
                self.error = error.localizedDescription
                isLoading = false
                print("❌ Failed to load analytics data: \(error)")
            }
        }
    }
    
    private func calculateDailyAverage(from records: [UsageRecord]) async {
        guard !records.isEmpty else {
            dailyAverage = 0
            return
        }
        
        // Filter out records without dates and group by date
        let validRecords = records.compactMap { record -> (Date, UsageRecord)? in
            guard let date = record.date else { return nil }
            return (date, record)
        }
        
        let dailyUsage = Dictionary(grouping: validRecords) { item in
            Calendar.current.startOfDay(for: item.0)
        }.mapValues { dayRecords in
            dayRecords.reduce(0) { $0 + TimeInterval($1.1.usageMinutes * 60) }
        }
        
        let totalUsage = dailyUsage.values.reduce(0, +)
        dailyAverage = totalUsage / Double(dailyUsage.count)
    }
    
    private func calculateGoalAchievementRate(from records: [UsageRecord]) async {
        guard !records.isEmpty else {
            goalAchievementRate = 0
            return
        }
        
        // Filter out records without dates and group by date
        let validRecords = records.compactMap { record -> (Date, UsageRecord)? in
            guard let date = record.date else { return nil }
            return (date, record)
        }
        
        let dailyGoals = Dictionary(grouping: validRecords) { item in
            Calendar.current.startOfDay(for: item.0)
        }.mapValues { dayRecords in
            !dayRecords.contains { $0.1.limitExceeded }
        }
        
        let goalsAchieved = dailyGoals.values.filter { $0 }.count
        goalAchievementRate = Double(goalsAchieved) / Double(dailyGoals.count)
    }
    
    private func loadCurrentStreak() async {
        currentStreak = goalTrackingService.currentStreak
    }
    
    private func calculateFocusScore(from records: [UsageRecord]) async {
        // Focus score based on consistency and goal achievement
        let recentRecords = records.filter { record in
            guard let recordDate = record.date else { return false }
            return Calendar.current.dateInterval(of: .weekOfYear, for: Date())?.contains(recordDate) ?? false
        }
        
        guard !recentRecords.isEmpty else {
            focusScore = 0
            return
        }
        
        // Calculate based on goal achievement and usage consistency
        let goalAchievements = recentRecords.filter { !$0.limitExceeded }.count
        let achievementRate = Double(goalAchievements) / Double(recentRecords.count)
        
        // Calculate consistency (lower variance = higher score)
        let usageValues = recentRecords.map { Double($0.usageMinutes) }
        let average = usageValues.reduce(0, +) / Double(usageValues.count)
        let variance = usageValues.map { pow($0 - average, 2) }.reduce(0, +) / Double(usageValues.count)
        let consistencyScore = max(0, 1 - (variance / (average * average)))
        
        // Combine achievement rate and consistency
        focusScore = (achievementRate * 0.7 + consistencyScore * 0.3) * 10
    }
    
    private func generateChartData(from records: [UsageRecord]) async {
        let calendar = Calendar.current
        let now = Date()
        
        // Generate weekly data (last 7 days)
        var weeklyPoints: [UsageDataPoint] = []
        for i in 0..<7 {
            let date = calendar.date(byAdding: .day, value: -i, to: now)!
            let dayStart = calendar.startOfDay(for: date)
            
            let dayRecords = records.filter { record in
                guard let recordDate = record.date else { return false }
                return calendar.isDate(recordDate, inSameDayAs: dayStart)
            }
            
            let totalUsage = dayRecords.reduce(0) { $0 + TimeInterval($1.usageMinutes * 60) }
            let limitExceeded = dayRecords.contains { $0.limitExceeded }
            
            // Get the daily limit from the most recent app group
            let appGroups = coreDataManager.fetchAppGroups()
            let dailyLimit = TimeInterval((appGroups.first?.dailyLimitMinutes ?? 120) * 60)
            
            weeklyPoints.append(UsageDataPoint(
                date: dayStart,
                usage: totalUsage,
                limit: dailyLimit,
                goalMet: !limitExceeded
            ))
        }
        
        weeklyData = weeklyPoints.reversed()
        
        // Generate monthly data (last 30 days, grouped by week)
        var monthlyPoints: [UsageDataPoint] = []
        for i in 0..<4 {
            let weekStart = calendar.date(byAdding: .weekOfYear, value: -i, to: now)!
            let weekEnd = calendar.date(byAdding: .day, value: 6, to: weekStart)!
            
            let weekRecords = records.filter { record in
                guard let recordDate = record.date else { return false }
                return recordDate >= weekStart && recordDate <= weekEnd
            }
            
            let totalUsage = weekRecords.reduce(0) { $0 + TimeInterval($1.usageMinutes * 60) }
            let averageUsage = totalUsage / 7 // Average per day
            
            let appGroups = coreDataManager.fetchAppGroups()
            let dailyLimit = TimeInterval((appGroups.first?.dailyLimitMinutes ?? 120) * 60)
            
            let goalsMetCount = weekRecords.filter { !$0.limitExceeded }.count
            let goalsMet = goalsMetCount > weekRecords.count / 2
            
            monthlyPoints.append(UsageDataPoint(
                date: weekStart,
                usage: averageUsage,
                limit: dailyLimit,
                goalMet: goalsMet
            ))
        }
        
        monthlyData = monthlyPoints.reversed()
    }
    
    private func generateAppBreakdown(from records: [UsageRecord]) async {
        // Group by app group name and calculate totals
        let appGroupUsage = Dictionary(grouping: records) { $0.appGroupName ?? "Unknown" }
            .mapValues { records in
                records.reduce(0) { $0 + TimeInterval($1.usageMinutes * 60) }
            }
        
        let totalUsage = appGroupUsage.values.reduce(0, +)
        guard totalUsage > 0 else {
            appBreakdown = []
            return
        }
        
        let colors: [Color] = [.blue, .green, .orange, .purple, .red, .pink, .yellow, .cyan]
        
        appBreakdown = appGroupUsage.enumerated().map { index, item in
            AppUsageBreakdown(
                appName: item.key,
                usage: item.value,
                percentage: item.value / totalUsage,
                color: colors[index % colors.count]
            )
        }.sorted { $0.usage > $1.usage }
    }
    
    // MARK: - Formatted Data for UI
    
    var formattedDailyAverage: String {
        formatDuration(dailyAverage)
    }
    
    var formattedGoalAchievementRate: String {
        "\(Int(goalAchievementRate * 100))%"
    }
    
    var formattedCurrentStreak: String {
        "\(currentStreak) days"
    }
    
    var formattedFocusScore: String {
        String(format: "%.1f/10", focusScore)
    }
    
    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = (Int(duration) % 3600) / 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
    
    // MARK: - Export Functionality
    
    func exportData() -> String {
        var csvContent = "Date,Usage (minutes),Limit (minutes),Goal Met,App Group\n"
        
        // Get records from the last 30 days
        let calendar = Calendar.current
        let endDate = Date()
        let startDate = calendar.date(byAdding: .day, value: -30, to: endDate)!
        let records = coreDataManager.fetchUsageRecords(from: startDate, to: endDate)
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        
        for record in records {
            guard let recordDate = record.date else { continue }
            csvContent += "\(formatter.string(from: recordDate)),\(record.usageMinutes),\(record.usageMinutes),\(!record.limitExceeded),\(record.appGroupName ?? "Unknown")\n"
        }
        
        return csvContent
    }
}