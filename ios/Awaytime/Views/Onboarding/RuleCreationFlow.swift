import SwiftUI

struct RuleCreationFlow: View {
    @StateObject private var viewModel = RuleCreationViewModel()
    @State private var currentStep: RuleCreationStep = .welcome
    @State private var selectedAppGroup: AppGroupEntity?
    @State private var dailyLimit: Int = 120 // minutes
    let onComplete: (Rule) -> Void
    
    enum RuleCreationStep {
        case welcome
        case appSelection
        case timeLimit
        case confirmation
        case celebration
    }
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress bar
                ProgressView(value: progressValue)
                    .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                    .padding(.horizontal)
                    .padding(.top, 10)
                
                // Content
                switch currentStep {
                case .welcome:
                    WelcomeRuleView {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentStep = .appSelection
                        }
                    }
                case .appSelection:
                    AppSelectionRuleView(selectedAppGroup: $selectedAppGroup) {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentStep = .timeLimit
                        }
                    }
                case .timeLimit:
                    TimeLimitRuleView(dailyLimit: $dailyLimit) {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentStep = .confirmation
                        }
                    }
                case .confirmation:
                    ConfirmationRuleView(
                        appGroup: selectedAppGroup,
                        dailyLimit: dailyLimit
                    ) {
                        createRule()
                        withAnimation(.easeInOut(duration: 0.3)) {
                            currentStep = .celebration
                        }
                    }
                case .celebration:
                    CelebrationRuleView {
                        // Complete the flow
                        if let rule = viewModel.createdRule {
                            onComplete(rule)
                        }
                    }
                }
            }
        }
    }
    
    private var progressValue: Double {
        switch currentStep {
        case .welcome: return 0.2
        case .appSelection: return 0.4
        case .timeLimit: return 0.6
        case .confirmation: return 0.8
        case .celebration: return 1.0
        }
    }
    
    private func createRule() {
        guard let appGroup = selectedAppGroup else { return }
        viewModel.createFirstRule(appGroup: appGroup, dailyLimit: dailyLimit)
    }
}

// MARK: - Welcome Step
struct WelcomeRuleView: View {
    let onContinue: () -> Void
    
    var body: some View {
        VStack(spacing: 40) {
            Spacer()
            
            BrainMascotView(state: .encouraging, size: 140)
            
            VStack(spacing: 16) {
                Text("ready to\ntake control?")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("enable your first rule to get started")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 40)
            
            Spacer()
            
            Button(action: onContinue) {
                Text("continue")
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 28)
                            .fill(Color.blue)
                    )
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }
}

// MARK: - App Selection Step
struct AppSelectionRuleView: View {
    @Binding var selectedAppGroup: AppGroupEntity?
    let onContinue: () -> Void
    
    var body: some View {
        VStack(spacing: 40) {
            Spacer()
            
            BrainMascotView(state: .thinking, size: 120)
            
            VStack(spacing: 16) {
                Text("your first rule")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                Text("choose apps to set limits for")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 40)
            
            // Suggested app groups
            VStack(spacing: 16) {
                AppGroupOption(
                    title: "Social Media",
                    subtitle: "Instagram, TikTok, Facebook",
                    icon: "heart.fill",
                    color: .pink,
                    isSelected: selectedAppGroup?.name == "Social Media"
                ) {
                    selectedAppGroup = createOrFetchAppGroup(name: "Social Media")
                }
                
                AppGroupOption(
                    title: "Entertainment",
                    subtitle: "YouTube, Netflix, Twitch",
                    icon: "play.fill",
                    color: .red,
                    isSelected: selectedAppGroup?.name == "Entertainment"
                ) {
                    selectedAppGroup = createOrFetchAppGroup(name: "Entertainment")
                }
                
                AppGroupOption(
                    title: "Games",
                    subtitle: "Mobile games and apps",
                    icon: "gamecontroller.fill",
                    color: .purple,
                    isSelected: selectedAppGroup?.name == "Games"
                ) {
                    selectedAppGroup = createOrFetchAppGroup(name: "Games")
                }
            }
            .padding(.horizontal, 32)
            
            Spacer()
            
            Button(action: onContinue) {
                Text("continue")
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 28)
                            .fill(selectedAppGroup != nil ? Color.blue : Color.gray)
                    )
            }
            .disabled(selectedAppGroup == nil)
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }
    
    private func createOrFetchAppGroup(name: String) -> AppGroupEntity {
        // Create or fetch from Core Data
        let coreDataManager = CoreDataManager.shared
        
        // Check if app group already exists
        let existingGroups = coreDataManager.fetchAppGroups()
        if let existingGroup = existingGroups.first(where: { $0.name == name }) {
            return existingGroup
        }
        
        // Create new app group
        let group = coreDataManager.createAppGroup(
            name: name,
            dailyLimitMinutes: dailyLimit,
            isActive: false
        )
        // group.name = name
        return group
    }
}

struct AppGroupOption: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                    .frame(width: 24, height: 24)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Circle()
                    .stroke(isSelected ? Color.blue : Color.gray, lineWidth: 2)
                    .fill(isSelected ? Color.blue : Color.clear)
                    .frame(width: 24, height: 24)
                    .overlay(
                        Circle()
                            .fill(Color.white)
                            .frame(width: 8, height: 8)
                            .opacity(isSelected ? 1 : 0)
                    )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(UIColor.secondarySystemBackground))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Time Limit Step
struct TimeLimitRuleView: View {
    @Binding var dailyLimit: Int
    let onContinue: () -> Void
    
    var body: some View {
        VStack(spacing: 40) {
            Spacer()
            
            BrainMascotView(state: .thinking, size: 120)
            
            VStack(spacing: 16) {
                Text("daily screen time limit")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                Text("2h daily limit for selected apps")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 40)
            
            // Time limit toggle
            VStack(spacing: 20) {
                HStack {
                    Image(systemName: "circle.fill")
                        .foregroundColor(.green)
                        .font(.title3)
                    
                    Text("daily screen time limit")
                        .font(.body)
                        .fontWeight(.medium)
                    
                    Spacer()
                    
                    Toggle("", isOn: .constant(true))
                        .labelsHidden()
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(UIColor.secondarySystemBackground))
                )
                
                Text("Lowest price ever")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 32)
            
            Spacer()
            
            VStack(spacing: 16) {
                Text("congratulations!")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.green)
                
                Text("you're ready to take control of your\nscreen time")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 40)
            
            Button(action: onContinue) {
                Text("continue")
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 28)
                            .fill(Color.blue)
                    )
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }
}

// MARK: - Confirmation Step
struct ConfirmationRuleView: View {
    let appGroup: AppGroupEntity?
    let dailyLimit: Int
    let onConfirm: () -> Void
    
    var body: some View {
        VStack(spacing: 40) {
            Spacer()
            
            BrainMascotView(state: .happy, size: 120)
            
            VStack(spacing: 16) {
                Text("Ready to create your rule?")
                    .font(.title2)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                VStack(spacing: 8) {
                    Text("App Group: \(appGroup?.name ?? "Selected Apps")")
                        .font(.body)
                    Text("Daily Limit: \(dailyLimit / 60)h \(dailyLimit % 60)m")
                        .font(.body)
                }
                .foregroundColor(.secondary)
            }
            .padding(.horizontal, 40)
            
            Spacer()
            
            Button(action: onConfirm) {
                Text("create rule")
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(
                        RoundedRectangle(cornerRadius: 28)
                            .fill(Color.blue)
                    )
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }
}

// MARK: - Celebration Step
struct CelebrationRuleView: View {
    let onComplete: () -> Void
    @State private var showConfetti = false
    
    var body: some View {
        ZStack {
            VStack(spacing: 40) {
                Spacer()
                
                BrainMascotView(state: .celebrating, size: 160)
                
                VStack(spacing: 16) {
                    Text("congratulations!")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                        .multilineTextAlignment(.center)
                    
                    Text("you're ready to take control of your\nscreen time")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                Button(action: onComplete) {
                    Text("continue")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 28)
                                .fill(Color.blue)
                        )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
            
            // Confetti overlay
            if showConfetti {
                ConfettiView()
                    .allowsHitTesting(false)
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                showConfetti = true
            }
        }
    }
}

// MARK: - View Model
class RuleCreationViewModel: ObservableObject {
    @Published var createdRule: Rule?
    
    func createFirstRule(appGroup: AppGroupEntity, dailyLimit: Int) {
        // In real implementation, this would create and save to Core Data
        let rule = Rule()
        // rule.name = "Daily Screen Time Limit"
        // rule.isFirstRule = true
        // rule.dailyLimitMinutes = Int32(dailyLimit)
        // rule.appGroup = appGroup
        // rule.createdAt = Date()
        
        createdRule = rule
    }
}

struct RuleCreationFlow_Previews: PreviewProvider {
    static var previews: some View {
        RuleCreationFlow { _ in }
    }
}