import SwiftUI

struct ProgressCircle: View {
    let progress: Double
    let size: CGFloat
    let lineWidth: CGFloat
    let primaryColor: Color
    let backgroundColor: Color
    
    init(
        progress: Double,
        size: CGFloat = 200,
        lineWidth: CGFloat = 12,
        primaryColor: Color = AwayTimeColors.primary,
        backgroundColor: Color = AwayTimeColors.secondary.opacity(0.3)
    ) {
        self.progress = progress
        self.size = size
        self.lineWidth = lineWidth
        self.primaryColor = primaryColor
        self.backgroundColor = backgroundColor
    }
    
    var body: some View {
        ZStack {
            // Background circle
            Circle()
                .stroke(backgroundColor, lineWidth: lineWidth)
                .frame(width: size, height: size)
            
            // Progress circle
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    primaryColor,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .frame(width: size, height: size)
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 1.0), value: progress)
        }
    }
}

struct ProgressCircleWithContent<Content: View>: View {
    let progress: Double
    let size: CGFloat
    let lineWidth: CGFloat
    let primaryColor: Color
    let backgroundColor: Color
    let content: () -> Content
    
    init(
        progress: Double,
        size: CGFloat = 200,
        lineWidth: CGFloat = 12,
        primaryColor: Color = AwayTimeColors.primary,
        backgroundColor: Color = AwayTimeColors.secondary.opacity(0.3),
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.progress = progress
        self.size = size
        self.lineWidth = lineWidth
        self.primaryColor = primaryColor
        self.backgroundColor = backgroundColor
        self.content = content
    }
    
    var body: some View {
        ZStack {
            ProgressCircle(
                progress: progress,
                size: size,
                lineWidth: lineWidth,
                primaryColor: primaryColor,
                backgroundColor: backgroundColor
            )
            
            content()
        }
    }
}

#Preview {
    VStack(spacing: 40) {
        ProgressCircle(progress: 0.65)
        
        ProgressCircleWithContent(progress: 0.75) {
            VStack(spacing: 4) {
                Text("75%")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundColor(AwayTimeColors.primary)
                
                Text("used today")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
    .padding()
}