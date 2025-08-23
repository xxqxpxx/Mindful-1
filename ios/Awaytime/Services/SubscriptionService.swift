import StoreKit
import SwiftUI
import Combine

/// Subscription service using StoreKit 2 for premium features
@MainActor
class SubscriptionService: ObservableObject {
    @Published var subscriptionStatus: SubscriptionStatus = .unknown
    @Published var availableProducts: [Product] = []
    @Published var purchasedProducts: [Product] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private var updateListenerTask: Task<Void, Error>?
    private let productIds = [
        "com.awaytime.premium.monthly",
        "com.awaytime.premium.yearly"
    ]
    
    enum SubscriptionStatus {
        case unknown
        case notSubscribed
        case subscribed(Product)
        case expired
        case inGracePeriod
        case inBillingRetryPeriod
        
        var isActive: Bool {
            switch self {
            case .subscribed, .inGracePeriod, .inBillingRetryPeriod:
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
            case .inGracePeriod:
                return "Grace period active"
            case .inBillingRetryPeriod:
                return "Billing retry in progress"
            }
        }
    }
    
    init() {
        // Start listening for transaction updates
        updateListenerTask = listenForTransactions()
        
        // Load products and check subscription status
        Task {
            await loadProducts()
            await updateSubscriptionStatus()
        }
    }
    
    deinit {
        updateListenerTask?.cancel()
    }
    
    // MARK: - Product Loading
    
    func loadProducts() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let products = try await Product.products(for: productIds)
            
            // Sort products by price (monthly first, then yearly)
            availableProducts = products.sorted { product1, product2 in
                if product1.id.contains("monthly") && product2.id.contains("yearly") {
                    return true
                } else if product1.id.contains("yearly") && product2.id.contains("monthly") {
                    return false
                } else {
                    return product1.price < product2.price
                }
            }
            
            print("✅ Loaded \(availableProducts.count) products")
        } catch {
            errorMessage = "Failed to load products: \(error.localizedDescription)"
            print("❌ Failed to load products: \(error)")
        }
        
        isLoading = false
    }
    
    // MARK: - Purchase Handling
    
    func purchase(_ product: Product) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            let result = try await product.purchase()
            
            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                
                // Update subscription status
                await updateSubscriptionStatus()
                
                // Finish the transaction
                await transaction.finish()
                
                print("✅ Purchase successful: \(product.displayName)")
                return true
                
            case .userCancelled:
                print("ℹ️ User cancelled purchase")
                return false
                
            case .pending:
                print("⏳ Purchase pending approval")
                return false
                
            @unknown default:
                print("❌ Unknown purchase result")
                return false
            }
        } catch {
            errorMessage = "Purchase failed: \(error.localizedDescription)"
            print("❌ Purchase failed: \(error)")
            return false
        }
        
        isLoading = false
    }
    
    // MARK: - Restore Purchases
    
    func restorePurchases() async {
        isLoading = true
        errorMessage = nil
        
        do {
            try await AppStore.sync()
            await updateSubscriptionStatus()
            print("✅ Purchases restored")
        } catch {
            errorMessage = "Failed to restore purchases: \(error.localizedDescription)"
            print("❌ Failed to restore purchases: \(error)")
        }
        
        isLoading = false
    }
    
    // MARK: - Subscription Status
    
    func updateSubscriptionStatus() async {
        var activeSubscription: Product?
        
        // Check for active subscriptions
        for await result in Transaction.currentEntitlements {
            do {
                let transaction = try checkVerified(result)
                
                // Find the product for this transaction
                if let product = availableProducts.first(where: { $0.id == transaction.productID }) {
                    activeSubscription = product
                    break
                }
            } catch {
                print("❌ Failed to verify transaction: \(error)")
            }
        }
        
        // Update subscription status
        if let subscription = activeSubscription {
            subscriptionStatus = .subscribed(subscription)
            
            // Check if we need to update purchased products
            if !purchasedProducts.contains(subscription) {
                purchasedProducts.append(subscription)
            }
        } else {
            subscriptionStatus = .notSubscribed
            purchasedProducts.removeAll()
        }
        
        print("📱 Subscription status updated: \(subscriptionStatus.displayText)")
    }
    
    // MARK: - Transaction Listening
    
    private func listenForTransactions() -> Task<Void, Error> {
        return Task.detached {
            // Listen for transaction updates
            for await result in Transaction.updates {
                do {
                    let transaction = try self.checkVerified(result)
                    
                    // Update subscription status on main actor
                    await MainActor.run {
                        Task {
                            await self.updateSubscriptionStatus()
                        }
                    }
                    
                    // Finish the transaction
                    await transaction.finish()
                } catch {
                    print("❌ Transaction update failed: \(error)")
                }
            }
        }
    }
    
    // MARK: - Verification
    
    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw SubscriptionError.failedVerification
        case .verified(let safe):
            return safe
        }
    }
    
    // MARK: - Premium Feature Checks
    
    var isPremiumActive: Bool {
        return subscriptionStatus.isActive
    }
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        switch feature {
        case .multipleAppGroups, .advancedAnalytics, .customThemes, .exportData:
            return isPremiumActive
        case .basicUsageTracking, .singleAppGroup, .basicNotifications:
            return true // Free features
        }
    }
    
    func requiresPremium(_ feature: PremiumFeature) -> Bool {
        return !canUseFeature(feature)
    }
    
    // MARK: - Product Information
    
    func getMonthlyProduct() -> Product? {
        return availableProducts.first { $0.id.contains("monthly") }
    }
    
    func getYearlyProduct() -> Product? {
        return availableProducts.first { $0.id.contains("yearly") }
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
    
    // MARK: - Error Handling
    
    func clearError() {
        errorMessage = nil
    }
}

// MARK: - Premium Features

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

// MARK: - Subscription Errors

enum SubscriptionError: LocalizedError {
    case failedVerification
    case productNotFound
    case purchaseFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .failedVerification:
            return "Failed to verify purchase"
        case .productNotFound:
            return "Product not found"
        case .purchaseFailed(let message):
            return "Purchase failed: \(message)"
        }
    }
}

// MARK: - Product Extensions

extension Product {
    var localizedPrice: String {
        return priceFormatStyle.format(price)
    }
    
    var subscriptionPeriod: String {
        guard let subscription = subscription else { return "" }
        
        let unit = subscription.subscriptionPeriod.unit
        let value = subscription.subscriptionPeriod.value
        
        switch unit {
        case .day:
            return value == 1 ? "day" : "\(value) days"
        case .week:
            return value == 1 ? "week" : "\(value) weeks"
        case .month:
            return value == 1 ? "month" : "\(value) months"
        case .year:
            return value == 1 ? "year" : "\(value) years"
        @unknown default:
            return "period"
        }
    }
    
    var isMonthly: Bool {
        return id.contains("monthly")
    }
    
    var isYearly: Bool {
        return id.contains("yearly")
    }
}

// MARK: - Global Subscription Manager

class SubscriptionManager {
    static let shared = SubscriptionManager()
    private let subscriptionService = SubscriptionService()
    
    private init() {}
    
    func getService() -> SubscriptionService {
        return subscriptionService
    }
    
    func isPremiumActive() -> Bool {
        return subscriptionService.isPremiumActive
    }
    
    func canUseFeature(_ feature: PremiumFeature) -> Bool {
        return subscriptionService.canUseFeature(feature)
    }
    
    func requiresPremium(_ feature: PremiumFeature) -> Bool {
        return subscriptionService.requiresPremium(feature)
    }
}