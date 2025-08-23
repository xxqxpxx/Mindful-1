import SwiftUI
import FamilyControls
import Lottie

struct OnboardingFlow: View {
    @StateObject private var onboardingManager = OnboardingManager()
    @Environment(\.dismiss) private var dismiss
    
    // Callback to notify when onboarding is completed
    var onCompleted: (() -> Void)?
    
    var body: some View {
        NavigationView {
            ZStack {
                // Purple gradient background
                LinearGradient(
                    colors: [
                        Color.purple.opacity(0.1),
                        Color.purple.opacity(0.05),
                        Color.clear
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()
                
                TabView(selection: $onboardingManager.currentStep) {
                    // Welcome Screen
                    WelcomeScreen()
                        .tag(OnboardingStep.welcome)
                    
                    // Features Overview
                    FeaturesOverviewScreen()
                        .tag(OnboardingStep.features)
                    
                    // Permissions Request
                    PermissionsScreen()
                        .tag(OnboardingStep.permissions)
                    
                    // App Selection
                    AppSelectionScreen()
                        .tag(OnboardingStep.appSelection)
                    
                    // Goal Setting
                    GoalSettingScreen()
                        .tag(OnboardingStep.goalSetting)
                    
                    // Completion
                    CompletionScreen()
                        .tag(OnboardingStep.completion)
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.3), value: onboardingManager.currentStep)
                
                // Progress indicator
                VStack {
                    HStack {
                        Spacer()
                        
                        Button("Skip") {
                            onboardingManager.skipOnboarding()
                        }
                        .foregroundColor(.purple)
                        .opacity(onboardingManager.canSkip ? 1 : 0)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 8)
                    
                    Spacer()
                    
                    // Bottom navigation
                    OnboardingBottomNavigation()
                        .environmentObject(onboardingManager)
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            onboardingManager.startOnboarding()
        }
        .onChange(of: onboardingManager.isCompleted) { completed in
            if completed {
                onCompleted?()
                dismiss()
            }
        }
    }
}

// MARK: - Welcome Screen

struct WelcomeScreen: View {
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            // App logo and branding
            VStack(spacing: 24) {
                ZStack {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(
                            LinearGradient(
                                colors: [Color.purple, Color.purple.opacity(0.8)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 100, height: 100)
                    
                    SequentialFoxAnimationView()
                        .frame(width: 80, height: 80)
                }
                
                VStack(spacing: 12) {
                    Text("Welcome to Awaytime")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Take control of your digital wellness with mindful app usage tracking")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }
            
            // Key benefits
            VStack(spacing: 20) {
                OnboardingBenefitRow(
                    icon: "clock.fill",
                    title: "Track Your Time",
                    description: "See exactly how much time you spend on each app"
                )
                
                OnboardingBenefitRow(
                    icon: "target",
                    title: "Set Healthy Limits",
                    description: "Create realistic goals and stick to them"
                )
                
                OnboardingBenefitRow(
                    icon: "chart.line.uptrend.xyaxis",
                    title: "Build Better Habits",
                    description: "Track your progress and celebrate achievements"
                )
            }
            .padding(.horizontal, 32)
            
            Spacer()
        }
        .padding(.vertical, 40)
    }
}

// MARK: - Features Overview Screen

struct FeaturesOverviewScreen: View {
    @State private var currentFeature = 0
    private let features = [
        OnboardingFeature(
            icon: "apps.iphone",
            title: "Smart App Monitoring",
            description: "Automatically track your app usage without any manual input",
            color: .blue
        ),
        OnboardingFeature(
            icon: "bell.badge",
            title: "Gentle Reminders",
            description: "Get notified when you're approaching your daily limits",
            color: .orange
        ),
        OnboardingFeature(
            icon: "shield.checkered",
            title: "Mindful Blocking",
            description: "Temporarily block distracting apps when you reach your limits",
            color: .green
        ),
        OnboardingFeature(
            icon: "chart.bar.fill",
            title: "Progress Insights",
            description: "See your improvement over time with detailed analytics",
            color: .purple
        )
    ]
    
    var body: some View {
        VStack(spacing: 40) {
            VStack(spacing: 16) {
                Text("How Awaytime Helps")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("Discover the features that will transform your digital habits")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            // Feature carousel
            TabView(selection: $currentFeature) {
                ForEach(features.indices, id: \.self) { index in
                    FeatureCard(feature: features[index])
                        .tag(index)
                }
            }
            .tabViewStyle(PageTabViewStyle())
            .frame(height: 300)
            .onAppear {
                startAutoScroll()
            }
            
            // Feature indicators
            HStack(spacing: 8) {
                ForEach(features.indices, id: \.self) { index in
                    Circle()
                        .fill(index == currentFeature ? Color.purple : Color.gray.opacity(0.3))
                        .frame(width: 8, height: 8)
                        .animation(.easeInOut(duration: 0.3), value: currentFeature)
                }
            }
            
            Spacer()
        }
        .padding(.vertical, 40)
    }
    
    private func startAutoScroll() {
        Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { _ in
            withAnimation(.easeInOut(duration: 0.5)) {
                currentFeature = (currentFeature + 1) % features.count
            }
        }
    }
}

// MARK: - Permissions Screen

struct PermissionsScreen: View {
    @StateObject private var permissionManager = PermissionManager()
    
    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 16) {
                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.purple)
                
                Text("Privacy & Permissions")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("Awaytime needs a few permissions to help you track and manage your app usage")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            VStack(spacing: 20) {
                PermissionCard(
                    icon: "hourglass",
                    title: "Screen Time Access",
                    description: "Required to monitor your app usage and set limits",
                    status: permissionManager.screenTimePermissionStatus,
                    onRequest: {
                        Task {
                            await permissionManager.requestScreenTimePermission()
                        }
                    }
                )
                
                PermissionCard(
                    icon: "bell",
                    title: "Notifications",
                    description: "Get helpful reminders and achievement celebrations",
                    status: permissionManager.notificationPermissionStatus,
                    onRequest: {
                        Task {
                            await permissionManager.requestNotificationPermission()
                        }
                    }
                )
            }
            .padding(.horizontal, 24)
            
            VStack(spacing: 12) {
                Text("🔒 Your Privacy Matters")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Text("All your data stays on your device. We never collect or share your personal usage information.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            .padding(.top, 20)
            
            Spacer()
        }
        .padding(.vertical, 40)
    }
}

// MARK: - App Selection Screen

struct AppSelectionScreen: View {
    @StateObject private var appSelectionManager = AppSelectionManager()
    
    var body: some View {
        VStack(spacing: 24) {
            VStack(spacing: 16) {
                Image(systemName: "apps.iphone")
                    .font(.system(size: 60))
                    .foregroundColor(.purple)
                
                Text("Choose Your Apps")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("Select the apps you'd like to monitor and set limits for")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            // App selection interface
            if appSelectionManager.hasScreenTimePermission {
                VStack(spacing: 16) {
                    Button(action: {
                        appSelectionManager.showAppPicker = true
                    }) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text("Select Apps to Monitor")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.purple)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    
                    if !appSelectionManager.selectedApps.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Selected Apps (\(appSelectionManager.selectedApps.count))")
                                .font(.headline)
                                .fontWeight(.semibold)
                            
                            LazyVGrid(columns: [
                                GridItem(.flexible()),
                                GridItem(.flexible()),
                                GridItem(.flexible())
                            ], spacing: 12) {
                                ForEach(Array(appSelectionManager.selectedApps), id: \.self) { app in
                                    SelectedAppCard(appName: app)
                                }
                            }
                        }
                        .padding(.top, 8)
                    }
                }
            } else {
                VStack(spacing: 16) {
                    Text("⚠️ Screen Time Permission Required")
                        .font(.headline)
                        .foregroundColor(.orange)
                    
                    Text("Please grant Screen Time permission in the previous step to select apps")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 32)
            }
            
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 40)
        .familyActivityPicker(
            isPresented: $appSelectionManager.showAppPicker,
            selection: $appSelectionManager.activitySelection
        )
        .onChange(of: appSelectionManager.activitySelection) { _ in
            appSelectionManager.updateSelectedApps()
        }
    }
}

// MARK: - Goal Setting Screen

struct GoalSettingScreen: View {
    @StateObject private var goalManager = OnboardingGoalManager()
    
    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 16) {
                Image(systemName: "target")
                    .font(.system(size: 60))
                    .foregroundColor(.purple)
                
                Text("Set Your Goals")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("Choose a daily time limit that feels realistic and achievable")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            VStack(spacing: 24) {
                // Preset goals
                Text("Recommended Daily Limits")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: 16) {
                    ForEach(goalManager.presetGoals, id: \.minutes) { preset in
                        GoalPresetCard(
                            preset: preset,
                            isSelected: goalManager.selectedGoal?.minutes == preset.minutes,
                            onSelect: {
                                goalManager.selectGoal(preset)
                            }
                        )
                    }
                }
                
                // Custom goal option
                VStack(spacing: 12) {
                    Text("Or set a custom limit")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    HStack {
                        Text("Daily limit:")
                        
                        Spacer()
                        
                        HStack {
                            Button("-") {
                                goalManager.decreaseCustomGoal()
                            }
                            .disabled(goalManager.customGoalMinutes <= 30)
                            
                            Text("\(goalManager.customGoalMinutes) min")
                                .frame(width: 80)
                                .font(.headline)
                            
                            Button("+") {
                                goalManager.increaseCustomGoal()
                            }
                            .disabled(goalManager.customGoalMinutes >= 480)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                    }
                    
                    Button("Use Custom Goal") {
                        goalManager.selectCustomGoal()
                    }
                    .foregroundColor(.purple)
                }
                .padding(.top, 16)
            }
            .padding(.horizontal, 24)
            
            Spacer()
        }
        .padding(.vertical, 40)
    }
}

// MARK: - Completion Screen

struct CompletionScreen: View {
    @EnvironmentObject private var onboardingManager: OnboardingManager
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            VStack(spacing: 24) {
                // Success animation
                ZStack {
                    Circle()
                        .fill(Color.green.opacity(0.2))
                        .frame(width: 120, height: 120)
                    
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.green)
                }
                
                VStack(spacing: 16) {
                    Text("You're All Set! 🎉")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Awaytime is now ready to help you build healthier digital habits")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }
            
            VStack(spacing: 20) {
                OnboardingBenefitRow(
                    icon: "chart.line.uptrend.xyaxis",
                    title: "Track Your Progress",
                    description: "See your daily usage and improvement over time"
                )
                
                OnboardingBenefitRow(
                    icon: "bell.badge",
                    title: "Stay Mindful",
                    description: "Get gentle reminders when approaching your limits"
                )
                
                OnboardingBenefitRow(
                    icon: "trophy.fill",
                    title: "Celebrate Success",
                    description = "Earn achievements and build lasting habits"
                )
            }
            .padding(.horizontal, 32)
            
            Button(action: {
                onboardingManager.completeOnboarding()
            }) {
                HStack {
                    Text("Start Your Journey")
                    Image(systemName: "arrow.right")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.purple)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .padding(.horizontal, 32)
            
            Spacer()
        }
        .padding(.vertical, 40)
    }
}

// MARK: - Supporting Views

struct OnboardingBenefitRow: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.purple)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}

struct FeatureCard: View {
    let feature: OnboardingFeature
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: feature.icon)
                .font(.system(size: 50))
                .foregroundColor(feature.color)
            
            VStack(spacing: 12) {
                Text(feature.title)
                    .font(.title2)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text(feature.description)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
            }
        }
        .padding(32)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
        )
        .padding(.horizontal, 24)
    }
}

struct PermissionCard: View {
    let icon: String
    let title: String
    let description: String
    let status: PermissionStatus
    let onRequest: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.purple)
                    .frame(width: 30)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                statusIcon
            }
            
            if status == .notRequested || status == .denied {
                Button(action: onRequest) {
                    Text(status == .denied ? "Open Settings" : "Grant Permission")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color.purple)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }
    
    private var statusIcon: some View {
        Group {
            switch status {
            case .granted:
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            case .denied:
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red)
            case .notRequested:
                Image(systemName: "circle")
                    .foregroundColor(.gray)
            }
        }
        .font(.title3)
    }
}

struct SelectedAppCard: View {
    let appName: String
    
    var body: some View {
        VStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.purple.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay(
                    Text("📱")
                        .font(.title3)
                )
            
            Text(appName)
                .font(.caption)
                .fontWeight(.medium)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemGray6))
        )
    }
}

struct GoalPresetCard: View {
    let preset: GoalPreset
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 12) {
                Text(preset.emoji)
                    .font(.system(size: 30))
                
                VStack(spacing: 4) {
                    Text(preset.title)
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("\(preset.minutes) min/day")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text(preset.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 120)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? Color.purple.opacity(0.2) : Color(.systemGray6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct OnboardingBottomNavigation: View {
    @EnvironmentObject private var onboardingManager: OnboardingManager
    
    var body: some View {
        VStack(spacing: 16) {
            // Progress indicator
            HStack(spacing: 8) {
                ForEach(OnboardingStep.allCases, id: \.self) { step in
                    Circle()
                        .fill(step.rawValue <= onboardingManager.currentStep.rawValue ? Color.purple : Color.gray.opacity(0.3))
                        .frame(width: 8, height: 8)
                }
            }
            
            // Navigation buttons
            HStack {
                if onboardingManager.canGoBack {
                    Button("Back") {
                        onboardingManager.goBack()
                    }
                    .foregroundColor(.purple)
                } else {
                    Spacer()
                }
                
                Spacer()
                
                Button(onboardingManager.nextButtonTitle) {
                    onboardingManager.goNext()
                }
                .disabled(!onboardingManager.canGoNext)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(onboardingManager.canGoNext ? Color.purple : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 32)
    }
}

// MARK: - Sequential Fox Animation

struct SequentialFoxAnimationView: View {
    @State private var currentAnimationIndex = 0
    
    private let animations = [
        "baby_fox_happy",
        "baby_fox_sleepy", 
        "baby_fox_sad",
        "baby_fox_exhausted"
    ]
    
    private let animationDuration: Double = 2.0 // 2 seconds per animation
    
    var body: some View {
        LottieView(animation: .named(animations[currentAnimationIndex]))
            .playing(looping: .loop)
            .onAppear {
                startAnimationCycle()
            }
    }
    
    private func startAnimationCycle() {
        Timer.scheduledTimer(withTimeInterval: animationDuration, repeats: true) { _ in
            withAnimation(.easeInOut(duration: 0.3)) {
                currentAnimationIndex = (currentAnimationIndex + 1) % animations.count
            }
        }
    }
}

#Preview {
    OnboardingFlow()
}