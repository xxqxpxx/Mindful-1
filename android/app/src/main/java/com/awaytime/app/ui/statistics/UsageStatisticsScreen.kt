package com.awaytime.app.ui.statistics

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.awaytime.app.data.entity.UsageRecordEntity
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.viewmodel.UsageStatisticsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Usage Statistics Screen with Paging 3 for efficient historical data loading
 * Implements proper pagination for large datasets and smooth scrolling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatisticsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: UsageStatisticsViewModel = viewModel { UsageStatisticsViewModel(context) }
    val scope = rememberCoroutineScope()

    // Collect paging data for usage records
    val usageRecordsPaging: LazyPagingItems<UsageRecordEntity> = 
        viewModel.usageRecordsPagingFlow.collectAsLazyPagingItems()

    // Collect ViewModel state
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedAppGroup by viewModel.selectedAppGroup.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { 
                            scope.launch {
                                usageRecordsPaging.refresh()
                                viewModel.refreshAnalytics()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AwayTimeColors.primary
                        )
                    }
                    
                    // Export button
                    IconButton(
                        onClick = { viewModel.exportStatistics() }
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = AwayTimeColors.primary
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
                .padding(horizontal = 16.dp)
        ) {
            // Analytics Summary Card
            analyticsData?.let { analytics ->
                AnalyticsSummaryCard(
                    analytics = analytics,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Filters Row
            FiltersRow(
                selectedFilter = selectedFilter,
                selectedAppGroup = selectedAppGroup,
                dateRange = dateRange,
                onFilterChange = { viewModel.updateFilter(it) },
                onAppGroupChange = { viewModel.updateAppGroupFilter(it) },
                onDateRangeChange = { viewModel.updateDateRange(it) },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Usage Records List with Paging
            PagingUsageRecordsList(
                usageRecordsPaging = usageRecordsPaging,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    analytics: UsageAnalyticsData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AwayTimeColors.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Analytics Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AwayTimeColors.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalyticsItem(
                    label = "Total Usage",
                    value = "${analytics.totalUsageMinutes} min",
                    icon = Icons.Default.Timer
                )
                
                AnalyticsItem(
                    label = "Avg Daily",
                    value = "${analytics.avgDailyUsage} min",
                    icon = Icons.Default.CalendarToday
                )
                
                AnalyticsItem(
                    label = "Success Rate",
                    value = "${analytics.successRate}%",
                    icon = Icons.Default.TrendingUp
                )
            }
        }
    }
}

@Composable
private fun AnalyticsItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AwayTimeColors.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersRow(
    selectedFilter: UsageFilter,
    selectedAppGroup: String?,
    dateRange: DateRange,
    onFilterChange: (UsageFilter) -> Unit,
    onAppGroupChange: (String?) -> Unit,
    onDateRangeChange: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AwayTimeColors.primary
            )
        }
        
        item {
            // Time Range Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageFilter.values().forEach { filter ->
                    FilterChip(
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.displayName) },
                        selected = filter == selectedFilter,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AwayTimeColors.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        
        item {
            // App Group Filter
            Text(
                text = "App Group: ${selectedAppGroup ?: "All Groups"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagingUsageRecordsList(
    usageRecordsPaging: LazyPagingItems<UsageRecordEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Handle loading state at the top
        when (usageRecordsPaging.loadState.refresh) {
            is LoadState.Loading -> {
                item {
                    LoadingSection()
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorSection(
                        error = usageRecordsPaging.loadState.refresh as LoadState.Error,
                        onRetry = { usageRecordsPaging.retry() }
                    )
                }
            }
            else -> {
                // Show header with record count
                item {
                    UsageRecordsHeader(recordCount = usageRecordsPaging.itemCount)
                }
            }
        }

        // Usage record items with paging
        items(
            count = usageRecordsPaging.itemCount,
            key = usageRecordsPaging.itemKey { record -> "${record.appGroupName}_${record.date}" }
        ) { index ->
            val record = usageRecordsPaging[index]
            if (record != null) {
                UsageRecordItem(
                    record = record,
                    modifier = Modifier.animateItemPlacement()
                )
            } else {
                // Placeholder while loading
                UsageRecordPlaceholder()
            }
        }

        // Handle bottom loading state
        when (usageRecordsPaging.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AwayTimeColors.primary
                        )
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorSection(
                        error = usageRecordsPaging.loadState.append as LoadState.Error,
                        onRetry = { usageRecordsPaging.retry() }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun UsageRecordsHeader(
    recordCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = "📋 Showing $recordCount usage records",
            style = MaterialTheme.typography.bodyMedium,
            color = AwayTimeColors.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun UsageRecordItem(
    record: UsageRecordEntity,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val limitExceededColor = if (record.limitExceeded) Color.Red else AwayTimeColors.success
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.limitExceeded) {
                Color.Red.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (record.limitExceeded) {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appGroupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = dateFormatter.format(Date(record.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${record.usageMinutes} min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = limitExceededColor
                    )
                    
                    if (record.limitExceeded) {
                        Text(
                            text = "Limit Exceeded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Within Limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = AwayTimeColors.success
                        )
                    }
                }
            }
            
            if (record.pickupCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📱 ${record.pickupCount} pickups",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UsageRecordPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AwayTimeColors.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AwayTimeColors.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Loading usage statistics...",
                style = MaterialTheme.typography.bodyLarge,
                color = AwayTimeColors.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Analyzing your app usage patterns",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorSection(
    error: LoadState.Error,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Error loading statistics",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = error.error.message ?: "Unknown error occurred",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

// Data classes for the statistics screen
data class UsageAnalyticsData(
    val totalUsageMinutes: Int,
    val avgDailyUsage: Int,
    val successRate: Int,
    val mostUsedApp: String,
    val streakDays: Int
)

enum class UsageFilter(val displayName: String) {
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    LAST_90_DAYS("90 Days"),
    ALL_TIME("All Time")
}

data class DateRange(
    val start: Date,
    val end: Date
)
