package com.exemple.blockingapps.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val realName = remember {
        SessionManager.getUserName(context) ?: "User"
    }
    val uiState by viewModel.uiState.collectAsState()


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

    ){ padding ->
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


            // Request more time
            item {
                Button(
                    onClick = onRequestMoreTime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request more time")
                }
            }
        }
    }
}



private data class ChildFeatureTile(
    val title: String,
    val subtitle: String,
    val action: () -> Unit
)

