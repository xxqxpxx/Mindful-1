package com.awaytime.app.service

enum class PremiumFeature(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val limitationType: FeatureLimitationType = FeatureLimitationType.PREMIUM_ONLY,
    val usageCount: Int = 0,
    val timeLimit: Int = 0
) {
    MULTIPLE_APP_GROUPS("multiple_app_groups", "Multiple App Groups", "Create and manage multiple app groups", "folder_plus"),
    ADVANCED_ANALYTICS("advanced_analytics", "Advanced Analytics", "Detailed usage insights and trends", "analytics"),
    CUSTOM_THEMES("custom_themes", "Custom Themes", "Personalize your app appearance", "palette"),
    EXPORT_DATA("export_data", "Export Data", "Export your usage data", "download"),
    FOCUS_SESSIONS("focus_sessions", "Focus Sessions", "Dedicated focus time sessions", "timer"),
    SMART_APP_CATEGORIZATION("smart_app_categorization", "Smart App Categories", "AI-powered automatic app categorization and custom category creation", "auto_awesome"),
    SCHEDULED_LIMITS("scheduled_limits", "Scheduled Limits", "Set different time limits for different hours of the day and days of the week", "schedule"),
    ADVANCED_GOALS("advanced_goals", "Advanced Goals & Challenges", "Custom challenges, milestone tracking, and achievement systems", "emoji_events"),
    BREAK_REMINDERS("break_reminders", "Smart Break Reminders", "Intelligent break suggestions based on your usage patterns", "self_improvement"),
    WEBSITE_BLOCKING("website_blocking", "Website Blocking", "Block distracting websites in addition to apps", "block"),
    EMERGENCY_OVERRIDE("emergency_override", "Emergency Override", "Temporary limit overrides with increasing friction and cooldowns", "warning"),
    AI_INSIGHTS("ai_insights", "AI-Powered Insights", "Get intelligent recommendations and behavioral pattern analysis", "psychology"),
    WEEKLY_REPORTS("weekly_reports", "Weekly Reports", "Comprehensive weekly usage reports with actionable insights", "assessment"),
    ADVANCED_NOTIFICATIONS("advanced_notifications", "Advanced Notifications", "Custom notification schedules, motivational quotes, and smart alerts", "notifications_active"),
    CLOUD_SYNC("cloud_sync", "Cloud Sync", "Sync your data and settings across all your devices", "cloud_sync"),
    FAMILY_SHARING("family_sharing", "Family & Team Management", "Share limits and track progress with family members or team", "family_restroom"),

    // Free features for reference
    BASIC_USAGE_TRACKING("basic_usage_tracking", "Basic Usage Tracking", "Track your daily app usage and set limits", "clock", FeatureLimitationType.UNLIMITED),
    SINGLE_APP_GROUP("single_app_group", "Single App Group", "Manage one group of apps with time limits", "folder", FeatureLimitationType.UNLIMITED),
    BASIC_NOTIFICATIONS("basic_notifications", "Basic Notifications", "Get notified when approaching your limits", "notifications", FeatureLimitationType.UNLIMITED);

    companion object {
        fun fromId(id: String): PremiumFeature? {
            return values().find { it.id == id }
        }
    }
}
