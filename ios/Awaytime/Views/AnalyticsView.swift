import SwiftUI
import Charts

struct AnalyticsView: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    @StateObject private var subscriptionService = SubscriptionManager.shared.getService()
    @State private var selectedTimeRange: TimeRange = .week
    @State private var selectedMetric: AnalyticsMetric = .screenTime
    @State private var showingExportSheet = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Time Range Selector
                    timeRangeSelector
                    
                    // Key Metrics Overview
                    keyMetricsSection
                    
                    // Main Chart
                    mainChartSection
                    
                    // Premium Analytics Features
                    premiumAnalyticsSection
                    
                    // App Breakdown
                    appBreakdownSection
                    
                    // Insights & Recommendations
                    insightsSection
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(AwayTimeColors.background)
            .navigationTitle("Analytics")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                viewModel.loadAnalyticsData()
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Refresh") {
                        viewModel.loadAnalyticsData()
                    }
                    .disabled(viewModel.isLoading)
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: { showingExportSheet = true }) {
                            Label("Export Data", systemImage: "square.and.arrow.up")
                        }
                        .requiresPremium(.exportData)
                        
                        Button(action: { /* Share insights */ }) {
                            Label("Share Insights", systemImage: "square.and.arrow.up")
                        }
                        
                        Button(action: { /* Settings */ }) {
                            Label("Analytics Settings", systemImage: "gear")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .sheet(isPresented: $showingExportSheet) {
            ExportDataView()
                .requiresPremium(.exportData)
        }
    }
    
    // MARK: - Time Range Selector
    
    private var timeRangeSelector: some View {
        HStack(spacing: 0) {
            ForEach(TimeRange.allCases, id: \.self) { range in
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        selectedTimeRange = range
                    }
                }) {
                    Text(range.displayName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(selectedTimeRange == range ? Color.purple : Color.clear)
                        )
                        .foregroundColor(selectedTimeRange == range ? .white : .primary)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }
    
    // MARK: - Key Metrics Section
    
    private var keyMetricsSection: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
            if viewModel.isLoading {
                ForEach(0..<4, id: \.self) { _ in
                    SkeletonView(width: CGFloat.infinity, height: 80, cornerRadius: 12)
                }
            } else {
                MetricCard(
                    title: "Daily Average",
                    value: viewModel.formattedDailyAverage,
                    change: "", // Could calculate week-over-week change
                    changeType: .neutral,
                    icon: "clock.fill",
                    color: .blue
                )
                
                MetricCard(
                    title: "Goal Achievement",
                    value: viewModel.formattedGoalAchievementRate,
                    change: "",
                    changeType: .positive,
                    icon: "target",
                    color: .green
                )
                
                MetricCard(
                    title: "Streak",
                    value: viewModel.formattedCurrentStreak,
                    change: "",
                    changeType: .positive,
                    icon: "flame.fill",
                    color: .orange
                )
                
                MetricCard(
                    title: "Focus Score",
                    value: viewModel.formattedFocusScore,
                    change: "",
                    changeType: .positive,
                    icon: "brain.head.profile",
                    color: .purple
                )
                .requiresPremium(.advancedAnalytics)
            }
        }
    }
    
    // MARK: - Main Chart Section
    
    private var mainChartSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Usage Trends")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Spacer()
                
                Menu {
                    ForEach(AnalyticsMetric.allCases, id: \.self) { metric in
                        Button(metric.displayName) {
                            selectedMetric = metric
                        }
                    }
                } label: {
                    HStack {
                        Text(selectedMetric.displayName)
                            .font(.subheadline)
                        Image(systemName: "chevron.down")
                            .font(.caption)
                    }
                    .foregroundColor(.secondary)
                }
            }
            
            // Chart Container
            VStack {
                if subscriptionService.canUseFeature(.advancedAnalytics) {
                    UsageChart(
                        timeRange: selectedTimeRange,
                        metric: selectedMetric
                    )
                    .frame(height: 200)
                } else {
                    BasicUsageChart()
                        .frame(height: 200)
                        .requiresPremium(.advancedAnalytics)
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
        }
    }
    
    // MARK: - Premium Analytics Section
    
    private var premiumAnalyticsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Advanced Insights")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 1), spacing: 12) {
                // Productivity Analysis
                if subscriptionService.canUseFeature(.advancedAnalytics) {
                    PremiumAnalyticsCard(
                        title: "Productivity Analysis",
                        description: "See how your app usage affects your productivity throughout the day",
                        icon: "chart.line.uptrend.xyaxis",
                        color: .green
                    ) {
                        AnyView(Text("Productivity Analysis Coming Soon").foregroundColor(.secondary))
                    }
                } else {
                    PremiumFeatureLockedView(feature: .advancedAnalytics) {
                        // Show paywall
                    }
                }
                
                // Usage Patterns
                if subscriptionService.canUseFeature(.advancedAnalytics) {
                    PremiumAnalyticsCard(
                        title: "Usage Patterns",
                        description: "Discover your digital habits and peak usage times",
                        icon: "waveform.path.ecg",
                        color: .blue
                    ) {
                        AnyView(Text("Usage Patterns Coming Soon").foregroundColor(.secondary))
                    }
                } else {
                    PremiumFeatureLockedView(feature: .advancedAnalytics) {
                        // Show paywall
                    }
                }
                
                // App Categories
                if subscriptionService.canUseFeature(.advancedAnalytics) {
                    PremiumAnalyticsCard(
                        title: "Category Breakdown",
                        description: "Analyze time spent across different app categories",
                        icon: "chart.pie.fill",
                        color: .orange
                    ) {
                        AnyView(Text("Category Breakdown Coming Soon").foregroundColor(.secondary))
                    }
                } else {
                    PremiumFeatureLockedView(feature: .advancedAnalytics) {
                        // Show paywall
                    }
                }
            }
        }
    }
    
    // MARK: - App Breakdown Section
    
    private var appBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Top Apps")
                .font(.title2)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                if viewModel.isLoading {
                    ForEach(0..<3, id: \.self) { _ in
                        SkeletonView(width: CGFloat.infinity, height: 60, cornerRadius: 12)
                    }
                } else {
                    ForEach(viewModel.appBreakdown, id: \.id) { app in
                        AppUsageRow(app: AppUsageData(
                            name: app.appName,
                            category: "Apps",
                            duration: formatDuration(app.usage),
                            percentage: Int(app.percentage * 100),
                            icon: "apps.iphone",
                            color: app.color
                        ))
                    }
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
        }
    }
    
    // MARK: - Insights Section
    
    private var insightsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Insights & Recommendations")
                .font(.title2)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                InsightCard(
                    type: .positive,
                    title: "Great Progress!",
                    description: "You've reduced your social media usage by 25% this week. Keep it up!",
                    icon: "arrow.down.circle.fill"
                )
                
                InsightCard(
                    type: .suggestion,
                    title: "Peak Usage Alert",
                    description: "Your highest usage is between 8-10 PM. Consider setting a wind-down routine.",
                    icon: "moon.fill"
                )
                .requiresPremium(.advancedAnalytics)
                
                InsightCard(
                    type: .warning,
                    title: "Weekend Spike",
                    description: "Weekend usage is 40% higher than weekdays. Try planning offline activities.",
                    icon: "exclamationmark.triangle.fill"
                )
                .requiresPremium(.advancedAnalytics)
            }
        }
    }
}

// MARK: - Supporting Views

struct MetricCard: View {
    let title: String
    let value: String
    let change: String
    let changeType: ChangeType
    let icon: String
    let color: Color
    
    enum ChangeType {
        case positive, negative, neutral
        
        var color: Color {
            switch self {
            case .positive: return .green
            case .negative: return .red
            case .neutral: return .secondary
            }
        }
        
        var icon: String {
            switch self {
            case .positive: return "arrow.up"
            case .negative: return "arrow.down"
            case .neutral: return "minus"
            }
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                    .font(.title3)
                
                Spacer()
                
                HStack(spacing: 4) {
                    Image(systemName: changeType.icon)
                        .font(.caption)
                    Text(change)
                        .font(.caption)
                        .fontWeight(.medium)
                }
                .foregroundColor(changeType.color)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
        )
    }
}

struct UsageChart: View {
    let timeRange: TimeRange
    let metric: AnalyticsMetric
    
    var body: some View {
        // This would be a real Chart in production
        VStack {
            Text("Advanced Chart")
                .font(.headline)
                .foregroundColor(.secondary)
            
            Text("Showing \(metric.displayName) for \(timeRange.displayName)")
                .font(.caption)
                .foregroundColor(.secondary)
            
            // Placeholder for actual chart
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.purple.opacity(0.1))
                .overlay(
                    Text("📊 Interactive Chart")
                        .foregroundColor(.purple)
                )
        }
    }
}

struct BasicUsageChart: View {
    var body: some View {
        VStack {
            Text("Basic Chart")
                .font(.headline)
                .foregroundColor(.secondary)
            
            // Placeholder for basic chart
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.gray.opacity(0.1))
                .overlay(
                    Text("📈 Basic View")
                        .foregroundColor(.gray)
                )
        }
    }
}

struct PremiumAnalyticsCard: View {
    let title: String
    let description: String
    let icon: String
    let color: Color
    let content: () -> AnyView
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                    .font(.title2)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
                    .font(.caption)
            }
            
            content()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
        )
    }
}

struct AppUsageRow: View {
    let app: AppUsageData
    
    var body: some View {
        HStack(spacing: 12) {
            // App icon placeholder
            RoundedRectangle(cornerRadius: 8)
                .fill(app.color.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: app.icon)
                        .foregroundColor(app.color)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text(app.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(app.category)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 2) {
                Text(app.duration)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("\(app.percentage)%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

struct InsightCard: View {
    let type: InsightType
    let title: String
    let description: String
    let icon: String
    
    enum InsightType {
        case positive, suggestion, warning
        
        var color: Color {
            switch self {
            case .positive: return .green
            case .suggestion: return .blue
            case .warning: return .orange
            }
        }
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(type.color)
                .font(.title3)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
            
            Spacer()
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(type.color.opacity(0.1))
        )
    }
}

// MARK: - Premium Feature Views

struct ProductivityAnalysisView: View {
    var body: some View {
        VStack {
            Text("📊 Productivity trends and correlations")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(height: 60)
    }
}

struct UsagePatternsView: View {
    var body: some View {
        VStack {
            Text("🕐 Peak usage times and patterns")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(height: 60)
    }
}

struct CategoryBreakdownView: View {
    var body: some View {
        VStack {
            Text("📱 Social • Entertainment • Productivity")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(height: 60)
    }
}

struct ExportDataView: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Text("Export Your Data")
                    .font(.title2)
                    .fontWeight(.bold)
                
                VStack(spacing: 16) {
                    ExportOption(
                        title: "CSV Export",
                        description: "Raw data for spreadsheet analysis",
                        icon: "tablecells",
                        action: { /* Export CSV */ }
                    )
                    
                    ExportOption(
                        title: "PDF Report",
                        description: "Formatted report with charts",
                        icon: "doc.text",
                        action: { /* Export PDF */ }
                    )
                    
                    ExportOption(
                        title: "JSON Data",
                        description: "Complete data for developers",
                        icon: "curlybraces",
                        action: { /* Export JSON */ }
                    )
                }
                
                Spacer()
            }
            .padding()
            .navigationTitle("Export")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}

struct ExportOption: View {
    let title: String
    let description: String
    let icon: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.purple)
                    .frame(width: 30)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    Text(description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemGray6))
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Supporting Models

enum TimeRange: CaseIterable {
    case day, week, month, year
    
    var displayName: String {
        switch self {
        case .day: return "Day"
        case .week: return "Week"
        case .month: return "Month"
        case .year: return "Year"
        }
    }
}

enum AnalyticsMetric: CaseIterable {
    case screenTime, pickups, notifications, focusTime
    
    var displayName: String {
        switch self {
        case .screenTime: return "Screen Time"
        case .pickups: return "Pickups"
        case .notifications: return "Notifications"
        case .focusTime: return "Focus Time"
        }
    }
}

struct AppUsageData {
    let name: String
    let category: String
    let duration: String
    let percentage: Int
    let icon: String
    let color: Color
}

// MARK: - Helper Functions

private func formatDuration(_ duration: TimeInterval) -> String {
    let hours = Int(duration) / 3600
    let minutes = (Int(duration) % 3600) / 60
    
    if hours > 0 {
        return "\(hours)h \(minutes)m"
    } else {
        return "\(minutes)m"
    }
}

// MARK: - Real Usage Chart

struct RealUsageChart: View {
    let data: [AnalyticsViewModel.UsageDataPoint]
    
    var body: some View {
        VStack {
            if data.isEmpty {
                Text("No usage data available")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                // Simple bar chart representation
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(data, id: \.id) { point in
                        VStack(spacing: 4) {
                            Rectangle()
                                .fill(point.goalMet ? Color.green : Color.red)
                                .frame(width: 30, height: max(4, CGFloat(point.usage / 3600) * 100))
                            
                            Text(dayLabel(for: point.date))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemGray6))
        )
    }
    
    private func dayLabel(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "E"
        return formatter.string(from: date)
    }
}

// MARK: - Analytics View Model

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
}

#Preview {
    AnalyticsView()
}
