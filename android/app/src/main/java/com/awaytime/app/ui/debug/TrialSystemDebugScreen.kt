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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.*
import com.awaytime.app.testing.TrialSystemTest
import com.awaytime.app.ui.components.*
import kotlinx.coroutines.launch

/**
 * Debug screen for testing and demonstrating the premium trial system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialSystemDebugScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Trial system services
    val trialManager = remember { PremiumTrialManager.getInstance(context) }
    val premiumFeatureManager = remember { PremiumFeatureManager.getInstance(context) }
    
    // State
    val trialStatus by trialManager.rememberTrialStatus()
    val daysRemaining by trialManager.rememberDaysRemaining()
    val isTrialActive by trialManager.rememberIsTrialActive()
    val isPremiumActive by premiumFeatureManager.rememberIsPremiumActive()
    
    var testResults by remember { mutableStateOf<String?>(null) }
    var isRunningTests by remember { mutableStateOf(false) }
    var showDebugInfo by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trial System Debug") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDebugInfo = !showDebugInfo }
                    ) {
                        Icon(
                            imageVector = if (showDebugInfo) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Debug Info"
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trial Status Components Demo
            TrialStatusCard(
                trialStatus = trialStatus,
                daysRemaining = daysRemaining,
                isTrialActive = isTrialActive,
                isPremiumActive = isPremiumActive
            )
            
            // UI Components Demo
            TrialComponentsDemo()
            
            // Trial Controls
            TrialControlPanel(
                trialManager = trialManager,
                onStateChanged = { /* State will update automatically */ }
            )
            
            // Test Runner
            TrialTestRunner(
                isRunning = isRunningTests,
                results = testResults,
                onRunTests = {
                    isRunningTests = true
                    scope.launch {
                        try {
                            val test = TrialSystemTest(context)
                            val results = test.runAllTests()
                            testResults = test.generateTestReport(results)
                        } catch (e: Exception) {
                            testResults = "Test failed: ${e.message}"
                        } finally {
                            isRunningTests = false
                        }
                    }
                },
                onRunSmokeTest = {
                    isRunningTests = true
                    scope.launch {
                        try {
                            val test = TrialSystemTest(context)
                            val success = test.quickSmokeTest()
                            testResults = if (success) {
                                "✅ Quick smoke test PASSED"
                            } else {
                                "❌ Quick smoke test FAILED"
                            }
                        } catch (e: Exception) {
                            testResults = "Smoke test failed: ${e.message}"
                        } finally {
                            isRunningTests = false
                        }
                    }
                }
            )
            
            // Debug Information
            if (showDebugInfo) {
                DebugInfoCard(trialManager = trialManager)
            }
        }
    }
}

@Composable
private fun TrialStatusCard(
    trialStatus: TrialStatus,
    daysRemaining: Int,
    isTrialActive: Boolean,
    isPremiumActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Trial Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status:")
                Text(
                    text = trialStatus.name,
                    fontWeight = FontWeight.Medium,
                    color = when (trialStatus) {
                        TrialStatus.ACTIVE -> Color(0xFF4CAF50)
                        TrialStatus.EXPIRED -> Color(0xFFFF5722)
                        TrialStatus.ENDED -> Color(0xFF9E9E9E)
                        TrialStatus.NOT_STARTED -> Color(0xFF2196F3)
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Trial Active:")
                Text(
                    text = if (isTrialActive) "YES" else "NO",
                    fontWeight = FontWeight.Medium,
                    color = if (isTrialActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Premium Active:")
                Text(
                    text = if (isPremiumActive) "YES" else "NO",
                    fontWeight = FontWeight.Medium,
                    color = if (isPremiumActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Days Remaining:")
                Text(
                    text = daysRemaining.toString(),
                    fontWeight = FontWeight.Medium,
                    color = when {
                        daysRemaining > 3 -> Color(0xFF4CAF50)
                        daysRemaining > 0 -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                )
            }
        }
    }
}

@Composable
private fun TrialComponentsDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "UI Components Demo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Trial Status Banner
            TrialStatusBanner(
                onUpgradeClick = { /* Demo */ },
                onDismiss = { /* Demo */ }
            )
            
            // Trial Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status Indicator: ")
                TrialStatusIndicator(
                    onClick = { /* Demo */ }
                )
            }
            
            // Trial Expired Notice
            TrialExpiredNotice(
                onUpgradeClick = { /* Demo */ },
                onDismiss = { /* Demo */ }
            )
        }
    }
}

@Composable
private fun TrialControlPanel(
    trialManager: PremiumTrialManager,
    onStateChanged: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Trial Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        trialManager.activateTrial()
                        onStateChanged()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Activate Trial")
                }
                
                Button(
                    onClick = {
                        trialManager.endTrial()
                        onStateChanged()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("End Trial")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        trialManager.resetTrial()
                        onStateChanged()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Trial")
                }
                
                OutlinedButton(
                    onClick = {
                        trialManager.extendTrial(1)
                        onStateChanged()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Extend +1d")
                }
            }
        }
    }
}

@Composable
private fun TrialTestRunner(
    isRunning: Boolean,
    results: String?,
    onRunTests: () -> Unit,
    onRunSmokeTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Test Runner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunSmokeTest,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Quick Test")
                    }
                }
                
                Button(
                    onClick = onRunTests,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Full Tests")
                    }
                }
            }
            
            if (results != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = results,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugInfoCard(
    trialManager: PremiumTrialManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Debug Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = trialManager.getTrialDebugInfo(),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}