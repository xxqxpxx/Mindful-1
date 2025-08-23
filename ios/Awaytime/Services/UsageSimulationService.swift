import Foundation
import SwiftUI

/// Service to simulate usage for testing dashboard functionality
@MainActor
class UsageSimulationService: ObservableObject {
    @Published var isSimulating = false
    @Published var simulatedUsageMinutes = 0
    
    private var simulationTimer: Timer?
    private let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
    
    func startSimulation() {
        guard !isSimulating else { return }
        
        isSimulating = true
        simulatedUsageMinutes = 0
        
        print("🎭 Starting usage simulation...")
        
        // Start with some initial usage
        simulatedUsageMinutes = Int.random(in: 10...30)
        updateSimulatedData()
        
        // Update every 10 seconds to simulate real-time changes
        simulationTimer = Timer.scheduledTimer(withTimeInterval: 10.0, repeats: true) { _ in
            self.incrementUsage()
        }
    }
    
    func stopSimulation() {
        isSimulating = false
        simulationTimer?.invalidate()
        simulationTimer = nil
        
        // Clear simulated data
        userDefaults?.removeObject(forKey: "todayUsage_My Apps")
        userDefaults?.removeObject(forKey: "currentUsageMinutes_My Apps")
        userDefaults?.removeObject(forKey: "lastUpdated_My Apps")
        
        print("🎭 Stopped usage simulation")
    }
    
    private func incrementUsage() {
        // Simulate realistic usage increase (1-5 minutes per update)
        let increment = Int.random(in: 1...5)
        simulatedUsageMinutes += increment
        
        // Cap at reasonable daily limit
        simulatedUsageMinutes = min(simulatedUsageMinutes, 180)
        
        updateSimulatedData()
        
        // Simulate threshold events
        let dailyLimit = UserDefaults.standard.integer(forKey: "dailyLimitMinutes")
        let limitToUse = dailyLimit > 0 ? dailyLimit : 120
        
        let usagePercentage = Double(simulatedUsageMinutes) / Double(limitToUse)
        
        if usagePercentage >= 0.8 && usagePercentage < 0.9 {
            // Simulate warning event
            simulateThresholdEvent(type: "warning")
        } else if usagePercentage >= 1.0 {
            // Simulate limit event
            simulateThresholdEvent(type: "limit")
            stopSimulation() // Stop when limit reached
        }
        
        print("🎭 Simulated usage: \(simulatedUsageMinutes) minutes (\(Int(usagePercentage * 100))% of limit)")
    }
    
    private func updateSimulatedData() {
        // Update shared UserDefaults that the dashboard reads from
        userDefaults?.set(simulatedUsageMinutes, forKey: "todayUsage_My Apps")
        userDefaults?.set(simulatedUsageMinutes, forKey: "currentUsageMinutes_My Apps")
        userDefaults?.set(Date(), forKey: "lastUpdated_My Apps")
        
        // Also update Core Data for persistence
        CoreDataManager.shared.saveUsageRecord(
            date: Date(),
            usageMinutes: simulatedUsageMinutes,
            appGroupName: "My Apps",
            limitExceeded: false
        )
    }
    
    private func simulateThresholdEvent(type: String) {
        let eventData = [
            "type": type,
            "appGroupName": "My Apps",
            "timestamp": Date().timeIntervalSince1970
        ] as [String: Any]
        
        userDefaults?.set(eventData, forKey: "latestEvent")
        
        print("🎭 Simulated \(type) event at \(simulatedUsageMinutes) minutes")
    }
    
    // MARK: - Quick Actions
    
    func simulateWarningEvent() {
        let dailyLimit = UserDefaults.standard.integer(forKey: "dailyLimitMinutes")
        let limitToUse = dailyLimit > 0 ? dailyLimit : 120
        
        simulatedUsageMinutes = Int(Double(limitToUse) * 0.8)
        updateSimulatedData()
        simulateThresholdEvent(type: "warning")
    }
    
    func simulateLimitEvent() {
        let dailyLimit = UserDefaults.standard.integer(forKey: "dailyLimitMinutes")
        let limitToUse = dailyLimit > 0 ? dailyLimit : 120
        
        simulatedUsageMinutes = limitToUse
        updateSimulatedData()
        simulateThresholdEvent(type: "limit")
    }
    
    func addRandomUsage() {
        let addition = Int.random(in: 5...15)
        simulatedUsageMinutes += addition
        updateSimulatedData()
    }
    
    deinit {
        simulationTimer?.invalidate()
    }
}