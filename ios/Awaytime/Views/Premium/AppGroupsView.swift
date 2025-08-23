import SwiftUI

struct AppGroupsView: View {
    @StateObject private var subscriptionService = SubscriptionManager.shared.getService()
    @State private var appGroups: [AppGroup] = sampleAppGroups
    @State private var showingCreateGroup = false
    @State private var selectedGroup: AppGroup?
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header Section
                    headerSection
                    
                    // App Groups List
                    appGroupsSection
                    
                    // Premium Features Info
                    if !subscriptionService.isPremiumActive {
                        premiumInfoSection
                    }
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(AwayTimeColors.background)
            .navigationTitle("App Groups")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        if subscriptionService.canUseFeature(.multipleAppGroups) {
                            showingCreateGroup = true
                        } else {
                            // Show premium paywall
                        }
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .sheet(isPresented: $showingCreateGroup) {
            if subscriptionService.canUseFeature(.multipleAppGroups) {
                CreateAppGroupView { newGroup in
                    appGroups.append(newGroup)
                }
            } else {
                PaywallView(triggeredByFeature: .multipleAppGroups)
            }
        }
        .sheet(item: $selectedGroup) { group in
            AppGroupDetailView(group: group) { updatedGroup in
                if let index = appGroups.firstIndex(where: { $0.id == updatedGroup.id }) {
                    appGroups[index] = updatedGroup
                }
            }
        }
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Organize Your Apps")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Create groups with individual limits and goals")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "folder.badge.plus")
                    .font(.title)
                    .foregroundColor(.purple)
            }
            
            // Quick Stats
            HStack(spacing: 20) {
                StatItem(
                    title: "Groups",
                    value: "\(appGroups.count)",
                    icon: "folder.fill",
                    color: .blue
                )
                
                StatItem(
                    title: "Total Apps",
                    value: "\(appGroups.reduce(0) { $0 + $1.apps.count })",
                    icon: "apps.iphone",
                    color: .green
                )
                
                StatItem(
                    title: "Active Limits",
                    value: "\(appGroups.filter { $0.hasActiveLimit }.count)",
                    icon: "clock.fill",
                    color: .orange
                )
            }
        }
    }
    
    // MARK: - App Groups Section
    
    private var appGroupsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Your Groups")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVStack(spacing: 12) {
                ForEach(appGroups) { group in
                    AppGroupCard(group: group) {
                        selectedGroup = group
                    }
                    .opacity(subscriptionService.canUseFeature(.multipleAppGroups) || group.isDefault ? 1.0 : 0.6)
                    .overlay(
                        Group {
                            if !subscriptionService.canUseFeature(.multipleAppGroups) && !group.isDefault {
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.purple.opacity(0.5), lineWidth: 2)
                                    .overlay(
                                        HStack {
                                            Spacer()
                                            VStack {
                                                Image(systemName: "crown.fill")
                                                    .foregroundColor(.purple)
                                                    .font(.caption)
                                                Text("Premium")
                                                    .font(.caption2)
                                                    .foregroundColor(.purple)
                                            }
                                            .padding(8)
                                            .background(
                                                RoundedRectangle(cornerRadius: 6)
                                                    .fill(Color.purple.opacity(0.1))
                                            )
                                            .padding(.trailing, 8)
                                            .padding(.top, 8)
                                            Spacer()
                                        }
                                    )
                            }
                        }
                    )
                }
            }
        }
    }
    
    // MARK: - Premium Info Section
    
    private var premiumInfoSection: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "crown.fill")
                    .foregroundColor(.purple)
                    .font(.title2)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Unlock Multiple Groups")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Create unlimited app groups with individual limits and customization")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.purple.opacity(0.1))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.purple.opacity(0.3), lineWidth: 1)
                    )
            )
            
            VStack(spacing: 12) {
                PremiumFeatureItem(
                    icon: "folder.badge.plus",
                    title: "Unlimited Groups",
                    description: "Create as many groups as you need"
                )
                
                PremiumFeatureItem(
                    icon: "clock.badge",
                    title: "Individual Limits",
                    description: "Set different time limits for each group"
                )
                
                PremiumFeatureItem(
                    icon: "paintbrush.pointed",
                    title: "Custom Themes",
                    description: "Personalize each group with colors and icons"
                )
                
                PremiumFeatureItem(
                    icon: "chart.bar.fill",
                    title: "Group Analytics",
                    description: "Track usage patterns for each group"
                )
            }
        }
    }
}

// MARK: - Supporting Views

struct StatItem: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.title3)
            
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        )
    }
}

struct AppGroupCard: View {
    let group: AppGroup
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                HStack {
                    HStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(group.color.opacity(0.2))
                                .frame(width: 40, height: 40)
                            
                            Image(systemName: group.icon)
                                .foregroundColor(group.color)
                                .font(.title3)
                        }
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(group.name)
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                            
                            Text("\(group.apps.count) apps")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 2) {
                        if group.hasActiveLimit {
                            Text(group.todayUsage)
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                            
                            Text("of \(group.dailyLimit)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        } else {
                            Text("No limit")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // Progress Bar
                if group.hasActiveLimit {
                    VStack(alignment: .leading, spacing: 4) {
                        ProgressView(value: group.usageProgress)
                            .progressViewStyle(LinearProgressViewStyle(tint: progressColor(for: group.usageProgress)))
                            .scaleEffect(x: 1, y: 2, anchor: .center)
                        
                        HStack {
                            Text("\(Int(group.usageProgress * 100))% used")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            Spacer()
                            
                            Text(group.timeRemaining)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                // App Icons Preview
                HStack(spacing: -8) {
                    ForEach(group.apps.prefix(5), id: \.name) { app in
                        ZStack {
                            Circle()
                                .fill(Color(.systemBackground))
                                .frame(width: 28, height: 28)
                            
                            Circle()
                                .fill(app.color.opacity(0.2))
                                .frame(width: 24, height: 24)
                            
                            Image(systemName: app.icon)
                                .foregroundColor(app.color)
                                .font(.caption)
                        }
                    }
                    
                    if group.apps.count > 5 {
                        ZStack {
                            Circle()
                                .fill(Color(.systemGray5))
                                .frame(width: 24, height: 24)
                            
                            Text("+\(group.apps.count - 5)")
                                .font(.caption2)
                                .fontWeight(.medium)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .foregroundColor(.secondary)
                        .font(.caption)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    private func progressColor(for progress: Double) -> Color {
        if progress < 0.7 {
            return .green
        } else if progress < 0.9 {
            return .orange
        } else {
            return .red
        }
    }
}

struct PremiumFeatureItem: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.purple)
                .font(.title3)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}

// MARK: - Create App Group View

struct CreateAppGroupView: View {
    let onSave: (AppGroup) -> Void
    @Environment(\.dismiss) private var dismiss
    
    @State private var groupName = ""
    @State private var selectedColor = Color.blue
    @State private var selectedIcon = "folder.fill"
    @State private var selectedApps: Set<AppInfo> = []
    @State private var dailyLimitHours = 2
    @State private var dailyLimitMinutes = 0
    @State private var hasLimit = true
    
    private let availableColors: [Color] = [.blue, .green, .orange, .purple, .pink, .red, .yellow, .cyan]
    private let availableIcons = ["folder.fill", "briefcase.fill", "gamecontroller.fill", "tv.fill", "book.fill", "music.note", "camera.fill", "heart.fill"]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Group Details") {
                    TextField("Group Name", text: $groupName)
                    
                    HStack {
                        Text("Color")
                        Spacer()
                        HStack(spacing: 8) {
                            ForEach(availableColors, id: \.self) { color in
                                Button(action: { selectedColor = color }) {
                                    Circle()
                                        .fill(color)
                                        .frame(width: 24, height: 24)
                                        .overlay(
                                            Circle()
                                                .stroke(Color.primary, lineWidth: selectedColor == color ? 2 : 0)
                                        )
                                }
                            }
                        }
                    }
                    
                    HStack {
                        Text("Icon")
                        Spacer()
                        HStack(spacing: 8) {
                            ForEach(availableIcons, id: \.self) { icon in
                                Button(action: { selectedIcon = icon }) {
                                    Image(systemName: icon)
                                        .foregroundColor(selectedIcon == icon ? selectedColor : .secondary)
                                        .font(.title3)
                                        .frame(width: 24, height: 24)
                                }
                            }
                        }
                    }
                }
                
                Section("Daily Limit") {
                    Toggle("Set daily limit", isOn: $hasLimit)
                    
                    if hasLimit {
                        HStack {
                            Text("Hours")
                            Spacer()
                            Picker("Hours", selection: $dailyLimitHours) {
                                ForEach(0...12, id: \.self) { hour in
                                    Text("\(hour)").tag(hour)
                                }
                            }
                            .pickerStyle(MenuPickerStyle())
                        }
                        
                        HStack {
                            Text("Minutes")
                            Spacer()
                            Picker("Minutes", selection: $dailyLimitMinutes) {
                                ForEach([0, 15, 30, 45], id: \.self) { minute in
                                    Text("\(minute)").tag(minute)
                                }
                            }
                            .pickerStyle(MenuPickerStyle())
                        }
                    }
                }
                
                Section("Apps") {
                    Text("Select apps for this group")
                        .foregroundColor(.secondary)
                    
                    // App selection would go here
                    Text("App selection coming soon")
                        .foregroundColor(.secondary)
                        .italic()
                }
            }
            .navigationTitle("New Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        let newGroup = AppGroup(
                            name: groupName.isEmpty ? "New Group" : groupName,
                            icon: selectedIcon,
                            color: selectedColor,
                            apps: Array(selectedApps),
                            dailyLimitMinutes: hasLimit ? (dailyLimitHours * 60 + dailyLimitMinutes) : nil,
                            isDefault: false
                        )
                        onSave(newGroup)
                        dismiss()
                    }
                    .disabled(groupName.isEmpty)
                }
            }
        }
    }
}

// MARK: - App Group Detail View

struct AppGroupDetailView: View {
    let group: AppGroup
    let onUpdate: (AppGroup) -> Void
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Group header
                    VStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(group.color.opacity(0.2))
                                .frame(width: 80, height: 80)
                            
                            Image(systemName: group.icon)
                                .foregroundColor(group.color)
                                .font(.system(size: 32))
                        }
                        
                        Text(group.name)
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                    
                    // Usage stats
                    if group.hasActiveLimit {
                        VStack(spacing: 12) {
                            Text("Today's Usage")
                                .font(.headline)
                            
                            Text(group.todayUsage)
                                .font(.largeTitle)
                                .fontWeight(.bold)
                                .foregroundColor(group.color)
                            
                            Text("of \(group.dailyLimit) limit")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            ProgressView(value: group.usageProgress)
                                .progressViewStyle(LinearProgressViewStyle(tint: group.color))
                                .scaleEffect(x: 1, y: 3, anchor: .center)
                        }
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color(.systemBackground))
                                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
                        )
                    }
                    
                    // Apps in group
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Apps in Group")
                            .font(.headline)
                        
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 16) {
                            ForEach(group.apps, id: \.name) { app in
                                VStack(spacing: 8) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 12)
                                            .fill(app.color.opacity(0.2))
                                            .frame(width: 50, height: 50)
                                        
                                        Image(systemName: app.icon)
                                            .foregroundColor(app.color)
                                            .font(.title2)
                                    }
                                    
                                    Text(app.name)
                                        .font(.caption)
                                        .lineLimit(1)
                                }
                            }
                        }
                    }
                    
                    Spacer(minLength: 20)
                }
                .padding()
            }
            .navigationTitle("Group Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Models



// MARK: - Sample Data

private let sampleAppGroups = [
    AppGroup(
        name: "Social Media",
        icon: "person.2.fill",
        color: .pink,
        apps: [
            AppInfo(name: "Instagram", bundleId: "com.instagram.app", icon: "camera.fill", color: .pink),
            AppInfo(name: "Twitter", bundleId: "com.twitter.app", icon: "at", color: .blue),
            AppInfo(name: "TikTok", bundleId: "com.tiktok.app", icon: "music.note", color: .black)
        ],
        dailyLimitMinutes: 120,
        isDefault: true
    ),
    AppGroup(
        name: "Work & Productivity",
        icon: "briefcase.fill",
        color: .blue,
        apps: [
            AppInfo(name: "Slack", bundleId: "com.slack.app", icon: "message.fill", color: .purple),
            AppInfo(name: "Notion", bundleId: "com.notion.app", icon: "doc.text", color: .gray),
            AppInfo(name: "Calendar", bundleId: "com.apple.calendar", icon: "calendar", color: .red)
        ],
        dailyLimitMinutes: nil,
        isDefault: false
    ),
    AppGroup(
        name: "Entertainment",
        icon: "tv.fill",
        color: .orange,
        apps: [
            AppInfo(name: "YouTube", bundleId: "com.youtube.app", icon: "play.rectangle.fill", color: .red),
            AppInfo(name: "Netflix", bundleId: "com.netflix.app", icon: "tv", color: .red),
            AppInfo(name: "Spotify", bundleId: "com.spotify.app", icon: "music.note", color: .green)
        ],
        dailyLimitMinutes: 180,
        isDefault: false
    )
]

#Preview {
    AppGroupsView()
}