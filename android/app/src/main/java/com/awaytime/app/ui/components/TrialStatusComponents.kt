package com.awaytime.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * Trial status banner that shows at the top of screens during trial
 */
@Composable
fun TrialStatusBanner(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val trialStatus by trialManager.rememberTrialStatus()
    val daysRemaining by trialManager.rememberDaysRemaining()
    val isTrialActive by trialManager.rememberIsTrialActive()
    
    AnimatedVisibility(
        visible = isTrialActive,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Premium Trial",
                        tint = AwayTimeColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "Premium Trial Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AwayTimeColors.primary
                        )
                        Text(
                            text = when {
                                daysRemaining > 1 -> "$daysRemaining days left"
                                daysRemaining == 1 -> "1 day left"
                                else -> "Less than 1 day left"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AwayTimeColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row {
                    TextButton(
                        onClick = onUpgradeClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AwayTimeColors.primary
                        )
                    ) {
                        Text("Upgrade", fontWeight = FontWeight.Medium)
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = AwayTimeColors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact trial status indicator for navigation bars
 */
@Composable
fun TrialStatusIndicator(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val isTrialActive by trialManager.rememberIsTrialActive()
    val daysRemaining by trialManager.rememberDaysRemaining()
    
    AnimatedVisibility(
        visible = isTrialActive,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .clickable { onClick() }
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            color = AwayTimeColors.primary,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Trial",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "${daysRemaining}d",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Trial progress card showing detailed trial information
 */
@Composable
fun TrialProgressCard(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onLearnMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val trialStatus by trialManager.rememberTrialStatus()
    val daysRemaining by trialManager.rememberDaysRemaining()
    val isTrialActive by trialManager.rememberIsTrialActive()
    
    if (isTrialActive) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AwayTimeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Premium",
                            tint = AwayTimeColors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = "Premium Trial",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AwayTimeColors.onSurface
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AwayTimeColors.primary.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AwayTimeColors.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress
                val progress = trialManager.getTrialProgress()
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Days remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AwayTimeColors.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = when {
                                daysRemaining > 1 -> "$daysRemaining days"
                                daysRemaining == 1 -> "1 day"
                                else -> "< 1 day"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AwayTimeColors.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AwayTimeColors.primary,
                        trackColor = AwayTimeColors.primary.copy(alpha = 0.2f)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Features preview
                Text(
                    text = "You're enjoying premium features:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AwayTimeColors.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val premiumFeatures = listOf(
                    "Multiple app groups" to Icons.Default.Folder,
                    "Advanced analytics" to Icons.Default.Analytics,
                    "Custom themes" to Icons.Default.Palette,
                    "Data export" to Icons.Default.Download
                )
                
                premiumFeatures.take(3).forEach { (feature, icon) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = feature,
                            tint = AwayTimeColors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AwayTimeColors.onSurface
                        )
                    }
                }
                
                if (premiumFeatures.size > 3) {
                    Text(
                        text = "And ${premiumFeatures.size - 3} more features...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayTimeColors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onLearnMoreClick,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AwayTimeColors.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Learn More",
                            color = AwayTimeColors.primary
                        )
                    }
                    
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.primary
                        )
                    ) {
                        Text(
                            text = "Upgrade Now",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Trial expired notice
 */
@Composable
fun TrialExpiredNotice(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val shouldShow = trialManager.shouldShowTrialExpiredMessage()
    
    AnimatedVisibility(
        visible = shouldShow,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AwayTimeColors.warning.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Trial Expired",
                            tint = AwayTimeColors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "Trial Expired",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AwayTimeColors.warning
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Your 7-day premium trial has ended. Upgrade to continue using premium features.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AwayTimeColors.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = AwayTimeColors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.warning
                    )
                ) {
                    Text(
                        text = "Upgrade to Premium",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Floating trial upgrade button
 */
@Composable
fun TrialUpgradeFloatingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val isTrialActive by trialManager.rememberIsTrialActive()
    val daysRemaining by trialManager.rememberDaysRemaining()
    
    AnimatedVisibility(
        visible = isTrialActive && daysRemaining <= 2,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = AwayTimeColors.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Upgrade,
                contentDescription = "Upgrade"
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (daysRemaining == 1) "Last Day!" else "Upgrade",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Premium feature lock overlay
 */
@Composable
fun PremiumFeatureLock(
    feature: PremiumFeature,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onStartTrialClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val premiumManager = remember { PremiumFeatureManager.getInstance(context) }
    
    val canUseFeature = premiumManager.canUseFeature(feature)
    val canStartTrial = !trialManager.hasUserEverHadTrial()
    
    if (!canUseFeature) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AwayTimeColors.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = AwayTimeColors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = feature.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = AwayTimeColors.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = premiumManager.getFeatureLimitationMessage(feature),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = AwayTimeColors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (canStartTrial) {
                        Button(
                            onClick = onStartTrialClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AwayTimeColors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start Trial"
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Start 7-Day Free Trial",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = onUpgradeClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "View Premium Plans",
                                color = AwayTimeColors.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = onUpgradeClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AwayTimeColors.primary
                            )
                        ) {
                            Text(
                                text = "Upgrade to Premium",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}