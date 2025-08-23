package com.awaytime.app.ui.premium

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.*
import com.awaytime.app.ui.theme.AwayTimeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeaturesScreen(
    onNavigateBack: () -> Unit = {},
    onUpgradeClicked: () -> Unit = {},
    onNavigateToFocusSessions: () -> Unit = {},
    onNavigateToSmartCategorization: () -> Unit = {},
    onNavigateToDataExport: () -> Unit = {}
) {
    val context = LocalContext.current
    val premiumFeatureManager = remember { PremiumFeatureManager.getInstance(context) }
    val features = premiumFeatureManager.getPremiumBenefits()
    
    // Group features by category for better organization
    val groupedFeatures = features.groupBy { benefit ->
        when (benefit.feature) {
            PremiumFeature.MULTIPLE_APP_GROUPS,
            PremiumFeature.SMART_APP_CATEGORIZATION,
            PremiumFeature.SCHEDULED_LIMITS -> "Organization & Control"
            
            PremiumFeature.FOCUS_SESSIONS,
            PremiumFeature.ADVANCED_GOALS,
            PremiumFeature.BREAK_REMINDERS -> "Focus & Productivity"
            
            PremiumFeature.WEBSITE_BLOCKING,
            PremiumFeature.EMERGENCY_OVERRIDE -> "Enhanced Control"
            
            PremiumFeature.ADVANCED_ANALYTICS,
            PremiumFeature.AI_INSIGHTS,
            PremiumFeature.WEEKLY_REPORTS -> "Analytics & Insights"
            
            PremiumFeature.CUSTOM_THEMES,
            PremiumFeature.ADVANCED_NOTIFICATIONS -> "Personalization"
            
            PremiumFeature.EXPORT_DATA,
            PremiumFeature.CLOUD_SYNC,
            PremiumFeature.FAMILY_SHARING -> "Data & Sync"
            
            else -> "Other Features"
        }
    }

    AwayTimeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Premium Features",
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Premium status card
                item {
                    PremiumStatusCard(
                        isPremium = premiumFeatureManager.isPremiumActive.value,
                        onUpgradeClicked = onUpgradeClicked
                    )
                }

                // Feature categories
                groupedFeatures.forEach { (category, categoryFeatures) ->
                    item {
                        Text(
                            text = category,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(categoryFeatures) { benefit ->
                        PremiumFeatureCard(
                            benefit = benefit,
                            isUnlocked = premiumFeatureManager.canUseFeature(benefit.feature),
                            onFeatureClick = {
                                if (premiumFeatureManager.canUseFeature(benefit.feature)) {
                                    // Navigate to the actual feature
                                    when (benefit.feature) {
                                        PremiumFeature.FOCUS_SESSIONS -> onNavigateToFocusSessions()
                                        PremiumFeature.SMART_APP_CATEGORIZATION -> onNavigateToSmartCategorization()
                                        PremiumFeature.EXPORT_DATA -> onNavigateToDataExport()
                                        else -> {
                                            // Feature is unlocked but no specific screen yet
                                            println("✅ ${benefit.feature.displayName} is available!")
                                        }
                                    }
                                } else {
                                    premiumFeatureManager.requestFeatureAccess(benefit.feature)
                                }
                            }
                        )
                    }
                }

                // Call-to-action
                if (!premiumFeatureManager.isPremiumActive.value) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Unlock Your Full Potential",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = "Get access to all premium features and take control of your digital wellness journey",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                Button(
                                    onClick = onUpgradeClicked,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upgrade,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("Upgrade to Premium")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumStatusCard(
    isPremium: Boolean,
    onUpgradeClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPremium) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column {
                    Text(
                        text = if (isPremium) "Premium Active" else "Free Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isPremium) 
                            "Enjoy all premium features" 
                        else 
                            "Upgrade to unlock advanced features",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!isPremium) {
                TextButton(onClick = onUpgradeClicked) {
                    Text("Upgrade")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumFeatureCard(
    benefit: PremiumBenefit,
    isUnlocked: Boolean,
    onFeatureClick: () -> Unit
) {
    Card(
        onClick = onFeatureClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feature icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = benefit.color.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Map icon strings to actual icons
                    val icon = getIconForFeature(benefit.icon)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = benefit.color
                    )
                }
            }
            
            // Feature details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = benefit.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (isUnlocked) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = benefit.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun getIconForFeature(iconString: String): ImageVector {
    return when (iconString) {
        "folder_plus" -> Icons.Default.FolderOpen
        "auto_awesome" -> Icons.Default.AutoAwesome
        "schedule" -> Icons.Default.Schedule
        "timer" -> Icons.Default.Timer
        "emoji_events" -> Icons.Default.EmojiEvents
        "self_improvement" -> Icons.Default.SelfImprovement
        "block" -> Icons.Default.Block
        "warning" -> Icons.Default.Warning
        "analytics" -> Icons.Default.Analytics
        "psychology" -> Icons.Default.Psychology
        "assessment" -> Icons.Default.Assessment
        "palette" -> Icons.Default.Palette
        "notifications_active" -> Icons.Default.NotificationsActive
        "download" -> Icons.Default.Download
        "cloud_sync" -> Icons.Default.CloudSync
        "family_restroom" -> Icons.Default.FamilyRestroom
        else -> Icons.Default.Star
    }
} 