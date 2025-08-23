package com.awaytime.app.ui.dashboard

// import com.awaytime.app.BuildConfig // Temporarily disabled
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awaytime.app.service.GoalTrackingService
import com.awaytime.app.service.PermissionService
import com.awaytime.app.service.PremiumFeature
import com.awaytime.app.service.PremiumFeatureManager
import com.awaytime.app.ui.components.BabyFoxMascot
import com.awaytime.app.ui.components.LoadingProgressCircle
import com.awaytime.app.ui.components.SkeletonBox
import com.awaytime.app.ui.components.SkeletonText
import com.awaytime.app.ui.components.SmoothNumberTransition
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.ui.theme.AwayTimeTheme
import com.awaytime.app.viewmodel.DashboardViewModel
import com.awaytime.app.viewmodel.DashboardUiState
import androidx.compose.foundation.background
import com.awaytime.app.service.EnhancedServiceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = run {
        val context = LocalContext.current
        viewModel { DashboardViewModel(context) }
    },
    onNavigateToAppSelection: () -> Unit = {},
    onNavigateToLimitSetting: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToBedtime: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionService = remember { PermissionService(context) }
    val goalTrackingService = remember { GoalTrackingService(context) }

    // Setup dashboard on screen load with proper coroutine management
    LaunchedEffect(Unit) {
        try {
            // Add delay to prevent immediate UI blocking
            kotlinx.coroutines.delay(100)

            // Use SupervisorJob to prevent child failures from canceling other operations
            val supervisorJob = kotlinx.coroutines.SupervisorJob()

            // Refresh data when returning to dashboard (e.g., from app selection)
            launch(Dispatchers.IO + supervisorJob) {
                try {
                    viewModel.refreshData()
                } catch (e: Exception) {
                    println("❌ Error refreshing data: ${e.message}")
                    e.printStackTrace()
                }
            }

            launch(Dispatchers.IO + supervisorJob) {
                try {
                    viewModel.forceRefreshUsage()
                } catch (e: Exception) {
                    println("❌ Error refreshing usage: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Initial permission check on background thread with timeout
            launch(Dispatchers.IO + supervisorJob) {
                try {
                    kotlinx.coroutines.withTimeout(5000L) { // 5 second timeout
                        permissionService.updatePermissionStatuses()
                        if (permissionService.needsAnyPermission) {
                            println("⚠️ Some permissions are missing but user can still access app selection")
                        }
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    println("⚠️ Permission check timed out - continuing without blocking")
                } catch (e: Exception) {
                    println("❌ Error checking permissions: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            println("❌ Error in dashboard setup: ${e.message}")
            e.printStackTrace()
        }
    }
    val uiState by viewModel.uiState.collectAsState()

    // Memoize expensive calculations to prevent unnecessary recomposition
    val memoizedUsageProgress = remember(uiState.todayUsageMinutes, uiState.dailyLimitMinutes) {
        uiState.usageProgress
    }

    val memoizedTimeRemaining = remember(uiState.todayUsageMinutes, uiState.dailyLimitMinutes) {
        uiState.timeRemainingText
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section with Baby Fox Mascot
        HeaderSection(usageProgress = memoizedUsageProgress)

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Progress Section
        EnhancedProgressSection(
            progress = memoizedUsageProgress,
            timeRemainingText = memoizedTimeRemaining,
            timeRemainingMinutes = uiState.timeRemainingMinutes,
            currentUsage = uiState.todayUsageMinutes,
            dailyLimit = uiState.dailyLimitMinutes,
            goalTrackingService = goalTrackingService,
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Stats Section
        EnhancedStatsSection(
            goalTrackingService = goalTrackingService,
            onNavigateToAnalytics = onNavigateToAnalytics
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission Management Section
        PermissionManagementCard(
            permissionService = permissionService,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Features Section
        EnhancedFeaturesSection(
            uiState = uiState,
            viewModel = viewModel,
            onNavigateToBedtime = onNavigateToBedtime,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Section
        ActionButtonsSection(
            dailyLimitMinutes = uiState.dailyLimitMinutes,
            isPremium = uiState.isPremium,
            onNavigateToAppSelection = onNavigateToAppSelection,
            onNavigateToLimitSetting = onNavigateToLimitSetting,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToPremium = onNavigateToPremium,
            onNavigateToDebug = onNavigateToDebug
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Premium Features Demo Section
        PremiumFeaturesDemoSection(
            onNavigateToPremium = onNavigateToPremium
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HeaderSection(usageProgress: Float) {
    // Memoize the mascot to prevent excessive recomposition
    val mascotUsagePercent = remember(usageProgress) {
        (usageProgress * 100f).coerceIn(0f, 100f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 20.dp)
    ) {
        // Baby Fox Mascot at the top
        BabyFoxMascot(
            usagePercent = mascotUsagePercent,
            modifier = Modifier.padding(bottom = 16.dp),
            size = 120
        )

        Text(
            text = "Awaytime",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AwayTimeColors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Take control of your screen time",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EnhancedProgressSection(
    progress: Float,
    timeRemainingText: String,
    timeRemainingMinutes: Int,
    currentUsage: Int,
    dailyLimit: Int,
    goalTrackingService: GoalTrackingService,
    isLoading: Boolean = false
) {
    val progressTextColor = remember(progress) {
        when {
            progress <= 0.5f -> AwayTimeColors.success
            progress <= 0.8f -> AwayTimeColors.primary
            else -> AwayTimeColors.warning
        }
    }

    val timeRemainingColor = remember(timeRemainingMinutes) {
        when {
            timeRemainingMinutes > 60 -> AwayTimeColors.success
            timeRemainingMinutes > 30 -> AwayTimeColors.primary
            else -> AwayTimeColors.warning
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        // Enhanced Progress Circle with loading state
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            if (isLoading) {
                LoadingProgressCircle(size = 200.dp, strokeWidth = 12.dp)
            } else {
                com.awaytime.app.ui.components.EnhancedProgressCircle(
                    progress = progress,
                    size = 200.dp,
                    strokeWidth = 12.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SmoothNumberTransition(
                        targetValue = (progress * 100).toInt(),
                        formatter = { "$it%" },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = progressTextColor,
                        modifier = Modifier.semantics {
                            contentDescription =
                                "Usage progress: ${(progress * 100).toInt()} percent used today"
                        }
                    )

                    Text(
                        text = "used today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Time remaining with better styling and loading state
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                SkeletonText(width = 150.dp, height = 20.dp)
            } else {
                Text(
                    text = timeRemainingText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = timeRemainingColor,
                    modifier = Modifier.semantics {
                        contentDescription = "Time remaining: $timeRemainingText"
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Goal recommendation with loading state
            if (isLoading) {
                SkeletonBox(
                    width = 280.dp,
                    height = 60.dp,
                    cornerRadius = 12.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                val recommendation =
                    goalTrackingService.getGoalRecommendation(currentUsage, dailyLimit)
                com.awaytime.app.ui.components.GoalRecommendationCard(
                    recommendation = recommendation,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedStatsSection(
    goalTrackingService: GoalTrackingService,
    onNavigateToAnalytics: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Streak visualization
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            com.awaytime.app.ui.components.StreakVisualization(
                currentStreak = goalTrackingService.currentStreak,
                longestStreak = goalTrackingService.longestStreak,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Weekly progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = onNavigateToAnalytics
        ) {
            com.awaytime.app.ui.components.WeeklyProgressChart(
                weeklyData = goalTrackingService.weeklyProgress,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    dailyLimitMinutes: Int,
    isPremium: Boolean,
    onNavigateToAppSelection: () -> Unit,
    onNavigateToLimitSetting: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isNavigatingToAppSelection by remember { mutableStateOf(false) }

    // Reset navigation state if stuck
    LaunchedEffect(isNavigatingToAppSelection) {
        if (isNavigatingToAppSelection) {
            kotlinx.coroutines.delay(5000) // Reset after 5 seconds if stuck
            if (isNavigatingToAppSelection) {
                println("⚠️ Navigation seems stuck, resetting state")
                isNavigatingToAppSelection = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        ActionButton(
            title = "Select Apps",
            subtitle = "Choose apps to monitor",
            icon = Icons.Default.Apps,
            color = AwayTimeColors.primary,
            isLoading = isNavigatingToAppSelection,
            onClick = {
                if (!isNavigatingToAppSelection) {
                    isNavigatingToAppSelection = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    scope.launch {
                        try {
                            println("🚀 FIXED: Navigate to app selection")
                            onNavigateToAppSelection()
                            println("✅ FIXED: Navigation completed")
                        } catch (e: Exception) {
                            println("❌ FIXED: Navigation error: ${e.message}")
                        } finally {
                            kotlinx.coroutines.delay(1000) // Brief delay to prevent rapid clicks
                            isNavigatingToAppSelection = false
                        }
                    }
                }
            }
        )

        ActionButton(
            title = "Set Daily Limit",
            subtitle = "Current: ${dailyLimitMinutes / 60}h ${dailyLimitMinutes % 60}m",
            icon = Icons.Default.Schedule,
            color = AwayTimeColors.accent,
            onClick = onNavigateToLimitSetting
        )

        if (isPremium) {
            ActionButton(
                title = "View Analytics",
                subtitle = "Detailed usage insights",
                icon = Icons.Default.BarChart,
                color = AwayTimeColors.secondary,
                onClick = onNavigateToAnalytics
            )
        } else {
            ActionButton(
                title = "Upgrade to Premium",
                subtitle = "Unlock advanced features",
                icon = Icons.Default.Star,
                color = AwayTimeColors.warning,
                onClick = onNavigateToPremium
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        onClick = {
            if (!isLoading) {
                onClick()
            }
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = color,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedFeaturesSection(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    onNavigateToBedtime: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Service Status Card
        uiState.enhancedServiceStatus?.let { status ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (status.anyServiceRunning()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (status.anyServiceRunning()) Icons.Default.Shield else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (status.anyServiceRunning()) AwayTimeColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Enhanced Protection Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Service status indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServiceStatusChip(
                            label = "VPN",
                            isActive = status.vpnServiceRunning,
                            modifier = Modifier.weight(1f)
                        )
                        ServiceStatusChip(
                            label = "Focus",
                            isActive = status.focusSessionActive,
                            modifier = Modifier.weight(1f)
                        )
                        ServiceStatusChip(
                            label = "Smart Notifications",
                            isActive = status.notificationServiceRunning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Second row for bedtime and additional services
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServiceStatusChip(
                            label = "Bedtime Mode",
                            isActive = status.bedtimeActive,
                            modifier = Modifier.weight(1f)
                        )
                        // Placeholder for future services
                        Spacer(modifier = Modifier.weight(2f))
                    }
                    
                    if (uiState.isFocusSessionActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AwayTimeColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Focus session: ${uiState.focusSessionTimeRemaining} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = AwayTimeColors.primary
                            )
                        }
                    }
                }
            }
        }
        
        // Focus Session Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AwayTimeColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Focus Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (uiState.isFocusSessionActive) {
                    // Active session controls
                    OutlinedButton(
                        onClick = { viewModel.stopFocusSession() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AwayTimeColors.warning
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Focus Session")
                    }
                } else {
                    // Start session controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startProductivitySession(25) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AwayTimeColors.primary
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("25min Work", fontSize = 12.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.startFocusSession(60) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("1hr Focus", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // Bedtime Mode Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = AwayTimeColors.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Bedtime Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Status indicator
                    if (uiState.enhancedServiceStatus?.bedtimeActive == true) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AwayTimeColors.secondary.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AwayTimeColors.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configure sleep schedule and restrict distracting apps during bedtime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onNavigateToBedtime,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Bedtime")
                }
            }
        }
        
        // Enhanced Blocking Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AwayTimeColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Enhanced Blocking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.applySocialMediaPreset() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.secondary
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Block Social", fontSize = 12.sp)
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.enableContentFiltering() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.accent
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Filter Content", fontSize = 12.sp)
                        }
                    }
                }
                
                if (uiState.enhancedBlockingEnabled || uiState.contentFilteringEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.emergencyDisableAll() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AwayTimeColors.warning
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Disable All")
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusChip(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AwayTimeColors.success.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (isActive) AwayTimeColors.success else MaterialTheme.colorScheme.outline,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) AwayTimeColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PremiumFeaturesDemoSection(
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val premiumFeatureManager = remember { PremiumFeatureManager.getInstance(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Premium Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🎯 Focus Sessions • 🤖 Smart Categorization • 📊 Advanced Analytics • 💾 Data Export",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick access buttons for key premium features
                Button(
                    onClick = onNavigateToPremium,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (premiumFeatureManager.isPremiumActive.value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (premiumFeatureManager.isPremiumActive.value)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Upgrade,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (premiumFeatureManager.isPremiumActive.value)
                                "Explore Features"
                            else
                                "Get Premium",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        // Demo action - show some premium feature info
                        println("🚀 Premium demo: ${premiumFeatureManager.getPremiumBenefitsList().size} features available!")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try Demo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!premiumFeatureManager.isPremiumActive.value) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✨ ${premiumFeatureManager.getPremiumBenefitsList().size} premium features available",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    AwayTimeTheme {
        DashboardScreen()
    }
}
