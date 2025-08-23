/*
 * Adapted from Mindful (https://github.com/akaMrNagar/Mindful)
 * Original Author: Pawan Nagar (https://github.com/akaMrNagar)
 * 
 * Licensed under GPL-2.0 license
 * Adapted for AwayTime digital wellness app
 */
package com.awaytime.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awaytime.app.models.BedtimeSettings
import com.awaytime.app.models.BedtimeState
import com.awaytime.app.models.DayOfWeek
import com.awaytime.app.viewmodel.BedtimeViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeScreen(
    viewModel: BedtimeViewModel = run {
        val context = LocalContext.current
        viewModel { BedtimeViewModel(context) }
    }
) {
    val bedtimeSettings by viewModel.bedtimeSettings.collectAsState()
    val bedtimeState by viewModel.bedtimeState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Bedtime Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Current Status
        item {
            BedtimeStatusCard(bedtimeState = bedtimeState)
        }

        // Enable/Disable Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Enable Bedtime Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Automatically restrict apps during bedtime hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bedtimeSettings.isEnabled,
                            onCheckedChange = { viewModel.updateIsEnabled(it) }
                        )
                    }
                }
            }
        }

        // Time Settings
        if (bedtimeSettings.isEnabled) {
            item {
                TimeSettingsCard(
                    bedtimeSettings = bedtimeSettings,
                    onStartTimeChange = { viewModel.updateStartTime(it) },
                    onEndTimeChange = { viewModel.updateEndTime(it) }
                )
            }

            // Days Selection
            item {
                DaysSelectionCard(
                    selectedDays = bedtimeSettings.enabledDays,
                    onDaysChange = { viewModel.updateEnabledDays(it) }
                )
            }

            // Wind Down Settings
            item {
                WindDownSettingsCard(
                    windDownMinutes = bedtimeSettings.windDownDurationMinutes,
                    onWindDownChange = { viewModel.updateWindDownDuration(it) }
                )
            }

            // Bedtime Options
            item {
                BedtimeOptionsCard(
                    bedtimeSettings = bedtimeSettings,
                    onEnableDndChange = { viewModel.updateEnableDnd(it) },
                    onDimNotificationsChange = { viewModel.updateDimNotifications(it) },
                    onBlockDistractionsChange = { viewModel.updateBlockDistractions(it) },
                    onAllowEmergencyChange = { viewModel.updateAllowEmergency(it) }
                )
            }

            // Emergency Override
            item {
                EmergencyOverrideCard(
                    isActive = bedtimeState is BedtimeState.Active,
                    onEmergencyOverride = { viewModel.emergencyOverride() }
                )
            }
        }
    }
}

@Composable
private fun BedtimeStatusCard(bedtimeState: BedtimeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (bedtimeState) {
                is BedtimeState.Active -> MaterialTheme.colorScheme.primaryContainer
                is BedtimeState.WindDown -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val (statusText, statusDescription) = when (bedtimeState) {
                is BedtimeState.Active -> "Active" to "Bedtime mode is currently restricting apps"
                is BedtimeState.WindDown -> "Wind Down" to "Preparing for bedtime"
                else -> "Inactive" to "Bedtime mode is not active"
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSettingsCard(
    bedtimeSettings: BedtimeSettings,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Bedtime Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Start Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bedtime",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = bedtimeSettings.getStartTimeFormatted(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "to",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )
                
                // End Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Wake Up",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = bedtimeSettings.getEndTimeFormatted(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DaysSelectionCard(
    selectedDays: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDaysChange(DayOfWeek.weekdays()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Weekdays")
                }
                OutlinedButton(
                    onClick = { onDaysChange(DayOfWeek.values().toSet()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Every Day")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Individual Day Selection
            Column {
                DayOfWeek.values().forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = day in selectedDays,
                                onClick = {
                                    val newDays = selectedDays.toMutableSet()
                                    if (day in selectedDays) {
                                        newDays.remove(day)
                                    } else {
                                        newDays.add(day)
                                    }
                                    onDaysChange(newDays)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = day in selectedDays,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = day.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WindDownSettingsCard(
    windDownMinutes: Int,
    onWindDownChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Wind Down",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Gradual preparation before bedtime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "$windDownMinutes minutes before bedtime",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Slider(
                value = windDownMinutes.toFloat(),
                onValueChange = { onWindDownChange(it.toInt()) },
                valueRange = 0f..60f,
                steps = 11 // 0, 5, 10, 15, ..., 60
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 min", style = MaterialTheme.typography.labelSmall)
                Text("60 min", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BedtimeOptionsCard(
    bedtimeSettings: BedtimeSettings,
    onEnableDndChange: (Boolean) -> Unit,
    onDimNotificationsChange: (Boolean) -> Unit,
    onBlockDistractionsChange: (Boolean) -> Unit,
    onAllowEmergencyChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Bedtime Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Block Distractions
            SettingRow(
                title = "Block Distracting Apps",
                description = "Restrict access to selected apps during bedtime",
                checked = bedtimeSettings.blockDistractions,
                onCheckedChange = onBlockDistractionsChange
            )
            
            // Do Not Disturb
            SettingRow(
                title = "Enable Do Not Disturb",
                description = "Silence notifications during bedtime",
                checked = bedtimeSettings.enableDnd,
                onCheckedChange = onEnableDndChange
            )
            
            // Dim Notifications
            SettingRow(
                title = "Dim Notifications",
                description = "Reduce notification brightness",
                checked = bedtimeSettings.dimNotifications,
                onCheckedChange = onDimNotificationsChange
            )
            
            // Allow Emergency
            SettingRow(
                title = "Allow Emergency Calls",
                description = "Always allow phone and emergency apps",
                checked = bedtimeSettings.allowEmergencyCalls,
                onCheckedChange = onAllowEmergencyChange
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun EmergencyOverrideCard(
    isActive: Boolean,
    onEmergencyOverride: () -> Unit
) {
    if (isActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Emergency Override",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Temporarily disable bedtime mode for emergencies",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onEmergencyOverride,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Emergency Override (1 hour)")
                }
            }
        }
    }
}