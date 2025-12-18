package com.exemple.blockingapps.ui.home


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock // <-- Cần import icon này
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = remember { HomeViewModel() },
    onNavigateToFamily: () -> Unit = {},
    onNavigateToBlockedApps: () -> Unit = {},
    onNavigateToTimeLimit: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToRecommend: () -> Unit = {},
    onNavigateToFace: () -> Unit = {},
    onNavigateToGeoBlock: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        floatingActionButton = {
            if (uiState.role.equals("Parent", ignoreCase = true)) {
                ExtendedFloatingActionButton(
                    text = { Text("Lock Now") },
                    onClick = { viewModel.lockAllNow() },
                    icon = { Icon(Icons.Filled.Lock, contentDescription = "Lock") },
                    modifier = Modifier,
                    expanded = true,
                    shape = FloatingActionButtonDefaults.extendedFabShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(),
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hello, ${uiState.username}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("Today: ${uiState.totalUsageMinutesToday} minutes used", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("${uiState.devices.size} devices linked • ${uiState.blockedApps.size} blocked apps")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val featuresParent = listOf(
                FeatureTile("Family Members", "View and manage child devices", onNavigateToFamily),
                FeatureTile("Blocked Apps", "Manage blocked apps and schedules", onNavigateToBlockedApps),
                FeatureTile("Time Limits", "See remaining time per app", onNavigateToTimeLimit),
                FeatureTile("Lock Child Device", "Instant lock all apps on selected device") { viewModel.lockAllNow() },
                FeatureTile("Face Recognition", "Manage face profiles", onNavigateToFace),
                FeatureTile("Usage History", "Charts and daily usage", onNavigateToHistory),
                FeatureTile("Auto Recommendations", "Suggestions based on usage", onNavigateToRecommend),
                FeatureTile("Location-based Blocking", "Block apps by zone", onNavigateToGeoBlock)
            )

            val features = if (uiState.role.equals("Parent", ignoreCase = true)) featuresParent else featuresParent.filter { it.title != "Lock Child Device" }

            items(features) { ft ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { ft.action.invoke() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ft.title, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ft.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                Text("Linked devices", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(6.dp))
            }
            items(uiState.devices) { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.deviceName, fontWeight = FontWeight.Medium)
                            Text("ID: ${d.deviceId}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.removeDevice(d.deviceId) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                Text("Blocked / Limited apps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }

            items(uiState.blockedApps) { app ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text("Category: ${app.category}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            if (app.remainingSeconds > 0L) {
                                val mins = (app.remainingSeconds / 60)
                                val secs = (app.remainingSeconds % 60)
                                Text("Remaining: ${mins}m ${secs}s", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Limit: ${app.dailyLimitMinutes} min/day", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = { viewModel.removeBlockedApp(app.appId) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private data class FeatureTile(
    val title: String,
    val subtitle: String,
    val action: () -> Unit
)