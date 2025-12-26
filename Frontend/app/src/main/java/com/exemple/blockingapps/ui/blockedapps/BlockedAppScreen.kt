package com.exemple.blockingapps.ui.blockedapps

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.home.TimePreset

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsScreen(
    viewModel: HomeViewModel,
    geoViewModel: GeoBlockViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val geoApps by geoViewModel.appList.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<TimePreset?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshDataFromDisk(context)
        viewModel.loadPresetsFromDisk(context)
        geoViewModel.getInstalledApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Blocking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val rulesToSave = uiState.blockedApps.map { app ->
                                // Adjusting to match your actual BlockRule data class structure
                                com.exemple.blockingapps.model.BlockRule(
                                    packageName = app.packageName,
                                    isBlocked = true,
                                    startTime = app.scheduleFrom,
                                    endTime = app.scheduleTo,
                                    limitMinutes = 0 // Adding missing mandatory parameter
                                )
                            }
                            com.exemple.blockingapps.utils.BlockManager.saveRulesFromUI(context, rulesToSave)
                            android.widget.Toast.makeText(context, "Settings Saved & Applied!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("SAVE", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Blocking Presets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        OutlinedCard(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.size(width = 100.dp, height = 90.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    items(uiState.timePresets) { preset ->
                        val isSelected = selectedPreset?.id == preset.id
                        Box {
                            Card(
                                onClick = { selectedPreset = preset },
                                modifier = Modifier.size(width = 150.dp, height = 90.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                    Text(preset.label, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${preset.startTime} - ${preset.endTime}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteTimePreset(context, preset.id)
                                    if (selectedPreset?.id == preset.id) selectedPreset = null
                                },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Divider()
                Text("Select Apps to Block", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

                if (selectedPreset == null) {
                    Text("Please select a preset above", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search app name...") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Text("Assigning to: ${selectedPreset?.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (selectedPreset != null) {
                val filteredApps = geoApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
                items(filteredApps) { app ->
                    ListItem(
                        leadingContent = {
                            val iconBitmap = remember(app.packageName) { app.icon?.toBitmap()?.asImageBitmap() }
                            if (iconBitmap != null) Image(iconBitmap, null, modifier = Modifier.size(40.dp))
                            else Icon(Icons.Default.Block, null)
                        },
                        headlineContent = { Text(app.name) },
                        trailingContent = {
                            Button(onClick = {
                                viewModel.assignAppToPreset(context, app, selectedPreset!!)
                                android.widget.Toast.makeText(context, "Added ${app.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }) { Text("Assign") }
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Currently Scheduled Apps", fontWeight = FontWeight.Bold)
            }

            val timedApps = uiState.blockedApps.filter { it.scheduleFrom != null }
            items(timedApps) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.appName, fontWeight = FontWeight.SemiBold)
                            Text("Blocked: ${app.scheduleFrom} - ${app.scheduleTo}", fontSize = 12.sp, color = Color.Red)
                        }
                        IconButton(onClick = { viewModel.removeBlockedApp(app.appId, context) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Gray)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(50.dp)) }
        }
    }

    if (showAddDialog) {
        AddPresetDialog(
            onDismiss = { showAddDialog = false },
            onSave = { label, start, end ->
                viewModel.addTimePreset(context, label, start, end)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddPresetDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("17:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Preset Label (e.g. Focus Time)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = label.isNotBlank(),
                onClick = { onSave(label, startTime, endTime) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}