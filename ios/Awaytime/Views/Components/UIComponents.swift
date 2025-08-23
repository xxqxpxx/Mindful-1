import SwiftUI

// MARK: - Enhanced Progress Circle

struct EnhancedProgressCircle: View {
    let progress: Double
    let size: CGFloat
    let lineWidth: CGFloat
    
    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.gray.opacity(0.2), lineWidth: lineWidth)
            
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    LinearGradient(
                        colors: [Color.purple, Color.blue],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 1), value: progress)
        }
        .frame(width: size, height: size)
    }
}

// MARK: - Loading Progress Circle

struct LoadingProgressCircle: View {
    let size: CGFloat
    let lineWidth: CGFloat
    @State private var rotation = 0.0
    
    var body: some View {
        Circle()
            .trim(from: 0, to: 0.3)
            .stroke(Color.purple, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .frame(width: size, height: size)
            .rotationEffect(.degrees(rotation))
            .onAppear {
                withAnimation(.linear(duration: 1).repeatForever(autoreverses: false)) {
                    rotation = 360
                }
            }
    }
}

// MARK: - Smooth Number Transition

struct SmoothNumberTransition: View {
    let value: Int
    let formatter: (Int) -> String
    let font: Font
    let color: Color
    
    var body: some View {
        Text(formatter(value))
            .font(font)
            .foregroundColor(color)
            .animation(.easeInOut(duration: 0.3), value: value)
    }
}

// MARK: - Skeleton View

struct SkeletonView: View {
    let width: SkeletonWidth
    let height: CGFloat
    let cornerRadius: CGFloat
    @State private var opacity = 0.3
    
    enum SkeletonWidth {
        case fixed(CGFloat)
        case infinity
    }
    
    init(width: SkeletonWidth, height: CGFloat, cornerRadius: CGFloat) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }
    
    init(width: CGFloat, height: CGFloat, cornerRadius: CGFloat) {
        self.width = .fixed(width)
        self.height = height
        self.cornerRadius = cornerRadius
    }
    
@ViewBuilder
    var body: some View {
        switch width {
        case .fixed(let value):
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.gray.opacity(opacity))
                .frame(width: value, height: height)
                .onAppear {
                    withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) {
                        opacity = 0.7
                    }
                }
        case .infinity:
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.gray.opacity(opacity))
                .frame(height: height)
                .frame(maxWidth: .infinity)
                .onAppear {
                    withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) {
                        opacity = 0.7
                    }
                }
        }
    }
}

// MARK: - Streak Visualization

struct StreakVisualization: View {
    let currentStreak: Int
    let longestStreak: Int
    
    var body: some View {
        HStack(spacing: 20) {
            VStack(spacing: 8) {
                Text("🔥")
                    .font(.title)
                
                Text("\(currentStreak)")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
                
                Text("Day Streak")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Divider()
                .frame(height: 40)
            
            VStack(spacing: 8) {
                Text("🏆")
                    .font(.title)
                
                Text("\(longestStreak)")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.purple)
                
                Text("Best Streak")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Weekly Progress Chart

struct WeeklyProgressChart: View {
    let weeklyData: [WeeklyProgressData]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("This Week")
                .font(.headline)
                .fontWeight(.semibold)
            
            HStack(alignment: .bottom, spacing: 8) {
                ForEach(weeklyData, id: \.day) { data in
                    VStack(spacing: 4) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(data.usage <= data.limit ? Color.green : Color.red)
                            .frame(width: 20, height: CGFloat(data.usage) / 4)
                        
                        Text(data.day)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
}