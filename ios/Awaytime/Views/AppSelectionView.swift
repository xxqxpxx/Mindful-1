import SwiftUI
import FamilyControls

struct AppSelectionView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var permissionService = PermissionService()
    @State private var familyActivitySelection = FamilyActivitySelection()
    @State private var isShowingFamilyPicker = false
    @State private var error: String?
    
    private let coreDataManager = CoreDataManager.shared
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if permissionService.authorizationStatus != .approved {
                    // Permission required view
                    VStack(spacing: 16) {
                        Image(systemName: "lock.shield")
                            .font(.system(size: 60))
                            .foregroundColor(.purple)
                        
                        Text("Screen Time Permission Required")
                            .font(.headline)
                            .multilineTextAlignment(.center)
                        
                        Text("To select apps for monitoring, please grant Screen Time permission.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        
                        Button("Grant Permission") {
                            Task {
                                await permissionService.requestPermission()
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.purple)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                    }
                    .padding()
                } else {
                    // App selection view
                    VStack(spacing: 16) {
                        Text("Select Apps to Monitor")
                            .font(.headline)
                            .padding(.top)
                        
                        Text("Choose which apps you'd like to track and set limits for.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        
                        // Family Activity Picker Button
                        Button(action: {
                            print("🖱️ App selection picker button tapped")
                            isShowingFamilyPicker = true
                        }) {
                            HStack {
                                Image(systemName: "apps.iphone")
                                    .font(.title2)
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Choose Apps & Categories")
                                        .font(.headline)
                                    
                                    Text(selectionSummary)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                Image(systemName: "chevron.right")
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(.systemGray6))
                            )
                        }
                        .foregroundColor(.primary)
                        .padding(.horizontal, 20)
                        
                        // Selected apps preview with animations
                        if !familyActivitySelection.applicationTokens.isEmpty || !familyActivitySelection.categoryTokens.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Text("Selected for Monitoring:")
                                        .font(.headline)
                                    
                                    Spacer()
                                    
                                    // Clear button with debug logging
                                    Button("Clear All") {
                                        print("🗑️ Clear all button tapped")
                                        withAnimation(.easeOut(duration: 0.3)) {
                                            familyActivitySelection = FamilyActivitySelection()
                                        }
                                    }
                                    .font(.caption)
                                    .foregroundColor(.red)
                                }
                                .padding(.horizontal, 20)
                                
                                ScrollView {
                                    VStack(spacing: 8) {
                                        if !familyActivitySelection.applicationTokens.isEmpty {
                                            HStack {
                                                Image(systemName: "apps.iphone")
                                                    .foregroundColor(.purple)
                                                Text("Apps: \(familyActivitySelection.applicationTokens.count) selected")
                                                    .font(.subheadline)
                                                    .foregroundColor(.purple)
                                                Spacer()
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .fill(Color.purple.opacity(0.1))
                                            )
                                            .transition(.scale.combined(with: .opacity))
                                        }
                                        
                                        if !familyActivitySelection.categoryTokens.isEmpty {
                                            HStack {
                                                Image(systemName: "rectangle.3.group")
                                                    .foregroundColor(.purple)
                                                Text("Categories: \(familyActivitySelection.categoryTokens.count) selected")
                                                    .font(.subheadline)
                                                    .foregroundColor(.purple)
                                                Spacer()
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .fill(Color.purple.opacity(0.1))
                                            )
                                            .transition(.scale.combined(with: .opacity))
                                        }
                                        
                                        if !familyActivitySelection.webDomainTokens.isEmpty {
                                            HStack {
                                                Image(systemName: "globe")
                                                    .foregroundColor(.purple)
                                                Text("Web Domains: \(familyActivitySelection.webDomainTokens.count) selected")
                                                    .font(.subheadline)
                                                    .foregroundColor(.purple)
                                                Spacer()
                                            }
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .fill(Color.purple.opacity(0.1))
                                            )
                                            .transition(.scale.combined(with: .opacity))
                                        }
                                    }
                                    .animation(.easeInOut(duration: 0.3), value: familyActivitySelection.applicationTokens.count)
                                    .animation(.easeInOut(duration: 0.3), value: familyActivitySelection.categoryTokens.count)
                                    .animation(.easeInOut(duration: 0.3), value: familyActivitySelection.webDomainTokens.count)
                                }
                                .frame(maxHeight: 150)
                                .padding(.horizontal, 20)
                            }
                        }
                        
                        Spacer()
                        
                        if let error = error {
                            Text("Error: \(error)")
                                .foregroundColor(.red)
                                .font(.caption)
                                .padding(.horizontal, 20)
                        }
                    }
                }
            }
            .navigationTitle("Select Apps")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        saveSelection()
                        dismiss()
                    }
                    .font(.headline)
                    .disabled(!hasValidSelection)
                }
            }
        }
        .familyActivityPicker(isPresented: $isShowingFamilyPicker, selection: $familyActivitySelection)
        .onAppear {
            permissionService.updateAuthorizationStatus()
            loadSavedSelection()
        }
        .onChange(of: familyActivitySelection) { _ in
            // Debug logging for selection changes
            let appCount = familyActivitySelection.applicationTokens.count
            let categoryCount = familyActivitySelection.categoryTokens.count
            let webCount = familyActivitySelection.webDomainTokens.count
            
            print("🔄 Family Activity Selection changed: \(appCount) apps, \(categoryCount) categories, \(webCount) websites")
            
            // Auto-save when selection changes
            saveSelection()
        }
    }
    
    private var selectionSummary: String {
        let appCount = familyActivitySelection.applicationTokens.count
        let categoryCount = familyActivitySelection.categoryTokens.count
        let webCount = familyActivitySelection.webDomainTokens.count
        
        if appCount == 0 && categoryCount == 0 && webCount == 0 {
            return "Tap to select apps and categories"
        }
        
        var parts: [String] = []
        if appCount > 0 { parts.append("\(appCount) apps") }
        if categoryCount > 0 { parts.append("\(categoryCount) categories") }
        if webCount > 0 { parts.append("\(webCount) websites") }
        
        return parts.joined(separator: ", ")
    }
    
    private var hasValidSelection: Bool {
        return !familyActivitySelection.applicationTokens.isEmpty || 
               !familyActivitySelection.categoryTokens.isEmpty ||
               !familyActivitySelection.webDomainTokens.isEmpty
    }
    
    private func loadSavedSelection() {
        // Load from the current active app group
        let appGroups = coreDataManager.fetchAppGroups()
        if let activeGroup = appGroups.first(where: { $0.isActive }),
           let selectionData = activeGroup.selectedAppsData {
            
            do {
                if let selection = try NSKeyedUnarchiver.unarchiveTopLevelObjectWithData(selectionData) as? FamilyActivitySelection {
                    familyActivitySelection = selection
                }
            } catch {
                print("❌ Failed to load saved selection: \(error)")
                self.error = "Failed to load saved selection"
            }
        }
    }
    
    private func saveSelection() {
        guard hasValidSelection else { return }
        
        do {
            // Archive the FamilyActivitySelection
            let selectionData = try NSKeyedArchiver.archivedData(withRootObject: familyActivitySelection, requiringSecureCoding: true)
            
            // Get or create the active app group
            let appGroups = coreDataManager.fetchAppGroups()
            let activeGroup: AppGroupEntity
            
            if let existingGroup = appGroups.first(where: { $0.isActive }) {
                activeGroup = existingGroup
            } else {
                // Create new app group
                activeGroup = coreDataManager.createAppGroup(
                    name: "My Apps",
                    dailyLimitMinutes: 120, // Default 2 hours
                    isActive: true
                )
            }
            
            // Update the app group with new selection
            activeGroup.selectedAppsData = selectionData
            
            // Save to Core Data
            coreDataManager.saveContext()
            
            print("✅ App selection saved successfully")
            
        } catch {
            print("❌ Failed to save selection: \(error)")
            self.error = "Failed to save selection: \(error.localizedDescription)"
        }
    }
}





#Preview {
    AppSelectionView()
}