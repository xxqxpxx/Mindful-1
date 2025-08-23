import SwiftUI

struct OneTimeOfferView: View {
    @StateObject private var viewModel = OneTimeOfferViewModel()
    @Environment(\.dismiss) private var dismiss
    let onPurchase: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Close button
                HStack {
                    Spacer()
                    Button(action: {
                        onDismiss()
                        dismiss()
                    }) {
                        Image(systemName: "xmark")
                            .font(.title2)
                            .foregroundColor(.secondary)
                            .padding()
                    }
                }
                .padding(.top, 10)
                
                Spacer()
                
                // Brain mascot
                BrainMascotView(state: .encouraging, size: 100)
                    .padding(.bottom, 20)
                
                // Offer title
                VStack(spacing: 12) {
                    Text("One-time offer")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("You will never see this again")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 30)
                
                // Pricing card
                VStack(spacing: 16) {
                    // Discount badge
                    HStack {
                        Spacer()
                        Text("67% off")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 20)
                                    .fill(Color.orange)
                            )
                        Spacer()
                    }
                    
                    // Pricing
                    VStack(spacing: 8) {
                        // Original price (crossed out)
                        Text("EGP 1,499.99")
                            .font(.title2)
                            .foregroundColor(.secondary)
                            .strikethrough()
                        
                        // Discounted price
                        Text("EGP 41.67/mo")
                            .font(.system(size: 36, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        Text("Lowest price ever")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 20)
                    .frame(maxWidth: .infinity)
                    .background(
                        RoundedRectangle(cornerRadius: 16)
                            .fill(Color(UIColor.secondarySystemBackground))
                    )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 30)
                
                // Features
                VStack(spacing: 16) {
                    OfferFeatureRow(
                        icon: "chart.bar.fill",
                        title: "Advanced Analytics",
                        description: "Detailed insights into your habits"
                    )
                    
                    OfferFeatureRow(
                        icon: "target",
                        title: "Smart Goals",
                        description: "AI-powered recommendations"
                    )
                    
                    OfferFeatureRow(
                        icon: "brain.head.profile",
                        title: "Brain Health Tracking",
                        description: "Monitor your cognitive wellness"
                    )
                    
                    OfferFeatureRow(
                        icon: "bell.fill",
                        title: "Custom Notifications",
                        description: "Personalized reminders & motivation"
                    )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 30)
                
                // No commitment message
                HStack {
                    Image(systemName: "checkmark")
                        .foregroundColor(.green)
                        .font(.title3)
                    
                    Text("No commitment, cancel anytime")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.bottom, 20)
                
                Spacer()
                
                // CTA button
                Button(action: {
                    viewModel.purchasePremium()
                    onPurchase()
                }) {
                    Text("Claim my limited time offer")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 28)
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(colors: [Color.blue, Color.purple]),
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                        )
                }
                .padding(.horizontal, 32)
                
                // Fine print
                Text("Billed yearly at EGP 499.99 per year")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 12)
                    .padding(.bottom, 40)
            }
        }
    }
}

struct OfferFeatureRow: View {
    let icon: String
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.blue)
                .frame(width: 24, height: 24)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}

class OneTimeOfferViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var offerExpired = false
    
    func purchasePremium() {
        isLoading = true
        
        // Simulate purchase process
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.isLoading = false
            // Handle purchase completion
            print("💳 Premium purchase initiated")
        }
    }
    
    func markOfferAsShown() {
        UserDefaults.standard.set(true, forKey: "oneTimeOfferShown")
        UserDefaults.standard.set(Date(), forKey: "oneTimeOfferShownDate")
    }
    
    static func shouldShowOffer() -> Bool {
        let hasShown = UserDefaults.standard.bool(forKey: "oneTimeOfferShown")
        let isPremium = UserDefaults.standard.bool(forKey: "isPremium")
        return !hasShown && !isPremium
    }
}

// MARK: - Specialized One-Time Offer Views

struct PostOnboardingOfferView: View {
    let onPurchase: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        OneTimeOfferView(
            onPurchase: onPurchase,
            onDismiss: onDismiss
        )
    }
}

struct FirstRuleOfferView: View {
    let onPurchase: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 30) {
                // Brain celebrating
                BrainMascotView(state: .celebrating, size: 120)
                
                VStack(spacing: 16) {
                    Text("Great start!")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text("You created your first rule. Want to unlock the full potential of your brain health journey?")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                
                // Special offer card
                VStack(spacing: 16) {
                    Text("Special Launch Offer")
                        .font(.headline)
                        .fontWeight(.bold)
                    
                    Text("67% OFF")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.orange)
                    
                    Text("Just EGP 41.67/month")
                        .font(.title3)
                        .fontWeight(.semibold)
                }
                .padding(.vertical, 24)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color.orange.opacity(0.1))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.orange.opacity(0.3), lineWidth: 2)
                        )
                )
                .padding(.horizontal, 32)
                
                VStack(spacing: 20) {
                    Button(action: onPurchase) {
                        Text("Unlock Premium Features")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(
                                RoundedRectangle(cornerRadius: 26)
                                    .fill(Color.orange)
                            )
                    }
                    
                    Button(action: onDismiss) {
                        Text("Maybe Later")
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 32)
            }
        }
    }
}

struct OneTimeOfferView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            OneTimeOfferView(onPurchase: {}, onDismiss: {})
            
            FirstRuleOfferView(onPurchase: {}, onDismiss: {})
        }
    }
}