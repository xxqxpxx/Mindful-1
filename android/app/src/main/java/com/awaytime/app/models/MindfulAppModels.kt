/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.models

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Represents an installed application on the device
 */
data class InstalledApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val iconBitmap: ImageBitmap? = null,
    val isSystemApp: Boolean = false,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val targetSdk: Int = 0,
    val isEnabled: Boolean = true,
    val dataDir: String? = null,
    val publicSourceDir: String? = null,
    val uid: Int = 0
) {
    /**
     * Returns true if this app matches the search query
     */
    fun matchesSearch(query: String): Boolean {
        if (query.isBlank()) return true
        
        val lowerQuery = query.lowercase()
        return appName.lowercase().contains(lowerQuery) ||
               packageName.lowercase().contains(lowerQuery)
    }
    
    /**
     * Returns a display name for the app, preferring appName over packageName
     */
    fun displayName(): String = appName.ifEmpty { packageName }
    
    /**
     * Returns true if this is a popular/commonly blocked app
     */
    fun isPopularApp(): Boolean {
        return packageName in POPULAR_APPS_SET
    }
    
    companion object {
        val POPULAR_APPS_SET = setOf(
            // Social Media
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.snapchat.android",
            "com.twitter.android",
            "com.facebook.katana",
            "com.facebook.orca", // Messenger
            "com.discord",
            "org.telegram.messenger",
            "com.whatsapp",
            "com.reddit.frontpage",
            "com.pinterest",
            "com.linkedin.android",
            "com.tumblr",
            
            // Entertainment
            "com.youtube.android",
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.amazon.avod.thirdpartyclient", // Prime Video
            "com.disney.disneyplus",
            "com.hulu.plus",
            "com.twitch.android.app",
            "com.google.android.youtube.tv",
            
            // Gaming
            "com.king.candycrushsaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.android.vending", // Play Store
            "com.roblox.client",
            "com.mojang.minecraftpe",
            "com.ea.game.pvzfree_row",
            "com.nianticlabs.pokemongo",
            
            // Shopping
            "com.amazon.mShop.android.shopping",
            "com.ebay.mobile",
            "com.contextlogic.wish",
            "com.alibaba.aliexpresshd",
            "com.etsy.android",
            
            // News & Reading
            "flipboard.app",
            "com.google.android.apps.magazines",
            "com.cnn.mobile.android.phone",
            "com.nytimes.android",
            "bbc.mobile.news.ww",
            
            // Dating
            "com.tinder",
            "com.bumble.app",
            "com.match.android.matchmobile",
            "com.okcupid.okcupid",
            "com.badoo.mobile"
        )
    }
}

/**
 * Represents a category of apps for organization
 */
enum class AppCategory(val displayName: String) {
    SOCIAL_MEDIA("Social Media"),
    ENTERTAINMENT("Entertainment"),
    GAMES("Games"),
    SHOPPING("Shopping"),
    NEWS("News & Reading"),
    DATING("Dating"),
    PRODUCTIVITY("Productivity"),
    COMMUNICATION("Communication"),
    UTILITIES("Utilities"),
    SYSTEM("System"),
    OTHER("Other");
    
    companion object {
        fun fromPackageName(packageName: String): AppCategory {
            return when {
                packageName in setOf(
                    "com.instagram.android", "com.zhiliaoapp.musically", "com.snapchat.android",
                    "com.twitter.android", "com.facebook.katana", "com.reddit.frontpage",
                    "com.pinterest", "com.linkedin.android", "com.tumblr"
                ) -> SOCIAL_MEDIA
                
                packageName in setOf(
                    "com.youtube.android", "com.netflix.mediaclient", "com.spotify.music",
                    "com.amazon.avod.thirdpartyclient", "com.disney.disneyplus", "com.hulu.plus",
                    "com.twitch.android.app"
                ) -> ENTERTAINMENT
                
                packageName in setOf(
                    "com.king.candycrushsaga", "com.supercell.clashofclans", "com.supercell.clashroyale",
                    "com.roblox.client", "com.mojang.minecraftpe", "com.ea.game.pvzfree_row",
                    "com.nianticlabs.pokemongo"
                ) -> GAMES
                
                packageName in setOf(
                    "com.amazon.mShop.android.shopping", "com.ebay.mobile", "com.contextlogic.wish",
                    "com.alibaba.aliexpresshd", "com.etsy.android"
                ) -> SHOPPING
                
                packageName in setOf(
                    "flipboard.app", "com.google.android.apps.magazines", "com.cnn.mobile.android.phone",
                    "com.nytimes.android", "bbc.mobile.news.ww"
                ) -> NEWS
                
                packageName in setOf(
                    "com.tinder", "com.bumble.app", "com.match.android.matchmobile",
                    "com.okcupid.okcupid", "com.badoo.mobile"
                ) -> DATING
                
                packageName in setOf(
                    "com.whatsapp", "com.facebook.orca", "com.discord", "org.telegram.messenger"
                ) -> COMMUNICATION
                
                else -> OTHER
            }
        }
    }
}

/**
 * Represents the loading state of app data
 */
sealed class AppLoadingState {
    object Idle : AppLoadingState()
    object Loading : AppLoadingState()
    data class Success(val apps: List<InstalledApp>) : AppLoadingState()
    data class Error(val message: String, val throwable: Throwable? = null) : AppLoadingState()
    
    fun isLoading(): Boolean = this is Loading
    fun isError(): Boolean = this is Error
    fun isSuccess(): Boolean = this is Success
    
    fun getAppsOrEmpty(): List<InstalledApp> {
        return when (this) {
            is Success -> apps
            else -> emptyList()
        }
    }
}

/**
 * Represents search and filter criteria for apps
 */
data class AppFilterCriteria(
    val searchQuery: String = "",
    val category: AppCategory? = null,
    val showSystemApps: Boolean = false,
    val showPopularOnly: Boolean = false,
    val sortBy: AppSortBy = AppSortBy.NAME
)

/**
 * Enum for different sorting options
 */
enum class AppSortBy(val displayName: String) {
    NAME("Name"),
    PACKAGE_NAME("Package Name"),
    INSTALL_TIME("Install Date"),
    UPDATE_TIME("Last Updated"),
    SIZE("Size")
}

/**
 * Result class for app loading operations
 */
sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val exception: Throwable) : AppResult<Nothing>()
    data class Loading(val message: String = "Loading...") : AppResult<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (Throwable) -> Unit): AppResult<T> {
        if (this is Error) action(exception)
        return this
    }
    
    inline fun onLoading(action: (String) -> Unit): AppResult<T> {
        if (this is Loading) action(message)
        return this
    }
}