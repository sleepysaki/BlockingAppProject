package com.exemple.blockingapps.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    currentUserId: String, // Pass logged-in User UUID here
    viewModel: GroupViewModel = viewModel()
) {
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }

    val members by viewModel.members.collectAsState()
    val generatedCode by viewModel.currentJoinCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Workspace & Groups", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // --- CREATE GROUP SECTION ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create New Group", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.createGroup(context, groupName, currentUserId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Group")
                }

                if (generatedCode.isNotEmpty()) {
                    Text(
                        text = "Group Code: $generatedCode",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- JOIN GROUP SECTION ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Join Group", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it.uppercase() },
                    label = { Text("Enter Code (e.g. ABC123)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.joinGroup(context, joinCodeInput, currentUserId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Join Group")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
// Subscribe to myGroups state
        val myGroups by viewModel.myGroups.collectAsState()

        // Trigger fetch on first load
        LaunchedEffect(currentUserId) {
            viewModel.fetchMyGroups(currentUserId)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make screen scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Keep Create Group & Join Group sections) ...

            Spacer(modifier = Modifier.height(24.dp))

            // --- MY GROUPS SECTION ---
            Text("My Groups", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (myGroups.isEmpty()) {
                Text("You haven't joined any groups yet.", color = Color.Gray)
            } else {
                myGroups.forEach { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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

            // ... (Keep Members List section if needed, or remove it as it handles single group logic) ...
        }
        // --- MEMBER LIST ---
        if (members.isNotEmpty()) {
            Text("Members", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(Color.LightGray.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(member.fullName, fontWeight = FontWeight.Bold)
                        Text(if (member.isAdmin) "Admin" else "Member", color = Color.DarkGray)
                    }
                }
            }
        }
    }
}