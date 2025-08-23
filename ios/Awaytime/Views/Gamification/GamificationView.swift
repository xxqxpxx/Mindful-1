import SwiftUI

struct GamificationView: View {
    @StateObject private var gamificationService = GamificationService()
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Header with level and XP
                levelHeader
                
                // Tab selector
                tabSelector
                
                // Content based on selected tab
                TabView(selection: $selectedTab) {
                    ProgressView()
                        .tag(0)
                    
                    AchievementsView()
                        .tag(1)
                    
                    BadgesView()
                        .tag(2)
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            }
            .navigationTitle("Your Journey")
            .navigationBarTitleDisplayMode(.large)
            .background(Color(.systemGroupedBackground))
        }
        .overlay(
            celebrationOverlay
        )
        .onAppear {
            gamificationService.checkAchievements()
        }
    }
    
    private var levelHeader: some View {
        VStack(spacing: 16) {
            // Level circle
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color.purple.opacity(0.8), Color.purple],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 120, height: 120)
                
                VStack(spacing: 4) {
                    Text("LEVEL")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.white.opacity(0.8))
                    
                    Text("\(gamificationService.currentLevel)")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
            }
            
            // XP Progress
            VStack(spacing: 8) {
                HStack {
                    Text("\(gamificationService.experiencePoints) XP")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Spacer()
                    
                    Text("\(gamificationService.getXPToNextLevel()) to next level")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                ProgressView(value: gamificationService.getProgressToNextLevel())
                    .progressViewStyle(LinearProgressViewStyle(tint: .purple))
                    .scaleEffect(x: 1, y: 2, anchor: .center)
            }
            .padding(.horizontal, 24)
        }
        .padding(.vertical, 24)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)
        .padding(.horizontal, 16)
    }
    
    private var tabSelector: some View {
        HStack(spacing: 0) {
            TabButton(title: "Progress", isSelected: selectedTab == 0) {
                withAnimation(.easeInOut(duration: 0.3)) {
                    selectedTab = 0
                }
            }
            
            TabButton(title: "Achievements", isSelected: selectedTab == 1) {
                withAnimation(.easeInOut(duration: 0.3)) {
                    selectedTab = 1
                }
            }
            
            TabButton(title: "Badges", isSelected: selectedTab == 2) {
                withAnimation(.easeInOut(duration: 0.3)) {
                    selectedTab = 2
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
    }
    
    private var celebrationOverlay: some View {
        Group {
            if gamificationService.showingCelebration {
                CelebrationView(type: gamificationService.celebrationType)
                    .transition(.opacity.combined(with: .scale))
            }
        }
        .animation(.spring(response: 0.6, dampingFraction: 0.8), value: gamificationService.showingCelebration)
    }
}

struct TabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .medium)
                    .foregroundColor(isSelected ? .purple : .secondary)
                
                Rectangle()
                    .fill(isSelected ? Color.purple : Color.clear)
                    .frame(height: 2)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

struct ProgressView: View {
    @StateObject private var gamificationService = GamificationService()
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Motivational message card
                motivationalCard
                
                // Recent achievements
                if !gamificationService.achievements.isEmpty {
                    recentAchievementsCard
                }
                
                // Statistics cards
                statisticsCards
            }
            .padding(16)
        }
    }
    
    private var motivationalCard: some View {
        let message = gamificationService.getMotivationalMessage()
        
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(message.icon)
                    .font(.title2)
                
                Text("Daily Motivation")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
            }
            
            Text(message.text)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.leading)
        }
        .padding(20)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
    
    private var recentAchievementsCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("🏆")
                    .font(.title2)
                
                Text("Recent Achievements")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
            }
            
            ForEach(gamificationService.achievements.prefix(3), id: \.id) { achievement in
                HStack(spacing: 12) {
                    Text(achievement.icon)
                        .font(.title3)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text(achievement.title)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        Text(achievement.description)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Text(achievement.rarity.rawValue)
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(achievement.rarity.color.opacity(0.2))
                        .foregroundColor(achievement.rarity.color)
                        .cornerRadius(8)
                }
                .padding(.vertical, 4)
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
    
    private var statisticsCards: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 16) {
            StatCard(
                title: "Current Level",
                value: "\(gamificationService.currentLevel)",
                icon: "⭐",
                color: .purple
            )
            
            StatCard(
                title: "Total XP",
                value: "\(gamificationService.experiencePoints)",
                icon: "✨",
                color: .blue
            )
            
            StatCard(
                title: "Achievements",
                value: "\(gamificationService.achievements.count)",
                icon = "🏆",
                color: .orange
            )
            
            StatCard(
                title: "Badges",
                value: "\(gamificationService.badges.count)",
                icon: "🎖️",
                color: .green
            )
        }
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 12) {
            Text(icon)
                .font(.title2)
            
            Text(value)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(color)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

struct AchievementsView: View {
    @StateObject private var gamificationService = GamificationService()
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(gamificationService.achievements, id: \.id) { achievement in
                    AchievementCard(achievement: achievement)
                }
                
                if gamificationService.achievements.isEmpty {
                    emptyState
                }
            }
            .padding(16)
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("🎯")
                .font(.system(size: 60))
            
            Text("No Achievements Yet")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("Keep using Awaytime mindfully to unlock your first achievement!")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity)
    }
}

struct AchievementCard: View {
    let achievement: Achievement
    
    var body: some View {
        HStack(spacing: 16) {
            // Icon
            Text(achievement.icon)
                .font(.title)
                .frame(width: 50, height: 50)
                .background(achievement.rarity.color.opacity(0.2))
                .cornerRadius(25)
            
            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(achievement.title)
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Spacer()
                    
                    Text(achievement.rarity.rawValue)
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(achievement.rarity.color.opacity(0.2))
                        .foregroundColor(achievement.rarity.color)
                        .cornerRadius(8)
                }
                
                Text(achievement.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                Text("Unlocked \(achievement.unlockedDate, style: .date)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

struct BadgesView: View {
    @StateObject private var gamificationService = GamificationService()
    
    var body: some View {
        ScrollView {
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 16) {
                ForEach(gamificationService.badges, id: \.id) { badge in
                    BadgeCard(badge: badge)
                }
            }
            .padding(16)
            
            if gamificationService.badges.isEmpty {
                emptyState
            }
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("🎖️")
                .font(.system(size: 60))
            
            Text("No Badges Yet")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("Earn badges by leveling up and unlocking achievements!")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity)
    }
}

struct BadgeCard: View {
    let badge: Badge
    
    var body: some View {
        VStack(spacing: 8) {
            Text(badge.icon)
                .font(.title)
            
            Text(badge.title)
                .font(.caption)
                .fontWeight(.medium)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

struct CelebrationView: View {
    let type: CelebrationType
    @State private var showConfetti = false
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
            
            VStack(spacing: 24) {
                celebrationContent
                
                Button("Continue") {
                    // This would be handled by the parent view
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
            .padding(32)
            .background(Color(.systemBackground))
            .cornerRadius(20)
            .shadow(radius: 20)
            .padding(32)
        }
        .onAppear {
            showConfetti = true
        }
        .overlay(
            ConfettiView(isActive: showConfetti)
        )
    }
    
    @ViewBuilder
    private var celebrationContent: some View {
        switch type {
        case .goalAchieved:
            VStack(spacing: 16) {
                Text("🎉")
                    .font(.system(size: 80))
                
                Text("Goal Achieved!")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Amazing work! You're building incredible habits.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
        case .levelUp(let from, let to):
            VStack(spacing: 16) {
                Text("⭐")
                    .font(.system(size: 80))
                
                Text("Level Up!")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Level \(from) → \(to)")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.purple)
                
                Text("Your dedication is paying off!")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
        case .achievementUnlocked(let achievement):
            VStack(spacing: 16) {
                Text(achievement.icon)
                    .font(.system(size: 80))
                
                Text("Achievement Unlocked!")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text(achievement.title)
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(achievement.rarity.color)
                
                Text(achievement.description)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
        case .streakMilestone(let days):
            VStack(spacing: 16) {
                Text("🔥")
                    .font(.system(size: 80))
                
                Text("Streak Milestone!")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("\(days) Days")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.orange)
                
                Text("You're on fire! Keep the momentum going.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
    }
}

struct ConfettiView: View {
    let isActive: Bool
    @State private var animate = false
    
    var body: some View {
        ZStack {
            ForEach(0..<50, id: \.self) { _ in
                ConfettiPiece()
                    .opacity(isActive ? 1 : 0)
                    .animation(
                        .easeOut(duration: Double.random(in: 1...3))
                        .delay(Double.random(in: 0...0.5)),
                        value: isActive
                    )
            }
        }
        .allowsHitTesting(false)
    }
}

struct ConfettiPiece: View {
    @State private var location = CGPoint(x: 0, y: 0)
    @State private var opacity: Double = 1
    
    let colors: [Color] = [.red, .blue, .green, .yellow, .purple, .orange, .pink]
    
    var body: some View {
        Rectangle()
            .fill(colors.randomElement() ?? .blue)
            .frame(width: 8, height: 8)
            .position(location)
            .opacity(opacity)
            .onAppear {
                withAnimation(.easeOut(duration: 3)) {
                    location = CGPoint(
                        x: CGFloat.random(in: 0...UIScreen.main.bounds.width),
                        y: UIScreen.main.bounds.height + 100
                    )
                    opacity = 0
                }
            }
    }
}

#Preview {
    GamificationView()
}