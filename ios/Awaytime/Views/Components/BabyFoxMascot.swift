import SwiftUI
import DotLottie

enum FoxMood: String, CaseIterable {
    case happy = "happy"
    case sleepy = "sleepy"
    case sad = "sad"
    case exhausted = "exhausted"
    
    static func fromUsagePercent(_ percent: Float) -> FoxMood {
        switch percent {
        case 0...40:
            return .happy
        case 41...70:
            return .sleepy
        case 71...90:
            return .sad
        default:
            return .exhausted
        }
    }
}

struct BabyFoxMascot: View {
    let usagePercent: Float
    let size: CGFloat
    
    @State private var currentMood: FoxMood = .happy
    
    init(usagePercent: Float, size: CGFloat = 120) {
        self.usagePercent = usagePercent
        self.size = size
    }
    
    var body: some View {
        ZStack {
            DotLottieAnimation(
                fileName: currentAnimationName,
                config: .init(autoplay: true, loop: true)
            )
            .view()
            .frame(width: size, height: size)
        }
        .onAppear {
            updateMood()
        }
        .onChange(of: usagePercent) { _ in
            updateMood()
        }
    }
    
    private var currentAnimationName: String {
        return "baby_fox_\(currentMood.rawValue)"
    }
    
    private func updateMood() {
        let newMood = FoxMood.fromUsagePercent(usagePercent)
        
        if newMood != currentMood {
            withAnimation(.easeInOut(duration: 0.3)) {
                currentMood = newMood
            }
        }
    }
}

struct BabyFoxMascot_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            BabyFoxMascot(usagePercent: 25, size: 120)
            BabyFoxMascot(usagePercent: 55, size: 120)
            BabyFoxMascot(usagePercent: 80, size: 120)
            BabyFoxMascot(usagePercent: 95, size: 120)
        }
        .padding()
    }
}