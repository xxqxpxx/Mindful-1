package com.awaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awaytime.app.service.AccessibilityServiceMonitor
import com.awaytime.app.ui.theme.AwayTimeColors

@Composable
fun AccessibilityServiceStatus(
    modifier: Modifier = Modifier,
    showWhenEnabled: Boolean = false
) {
    val context = LocalContext.current
    val monitor = remember { AccessibilityServiceMonitor.getInstance(context) }
    
    val isServiceEnabled by monitor.isServiceEnabled.observeAsState(initial = false)
    val statusMessage by monitor.serviceStateMessage.observeAsState(initial = "Checking...")
    
    // Start monitoring when component is composed
    LaunchedEffect(Unit) {
        monitor.startMonitoring()
    }
    
    // Only show if service is disabled or if explicitly requested to show when enabled
    if (!isServiceEnabled || showWhenEnabled) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) {
                    AwayTimeColors.success.copy(alpha = 0.1f)
                } else {
                    AwayTimeColors.warning.copy(alpha = 0.1f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon
                Icon(
                    imageVector = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isServiceEnabled) "Service Active" else "Service Disabled",
                    tint = if (isServiceEnabled) AwayTimeColors.success else AwayTimeColors.warning,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Status Text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isServiceEnabled) "App Blocking Active" else "App Blocking Disabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isServiceEnabled) AwayTimeColors.success else AwayTimeColors.warning
                    )
                    
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action Button (only show when disabled)
                if (!isServiceEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { monitor.openAccessibilitySettings() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Enable",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibilityServiceBanner(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val monitor = remember { AccessibilityServiceMonitor.getInstance(context) }
    
    val isServiceEnabled by monitor.isServiceEnabled.observeAsState(initial = true)
    
    // Only show banner when service is disabled
    if (!isServiceEnabled) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AwayTimeColors.error.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AwayTimeColors.error,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "App Blocking Not Working",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AwayTimeColors.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "The accessibility service required for app blocking has been disabled. " +
                            "Please enable it in Settings to continue blocking apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { monitor.openAccessibilitySettings() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AwayTimeColors.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Settings")
                    }
                    
                    if (onDismiss != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibilityServiceIndicator(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val monitor = remember { AccessibilityServiceMonitor.getInstance(context) }
    
    val isServiceEnabled by monitor.isServiceEnabled.observeAsState(initial = false)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isServiceEnabled) AwayTimeColors.success else AwayTimeColors.error,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = if (isServiceEnabled) "Active" else "Disabled",
            style = MaterialTheme.typography.labelSmall,
            color = if (isServiceEnabled) AwayTimeColors.success else AwayTimeColors.error
        )
    }
}