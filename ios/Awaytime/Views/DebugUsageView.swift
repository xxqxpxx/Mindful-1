import SwiftUI

struct DebugUsageView: View {
    @StateObject private var simulationService = UsageSimulationService()
    @State private var debugInfo: [String] = []
    @State private var isRefreshing = false
    
    var body: some View {
        NavigationView {
            List {
                Section("Current Status") {
                    HStack {
                        Text("Usage Simulation:")
                        Spacer()
                        Text(simulationService.isSimulating ? "🎭 Running" : "⏸️ Stopped")
                            .foregroundColor(simulationService.isSimulating ? .green : .gray)
                    }
                    
                    if simulationService.isSimulating {
                        HStack {
                            Text("Simulated Usage:")
                            Spacer()
                            Text("\(simulationService.simulatedUsageMinutes) minutes")
                                .foregroundColor(.blue)
                        }
                    }
                }
                
                Section("Debug Information") {
                    ForEach(debugInfo, id: \.self) { info in
                        Text(info)
                            .font(.system(.caption, design: .monospaced))
                    }
                }
                
                Section("Usage Simulation") {
                    Button(simulationService.isSimulating ? "Stop Simulation" : "Start Simulation") {
                        if simulationService.isSimulating {
                            simulationService.stopSimulation()
                        } else {
                            simulationService.startSimulation()
                        }
                    }
                    
                    Button("Simulate Warning Event") {
                        simulationService.simulateWarningEvent()
                    }
                    .disabled(simulationService.isSimulating)
                    
                    Button("Simulate Limit Reached") {
                        simulationService.simulateLimitEvent()
                    }
                    .disabled(simulationService.isSimulating)
                    
                    Button("Add Random Usage") {
                        simulationService.addRandomUsage()
                    }
                    .disabled(simulationService.isSimulating)
                }
                
                Section("Debug Actions") {
                    Button("Refresh Usage Data") {
                        refreshUsageData()
                    }
                    .disabled(isRefreshing)
                    
                    Button("Check UserDefaults") {
                        checkUserDefaults()
                    }
                    
                    Button("Check Core Data") {
                        checkCoreData()
                    }
                }
            }
            .navigationTitle("Usage Debug")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Refresh") {
                        refreshUsageData()
                    }
                    .disabled(isRefreshing)
                }
            }
            .onAppear {
                refreshUsageData()
            }
        }
    }
    
    private func refreshUsageData() {
        isRefreshing = true
        debugInfo.removeAll()
        
        Task {
            // Check app groups
            let coreDataManager = CoreDataManager.shared
            let appGroups = coreDataManager.fetchAppGroups()
            
            await MainActor.run {
                debugInfo.append("📱 App Groups: \(appGroups.count)")
                
                for group in appGroups {
                    debugInfo.append("  - \(group.name): \(group.dailyLimitMinutes)min limit")
                    debugInfo.append("    Active: \(group.isActive)")
                    
                    let todayUsage = coreDataManager.getTodayUsage(for: group.name)
                    debugInfo.append("    Today's usage: \(todayUsage) minutes")
                    
                    if group.isActive {
                        // Real-time usage temporarily disabled until UsageTrackingService is fixed
                        debugInfo.append("    Real-time usage: Service unavailable")
                    }
                }
                
                isRefreshing = false
            }
        }
    }
    
    private func checkUserDefaults() {
        let userDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
        let keys = userDefaults?.dictionaryRepresentation().keys.filter { 
            $0.contains("usage") || $0.contains("Usage") || $0.contains("Event") || $0.contains("event")
        } ?? []
        
        debugInfo.append("🔧 UserDefaults keys: \(keys.count)")
        for key in keys.sorted() {
            if let value = userDefaults?.object(forKey: key) {
                debugInfo.append("  \(key): \(value)")
            }
        }
    }
    
    private func checkCoreData() {
        let coreDataManager = CoreDataManager.shared
        let usageRecords = coreDataManager.getUsageRecords(days: 1)
        
        debugInfo.append("💾 Usage Records (today): \(usageRecords.count)")
        for record in usageRecords.prefix(5) {
            let formatter = DateFormatter()
            formatter.timeStyle = .short
            debugInfo.append("  \(formatter.string(from: record.date)): \(record.usageMinutes)min (\(record.appGroupName))")
        }
    }
}

#Preview {
    DebugUsageView()
}