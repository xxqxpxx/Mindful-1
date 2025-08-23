import SwiftUI

@MainActor
struct LimitSettingView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedHours = 2
    @State private var selectedMinutes = 0
    @State private var error: String?
    @State private var isSaving = false
    
    private let coreDataManager = CoreDataManager.shared
    private let hours = Array(0...12)
    private let minutes = Array(0...59).filter { $0 % 15 == 0 }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 32) {
                VStack(spacing: 16) {
                    Image(systemName: "clock")
                        .font(.system(size: 60))
                        .foregroundColor(.purple)
                    
                    Text("Set Daily Limit")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Choose a realistic daily time limit for your selected apps")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                
                VStack(spacing: 16) {
                    Text("Daily Time Limit")
                        .font(.headline)
                    
                    HStack {
                        Picker("Hours", selection: $selectedHours) {
                            ForEach(hours, id: \.self) { hour in
                                Text("\(hour)h").tag(hour)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                        .frame(width: 100)
                        
                        Picker("Minutes", selection: $selectedMinutes) {
                            ForEach(minutes, id: \.self) { minute in
                                Text("\(minute)m").tag(minute)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                        .frame(width: 100)
                    }
                    .frame(height: 150)
                }
                
                Button(action: {
                    saveLimit()
                }) {
                    HStack {
                        if isSaving {
                            ProgressView()
                                .scaleEffect(0.8)
                                .foregroundColor(.white)
                        }
                        Text(isSaving ? "Saving..." : "Set Limit")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(isSaving || (selectedHours == 0 && selectedMinutes == 0))
                .padding(.horizontal, 32)
                
                if let error = error {
                    Text("Error: \(error)")
                        .foregroundColor(.red)
                        .font(.caption)
                        .padding(.horizontal, 32)
                }
                
                Spacer()
            }
            .navigationTitle("Daily Limit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                loadCurrentLimit()
            }
        }
    }
    
    private func loadCurrentLimit() {
        // Load current limit from active app group
        let appGroups = coreDataManager.fetchAppGroups()
        if let activeGroup = appGroups.first(where: { $0.isActive }) {
            let totalMinutes = Int(activeGroup.dailyLimitMinutes)
            selectedHours = totalMinutes / 60
            selectedMinutes = totalMinutes % 60
        }
    }
    
    private func saveLimit() {
        guard selectedHours > 0 || selectedMinutes > 0 else {
            error = "Please set a limit greater than 0 minutes"
            return
        }
        
        isSaving = true
        error = nil
        
        let totalMinutes = selectedHours * 60 + selectedMinutes
        
        // Get or create active app group
        let appGroups = coreDataManager.fetchAppGroups()
        let activeGroup: AppGroupEntity
        
        if let existingGroup = appGroups.first(where: { $0.isActive }) {
            activeGroup = existingGroup
        } else {
            // Create new app group if none exists
            activeGroup = coreDataManager.createAppGroup(
                name: "My Apps",
                dailyLimitMinutes: totalMinutes,
                isActive: true
            )
        }
        
        // Update the limit
        activeGroup.dailyLimitMinutes = Int32(totalMinutes)
        
        // Save to Core Data
        do {
            coreDataManager.saveContext()
            
            // Also save to UserDefaults for backward compatibility
            UserDefaults.standard.set(totalMinutes, forKey: "dailyLimitMinutes")
            
            // Save to app group UserDefaults for DeviceActivity extension
            let sharedDefaults = UserDefaults(suiteName: "group.com.awaytime.app")
            sharedDefaults?.set(totalMinutes, forKey: "dailyLimitMinutes_My Apps")
            sharedDefaults?.set(activeGroup.name, forKey: "activeGroupName")
            
            print("✅ Daily limit saved: \(totalMinutes) minutes")
            
            // Start monitoring with new limit
            Task {
                // Create service instance (both LimitSettingView and UsageTrackingService are @MainActor)
                let usageTrackingService = UsageTrackingService()
                usageTrackingService.startMonitoring(for: activeGroup)
                print("📱 Started monitoring for app group: \(activeGroup.name)")
            }
            
            isSaving = false
            dismiss()
            
        } catch {
            self.error = "Failed to save limit: \(error.localizedDescription)"
            isSaving = false
            print("❌ Failed to save limit: \(error)")
        }
    }
}

#Preview {
    LimitSettingView()
}