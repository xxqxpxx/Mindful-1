import SwiftUI

struct AwayTimeColors {
    static let primary = Color.purple
    static let secondary = Color.blue
    static let accent = Color.orange
    static let success = Color.green
    static let warning = Color.orange
    static let error = Color.red
    static let background = Color(.systemBackground)
    static let cardBackground = Color(.systemBackground)
}

// MARK: - Accessibility Extensions

extension View {
    func awayTimeAccessibility(label: String, hint: String? = nil, value: String? = nil) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityHint(hint ?? "")
            .accessibilityValue(value ?? "")
    }
    
    func awayTimeHapticFeedback() -> some View {
        self.onTapGesture {
            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
            impactFeedback.impactOccurred()
        }
    }
}