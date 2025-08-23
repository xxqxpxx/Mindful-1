package com.awaytime.app.ui.theme

import androidx.compose.ui.graphics.Color

// Awaytime Purple Color Scheme - Enhanced with better contrast
val AwayTimePurple = Color(0xFF8B5CF6)
val AwayTimeLightPurple = Color(0xFFC4B5FD)
val AwayTimeDarkPurple = Color(0xFF5B21B6)

// Additional colors with improved accessibility
val AwayTimeSuccess = Color(0xFF10B981)
val AwayTimeWarning = Color(0xFFF59E0B)
val AwayTimeError = Color(0xFFEF4444)

// Background colors with better contrast
val AwayTimeBackground = Color(0xFFFAFAFA)
val AwayTimeDarkBackground = Color(0xFF1A1A1A)
val AwayTimeCardBackground = Color(0xFFFFFFFF)
val AwayTimeDarkCardBackground = Color(0xFF2A2A2A)

// Loading and skeleton colors
val AwayTimeSkeletonBase = Color(0xFFE5E5E5)
val AwayTimeSkeletonHighlight = Color(0xFFF2F2F2)

// Surface colors for better depth
val AwayTimeSurfaceVariant = Color(0xFFF5F5F5)
val AwayTimeDarkSurfaceVariant = Color(0xFF1F1F1F)

object AwayTimeColors {
    val primary = AwayTimePurple
    val secondary = AwayTimeLightPurple
    val accent = AwayTimeDarkPurple
    val success = AwayTimeSuccess
    val warning = AwayTimeWarning
    val error = AwayTimeError
    val background = AwayTimeBackground
    val darkBackground = AwayTimeDarkBackground
    val cardBackground = AwayTimeCardBackground
    val darkCardBackground = AwayTimeDarkCardBackground
    val skeletonBase = AwayTimeSkeletonBase
    val skeletonHighlight = AwayTimeSkeletonHighlight
    val surfaceVariant = AwayTimeSurfaceVariant
    val darkSurfaceVariant = AwayTimeDarkSurfaceVariant
    val onSurface = Color(0xFF1C1C1C) // Added for Material3 compatibility
    val surface = AwayTimeCardBackground // Added surface property
}