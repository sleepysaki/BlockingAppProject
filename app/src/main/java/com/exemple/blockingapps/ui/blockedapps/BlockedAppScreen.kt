package com.exemple.blockingapps.ui.blockedapps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsScreen(
    viewModel: BlockedAppsViewModel = remember { BlockedAppsViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps") },
                actions = {
                    IconButton(onClick = { viewModel.blockNow() }) {
                        Icon(Icons.Default.Block, contentDescription = "Block Now")
                    }
                    IconButton(onClick = { viewModel.stopBlocking() }) {
                        Icon(Icons.Default.Check, contentDescription = "Stop Blocking")
                    }
                    IconButton(onClick = { viewModel.toggleBlock("com.google.android.youtube") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Toggle YouTube")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (state.isLoading) {
                // center loading
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(state.installedApps) { app ->
                    BlockedAppRow(
                        app = app,
                        isBlocked = state.blockedPackages.contains(app.packageName),
                        onToggle = {
                            viewModel.toggleBlock(app.packageName)
                            scope.launch {
                                val msg = if (state.blockedPackages.contains(app.packageName)) {
                                    "${app.name} will be unblocked"
                                } else {
                                    "${app.name} blocked"
                                }
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BlockedAppRow(
    app: InstalledApp,
    isBlocked: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = app.category, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = onToggle) {
                Text(if (isBlocked) "Unblock" else "Block")
            }
        }
    }
}
**/