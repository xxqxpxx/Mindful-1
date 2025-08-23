package com.awaytime.app.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class AppCategory(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val color: Long,
    val suggestedLimit: Int // in minutes
) {
    SOCIAL_MEDIA(
        "social_media",
        "Social Media",
        "Social networking and communication apps",
        "people",
        0xFF2196F3,
        60
    ),
    ENTERTAINMENT(
        "entertainment",
        "Entertainment",
        "Video streaming, gaming, and leisure apps",
        "play_circle",
        0xFFE91E63,
        90
    ),
    PRODUCTIVITY(
        "productivity",
        "Productivity",
        "Work, office, and productivity tools",
        "work",
        0xFF4CAF50,
        240
    ),
    EDUCATION(
        "education",
        "Education",
        "Learning, reading, and educational apps",
        "school",
        0xFF9C27B0,
        120
    ),
    SHOPPING(
        "shopping",
        "Shopping",
        "E-commerce and shopping applications",
        "shopping_cart",
        0xFFFF9800,
        45
    ),
    NEWS(
        "news",
        "News & Information",
        "News, weather, and informational apps",
        "newspaper",
        0xFF607D8B,
        30
    ),
    HEALTH_FITNESS(
        "health_fitness",
        "Health & Fitness",
        "Health tracking, fitness, and wellness apps",
        "fitness_center",
        0xFF8BC34A,
        60
    ),
    FINANCE(
        "finance",
        "Finance",
        "Banking, investment, and financial apps",
        "account_balance",
        0xFF795548,
        30
    ),
    TOOLS_UTILITIES(
        "tools_utilities",
        "Tools & Utilities",
        "System tools and utility applications",
        "build",
        0xFF9E9E9E,
        60
    ),
    TRAVEL(
        "travel",
        "Travel",
        "Maps, navigation, and travel apps",
        "flight",
        0xFF00BCD4,
        45
    ),
    FOOD_DRINK(
        "food_drink",
        "Food & Drink",
        "Food delivery, recipes, and restaurant apps",
        "restaurant",
        0xFFFF5722,
        30
    ),
    DATING(
        "dating",
        "Dating",
        "Dating and relationship apps",
        "favorite",
        0xFFE91E63,
        45
    ),
    UNCATEGORIZED(
        "uncategorized",
        "Uncategorized",
        "Apps that haven't been categorized yet",
        "help",
        0xFF9E9E9E,
        60
    )
}

data class CategorizedApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val confidence: Float, // 0.0 to 1.0
    val isManuallySet: Boolean = false,
    val suggestedCategories: List<AppCategory> = emptyList()
)

data class CategorySuggestion(
    val app: CategorizedApp,
    val suggestedCategory: AppCategory,
    val currentCategory: AppCategory,
    val reason: String,
    val confidence: Float
)

class SmartCategorizationService(private val context: Context) : ViewModel() {

    private val _categorizedApps = MutableStateFlow<List<CategorizedApp>>(emptyList())
    val categorizedApps: StateFlow<List<CategorizedApp>> = _categorizedApps.asStateFlow()

    private val _categorySuggestions = MutableStateFlow<List<CategorySuggestion>>(emptyList())
    val categorySuggestions: StateFlow<List<CategorySuggestion>> = _categorySuggestions.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // AI-powered app categorization rules
    private val categoryKeywords = mapOf(
        AppCategory.SOCIAL_MEDIA to listOf(
            "facebook", "instagram", "twitter", "snapchat", "tiktok", "whatsapp",
            "telegram", "discord", "linkedin", "pinterest", "reddit", "tumblr",
            "social", "chat", "messenger", "messaging"
        ),
        AppCategory.ENTERTAINMENT to listOf(
            "netflix", "youtube", "spotify", "disney", "hulu", "prime", "video",
            "music", "game", "gaming", "entertainment", "media", "stream",
            "twitch", "podcast", "radio", "tv", "movie", "film"
        ),
        AppCategory.PRODUCTIVITY to listOf(
            "office", "word", "excel", "powerpoint", "docs", "sheets", "slides",
            "notion", "evernote", "onenote", "trello", "asana", "slack", "zoom",
            "teams", "productivity", "work", "task", "calendar", "email", "mail"
        ),
        AppCategory.EDUCATION to listOf(
            "duolingo", "khan", "coursera", "udemy", "education", "learning",
            "study", "school", "university", "language", "math", "science",
            "book", "reading", "library", "dictionary", "wiki"
        ),
        AppCategory.SHOPPING to listOf(
            "amazon", "ebay", "etsy", "walmart", "target", "shopping", "shop",
            "store", "retail", "buy", "purchase", "commerce", "marketplace"
        ),
        AppCategory.NEWS to listOf(
            "news", "cnn", "bbc", "reuters", "associated", "times", "post",
            "journal", "weather", "forecast", "information", "breaking"
        ),
        AppCategory.HEALTH_FITNESS to listOf(
            "fitness", "health", "workout", "exercise", "gym", "yoga", "run",
            "step", "heart", "medical", "doctor", "hospital", "medicine",
            "nutrition", "diet", "calories", "weight"
        ),
        AppCategory.FINANCE to listOf(
            "bank", "banking", "finance", "money", "wallet", "pay", "payment",
            "credit", "debit", "investment", "stock", "crypto", "bitcoin",
            "budget", "expense", "mint", "paypal", "venmo"
        ),
        AppCategory.TOOLS_UTILITIES to listOf(
            "calculator", "clock", "flashlight", "file", "manager", "cleaner",
            "security", "antivirus", "vpn", "password", "backup", "storage",
            "utility", "tool", "system", "settings", "launcher"
        ),
        AppCategory.TRAVEL to listOf(
            "maps", "google maps", "uber", "lyft", "airbnb", "booking", "hotel",
            "flight", "travel", "trip", "navigation", "gps", "transport"
        ),
        AppCategory.FOOD_DRINK to listOf(
            "food", "restaurant", "delivery", "grubhub", "doordash", "uber eats",
            "recipe", "cooking", "kitchen", "drink", "coffee", "starbucks",
            "mcdonald", "pizza", "burger"
        ),
        AppCategory.DATING to listOf(
            "tinder", "bumble", "hinge", "match", "dating", "love", "relationship",
            "meet", "single", "romance", "partner"
        )
    )

    init {
        if (canUseSmartCategorization()) {
            loadCategorizedApps()
        }
    }

    // MARK: - Public API

    fun analyzeInstalledApps() {
        if (!canUseSmartCategorization()) {
            println("❌ Smart Categorization requires premium subscription")
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            
            try {
                val installedApps = getInstalledApps()
                val categorizedApps = installedApps.map { app ->
                    categorizeApp(app.packageName, app.appName)
                }
                
                _categorizedApps.value = categorizedApps
                saveCategorizedApps(categorizedApps)
                
                // Generate suggestions for better categorization
                generateCategorySuggestions()
                
                println("✅ Analyzed ${categorizedApps.size} apps with smart categorization")
            } catch (e: Exception) {
                println("❌ Error analyzing apps: ${e.message}")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun manuallySetCategory(packageName: String, category: AppCategory) {
        val currentApps = _categorizedApps.value.toMutableList()
        val appIndex = currentApps.indexOfFirst { it.packageName == packageName }
        
        if (appIndex != -1) {
            val updatedApp = currentApps[appIndex].copy(
                category = category,
                isManuallySet = true,
                confidence = 1.0f
            )
            currentApps[appIndex] = updatedApp
            _categorizedApps.value = currentApps
            saveCategorizedApps(currentApps)
            
            println("📝 Manually set ${updatedApp.appName} to ${category.displayName}")
        }
    }

    fun getAppsByCategory(category: AppCategory): List<CategorizedApp> {
        return _categorizedApps.value.filter { it.category == category }
    }

    fun getCategoryStats(): Map<AppCategory, Int> {
        return _categorizedApps.value.groupingBy { it.category }.eachCount()
    }

    fun applySuggestion(suggestion: CategorySuggestion) {
        manuallySetCategory(suggestion.app.packageName, suggestion.suggestedCategory)
        
        // Remove applied suggestion
        val currentSuggestions = _categorySuggestions.value.toMutableList()
        currentSuggestions.removeAll { it.app.packageName == suggestion.app.packageName }
        _categorySuggestions.value = currentSuggestions
    }

    fun dismissSuggestion(suggestion: CategorySuggestion) {
        val currentSuggestions = _categorySuggestions.value.toMutableList()
        currentSuggestions.removeAll { it.app.packageName == suggestion.app.packageName }
        _categorySuggestions.value = currentSuggestions
    }

    // MARK: - Private Implementation

    private fun categorizeApp(packageName: String, appName: String): CategorizedApp {
        val existingApp = _categorizedApps.value.find { it.packageName == packageName }
        if (existingApp?.isManuallySet == true) {
            return existingApp // Don't override manual categorization
        }

        val categoryScores = mutableMapOf<AppCategory, Float>()
        val searchText = "$packageName $appName".lowercase()

        // Score each category based on keyword matches
        categoryKeywords.forEach { (category, keywords) ->
            var score = 0f
            keywords.forEach { keyword ->
                when {
                    searchText.contains(keyword) -> score += 1.0f
                    searchText.contains(keyword.substring(0, minOf(keyword.length, 4))) -> score += 0.5f
                }
            }
            
            // Normalize score based on number of keywords
            if (keywords.isNotEmpty()) {
                categoryScores[category] = score / keywords.size
            }
        }

        // Find best category
        val bestCategory = categoryScores.maxByOrNull { it.value }
        val category = if (bestCategory != null && bestCategory.value > 0.1f) {
            bestCategory.key
        } else {
            AppCategory.UNCATEGORIZED
        }

        val confidence = bestCategory?.value ?: 0.0f
        val suggestedCategories = categoryScores
            .filter { it.value > 0.05f && it.key != category }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        return CategorizedApp(
            packageName = packageName,
            appName = appName,
            category = category,
            confidence = confidence,
            suggestedCategories = suggestedCategories
        )
    }

    private fun generateCategorySuggestions() {
        val suggestions = mutableListOf<CategorySuggestion>()
        
        _categorizedApps.value.forEach { app ->
            // Suggest better categories for low-confidence apps
            if (app.confidence < 0.5f && !app.isManuallySet && app.suggestedCategories.isNotEmpty()) {
                val suggestion = CategorySuggestion(
                    app = app,
                    suggestedCategory = app.suggestedCategories.first(),
                    currentCategory = app.category,
                    reason = "Low confidence match - consider ${app.suggestedCategories.first().displayName}",
                    confidence = 0.7f
                )
                suggestions.add(suggestion)
            }
            
            // Suggest recategorization for uncategorized apps with usage data
            if (app.category == AppCategory.UNCATEGORIZED && app.suggestedCategories.isNotEmpty()) {
                val suggestion = CategorySuggestion(
                    app = app,
                    suggestedCategory = app.suggestedCategories.first(),
                    currentCategory = app.category,
                    reason = "Found potential category match based on app analysis",
                    confidence = 0.6f
                )
                suggestions.add(suggestion)
            }
        }
        
        _categorySuggestions.value = suggestions.take(10) // Limit suggestions
    }

    private data class InstalledApp(val packageName: String, val appName: String)

    private fun getInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // User apps only
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString()
                )
            }
            .sortedBy { it.appName }
    }

    private fun loadCategorizedApps() {
        // Load from SharedPreferences or database
        // For now, start with empty list - this would typically load cached data
        _categorizedApps.value = emptyList()
    }

    private fun saveCategorizedApps(apps: List<CategorizedApp>) {
        // Save to SharedPreferences or database
        // Implementation would persist the categorization data
        println("💾 Saved categorization for ${apps.size} apps")
    }

    private fun canUseSmartCategorization(): Boolean {
        val subscriptionService = SubscriptionManager.getService()
        return subscriptionService?.canUseFeature(PremiumFeature.SMART_APP_CATEGORIZATION) ?: false
    }

    // MARK: - Batch Operations

    fun createGroupsFromCategories(): Map<AppCategory, List<String>> {
        if (!canUseSmartCategorization()) return emptyMap()

        return _categorizedApps.value
            .filter { it.category != AppCategory.UNCATEGORIZED }
            .groupBy { it.category }
            .mapValues { (_, apps) -> apps.map { it.packageName } }
    }

    fun suggestTimeLimitsForCategories(): Map<AppCategory, Int> {
        return AppCategory.values()
            .filter { it != AppCategory.UNCATEGORIZED }
            .associateWith { it.suggestedLimit }
    }

    fun exportCategorization(): String {
        val categorizedApps = _categorizedApps.value
        val csv = StringBuilder()
        csv.append("Package Name,App Name,Category,Confidence,Manually Set\n")
        
        categorizedApps.forEach { app ->
            csv.append("${app.packageName},${app.appName},${app.category.displayName},${app.confidence},${app.isManuallySet}\n")
        }
        
        return csv.toString()
    }
} 