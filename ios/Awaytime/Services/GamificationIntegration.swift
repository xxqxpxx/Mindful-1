import SwiftUI
import Combine

/// Integration service that connects gamification with other app services
@MainActor
class GamificationIntegration: ObservableObject {
    private let gamificationService = GamificationService()
    private let goalTrackingService = GoalTrackingService()
    private let usageTrackingService = UsageTrackingService()
    private let notificationService = NotificationService()
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupIntegrations()
    }
    
    private func setupIntegrations() {
        // Listen for goal achievements
        NotificationCenter.default.publisher(for: .goalAchieved)
            .sink { [weak self] notification in
                self?.handleGoalAchieved(notification)
            }
            .store(in: &cancellables)
        
        // Listen for usage limit respected
        NotificationCenter.default.publisher(for: .usageLimitRespected)
            .sink { [weak self] notification in
                self?.handleLimitRespected(notification)
            }
            .store(in: &cancellables)
        
        // Listen for streak updates
        NotificationCenter.default.publisher(for: .streakUpdated)
            .sink { [weak self] notification in
                self?.handleStreakUpdated(notification)
            }
            .store(in: &cancellables)
        
        // Listen for weekly goal completion
        NotificationCenter.default.publisher(for: .weeklyGoalCompleted)
            .sink { [weak self] notification in
                self?.handleWeeklyGoalCompleted(notification)
            }
            .store(in: &cancellables)
        
        // Daily check for achievements
        setupDailyAchievementCheck()
    }
    
    // MARK: - Event Handlers
    
    private func handleGoalAchieved(_ notification: Notification) {
        gamificationService.awardExperience(for: .goalAchieved)
        gamificationService.checkAchievements()
        
        // Show contextual motivational message
        let message = gamificationService.getContextualMessage(for: .goalAchieved)
        showMotivationalToast(message: message)
        
        print("🎯 Goal achieved - XP awarded and achievements checked")
    }
    
    private func handleLimitRespected(_ notification: Notification) {
        gamificationService.awardExperience(for: .limitRespected)
        
        let message = gamificationService.getContextualMessage(for: .limitReached)
        showMotivationalToast(message: message)
        
        print("✅ Usage limit respected - XP awarded")
    }
    
    private func handleStreakUpdated(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let streakDays = userInfo["streakDays"] as? Int else { return }
        
        gamificationService.awardExperience(for: .streakDay)
        
        // Check for streak milestones
        let milestones = [3, 7, 14, 30, 60, 100]
        if milestones.contains(streakDays) {
            gamificationService.celebrationType = .streakMilestone(days: streakDays)
            gamificationService.showCelebration()
            
            let message = gamificationService.getContextualMessage(for: .streakMilestone(days: streakDays))
            showMotivationalToast(message: message)
        }
        
        print("🔥 Streak updated to \\(streakDays) days - XP awarded")
    }
    
    private func handleWeeklyGoalCompleted(_ notification: Notification) {
        gamificationService.awardExperience(for: .weeklyGoalMet)
        gamificationService.checkAchievements()
        
        print("📅 Weekly goal completed - bonus XP awarded")
    }
    
    // MARK: - Daily Achievement Check
    
    private func setupDailyAchievementCheck() {
        // Check achievements daily at midnight
        Timer.scheduledTimer(withTimeInterval: 24 * 60 * 60, repeats: true) { [weak self] _ in
            self?.performDailyAchievementCheck()
        }
        
        // Also check on app launch
        performDailyAchievementCheck()
    }
    
    private func performDailyAchievementCheck() {
        gamificationService.checkAchievements()
        
        // Check for approaching limits and show motivational messages
        checkForApproachingLimits()
    }
    
    private func checkForApproachingLimits() {
        let todayUsage = usageTrackingService.getTodayUsage()
        let goals = goalTrackingService.getCurrentGoals()
        
        for goal in goals {
            if let appUsage = todayUsage.first(where: { $0.appIdentifier == goal.appIdentifier }) {
                let usagePercentage = Double(appUsage.totalTime) / Double(goal.dailyLimit)
                
                // Show motivational message when approaching 80% of limit
                if usagePercentage >= 0.8 && usagePercentage < 1.0 {
                    let message = gamificationService.getContextualMessage(for: .approachingLimit)
                    showMotivationalToast(message: message)
                    break
                }
            }
        }
    }
    
    // MARK: - UI Integration
    
    private func showMotivationalToast(message: String) {
        // Post notification for UI to show toast
        NotificationCenter.default.post(
            name: .showMotivationalToast,
            object: nil,
            userInfo: ["message": message]
        )
    }
    
    // MARK: - Public Interface
    
    func getGamificationService() -> GamificationService {
        return gamificationService
    }
    
    func triggerManualAchievementCheck() {
        gamificationService.checkAchievements()
    }
    
    func getMotivationalMessageForContext(_ context: MessageContext) -> String {
        return gamificationService.getContextualMessage(for: context)
    }
    
    func awardBonusExperience(points: Int, reason: String) {
        // For special events or manual rewards
        gamificationService.experiencePoints += points
        
        NotificationCenter.default.post(
            name: .experienceGained,
            object: nil,
            userInfo: [
                "points": points,
                "action": reason
            ]
        )
        
        print("🌟 Bonus XP awarded: \\(points) for \\(reason)")
    }
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let goalAchieved = Notification.Name("goalAchieved")
    static let usageLimitRespected = Notification.Name("usageLimitRespected")
    static let streakUpdated = Notification.Name("streakUpdated")
    static let weeklyGoalCompleted = Notification.Name("weeklyGoalCompleted")
    static let showMotivationalToast = Notification.Name("showMotivationalToast")
    static let showConfettiAnimation = Notification.Name("showConfettiAnimation")
}

// MARK: - Motivational Toast View

struct MotivationalToastView: View {
    let message: String
    @State private var isVisible = false
    
    var body: some View {
        VStack {
            Spacer()
            
            HStack {
                Text("💜")
                    .font(.title3)
                
                Text(message)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)
                
                Spacer()
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.purple.opacity(0.9))
                    .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
            )
            .padding(.horizontal, 16)
            .offset(y: isVisible ? 0 : 100)
            .opacity(isVisible ? 1 : 0)
        }
        .onAppear {
            withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                isVisible = true
            }
            
            // Auto-hide after 4 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 4) {
                withAnimation(.easeOut(duration: 0.3)) {
                    isVisible = false
                }
            }
        }
    }
}

// MARK: - Experience Gained Animation View

struct ExperienceGainedView: View {
    let points: Int
    let action: String
    @State private var offset: CGFloat = 0
    @State private var opacity: Double = 1
    
    var body: some View {
        VStack {
            HStack {
                Spacer()
                
                VStack(spacing: 4) {
                    Text("+\\(points) XP")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.purple)
                    
                    Text(action)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(12)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(.systemBackground))
                        .shadow(color: .purple.opacity(0.3), radius: 4, x: 0, y: 2)
                )
                .offset(y: offset)
                .opacity(opacity)
                
                Spacer()
            }
            
            Spacer()
        }
        .onAppear {
            withAnimation(.easeOut(duration: 2)) {
                offset = -100
                opacity = 0
            }
        }
    }
}

// MARK: - Integration with Main App

extension ContentView {
    func setupGamificationIntegration() {
        let integration = GamificationIntegration()
        
        // Listen for motivational toast notifications
        NotificationCenter.default.addObserver(
            forName: .showMotivationalToast,
            object: nil,
            queue: .main
        ) { notification in
            if let message = notification.userInfo?["message"] as? String {
                // Show toast in UI
                showMotivationalToast(message: message)
            }
        }
        
        // Listen for experience gained notifications
        NotificationCenter.default.addObserver(
            forName: .experienceGained,
            object: nil,
            queue: .main
        ) { notification in
            if let points = notification.userInfo?["points"] as? Int,
               let action = notification.userInfo?["action"] as? String {
                // Show XP animation
                showExperienceAnimation(points: points, action: action)
            }
        }
    }
    
    private func showMotivationalToast(message: String) {
        // Implementation would depend on your toast system
        print("Showing motivational toast: \\(message)")
    }
    
    private func showExperienceAnimation(points: Int, action: String) {
        // Implementation would depend on your animation system
        print("Showing XP animation: +\\(points) XP for \\(action)")
    }
}