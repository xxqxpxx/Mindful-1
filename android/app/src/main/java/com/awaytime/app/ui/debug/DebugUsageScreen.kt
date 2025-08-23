package com.awaytime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.service.PermissionService
import com.awaytime.app.service.UsageTrackingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugUsageScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val repository = remember { AwayTimeRepository(context) }
    val usageTrackingService = remember { UsageTrackingService(context) }
    val permissionService = remember { PermissionService(context) }
    
    var debugInfo by remember { mutableStateOf(listOf<String>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    fun refreshDebugInfo() {
        scope.launch {
            isRefreshing = true
            val info = mutableListOf<String>()
            
            try {
                // Check permissions
                info.add("🔐 Permissions:")
                info.add("  Usage Stats: ${if (permissionService.hasUsageStatsPermission()) "✅" else "❌"}")
                info.add("  Accessibility: ${if (permissionService.hasAccessibilityPermission()) "✅" else "❌"}")
                info.add("  Notifications: ${if (permissionService.hasNotificationPermission()) "✅" else "❌"}")
                info.add("")
                
                // Check monitoring status
                info.add("📊 Monitoring Status:")
                info.add("  Active: ${if (usageTrackingService.isMonitoringActive()) "✅" else "❌"}")
                info.add("")
                
                // Check app groups
                val appGroups = repository.getAllAppGroups().first()
                info.add("📱 App Groups: ${appGroups.size}")
                
                for (group in appGroups) {
                    info.add("  - ${group.name}:")
                    info.add("    Limit: ${group.dailyLimitMinutes} minutes")
                    info.add("    Active: ${group.isActive}")
                    info.add("    Apps: ${group.getSelectedApps().size}")
                    
                    val todayUsage = repository.getTodayUsage(group.name)
                    info.add("    Today's usage: $todayUsage minutes")
                    
                    if (group.isActive) {
                        val currentUsage = usageTrackingService.getCurrentUsage(group.name)
                        info.add("    Real-time usage: $currentUsage minutes")
                    }
                    info.add("")
                }
                
                // Check recent usage records
                val usageRecords = repository.getUsageRecordsSync(1) // Today only
                info.add("💾 Usage Records (today): ${usageRecords.size}")
                
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val recentRecords = usageRecords.take(5)
                for (record in recentRecords) {
                    info.add("  ${formatter.format(record.date)}: ${record.usageMinutes}min (${record.appGroupName})")
                }
                info.add("")
                
                // Check SharedPreferences
                val prefs = context.getSharedPreferences("awaytime_blocking", android.content.Context.MODE_PRIVATE)
                val blockedPackages = prefs.getStringSet("blocked_packages", emptySet())
                info.add("🚫 Currently Blocked: ${blockedPackages?.size ?: 0} apps")
                blockedPackages?.forEach { pkg ->
                    info.add("  - $pkg")
                }
                
            } catch (e: Exception) {
                info.add("❌ Error loading debug info: ${e.message}")
            }
            
            debugInfo = info
            isRefreshing = false
        }
    }
    
    LaunchedEffect(Unit) {
        refreshDebugInfo()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Debug") },
                actions = {
                    IconButton(
                        onClick = { refreshDebugInfo() },
                        enabled = !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRefreshing) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            items(debugInfo) { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (info.contains("❌")) {
                            MaterialTheme.colorScheme.errorContainer
                        } else if (info.contains("✅")) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Text(
                        text = info,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (info.contains("❌")) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else if (info.contains("✅")) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    usageTrackingService.updateUsageData()
                                    refreshDebugInfo()
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Force Update Usage Data")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    usageTrackingService.startMonitoring()
                                    refreshDebugInfo()
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Monitoring")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            usageTrackingService.stopMonitoring()
                            refreshDebugInfo()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Monitoring")
                    }
                }
            }
        }
    }
}