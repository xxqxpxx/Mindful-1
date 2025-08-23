package com.awaytime.app.presentation.appselection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.domain.model.AppInfo
import com.awaytime.app.domain.usecase.CreateAppGroupUseCase
import com.awaytime.app.domain.usecase.GetInstalledAppsUseCase
import com.awaytime.app.ui.theme.AwayTimeColors

/**
 * Mindful App Selection Screen
 * Clean architecture UI following Mindful patterns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Create dependencies manually (simple DI)
    val repository = remember { AwayTimeRepository(context) }
    val getInstalledAppsUseCase = remember { GetInstalledAppsUseCase(context) }
    val createAppGroupUseCase = remember { CreateAppGroupUseCase(repository) }
    
    val viewModel: AppSelectionViewModel = viewModel {
        AppSelectionViewModel(getInstalledAppsUseCase, createAppGroupUseCase)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    var showGroupNameDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            onNavigateBack()
        }
    }

    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or handle error
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasSelectedApps) {
                        TextButton(
                            onClick = { showGroupNameDialog = true },
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Save (${uiState.selectedAppCount})",
                                    color = AwayTimeColors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Choose Apps to Monitor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AwayTimeColors.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select apps you want to track and limit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content based on state
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.availableApps.isEmpty() -> {
                    EmptyContent(onRetry = { viewModel.loadInstalledApps() })
                }
                else -> {
                    AppListContent(
                        apps = uiState.availableApps,
                        selectedApps = uiState.selectedApps,
                        onToggleApp = { viewModel.toggleAppSelection(it) }
                    )
                }
            }
        }
    }

    // Group name dialog
    if (showGroupNameDialog) {
        GroupNameDialog(
            groupName = groupName,
            onGroupNameChange = { groupName = it },
            onConfirm = {
                showGroupNameDialog = false
                viewModel.createAppGroup(groupName)
            },
            onDismiss = { showGroupNameDialog = false }
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = "Loading your apps...",
                style = MaterialTheme.typography.bodyLarge,
                color = AwayTimeColors.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No apps found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please check your permissions or try again",
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
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AppListContent(
    apps: List<AppInfo>,
    selectedApps: Set<String>,
    onToggleApp: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps) { app ->
            AppItem(
                app = app,
                isSelected = selectedApps.contains(app.packageName),
                onToggle = { onToggleApp(app.packageName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AwayTimeColors.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, AwayTimeColors.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = AwayTimeColors.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AwayTimeColors.primary else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = AwayTimeColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupNameDialog(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Your App Group") },
        text = {
            Column {
                Text("Give your app group a name to help you remember what apps you're tracking.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = { Text("Group name (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save", color = AwayTimeColors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}