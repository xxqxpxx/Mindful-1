import Foundation

class OnboardingManager {
    private static let onboardingCompletedKey = "onboarding_completed"
    
    static var isOnboardingCompleted: Bool {
        UserDefaults.standard.bool(forKey: onboardingCompletedKey)
    }
    
    static func completeOnboarding() {
        UserDefaults.standard.set(true, forKey: onboardingCompletedKey)
    }
    
    static func resetOnboarding() {
        UserDefaults.standard.removeObject(forKey: onboardingCompletedKey)
    }
    
    // For testing purposes
    static func showOnboardingAgain() {
        resetOnboarding()
    }
}

// MARK: - Notification Extensions
extension Notification.Name {
    static let showConfettiAnimation = Notification.Name("showConfettiAnimation")
}