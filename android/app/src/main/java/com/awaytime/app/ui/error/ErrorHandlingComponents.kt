package com.awaytime.app.ui.error

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.awaytime.app.service.ErrorHandlingService
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.delay

// MARK: - Error Dialog

@Composable
fun ErrorDialog(
    error: ErrorHandlingService.AwayTimeError,
    onDismiss: () -> Unit,
    onRecovery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon and title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text(
                        text = error.icon,
                        fontSize = 50.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = error.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                // Error description
                Text(
                    text = error.message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Recovery suggestion
                Surface(
                    color = AwayTimeColors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AwayTimeColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to fix this:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = error.recoverySuggestion,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onRecovery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.primary
                        )
                    ) {
                        Icon(
                            imageVector = getRecoveryIcon(error),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getRecoveryButtonText(error))
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

private fun getRecoveryIcon(error: ErrorHandlingService.AwayTimeError): ImageVector {
    return when (error) {
        is ErrorHandlingService.AwayTimeError.PermissionDenied,
        is ErrorHandlingService.AwayTimeError.PermissionRevoked -> Icons.Default.Settings

        is ErrorHandlingService.AwayTimeError.BlockingFailed -> Icons.Default.Refresh
        is ErrorHandlingService.AwayTimeError.DataCorruption -> Icons.Default.Build
        is ErrorHandlingService.AwayTimeError.NetworkUnavailable -> Icons.Default.Refresh
        is ErrorHandlingService.AwayTimeError.SubscriptionError -> Icons.Default.CreditCard
        is ErrorHandlingService.AwayTimeError.UsageStatsError,
        is ErrorHandlingService.AwayTimeError.AccessibilityError -> Icons.Default.RestartAlt

        is ErrorHandlingService.AwayTimeError.StorageError -> Icons.Default.Storage
        is ErrorHandlingService.AwayTimeError.UnknownError -> Icons.Default.RestartAlt
    }
}

private fun getRecoveryButtonText(error: ErrorHandlingService.AwayTimeError): String {
    return when (error) {
        is ErrorHandlingService.AwayTimeError.PermissionDenied,
        is ErrorHandlingService.AwayTimeError.PermissionRevoked -> "Open Settings"

        is ErrorHandlingService.AwayTimeError.BlockingFailed -> "Try Again"
        is ErrorHandlingService.AwayTimeError.DataCorruption -> "Fix Data"
        is ErrorHandlingService.AwayTimeError.NetworkUnavailable -> "Retry"
        is ErrorHandlingService.AwayTimeError.SubscriptionError -> "Manage Subscription"
        is ErrorHandlingService.AwayTimeError.UsageStatsError,
        is ErrorHandlingService.AwayTimeError.AccessibilityError -> "Restart Service"

        is ErrorHandlingService.AwayTimeError.StorageError -> "Free Up Space"
        is ErrorHandlingService.AwayTimeError.UnknownError -> "Restart App"
    }
}

// MARK: - Data Recovery Dialog

@ExperimentalMaterial3Api
@Composable
fun DataRecoveryDialog(
    onSafeReset: () -> Unit,
    onFullReset: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "💾",
                        fontSize = 50.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Data Recovery Options",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Choose how you'd like to fix your data:",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Recovery options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecoveryOptionCard(
                        icon = Icons.Default.Refresh,
                        title = "Safe Reset",
                        description = "Fix corrupted data while keeping your app selections and goals",
                        recommended = true,
                        onClick = onSafeReset
                    )

                    RecoveryOptionCard(
                        icon = Icons.Default.Delete,
                        title = "Full Reset",
                        description = "Clear all data and start fresh (you'll need to set up again)",
                        recommended = false,
                        onClick = onFullReset
                    )
                }

                // Cancel button
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun RecoveryOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    if (recommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color.Green,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// MARK: - Error Toast

@Composable
fun ErrorToast(
    error: ErrorHandlingService.AwayTimeError,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        isVisible = true
        delay(4000) // Show for 4 seconds
        isVisible = false
        delay(300) // Wait for animation
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = error.severity.color.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error.icon,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = error.title,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        Text(
                            text = error.message,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Permission Guide Dialog

@Composable
fun PermissionGuideDialog(
    permissionType: ErrorHandlingService.PermissionType,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 50.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Permission Required",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Awaytime needs ${permissionType.displayName} permission to help you stay focused.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Step-by-step guide
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Follow these steps:",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val steps = getPermissionSteps(permissionType)
                        steps.forEachIndexed { index, step ->
                            PermissionStepItem(
                                number = index + 1,
                                text = step,
                                modifier = Modifier.padding(bottom = if (index < steps.size - 1) 12.dp else 0.dp)
                            )
                        }
                    }
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
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
                        Text("Open Settings")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I'll Do This Later")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStepItem(
    number: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = AwayTimeColors.primary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

private fun getPermissionSteps(type: ErrorHandlingService.PermissionType): List<String> {
    return when (type) {
        ErrorHandlingService.PermissionType.USAGE_STATS -> listOf(
            "Tap 'Open Settings' below",
            "Find Awaytime in the list",
            "Toggle the switch to enable",
            "Return to Awaytime"
        )

        ErrorHandlingService.PermissionType.ACCESSIBILITY -> listOf(
            "Tap 'Open Settings' below",
            "Find Awaytime in the list",
            "Tap on Awaytime",
            "Turn on the service"
        )

        ErrorHandlingService.PermissionType.NOTIFICATIONS -> listOf(
            "Tap 'Open Settings' below",
            "Go to Notifications",
            "Enable 'Show notifications'",
            "Return to Awaytime"
        )

        ErrorHandlingService.PermissionType.OVERLAY -> listOf(
            "Tap 'Open Settings' below",
            "Find Awaytime in the list",
            "Enable 'Display over other apps'",
            "Return to Awaytime"
        )
    }
}

// MARK: - Error Status Banner

@ExperimentalMaterial3Api
@Composable
fun ErrorStatusBanner(
    error: ErrorHandlingService.AwayTimeError,
    onTap: () -> Unit
) {
    Card(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = error.severity.color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            error.severity.color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error.icon,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = error.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                Text(
                    text = "Tap to resolve",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// MARK: - Loading with Error Fallback

@Composable
fun <T> LoadingWithErrorView(
    isLoading: Boolean,
    error: ErrorHandlingService.AwayTimeError?,
    onRetry: () -> Unit,
    content: @Composable () -> T
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = AwayTimeColors.primary,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Loading...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(40.dp)
                ) {
                    Text(
                        text = error.icon,
                        fontSize = 40.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = error.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = error.message,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.primary
                        )
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }

        else -> {
            content()
        }
    }
}