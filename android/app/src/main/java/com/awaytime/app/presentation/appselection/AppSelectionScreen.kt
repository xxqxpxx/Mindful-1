package com.awaytime.app.presentation.appselection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.domain.model.AppInfo
import com.awaytime.app.domain.usecase.CreateAppGroupUseCase
import com.awaytime.app.ui.theme.AwayTimeColors

/**
 * Paging version of App Selection Screen using Paging 3 library
 * Provides efficient lazy loading with search capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onNavigateBack: () -> Unit
) {
    println("🎯 AppSelectionScreen: Starting composition")
    val context = LocalContext.current
    
    // Create dependencies
    val repository = remember { AwayTimeRepository(context) }
    val createAppGroupUseCase = remember { CreateAppGroupUseCase(repository) }
    
    val viewModel: AppSelectionViewModel = viewModel {
        println("🎯 AppSelectionScreen: Creating ViewModel")
        AppSelectionViewModel(context, createAppGroupUseCase)
    }
    
    // Collect paging data
    println("🎯 AppSelectionScreen: Collecting paging data")
    val appsPaging: LazyPagingItems<AppInfo> = viewModel.appsPagingFlow.collectAsLazyPagingItems()
    println("🎯 AppSelectionScreen: Paging data collected, itemCount=${appsPaging.itemCount}")
    
    // Collect other UI state
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val hasSelectedApps by viewModel.hasSelectedApps.collectAsState()
    val selectedAppCount by viewModel.selectedAppCount.collectAsState()

    var showGroupNameDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    // Handle save success
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.clearSaveSuccess()
            onNavigateBack()
        }
    }

    // Handle errors
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }

    println("🎯 AppSelectionScreen: Rendering Scaffold")
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
                    if (hasSelectedApps) {
                        TextButton(
                            onClick = { showGroupNameDialog = true },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Save ($selectedAppCount)",
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
            
            // Search field
            SearchField(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App list with paging
            AppListPaging(
                appsPaging = appsPaging,
                selectedApps = selectedApps,
                onToggleApp = viewModel::toggleAppSelection
            )
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

    // Error handling
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You can show a snackbar here if needed
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = { Text("Search apps...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun AppListPaging(
    appsPaging: LazyPagingItems<AppInfo>,
    selectedApps: Set<String>,
    onToggleApp: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = appsPaging.itemCount
        ) { index ->
            val app = appsPaging[index]
            app?.let {
                AppItemPaging(
                    app = it,
                    isSelected = selectedApps.contains(it.packageName),
                    onToggle = { onToggleApp(it.packageName) }
                )
            }
        }

        // Handle loading states
        when (appsPaging.loadState.refresh) {
            is LoadState.Loading -> {
                item {
                    LoadingContent()
                }
            }
            is LoadState.Error -> {
                val error = appsPaging.loadState.refresh as LoadState.Error
                item {
                    ErrorContent(
                        message = error.error.message ?: "Failed to load apps",
                        onRetry = { appsPaging.retry() }
                    )
                }
            }
            is LoadState.NotLoading -> {
                if (appsPaging.itemCount == 0) {
                    item {
                        EmptyContent(onRetry = { appsPaging.refresh() })
                    }
                }
            }
        }

        // Handle append loading state
        when (appsPaging.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AwayTimeColors.primary
                        )
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load more apps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { appsPaging.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppItemPaging(
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
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
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
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error Loading Apps",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
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