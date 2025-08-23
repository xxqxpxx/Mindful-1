import SwiftUI

struct OnboardingFlow: View {
    let onCompleted: (() -> Void)?
    @State private var currentStep = 0
    @State private var isAnimating = false
    
    init(onCompleted: (() -> Void)? = nil) {
        self.onCompleted = onCompleted
    }
    
    private let steps = [
        OnboardingStepData(
            title: "Welcome to Awaytime",
            subtitle: "Take control of your digital wellness",
            icon: "brain.head.profile",
            description: "Track your app usage and build healthier digital habits",
            color: .purple
        ),
        OnboardingStepData(
            title: "Monitor Your Apps",
            subtitle: "See exactly how you spend your time",
            icon: "apps.iphone",
            description: "Get detailed insights into your daily app usage patterns",
            color: .blue
        ),
        OnboardingStepData(
            title: "Set Healthy Limits",
            subtitle: "Create realistic goals",
            icon: "clock",
            description: "Set daily time limits that work for your lifestyle",
            color: .green
        ),
        OnboardingStepData(
            title: "Stay Motivated",
            subtitle: "Build lasting habits",
            icon: "chart.line.uptrend.xyaxis",
            description: "Track your progress and celebrate your achievements",
            color: .orange
        )
    ]
    
    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                // Progress indicator
                HStack(spacing: 8) {
                    ForEach(0..<steps.count, id: \.self) { index in
                        Circle()
                            .fill(index <= currentStep ? steps[currentStep].color : Color.gray.opacity(0.3))
                            .frame(width: 8, height: 8)
                            .scaleEffect(index == currentStep ? 1.2 : 1.0)
                            .animation(.easeInOut, value: currentStep)
                    }
                }
                .padding(.top, 20)
                
                TabView(selection: $currentStep) {
                    ForEach(0..<steps.count, id: \.self) { index in
                        OnboardingStepView(step: steps[index], isActive: index == currentStep)
                            .tag(index)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.5), value: currentStep)
                
                // Navigation buttons
                HStack {
                    if currentStep > 0 {
                        Button("Back") {
                            withAnimation(.easeInOut(duration: 0.3)) {
                                currentStep -= 1
                            }
                        }
                        .foregroundColor(steps[currentStep].color)
                        .font(.headline)
                    } else {
                        Spacer()
                    }
                    
                    Spacer()
                    
                    Button(currentStep == steps.count - 1 ? "Get Started" : "Next") {
                        if currentStep == steps.count - 1 {
                            completeOnboarding()
                        } else {
                            withAnimation(.easeInOut(duration: 0.3)) {
                                currentStep += 1
                            }
                        }
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 16)
                    .background(
                        LinearGradient(
                            colors: [steps[currentStep].color, steps[currentStep].color.opacity(0.8)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .foregroundColor(.white)
                    .font(.headline)
                    .cornerRadius(12)
                    .shadow(color: steps[currentStep].color.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 32)
            }
            .background(
                LinearGradient(
                    colors: [steps[currentStep].color.opacity(0.1), Color.clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .animation(.easeInOut(duration: 0.5), value: currentStep)
            )
        }
        .onAppear {
            isAnimating = true
        }
    }
    
    private func completeOnboarding() {
        OnboardingManager.completeOnboarding()
        onCompleted?()
    }
}

struct OnboardingStepData {
    let title: String
    let subtitle: String
    let icon: String
    let description: String
    let color: Color
}

struct OnboardingStepView: View {
    let step: OnboardingStepData
    let isActive: Bool
    @State private var animateIcon = false
    @State private var animateText = false
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            VStack(spacing: 24) {
                ZStack {
                    Circle()
                        .fill(step.color.opacity(0.1))
                        .frame(width: 120, height: 120)
                        .scaleEffect(animateIcon ? 1.1 : 1.0)
                        .animation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true), value: animateIcon)
                    
                    Image(systemName: step.icon)
                        .font(.system(size: 50, weight: .medium))
                        .foregroundColor(step.color)
                        .scaleEffect(animateIcon ? 1.05 : 1.0)
                        .animation(.easeInOut(duration: 2.0).repeatForever(autoreverses: true), value: animateIcon)
                }
                
                VStack(spacing: 16) {
                    Text(step.title)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.primary)
                        .opacity(animateText ? 1.0 : 0.0)
                        .offset(y: animateText ? 0 : 20)
                        .animation(.easeOut(duration: 0.6).delay(0.2), value: animateText)
                    
                    Text(step.subtitle)
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .opacity(animateText ? 1.0 : 0.0)
                        .offset(y: animateText ? 0 : 20)
                        .animation(.easeOut(duration: 0.6).delay(0.4), value: animateText)
                    
                    Text(step.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                        .opacity(animateText ? 1.0 : 0.0)
                        .offset(y: animateText ? 0 : 20)
                        .animation(.easeOut(duration: 0.6).delay(0.6), value: animateText)
                }
            }
            
            Spacer()
        }
        .onChange(of: isActive) { active in
            if active {
                animateIcon = true
                animateText = true
            } else {
                animateIcon = false
                animateText = false
            }
        }
        .onAppear {
            if isActive {
                animateIcon = true
                animateText = true
            }
        }
    }
}

#Preview {
    OnboardingFlow()
}