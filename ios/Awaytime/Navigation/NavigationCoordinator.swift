import SwiftUI

enum NavigationDestination: Hashable {
    case dashboard
    case appSelection
    case limitSetting
    case analytics
    case premium
    case onboarding
    case permissions
}

enum OnboardingStep: CaseIterable {
    case welcome
    case permissions
    case appSelection
    case limits
    case complete
    
    var title: String {
        switch self {
        case .welcome:
            return "Welcome to Awaytime"
        case .permissions:
            return "Grant Permissions"
        case .appSelection:
            return "Select Apps"
        case .limits:
            return "Set Daily Limits"
        case .complete:
            return "You're All Set!"
        }
    }
    
    var subtitle: String {
        switch self {
        case .welcome:
            return "Take control of your digital wellness"
        case .permissions:
            return "Allow Awaytime to monitor your screen time"
        case .appSelection:
            return "Choose which apps to track"
        case .limits:
            return "Set healthy usage goals"
        case .complete:
            return "Start your journey to better digital habits"
        }
    }
    
    var icon: String {
        switch self {
        case .welcome:
            return "brain.head.profile"
        case .permissions:
            return "lock.shield"
        case .appSelection:
            return "apps.iphone"
        case .limits:
            return "clock"
        case .complete:
            return "checkmark.circle.fill"
        }
    }
}

class NavigationCoordinator: ObservableObject {
    @Published var currentDestination: NavigationDestination = .dashboard
    @Published var currentOnboardingStep: OnboardingStep = .welcome
    @Published var isOnboardingActive = false
    
    func navigate(to destination: NavigationDestination) {
        currentDestination = destination
    }
    
    func navigateBack() {
        currentDestination = .dashboard
    }
    
    func navigateToRoot() {
        currentDestination = .dashboard
    }
    
    func startOnboarding() {
        isOnboardingActive = true
        currentOnboardingStep = .welcome
    }
    
    func nextOnboardingStep() {
        let allSteps = OnboardingStep.allCases
        if let currentIndex = allSteps.firstIndex(of: currentOnboardingStep),
           currentIndex < allSteps.count - 1 {
            currentOnboardingStep = allSteps[currentIndex + 1]
        } else {
            completeOnboarding()
        }
    }
    
    func previousOnboardingStep() {
        let allSteps = OnboardingStep.allCases
        if let currentIndex = allSteps.firstIndex(of: currentOnboardingStep),
           currentIndex > 0 {
            currentOnboardingStep = allSteps[currentIndex - 1]
        }
    }
    
    func completeOnboarding() {
        OnboardingManager.completeOnboarding()
        isOnboardingActive = false
        currentDestination = .dashboard
    }
}

struct NavigationCoordinatorView: View {
    @StateObject private var coordinator = NavigationCoordinator()
    
    var body: some View {
        Group {
            if coordinator.isOnboardingActive {
                OnboardingFlowView()
                    .environmentObject(coordinator)
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing),
                        removal: .move(edge: .leading)
                    ))
            } else {
                MainAppView()
                    .environmentObject(coordinator)
                    .transition(.asymmetric(
                        insertion: .move(edge: .leading),
                        removal: .move(edge: .trailing)
                    ))
            }
        }
        .animation(.easeInOut(duration: 0.5), value: coordinator.isOnboardingActive)
        .onAppear {
            checkOnboardingStatus()
        }
    }
    
    private func checkOnboardingStatus() {
        if !OnboardingManager.isOnboardingCompleted {
            coordinator.startOnboarding()
        }
    }
}

struct MainAppView: View {
    @EnvironmentObject var coordinator: NavigationCoordinator
    
    var body: some View {
        NavigationView {
            destinationView(for: coordinator.currentDestination)
        }
    }
    
    @ViewBuilder
    private func destinationView(for destination: NavigationDestination) -> some View {
        switch destination {
        case .dashboard:
            DashboardView()
        case .appSelection:
            AppSelectionView()
        case .limitSetting:
            LimitSettingView()
        case .analytics:
            AnalyticsView()
        case .premium:
            PremiumView()
        case .onboarding:
            OnboardingFlowView()
        case .permissions:
            PermissionRequestView(onPermissionGranted: {
                coordinator.navigateBack()
            })
        }
    }
}

struct OnboardingFlowView: View {
    @EnvironmentObject var coordinator: NavigationCoordinator
    @State private var selectedApps: Set<String> = []
    @State private var dailyLimitHours = 2
    @State private var dailyLimitMinutes = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Progress indicator
            OnboardingProgressView(currentStep: coordinator.currentOnboardingStep)
                .padding(.top, 20)
            
            // Main content
            TabView(selection: $coordinator.currentOnboardingStep) {
                ForEach(OnboardingStep.allCases, id: \.self) { step in
                    onboardingStepView(for: step)
                        .tag(step)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            .animation(.easeInOut, value: coordinator.currentOnboardingStep)
            
            // Navigation buttons
            OnboardingNavigationView()
                .environmentObject(coordinator)
        }
        .background(
            LinearGradient(
                colors: [Color.purple.opacity(0.1), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
    
    @ViewBuilder
    private func onboardingStepView(for step: OnboardingStep) -> some View {
        switch step {
        case .welcome:
            OnboardingWelcomeView()
        case .permissions:
            OnboardingPermissionsView()
        case .appSelection:
            OnboardingAppSelectionView(selectedApps: $selectedApps)
        case .limits:
            OnboardingLimitsView(hours: $dailyLimitHours, minutes: $dailyLimitMinutes)
        case .complete:
            OnboardingCompleteView()
        }
    }
}

struct OnboardingProgressView: View {
    let currentStep: OnboardingStep
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach(OnboardingStep.allCases, id: \.self) { step in
                Circle()
                    .fill(stepColor(for: step))
                    .frame(width: 8, height: 8)
                    .scaleEffect(step == currentStep ? 1.2 : 1.0)
                    .animation(.easeInOut, value: currentStep)
            }
        }
        .padding(.horizontal, 32)
    }
    
    private func stepColor(for step: OnboardingStep) -> Color {
        let allSteps = OnboardingStep.allCases
        guard let currentIndex = allSteps.firstIndex(of: currentStep),
              let stepIndex = allSteps.firstIndex(of: step) else {
            return Color.gray.opacity(0.3)
        }
        
        return stepIndex <= currentIndex ? Color.purple : Color.gray.opacity(0.3)
    }
}

struct OnboardingNavigationView: View {
    @EnvironmentObject var coordinator: NavigationCoordinator
    
    var body: some View {
        HStack {
            if coordinator.currentOnboardingStep != .welcome {
                Button("Back") {
                    withAnimation {
                        coordinator.previousOnboardingStep()
                    }
                }
                .foregroundColor(.purple)
            } else {
                Spacer()
            }
            
            Spacer()
            
            Button(buttonTitle) {
                withAnimation {
                    coordinator.nextOnboardingStep()
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(Color.purple)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .padding(.horizontal, 32)
        .padding(.bottom, 32)
    }
    
    private var buttonTitle: String {
        coordinator.currentOnboardingStep == .complete ? "Get Started" : "Next"
    }
}
    


#Preview {
    NavigationCoordinatorView()
}

// MARK: - Individual Onboarding Step Views

struct OnboardingWelcomeView: View {
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            VStack(spacing: 24) {
                Image(systemName: "brain.head.profile")
                    .font(.system(size: 80))
                    .foregroundColor(.purple)
                    .scaleEffect(1.0)
                    .animation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true), value: UUID())
                
                VStack(spacing: 16) {
                    Text("Welcome to Awaytime")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Take control of your digital wellness")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
                    Text("Track your app usage, set healthy limits, and build better digital habits")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }
            
            Spacer()
        }
    }
}

struct OnboardingPermissionsView: View {
    @StateObject private var permissionService = PermissionService()
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            VStack(spacing: 24) {
                Image(systemName: "lock.shield")
                    .font(.system(size: 80))
                    .foregroundColor(.purple)
                
                VStack(spacing: 16) {
                    Text("Grant Permissions")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Allow Awaytime to monitor your screen time")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            
            VStack(spacing: 16) {
                PermissionCard(
                    icon: "hourglass",
                    title: "Screen Time Access",
                    description: "Monitor app usage and set limits",
                    isGranted: permissionService.authorizationStatus == .approved
                )
                
                if !permissionService.isIOSVersionSupported {
                    VStack(spacing: 8) {
                        Text("⚠️ iOS 16+ Required")
                            .font(.headline)
                            .foregroundColor(.orange)
                        
                        Text("Full Screen Time features require iOS 16.0 or later. Some features may be limited on your current iOS version.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(8)
                    .padding(.horizontal, 32)
                }
                
                Button(action: {
                    Task {
                        await permissionService.requestPermission()
                    }
                }) {
                    Text(permissionService.authorizationStatus == .approved ? "Permission Granted" : "Grant Permission")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(permissionService.authorizationStatus == .approved ? Color.green : Color.purple)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .disabled(permissionService.authorizationStatus == .approved)
                .padding(.horizontal, 32)
            }
            
            Spacer()
        }
        .onAppear {
            permissionService.updateAuthorizationStatus()
        }
    }
}

struct PermissionCard: View {
    let icon: String
    let title: String
    let description: String
    let isGranted: Bool
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.purple)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Image(systemName: isGranted ? "checkmark.circle.fill" : "circle")
                .font(.title2)
                .foregroundColor(isGranted ? .green : .gray)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal, 32)
    }
}

struct OnboardingAppSelectionView: View {
    @Binding var selectedApps: Set<String>
    
    private let popularApps = [
        AppInfo(name: "Instagram", bundleId: "com.instagram.app", icon: "camera.fill"),
        AppInfo(name: "TikTok", bundleId: "com.tiktok.app", icon: "music.note"),
        AppInfo(name: "Twitter", bundleId: "com.twitter.app", icon: "at"),
        AppInfo(name: "YouTube", bundleId: "com.youtube.app", icon: "play.rectangle.fill"),
        AppInfo(name: "Facebook", bundleId: "com.facebook.app", icon: "person.2.fill"),
        AppInfo(name: "Snapchat", bundleId: "com.snapchat.app", icon: "camera.circle.fill")
    ]
    
    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 24) {
                Image(systemName: "apps.iphone")
                    .font(.system(size: 80))
                    .foregroundColor(.purple)
                
                VStack(spacing: 16) {
                    Text("Select Apps to Track")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Choose which apps you'd like to monitor")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
                    Text("You can always change this later in settings")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(popularApps, id: \.bundleId) { app in
                        OnboardingAppRow(
                            app: app,
                            isSelected: selectedApps.contains(app.bundleId)
                        ) {
                            if selectedApps.contains(app.bundleId) {
                                selectedApps.remove(app.bundleId)
                            } else {
                                selectedApps.insert(app.bundleId)
                            }
                        }
                    }
                }
                .padding(.horizontal, 32)
            }
            
            Text("\(selectedApps.count) apps selected")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .onAppear {
            // Pre-select some popular apps
            selectedApps = Set(popularApps.prefix(3).map { $0.bundleId })
        }
    }
}

struct OnboardingAppRow: View {
    let app: AppInfo
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: app.icon)
                .font(.title2)
                .foregroundColor(.purple)
                .frame(width: 40, height: 40)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.purple.opacity(0.1))
                )
            
            Text(app.name)
                .font(.headline)
                .foregroundColor(.primary)
            
            Spacer()
            
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(.title2)
                .foregroundColor(isSelected ? .purple : .gray)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(isSelected ? Color.purple.opacity(0.1) : Color(.systemGray6))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            onTap()
        }
        .scaleEffect(isSelected ? 1.02 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
    }
}

struct OnboardingLimitsView: View {
    @Binding var hours: Int
    @Binding var minutes: Int
    
    private let hourOptions = Array(0...12)
    private let minuteOptions = [0, 15, 30, 45]
    
    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 24) {
                Image(systemName: "clock")
                    .font(.system(size: 80))
                    .foregroundColor(.purple)
                
                VStack(spacing: 16) {
                    Text("Set Daily Limit")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Choose a realistic daily time limit")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
                    Text("Start with a goal that feels achievable")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            
            VStack(spacing: 16) {
                Text("Daily Time Limit")
                    .font(.headline)
                
                HStack(spacing: 20) {
                    VStack {
                        Text("Hours")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Picker("Hours", selection: $hours) {
                            ForEach(hourOptions, id: \.self) { hour in
                                Text("\(hour)").tag(hour)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                        .frame(width: 80, height: 120)
                    }
                    
                    VStack {
                        Text("Minutes")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Picker("Minutes", selection: $minutes) {
                            ForEach(minuteOptions, id: \.self) { minute in
                                Text("\(minute)").tag(minute)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                        .frame(width: 80, height: 120)
                    }
                }
                
                Text("Total: \(hours)h \(minutes)m per day")
                    .font(.headline)
                    .foregroundColor(.purple)
                    .padding()
                    .background(Color.purple.opacity(0.1))
                    .cornerRadius(8)
            }
            
            Spacer()
        }
    }
}

struct OnboardingCompleteView: View {
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            VStack(spacing: 24) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.green)
                    .scaleEffect(1.0)
                    .animation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true), value: UUID())
                
                VStack(spacing: 16) {
                    Text("You're All Set!")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Start your journey to better digital habits")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            
            VStack(spacing: 16) {
                FeatureHighlight(
                    icon: "chart.bar.fill",
                    title: "Track Your Progress",
                    description: "See detailed analytics of your app usage"
                )
                
                FeatureHighlight(
                    icon: "bell.fill",
                    title: "Smart Notifications",
                    description: "Get gentle reminders when approaching limits"
                )
                
                FeatureHighlight(
                    icon: "target",
                    title: "Achieve Your Goals",
                    description: "Build streaks and celebrate your success"
                )
            }
            .padding(.horizontal, 32)
            
            Spacer()
        }
    }
}

struct FeatureHighlight: View {
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
                
                Text(description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}