import SwiftUI

struct CelebrationView: View {
    @State private var showConfetti = false
    @State private var animateTitle = false
    @State private var animateSubtitle = false
    @State private var animateMascot = false
    
    let title: String
    let subtitle: String
    let onComplete: () -> Void
    
    init(
        title: String = "congratulations!",
        subtitle: String = "you're ready to take control of your screen time",
        onComplete: @escaping () -> Void = {}
    ) {
        self.title = title
        self.subtitle = subtitle
        self.onComplete = onComplete
    }
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 40) {
                Spacer()
                
                // Animated brain mascot
                BrainMascotView(state: .celebrating, size: 160)
                    .scaleEffect(animateMascot ? 1.0 : 0.8)
                    .opacity(animateMascot ? 1.0 : 0.0)
                    .animation(
                        .spring(response: 0.8, dampingFraction: 0.6).delay(0.3),
                        value: animateMascot
                    )
                
                // Celebration text
                VStack(spacing: 16) {
                    Text(title)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                        .multilineTextAlignment(.center)
                        .scaleEffect(animateTitle ? 1.0 : 0.8)
                        .opacity(animateTitle ? 1.0 : 0.0)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(0.6),
                            value: animateTitle
                        )
                    
                    Text(subtitle)
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .opacity(animateSubtitle ? 1.0 : 0.0)
                        .animation(
                            .easeInOut(duration: 0.5).delay(0.9),
                            value: animateSubtitle
                        )
                }
                .padding(.horizontal, 40)
                
                Spacer()
                
                // Continue button
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
                .opacity(animateSubtitle ? 1.0 : 0.0)
                .animation(
                    .easeInOut(duration: 0.3).delay(1.2),
                    value: animateSubtitle
                )
            }
            
            // Confetti overlay
            if showConfetti {
                ConfettiView()
                    .allowsHitTesting(false)
            }
        }
        .onAppear {
            startAnimations()
        }
    }
    
    private func startAnimations() {
        // Stagger the animations for a nice effect
        animateMascot = true
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            showConfetti = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            animateTitle = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
            animateSubtitle = true
        }
    }
}

struct ConfettiView: View {
    @State private var particles: [ConfettiParticle] = []
    @State private var animationTimer: Timer?
    
    let colors: [Color] = [.red, .blue, .green, .yellow, .orange, .purple, .pink]
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                ForEach(particles, id: \.id) { particle in
                    ConfettiParticleView(particle: particle)
                }
            }
            .onAppear {
                startConfetti(in: geometry.size)
            }
            .onDisappear {
                stopConfetti()
            }
        }
    }
    
    private func startConfetti(in size: CGSize) {
        // Create initial burst of particles
        for _ in 0..<50 {
            createParticle(in: size)
        }
        
        // Continue creating particles
        animationTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            if particles.count < 100 {
                for _ in 0..<5 {
                    createParticle(in: size)
                }
            }
            
            // Remove old particles
            particles.removeAll { particle in
                particle.position.y > size.height + 100
            }
        }
    }
    
    private func createParticle(in size: CGSize) {
        let particle = ConfettiParticle(
            id: UUID(),
            position: CGPoint(
                x: CGFloat.random(in: 0...size.width),
                y: -50
            ),
            velocity: CGPoint(
                x: CGFloat.random(in: -2...2),
                y: CGFloat.random(in: 2...8)
            ),
            color: colors.randomElement() ?? .blue,
            rotation: CGFloat.random(in: 0...360),
            rotationSpeed: CGFloat.random(in: -10...10),
            scale: CGFloat.random(in: 0.5...1.5)
        )
        particles.append(particle)
    }
    
    private func stopConfetti() {
        animationTimer?.invalidate()
        animationTimer = nil
    }
}

struct ConfettiParticle {
    let id: UUID
    var position: CGPoint
    var velocity: CGPoint
    let color: Color
    var rotation: CGFloat
    let rotationSpeed: CGFloat
    let scale: CGFloat
}

struct ConfettiParticleView: View {
    @State private var particle: ConfettiParticle
    @State private var animating = false
    
    init(particle: ConfettiParticle) {
        self._particle = State(initialValue: particle)
    }
    
    var body: some View {
        Rectangle()
            .fill(particle.color)
            .frame(width: 8 * particle.scale, height: 8 * particle.scale)
            .rotationEffect(.degrees(particle.rotation))
            .position(particle.position)
            .onAppear {
                animateParticle()
            }
    }
    
    private func animateParticle() {
        withAnimation(.linear(duration: 3.0)) {
            particle.position.y += UIScreen.main.bounds.height + 200
            particle.position.x += particle.velocity.x * 100
            particle.rotation += particle.rotationSpeed * 360
        }
    }
}

// MARK: - Specialized Celebration Views

struct RuleCreatedCelebrationView: View {
    let onComplete: () -> Void
    
    var body: some View {
        CelebrationView(
            title: "congratulations!",
            subtitle: "you're ready to take control of your\nscreen time",
            onComplete: onComplete
        )
    }
}

struct GoalAchievedCelebrationView: View {
    let goalType: String
    let onComplete: () -> Void
    
    var body: some View {
        CelebrationView(
            title: "goal achieved!",
            subtitle: "you stayed under your \(goalType) limit today",
            onComplete: onComplete
        )
    }
}

struct StreakCelebrationView: View {
    let streakCount: Int
    let onComplete: () -> Void
    
    var body: some View {
        CelebrationView(
            title: "\(streakCount) day streak!",
            subtitle: "amazing consistency! keep it up",
            onComplete: onComplete
        )
    }
}

struct MilestoneCelebrationView: View {
    let milestone: String
    let onComplete: () -> Void
    
    var body: some View {
        CelebrationView(
            title: milestone,
            subtitle: "you're making real progress!",
            onComplete: onComplete
        )
    }
}

// MARK: - Preview
struct CelebrationView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            CelebrationView(
                title: "congratulations!",
                subtitle: "you're ready to take control of your screen time"
            ) {}
            
            RuleCreatedCelebrationView() {}
            
            GoalAchievedCelebrationView(goalType: "daily") {}
            
            StreakCelebrationView(streakCount: 7) {}
            
            MilestoneCelebrationView(milestone: "1 week strong!") {}
        }
    }
}