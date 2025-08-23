package com.awaytime.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awaytime.app.service.*
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.delay
import org.checkerframework.checker.units.qual.C

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ImprovedOnboardingFlow(
    onComplete: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onNavigateToAppSelection: () -> Unit,
    onboardingManager: ImprovedOnboardingManager = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { onboardingManager.simplifiedSteps.size })

    LaunchedEffect(onboardingManager.currentStep) {
        val stepIndex = onboardingManager.simplifiedSteps.indexOf(onboardingManager.currentStep)
        if (stepIndex >= 0) {
            pagerState.animateScrollToPage(stepIndex)
        }
    }

    LaunchedEffect(onboardingManager.isCompleted) {
        if (onboardingManager.isCompleted) {
            onComplete()
        }
    }
    
    // Set up navigation callback
    LaunchedEffect(Unit) {
        onboardingManager.onNavigateToAppSelection = onNavigateToAppSelection
    }
    
    // Load selected apps when returning to this screen
    LaunchedEffect(onboardingManager.currentStep) {
        if (onboardingManager.currentStep == OnboardingStep.APP_SELECTION) {
            onboardingManager.loadSelectedAppsFromService()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AwayTimeColors.primary.copy(alpha = 0.1f),
                        AwayTimeColors.primary.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (onboardingManager.canSkip) {
                    TextButton(
                        onClick = { onboardingManager.skipOnboarding() }
                    ) {
                        Text(
                            text = "Skip",
                            color = AwayTimeColors.primary
                        )
                    }
                }
            }      
      // Onboarding pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (onboardingManager.simplifiedSteps[page]) {
                    OnboardingStep.WELCOME -> WelcomeScreen()
                    OnboardingStep.FEATURES -> FeaturesOverviewScreen()
                    OnboardingStep.APP_SELECTION -> ImprovedAppSelectionScreen(
                        onboardingManager = onboardingManager,
                        onRequestUsageStatsPermission = onRequestUsageStatsPermission
                    )
                    OnboardingStep.GOAL_SETTING -> ImprovedGoalSettingScreen(onboardingManager)
                    OnboardingStep.COMPLETION -> ImprovedCompletionScreen(
                        onboardingManager = onboardingManager,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission
                    )
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }

            // Bottom navigation
            ImprovedOnboardingBottomNavigation(
                onboardingManager = onboardingManager,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ImprovedAppSelectionScreen(
    onboardingManager: ImprovedOnboardingManager,
    onRequestUsageStatsPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(60.dp)
            )

            Text(
                text = "Choose Your Apps",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select the apps you'd like to monitor and set limits for",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Show different content based on permission status
        when (onboardingManager.usageStatsPermissionStatus) {
            PermissionStatus.GRANTED -> {
                // User has permission - show app selection
                AppSelectionContent(onboardingManager)
            }
            else -> {
                // User needs permission - show contextual request
                UsagePermissionRequestCard(
                    onRequestPermission = {
                        onboardingManager.trackPermissionRequested("usage_stats", "app_selection")
                        onRequestUsageStatsPermission()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AppSelectionContent(onboardingManager: ImprovedOnboardingManager) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { 
                if (!onboardingManager.isLoadingApps) {
                    onboardingManager.showAppSelection()
                }
            },
            enabled = !onboardingManager.isLoadingApps,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AwayTimeColors.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onboardingManager.isLoadingApps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    if (onboardingManager.isLoadingApps) "Loading Apps..." else "Select Apps to Monitor"
                )
            }
        }

        if (onboardingManager.selectedApps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selected Apps (${onboardingManager.selectedApps.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(onboardingManager.selectedApps.take(6)) { app ->
                            SelectedAppCard(appName = app)
                        }
                    }

                    if (onboardingManager.selectedApps.size > 6) {
                        Text(
                            text = "and ${onboardingManager.selectedApps.size - 6} more...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsagePermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(48.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Usage Access Needed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To help you select and monitor apps, Awaytime needs access to usage statistics. This data stays private on your device.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Grant Usage Access")
                }
            }

            Text(
                text = "🔒 Your privacy is protected - we never collect or share your data",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun ImprovedGoalSettingScreen(onboardingManager: ImprovedOnboardingManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrackChanges,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(60.dp)
            )

            Text(
                text = "Set Your Goals",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose a daily time limit that feels realistic and achievable",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Recommended Daily Limits",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(onboardingManager.presetGoals) { preset ->
                    GoalPresetCard(
                        preset = preset,
                        isSelected = onboardingManager.selectedGoal?.minutes == preset.minutes,
                        onSelect = { onboardingManager.selectGoal(preset) }
                    )
                }
            }

            // Custom goal option
            CustomGoalCard(onboardingManager)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ImprovedCompletionScreen(
    onboardingManager: ImprovedOnboardingManager,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Color.Green.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(60.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "You're All Set! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Awaytime is ready to help you build healthier digital habits",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Optional permissions section
        OptionalPermissionsSection(
            onboardingManager = onboardingManager,
            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
            onRequestNotificationPermission = onRequestNotificationPermission
        )

        // What's available section
        AvailableFeaturesSection(onboardingManager)

        Spacer(modifier = Modifier.height(40.dp))
    }
}
@Composable
private fun OptionalPermissionsSection(
    onboardingManager: ImprovedOnboardingManager,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Optional Enhancements",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Accessibility permission for app blocking
        if (!onboardingManager.canBlockApps) {
            OptionalPermissionCard(
                icon = Icons.Default.Block,
                title = "App Blocking",
                description = "Automatically block apps when you reach your limits",
                buttonText = "Enable Blocking",
                onRequest = {
                    onboardingManager.trackPermissionRequested("accessibility", "completion")
                    onRequestAccessibilityPermission()
                }
            )
        }

        // Notification permission for reminders
        if (!onboardingManager.canSendNotifications) {
            OptionalPermissionCard(
                icon = Icons.Default.Notifications,
                title = "Smart Reminders",
                description = "Get helpful notifications and achievement celebrations",
                buttonText = "Enable Notifications",
                onRequest = {
                    onboardingManager.trackPermissionRequested("notifications", "completion")
                    onRequestNotificationPermission()
                }
            )
        }
    }
}

@Composable
private fun OptionalPermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onRequest
            ) {
                Text(
                    text = buttonText,
                    color = AwayTimeColors.primary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AvailableFeaturesSection(onboardingManager: ImprovedOnboardingManager) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What's Available Now",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureStatusRow(
                icon = Icons.Default.Analytics,
                title = "Usage Tracking",
                isAvailable = onboardingManager.canTrackUsage,
                description = if (onboardingManager.canTrackUsage) "Monitor your app usage" else "Needs usage access"
            )

            FeatureStatusRow(
                icon = Icons.Default.Block,
                title = "App Blocking",
                isAvailable = onboardingManager.canBlockApps,
                description = if (onboardingManager.canBlockApps) "Block apps at limits" else "Optional - enable anytime"
            )

            FeatureStatusRow(
                icon = Icons.Default.Notifications,
                title = "Smart Reminders",
                isAvailable = onboardingManager.canSendNotifications,
                description = if (onboardingManager.canSendNotifications) "Get helpful notifications" else "Optional - enable anytime"
            )
        }
    }
}

@Composable
private fun FeatureStatusRow(
    icon: ImageVector,
    title: String,
    isAvailable: Boolean,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isAvailable) Color.Green else Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isAvailable) Color.Green else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Supporting Composables (reuse from original)

@Composable
private fun SelectedAppCard(appName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = AwayTimeColors.primary
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun GoalPresetCard(
    preset: GoalPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AwayTimeColors.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, AwayTimeColors.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = preset.emoji,
                fontSize = 24.sp
            )

            Text(
                text = preset.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${preset.minutes} min",
                fontSize = 14.sp,
                color = AwayTimeColors.primary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = preset.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomGoalCard(onboardingManager: ImprovedOnboardingManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Or set a custom limit",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Daily limit:")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onboardingManager.decreaseCustomGoal() },
                        enabled = onboardingManager.customGoalMinutes > 30
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${onboardingManager.customGoalMinutes} min",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { onboardingManager.increaseCustomGoal() },
                        enabled = onboardingManager.customGoalMinutes < 480
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            TextButton(
                onClick = { onboardingManager.selectCustomGoal() }
            ) {
                Text(
                    text = "Use Custom Goal",
                    color = AwayTimeColors.primary
                )
            }
        }
    }
}

@Composable
private fun ImprovedOnboardingBottomNavigation(
    onboardingManager: ImprovedOnboardingManager,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        if (onboardingManager.canGoBack) {
            TextButton(
                onClick = { onboardingManager.goBack() }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Back")
                }
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Progress indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            onboardingManager.simplifiedSteps.forEachIndexed { index, step ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (step == onboardingManager.currentStep) {
                                AwayTimeColors.primary
                            } else if (onboardingManager.simplifiedSteps.indexOf(onboardingManager.currentStep) > index) {
                                AwayTimeColors.primary.copy(alpha = 0.5f)
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        // Next button
        Button(
            onClick = { onboardingManager.goNext() },
            enabled = onboardingManager.canGoNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = AwayTimeColors.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(onboardingManager.nextButtonTitle)
                if (onboardingManager.currentStep != OnboardingStep.COMPLETION) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}