import SwiftUI

struct ConfettiView: View {
    @State private var animate = false
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                ForEach(0..<50, id: \.self) { i in
                    Circle()
                        .fill(colors.randomElement() ?? .blue)
                        .frame(width: CGFloat.random(in: 2...6))
                        .position(
                            x: CGFloat.random(in: 0...geometry.size.width),
                            y: animate ? geometry.size.height + 50 : -50
                        )
                        .animation(
                            Animation.linear(duration: Double.random(in: 2...4))
                                .repeatForever(autoreverses: false)
                                .delay(Double.random(in: 0...2)),
                            value: animate
                        )
                }
            }
        }
        .onAppear {
            animate = true
        }
    }
    
    private let colors: [Color] = [
        .red, .blue, .green, .yellow, .orange, .pink, .purple
    ]
}

#Preview {
    ConfettiView()
}
