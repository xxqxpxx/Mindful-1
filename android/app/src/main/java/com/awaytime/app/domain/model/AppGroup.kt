package com.awaytime.app.domain.model

/**
 * Mindful Domain Model: App Group
 * Clean architecture domain model following Mindful patterns
 */
data class AppGroup(
    val id: String,
    val name: String,
    val packageNames: List<String>,
    val dailyLimitMinutes: Int,
    val isActive: Boolean = true
) {
    val displayName: String
        get() = name.ifBlank { "My Apps" }
    
    val appCount: Int
        get() = packageNames.size
    
    fun containsApp(packageName: String): Boolean {
        return packageNames.contains(packageName)
    }
}