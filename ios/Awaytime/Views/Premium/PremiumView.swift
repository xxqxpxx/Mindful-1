import SwiftUI

struct PremiumView: View {
    @StateObject private var subscriptionService = SubscriptionManager.shared.getService()
    @StateObject private var featureManager = PremiumFeatureManager()
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedPlan: MockProduct?
    @State private var showingPurchaseSuccess = false
    @State private var animateFeatures = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 32) {
                    // Header Section
                    headerSection
                    
                    // Current Status (if subscribed)
                    if subscriptionService.isPremiumActive {
                        currentStatusSection
                    }
                    
                    // Premium Features
                    premiumFeaturesSection
                    
                    // Pricing Plans (if not subscribed)
                    if !subscriptionService.isPremiumActive {
                        pricingPlansSection
                    }
                    
                    // Testimonials/Social Proof
                    socialProofSection
                    
                    // FAQ Section
                    faqSection
                    
                    Spacer(minLength: 20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
            }
            .background(
                LinearGradient(
                    colors: [
                        Color.purple.opacity(0.1),
                        Color.blue.opacity(0.05),
                        Color.clear
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .navigationTitle("Premium")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
        .onAppear {
            Task {
                await subscriptionService.loadProducts()
            }
            
            withAnimation(.easeInOut(duration: 0.8).delay(0.3)) {
                animateFeatures = true
            }
        }
        .alert("Purchase Successful! 🎉", isPresented: $showingPurchaseSuccess) {
            Button("Continue") {
                dismiss()
            }
        } message: {
            Text("Welcome to Awaytime Premium! All features are now unlocked.")
        }
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        VStack(spacing: 20) {
            // Premium Crown Icon
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color.purple, Color.blue],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 100, height: 100)
                    .scaleEffect(animateFeatures ? 1.0 : 0.8)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8), value: animateFeatures)
                
                Image(systemName: "crown.fill")
                    .font(.system(size: 40, weight: .medium))
                    .foregroundColor(.white)
                    .scaleEffect(animateFeatures ? 1.0 : 0.8)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(0.1), value: animateFeatures)
            }
            
            VStack(spacing: 12) {
                Text(subscriptionService.isPremiumActive ? "Premium Active" : "Upgrade to Premium")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .opacity(animateFeatures ? 1.0 : 0.0)
                    .animation(.easeInOut(duration: 0.6).delay(0.2), value: animateFeatures)
                
                Text(subscriptionService.isPremiumActive ? 
                     "Thank you for supporting Awaytime! Enjoy all premium features." :
                     "Unlock advanced features and take full control of your digital wellness journey")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .opacity(animateFeatures ? 1.0 : 0.0)
                    .animation(.easeInOut(duration: 0.6).delay(0.3), value: animateFeatures)
            }
        }
    }
    
    // MARK: - Current Status Section
    
    private var currentStatusSection: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.title2)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Premium Subscription")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(subscriptionService.subscriptionStatus.displayText)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.green.opacity(0.1))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.green.opacity(0.3), lineWidth: 1)
                    )
            )
            
            // Manage Subscription Button
            Button(action: {
                // Mock subscription management - would show real AppStore management in production
                print("Would show subscription management")
            }) {
                HStack {
                    Image(systemName: "gear")
                    Text("Manage Subscription")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(.systemGray6))
                .foregroundColor(.primary)
                .cornerRadius(12)
            }
        }
    }
    
    // MARK: - Premium Features Section
    
    private var premiumFeaturesSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Premium Features")
                .font(.title2)
                .fontWeight(.bold)
                .opacity(animateFeatures ? 1.0 : 0.0)
                .animation(.easeInOut(duration: 0.6).delay(0.4), value: animateFeatures)
            
            LazyVStack(spacing: 16) {
                ForEach(Array(featureManager.getPremiumBenefits().enumerated()), id: \.element.id) { index, benefit in
                    PremiumFeatureCard(
                        benefit: benefit,
                        isUnlocked: subscriptionService.canUseFeature(benefit.feature),
                        animationDelay: Double(index) * 0.1
                    )
                    .opacity(animateFeatures ? 1.0 : 0.0)
                    .offset(y: animateFeatures ? 0 : 20)
                    .animation(.easeOut(duration: 0.6).delay(0.5 + Double(index) * 0.1), value: animateFeatures)
                }
            }
        }
    }
    
    // MARK: - Pricing Plans Section
    
    private var pricingPlansSection: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Choose Your Plan")
                .font(.title2)
                .fontWeight(.bold)
            
            if subscriptionService.isLoading {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                    Text("Loading plans...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(40)
            } else if subscriptionService.availableProducts.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.title)
                        .foregroundColor(.orange)
                    
                    Text("Unable to load pricing plans")
                        .font(.headline)
                    
                    Text("Please check your internet connection and try again.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    
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
                .frame(maxWidth: .infinity)
                .padding(40)
            } else {
                VStack(spacing: 12) {
                    ForEach(subscriptionService.availableProducts, id: \.id) { product in
                        PricingPlanCard(
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
                            
                            Text(subscriptionService.isLoading ? "Processing..." : "Start Premium")
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
                        .shadow(color: Color.purple.opacity(0.3), radius: 8, x: 0, y: 4)
                    }
                    .disabled(subscriptionService.isLoading)
                    .padding(.top, 8)
                }
                
                // Restore Purchases
                Button("Restore Purchases") {
                    Task {
                        await subscriptionService.restorePurchases()
                    }
                }
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding(.top, 8)
            }
            
            // Error Message
            if let errorMessage = subscriptionService.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding(.top, 8)
            }
        }
    }
    
    // MARK: - Social Proof Section
    
    private var socialProofSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("What Users Say")
                .font(.title2)
                .fontWeight(.bold)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(testimonials, id: \.id) { testimonial in
                        TestimonialCard(testimonial: testimonial)
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.horizontal, -20)
        }
    }
    
    // MARK: - FAQ Section
    
    private var faqSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Frequently Asked Questions")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVStack(spacing: 12) {
                ForEach(faqItems, id: \.id) { faq in
                    FAQItem(faq: faq)
                }
            }
        }
    }
}

// MARK: - Premium Feature Card

struct PremiumFeatureCard: View {
    let benefit: PremiumBenefit
    let isUnlocked: Bool
    let animationDelay: Double
    
    @State private var isAnimated = false
    
    var body: some View {
        HStack(spacing: 16) {
            // Feature Icon
            ZStack {
                Circle()
                    .fill(isUnlocked ? benefit.color.opacity(0.2) : Color(.systemGray5))
                    .frame(width: 50, height: 50)
                
                Image(systemName: benefit.icon)
                    .font(.title2)
                    .foregroundColor(isUnlocked ? benefit.color : .secondary)
            }
            .scaleEffect(isAnimated ? 1.0 : 0.8)
            .animation(.spring(response: 0.6, dampingFraction: 0.8).delay(animationDelay), value: isAnimated)
            
            // Feature Content
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(benefit.title)
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(isUnlocked ? .primary : .secondary)
                    
                    Spacer()
                    
                    if isUnlocked {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                            .font(.title3)
                    } else {
                        Image(systemName: "crown.fill")
                            .foregroundColor(.purple)
                            .font(.caption)
                    }
                }
                
                Text(benefit.description)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(isUnlocked ? benefit.color.opacity(0.05) : Color(.systemGray6))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isUnlocked ? benefit.color.opacity(0.2) : Color.clear, lineWidth: 1)
                )
        )
        .onAppear {
            withAnimation {
                isAnimated = true
            }
        }
    }
}

// MARK: - Pricing Plan Card

struct PricingPlanCard: View {
    let product: MockProduct
    let isSelected: Bool
    let savings: String?
    let onSelect: () -> Void
    
    private var monthlyPrice: String {
        if product.isYearly {
            let monthly = product.price / 12
            return String(format: "$%.2f/month", monthly)
        }
        return ""
    }
    
    var body: some View {
        Button(action: onSelect) {
            VStack(spacing: 12) {
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
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Color.green)
                                    .foregroundColor(.white)
                                    .cornerRadius(4)
                            }
                        }
                        
                        Text("Billed \(product.subscriptionPeriod)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(product.localizedPrice)
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                        
                        if product.isYearly {
                            Text(monthlyPrice)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                if product.isYearly {
                    HStack {
                        Image(systemName: "star.fill")
                            .foregroundColor(.yellow)
                            .font(.caption)
                        
                        Text("Most Popular")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                        
                        Spacer()
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

// MARK: - Testimonial Card

struct TestimonialCard: View {
    let testimonial: Testimonial
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                ForEach(0..<5, id: \.self) { index in
                    Image(systemName: index < testimonial.rating ? "star.fill" : "star")
                        .foregroundColor(.yellow)
                        .font(.caption)
                }
            }
            
            Text(testimonial.text)
                .font(.subheadline)
                .lineLimit(4)
            
            HStack {
                Text(testimonial.author)
                    .font(.caption)
                    .fontWeight(.medium)
                
                Spacer()
                
                Text(testimonial.date)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .frame(width: 280)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }
}

// MARK: - FAQ Item

struct FAQItem: View {
    let faq: FAQ
    @State private var isExpanded = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: {
                withAnimation(.easeInOut(duration: 0.3)) {
                    isExpanded.toggle()
                }
            }) {
                HStack {
                    Text(faq.question)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .multilineTextAlignment(.leading)
                    
                    Spacer()
                    
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(16)
            }
            .buttonStyle(PlainButtonStyle())
            
            if isExpanded {
                Text(faq.answer)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }
}

// MARK: - Supporting Models

struct Testimonial: Identifiable {
    let id = UUID()
    let text: String
    let author: String
    let rating: Int
    let date: String
}

struct FAQ: Identifiable {
    let id = UUID()
    let question: String
    let answer: String
}

// MARK: - Sample Data

private let testimonials = [
    Testimonial(
        text: "Awaytime Premium has completely transformed my relationship with my phone. The detailed analytics help me understand my habits better.",
        author: "Sarah M.",
        rating: 5,
        date: "2 days ago"
    ),
    Testimonial(
        text: "The multiple app groups feature is a game-changer. I can set different limits for work and personal apps.",
        author: "Mike R.",
        rating: 5,
        date: "1 week ago"
    ),
    Testimonial(
        text: "Love the custom themes! Finally an app that looks exactly how I want it to. Worth every penny.",
        author: "Emma L.",
        rating: 5,
        date: "2 weeks ago"
    )
]

private let faqItems = [
    FAQ(
        question: "Can I cancel my subscription anytime?",
        answer: "Yes, you can cancel your subscription at any time through your Apple ID settings. Your premium features will remain active until the end of your current billing period."
    ),
    FAQ(
        question: "What happens to my data if I cancel?",
        answer: "Your usage data and settings are preserved. You'll continue to have access to basic features, but premium features will be locked until you resubscribe."
    ),
    FAQ(
        question: "Is there a free trial?",
        answer: "We offer a 7-day free trial for new subscribers. You can explore all premium features risk-free before committing to a subscription."
    ),
    FAQ(
        question: "Can I switch between monthly and yearly plans?",
        answer: "Yes, you can change your subscription plan at any time through your Apple ID settings. Changes will take effect at the start of your next billing cycle."
    ),
    FAQ(
        question: "Do you offer family sharing?",
        answer: "Premium subscriptions support Apple's Family Sharing, allowing up to 6 family members to access premium features with a single subscription."
    )
]

#Preview {
    PremiumView()
}