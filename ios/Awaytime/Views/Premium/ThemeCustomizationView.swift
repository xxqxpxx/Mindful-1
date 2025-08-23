import SwiftUI

struct ThemeCustomizationView: View {
    @StateObject private var subscriptionService = SubscriptionManager.shared.getService()
    @StateObject private var themeManager = ThemeManager.shared
    @State private var selectedTheme: AppTheme = .default
    @State private var showingColorPicker = false
    @State private var customColor = Color.purple
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 32) {
                    // Header Section
                    headerSection
                    
                    // Theme Preview
                    themePreviewSection
                    
                    // Predefined Themes
                    predefinedThemesSection
                    
                    // Custom Theme Builder
                    customThemeSection
                    
                    // Advanced Customization
                    advancedCustomizationSection
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(themeManager.currentTheme.backgroundColor)
            .navigationTitle("Themes")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Apply") {
                        themeManager.setTheme(selectedTheme)
                    }
                    .disabled(!subscriptionService.canUseFeature(.customThemes))
                }
            }
        }
        .onAppear {
            selectedTheme = themeManager.currentTheme
        }
        .requiresPremium(.customThemes)
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Customize Your Experience")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(themeManager.currentTheme.primaryTextColor)
                    
                    Text("Personalize colors, themes, and visual elements")
                        .font(.subheadline)
                        .foregroundColor(themeManager.currentTheme.secondaryTextColor)
                }
                
                Spacer()
                
                Image(systemName: "paintbrush.pointed.fill")
                    .font(.title)
                    .foregroundColor(themeManager.currentTheme.accentColor)
            }
            
            if !subscriptionService.isPremiumActive {
                HStack {
                    Image(systemName: "crown.fill")
                        .foregroundColor(.purple)
                    
                    Text("Premium Feature")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.purple)
                    
                    Spacer()
                }
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.purple.opacity(0.1))
                )
            }
        }
    }
    
    // MARK: - Theme Preview Section
    
    private var themePreviewSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Preview")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(themeManager.currentTheme.primaryTextColor)
            
            ThemePreviewCard(theme: selectedTheme)
        }
    }
    
    // MARK: - Predefined Themes Section
    
    private var predefinedThemesSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Predefined Themes")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(themeManager.currentTheme.primaryTextColor)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                ForEach(AppTheme.predefinedThemes, id: \.name) { theme in
                    ThemeCard(
                        theme: theme,
                        isSelected: selectedTheme.name == theme.name,
                        onSelect: {
                            selectedTheme = theme
                        }
                    )
                    .disabled(!subscriptionService.canUseFeature(.customThemes))
                    .opacity(subscriptionService.canUseFeature(.customThemes) ? 1.0 : 0.6)
                }
            }
        }
    }
    
    // MARK: - Custom Theme Section
    
    private var customThemeSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Create Custom Theme")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(themeManager.currentTheme.primaryTextColor)
            
            VStack(spacing: 16) {
                // Primary Color Picker
                ColorPickerRow(
                    title: "Primary Color",
                    color: $customColor,
                    description: "Main accent color for buttons and highlights"
                )
                
                // Background Style
                BackgroundStylePicker(selectedTheme: $selectedTheme)
                
                // Text Style
                TextStylePicker(selectedTheme: $selectedTheme)
                
                // Create Custom Theme Button
                Button(action: {
                    let customTheme = AppTheme.createCustom(
                        name: "Custom Theme",
                        primaryColor: customColor
                    )
                    selectedTheme = customTheme
                }) {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                        Text("Create Custom Theme")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(customColor)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(!subscriptionService.canUseFeature(.customThemes))
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
        }
    }
    
    // MARK: - Advanced Customization Section
    
    private var advancedCustomizationSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Advanced Options")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(themeManager.currentTheme.primaryTextColor)
            
            VStack(spacing: 12) {
                AdvancedOption(
                    title: "Dark Mode Override",
                    description: "Force dark or light mode regardless of system setting",
                    icon: "moon.fill",
                    isEnabled: false
                )
                
                AdvancedOption(
                    title: "High Contrast",
                    description: "Increase contrast for better accessibility",
                    icon: "circle.lefthalf.filled",
                    isEnabled: false
                )
                
                AdvancedOption(
                    title: "Reduced Motion",
                    description: "Minimize animations and transitions",
                    icon: "figure.walk.motion",
                    isEnabled: false
                )
                
                AdvancedOption(
                    title: "Custom Fonts",
                    description: "Use system fonts or custom typography",
                    icon: "textformat",
                    isEnabled: false
                )
            }
        }
    }
}

// MARK: - Supporting Views

struct ThemePreviewCard: View {
    let theme: AppTheme
    
    var body: some View {
        VStack(spacing: 16) {
            // Mock app header
            HStack {
                Text("Awaytime")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(theme.primaryTextColor)
                
                Spacer()
                
                Image(systemName: "brain.head.profile")
                    .foregroundColor(theme.accentColor)
                    .font(.title2)
            }
            
            // Mock progress circle
            ZStack {
                Circle()
                    .stroke(theme.accentColor.opacity(0.2), lineWidth: 8)
                    .frame(width: 80, height: 80)
                
                Circle()
                    .trim(from: 0, to: 0.7)
                    .stroke(theme.accentColor, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .frame(width: 80, height: 80)
                    .rotationEffect(.degrees(-90))
                
                Text("70%")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(theme.primaryTextColor)
            }
            
            // Mock stats
            HStack(spacing: 20) {
                VStack(spacing: 4) {
                    Text("2h 15m")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(theme.primaryTextColor)
                    
                    Text("Today")
                        .font(.caption)
                        .foregroundColor(theme.secondaryTextColor)
                }
                
                VStack(spacing: 4) {
                    Text("45m")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(theme.accentColor)
                    
                    Text("Remaining")
                        .font(.caption)
                        .foregroundColor(theme.secondaryTextColor)
                }
            }
            
            // Mock button
            Button(action: {}) {
                Text("View Details")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(theme.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .disabled(true)
        }
        .padding(20)
        .background(theme.cardBackgroundColor)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
    }
}

struct ThemeCard: View {
    let theme: AppTheme
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 12) {
                // Theme colors preview
                HStack(spacing: 4) {
                    Circle()
                        .fill(theme.accentColor)
                        .frame(width: 20, height: 20)
                    
                    Circle()
                        .fill(theme.backgroundColor)
                        .frame(width: 16, height: 16)
                        .overlay(
                            Circle()
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                    
                    Circle()
                        .fill(theme.cardBackgroundColor)
                        .frame(width: 16, height: 16)
                        .overlay(
                            Circle()
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                }
                
                VStack(spacing: 4) {
                    Text(theme.name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    Text(theme.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemBackground))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? theme.accentColor : Color.clear, lineWidth: 2)
                    )
                    .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 2)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct ColorPickerRow: View {
    let title: String
    @Binding var color: Color
    let description: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                ColorPicker("", selection: $color)
                    .labelsHidden()
                    .frame(width: 40, height: 30)
            }
            
            Text(description)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct BackgroundStylePicker: View {
    @Binding var selectedTheme: AppTheme
    
    private let backgroundStyles = ["Light", "Dark", "Auto", "Gradient"]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Background Style")
                .font(.subheadline)
                .fontWeight(.medium)
            
            HStack(spacing: 8) {
                ForEach(backgroundStyles, id: \.self) { style in
                    Button(style) {
                        // Update theme background style
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 6)
                            .fill(style == "Light" ? Color.purple.opacity(0.2) : Color(.systemGray5))
                    )
                    .foregroundColor(style == "Light" ? .purple : .primary)
                    .font(.caption)
                }
            }
        }
    }
}

struct TextStylePicker: View {
    @Binding var selectedTheme: AppTheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Text Style")
                .font(.subheadline)
                .fontWeight(.medium)
            
            VStack(spacing: 8) {
                HStack {
                    Text("Font Size")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    HStack(spacing: 8) {
                        Button("Small") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                        
                        Button("Medium") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.purple.opacity(0.2))
                            .foregroundColor(.purple)
                            .cornerRadius(4)
                        
                        Button("Large") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                    }
                }
                
                HStack {
                    Text("Font Weight")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    HStack(spacing: 8) {
                        Button("Regular") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.purple.opacity(0.2))
                            .foregroundColor(.purple)
                            .cornerRadius(4)
                        
                        Button("Medium") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                        
                        Button("Bold") { }
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                    }
                }
            }
        }
    }
}

struct AdvancedOption: View {
    let title: String
    let description: String
    let icon: String
    let isEnabled: Bool
    
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
                    .lineLimit(2)
            }
            
            Spacer()
            
            Toggle("", isOn: .constant(isEnabled))
                .disabled(true)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.systemGray6))
        )
    }
}

// MARK: - Theme Manager

class ThemeManager: ObservableObject {
    static let shared = ThemeManager()
    
    @Published var currentTheme: AppTheme = .default
    
    private init() {
        loadSavedTheme()
    }
    
    func setTheme(_ theme: AppTheme) {
        currentTheme = theme
        saveTheme(theme)
    }
    
    private func loadSavedTheme() {
        // Load from UserDefaults in production
        currentTheme = .default
    }
    
    private func saveTheme(_ theme: AppTheme) {
        // Save to UserDefaults in production
        print("Theme saved: \(theme.name)")
    }
}

// MARK: - App Theme Model

struct AppTheme {
    let name: String
    let description: String
    let accentColor: Color
    let backgroundColor: Color
    let cardBackgroundColor: Color
    let primaryTextColor: Color
    let secondaryTextColor: Color
    
    static let `default` = AppTheme(
        name: "Default",
        description: "Clean and modern design",
        accentColor: .purple,
        backgroundColor: Color(.systemBackground),
        cardBackgroundColor: Color(.systemBackground),
        primaryTextColor: Color(.label),
        secondaryTextColor: Color(.secondaryLabel)
    )
    
    static let predefinedThemes: [AppTheme] = [
        .default,
        AppTheme(
            name: "Ocean",
            description: "Calm blue tones",
            accentColor: .blue,
            backgroundColor: Color.blue.opacity(0.05),
            cardBackgroundColor: Color(.systemBackground),
            primaryTextColor: Color(.label),
            secondaryTextColor: Color(.secondaryLabel)
        ),
        AppTheme(
            name: "Forest",
            description: "Natural green palette",
            accentColor: .green,
            backgroundColor: Color.green.opacity(0.05),
            cardBackgroundColor: Color(.systemBackground),
            primaryTextColor: Color(.label),
            secondaryTextColor: Color(.secondaryLabel)
        ),
        AppTheme(
            name: "Sunset",
            description: "Warm orange and pink",
            accentColor: .orange,
            backgroundColor: Color.orange.opacity(0.05),
            cardBackgroundColor: Color(.systemBackground),
            primaryTextColor: Color(.label),
            secondaryTextColor: Color(.secondaryLabel)
        ),
        AppTheme(
            name: "Midnight",
            description: "Dark theme with purple accents",
            accentColor: .purple,
            backgroundColor: Color.black,
            cardBackgroundColor: Color(.systemGray6),
            primaryTextColor: .white,
            secondaryTextColor: Color(.systemGray)
        ),
        AppTheme(
            name: "Rose Gold",
            description: "Elegant pink and gold",
            accentColor: .pink,
            backgroundColor: Color.pink.opacity(0.05),
            cardBackgroundColor: Color(.systemBackground),
            primaryTextColor: Color(.label),
            secondaryTextColor: Color(.secondaryLabel)
        )
    ]
    
    static func createCustom(name: String, primaryColor: Color) -> AppTheme {
        return AppTheme(
            name: name,
            description: "Custom theme",
            accentColor: primaryColor,
            backgroundColor: primaryColor.opacity(0.05),
            cardBackgroundColor: Color(.systemBackground),
            primaryTextColor: Color(.label),
            secondaryTextColor: Color(.secondaryLabel)
        )
    }
}

#Preview {
    ThemeCustomizationView()
}