import SwiftUI
import Combine

/// Manager for premium feature access and gating
@MainActor
class PremiumFeatureManager: ObservableObject {
    @Published var isPremiumActive = false
    @Published var showingPaywall = false
    @Published var paywallTriggerFeature: PremiumFeature?
    
    private let subscriptionService = SubscriptionManager.shared.getService()
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupSubscriptionObserver()
    }
    
    private func setupSubscriptionObserver() {
        subscriptionService.$subscriptionStatus
            .map { $0.isActive }
            .assign(to: \.isPremiumActive, on: self)
            .store(in: &cancellables)
    }
    
    // MARK: - Feature Access Control
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        return subscriptionService.canUseFeature(feature)
    }
    
    func requiresPremium(_ feature: PremiumFeature) -> Bool {
        return subscriptionService.requiresPremium(feature)
    }
    
    func requestFeatureAccess(_ feature: PremiumFeature) {
        if canUseFeature(feature) {
            return // Feature is already available
        }
        
        paywallTriggerFeature = feature
        showingPaywall = true
        
        // Track feature request for analytics
        trackFeatureRequest(feature)
    }
    
    func dismissPaywall() {
        showingPaywall = false
        paywallTriggerFeature = nil
    }
    
    // MARK: - Feature Usage Tracking
    
    private func trackFeatureRequest(_ feature: PremiumFeature) {
        // In production, this would send to analytics
        print("📊 Premium feature requested: \(feature.displayName)")
    }
    
    func trackFeatureUsage(_ feature: PremiumFeature) {
        guard canUseFeature(feature) else { return }
        
        // In production, this would send to analytics
        print("📊 Premium feature used: \(feature.displayName)")
    }
    
    // MARK: - Feature Availability
    
    func getAvailableFeatures() -> [PremiumFeature] {
        return PremiumFeature.allCases.filter { canUseFeature($0) }
    }
    
    func getLockedFeatures() -> [PremiumFeature] {
        return PremiumFeature.allCases.filter { requiresPremium($0) }
    }
    
    // MARK: - Premium Benefits
    
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

// MARK: - Premium Benefit Model

struct PremiumBenefit: Identifiable {
    let id = UUID()
    let feature: PremiumFeature
    let title: String
    let description: String
    let icon: String
    let color: Color
}

// MARK: - Feature Gate Modifier

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

// MARK: - Premium Status Banner

// PremiumStatusBanner temporarily disabled to remove duplicate "Upgrade to Premium" button
struct PremiumStatusBanner: View {
    @StateObject private var featureManager = PremiumFeatureManager()
    
    var body: some View {
        // Disabled to prevent duplicate premium upgrade prompts
        EmptyView()
        
        /* Original implementation commented out to remove duplicate
        Group {
            if !featureManager.isPremiumActive {
                Button(action: {
                    featureManager.showingPaywall = true
                }) {
                    HStack {
                        Image(systemName: "crown.fill")
                            .foregroundColor(.yellow)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Upgrade to Premium")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                            
                            Text("Unlock all features")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(12)
                    .background(
                        LinearGradient(
                            colors: [Color.purple.opacity(0.1), Color.purple.opacity(0.05)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.purple.opacity(0.3), lineWidth: 1)
                    )
                }
                .buttonStyle(PlainButtonStyle())
                .sheet(isPresented: $featureManager.showingPaywall) {
                    PaywallView()
                }
            }
        }
        */
    }
}

// MARK: - Premium Feature List

// PremiumFeatureListView temporarily disabled to remove duplicate "Upgrade to Premium" button
struct PremiumFeatureListView: View {
    @StateObject private var featureManager = PremiumFeatureManager()
    
    var body: some View {
        // Disabled to prevent duplicate premium upgrade prompts
        EmptyView()
        
        /* Original implementation commented out to remove duplicate
        VStack(alignment: .leading, spacing: 16) {
            Text("Premium Features")
                .font(.headline)
                .fontWeight(.semibold)
            
            LazyVStack(spacing: 12) {
                ForEach(featureManager.getPremiumBenefits()) { benefit in
                    PremiumFeatureRow(
                        benefit: benefit,
                        isUnlocked: featureManager.canUseFeature(benefit.feature)
                    )
                }
            }
            
            if !featureManager.isPremiumActive {
                Button(action: {
                    featureManager.showingPaywall = true
                }) {
                    HStack {
                        Image(systemName: "crown.fill")
                        Text("Unlock All Features")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .padding(.top, 8)
            }
        }
        .sheet(isPresented: $featureManager.showingPaywall) {
            PaywallView()
        }
        */
    }
}

struct PremiumFeatureRow: View {
    let benefit: PremiumBenefit
    let isUnlocked: Bool
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: benefit.icon)
                .font(.title2)
                .foregroundColor(isUnlocked ? benefit.color : .secondary)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(benefit.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(isUnlocked ? .primary : .secondary)
                
                Text(benefit.description)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
            
            Spacer()
            
            if isUnlocked {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(isUnlocked ? benefit.color.opacity(0.1) : Color(.systemGray6))
        )
    }
}

// MARK: - Usage Examples

struct ExampleUsage: View {
    var body: some View {
        VStack(spacing: 20) {
            // Example 1: Gated content with fallback
            Text("Multiple App Groups")
                .requiresPremium(.multipleAppGroups) {
                    AnyView(
                        Text("Single App Group (Free)")
                            .foregroundColor(.secondary)
                    )
                }
            
            // Example 2: Gated content without fallback (shows premium gate)
            VStack {
                Text("Advanced Analytics")
                // This content will be replaced with premium gate if not subscribed
            }
            .requiresPremium(.advancedAnalytics)
            
            // Example 3: Premium status banner
            PremiumStatusBanner()
            
            // Example 4: Feature list
            PremiumFeatureListView()
        }
        .padding()
    }
}

#Preview {
    ExampleUsage()
}