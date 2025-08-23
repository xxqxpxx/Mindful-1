package com.awaytime.app.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.core.content.ContextCompat
import com.awaytime.app.service.PermissionService
import com.awaytime.app.service.PermissionStatus
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.delay

/**
 * Senior Android Engineer Permission Management Component
 * Provides comprehensive permission handling with excellent UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementCard(
    permissionService: PermissionService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var lastRefresh by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Permission states - Using safer state management to prevent segfaults
    var usageStatsPermission by remember { mutableStateOf(false) }
    var accessibilityPermission by remember { mutableStateOf(false) }
    var notificationPermission by remember { mutableStateOf(true) } // Default true for older Android
    var overlayPermission by remember { mutableStateOf(true) } // Default true for older Android
    
    // Safe permission checking with proper error handling
    LaunchedEffect(lastRefresh) {
        try {
            usageStatsPermission = permissionService.hasUsageStatsPermission()
            accessibilityPermission = permissionService.hasAccessibilityPermission()
            notificationPermission = permissionService.hasNotificationPermission()
            overlayPermission = permissionService.hasOverlayPermission()
        } catch (e: Exception) {
            println("❌ Error checking permissions safely: ${e.message}")
            // Keep previous states on error to prevent crashes
        }
    }
    
    // Calculate permission summary
    val totalPermissions = 4
    val grantedPermissions = listOf(
        usageStatsPermission,
        accessibilityPermission, 
        notificationPermission,
        overlayPermission
    ).count { it }
    
    val isFullySetup = grantedPermissions == totalPermissions
    val needsAttention = grantedPermissions < 2 // Critical permissions missing
    
    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Notification permission granted")
        } else {
            println("❌ Notification permission denied")
        }
        // Refresh permission states
        lastRefresh = System.currentTimeMillis()
    }
    
    // Auto-refresh permissions periodically when card is expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            while (isExpanded) {
                delay(2000) // Check every 2 seconds
                permissionService.updatePermissionStatuses()
                lastRefresh = System.currentTimeMillis()
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFullySetup -> Color(0xFFF0F9FF) // Light blue
                needsAttention -> Color(0xFFFEF2F2) // Light red
                else -> Color(0xFFFFFBEB) // Light yellow
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when {
                isFullySetup -> Color(0xFF3B82F6) // Blue
                needsAttention -> Color(0xFFEF4444) // Red
                else -> Color(0xFFF59E0B) // Yellow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFullySetup) Icons.Default.CheckCircle 
                                      else if (needsAttention) Icons.Default.Warning
                                      else Icons.Default.Settings,
                        contentDescription = null,
                        tint = when {
                            isFullySetup -> Color(0xFF10B981) // Green
                            needsAttention -> Color(0xFFEF4444) // Red
                            else -> Color(0xFFF59E0B) // Yellow
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = if (isFullySetup) "All Permissions Granted!" 
                                   else "App Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "$grantedPermissions of $totalPermissions permissions granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Expand/Collapse button
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp 
                                      else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Progress bar
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = grantedPermissions.toFloat() / totalPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    isFullySetup -> Color(0xFF10B981) // Green
                    needsAttention -> Color(0xFFEF4444) // Red
                    else -> Color(0xFFF59E0B) // Yellow
                },
                trackColor = Color(0xFFE5E7EB)
            )
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    if (!isFullySetup) {
                        Text(
                            text = "🚀 Grant permissions to unlock full functionality",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AwayTimeColors.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Individual permission items
                    PermissionItem(
                        title = "Usage Stats",
                        description = "Required to track app usage and enforce limits",
                        isGranted = usageStatsPermission,
                        isCritical = true,
                        icon = Icons.Default.BarChart,
                        onRequestPermission = {
                            openUsageStatsSettings(context)
                        }
                    )
                    
                    PermissionItem(
                        title = "Accessibility Service",
                        description = "Required to block apps when limits are reached",
                        isGranted = accessibilityPermission,
                        isCritical = true,
                        icon = Icons.Default.Accessibility,
                        onRequestPermission = {
                            openAccessibilitySettings(context)
                        }
                    )
                    
                    PermissionItem(
                        title = "Notifications",
                        description = "Shows daily usage summaries and limit warnings",
                        isGranted = notificationPermission,
                        isCritical = false,
                        icon = Icons.Default.Notifications,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    
                    PermissionItem(
                        title = "Display over other apps",
                        description = "Optional: Shows blocking overlay when limits are reached",
                        isGranted = overlayPermission,
                        isCritical = false,
                        icon = Icons.Default.OpenInNew,
                        onRequestPermission = {
                            openOverlaySettings(context)
                        }
                    )
                    
                    if (!isFullySetup) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                openAppSettings(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AwayTimeColors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    isCritical: Boolean,
    icon: ImageVector,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                Color(0xFFF0FDF4) // Light green
            } else if (isCritical) {
                Color(0xFFFEF2F2) // Light red
            } else {
                Color(0xFFFAFAFA) // Light gray
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isGranted) Color(0xFF10B981)
                    else if (isCritical) Color(0xFFEF4444)
                    else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981)
                      else if (isCritical) Color(0xFFEF4444)
                      else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isCritical) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .background(
                                    Color(0xFFEF4444).copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AwayTimeColors.primary
                    )
                ) {
                    Text(
                        text = "Grant",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Helper functions to open settings
private fun openUsageStatsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        println("❌ Failed to open usage stats settings: ${e.message}")
        // Fallback to app settings
        openAppSettings(context)
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        println("❌ Failed to open accessibility settings: ${e.message}")
        // Fallback to app settings
        openAppSettings(context)
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        println("❌ Failed to open overlay settings: ${e.message}")
        // Fallback to app settings
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        println("❌ Failed to open app settings: ${e.message}")
    }
}
