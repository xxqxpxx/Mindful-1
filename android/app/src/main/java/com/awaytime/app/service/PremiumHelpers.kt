package com.awaytime.app.service

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Composable function to easily check premium status
 */
@Composable
fun rememberPremiumStatus(): Boolean {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val isPremium by premiumManager.rememberIsPremiumActive()
    return isPremium
}

/**
 * Composable function to check feature access
 */
@Composable
fun rememberFeatureAccess(feature: PremiumFeature): Boolean {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUse by premiumManager.rememberCanUseFeature(feature)
    return canUse
}