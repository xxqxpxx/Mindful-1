package com.awaytime.app.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.*
import com.awaytime.app.ui.theme.AwayTimeColors

/**
 * A button that shows premium badge and triggers paywall for premium features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumButton(
    feature: PremiumFeature,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUseFeature by premiumManager.rememberCanUseFeature(feature)
    var showPaywall by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (canUseFeature) {
                premiumManager.trackFeatureUsage(feature)
                onClick()
            } else {
                showPaywall = true
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (canUseFeature) {
                AwayTimeColors.primary
            } else {
                AwayTimeColors.primary.copy(alpha = 0.7f)
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(text = text)
            
            if (!canUseFeature) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }

    if (showPaywall) {
        PaywallScreen(
            triggeredByFeature = feature,
            onDismiss = { showPaywall = false }
        )
    }
}

/**
 * A card that wraps content and shows premium gate if needed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeatureCard(
    feature: PremiumFeature,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUseFeature by premiumManager.rememberCanUseFeature(feature)
    var showPaywall by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (canUseFeature) {
                MaterialTheme.colorScheme.surface
            } else {
                AwayTimeColors.primary.copy(alpha = 0.05f)
            }
        ),
        border = if (!canUseFeature) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                AwayTimeColors.primary.copy(alpha = 0.3f)
            )
        } else null
    ) {
        if (canUseFeature) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        } else {
            // Premium gate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AwayTimeColors.primary,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = premiumManager.getFeatureLimitationMessage(feature),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { showPaywall = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Upgrade to Premium")
                    }
                }
            }
        }
    }

    if (showPaywall) {
        PaywallScreen(
            triggeredByFeature = feature,
            onDismiss = { showPaywall = false }
        )
    }
}

/**
 * A simple premium badge that can be added to any composable
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    size: PremiumBadgeSize = PremiumBadgeSize.Small
) {
    val badgeSize = when (size) {
        PremiumBadgeSize.Small -> 20.dp
        PremiumBadgeSize.Medium -> 24.dp
        PremiumBadgeSize.Large -> 32.dp
    }
    
    val iconSize = when (size) {
        PremiumBadgeSize.Small -> 12.dp
        PremiumBadgeSize.Medium -> 16.dp
        PremiumBadgeSize.Large -> 20.dp
    }

    Box(
        modifier = modifier
            .size(badgeSize)
            .background(
                color = Color(0xFFFFD700), // Gold
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFB8860B), // Dark gold
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

enum class PremiumBadgeSize {
    Small, Medium, Large
}

/**
 * A composable that conditionally shows content based on premium status
 */
@Composable
fun PremiumContent(
    feature: PremiumFeature,
    premiumContent: @Composable () -> Unit,
    freeContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUseFeature by premiumManager.rememberCanUseFeature(feature)

    if (canUseFeature) {
        premiumContent()
    } else {
        freeContent()
    }
}

/**
 * A text composable that shows premium indicator
 */
@Composable
fun PremiumText(
    text: String,
    feature: PremiumFeature,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUseFeature by premiumManager.rememberCanUseFeature(feature)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = style,
            color = if (canUseFeature) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        if (!canUseFeature) {
            PremiumBadge(size = PremiumBadgeSize.Small)
        }
    }
}

/**
 * A switch that shows premium gate when toggled
 */
@Composable
fun PremiumSwitch(
    feature: PremiumFeature,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    val canUseFeature by premiumManager.rememberCanUseFeature(feature)
    var showPaywall by remember { mutableStateOf(false) }

    Switch(
        checked = checked && canUseFeature,
        onCheckedChange = { newValue ->
            if (canUseFeature) {
                premiumManager.trackFeatureUsage(feature)
                onCheckedChange(newValue)
            } else {
                showPaywall = true
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AwayTimeColors.primary,
            checkedTrackColor = AwayTimeColors.primary.copy(alpha = 0.5f),
            uncheckedThumbColor = if (canUseFeature) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    )

    if (showPaywall) {
        PaywallScreen(
            triggeredByFeature = feature,
            onDismiss = { showPaywall = false }
        )
    }
}

/**
 * Premium status indicator for settings or profile screens
 */
@Composable
fun PremiumStatusCard(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {}
) {
    val isPremium = rememberPremiumStatus()
    var showPaywall by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) {
                Color(0xFF4CAF50).copy(alpha = 0.1f) // Green tint
            } else {
                AwayTimeColors.primary.copy(alpha = 0.1f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPremium) {
                Color(0xFF4CAF50).copy(alpha = 0.3f)
            } else {
                AwayTimeColors.primary.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPremium) Icons.Default.CheckCircle else Icons.Default.Star,
                contentDescription = null,
                tint = if (isPremium) Color(0xFF4CAF50) else AwayTimeColors.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Premium Active" else "Free Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = if (isPremium) {
                        "All premium features unlocked"
                    } else {
                        "Upgrade to unlock all features"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isPremium) {
                Button(
                    onClick = { showPaywall = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.primary
                    )
                ) {
                    Text("Upgrade")
                }
            }
        }
    }

    if (showPaywall) {
        PaywallScreen(
            onDismiss = { showPaywall = false }
        )
    }
}