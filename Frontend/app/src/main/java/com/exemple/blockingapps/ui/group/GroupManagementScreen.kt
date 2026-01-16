package com.exemple.blockingapps.ui.group

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.exemple.blockingapps.model.GroupDTO
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.ui.home.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupMembers by viewModel.groupMembers.collectAsStateWithLifecycle() // Get member list from ViewModel

    var showCreateDialog by remember { mutableStateOf(false) }

    // FIXED: Changed type from UserGroup? to GroupDTO? to match the data source
    var selectedGroupForManagement by remember { mutableStateOf<GroupDTO?>(null) }

    val context = LocalContext.current
    // NOTE: In production, get userId from UserPreferences or ViewModel
    val currentUserId = "36050457-f112-4762-a7f7-24daab6986ce"

    LaunchedEffect(Unit) {
        viewModel.fetchUserGroups(currentUserId)
    }

    // Load members immediately when a group is selected
    LaunchedEffect(selectedGroupForManagement) {
        selectedGroupForManagement?.let {
            viewModel.fetchGroupMembers(it.groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Management", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create Group")
                }
                Button(
                    onClick = { /* Join logic (already handled in GroupSettings) */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Join Group")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("My Groups", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.groups.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("You haven't joined any groups yet.", color = Color.Gray)
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = uiState.groups, key = { it.groupId }) { group ->
                            GroupItemCard(
                                group = group,
                                onClick = {
                                    // Click on group to open Member Management Dialog
                                    selectedGroupForManagement = group
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createGroup(context, name, currentUserId)
                showCreateDialog = false
            }
        )
    }

    // Member Management Dialog
    if (selectedGroupForManagement != null) {
        val group = selectedGroupForManagement!!
        val isCurrentUserAdmin = group.role == "ADMIN"

        MemberManagementDialog(
            groupName = group.groupName,
            members = groupMembers,
            isCurrentUserAdmin = isCurrentUserAdmin,
            currentUserId = currentUserId,
            onDismiss = { selectedGroupForManagement = null },
            onPromote = { targetUserId ->
                viewModel.promoteMember(context, group.groupId, currentUserId, targetUserId)
            },
            onRemove = { targetUserId ->
                viewModel.removeMember(context, group.groupId, currentUserId, targetUserId)
            }
        )
    }
}

@Composable
fun GroupItemCard(
    group: GroupDTO,
    // FIXED: Changed from (String) -> Unit to () -> Unit because the caller doesn't pass a String
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Allow click
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = group.groupName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Role: ${group.role}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (group.joinCode.isNotEmpty()) {
                    Text(
                        text = "Code: ${group.joinCode}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MemberManagementDialog(
    groupName: String,
    members: List<GroupMember>,
    isCurrentUserAdmin: Boolean,
    currentUserId: String,
    onDismiss: () -> Unit,
    onPromote: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Members: $groupName") },
        text = {
            if (members.isEmpty()) {
                Text("Loading members...", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp) // Limit height
                ) {
                    items(members) { member ->
                        MemberItem(
                            member = member,
                            isCurrentUserAdmin = isCurrentUserAdmin,
                            isSelf = member.userId == currentUserId,
                            onPromote = { onPromote(member.userId) },
                            onRemove = { onRemove(member.userId) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun MemberItem(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    isSelf: Boolean,
    onPromote: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- User Info Section ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = member.fullName + (if (isSelf) " (You)" else ""),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = member.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (member.role == "ADMIN") MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        // --- Action Section ---
        // Only show actions if current user is Admin and target is not Self
        if (isCurrentUserAdmin && !isSelf) {
            Box {
                // ICON CHANGE IS HERE: Use MoreVert (3 dots) instead of Delete
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Option 1: Promote to Admin (Hide if already Admin)
                    if (member.role != "ADMIN") {
                        DropdownMenuItem(
                            text = { Text("Promote to Admin") },
                            onClick = {
                                showMenu = false
                                onPromote()
                            }
                        )
                    }

                    // Option 2: Remove Member
                    DropdownMenuItem(
                        text = { Text("Remove from Group", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            OutlinedTextField(
                value = groupNameInput,
                onValueChange = { groupNameInput = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                enabled = groupNameInput.isNotBlank(),
                onClick = { onConfirm(groupNameInput) }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}