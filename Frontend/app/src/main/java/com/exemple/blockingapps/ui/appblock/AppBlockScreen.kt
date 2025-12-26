package com.exemple.blockingapps.ui.appblock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // Use material (or material3) depending on your setup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.model.BlockRule

@OptIn(ExperimentalMaterial3Api::class) // Needed for Scaffold/TopAppBar in M3
@Composable
fun AppBlockScreen(
    // Initialize the ViewModel here
    viewModel: AppBlockViewModel = viewModel()
) {
    // 1. Collect the list of rules from ViewModel
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps Manager") }
            )
        }
    ) { innerPadding ->
        // 2. Display the list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(rules) { rule ->
                AppRuleItem(
                    rule = rule,
                    onToggle = { isChecked ->
                        // 3. Call update when Switch is toggled
                        viewModel.updateRule(
                            groupId = 1L, // Default dummy ID
                            packageName = rule.packageName,
                            isBlocked = isChecked,
                            startTime = rule.startTime ?: "00:00",
                            endTime = rule.endTime ?: "23:59"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AppRuleItem(
    rule: BlockRule,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: App Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Limit: ${rule.limitMinutes ?: "No limit"} mins",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Schedule: ${rule.startTime ?: "00:00"} - ${rule.endTime ?: "23:59"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Right side: Toggle Switch
            Switch(
                checked = rule.isBlocked,
                onCheckedChange = { onToggle(it) }
            )
        }
    }
}