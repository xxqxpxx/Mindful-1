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
import com.awaytime.app.service.GoalPreset
import com.awaytime.app.service.OnboardingManager
import com.awaytime.app.service.OnboardingStep
import com.awaytime.app.service.PermissionStatus
import com.awaytime.app.ui.theme.AwayTimeColors
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import kotlinx.coroutines.delay

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onboardingManager: OnboardingManager = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingStep.values().size })

    LaunchedEffect(onboardingManager.currentStep) {
        pagerState.animateScrollToPage(onboardingManager.currentStep.ordinal)
    }

    LaunchedEffect(onboardingManager.isCompleted) {
        if (onboardingManager.isCompleted) {
            onComplete()
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
                when (OnboardingStep.values()[page]) {
                    OnboardingStep.WELCOME -> WelcomeScreen()
                    OnboardingStep.FEATURES -> FeaturesOverviewScreen()
                    OnboardingStep.PREMIUM_TRIAL -> PremiumTrialScreen(onboardingManager)
                    OnboardingStep.PERMISSIONS -> PermissionsScreen(onboardingManager)
                    OnboardingStep.APP_SELECTION -> AppSelectionScreen(onboardingManager)
                    OnboardingStep.GOAL_SETTING -> GoalSettingScreen(onboardingManager)
                    OnboardingStep.COMPLETION -> CompletionScreen(onboardingManager)
                }
            }

            // Bottom navigation
            OnboardingBottomNavigation(
                onboardingManager = onboardingManager,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun WelcomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item { Spacer(modifier = Modifier.height(40.dp)) }

        // App logo and branding
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AwayTimeColors.primary,
                                    AwayTimeColors.primary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SequentialFoxAnimation(
                        modifier = Modifier.size(80.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Welcome to Awaytime",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Take control of your digital wellness with mindful app usage tracking",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Key benefits
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OnboardingBenefitRow(
                    icon = Icons.Default.Schedule,
                    title = "Track Your Time",
                    description = "See exactly how much time you spend on each app"
                )

                OnboardingBenefitRow(
                    icon = Icons.Default.TrackChanges,
                    title = "Set Healthy Limits",
                    description = "Create realistic goals and stick to them"
                )

                OnboardingBenefitRow(
                    icon = Icons.Default.TrendingUp,
                    title = "Build Better Habits",
                    description = "Track your progress and celebrate achievements"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun FeaturesOverviewScreen() {
    var currentFeature by remember { mutableStateOf(0) }
    val features = remember {
        listOf(
            OnboardingFeature(
                icon = Icons.Default.PhoneAndroid,
                title = "Smart App Monitoring",
                description = "Automatically track your app usage without any manual input",
                color = Color.Blue
            ),
            OnboardingFeature(
                icon = Icons.Default.NotificationsActive,
                title = "Gentle Reminders",
                description = "Get notified when you're approaching your daily limits",
                color = Color(0xFFFF9500) // Orange
            ),
            OnboardingFeature(
                icon = Icons.Default.Shield,
                title = "Mindful Blocking",
                description = "Temporarily block distracting apps when you reach your limits",
                color = Color.Green
            ),
            OnboardingFeature(
                icon = Icons.Default.BarChart,
                title = "Progress Insights",
                description = "See your improvement over time with detailed analytics",
                color = AwayTimeColors.primary
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentFeature = (currentFeature + 1) % features.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How Awaytime Helps",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Discover the features that will transform your digital habits",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Feature carousel
        AnimatedContent(
            targetState = features[currentFeature],
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(500)
                )
            },
            label = "feature_carousel"
        ) { feature ->
            FeatureCard(feature = feature)
        }

        // Feature indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            features.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == currentFeature) {
                                AwayTimeColors.primary
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PermissionsScreen(onboardingManager: OnboardingManager) {
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
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(60.dp)
            )

            Text(
                text = "Privacy & Permissions",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Awaytime needs a few permissions to help you track and manage your app usage",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PermissionCard(
                icon = Icons.Default.Analytics,
                title = "Usage Access",
                description = "Required to monitor your app usage and set limits",
                status = onboardingManager.usageStatsPermissionStatus,
                onRequest = { onboardingManager.requestUsageStatsPermission() }
            )

            PermissionCard(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                description = "Needed to block apps when you reach your limits",
                status = onboardingManager.accessibilityPermissionStatus,
                onRequest = { onboardingManager.requestAccessibilityPermission() }
            )

            PermissionCard(
                icon = Icons.Default.PictureInPicture,
                title = "Display Over Other Apps",
                description = "Required to show the blocking screen over other apps",
                status = onboardingManager.overlayPermissionStatus,
                onRequest = { onboardingManager.requestOverlayPermission() }
            )

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Get helpful reminders and achievement celebrations",
                status = onboardingManager.notificationPermissionStatus,
                onRequest = { onboardingManager.requestNotificationPermission() }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔒 Your Privacy Matters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "All your data stays on your device. We never collect or share your personal usage information.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AppSelectionScreen(onboardingManager: OnboardingManager) {
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

        if (onboardingManager.hasUsageStatsPermission) {
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
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9500).copy(alpha = 0.1f) // Orange
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚠️ Usage Access Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9500)
                    )

                    Text(
                        text = "Please grant Usage Access permission in the previous step to select apps",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@ExperimentalMaterial3Api
@Composable
private fun GoalSettingScreen(onboardingManager: OnboardingManager) {
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

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun CompletionScreen(onboardingManager: OnboardingManager) {
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
                    text = "Awaytime is now ready to help you build healthier digital habits",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OnboardingBenefitRow(
                icon = Icons.Default.TrendingUp,
                title = "Track Your Progress",
                description = "See your daily usage and improvement over time"
            )

            OnboardingBenefitRow(
                icon = Icons.Default.NotificationsActive,
                title = "Stay Mindful",
                description = "Get gentle reminders when approaching your limits"
            )

            OnboardingBenefitRow(
                icon = Icons.Default.EmojiEvents,
                title = "Celebrate Success",
                description = "Earn achievements and build lasting habits"
            )
        }

        Button(
            onClick = { onboardingManager.completeOnboarding() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AwayTimeColors.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Start Your Journey")
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Supporting Composables

@Composable
private fun OnboardingBenefitRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(30.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureCard(feature: OnboardingFeature) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = feature.color,
                modifier = Modifier.size(50.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = feature.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = feature.description,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: PermissionStatus,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AwayTimeColors.primary,
                    modifier = Modifier.size(30.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (status) {
                    PermissionStatus.GRANTED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    PermissionStatus.DENIED -> {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    PermissionStatus.NOT_REQUESTED -> {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    else -> {  Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )}
                }
            }

            if (status == PermissionStatus.NOT_REQUESTED || status == PermissionStatus.DENIED) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.primary
                    )
                ) {
                    Text(
                        text = if (status == PermissionStatus.DENIED) "Open Settings" else "Grant Permission"
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedAppCard(appName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    AwayTimeColors.primary.copy(alpha = 0.2f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📱",
                fontSize = 20.sp
            )
        }

        Text(
            text = appName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
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
        modifier = Modifier.fillMaxWidth(),
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
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = preset.emoji,
                fontSize = 30.sp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = preset.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${preset.minutes} min/day",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = preset.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun OnboardingBottomNavigation(
    onboardingManager: OnboardingManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnboardingStep.values().forEachIndexed { index, step ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (step.ordinal <= onboardingManager.currentStep.ordinal) {
                                AwayTimeColors.primary
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                )

                if (index < OnboardingStep.values().size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onboardingManager.canGoBack) {
                TextButton(
                    onClick = { onboardingManager.goBack() }
                ) {
                    Text(
                        text = "Back",
                        color = AwayTimeColors.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = { onboardingManager.goNext() },
                enabled = onboardingManager.canGoNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.primary
                )
            ) {
                Text(onboardingManager.nextButtonTitle)
            }
        }
    }
}

// Data classes
data class OnboardingFeature(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun SequentialFoxAnimation(
    modifier: Modifier = Modifier,
    animationDuration: Long = 2000L // 2 seconds per animation
) {
    var currentAnimationIndex by remember { mutableStateOf(0) }
    
    val animations = listOf(
        "baby_fox_happy.lottie",
        "baby_fox_sleepy.lottie", 
        "baby_fox_sad.lottie",
        "baby_fox_exhausted.lottie"
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(animationDuration)
            currentAnimationIndex = (currentAnimationIndex + 1) % animations.size
        }
    }
    
    DotLottieAnimation(
        source = DotLottieSource.Asset("lottie/${animations[currentAnimationIndex]}"),
        autoplay = true,
        loop = true,
        speed = 1f,
        modifier = modifier
    )
}

@Composable
private fun PremiumTrialScreen(onboardingManager: OnboardingManager) {
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
                imageVector = Icons.Default.Diamond,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(60.dp)
            )

            Text(
                text = "Unlock Premium Features",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = onboardingManager.getTrialPromotionMessage(),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (onboardingManager.shouldPromoteTrial()) {
            Button(
                onClick = { onboardingManager.startPremiumTrial() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.primary
                )
            ) {
                Text("Start 7-Day Free Trial")
            }

            OutlinedButton(
                onClick = { onboardingManager.skipTrial() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("No Thanks, Continue with Free")
            }
        } else {
            Text(
                text = "You've already had a trial or are currently premium.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}