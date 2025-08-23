import SwiftUI
import StoreKit

@MainActor
class RealSubscriptionService: ObservableObject {
    @Published var isPremiumActive = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var availableProducts: [Product] = []
    @Published var subscriptionStatus: SubscriptionStatus = .notSubscribed
    
    enum SubscriptionStatus {
        case unknown
        case notSubscribed
        case subscribed(Product)
        case expired
        
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
    
    private let productIds = [
        "com.awaytime.premium.monthly",
        "com.awaytime.premium.yearly"
    ]
    
    init() {
        Task {
            await loadProducts()
            await checkSubscriptionStatus()
        }
    }
    
    func loadProducts() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let products = try await Product.products(for: productIds)
            availableProducts = products.sorted { product1, product2 in
                // Sort by price, monthly first
                product1.price < product2.price
            }
            
            print("✅ Loaded \(products.count) subscription products")
            
        } catch {
            errorMessage = "Failed to load products: \(error.localizedDescription)"
            print("❌ Failed to load products: \(error)")
        }
        
        isLoading = false
    }
    
    func purchase(_ product: Product) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            let result = try await product.purchase()
            
            switch result {
            case .success(let verification):
                switch verification {
                case .verified(let transaction):
                    // Transaction is verified, grant premium access
                    isPremiumActive = true
                    subscriptionStatus = .subscribed(product)
                    
                    // Save premium status locally
                    UserDefaults.standard.set(true, forKey: "isPremiumUser")
                    
                    // Finish the transaction
                    await transaction.finish()
                    
                    print("✅ Purchase successful: \(product.displayName)")
                    isLoading = false
                    return true
                    
                case .unverified(_, let error):
                    errorMessage = "Purchase verification failed: \(error.localizedDescription)"
                    print("❌ Purchase verification failed: \(error)")
                }
                
            case .userCancelled:
                print("ℹ️ User cancelled purchase")
                
            case .pending:
                errorMessage = "Purchase is pending approval"
                print("⏳ Purchase pending")
                
            @unknown default:
                errorMessage = "Unknown purchase result"
                print("❓ Unknown purchase result")
            }
            
        } catch {
            errorMessage = "Purchase failed: \(error.localizedDescription)"
            print("❌ Purchase failed: \(error)")
        }
        
        isLoading = false
        return false
    }
    
    func restorePurchases() async {
        isLoading = true
        errorMessage = nil
        
        do {
            try await AppStore.sync()
            await checkSubscriptionStatus()
            
            if isPremiumActive {
                print("✅ Purchases restored successfully")
            } else {
                errorMessage = "No active subscriptions found"
                print("ℹ️ No active subscriptions to restore")
            }
            
        } catch {
            errorMessage = "Failed to restore purchases: \(error.localizedDescription)"
            print("❌ Failed to restore purchases: \(error)")
        }
        
        isLoading = false
    }
    
    private func checkSubscriptionStatus() async {
        // Check for active subscriptions
        for await result in Transaction.currentEntitlements {
            switch result {
            case .verified(let transaction):
                if productIds.contains(transaction.productID) {
                    // Find the corresponding product
                    if let product = availableProducts.first(where: { $0.id == transaction.productID }) {
                        isPremiumActive = true
                        subscriptionStatus = .subscribed(product)
                        
                        // Save premium status locally
                        UserDefaults.standard.set(true, forKey: "isPremiumUser")
                        
                        print("✅ Active subscription found: \(product.displayName)")
                        return
                    }
                }
                
            case .unverified(_, let error):
                print("❌ Unverified transaction: \(error)")
            }
        }
        
        // No active subscription found
        isPremiumActive = false
        subscriptionStatus = .notSubscribed
        UserDefaults.standard.set(false, forKey: "isPremiumUser")
        
        print("ℹ️ No active subscription found")
    }
    
    func getMonthlyProduct() -> Product? {
        return availableProducts.first { $0.id.contains("monthly") }
    }
    
    func getYearlyProduct() -> Product? {
        return availableProducts.first { $0.id.contains("yearly") }
    }
    
    func calculateYearlySavings() -> String? {
        guard let monthly = getMonthlyProduct(),
              let yearly = getYearlyProduct() else {
            return nil
        }
        
        let monthlyYearlyPrice = monthly.price * 12
        let savings = monthlyYearlyPrice - yearly.price
        let percentage = (savings / monthlyYearlyPrice) * 100
        
        return String(format: "%.0f%%", percentage)
    }
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        return isPremiumActive || feature.isFreeFeature
    }
}

// MARK: - Premium Features

enum PremiumFeature: String, CaseIterable {
    case advancedAnalytics = "advanced_analytics"
    case multipleAppGroups = "multiple_app_groups"
    case customThemes = "custom_themes"
    case exportData = "export_data"
    case focusSessions = "focus_sessions"
    case smartCategorization = "smart_categorization"
    
    var displayName: String {
        switch self {
        case .advancedAnalytics:
            return "Advanced Analytics"
        case .multipleAppGroups:
            return "Multiple App Groups"
        case .customThemes:
            return "Custom Themes"
        case .exportData:
            return "Export Data"
        case .focusSessions:
            return "Focus Sessions"
        case .smartCategorization:
            return "Smart Categorization"
        }
    }
    
    var description: String {
        switch self {
        case .advancedAnalytics:
            return "Detailed insights and trends"
        case .multipleAppGroups:
            return "Create unlimited app groups"
        case .customThemes:
            return "Personalize your experience"
        case .exportData:
            return "Export your usage data"
        case .focusSessions:
            return "Timed focus sessions"
        case .smartCategorization:
            return "AI-powered app categorization"
        }
    }
    
    var icon: String {
        switch self {
        case .advancedAnalytics:
            return "chart.bar.fill"
        case .multipleAppGroups:
            return "folder.badge.plus"
        case .customThemes:
            return "paintbrush.pointed"
        case .exportData:
            return "square.and.arrow.up"
        case .focusSessions:
            return "timer"
        case .smartCategorization:
            return "brain.head.profile"
        }
    }
    
    var isFreeFeature: Bool {
        // Define which features are available in free tier
        return false // All features require premium for now
    }
}

// MARK: - Real Subscription Manager

class RealSubscriptionManager {
    static let shared = RealSubscriptionManager()
    
    private init() {}
    
    func getService() -> RealSubscriptionService {
        return RealSubscriptionService()
    }
    
    func isPremiumActive() -> Bool {
        return UserDefaults.standard.bool(forKey: "isPremiumUser")
    }
}