import SwiftUI

// MARK: - Enhanced Progress Circle

struct EnhancedProgressCircle: View {
    let progress: Double
    let size: CGFloat
    let lineWidth: CGFloat
    let showAnimation: Bool
    
    @State private var animatedProgress: Double = 0
    
    init(progress: Double, size: CGFloat = 200, lineWidth: CGFloat = 12, showAnimation: Bool = true) {
        self.progress = progress
        self.size = size
        self.lineWidth = lineWidth
        self.showAnimation = showAnimation
    }
    
    var body: some View {
        ZStack {
            // Background circle
            Circle()
                .stroke(AwayTimeColors.secondary.opacity(0.2), lineWidth: lineWidth)
                .frame(width: size, height: size)
            
            // Progress circle with gradient
            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: progressColors),
                        center: .center,
                        startAngle: .degrees(-90),
                        endAngle: .degrees(270)
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .frame(width: size, height: size)
                .rotationEffect(.degrees(-90))
                .animation(
                    showAnimation ? .easeInOut(duration: 1.5) : .none,
                    value: animatedProgress
                )
            
            // Glow effect for high progress
            if progress > 0.8 {
                Circle()
                    .trim(from: 0, to: animatedProgress)
                    .stroke(
                        progressColors.last ?? AwayTimeColors.primary,
                        style: StrokeStyle(lineWidth: lineWidth * 0.3, lineCap: .round)
                    )
                    .frame(width: size, height: size)
                    .rotationEffect(.degrees(-90))
                    .blur(radius: 3)
                    .opacity(0.6)
            }
        }
        .onAppear {
            if showAnimation {
                withAnimation(.easeInOut(duration: 1.5).delay(0.2)) {
                    animatedProgress = progress
                }
            } else {
                animatedProgress = progress
            }
        }
        .onChange(of: progress) { newProgress in
            withAnimation(.spring(response: 0.8, dampingFraction: 0.8, blendDuration: 0.2)) {
                animatedProgress = newProgress
            }
        }
    }
    
    private var progressColors: [Color] {
        if progress <= 0.5 {
            return [AwayTimeColors.success, AwayTimeColors.primary]
        } else if progress <= 0.8 {
            return [AwayTimeColors.primary, AwayTimeColors.warning]
        } else {
            return [AwayTimeColors.warning, .red]
        }
    }
}

// MARK: - Weekly Progress Chart

struct WeeklyProgressChart: View {
    let weeklyData: [Bool]
    let animated: Bool
    
    @State private var animatedHeights: [CGFloat] = Array(repeating: 0, count: 7)
    
    private let dayLabels = ["S", "M", "T", "W", "T", "F", "S"]
    private let barWidth: CGFloat = 24
    private let maxHeight: CGFloat = 40
    
    init(weeklyData: [Bool], animated: Bool = true) {
        self.weeklyData = weeklyData.count == 7 ? weeklyData : Array(repeating: false, count: 7)
        self.animated = animated
    }
    
    var body: some View {
        VStack(spacing: 12) {
            Text("This Week")
                .font(.headline)
                .foregroundColor(.primary)
            
            HStack(spacing: 8) {
                ForEach(0..<7, id: \.self) { index in
                    VStack(spacing: 6) {
                        // Progress bar
                        RoundedRectangle(cornerRadius: 4)
                            .fill(weeklyData[index] ? AwayTimeColors.success : AwayTimeColors.secondary.opacity(0.3))
                            .frame(width: barWidth, height: animatedHeights[index])
                            .animation(
                                animated ? .easeInOut(duration: 0.8).delay(Double(index) * 0.1) : .none,
                                value: animatedHeights[index]
                            )
                        
                        // Day label
                        Text(dayLabels[index])
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .onAppear {
            if animated {
                for index in 0..<7 {
                    animatedHeights[index] = weeklyData[index] ? maxHeight : maxHeight * 0.2
                }
            } else {
                animatedHeights = weeklyData.map { $0 ? maxHeight : maxHeight * 0.2 }
            }
        }
    }
}

// MARK: - Usage Trend Chart

struct UsageTrendChart: View {
    let usageData: [Int] // Usage in minutes for the last 7 days
    let limit: Int
    
    @State private var animatedData: [CGFloat] = Array(repeating: 0, count: 7)
    
    private let chartHeight: CGFloat = 120
    private let dayLabels = ["S", "M", "T", "W", "T", "F", "S"]
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Usage Trend")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text("Limit: \(formatTime(limit))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Chart area
            ZStack(alignment: .bottom) {
                // Limit line
                Rectangle()
                    .fill(AwayTimeColors.warning.opacity(0.3))
                    .frame(height: 1)
                    .position(x: UIScreen.main.bounds.width / 2, y: chartHeight - limitLinePosition)
                
                // Usage bars
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(0..<7, id: \.self) { index in
                        VStack(spacing: 4) {
                            // Usage bar
                            RoundedRectangle(cornerRadius: 3)
                                .fill(barColor(for: index))
                                .frame(width: 20, height: animatedData[index])
                                .animation(
                                    .easeInOut(duration: 1.0).delay(Double(index) * 0.1),
                                    value: animatedData[index]
                                )
                            
                            // Day label
                            Text(dayLabels[index])
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .frame(height: chartHeight)
        }
        .onAppear {
            updateAnimatedData()
        }
        .onChange(of: usageData) { _ in
            updateAnimatedData()
        }
    }
    
    private func updateAnimatedData() {
        let maxUsage = max(usageData.max() ?? 0, limit)
        
        for index in 0..<min(usageData.count, 7) {
            let normalizedHeight = CGFloat(usageData[index]) / CGFloat(maxUsage) * chartHeight
            animatedData[index] = max(normalizedHeight, 4) // Minimum height for visibility
        }
    }
    
    private var limitLinePosition: CGFloat {
        let maxUsage = max(usageData.max() ?? 0, limit)
        return CGFloat(limit) / CGFloat(maxUsage) * chartHeight
    }
    
    private func barColor(for index: Int) -> Color {
        guard index < usageData.count else { return AwayTimeColors.secondary.opacity(0.3) }
        
        let usage = usageData[index]
        if usage <= limit {
            return AwayTimeColors.success
        } else if usage <= limit * 1.2 {
            return AwayTimeColors.warning
        } else {
            return .red
        }
    }
    
    private func formatTime(_ minutes: Int) -> String {
        let hours = minutes / 60
        let mins = minutes % 60
        
        if hours > 0 {
            return "\(hours)h \(mins)m"
        } else {
            return "\(mins)m"
        }
    }
}

// MARK: - Streak Visualization

struct StreakVisualization: View {
    let currentStreak: Int
    let longestStreak: Int
    let animated: Bool
    
    @State private var animatedCurrentStreak: Int = 0
    @State private var showCelebration = false
    
    init(currentStreak: Int, longestStreak: Int, animated: Bool = true) {
        self.currentStreak = currentStreak
        self.longestStreak = longestStreak
        self.animated = animated
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Current streak
            VStack(spacing: 8) {
                HStack {
                    Image(systemName: "flame.fill")
                        .foregroundColor(streakColor)
                        .font(.title2)
                    
                    Text("\(animatedCurrentStreak)")
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                        .foregroundColor(streakColor)
                        .contentTransition(.numericText())
                }
                
                Text("Day Streak")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            // Progress to next milestone
            if let nextMilestone = getNextMilestone() {
                VStack(spacing: 8) {
                    Text("Next milestone: \(nextMilestone) days")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    ProgressView(value: Double(currentStreak), total: Double(nextMilestone))
                        .tint(AwayTimeColors.primary)
                        .scaleEffect(y: 2)
                }
            }
            
            // Longest streak
            if longestStreak > currentStreak {
                Text("Best: \(longestStreak) days")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .onAppear {
            if animated {
                animateStreak()
            } else {
                animatedCurrentStreak = currentStreak
            }
        }
        .onChange(of: currentStreak) { newStreak in
            if newStreak > animatedCurrentStreak {
                showCelebration = true
                animateStreak()
            }
        }
    }
    
    private func animateStreak() {
        let duration = 1.5
        let steps = max(currentStreak, 1)
        let stepDuration = duration / Double(steps)
        
        for i in 0...currentStreak {
            DispatchQueue.main.asyncAfter(deadline: .now() + stepDuration * Double(i)) {
                withAnimation(.easeOut(duration: 0.3)) {
                    animatedCurrentStreak = i
                }
            }
        }
    }
    
    private var streakColor: Color {
        if currentStreak >= 30 {
            return .orange
        } else if currentStreak >= 7 {
            return AwayTimeColors.success
        } else if currentStreak >= 3 {
            return AwayTimeColors.primary
        } else {
            return .secondary
        }
    }
    
    private func getNextMilestone() -> Int? {
        let milestones = [3, 7, 14, 30, 60, 100]
        return milestones.first { $0 > currentStreak }
    }
}

// MARK: - Goal Progress Card

struct GoalProgressCard: View {
    let title: String
    let current: Int
    let target: Int
    let unit: String
    let color: Color
    let icon: String
    
    var progress: Double {
        guard target > 0 else { return 0 }
        return min(Double(current) / Double(target), 1.0)
    }
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                    .font(.title3)
                
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Spacer()
            }
            
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(current)")
                        .font(.system(size: 24, weight: .bold, design: .rounded))
                        .foregroundColor(color)
                    
                    Text("of \(target) \(unit)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                CircularProgressView(progress: progress, color: color, size: 40)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        )
    }
}

// MARK: - Circular Progress View

struct CircularProgressView: View {
    let progress: Double
    let color: Color
    let size: CGFloat
    
    var body: some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.2), lineWidth: 3)
            
            Circle()
                .trim(from: 0, to: progress)
                .stroke(color, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 1.0), value: progress)
        }
        .frame(width: size, height: size)
    }
}