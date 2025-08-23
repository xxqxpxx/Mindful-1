package com.awaytime.app.service.accessibility

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.awaytime.app.models.Wellbeing

/**
 * Manages blocking of short-form content (reels, shorts, etc.) across platforms.
 * Adapted from Mindful's ShortsPlatformManager.
 */
class ShortsPlatformManager(
    private val context: Context,
    private val blockedContentGoBack: () -> Unit
) {
    companion object {
        private const val TAG = "ShortsPlatformManager"
    }
    
    fun blockDistraction(packageName: String, node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            when (packageName) {
                "com.instagram.android" -> blockInstagramContent(node, wellbeing)
                "com.google.android.youtube" -> blockYouTubeShorts(node, wellbeing)
                "com.zhiliaoapp.musically" -> blockTikTokContent(node, wellbeing)
                "com.snapchat.android" -> blockSnapchatContent(node, wellbeing)
                "com.reddit.frontpage" -> blockRedditShorts(node, wellbeing)
                else -> {
                    // Generic app blocking
                    if (wellbeing.blockedApps.contains(packageName)) {
                        Log.d(TAG, "Blocking access to app: $packageName")
                        blockedContentGoBack()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking distraction for package: $packageName", e)
        }
    }
    
    private fun blockInstagramContent(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            // Check for Instagram Reels
            val reelsIndicators = listOf(
                "reels_viewer_container",
                "clips_viewer_root",
                "reels_tab"
            )
            
            for (indicator in reelsIndicators) {
                if (node.findAccessibilityNodeInfosByViewId("com.instagram.android:id/$indicator").isNotEmpty()) {
                    Log.d(TAG, "Detected Instagram Reels, blocking access")
                    blockedContentGoBack()
                    return
                }
            }
            
            // Check for Explore page
            if (node.findAccessibilityNodeInfosByText("Explore").isNotEmpty() ||
                node.findAccessibilityNodeInfosByViewId("com.instagram.android:id/explore_tab").isNotEmpty()) {
                Log.d(TAG, "Detected Instagram Explore, blocking access")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Instagram content", e)
        }
    }
    
    private fun blockYouTubeShorts(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            // Check for YouTube Shorts
            val shortsIndicators = listOf(
                "reel_player_page_container",
                "shorts_player",
                "reel_watch_while_browsing_container"
            )
            
            for (indicator in shortsIndicators) {
                if (node.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/$indicator").isNotEmpty()) {
                    Log.d(TAG, "Detected YouTube Shorts, blocking access")
                    blockedContentGoBack()
                    return
                }
            }
            
            // Check for Shorts tab
            if (node.findAccessibilityNodeInfosByText("Shorts").isNotEmpty()) {
                Log.d(TAG, "Detected YouTube Shorts tab, blocking access")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking YouTube Shorts", e)
        }
    }
    
    private fun blockTikTokContent(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            // TikTok is primarily short-form content, so block the entire app if configured
            if (wellbeing.blockedApps.contains("com.zhiliaoapp.musically")) {
                Log.d(TAG, "Blocking TikTok access")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking TikTok content", e)
        }
    }
    
    private fun blockSnapchatContent(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            // Check for Snapchat Spotlight and Discover
            if (node.findAccessibilityNodeInfosByText("Spotlight").isNotEmpty() ||
                node.findAccessibilityNodeInfosByText("Discover").isNotEmpty()) {
                Log.d(TAG, "Detected Snapchat Spotlight/Discover, blocking access")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Snapchat content", e)
        }
    }
    
    private fun blockRedditShorts(node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            // Check for Reddit's short video content
            if (node.findAccessibilityNodeInfosByViewId("com.reddit.frontpage:id/video_player").isNotEmpty()) {
                Log.d(TAG, "Detected Reddit video content, blocking access")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Reddit content", e)
        }
    }
    
    fun resetShortsScreenTime() {
        try {
            Log.d(TAG, "Resetting shorts screen time for midnight reset")
            // TODO: Reset any tracked shorts screen time
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting shorts screen time", e)
        }
    }
}