package com.exemple.blockingapps.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GroupScreen(
    currentUserId: String,
    viewModel: GroupViewModel = viewModel(),
    onGroupClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val myGroups by viewModel.myGroups.collectAsState()

    // State for Dialogs
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // State for Inputs
    var newGroupName by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }

    // Fetch groups when screen loads
    LaunchedEffect(currentUserId) {
        viewModel.fetchMyGroups(currentUserId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Group Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Buttons Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { showCreateDialog = true }) { Text("Create Group") }
            Button(onClick = { showJoinDialog = true }) { Text("Join Group") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // My Groups List
        Text("My Groups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Debug ID: $currentUserId", style = MaterialTheme.typography.bodySmall, color = Color.Red)
        Spacer(modifier = Modifier.height(8.dp))

        if (myGroups.isEmpty()) {
            Text("You haven't joined any groups yet.", color = Color.Gray)
        } else {
            myGroups.forEach { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onGroupClick(group.groupId, group.groupName) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = group.groupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Code: ${group.joinCode}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Role: ${group.role}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                }
            }
        }
    }

    // --- DIALOGS IMPLEMENTATION ---

    // 1. Create Group Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createGroup(context, newGroupName, currentUserId)
                            showCreateDialog = false
                            newGroupName = "" // Reset input
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Join Group Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Group") },
            text = {
                Column {
                    Text("Enter the code provided by the group admin:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        label = { Text("Join Code (e.g. ABCXYZ)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            viewModel.joinGroup(context, joinCodeInput, currentUserId)
                            showJoinDialog = false
                            joinCodeInput = "" // Reset input
                        }
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}