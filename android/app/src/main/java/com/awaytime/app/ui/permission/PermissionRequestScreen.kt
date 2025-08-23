package com.awaytime.app.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.PermissionService
import com.awaytime.app.service.PermissionStatus
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.ui.theme.AwayTimeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    onPermissionGranted: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionService = remember { PermissionService(context) }

    // Update permission status when screen becomes visible
    LaunchedEffect(Unit) {
        permissionService.updatePermissionStatuses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                actions = {
                    if (permissionService.isFullyAuthorized) {
                        TextButton(
                            onClick = {
                                onPermissionGranted()
                                onDismiss()
                            }
                        ) {
                            Text(
                                "Done",
                                color = AwayTimeColors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            HeaderSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Permission explanation
            ExplanationSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Usage Stats Permission
            PermissionCard(
                title = "Usage Access",
                description = permissionService.getUsageStatsGuidanceText(),
                status = permissionService.usageStatsPermissionStatus,
                statusText = permissionService.getUsageStatsStatusText(),
                icon = Icons.Default.BarChart,
                onRequestPermission = {
                    permissionService.requestUsageStatsPermission()
                },
                isRequesting = permissionService.isRequestingPermission
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Accessibility Permission
            PermissionCard(
                title = "Accessibility Service",
                description = permissionService.getAccessibilityGuidanceText(),
                status = permissionService.accessibilityPermissionStatus,
                statusText = permissionService.getAccessibilityStatusText(),
                icon = Icons.Default.Accessibility,
                onRequestPermission = {
                    permissionService.requestAccessibilityPermission()
                },
                isRequesting = permissionService.isRequestingPermission
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error message
            permissionService.permissionError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AwayTimeColors.warning.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = AwayTimeColors.warning,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action buttons
            ActionButtonsSection(
                isFullyAuthorized = permissionService.isFullyAuthorized,
                onContinue = {
                    onPermissionGranted()
                    onDismiss()
                },
                onMaybeLater = onDismiss
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "App Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AwayTimeColors.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExplanationSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Let's set up Awaytime! We need permission to help you! 💜",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Awaytime needs these permissions to track your app usage and block apps when you reach your limits.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    status: PermissionStatus,
    statusText: String,
    icon: ImageVector,
    onRequestPermission: () -> Unit,
    isRequesting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AwayTimeColors.primary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getStatusIcon(status),
                            contentDescription = null,
                            tint = getStatusColor(status),
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = getStatusColor(status)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            if (status != PermissionStatus.GRANTED) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRequesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AwayTimeColors.primary
                    )
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Opening Settings...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    isFullyAuthorized: Boolean,
    onContinue: () -> Unit,
    onMaybeLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isFullyAuthorized) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.success
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Continue",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onMaybeLater,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Maybe Later",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getStatusIcon(status: PermissionStatus): ImageVector {
    return when (status) {
        PermissionStatus.NOT_DETERMINED -> Icons.Default.Help
        PermissionStatus.DENIED -> Icons.Default.Cancel
        PermissionStatus.GRANTED -> Icons.Default.CheckCircle
        else -> {Icons.Default.Cancel}
    }
}

private fun getStatusColor(status: PermissionStatus): Color {
    return when (status) {
        PermissionStatus.NOT_DETERMINED -> AwayTimeColors.warning
        PermissionStatus.DENIED -> Color.Red
        PermissionStatus.GRANTED -> AwayTimeColors.success
        else -> {Color.Red}
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestScreenPreview() {
    AwayTimeTheme {
        PermissionRequestScreen()
    }
}