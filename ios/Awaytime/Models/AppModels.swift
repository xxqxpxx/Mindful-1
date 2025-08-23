import SwiftUI

// MARK: - App Info Model

struct AppInfo: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let bundleId: String
    let icon: String
    let color: Color
    
    init(name: String, bundleId: String, icon: String, color: Color = .blue) {
        self.name = name
        self.bundleId = bundleId
        self.icon = icon
        self.color = color
    }
}

// MARK: - App Group Model

struct AppGroup: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    let color: Color
    let apps: [AppInfo]
    let dailyLimitMinutes: Int?
    let isDefault: Bool
    
    // Computed properties for demo
    var hasActiveLimit: Bool {
        dailyLimitMinutes != nil
    }
    
    var dailyLimit: String {
        guard let minutes = dailyLimitMinutes else { return "No limit" }
        let hours = minutes / 60
        let mins = minutes % 60
        return hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"
    }
    
    var todayUsage: String {
        let coreDataManager = CoreDataManager.shared
        let usageMinutes = coreDataManager.getTodayUsage(for: name)
        let hours = usageMinutes / 60
        let minutes = usageMinutes % 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
    
    var usageProgress: Double {
        let coreDataManager = CoreDataManager.shared
        let usageMinutes = coreDataManager.getTodayUsage(for: name)
        let appGroups = coreDataManager.fetchAppGroups()
        
        guard let activeGroup = appGroups.first(where: { $0.isActive }),
              activeGroup.dailyLimitMinutes > 0 else {
            return 0.0
        }
        
        return min(Double(usageMinutes) / Double(activeGroup.dailyLimitMinutes), 1.0)
    }
    
    var timeRemaining: String {
        let coreDataManager = CoreDataManager.shared
        let usageMinutes = coreDataManager.getTodayUsage(for: name)
        let appGroups = coreDataManager.fetchAppGroups()
        
        guard let activeGroup = appGroups.first(where: { $0.isActive }) else {
            return "No limit set"
        }
        
        let remainingMinutes = max(0, Int(activeGroup.dailyLimitMinutes) - usageMinutes)
        let hours = remainingMinutes / 60
        let minutes = remainingMinutes % 60
        
        if remainingMinutes <= 0 {
            return "Time's up!"
        } else if hours > 0 {
            return "\(hours)h \(minutes)m left"
        } else {
            return "\(minutes)m left"
        }
    }
}

// MARK: - Premium Feature Model

enum PremiumFeature: String, CaseIterable {
    case multipleAppGroups = "multiple_app_groups"
    case advancedAnalytics = "advanced_analytics"
    case customThemes = "custom_themes"
    case exportData = "export_data"
    
    // Free features for reference
    case basicUsageTracking = "basic_usage_tracking"
    case singleAppGroup = "single_app_group"
    case basicNotifications = "basic_notifications"
    
    var displayName: String {
        switch self {
        case .multipleAppGroups:
            return "Multiple App Groups"
        case .advancedAnalytics:
            return "Advanced Analytics"
        case .customThemes:
            return "Custom Themes"
        case .exportData:
            return "Export Data"
        case .basicUsageTracking:
            return "Basic Usage Tracking"
        case .singleAppGroup:
            return "Single App Group"
        case .basicNotifications:
            return "Basic Notifications"
        }
    }
    
    var description: String {
        switch self {
        case .multipleAppGroups:
            return "Create separate groups for work, social, entertainment, and more"
        case .advancedAnalytics:
            return "Detailed insights, trends, and personalized recommendations"
        case .customThemes:
            return "Personalize your experience with custom colors and themes"
        case .exportData:
            return "Export your usage data and progress reports"
        case .basicUsageTracking:
            return "Track your daily app usage and set limits"
        case .singleAppGroup:
            return "Manage one group of apps with time limits"
        case .basicNotifications:
            return "Get notified when approaching your limits"
        }
    }
    
    var icon: String {
        switch self {
        case .multipleAppGroups:
            return "folder.badge.plus"
        case .advancedAnalytics:
            return "chart.line.uptrend.xyaxis"
        case .customThemes:
            return "paintbrush.pointed"
        case .exportData:
            return "square.and.arrow.up"
        case .basicUsageTracking:
            return "clock"
        case .singleAppGroup:
            return "folder"
        case .basicNotifications:
            return "bell"
        }
    }
}

// MARK: - Premium Benefit Model

struct PremiumBenefit: Identifiable {
    let id = UUID()
    let feature: PremiumFeature
    let title: String
    let description: String
    let icon: String
    let color: Color
}

// MARK: - Mock Subscription Manager

class SubscriptionManager {
    static let shared = SubscriptionManager()
    
    private init() {}
    
    func getService() -> MockSubscriptionService {
        return MockSubscriptionService()
    }
    
    func isPremiumActive() -> Bool {
        return false // Mock implementation
    }
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        switch feature {
        case .basicUsageTracking, .singleAppGroup, .basicNotifications:
            return true // Free features
        default:
            return isPremiumActive() // Premium features
        }
    }
    
    func requiresPremium(_ feature: PremiumFeature) -> Bool {
        return !canUseFeature(feature)
    }
}

// MARK: - Mock Subscription Service

class MockSubscriptionService: ObservableObject {
    @Published var isPremiumActive = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var availableProducts: [MockProduct] = []
    @Published var subscriptionStatus: MockSubscriptionStatus = .notSubscribed
    
    enum MockSubscriptionStatus {
        case unknown
        case notSubscribed
        case subscribed(MockProduct)
        case expired
        
        var isActive: Bool {
            switch self {
            case .subscribed:
                return true
            default:
                return false
            }
        }
        
        var displayText: String {
            switch self {
            case .unknown:
                return "Checking subscription..."
            case .notSubscribed:
                return "Not subscribed"
            case .subscribed(let product):
                return "Subscribed to \(product.displayName)"
            case .expired:
                return "Subscription expired"
            }
        }
    }
    
    init() {
        // Mock products
        availableProducts = [
            MockProduct(id: "monthly", displayName: "Monthly Premium", price: 4.99, isYearly: false),
            MockProduct(id: "yearly", displayName: "Yearly Premium", price: 39.99, isYearly: true)
        ]
    }
    
    func loadProducts() async {
        // Mock implementation
        isLoading = true
        try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
        isLoading = false
    }
    
    func purchase(_ product: MockProduct) async -> Bool {
        // Mock implementation
        isLoading = true
        try? await Task.sleep(nanoseconds: 2_000_000_000) // 2 seconds
        isLoading = false
        
        // Simulate successful purchase
        isPremiumActive = true
        subscriptionStatus = .subscribed(product)
        return true
    }
    
    func restorePurchases() async {
        // Mock implementation
        isLoading = true
        try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
        isLoading = false
    }
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        return SubscriptionManager.shared.canUseFeature(feature)
    }
    
    func getMonthlyProduct() -> MockProduct? {
        return availableProducts.first { !$0.isYearly }
    }
    
    func getYearlyProduct() -> MockProduct? {
        return availableProducts.first { $0.isYearly }
    }
    
    func getYearlySavings() -> String? {
        guard let monthly = getMonthlyProduct(),
              let yearly = getYearlyProduct() else {
            return nil
        }
        
        let monthlyYearlyPrice = monthly.price * 12
        let savings = monthlyYearlyPrice - yearly.price
        let percentage = (savings / monthlyYearlyPrice) * 100
        
        return String(format: "%.0f%%", percentage)
    }
}

// MARK: - Mock Product

struct MockProduct: Identifiable {
    let id: String
    let displayName: String
    let price: Double
    let isYearly: Bool
    
    var localizedPrice: String {
        return String(format: "$%.2f", price)
    }
    
    var subscriptionPeriod: String {
        return isYearly ? "year" : "month"
    }
    
    var isMonthly: Bool {
        return !isYearly
    }
}

// MARK: - Mock Premium Feature Manager

class PremiumFeatureManager: ObservableObject {
    @Published var isPremiumActive = false
    @Published var showingPaywall = false
    @Published var paywallTriggerFeature: PremiumFeature?
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        return SubscriptionManager.shared.canUseFeature(feature)
    }
    
    func requiresPremium(_ feature: PremiumFeature) -> Bool {
        return SubscriptionManager.shared.requiresPremium(feature)
    }
    
    func requestFeatureAccess(_ feature: PremiumFeature) {
        if canUseFeature(feature) {
            return
        }
        
        paywallTriggerFeature = feature
        showingPaywall = true
    }
    
    func dismissPaywall() {
        showingPaywall = false
        paywallTriggerFeature = nil
    }
    
    func trackFeatureUsage(_ feature: PremiumFeature) {
        guard canUseFeature(feature) else { return }
        print("📊 Premium feature used: \(feature.displayName)")
    }
    
    func getPremiumBenefits() -> [PremiumBenefit] {
        return [
            PremiumBenefit(
                feature: .multipleAppGroups,
                title: "Organize Your Apps",
                description: "Create separate groups for work, social, and entertainment apps with individual limits",
                icon: "folder.badge.plus",
                color: .blue
            ),
            PremiumBenefit(
                feature: .advancedAnalytics,
                title: "Detailed Insights",
                description: "Get weekly reports, usage trends, and personalized recommendations",
                icon: "chart.line.uptrend.xyaxis",
                color: .green
            ),
            PremiumBenefit(
                feature: .customThemes,
                title: "Personal Style",
                description: "Customize colors, themes, and visual elements to match your preferences",
                icon: "paintbrush.pointed",
                color: .purple
            ),
            PremiumBenefit(
                feature: .exportData,
                title: "Export Your Data",
                description: "Download your usage data and progress reports in CSV or PDF format",
                icon: "square.and.arrow.up",
                color: .orange
            )
        ]
    }
}

// MARK: - Premium Feature Gate Modifier

struct PremiumFeatureGateModifier: ViewModifier {
    let feature: PremiumFeature
    let fallbackContent: (() -> AnyView)?
    
    @StateObject private var featureManager = PremiumFeatureManager()
    
    init(feature: PremiumFeature, fallbackContent: (() -> AnyView)? = nil) {
        self.feature = feature
        self.fallbackContent = fallbackContent
    }
    
    func body(content: Content) -> some View {
        Group {
            if featureManager.canUseFeature(feature) {
                content
                    .onAppear {
                        featureManager.trackFeatureUsage(feature)
                    }
            } else if let fallback = fallbackContent {
                fallback()
            } else {
                PremiumFeatureLockedView(feature: feature) {
                    featureManager.requestFeatureAccess(feature)
                }
            }
        }
        .sheet(isPresented: $featureManager.showingPaywall) {
            PaywallView(triggeredByFeature: featureManager.paywallTriggerFeature)
        }
    }
}

extension View {
    func requiresPremium(_ feature: PremiumFeature, fallback: (() -> AnyView)? = nil) -> some View {
        modifier(PremiumFeatureGateModifier(feature: feature, fallbackContent: fallback))
    }
}

// MARK: - Premium Feature Locked View

struct PremiumFeatureLockedView: View {
    let feature: PremiumFeature
    let onUpgrade: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "crown.fill")
                .font(.system(size: 40))
                .foregroundColor(.purple)
            
            VStack(spacing: 8) {
                Text("Premium Feature")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Text(feature.displayName)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.purple)
                
                Text(feature.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
            
            Button(action: onUpgrade) {
                HStack {
                    Image(systemName: "crown.fill")
                    Text("Upgrade to Premium")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.purple)
                .foregroundColor(.white)
                .cornerRadius(12)
            }
            .padding(.horizontal)
        }
        .padding(24)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.purple.opacity(0.1))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.purple.opacity(0.3), lineWidth: 1)
                )
        )
        .padding()
    }
}