package com.exemple.blockingapps.ui.group

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy // Icon copy
import androidx.compose.material.icons.filled.Share // Icon share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager // Để copy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.model.GroupRuleDTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    val rules by viewModel.groupRules.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current // Quản lý clipboard

    // Lấy Join Code từ ViewModel
    // Lưu ý: Đảm bảo ViewModel đã load danh sách nhóm để có thể tìm thấy code
    val joinCode = remember(groupId) { viewModel.getJoinCodeForGroup(groupId) }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.fetchGroupRules(context, groupId)
        viewModel.loadInstalledApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Invite") }, // Đổi tiêu đề chút cho hợp
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(joinCode))
                        Toast.makeText(context, "Copied Code: $joinCode", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Group Join Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = joinCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to copy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Blocked Apps List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 👇 DANH SÁCH APP CHẶN (CODE CŨ)
            if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No rules configured yet. Tap + to add.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(rules) { rule ->
                        GroupRuleItem(
                            rule = rule,
                            onToggle = { isBlocked ->
                                viewModel.updateGroupRule(context, groupId, rule.packageName, isBlocked)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            SelectAppDialog(
                apps = installedApps,
                onDismiss = { showAddDialog = false },
                onAppSelected = { app ->
                    viewModel.addGroupRule(context, groupId, app)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SelectAppDialog(
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppSelected: (AppItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App to Block") },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(apps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(app) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { drawable ->
                            Image(
                                bitmap = drawable.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = app.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GroupRuleItem(
    rule: GroupRuleDTO,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                val info = when {
                    rule.radius != null && rule.radius > 0 -> "Geo-Blocking Active"
                    !rule.startTime.isNullOrEmpty() -> "Time: ${rule.startTime} - ${rule.endTime}"
                    else -> "Manual Block"
                }
                Text(text = info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }

            Switch(
                checked = rule.isBlocked,
                onCheckedChange = { isChecked ->
                    onToggle(isChecked)
                }
            )
        }
    }
}