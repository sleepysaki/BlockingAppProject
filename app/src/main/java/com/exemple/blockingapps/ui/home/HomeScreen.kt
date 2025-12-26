package com.exemple.blockingapps.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
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
    val context = LocalContext.current

    // Tự động load lại dữ liệu mỗi khi màn hình Home hiển thị
    LaunchedEffect(Unit) {
        viewModel.refreshAllData(context)
        viewModel.startTimeTicker(context)
    }

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
                    icon = { Icon(Icons.Filled.Lock, contentDescription = "Lock") }
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
                FeatureTile("Lock Child Device", "Instant lock all apps", { viewModel.lockAllNow() }),
                FeatureTile("Usage History", "Charts and daily usage", onNavigateToHistory),
                FeatureTile("Auto Recommendations", "Suggestions based on usage", onNavigateToRecommend),
                FeatureTile("Location-based Blocking", "Block apps by zone", onNavigateToGeoBlock)
            )

            val features = if (uiState.role.equals("Parent", ignoreCase = true)) featuresParent else featuresParent.filter { it.title != "Lock Child Device" }

            items(features) { ft ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { ft.action() }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ft.title, fontWeight = FontWeight.SemiBold)
                            Text(ft.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item { Text("Blocked / Limited apps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }

            items(uiState.blockedApps, key = { it.appId }) { app ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, fontWeight = FontWeight.Medium)
                            Text("Category: ${app.category}", style = MaterialTheme.typography.bodySmall)
                            if (app.remainingSeconds > 0L) {
                                val mins = app.remainingSeconds / 60
                                val secs = app.remainingSeconds % 60
                                Text("Remaining: ${mins}m ${secs}s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { viewModel.removeBlockedApp(app.appId, context) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private data class FeatureTile(val title: String, val subtitle: String, val action: () -> Unit)