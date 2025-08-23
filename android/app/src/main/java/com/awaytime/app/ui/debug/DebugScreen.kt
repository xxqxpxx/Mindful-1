package com.awaytime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.DebugUtilities
import com.awaytime.app.ui.theme.AwayTimeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val debugUtilities = remember { DebugUtilities(context) }
    val scope = rememberCoroutineScope()
    
    var debugOutput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Console") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AwayTimeColors.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Action buttons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    DebugButton(
                        text = "Full Report",
                        icon = Icons.Default.Assessment,
                        enabled = !isLoading
                    ) {
                        scope.launch {
                            isLoading = true
                            debugOutput = debugUtilities.fullDebugReport()
                            isLoading = false
                        }
                    }
                }
                
                item {
                    DebugButton(
                        text = "App Selection",
                        icon = Icons.Default.Apps,
                        enabled = !isLoading
                    ) {
                        scope.launch {
                            isLoading = true
                            debugOutput = debugUtilities.debugAppSelection()
                            isLoading = false
                        }
                    }
                }
                
                item {
                    DebugButton(
                        text = "Usage Tracking",
                        icon = Icons.Default.Timeline,
                        enabled = !isLoading
                    ) {
                        scope.launch {
                            isLoading = true
                            debugOutput = debugUtilities.debugUsageTracking()
                            isLoading = false
                        }
                    }
                }
                
                item {
                    DebugButton(
                        text = "Permissions",
                        icon = Icons.Default.Security,
                        enabled = !isLoading
                    ) {
                        scope.launch {
                            isLoading = true
                            debugOutput = debugUtilities.debugPermissions()
                            isLoading = false
                        }
                    }
                }
                
                item {
                    DebugButton(
                        text = "Auto-Fix",
                        icon = Icons.Default.Build,
                        enabled = !isLoading,
                        color = AwayTimeColors.success
                    ) {
                        scope.launch {
                            isLoading = true
                            debugOutput = debugUtilities.autoFix()
                            isLoading = false
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Output area
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = AwayTimeColors.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Running diagnostics...")
                        }
                    }
                } else if (debugOutput.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = AwayTimeColors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select a debug option above",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This will help diagnose app selection and usage tracking issues",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = debugOutput,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    color: androidx.compose.ui.graphics.Color = AwayTimeColors.primary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LazyRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}