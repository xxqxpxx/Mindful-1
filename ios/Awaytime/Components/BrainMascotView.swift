import SwiftUI

struct BrainMascotView: View {
    enum MascotState {
        case happy
        case concerned
        case celebrating
        case thinking
        case encouraging
    }
    
    let state: MascotState
    let size: CGFloat
    @State private var isAnimating = false
    @State private var bounceAnimation = false
    @State private var glowAnimation = false
    
    init(state: MascotState = .happy, size: CGFloat = 120) {
        self.state = state
        self.size = size
    }
    
    var body: some View {
        ZStack {
            // Glow effect for celebration
            if state == .celebrating {
                Circle()
                    .fill(
                        RadialGradient(
                            gradient: Gradient(colors: [
                                Color.orange.opacity(0.3),
                                Color.clear
                            ]),
                            center: .center,
                            startRadius: size * 0.3,
                            endRadius: size * 0.8
                        )
                    )
                    .frame(width: size * 1.5, height: size * 1.5)
                    .scaleEffect(glowAnimation ? 1.2 : 1.0)
                    .opacity(glowAnimation ? 0.5 : 0.8)
                    .animation(
                        Animation.easeInOut(duration: 1.5).repeatForever(autoreverses: true),
                        value: glowAnimation
                    )
            }
            
            // Main brain body
            brainBody
                .scaleEffect(bounceAnimation ? 1.05 : 1.0)
                .rotationEffect(.degrees(isAnimating ? (state == .thinking ? 5 : 2) : 0))
                .animation(
                    Animation.easeInOut(duration: animationDuration)
                        .repeatForever(autoreverses: true),
                    value: isAnimating
                )
                .animation(
                    Animation.spring(response: 0.6, dampingFraction: 0.8),
                    value: bounceAnimation
                )
        }
        .onAppear {
            startAnimations()
        }
        .onChange(of: state) { _ in
            startAnimations()
        }
    }
    
    private var brainBody: some View {
        ZStack {
            // Brain base
            RoundedRectangle(cornerRadius: size * 0.3)
                .fill(brainColor)
                .frame(width: size, height: size * 0.8)
            
            // Brain folds/wrinkles
            brainFolds
            
            // Eyes
            HStack(spacing: size * 0.2) {
                eye(isWinking: state == .celebrating)
                eye(isWinking: false)
            }
            .offset(y: -size * 0.1)
            
            // Mouth
            mouth
                .offset(y: size * 0.15)
            
            // Little legs
            HStack(spacing: size * 0.3) {
                leg
                leg
            }
            .offset(y: size * 0.35)
        }
    }
    
    private var brainFolds: some View {
        ZStack {
            // Top fold
            RoundedRectangle(cornerRadius: 2)
                .fill(Color.black.opacity(0.1))
                .frame(width: size * 0.6, height: 3)
                .offset(x: -size * 0.1, y: -size * 0.25)
            
            // Side folds
            RoundedRectangle(cornerRadius: 2)
                .fill(Color.black.opacity(0.1))
                .frame(width: size * 0.4, height: 2)
                .offset(x: size * 0.15, y: -size * 0.1)
            
            RoundedRectangle(cornerRadius: 2)
                .fill(Color.black.opacity(0.1))
                .frame(width: size * 0.3, height: 2)
                .offset(x: -size * 0.2, y: size * 0.05)
        }
    }
    
    private func eye(isWinking: Bool) -> some View {
        ZStack {
            // Eye white
            Circle()
                .fill(Color.white)
                .frame(width: size * 0.12, height: size * 0.12)
            
            // Pupil
            if !isWinking {
                Circle()
                    .fill(Color.black)
                    .frame(width: size * 0.06, height: size * 0.06)
                    .offset(x: eyeOffset.x, y: eyeOffset.y)
            } else {
                // Winking line
                RoundedRectangle(cornerRadius: 1)
                    .fill(Color.black)
                    .frame(width: size * 0.1, height: 2)
            }
        }
    }
    
    private var mouth: some View {
        Group {
            switch state {
            case .happy, .encouraging:
                // Happy smile
                Arc(startAngle: .degrees(0), endAngle: .degrees(180), clockwise: false)
                    .stroke(Color.black, lineWidth: 2)
                    .frame(width: size * 0.15, height: size * 0.08)
            case .concerned:
                // Concerned frown
                Arc(startAngle: .degrees(180), endAngle: .degrees(360), clockwise: false)
                    .stroke(Color.black, lineWidth: 2)
                    .frame(width: size * 0.15, height: size * 0.08)
            case .celebrating:
                // Big smile
                Arc(startAngle: .degrees(0), endAngle: .degrees(180), clockwise: false)
                    .stroke(Color.black, lineWidth: 3)
                    .frame(width: size * 0.2, height: size * 0.1)
            case .thinking:
                // Neutral mouth
                RoundedRectangle(cornerRadius: 1)
                    .fill(Color.black)
                    .frame(width: size * 0.1, height: 2)
            }
        }
    }
    
    private var leg: some View {
        Capsule()
            .fill(brainColor.opacity(0.8))
            .frame(width: size * 0.08, height: size * 0.12)
    }
    
    private var brainColor: Color {
        switch state {
        case .happy, .encouraging:
            return Color(red: 0.95, green: 0.7, blue: 0.5) // Warm orange
        case .concerned:
            return Color(red: 0.9, green: 0.6, blue: 0.4) // Slightly muted
        case .celebrating:
            return Color(red: 1.0, green: 0.75, blue: 0.55) // Bright orange
        case .thinking:
            return Color(red: 0.9, green: 0.65, blue: 0.45) // Thoughtful tone
        }
    }
    
    private var eyeOffset: CGPoint {
        switch state {
        case .concerned:
            return CGPoint(x: 0, y: -size * 0.01) // Looking up worried
        case .thinking:
            return CGPoint(x: size * 0.01, y: 0) // Looking sideways
        default:
            return CGPoint(x: 0, y: 0) // Looking forward
        }
    }
    
    private var animationDuration: Double {
        switch state {
        case .celebrating:
            return 0.8
        case .thinking:
            return 2.0
        case .concerned:
            return 1.5
        default:
            return 2.5
        }
    }
    
    private func startAnimations() {
        isAnimating = true
        
        if state == .celebrating {
            glowAnimation = true
            // Bounce effect for celebration
            withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) {
                bounceAnimation = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                bounceAnimation = false
            }
        }
    }
}

// Helper struct for drawing arcs
struct Arc: Shape {
    let startAngle: Angle
    let endAngle: Angle
    let clockwise: Bool
    
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.addArc(
            center: CGPoint(x: rect.midX, y: rect.midY),
            radius: rect.width / 2,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: clockwise
        )
        return path
    }
}

struct BrainMascotView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 30) {
            BrainMascotView(state: .happy, size: 120)
            BrainMascotView(state: .concerned, size: 100)
            BrainMascotView(state: .celebrating, size: 140)
            BrainMascotView(state: .thinking, size: 110)
            BrainMascotView(state: .encouraging, size: 130)
        }
        .padding()
        .background(Color.gray.opacity(0.1))
    }
}