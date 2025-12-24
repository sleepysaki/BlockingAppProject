package com.exemple.blockingapps.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

val COMMON_APPS = listOf(
    "com.facebook.katana" to "Facebook",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.google.android.youtube" to "YouTube",
    "com.instagram.android" to "Instagram",
    "com.zing.zalo" to "Zalo",
    "com.google.android.gm" to "Gmail"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupName: String,
    joinCode: String,
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    val context = LocalContext.current
    val rules by viewModel.groupRules.collectAsState()

    LaunchedEffect(groupId) {
        if (groupId.isNotEmpty()) {
            viewModel.fetchGroupRules(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings: $groupName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- JOIN CODE ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Group Join Code", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = joinCode,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LIST APP ---
            Text(
                "Block Apps for Everyone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- LIST APP WHICH HAVE BUTTON ---
            LazyColumn {
                items(COMMON_APPS) { (pkg, name) ->
                    val isBlocked = rules.any { it.packageName == pkg && it.isBlocked }

                    AppSwitchItem(
                        appName = name,
                        isBlocked = isBlocked,
                        onToggle = { newState ->
                            viewModel.updateGroupRule(context, groupId, pkg, newState)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppSwitchItem(appName: String, isBlocked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = appName, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isBlocked,
            onCheckedChange = onToggle
        )
    }
}