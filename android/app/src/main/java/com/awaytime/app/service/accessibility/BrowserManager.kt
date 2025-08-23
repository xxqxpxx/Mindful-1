package com.awaytime.app.service.accessibility

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.awaytime.app.models.Wellbeing

/**
 * Manages blocking of websites and browser content.
 * Adapted from Mindful's BrowserManager.
 */
class BrowserManager(
    private val context: Context,
    private val shortsPlatformManager: ShortsPlatformManager,
    private val blockedContentGoBack: () -> Unit
) {
    companion object {
        private const val TAG = "BrowserManager"
        private val nsfwDomains = mutableSetOf<String>()
        
        fun initializeNsfwDomains() {
            try {
                // Load NSFW domains list
                // TODO: Load from assets or remote source
                nsfwDomains.addAll(getBasicNsfwDomains())
                Log.d(TAG, "Initialized ${nsfwDomains.size} NSFW domains")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing NSFW domains", e)
            }
        }
        
        fun clearNsfwDomains() {
            nsfwDomains.clear()
            Log.d(TAG, "Cleared NSFW domains")
        }
        
        private fun getBasicNsfwDomains(): Set<String> {
            // Basic list of common NSFW domains for demonstration
            return setOf(
                "pornhub.com",
                "xvideos.com",
                "xnxx.com",
                "redtube.com",
                "youporn.com",
                "tube8.com"
            )
        }
    }
    
    fun blockDistraction(packageName: String, node: AccessibilityNodeInfo, wellbeing: Wellbeing) {
        try {
            val currentUrl = extractUrlFromBrowser(node, packageName)
            
            if (currentUrl != null && shouldBlockUrl(currentUrl, wellbeing)) {
                Log.d(TAG, "Blocking access to URL: $currentUrl")
                blockedContentGoBack()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking browser content for package: $packageName", e)
        }
    }
    
    private fun extractUrlFromBrowser(node: AccessibilityNodeInfo, packageName: String): String? {
        try {
            // Common URL bar IDs across different browsers
            val urlBarIds = when (packageName) {
                "com.android.chrome" -> listOf("url_bar", "search_box_text")
                "org.mozilla.firefox" -> listOf("url_bar_title", "mozac_browser_toolbar_url_view")
                "com.microsoft.emmx" -> listOf("url_bar")
                "com.opera.browser" -> listOf("url_field")
                "com.brave.browser" -> listOf("url_bar")
                else -> listOf("url_bar", "search_box_text", "address_bar")
            }
            
            for (urlBarId in urlBarIds) {
                val urlNodes = node.findAccessibilityNodeInfosByViewId("$packageName:id/$urlBarId")
                if (urlNodes.isNotEmpty()) {
                    val urlText = urlNodes[0].text?.toString()
                    if (!urlText.isNullOrBlank()) {
                        return urlText
                    }
                }
            }
            
            // Fallback: try to find any editable text that looks like a URL
            return findUrlInEditableText(node)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting URL from browser", e)
            return null
        }
    }
    
    private fun findUrlInEditableText(node: AccessibilityNodeInfo): String? {
        try {
            if (node.isEditable && node.text != null) {
                val text = node.text.toString()
                if (text.contains("http") || text.contains(".com") || text.contains(".org")) {
                    return text
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                child?.let {
                    val result = findUrlInEditableText(it)
                    if (result != null) return result
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding URL in editable text", e)
            return null
        }
    }
    
    private fun shouldBlockUrl(url: String, wellbeing: Wellbeing): Boolean {
        try {
            val domain = extractDomain(url.lowercase())
            
            // Check blocked websites
            if (wellbeing.blockedWebsites.any { domain.contains(it.lowercase()) }) {
                return true
            }
            
            // Check NSFW sites
            if (wellbeing.blockNsfwSites && nsfwDomains.any { domain.contains(it) }) {
                return true
            }
            
            // Check blocked keywords
            if (wellbeing.blockedKeywords.any { url.lowercase().contains(it.lowercase()) }) {
                return true
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if URL should be blocked", e)
            return false
        }
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = url.removePrefix("http://").removePrefix("https://").removePrefix("www.")
            val domain = cleanUrl.split("/")[0].split("?")[0]
            domain
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting domain from URL: $url", e)
            url
        }
    }
}