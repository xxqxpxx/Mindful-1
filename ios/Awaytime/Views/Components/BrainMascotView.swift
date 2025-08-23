import SwiftUI

struct BrainMascotView: View {
    let state: MascotState
    let size: CGFloat
    
    enum MascotState {
        case happy
        case thinking
        case concerned
    }
    
    var body: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [Color.purple.opacity(0.2), Color.purple.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: size, height: size)
            
            Text(mascotEmoji)
                .font(.system(size: size * 0.6))
        }
    }
    
    private var mascotEmoji: String {
        switch state {
        case .happy:
            return "🧠"
        case .thinking:
            return "🤔"
        case .concerned:
            return "😟"
        }
    }
}