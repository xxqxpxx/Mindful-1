@file:OptIn(ExperimentalFoundationApi::class)

package com.awaytime.app.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.awaytime.app.ui.dashboard.DashboardScreen
import com.awaytime.app.presentation.appselection.AppSelectionScreen
import com.awaytime.app.ui.onboarding.OnboardingFlow
import com.awaytime.app.ui.onboarding.ImprovedOnboardingFlow
import com.awaytime.app.ui.premium.PremiumFeaturesScreen
import com.awaytime.app.ui.premium.PaywallScreen
import com.awaytime.app.ui.premium.PremiumFeatureGate
import com.awaytime.app.service.*

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AppSelection : Screen("app_selection")
    object LimitSetting : Screen("limit_setting")
    object Analytics : Screen("analytics")
    object Premium : Screen("premium")
    object FocusSessions : Screen("focus_sessions")
    object SmartCategorization : Screen("smart_categorization")
    object DataExport : Screen("data_export")
    object Paywall : Screen("paywall")
    object Onboarding : Screen("onboarding")
    object Permissions : Screen("permissions")
    object Debug : Screen("debug")
}

@ExperimentalMaterial3Api
@Composable
fun AwayTimeNavigation(
    navController: NavHostController = rememberNavController(),
    permissionService: PermissionService? = null,
    onRequestNotificationPermission: (() -> Unit)? = null,
    onRequestUsageStatsPermission: (() -> Unit)? = null,
    onRequestAccessibilityPermission: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val onboardingCoordinator = remember { ImprovedOnboardingCoordinator(context.applicationContext) }
    
    // Check onboarding status on first composition
    LaunchedEffect(Unit) {
        try {
            onboardingCoordinator.checkOnboardingStatus()
            println("🎯 Navigation: NavController ready, onboarding status checked")
        } catch (e: Exception) {
            println("❌ Navigation: Error checking onboarding status: ${e.message}")
        }
    }
    
    // Determine start destination based on onboarding status
    val startDestination = if (onboardingCoordinator.showOnboarding) {
        Screen.Onboarding.route
    } else {
        Screen.Dashboard.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAppSelection = {
                    try {
                        println("🎯 FIXED: Dashboard navigation to app selection")
                        
                        // Simple, reliable navigation without complex checks
                        navController.navigate(Screen.AppSelection.route) {
                            launchSingleTop = true
                        }
                        println("✅ FIXED: Navigation completed")
                    } catch (e: Exception) {
                        println("❌ FIXED: Navigation error: ${e.message}")
                    }
                },
                onNavigateToLimitSetting = {
                    navController.navigate(Screen.LimitSetting.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                },
                onNavigateToDebug = {
                    navController.navigate(Screen.Debug.route)
                }
            )
        }

        composable(Screen.AppSelection.route) {
            println("🚀 FIXED: Composing App Selection Screen...")
            
            // Clean Mindful app selection screen
            AppSelectionScreen(
                onNavigateBack = { 
                    try {
                        println("🎯 MINDFUL: Navigate back from app selection")
                        navController.popBackStack()
                        println("✅ MINDFUL: Navigation back completed")
                    } catch (e: Exception) {
                        println("❌ MINDFUL: Navigation back error: ${e.message}")
                        // Simple fallback
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.AppSelection.route) { inclusive = true }
                        }
                    }
                }
            )
            println("✅ MINDFUL: App Selection Screen composed successfully!")
        }

        composable(Screen.LimitSetting.route) {
            com.awaytime.app.ui.limitsetting.LimitSettingScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = {
                    // Limit saved successfully
                }
            )
        }

        composable(Screen.Analytics.route) {
            AdvancedAnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Premium.route) {
            PremiumFeaturesScreen(
                onNavigateBack = { navController.popBackStack() },
                onUpgradeClicked = {
                    navController.navigate(Screen.Paywall.route)
                },
                onNavigateToFocusSessions = {
                    navController.navigate(Screen.FocusSessions.route)
                },
                onNavigateToSmartCategorization = {
                    navController.navigate(Screen.SmartCategorization.route)
                },
                onNavigateToDataExport = {
                    navController.navigate(Screen.DataExport.route)
                }
            )
        }

        composable(Screen.FocusSessions.route) {
            FocusSessionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SmartCategorization.route) {
            SmartCategorizationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DataExport.route) {
            DataExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Paywall.route) {
            PaywallScreen(
                onDismiss = { navController.popBackStack() }
            )
        }

        composable(Screen.Onboarding.route) {
            ImprovedOnboardingFlow(
                onComplete = {
                    onboardingCoordinator.onboardingCompleted()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onRequestUsageStatsPermission = onRequestUsageStatsPermission ?: {},
                onRequestAccessibilityPermission = onRequestAccessibilityPermission ?: {},
                onRequestNotificationPermission = onRequestNotificationPermission ?: {},
                onNavigateToAppSelection = {
                    try {
                        println("🎯 FIXED: Onboarding -> App Selection")
                        navController.navigate(Screen.AppSelection.route)
                        println("✅ FIXED: Onboarding navigation completed")
                    } catch (e: Exception) {
                        println("❌ FIXED: Onboarding navigation error: ${e.message}")
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            com.awaytime.app.ui.permission.PermissionRequestScreen(
                onPermissionGranted = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Debug.route) {
            com.awaytime.app.ui.debug.DebugScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Placeholder screens
// AppSelectionScreen moved to separate file

// LimitSettingScreen moved to separate file

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalyticsScreen(onNavigateBack: () -> Unit) {
    PremiumFeatureGate(
        feature = PremiumFeature.ADVANCED_ANALYTICS,
        content = {
            // Real advanced analytics content here
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Advanced Analytics") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 Advanced Analytics Available!\nDetailed insights, AI recommendations, and weekly reports coming soon!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        fallbackContent = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Analytics") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Upgrade to Premium for Advanced Analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionsScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusService = remember { FocusSessionService(context) }
    
    PremiumFeatureGate(
        feature = PremiumFeature.FOCUS_SESSIONS,
        content = {
            // Focus Sessions UI
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Focus Sessions") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎯 Focus Sessions Active!\nPomodoro timer, app blocking, and productivity tracking available!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        fallbackContent = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Focus Sessions") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 Premium Feature\nUpgrade to unlock Focus Sessions with Pomodoro-style productivity!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCategorizationScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categorizationService = remember { SmartCategorizationService(context) }
    
    PremiumFeatureGate(
        feature = PremiumFeature.SMART_APP_CATEGORIZATION,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Smart Categorization") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧠 Smart Categorization Active!\nAI analysis, automatic grouping, and category suggestions available!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        fallbackContent = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Smart Categorization") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖 Premium Feature\nUpgrade to unlock AI-powered app categorization!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportService = remember { DataExportService(context) }
    
    PremiumFeatureGate(
        feature = PremiumFeature.EXPORT_DATA,
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Data Export") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💾 Data Export Active!\nCSV, JSON, PDF, and HTML export formats available!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        fallbackContent = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Data Export") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Premium Feature\nUpgrade to export your data in multiple formats!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

// OnboardingScreen removed - using OnboardingFlow directly