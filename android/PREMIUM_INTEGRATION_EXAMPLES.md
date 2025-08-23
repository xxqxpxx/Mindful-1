# Premium Integration Examples

This document shows how to integrate premium features throughout your Android app using the new premium components.

## Basic Usage Examples

### 1. Premium Button
Use `PremiumButton` for actions that require premium access:

```kotlin
@Composable
fun AdvancedAnalyticsScreen() {
    Column {
        // Regular content...
        
        PremiumButton(
            feature = PremiumFeature.EXPORT_DATA,
            text = "Export Data",
            icon = Icons.Default.Download,
            onClick = {
                // This will only execute if user has premium
                exportUserData()
            }
        )
    }
}
```

### 2. Premium Feature Card
Wrap entire features in premium gates:

```kotlin
@Composable
fun MultipleAppGroupsSection() {
    PremiumFeatureCard(
        feature = PremiumFeature.MULTIPLE_APP_GROUPS,
        title = "Multiple App Groups",
        description = "Create separate groups for work, social, and entertainment"
    ) {
        // This content only shows for premium users
        LazyColumn {
            items(appGroups) { group ->
                AppGroupItem(group = group)
            }
        }
        
        Button(
            onClick = { createNewAppGroup() }
        ) {
            Text("Add New Group")
        }
    }
}
```

### 3. Premium Switch
For settings that require premium:

```kotlin
@Composable
fun SettingsScreen() {
    Column {
        // Free settings...
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Smart Notifications")
                Text(
                    "AI-powered notification timing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            PremiumSwitch(
                feature = PremiumFeature.ADVANCED_NOTIFICATIONS,
                checked = smartNotificationsEnabled,
                onCheckedChange = { enabled ->
                    smartNotificationsEnabled = enabled
                    updateNotificationSettings(enabled)
                }
            )
        }
    }
}
```

### 4. Conditional Premium Content
Show different content based on premium status:

```kotlin
@Composable
fun AnalyticsScreen() {
    val isPremium = rememberPremiumStatus()
    
    Column {
        // Basic analytics (always shown)
        BasicUsageChart()
        
        // Premium analytics
        PremiumContent(
            feature = PremiumFeature.ADVANCED_ANALYTICS,
            premiumContent = {
                AdvancedInsightsSection()
                TrendAnalysisChart()
                PersonalizedRecommendations()
            },
            freeContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = AwayTimeColors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = "Advanced Analytics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Text(
                            text = "Get detailed insights, trends, and personalized recommendations",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Button(
                            onClick = { /* Show paywall */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AwayTimeColors.primary
                            )
                        ) {
                            Text("Upgrade to Premium")
                        }
                    }
                }
            }
        )
    }
}
```

### 5. Premium Status in Profile
Show premium status in user profile:

```kotlin
@Composable
fun ProfileScreen() {
    Column {
        // User info...
        
        PremiumStatusCard(
            modifier = Modifier.padding(16.dp)
        )
        
        // Other profile content...
    }
}
```

## Advanced Integration Examples

### 1. App Group Management with Premium Limits
```kotlin
@Composable
fun AppGroupsScreen() {
    val appGroups by viewModel.appGroups.collectAsState()
    val canCreateMultiple = rememberFeatureAccess(PremiumFeature.MULTIPLE_APP_GROUPS)
    
    Column {
        LazyColumn {
            items(appGroups) { group ->
                AppGroupCard(group = group)
            }
        }
        
        // Add button with premium enforcement
        if (appGroups.size < 1 || canCreateMultiple) {
            Button(
                onClick = { viewModel.createNewGroup() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add App Group")
            }
        } else {
            // Show premium gate for additional groups
            PremiumFeatureCard(
                feature = PremiumFeature.MULTIPLE_APP_GROUPS,
                title = "Create More App Groups",
                description = "Organize your apps into unlimited custom groups"
            ) {
                Button(
                    onClick = { viewModel.createNewGroup() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add App Group")
                }
            }
        }
    }
}
```

### 2. Focus Sessions with Premium Gate
```kotlin
@Composable
fun FocusSessionScreen() {
    val canUseFocusSessions = rememberFeatureAccess(PremiumFeature.FOCUS_SESSIONS)
    
    if (canUseFocusSessions) {
        // Full focus session UI
        FocusSessionContent()
    } else {
        // Premium gate with preview
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(80.dp)
            )
            
            Text(
                text = "Focus Sessions",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Text(
                text = "Boost your productivity with Pomodoro-style focus sessions that automatically block distracting apps.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Preview of features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    FeaturePreviewItem("25-minute focused work sessions")
                    FeaturePreviewItem("5-minute break reminders")
                    FeaturePreviewItem("Automatic app blocking during sessions")
                    FeaturePreviewItem("Progress tracking and statistics")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumButton(
                feature = PremiumFeature.FOCUS_SESSIONS,
                text = "Start Focus Session",
                icon = Icons.Default.PlayArrow,
                onClick = { /* This will show paywall */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FeaturePreviewItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 14.sp
        )
    }
}
```

### 3. Settings Screen with Premium Features
```kotlin
@Composable
fun SettingsScreen() {
    LazyColumn {
        // Basic settings section
        item {
            SettingsSection(title = "General") {
                SettingsItem(
                    title = "Notifications",
                    subtitle = "Basic app limit notifications",
                    onClick = { /* Navigate to notifications */ }
                )
                
                SettingsItem(
                    title = "Theme",
                    subtitle = "Light or dark mode",
                    onClick = { /* Navigate to theme */ }
                )
            }
        }
        
        // Premium settings section
        item {
            SettingsSection(title = "Premium Features") {
                PremiumSettingsItem(
                    feature = PremiumFeature.ADVANCED_NOTIFICATIONS,
                    title = "Smart Notifications",
                    subtitle = "AI-powered notification timing"
                )
                
                PremiumSettingsItem(
                    feature = PremiumFeature.CUSTOM_THEMES,
                    title = "Custom Themes",
                    subtitle = "Personalize your app appearance"
                )
                
                PremiumSettingsItem(
                    feature = PremiumFeature.CLOUD_SYNC,
                    title = "Cloud Sync",
                    subtitle = "Sync data across devices"
                )
                
                PremiumSettingsItem(
                    feature = PremiumFeature.EXPORT_DATA,
                    title = "Export Data",
                    subtitle = "Download your usage reports"
                )
            }
        }
        
        // Premium status
        item {
            PremiumStatusCard(
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun PremiumSettingsItem(
    feature: PremiumFeature,
    title: String,
    subtitle: String
) {
    val canUseFeature = rememberFeatureAccess(feature)
    var showPaywall by remember { mutableStateOf(false) }
    
    SettingsItem(
        title = title,
        subtitle = subtitle,
        onClick = {
            if (canUseFeature) {
                // Navigate to feature settings
            } else {
                showPaywall = true
            }
        },
        trailingContent = {
            if (!canUseFeature) {
                PremiumBadge(size = PremiumBadgeSize.Small)
            }
        }
    )
    
    if (showPaywall) {
        PaywallScreen(
            triggeredByFeature = feature,
            onDismiss = { showPaywall = false }
        )
    }
}
```

## Integration in Existing Screens

### Update MainActivity Navigation
```kotlin
// In your MainActivity or main navigation
@Composable
fun MainNavigation() {
    val isPremium = rememberPremiumStatus()
    
    NavHost(/*...*/) {
        // Existing routes...
        
        composable("analytics") {
            if (isPremium) {
                AdvancedAnalyticsScreen()
            } else {
                BasicAnalyticsWithPremiumGate()
            }
        }
        
        composable("focus") {
            FocusSessionScreen() // Handles premium gate internally
        }
    }
}
```

### Update Bottom Navigation
```kotlin
@Composable
fun BottomNavigationBar() {
    val isPremium = rememberPremiumStatus()
    
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navigate("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        
        NavigationBarItem(
            selected = currentRoute == "analytics",
            onClick = { navigate("analytics") },
            icon = { 
                Box {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    if (!isPremium) {
                        PremiumBadge(
                            modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
                            size = PremiumBadgeSize.Small
                        )
                    }
                }
            },
            label = { 
                PremiumText(
                    text = "Analytics",
                    feature = PremiumFeature.ADVANCED_ANALYTICS
                )
            }
        )
        
        // More navigation items...
    }
}
```

## Testing Premium Features

### Test with Mock Premium Status
```kotlin
// For testing, you can override premium status
@Composable
fun TestPremiumFeatures() {
    // Force premium status for testing
    CompositionLocalProvider(
        LocalPremiumStatus provides true
    ) {
        YourPremiumFeatureScreen()
    }
}
```

This integration approach ensures:
- **Consistent UX**: All premium features use the same visual language
- **Easy Maintenance**: Centralized premium logic
- **Good Performance**: Efficient state management
- **User-Friendly**: Clear value proposition for premium features
- **Conversion Focused**: Strategic placement of upgrade prompts