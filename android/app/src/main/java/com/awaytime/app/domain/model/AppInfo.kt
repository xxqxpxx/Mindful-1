package com.awaytime.app.domain.model

/**
 * Mindful Domain Model: App Information
 * Clean architecture domain model following Mindful patterns
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
) {
    val displayName: String
        get() = appName.ifBlank { 
            packageName.substringAfterLast(".").replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            }
        }
}