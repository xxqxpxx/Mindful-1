import Foundation
import FirebaseAnalytics
import FirebaseCrashlytics

// MARK: - Analytics Manager for iOS
class AnalyticsManager: ObservableObject {
    static let shared = AnalyticsManager()
    
    private init() {}
    
    // MARK: - Analytics Events
    
    func logEvent(_ eventName: String, parameters: [String: Any]? = nil) {
        Analytics.logEvent(eventName, parameters: parameters)
        print("📊 Analytics Event: \(eventName) with parameters: \(parameters ?? [:])")
    }
    
    func logScreenView(_ screenName: String, screenClass: String? = nil) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenClass ?? screenName
        ])
        print("📱 Screen View: \(screenName)")
    }
    
    func logAppUsage(appName: String, duration: TimeInterval, category: String) {
        logEvent("app_usage_tracked", parameters: [
            "app_name": appName,
            "duration_minutes": Int(duration / 60),
            "category": category
        ])
    }
    
    func logGoalAchievement(goalType: String, value: Int) {
        logEvent("goal_achieved", parameters: [
            "goal_type": goalType,
            "value": value
        ])
    }
    
    func logLimitSet(appName: String, limitMinutes: Int) {
        logEvent("limit_set", parameters: [
            "app_name": appName,
            "limit_minutes": limitMinutes
        ])
    }
    
    func logSubscriptionEvent(_ event: String, productId: String? = nil) {
        var parameters: [String: Any] = ["event_type": event]
        if let productId = productId {
            parameters["product_id"] = productId
        }
        logEvent("subscription_event", parameters: parameters)
    }
    
    // MARK: - User Properties
    
    func setUserProperty(_ value: String?, forName name: String) {
        Analytics.setUserProperty(value, forName: name)
        print("👤 User Property Set: \(name) = \(value ?? "nil")")
    }
    
    func setUserId(_ userId: String?) {
        Analytics.setUserID(userId)
        print("🆔 User ID Set: \(userId ?? "anonymous")")
    }
    
    // MARK: - Crashlytics
    
    func logError(_ error: Error, additionalInfo: [String: Any]? = nil) {
        Crashlytics.crashlytics().record(error: error)
        
        if let info = additionalInfo {
            for (key, value) in info {
                Crashlytics.crashlytics().setCustomValue(value, forKey: key)
            }
        }
        
        print("💥 Error logged to Crashlytics: \(error.localizedDescription)")
    }
    
    func logMessage(_ message: String, level: CrashlyticsLogLevel = .info) {
        Crashlytics.crashlytics().log(format: message)
        print("📝 Crashlytics Log: \(message)")
    }
    
    func setCustomKey(_ key: String, value: Any) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }
    
    // MARK: - Session Tracking
    
    private var sessionStartTime: Date?
    
    func startSession() {
        sessionStartTime = Date()
        logEvent("session_start")
    }
    
    func endSession() {
        guard let startTime = sessionStartTime else { return }
        
        let sessionDuration = Date().timeIntervalSince(startTime)
        logEvent("session_end", parameters: [
            "session_duration_seconds": Int(sessionDuration)
        ])
        
        sessionStartTime = nil
    }
}

// MARK: - Analytics Event Extensions
extension AnalyticsManager {
    
    // Onboarding Events
    func logOnboardingStart() {
        logEvent("onboarding_start")
    }
    
    func logOnboardingComplete() {
        logEvent("onboarding_complete")
    }
    
    func logOnboardingStep(_ step: String) {
        logEvent("onboarding_step", parameters: ["step": step])
    }
    
    // App Blocking Events
    func logAppBlocked(appName: String, reason: String) {
        logEvent("app_blocked", parameters: [
            "app_name": appName,
            "reason": reason
        ])
    }
    
    func logAppUnblocked(appName: String) {
        logEvent("app_unblocked", parameters: ["app_name": appName])
    }
    
    // Settings Events
    func logSettingChanged(_ setting: String, value: Any) {
        logEvent("setting_changed", parameters: [
            "setting": setting,
            "value": String(describing: value)
        ])
    }
    
    // Dashboard Events
    func logDashboardViewed() {
        logScreenView("dashboard")
    }
    
    func logAnalyticsViewed() {
        logScreenView("analytics")
    }
    
    func logSettingsViewed() {
        logScreenView("settings")
    }
}