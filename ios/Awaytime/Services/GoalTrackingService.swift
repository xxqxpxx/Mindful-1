import SwiftUI
import Combine

@MainActor
class GoalTrackingService: ObservableObject {
    @Published var currentStreak = 0
    @Published var longestStreak = 0
    @Published var weeklyProgress: [WeeklyProgressData] = []
    
    private let coreDataManager = CoreDataManager.shared
    
    init() {
        loadRealData()
    }
    
    func getGoalRecommendation(currentUsage: Int, currentLimit: Int) -> GoalRecommendation? {
        let progress = Double(currentUsage) / Double(currentLimit)
        
        if progress <= 0.5 {
            return GoalRecommendation(
                type: .excellent,
                message: "Excellent progress!",
                suggestion: "Keep up the great work"
            )
        } else if progress <= 0.8 {
            return GoalRecommendation(
                type: .good,
                message: "Good progress",
                suggestion: "You're on track"
            )
        } else if progress < 1.0 {
            return GoalRecommendation(
                type: .warning,
                message: "Approaching limit",
                suggestion: "Consider taking a break"
            )
        } else {
            return GoalRecommendation(
                type: .exceeded,
                message: "Limit exceeded",
                suggestion: "Time for a digital detox"
            )
        }
    }
    
    private func loadRealData() {
        // Load real streak data
        loadStreakData()
        
        // Load real weekly progress
        loadWeeklyProgress()
    }
    
    private func loadStreakData() {
        // Calculate current streak from usage records
        let calendar = Calendar.current
        let today = Date()
        let thirtyDaysAgo = calendar.date(byAdding: .day, value: -30, to: today)!
        let usageRecords = coreDataManager.fetchUsageRecords(from: thirtyDaysAgo, to: today)
        
        // Group by date and check if daily goal was met
        let startOfToday = calendar.startOfDay(for: today)
        
        var streak = 0
        var maxStreak = 0
        var currentStreakCount = 0
        
        // Check last 30 days for streaks
        for i in 0..<30 {
            let date = calendar.date(byAdding: .day, value: -i, to: startOfToday)!
            let dayRecords = usageRecords.filter { record in
                guard let recordDate = record.date else { return false }
                return calendar.isDate(recordDate, inSameDayAs: date)
            }
            
            let goalMet = !dayRecords.contains { $0.limitExceeded }
            
            if goalMet {
                currentStreakCount += 1
                if i == 0 { // Today or most recent day
                    streak = currentStreakCount
                }
            } else {
                maxStreak = max(maxStreak, currentStreakCount)
                if i == 0 {
                    streak = 0
                }
                currentStreakCount = 0
            }
        }
        
        maxStreak = max(maxStreak, currentStreakCount)
        
        currentStreak = streak
        longestStreak = maxStreak
    }
    
    private func loadWeeklyProgress() {
        let calendar = Calendar.current
        let today = Date()
        
        // Get current app group limit
        let appGroups = coreDataManager.fetchAppGroups()
        let dailyLimit = Int(appGroups.first?.dailyLimitMinutes ?? 180)
        
        // Get the start of the current week (Monday)
        guard let startOfWeek = calendar.date(byAdding: .day, value: -(calendar.firstWeekday - 1 + (calendar.component(.weekday, from: today) - 1)) % 7, to: today) else { return }
        
        var weeklyData: [WeeklyProgressData] = []
        for i in 0..<7 {
            guard let targetDate = calendar.date(byAdding: .day, value: i, to: startOfWeek) else { continue }
            
            let dayUsageRecords = coreDataManager.fetchUsageRecords(for: targetDate)
            let dayUsageMinutes = dayUsageRecords.reduce(0) { $0 + Int($1.usageMinutes) }
            
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "EEE" // Mon, Tue, etc.
            let dayName = dateFormatter.string(from: targetDate)
            
            weeklyData.append(WeeklyProgressData(
                day: dayName,
                usage: dayUsageMinutes,
                limit: dailyLimit
            ))
        }
        weeklyProgress = weeklyData
    }
}

struct GoalRecommendation {
    let type: RecommendationType
    let message: String
    let suggestion: String
    
    enum RecommendationType {
        case excellent
        case good
        case warning
        case exceeded
    }
}

struct WeeklyProgressData {
    let day: String
    let usage: Int
    let limit: Int
}