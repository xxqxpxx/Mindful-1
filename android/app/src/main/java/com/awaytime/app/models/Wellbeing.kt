package com.awaytime.app.models

import com.awaytime.app.enums.PlatformFeatures

/**
 * Data class representing user wellbeing settings for app blocking and content filtering.
 * Adapted from Mindful's Wellbeing model.
 */
data class Wellbeing(
    /**
     * Set of blocked app package names
     */
    val blockedApps: Set<String> = emptySet(),
    
    /**
     * Set of blocked platform features (shorts, reels, etc.)
     */
    val blockedFeatures: Set<PlatformFeatures> = emptySet(),
    
    /**
     * Set of blocked website domains
     */
    val blockedWebsites: Set<String> = emptySet(),
    
    /**
     * Set of NSFW website domains
     */
    val nsfwWebsites: Set<String> = emptySet(),
    
    /**
     * Whether to block NSFW sites automatically
     */
    val blockNsfwSites: Boolean = false,
    
    /**
     * Keywords to block in content
     */
    val blockedKeywords: Set<String> = emptySet(),
    
    /**
     * Whether to enable safe search in browsers
     */
    val enableSafeSearch: Boolean = false,
    
    /**
     * Whether parental controls are active
     */
    val parentalControlsActive: Boolean = false,
    
    /**
     * Whether tamper protection is enabled
     */
    val tamperProtectionEnabled: Boolean = false
)