package com.awaytime.app.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.awaytime.app.service.ImprovedUsageTrackingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsageTrackingTestActivity : ComponentActivity() {
    
    private lateinit var usageTracker: ImprovedUsageTrackingManager
    private lateinit var quickTest: QuickUsageTest
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        usageTracker = ImprovedUsageTrackingManager(this)
        quickTest = QuickUsageTest(this)
        
        setContent {
            UsageTrackingTestScreen()
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UsageTrackingTestScreen() {
        var debugInfo by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
        var userApps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var usageData by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
        var testResult by remember { mutableStateOf<UsageTestResult?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        
        // Auto-refresh every 10 seconds
        LaunchedEffect(Unit) {
            while (true) {
                refreshData { info, apps, usage ->
                    debugInfo = info
                    userApps = apps
                    usageData = usage
                }
                delay(10000)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Usage Tracking Test") }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quick Test Results",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            testResult?.let { result ->
                                Text(
                                    text = result.getSummary(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (result.isWorking) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                                
                                if (result.isWorking) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("📱 User apps: ${result.userAppsCount}")
                                    Text("📊 Apps with usage: ${result.totalAppsWithUsage}")
                                    Text("⏱️ Total usage: ${result.totalUsageMinutes} minutes")
                                }
                            } ?: run {
                                Text(
                                    text = "Tap 'Run Quick Test' to check usage tracking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Debug Information",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            debugInfo.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isLoading = true
                                lifecycleScope.launch {
                                    val result = quickTest.runQuickTest()
                                    testResult = result
                                    
                                    refreshData { info, apps, usage ->
                                        debugInfo = info
                                        userApps = apps
                                        usageData = usage
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Run Quick Test")
                            }
                        }
                        
                        Button(
                            onClick = {
                                usageTracker.requestUsageStatsPermission()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Request Permission")
                        }
                    }
                }
                
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Today's Usage Data",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (usageData.isEmpty()) {
                                Text(
                                    text = "No usage data available. Make sure you have granted usage access permission.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                usageData.forEach { (packageName, usageMs) ->
                                    val appName = userApps[packageName] ?: packageName
                                    val usageMinutes = (usageMs / (1000 * 60)).toInt()
                                    
                                    if (usageMinutes > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = usageTracker.formatUsageTime(usageMinutes),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "User Installed Apps (${userApps.size})",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (userApps.isEmpty()) {
                                Text(
                                    text = "Loading apps...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = "Showing first 10 apps:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                userApps.entries.take(10).forEach { (packageName, appName) ->
                                    Text(
                                        text = "$appName ($packageName)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun refreshData(
        onResult: (Map<String, Any>, Map<String, String>, Map<String, Long>) -> Unit
    ) {
        try {
            val debugInfo = usageTracker.getDebugInfo()
            val userApps = usageTracker.getUserInstalledApps()
            
            // Get usage for top 20 most used apps
            val topApps = userApps.keys.take(20)
            val usageData = usageTracker.getTodayUsageEvents(topApps)
            
            onResult(debugInfo, userApps, usageData)
        } catch (e: Exception) {
            println("❌ Error refreshing data: ${e.message}")
        }
    }
}