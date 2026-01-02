package com.exemple.blockingapps.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exemple.blockingapps.data.local.SessionManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToBlockApp: () -> Unit = {},
    onNavigateToAvailableApp: () -> Unit = {},
    onRequestMoreTime: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshDataFromDisk(context)
    }

    val realName = remember {
        SessionManager.getUserName(context) ?: "User"
    }
    val uiState by viewModel.uiState.collectAsState()

    var showRequestDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when request is submitted
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            launch {
                snackbarHostState.showSnackbar(
                    message = "Request sent successfully!",
                    duration = SnackbarDuration.Short
                )
                showSuccessMessage = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard") },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hello, $realName", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Today: ${uiState.totalUsageMinutesToday} minutes used",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val features = listOf(
                ChildFeatureTile("Available apps", "View all available apps", onNavigateToAvailableApp),
                ChildFeatureTile("Blocked apps", "View all blocked apps", onNavigateToBlockApp),
            )

            items(features) { feature ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { feature.action() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            feature.title,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showRequestDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request more time")
                }
            }
        }

        if (showRequestDialog) {
            RequestExtraTimeDialog(
                blockedApps = uiState.blockedApps
                    .groupBy { it.appName }
                    .map { it.value.first() },
                onDismiss = { showRequestDialog = false },
                onSubmit = { app, minutes, reason ->
                    viewModel.submitExtraTimeRequest(
                        app = app,
                        requestedMinutes = minutes,
                        reason = reason,
                        childName = realName
                    )
                    showRequestDialog = false
                    showSuccessMessage = true // Trigger snackbar
                }
            )
        }
    }
}

private data class ChildFeatureTile(
    val title: String,
    val subtitle: String,
    val action: () -> Unit
)

@Composable
fun RequestExtraTimeDialog(
    blockedApps: List<BlockedAppItem>,
    onDismiss: () -> Unit,
    onSubmit: (BlockedAppItem, Int, String) -> Unit
) {
    var selectedApp by remember { mutableStateOf<BlockedAppItem?>(null) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request more time") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Select blocked app", fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(8.dp))

                if (blockedApps.isEmpty()) {
                    Text(
                        "No blocked apps available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // FIXED: Removed duplicate forEach loop
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        blockedApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedApp = app }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedApp?.packageName == app.packageName,
                                    onClick = { selectedApp = app }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(app.appName)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text("Extra time", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it.filter(Char::isDigit) },
                        label = { Text("Hours") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter(Char::isDigit) },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val totalMinutes =
                (hours.toIntOrNull() ?: 0) * 60 +
                        (minutes.toIntOrNull() ?: 0)

            Button(
                enabled = selectedApp != null && totalMinutes > 0,
                onClick = {
                    onSubmit(
                        selectedApp!!,
                        totalMinutes,
                        reason
                    )
                }
            ) {
                Text("Send request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
