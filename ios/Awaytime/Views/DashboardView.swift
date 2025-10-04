import SwiftUI
import DeviceActivity // Add this import
import FamilyControls

// Extension for custom notification names
extension Notification.Name {
    static let deviceActivityDataUpdated = Notification.Name("DeviceActivityDataUpdated")
}

struct DashboardView: View {
    @StateObject private var viewModel = DashboardViewModel()
    @StateObject private var permissionService = PermissionService()
    @StateObject private var goalTrackingService = GoalTrackingService()
    @EnvironmentObject private var coordinator: NavigationCoordinator
    @State private var showingPermissionSheet = false
    @State private var showingAppSelection = false
    @State private var showingLimitSetting = false
    @State private var showConfetti = false
    @State private var animationScale: CGFloat = 1.0

    // Define context and filter for DeviceActivityReport
    let context: DeviceActivityReport.Context = DeviceActivityReport.Context(rawValue: "totalActivity") ?? DeviceActivityReport.Context(rawValue: "default")
    var filter: DeviceActivityFilter {
        let now = Date()
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: now)
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        
        let startComponents = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: startOfDay)
        let endComponents = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: endOfDay)
        
        return DeviceActivityFilter(
            segment: .daily(
                during: DateInterval(start: startOfDay, end: endOfDay)
            ),
            users: .all,
            devices: .all
        )
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Uncomment and use DeviceActivityReport
                    DeviceActivityReport(context, filter: filter)
                        .frame(height: 0) // Make it invisible as it's just for data
                        .onReceive(NotificationCenter.default.publisher(for: .deviceActivityDataUpdated)) { _ in
                            // Trigger a refresh in the view model when new data is available
                            viewModel.refreshData()
                        }
                    // Header with brain mascot
                    headerSection
                    
                    // Brain health indicator
                    if !viewModel.isLoading {
                        BrainHealthIndicator(
                            screenTimeHours: Double(viewModel.todayUsageMinutes) / 60.0,
                            motivationAlignment: getMotivationAlignment(),
                            brainStressLevel: getBrainStressLevel()
                        )
                    }
                    
                    // Progress circle section
                    progressSection
                    
                    // Quick stats section
                    statsSection
                    
                    // Action buttons section
                    actionButtonsSection
                    
                    // Premium features section removed to avoid duplicate "Upgrade to Premium" buttons
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(AwayTimeColors.background)
            .navigationBarHidden(true)
            .onAppear {
                // Only show permission sheet if we truly need permission and don't already have it
                permissionService.updateAuthorizationStatus()

                // Add a small delay to let the permission service properly initialize
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                    if permissionService.needsPermission && permissionService.authorizationStatus != .approved {
                        showingPermissionSheet = true
                    }
                }
            }
            .sheet(isPresented: $showingPermissionSheet) {
                PermissionRequestView(onPermissionGranted: {
                    showingPermissionSheet = false
                })
            }
            .sheet(isPresented: $showingAppSelection) {
                AppSelectionView()
            }
            .sheet(isPresented: $showingLimitSetting) {
                LimitSettingView()
            }
            .onReceive(NotificationCenter.default.publisher(for: .showConfettiAnimation)) { _ in
                showConfetti = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    showConfetti = false
                }
            }
        }
    }
    
    // MARK: - Helper Methods
    
    private func getGoalRecommendation() -> GoalRecommendation? {
        return goalTrackingService.getGoalRecommendation(
            currentUsage: viewModel.todayUsageMinutes,
            currentLimit: viewModel.dailyLimitMinutes
        )
    }
    
    private func getBrainMascotState() -> BrainMascotView.MascotState {
        let progress = viewModel.usageProgress
        if progress <= 0.5 {
            return .happy
        } else if progress <= 0.8 {
            return .thinking
        } else if progress < 1.0 {
            return .concerned
        } else {
            return .concerned
        }
    }
    
    private func getBrainWelcomeMessage() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        let progress = viewModel.usageProgress
        
        if progress >= 1.0 {
            return "time to take a break"
        } else if progress >= 0.8 {
            return "almost at your limit"
        } else if hour < 12 {
            return "good morning!"
        } else if hour < 17 {
            return "how's your day going?"
        } else {
            return "evening check-in"
        }
    }
    
    private func getBrainSubtitle() -> String {
        let progress = viewModel.usageProgress
        let motivations = getUserMotivations()
        
        if progress >= 1.0 {
            return "you've reached your daily limit"
        } else if progress >= 0.8 {
            return "\(Int((1.0 - progress) * Double(viewModel.dailyLimitMinutes))) minutes left today"
        } else if motivations.contains(.improveFocus) {
            return "stay focused on what matters"
        } else if motivations.contains(.beMorePresent) {
            return "be present in the moment"
        } else {
            return "you're in control of your screen time"
        }
    }
    
    private func getUserMotivations() -> Set<UserMotivation.Reason> {
        // In real implementation, fetch from Core Data
        guard let savedReasons = UserDefaults.standard.array(forKey: "selectedMotivations") as? [String] else {
            return []
        }
        return Set(savedReasons.compactMap { UserMotivation.Reason(rawValue: $0) })
    }
    
    private func getMotivationAlignment() -> Double {
        let progress = viewModel.usageProgress
        if progress <= 0.7 {
            return 1.0 // Perfect alignment
        } else if progress <= 0.9 {
            return 0.6 // Good alignment
        } else {
            return 0.3 // Poor alignment
        }
    }
    
    private func getBrainStressLevel() -> Int {
        let usageHours = Double(viewModel.todayUsageMinutes) / 60.0
        
        if usageHours <= 2.0 {
            return 1 // Excellent
        } else if usageHours <= 4.0 {
            return 2 // Good
        } else if usageHours <= 6.0 {
            return 3 // Fair
        } else if usageHours <= 8.0 {
            return 4 // Poor
        } else {
            return 5 // Critical
        }
    }
    
    // MARK: - Header Section
    private var headerSection: some View {
        VStack(spacing: 16) {
            // Baby Fox mascot with welcome message
            BabyFoxMascot(
                usagePercent: Float(viewModel.usageProgress * 100),
                size: 120
            )
            
            VStack(spacing: 8) {
                Text("Awaytime")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(AwayTimeColors.primary)
                    .multilineTextAlignment(.center)
                
                Text("Take control of your screen time")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(.top, 20)
    }
    
    // MARK: - Progress Section
    private var progressSection: some View {
        VStack(spacing: 20) {
            // Enhanced Progress Circle with loading state
            ZStack {
                if viewModel.isLoading {
                    LoadingProgressCircle(size: 200, lineWidth: 12)
                } else {
                    EnhancedProgressCircle(
                        progress: viewModel.usageProgress,
                        size: 200,
                        lineWidth: 12
                    )
                    
                    VStack(spacing: 4) {
                        SmoothNumberTransition(
                            value: Int(viewModel.usageProgress * 100),
                            formatter: { "\($0)%" },
                            font: .system(size: 28, weight: .bold, design: .rounded),
                            color: progressTextColor
                        )
                        .awayTimeAccessibility(
                            label: "Usage progress",
                            value: "\(Int(viewModel.usageProgress * 100)) percent used today"
                        )
                        
                        Text("used today")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            // Time remaining with better styling and loading state
            VStack(spacing: 8) {
                if viewModel.isLoading {
                    SkeletonView(width: 150, height: 20, cornerRadius: 10)
                } else {
                    Text(viewModel.timeRemainingText)
                        .font(.headline)
                        .foregroundColor(timeRemainingColor)
                        .animation(.easeInOut, value: viewModel.timeRemainingMinutes)
                        .awayTimeAccessibility(
                            label: "Time remaining",
                            value: viewModel.timeRemainingText
                        )
                }
                
                // Goal recommendation with loading state
                if viewModel.isLoading {
                    SkeletonView(width: 200, height: 40, cornerRadius: 8)
                        .padding(.horizontal, 20)
                } else if let recommendation = getGoalRecommendation() {
                    GoalRecommendationCard(recommendation: recommendation)
                        .padding(.horizontal, 20)
                }
            }
        }
        .padding(.vertical, 20)
    }
    
    private var progressTextColor: Color {
        if viewModel.usageProgress <= 0.5 {
            return AwayTimeColors.success
        } else if viewModel.usageProgress <= 0.8 {
            return AwayTimeColors.primary
        } else {
            return AwayTimeColors.warning
        }
    }
    
    private var timeRemainingColor: Color {
        if viewModel.timeRemainingMinutes > 60 {
            return AwayTimeColors.success
        } else if viewModel.timeRemainingMinutes > 30 {
            return AwayTimeColors.primary
        } else {
            return AwayTimeColors.warning
        }
    }
    
    // MARK: - Stats Section
    private var statsSection: some View {
        VStack(spacing: 20) {
            // Streak visualization
            StreakVisualization(
                currentStreak: goalTrackingService.currentStreak,
                longestStreak: goalTrackingService.longestStreak
            )
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
            
            // Weekly progress
            NavigationLink(destination: AnalyticsView()) { // Added NavigationLink
                WeeklyProgressChart(weeklyData: goalTrackingService.weeklyProgress)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color(.systemBackground))
                            .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
                    )
            }
            .buttonStyle(PlainButtonStyle()) // To remove default button styling
        }
    }
    
    // MARK: - Action Buttons Section
    private var actionButtonsSection: some View {
        VStack(spacing: 16) {
            Button {
                showingAppSelection = true
            } label: {
                VStack {
                    ActionButton(
                        title: "Select Apps",
                        subtitle: viewModel.selectedApps.isEmpty ? "Choose apps to monitor" : "",
                        icon: "apps.iphone",
                        color: AwayTimeColors.primary
                    )
                    if let selection = viewModel.selectedApps.first { // Get the single FamilyActivitySelection
                        VStack(alignment: .leading, spacing: 4) {
                            if !selection.applicationTokens.isEmpty {
                                Label("\(selection.applicationTokens.count) apps selected", systemImage: "app.fill")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            if !selection.categoryTokens.isEmpty {
                                Label("\(selection.categoryTokens.count) categories selected", systemImage: "folder.fill")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            if !selection.webDomainTokens.isEmpty {
                                Label("\(selection.webDomainTokens.count) websites selected", systemImage: "globe")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.leading, 10) // Indent slightly for better visual
                    }
                }
            }
            .buttonStyle(PlainButtonStyle())
            .awayTimeHapticFeedback()
            .awayTimeAccessibility(
                label: "Select Apps",
                hint: "Tap to choose which apps to monitor",
                value: viewModel.selectedApps.isEmpty ? "No apps selected" : "\(viewModel.selectedApps.count) apps selected"
            )
            
            Button {
                showingLimitSetting = true
            } label: {
                ActionButton(
                    title: "Set Daily Limit",
                    subtitle: "Current: \(viewModel.dailyLimitMinutes / 60)h \(viewModel.dailyLimitMinutes % 60)m",
                    icon: "clock",
                    color: AwayTimeColors.accent
                )
            }
            .buttonStyle(PlainButtonStyle())
            .awayTimeHapticFeedback()
            .awayTimeAccessibility(
                label: "Set Daily Limit",
                hint: "Tap to set your daily usage limit",
                value: "Current limit: \(viewModel.dailyLimitMinutes / 60) hours \(viewModel.dailyLimitMinutes % 60) minutes"
            )
            
            if viewModel.isPremium {
                NavigationLink(destination: AnalyticsView()) {
                    ActionButton(
                        title: "View Analytics",
                        subtitle: "Detailed usage insights",
                        icon: "chart.bar",
                        color: AwayTimeColors.secondary
                    )
                }
                
                NavigationLink(destination: AppGroupsView()) {
                    ActionButton(
                        title: "App Groups",
                        subtitle: "Organize apps with individual limits",
                        icon: "folder.badge.plus",
                        color: .blue
                    )
                }
                
                NavigationLink(destination: ThemeCustomizationView()) {
                    ActionButton(
                        title: "Customize Theme",
                        subtitle: "Personalize your experience",
                        icon: "paintbrush.pointed",
                        color: .purple
                    )
                }
            } else {
                NavigationLink(destination: PremiumView()) {
                    ActionButton(
                        title: "Upgrade to Premium",
                        subtitle: "Unlock advanced features",
                        icon: "star.fill",
                        color: AwayTimeColors.warning
                    )
                }
                
                // Preview of premium features
                VStack(spacing: 12) {
                    Text("Premium Features")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    LazyVStack(spacing: 8) {
                        PremiumFeaturePreview(
                            title: "Advanced Analytics",
                            icon: "chart.line.uptrend.xyaxis",
                            color: .green
                        )
                        
                        PremiumFeaturePreview(
                            title: "Multiple App Groups",
                            icon: "folder.badge.plus",
                            color: .blue
                        )
                        
                        PremiumFeaturePreview(
                            title: "Custom Themes",
                            icon: "paintbrush.pointed",
                            color: .purple
                        )
                    }
                }
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color.purple.opacity(0.05))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.purple.opacity(0.2), lineWidth: 1)
                        )
                )
            }
            
            // Debug buttons for testing (remove in production)
            #if DEBUG
            Button {
                OnboardingManager.resetOnboarding()
                coordinator.startOnboarding()
            } label: {
                ActionButton(
                    title: "Reset Onboarding",
                    subtitle: "Debug: Show onboarding again",
                    icon: "arrow.clockwise",
                    color: .gray
                )
            }
            .buttonStyle(PlainButtonStyle())
            #endif
        }
    }
}

// MARK: - Supporting Views

struct PremiumFeaturePreview: View {
    let title: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.title3)
                .frame(width: 24)
            
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.primary)
            
            Spacer()
            
            Image(systemName: "crown.fill")
                .foregroundColor(.purple)
                .font(.caption)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemBackground))
        )
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let subtitle: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
            
            Text(value)
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .foregroundColor(color)
            
            Text(subtitle)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
        )
    }
}

struct ActionButton: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
                .frame(width: 32, height: 32)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(AwayTimeColors.cardBackground)
                .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 4)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(color.opacity(0.1), lineWidth: 1)
        )
    }
}

// MARK: - Goal Recommendation Card

struct GoalRecommendationCard: View {
    let recommendation: GoalRecommendation
    @State private var isVisible = false
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: recommendation.type.iconName)
                .foregroundColor(recommendation.type.color)
                .font(.title3)
                .frame(width: 24, height: 24)
                .scaleEffect(isVisible ? 1.0 : 0.8)
                .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.1), value: isVisible)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(recommendation.message)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                
                Text(recommendation.suggestion)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .opacity(isVisible ? 1.0 : 0.0)
            .animation(.easeInOut(duration: 0.5).delay(0.2), value: isVisible)
            
            Spacer()
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(recommendation.type.color.opacity(0.1))
                .scaleEffect(isVisible ? 1.0 : 0.95)
                .animation(.spring(response: 0.6, dampingFraction: 0.8), value: isVisible)
        )
        .awayTimeAccessibility(
            label: "Goal recommendation",
            value: "\(recommendation.message). \(recommendation.suggestion)"
        )
        .onAppear {
            withAnimation {
                isVisible = true
            }
        }
    }
}

// MARK: - Loading State View

struct DashboardLoadingView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header skeleton
                VStack(spacing: 8) {
                    SkeletonView(width: 150, height: 32, cornerRadius: 16)
                    SkeletonView(width: 200, height: 16, cornerRadius: 8)
                }
                .padding(.top, 40)
                
                // Progress circle skeleton
                SkeletonView(width: 200, height: 200, cornerRadius: 100)
                
                // Stats skeleton
                HStack(spacing: 16) {
                    ForEach(0..<3, id: \.self) { _ in
                        VStack(spacing: 8) {
                            SkeletonView(width: 60, height: 12, cornerRadius: 6)
                            SkeletonView(width: 40, height: 24, cornerRadius: 12)
                            SkeletonView(width: 50, height: 12, cornerRadius: 6)
                        }
                        .padding(.vertical, 16)
                        .frame(maxWidth: .infinity)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(AwayTimeColors.cardBackground)
                                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
                        )
                    }
                }
                
                // Action buttons skeleton
                VStack(spacing: 16) {
                    ForEach(0..<3, id: \.self) { _ in
                        HStack(spacing: 16) {
                            SkeletonView(width: 32, height: 32, cornerRadius: 16)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                SkeletonView(width: 120, height: 18, cornerRadius: 9)
                                SkeletonView(width: 160, height: 14, cornerRadius: 7)
                            }
                            
                            Spacer()
                            
                            SkeletonView(width: 16, height: 16, cornerRadius: 2)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 16)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(AwayTimeColors.cardBackground)
                                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
                        )
                    }
                }
                
                Spacer(minLength: 20)
            }
            .padding(.horizontal, 20)
        }
        .background(AwayTimeColors.background)
    }
}

// MARK: - Error State View

struct DashboardErrorView: View {
    let error: Error
    let onRetry: () -> Void
    
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            
            // Error icon
            ZStack {
                Circle()
                    .fill(AwayTimeColors.error.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 32))
                    .foregroundColor(AwayTimeColors.error)
            }
            
            // Error text
            VStack(spacing: 8) {
                Text("Something went wrong")
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                
                Text("We couldn't load your data. Please try again.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            // Retry button
            Button("Try Again", action: onRetry)
                .font(.headline)
                .foregroundColor(.white)
                .padding(.horizontal, 32)
                .padding(.vertical, 16)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(AwayTimeColors.primary)
                )
                .buttonStyle(PlainButtonStyle())
                .awayTimeHapticFeedback()
            
            Spacer()
        }
        .padding(.horizontal, 32)
        .background(AwayTimeColors.background)
    }
}

#Preview {
    DashboardView()
}
// MARK: - GoalRecommendation Extensions

extension GoalRecommendation.RecommendationType {
    var iconName: String {
        switch self {
        case .excellent:
            return "star.fill"
        case .good:
            return "checkmark.circle.fill"
        case .warning:
            return "exclamationmark.triangle.fill"
        case .exceeded:
            return "xmark.circle.fill"
        }
    }
    
    var color: Color {
        switch self {
        case .excellent:
            return AwayTimeColors.success
        case .good:
            return AwayTimeColors.primary
        case .warning:
            return AwayTimeColors.warning
        case .exceeded:
            return AwayTimeColors.error
        }
    }
}
