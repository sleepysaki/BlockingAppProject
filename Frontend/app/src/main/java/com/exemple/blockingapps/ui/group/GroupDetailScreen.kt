package com.exemple.blockingapps.ui.group

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.model.GroupRuleDTO
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupName: String,
    currentUserId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val groupRules by viewModel.groupRules.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Tabs: 0 = Members, 1 = Blocked Apps
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Members", "Blocked Apps")

    // --- STATES CHO DIALOG ---
    var showAddAppDialog by remember { mutableStateOf(false) }

    // Biến này lưu App vừa được chọn từ danh sách cài đặt (để chuyển sang Config Dialog)
    var selectedAppForConfig by remember { mutableStateOf<AppItem?>(null) }

    // Biến này lưu Rule đang muốn chỉnh sửa (Edit Mode)
    var selectedRuleForEdit by remember { mutableStateOf<GroupRuleDTO?>(null) }

    LaunchedEffect(groupId) {
        viewModel.fetchMembers(groupId)
        viewModel.fetchGroupRules(context, groupId)
    }

    val isAdmin = remember(members, currentUserId) {
        members.find { it.userId.trim() == currentUserId.trim() }
            ?.role.equals("ADMIN", ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupName)
                        Text("ID: ${groupId.take(4)}...", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1 && isAdmin) {
                FloatingActionButton(onClick = {
                    viewModel.loadInstalledApps(context)
                    showAddAppDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier
                .weight(1f)
                .padding(16.dp)) {
                when (selectedTabIndex) {
                    0 -> MembersTabContent(
                        members = members,
                        isAdmin = isAdmin,
                        currentUserId = currentUserId,
                        context = context,
                        groupId = groupId,
                        viewModel = viewModel,
                        onBack = onBack
                    )
                    1 -> BlockedAppsTabContent(
                        rules = groupRules,
                        isAdmin = isAdmin,
                        onRuleClick = { rule ->
                            // Bấm vào rule để sửa
                            if (isAdmin) selectedRuleForEdit = rule
                        }
                    )
                }
            }
        }
    }

    // --- DIALOG 1: CHỌN APP TỪ DANH SÁCH ---
    if (showAddAppDialog) {
        AppSelectionDialog(
            apps = installedApps,
            onDismiss = { showAddAppDialog = false },
            onAppSelected = { app ->
                showAddAppDialog = false
                // Thay vì lưu ngay, ta mở Dialog cấu hình
                selectedAppForConfig = app
            }
        )
    }

    // --- DIALOG 2: CẤU HÌNH LUẬT (CREATE NEW) ---
    selectedAppForConfig?.let { app ->
        RuleConfigDialog(
            packageName = app.packageName,
            initialStartTime = null,
            initialEndTime = null,
            onDismiss = { selectedAppForConfig = null },
            onSave = { start, end ->
                viewModel.updateGroupRule(context, groupId, app.packageName, true, start, end)
                selectedAppForConfig = null
            },
            onDelete = null // Creating new, no delete option
        )
    }

    // --- DIALOG 3: CẤU HÌNH LUẬT (EDIT EXISTING) ---
    selectedRuleForEdit?.let { rule ->
        RuleConfigDialog(
            packageName = rule.packageName,
            initialStartTime = rule.startTime,
            initialEndTime = rule.endTime,
            onDismiss = { selectedRuleForEdit = null },
            onSave = { start, end ->
                viewModel.updateGroupRule(context, groupId, rule.packageName, true, start, end)
                selectedRuleForEdit = null
            },
            onDelete = {
                // Xóa rule (Unblock)
                viewModel.updateGroupRule(context, groupId, rule.packageName, false, null, null)
                selectedRuleForEdit = null
            }
        )
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun RuleConfigDialog(
    packageName: String,
    initialStartTime: String?, // Format "HH:mm"
    initialEndTime: String?,
    onDismiss: () -> Unit,
    onSave: (startTime: String?, endTime: String?) -> Unit,
    onDelete: (() -> Unit)? // Null if creating new
) {
    // Mode: 0 = Always, 1 = Time Schedule
    var modeIndex by remember {
        mutableIntStateOf(if (initialStartTime != null && initialEndTime != null) 1 else 0)
    }

    var startTime by remember { mutableStateOf(initialStartTime ?: "08:00") }
    var endTime by remember { mutableStateOf(initialEndTime ?: "17:00") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Rule: $packageName") },
        text = {
            Column {
                // Chọn chế độ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = modeIndex == 0, onClick = { modeIndex = 0 })
                    Text("Always Block")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = modeIndex == 1, onClick = { modeIndex = 1 })
                    Text("Time Schedule")
                }

                if (modeIndex == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Time Range:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showTimePicker(context) { h, m -> startTime = String.format("%02d:%02d", h, m) } }) {
                            Text("Start: $startTime")
                        }
                        Button(onClick = { showTimePicker(context) { h, m -> endTime = String.format("%02d:%02d", h, m) } }) {
                            Text("End: $endTime")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (modeIndex == 0) {
                    onSave(null, null) // Always block -> Send null times
                } else {
                    onSave(startTime, endTime)
                }
            }) { Text("Save Rule") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                        Text("Unblock App")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

fun showTimePicker(context: Context, onTimeSelected: (Int, Int) -> Unit) {
    val cal = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, h, m -> onTimeSelected(h, m) },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        true // 24h format
    ).show()
}

@Composable
fun BlockedAppsTabContent(
    rules: List<GroupRuleDTO>,
    isAdmin: Boolean,
    onRuleClick: (GroupRuleDTO) -> Unit
) {
    val blockedRules = rules.filter { it.isBlocked } // Chỉ hiện rule đang chặn

    if (blockedRules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No apps are blocked yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(blockedRules) { rule ->
                Card(
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.clickable {
                        if (isAdmin) onRuleClick(rule)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = rule.packageName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

                            val statusText = if (rule.startTime != null && rule.endTime != null) {
                                "Scheduled: ${rule.startTime} - ${rule.endTime}"
                            } else {
                                "Status: ALWAYS BLOCKED"
                            }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        if (isAdmin) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ... (Giữ nguyên MembersTabContent, AppSelectionDialog, GroupDetailMemberItem cũ của bạn)
@Composable
fun MembersTabContent(
    members: List<GroupMember>,
    isAdmin: Boolean,
    currentUserId: String,
    context: android.content.Context,
    groupId: String,
    viewModel: GroupViewModel,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Members List (${members.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(members) { member ->
                val isSelf = member.userId == currentUserId
                GroupDetailMemberItem(
                    member = member,
                    isCurrentUserAdmin = isAdmin,
                    isSelf = isSelf,
                    onRemoveClick = {
                        viewModel.removeMember(context, groupId, currentUserId, member.userId)
                    },
                    onPromoteClick = {
                        viewModel.promoteMember(context, groupId, currentUserId, member.userId)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.leaveGroup(context, groupId, currentUserId) { onBack() }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Leave Group", color = Color.White)
        }
    }
}

@Composable
fun AppSelectionDialog(
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppSelected: (AppItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Block") },
        text = {
            if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(apps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GroupDetailMemberItem(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    isSelf: Boolean,
    onRemoveClick: () -> Unit,
    onPromoteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) "${member.fullName} (You)" else member.fullName,
                    fontWeight = FontWeight.Bold
                )
                if (!member.email.isNullOrEmpty()) {
                    Text(text = member.email, style = MaterialTheme.typography.bodySmall)
                }
            }

            val isTargetAdmin = member.role.equals("ADMIN", ignoreCase = true)

            if (isTargetAdmin) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "ADMIN",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                if (isCurrentUserAdmin && !isSelf) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Promote to Admin") },
                                onClick = {
                                    showMenu = false
                                    onPromoteClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from Group", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onRemoveClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}