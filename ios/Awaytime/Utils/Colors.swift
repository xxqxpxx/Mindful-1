import SwiftUI

extension Color {
    // Awaytime Purple Color Scheme - Enhanced with better contrast
    static let awayTimePurple = Color(red: 0.545, green: 0.361, blue: 0.965) // #8B5CF6
    static let awayTimeLightPurple = Color(red: 0.769, green: 0.710, blue: 0.992) // #C4B5FD
    static let awayTimeDarkPurple = Color(red: 0.357, green: 0.129, blue: 0.714) // #5B21B6
    
    // Additional colors with improved accessibility
    static let awayTimeSuccess = Color(red: 0.063, green: 0.725, blue: 0.506) // #10B981
    static let awayTimeWarning = Color(red: 0.961, green: 0.620, blue: 0.043) // #F59E0B
    static let awayTimeError = Color(red: 0.937, green: 0.333, blue: 0.333) // #EF4444
    
    // Background colors with better contrast
    static let awayTimeBackground = Color(red: 0.980, green: 0.980, blue: 0.980) // #FAFAFA
    static let awayTimeDarkBackground = Color(red: 0.102, green: 0.102, blue: 0.102) // #1A1A1A
    static let awayTimeCardBackground = Color(red: 1.0, green: 1.0, blue: 1.0) // #FFFFFF
    static let awayTimeDarkCardBackground = Color(red: 0.165, green: 0.165, blue: 0.165) // #2A2A2A
    
    // Loading and skeleton colors
    static let awayTimeSkeletonBase = Color(red: 0.9, green: 0.9, blue: 0.9) // #E5E5E5
    static let awayTimeSkeletonHighlight = Color(red: 0.95, green: 0.95, blue: 0.95) // #F2F2F2
}

struct AwayTimeColors {
    static let primary = Color.awayTimePurple
    static let secondary = Color.awayTimeLightPurple
    static let accent = Color.awayTimeDarkPurple
    static let success = Color.awayTimeSuccess
    static let warning = Color.awayTimeWarning
    static let error = Color.awayTimeError
    static let background = Color.awayTimeBackground
    static let darkBackground = Color.awayTimeDarkBackground
    static let cardBackground = Color.awayTimeCardBackground
    static let darkCardBackground = Color.awayTimeDarkCardBackground
    static let skeletonBase = Color.awayTimeSkeletonBase
    static let skeletonHighlight = Color.awayTimeSkeletonHighlight
}

// MARK: - Loading States and Skeleton Views

struct SkeletonView: View {
    @State private var isAnimating = false
    
    let width: CGFloat
    let height: CGFloat
    let cornerRadius: CGFloat
    
    init(width: CGFloat, height: CGFloat, cornerRadius: CGFloat = 8) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }
    
    var body: some View {
        Rectangle()
            .fill(
                LinearGradient(
                    colors: [
                        AwayTimeColors.skeletonBase,
                        AwayTimeColors.skeletonHighlight,
                        AwayTimeColors.skeletonBase
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(width: width, height: height)
            .cornerRadius(cornerRadius)
            .mask(
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.clear,
                                Color.black,
                                Color.clear
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .scaleEffect(x: 3)
                    .offset(x: isAnimating ? width : -width)
            )
            .onAppear {
                withAnimation(
                    .linear(duration: 1.5)
                    .repeatForever(autoreverses: false)
                ) {
                    isAnimating = true
                }
            }
    }
}

struct LoadingProgressCircle: View {
    @State private var isAnimating = false
    
    let size: CGFloat
    let lineWidth: CGFloat
    
    init(size: CGFloat = 200, lineWidth: CGFloat = 12) {
        self.size = size
        self.lineWidth = lineWidth
    }
    
    var body: some View {
        ZStack {
            // Background circle
            Circle()
                .stroke(AwayTimeColors.secondary.opacity(0.2), lineWidth: lineWidth)
                .frame(width: size, height: size)
            
            // Animated loading circle
            Circle()
                .trim(from: 0, to: 0.3)
                .stroke(
                    AwayTimeColors.primary,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .frame(width: size, height: size)
                .rotationEffect(.degrees(isAnimating ? 360 : 0))
                .animation(
                    .linear(duration: 1.0)
                    .repeatForever(autoreverses: false),
                    value: isAnimating
                )
            
            // Loading text
            VStack(spacing: 4) {
                Text("Loading...")
                    .font(.headline)
                    .foregroundColor(AwayTimeColors.primary)
                
                Text("Fetching your data")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .onAppear {
            isAnimating = true
        }
    }
}

// MARK: - Smooth Transition Components

struct SmoothNumberTransition: View {
    let value: Int
    let formatter: (Int) -> String
    let font: Font
    let color: Color
    
    @State private var displayValue: Int = 0
    
    init(
        value: Int,
        formatter: @escaping (Int) -> String = { "\($0)" },
        font: Font = .title,
        color: Color = .primary
    ) {
        self.value = value
        self.formatter = formatter
        self.font = font
        self.color = color
    }
    
    var body: some View {
        Text(formatter(displayValue))
            .font(font)
            .foregroundColor(color)
            .contentTransition(.numericText())
            .onAppear {
                animateToValue()
            }
            .onChange(of: value) { _ in
                animateToValue()
            }
    }
    
    private func animateToValue() {
        let duration = 1.0
        let steps = max(abs(value - displayValue), 1)
        let stepDuration = duration / Double(steps)
        
        let increment = value > displayValue ? 1 : -1
        
        for i in 0..<steps {
            DispatchQueue.main.asyncAfter(deadline: .now() + stepDuration * Double(i)) {
                withAnimation(.easeOut(duration: 0.1)) {
                    displayValue += increment
                }
            }
        }
    }
}

// MARK: - Enhanced Button Styles

struct AwayTimeButtonStyle: ButtonStyle {
    let color: Color
    let isSecondary: Bool
    
    init(color: Color = AwayTimeColors.primary, isSecondary: Bool = false) {
        self.color = color
        self.isSecondary = isSecondary
    }
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSecondary ? color.opacity(0.1) : color)
            )
            .foregroundColor(isSecondary ? color : .white)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

// MARK: - Accessibility Improvements

extension View {
    func awayTimeAccessibility(
        label: String? = nil,
        hint: String? = nil,
        value: String? = nil,
        traits: AccessibilityTraits = []
    ) -> some View {
        self
            .accessibilityLabel(label ?? "")
            .accessibilityHint(hint ?? "")
            .accessibilityValue(value ?? "")
            .accessibilityAddTraits(traits)
    }
    
    func awayTimeHapticFeedback(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .medium) -> some View {
        self.onTapGesture {
            let impactFeedback = UIImpactFeedbackGenerator(style: style)
            impactFeedback.impactOccurred()
        }
    }
}