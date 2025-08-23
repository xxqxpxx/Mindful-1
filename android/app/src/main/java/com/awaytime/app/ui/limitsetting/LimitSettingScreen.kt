package com.awaytime.app.ui.limitsetting

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.ui.theme.AwayTimeTheme
import com.awaytime.app.viewmodel.DashboardViewModel

data class PresetLimit(
    val hours: Int,
    val minutes: Int,
    val label: String
) {
    val totalMinutes: Int get() = hours * 60 + minutes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSettingScreen(
    onNavigateBack: () -> Unit = {},
    onSaveComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel { DashboardViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    var selectedHours by remember { mutableIntStateOf(2) }
    var selectedMinutes by remember { mutableIntStateOf(0) }
    var showingPresets by remember { mutableStateOf(true) }
    var hasChanges by remember { mutableStateOf(false) }

    val presetLimits = remember {
        listOf(
            PresetLimit(0, 30, "30 minutes"),
            PresetLimit(1, 0, "1 hour"),
            PresetLimit(1, 30, "1.5 hours"),
            PresetLimit(2, 0, "2 hours"),
            PresetLimit(3, 0, "3 hours"),
            PresetLimit(4, 0, "4 hours")
        )
    }

    val totalMinutes = selectedHours * 60 + selectedMinutes
    val isValidLimit = totalMinutes in 15..720 // 15 minutes to 12 hours

    // Load current limit on first composition
    LaunchedEffect(uiState.dailyLimitMinutes) {
        if (!hasChanges) {
            selectedHours = uiState.dailyLimitMinutes / 60
            selectedMinutes = (uiState.dailyLimitMinutes % 60).let { mins ->
                // Snap to nearest 15-minute interval
                (mins / 15) * 15
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Daily Limit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isValidLimit) {
                                viewModel.updateDailyLimit(totalMinutes)
                                onSaveComplete()
                                onNavigateBack()
                            }
                        },
                        enabled = hasChanges && isValidLimit
                    ) {
                        Text(
                            "Save",
                            color = if (hasChanges && isValidLimit) AwayTimeColors.primary else Color.Gray,
                            fontWeight = FontWeight.SemiBold
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header section
            HeaderSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Current limit display
            CurrentLimitSection(currentLimit = uiState.dailyLimitMinutes)

            Spacer(modifier = Modifier.height(32.dp))

            // Preset options or custom picker
            if (showingPresets) {
                PresetSection(
                    presets = presetLimits,
                    selectedHours = selectedHours,
                    selectedMinutes = selectedMinutes,
                    onPresetSelected = { preset ->
                        selectedHours = preset.hours
                        selectedMinutes = preset.minutes
                        hasChanges = true
                    },
                    onShowCustom = { showingPresets = false }
                )
            } else {
                CustomPickerSection(
                    selectedHours = selectedHours,
                    selectedMinutes = selectedMinutes,
                    onHoursChanged = {
                        selectedHours = it
                        hasChanges = true
                    },
                    onMinutesChanged = {
                        selectedMinutes = it
                        hasChanges = true
                    },
                    onShowPresets = { showingPresets = true },
                    totalMinutes = totalMinutes
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Guidance section
            GuidanceSection(isValidLimit = isValidLimit, totalMinutes = totalMinutes)

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
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set Your Daily Limit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AwayTimeColors.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose how much time you want to spend on your selected apps each day.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CurrentLimitSection(currentLimit: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AwayTimeColors.secondary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Limit",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatTime(currentLimit),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwayTimeColors.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "per day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PresetSection(
    presets: List<PresetLimit>,
    selectedHours: Int,
    selectedMinutes: Int,
    onPresetSelected: (PresetLimit) -> Unit,
    onShowCustom: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Options",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onShowCustom) {
                Text(
                    "Custom",
                    color = AwayTimeColors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid of preset buttons
        for (row in presets.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (preset in row) {
                    PresetButton(
                        preset = preset,
                        isSelected = selectedHours == preset.hours && selectedMinutes == preset.minutes,
                        onSelected = { onPresetSelected(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill remaining space if odd number of items
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetButton(
    preset: PresetLimit,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelected,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AwayTimeColors.primary else AwayTimeColors.secondary.copy(
                alpha = 0.2f
            )
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            AwayTimeColors.primary
        ) else null
    ) {
        Text(
            text = preset.label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else AwayTimeColors.primary
        )
    }
}

@Composable
private fun CustomPickerSection(
    selectedHours: Int,
    selectedMinutes: Int,
    onHoursChanged: (Int) -> Unit,
    onMinutesChanged: (Int) -> Unit,
    onShowPresets: () -> Unit,
    totalMinutes: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onShowPresets) {
                Text(
                    "Presets",
                    color = AwayTimeColors.primary
                )
            }

            Text(
                text = "Set Custom Time",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time display
                Text(
                    text = formatTime(totalMinutes),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwayTimeColors.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Time pickers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // Hours picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Simple hour selector
                        Column {
                            for (hour in 0..12 step 1) {
                                TextButton(
                                    onClick = { onHoursChanged(hour) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedHours == hour) AwayTimeColors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "$hour",
                                        fontWeight = if (selectedHours == hour) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Minutes picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Simple minute selector (15-minute intervals)
                        Column {
                            for (minute in 0 until 60 step 15) {
                                TextButton(
                                    onClick = { onMinutesChanged(minute) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (selectedMinutes == minute) AwayTimeColors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "$minute",
                                        fontWeight = if (selectedMinutes == minute) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceSection(isValidLimit: Boolean, totalMinutes: Int) {
    Column {
        Text(
            text = "💡 Tips for Success",
            style = MaterialTheme.typography.headlineSmall,
            color = AwayTimeColors.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GuidanceItem(
                icon = Icons.Default.Flag,
                text = "Start with a realistic goal you can achieve consistently"
            )

            GuidanceItem(
                icon = Icons.Default.TrendingUp,
                text = "Gradually reduce your limit over time as you build better habits"
            )

            GuidanceItem(
                icon = Icons.Default.Notifications,
                text = "You'll get a warning when you reach 80% of your limit"
            )

            if (!isValidLimit && totalMinutes > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AwayTimeColors.warning,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Limit must be between 15 minutes and 12 hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = AwayTimeColors.warning
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidanceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AwayTimeColors.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60

    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

@Preview(showBackground = true)
@Composable
fun LimitSettingScreenPreview() {
    AwayTimeTheme {
        LimitSettingScreen()
    }
}