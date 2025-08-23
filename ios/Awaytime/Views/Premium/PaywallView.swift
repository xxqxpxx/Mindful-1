import SwiftUI

struct PaywallView: View {
    let triggeredByFeature: PremiumFeature?
    
    @StateObject private var subscriptionService = SubscriptionManager.shared.getService()
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedPlan: MockProduct?
    @State private var showingPurchaseSuccess = false
    @State private var currentFeatureIndex = 0
    
    init(triggeredByFeature: PremiumFeature? = nil) {
        self.triggeredByFeature = triggeredByFeature
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 32) {
                    // Header with triggered feature context
                    headerSection
                    
                    // Feature showcase
                    featureShowcaseSection
                    
                    // Pricing plans
                    pricingSection
                    
                    // Trust indicators
                    trustIndicatorsSection
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(
                LinearGradient(
                    colors: [
                        Color.purple.opacity(0.15),
                        Color.blue.opacity(0.1),
                        Color.clear
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            Task {
                await subscriptionService.loadProducts()
            }
            
            // Auto-select yearly plan by default
            if let yearlyProduct = subscriptionService.getYearlyProduct() {
                selectedPlan = yearlyProduct
            }
            
            // Start feature carousel
            startFeatureCarousel()
        }
        .alert("Welcome to Premium! 🎉", isPresented: $showingPurchaseSuccess) {
            Button("Get Started") {
                dismiss()
            }
        } message: {
            Text("All premium features are now unlocked. Enjoy your enhanced Awaytime experience!")
        }
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        VStack(spacing: 20) {
            // Premium Crown with glow effect
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [
                                Color.purple.opacity(0.3),
                                Color.purple.opacity(0.1),
                                Color.clear
                            ],
                            center: .center,
                            startRadius: 30,
                            endRadius: 60
                        )
                    )
                    .frame(width: 120, height: 120)
                
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color.purple, Color.blue],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 80, height: 80)
                
                Image(systemName: "crown.fill")
                    .font(.system(size: 32, weight: .medium))
                    .foregroundColor(.white)
            }
            
            VStack(spacing: 12) {
                if let feature = triggeredByFeature {
                    Text("Unlock \(feature.displayName)")
                        .font(.title)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text(feature.description)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                } else {
                    Text("Upgrade to Premium")
                        .font(.title)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("Unlock all features and take full control of your digital wellness")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
        }
    }
    
    // MARK: - Feature Showcase Section
    
    private var featureShowcaseSection: some View {
        VStack(spacing: 20) {
            Text("What You'll Get")
                .font(.title2)
                .fontWeight(.bold)
            
            // Animated feature carousel
            TabView(selection: $currentFeatureIndex) {
                ForEach(Array(PremiumFeatureManager().getPremiumBenefits().enumerated()), id: \.element.id) { index, benefit in
                    PaywallFeatureCard(benefit: benefit)
                        .tag(index)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .automatic))
            .frame(height: 200)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
            )
        }
    }
    
    // MARK: - Pricing Section
    
    private var pricingSection: some View {
        VStack(spacing: 20) {
            Text("Choose Your Plan")
                .font(.title2)
                .fontWeight(.bold)
            
            if subscriptionService.isLoading {
                ProgressView("Loading plans...")
                    .frame(maxWidth: .infinity)
                    .padding(40)
            } else if subscriptionService.availableProducts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "wifi.slash")
                        .font(.title)
                        .foregroundColor(.secondary)
                    
                    Text("Unable to load plans")
                        .font(.headline)
                    
                    Button("Retry") {
                        Task {
                            await subscriptionService.loadProducts()
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .padding(40)
            } else {
                VStack(spacing: 12) {
                    ForEach(subscriptionService.availableProducts, id: \.id) { product in
                        PaywallPricingCard(
                            product: product,
                            isSelected: selectedPlan?.id == product.id,
                            savings: product.isYearly ? subscriptionService.getYearlySavings() : nil,
                            onSelect: {
                                selectedPlan = product
                            }
                        )
                    }
                }
                
                // Purchase Button
                if let selectedPlan = selectedPlan {
                    Button(action: {
                        Task {
                            let success = await subscriptionService.purchase(selectedPlan)
                            if success {
                                showingPurchaseSuccess = true
                            }
                        }
                    }) {
                        HStack {
                            if subscriptionService.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            } else {
                                Image(systemName: "crown.fill")
                            }
                            
                            Text(subscriptionService.isLoading ? "Processing..." : "Start Free Trial")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            LinearGradient(
                                colors: [Color.purple, Color.blue],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .foregroundColor(.white)
                        .cornerRadius(12)
                        .shadow(color: Color.purple.opacity(0.4), radius: 12, x: 0, y: 6)
                    }
                    .disabled(subscriptionService.isLoading)
                    .scaleEffect(subscriptionService.isLoading ? 0.98 : 1.0)
                    .animation(.easeInOut(duration: 0.1), value: subscriptionService.isLoading)
                }
                
                // Trial info
                Text("7-day free trial • Cancel anytime")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
                
                // Restore purchases
                Button("Restore Purchases") {
                    Task {
                        await subscriptionService.restorePurchases()
                    }
                }
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding(.top, 4)
            }
            
            // Error message
            if let errorMessage = subscriptionService.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
            }
        }
    }
    
    // MARK: - Trust Indicators Section
    
    private var trustIndicatorsSection: some View {
        VStack(spacing: 16) {
            HStack(spacing: 24) {
                TrustIndicator(
                    icon: "lock.shield.fill",
                    text: "Secure Payment",
                    color: .green
                )
                
                TrustIndicator(
                    icon: "arrow.clockwise",
                    text: "Cancel Anytime",
                    color: .blue
                )
                
                TrustIndicator(
                    icon: "person.2.fill",
                    text: "Family Sharing",
                    color: .purple
                )
            }
            
            Text("Trusted by thousands of users worldwide")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Helper Methods
    
    private func startFeatureCarousel() {
        Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { _ in
            withAnimation(.easeInOut(duration: 0.5)) {
                let benefitsCount = PremiumFeatureManager().getPremiumBenefits().count
                currentFeatureIndex = (currentFeatureIndex + 1) % benefitsCount
            }
        }
    }
}

// MARK: - Paywall Feature Card

struct PaywallFeatureCard: View {
    let benefit: PremiumBenefit
    
    var body: some View {
        VStack(spacing: 16) {
            // Feature icon with background
            ZStack {
                Circle()
                    .fill(benefit.color.opacity(0.2))
                    .frame(width: 60, height: 60)
                
                Image(systemName: benefit.icon)
                    .font(.title2)
                    .foregroundColor(benefit.color)
            }
            
            VStack(spacing: 8) {
                Text(benefit.title)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .multilineTextAlignment(.center)
                
                Text(benefit.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Paywall Pricing Card

struct PaywallPricingCard: View {
    let product: MockProduct
    let isSelected: Bool
    let savings: String?
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(product.displayName)
                            .font(.headline)
                            .fontWeight(.semibold)
                        
                        if let savings = savings {
                            Text("Save \(savings)")
                                .font(.caption)
                                .fontWeight(.medium)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }
                        
                        if product.isYearly {
                            Text("POPULAR")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.purple)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }
                    }
                    
                    Text("7-day free trial, then \(product.localizedPrice)/\(product.subscriptionPeriod)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Selection indicator
                ZStack {
                    Circle()
                        .stroke(isSelected ? Color.purple : Color.gray, lineWidth: 2)
                        .frame(width: 24, height: 24)
                    
                    if isSelected {
                        Circle()
                            .fill(Color.purple)
                            .frame(width: 12, height: 12)
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isSelected ? Color.purple.opacity(0.1) : Color(.systemGray6))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? Color.purple : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Trust Indicator

struct TrustIndicator: View {
    let icon: String
    let text: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            
            Text(text)
                .font(.caption)
                .fontWeight(.medium)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    PaywallView()
}

#Preview("With Feature Context") {
    PaywallView(triggeredByFeature: .advancedAnalytics)
}