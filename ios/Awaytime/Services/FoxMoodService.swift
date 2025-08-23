import Foundation
import SwiftUI

class FoxMoodService: ObservableObject {
    @Published var currentMood: FoxMood = .happy
    @Published var usagePercent: Float = 0.0
    
    private let userDefaults = UserDefaults.standard
    private let currentMoodKey = "current_fox_mood"
    
    init() {
        loadSavedMood()
    }
    
    func updateUsagePercent(_ percent: Float) {
        usagePercent = percent
        let newMood = FoxMood.fromUsagePercent(percent)
        
        if newMood != currentMood {
            currentMood = newMood
            saveMood(newMood)
        }
    }
    
    private func loadSavedMood() {
        if let savedMoodString = userDefaults.string(forKey: currentMoodKey),
           let savedMood = FoxMood(rawValue: savedMoodString) {
            currentMood = savedMood
        }
    }
    
    private func saveMood(_ mood: FoxMood) {
        userDefaults.set(mood.rawValue, forKey: currentMoodKey)
    }
    
    func getMoodMessage(for mood: FoxMood) -> String {
        switch mood {
        case .happy:
            return "Great job! You're managing your screen time well."
        case .sleepy:
            return "You're doing okay, but consider taking a break soon."
        case .sad:
            return "You're getting close to your limit. Time for a break?"
        case .exhausted:
            return "You've reached your limit. Please take a break!"
        }
    }
    
    func getMoodColor(for mood: FoxMood) -> Color {
        switch mood {
        case .happy:
            return .green
        case .sleepy:
            return .orange
        case .sad:
            return .blue
        case .exhausted:
            return .gray
        }
    }
}