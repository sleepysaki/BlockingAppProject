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
                title = { Text("Quản lý chặn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Khung giờ chặn (Presets)", style = MaterialTheme.typography.titleMedium)
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
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                items(uiState.timePresets) { preset ->
                    val isSelected = selectedPreset?.id == preset.id

                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        Card(
                            onClick = { selectedPreset = preset },
                            modifier = Modifier.size(width = 150.dp, height = 90.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                Modifier.padding(12.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(preset.label, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${preset.startTime} - ${preset.endTime}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteTimePreset(context, preset.id)
                                if (selectedPreset?.id == preset.id) selectedPreset = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa Preset",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text("Chọn ứng dụng để áp dụng", style = MaterialTheme.typography.titleMedium)

            if (selectedPreset == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Vui lòng chọn một khung giờ phía trên", color = Color.Gray)
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm tên ứng dụng...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Text(
                    "Đang gán cho: ${selectedPreset?.label}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )

                val filteredApps = geoApps.filter { app ->
                    val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true)
                    matchesSearch
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps) { app ->
                        ListItem(
                            leadingContent = {
                                val iconBitmap = remember(app.packageName) {
                                    app.icon?.toBitmap()?.asImageBitmap()
                                }
                                if (iconBitmap != null) {
                                    Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                                } else {
                                    Icon(Icons.Default.Block, null)
                                }
                            },
                            headlineContent = { Text(app.name) },
                            // supportingContent = { Text(app.packageName) },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        viewModel.assignAppToPreset(context, app, selectedPreset!!)
                                        android.widget.Toast.makeText(context, "Đã gán ${app.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Gán")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Ứng dụng đang trong lịch trình", fontWeight = FontWeight.Bold)

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                val timedApps = uiState.blockedApps.filter { it.scheduleFrom != null }
                items(timedApps) { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(app.appName, fontWeight = FontWeight.SemiBold)
                                Text("Chặn: ${app.scheduleFrom} - ${app.scheduleTo}", fontSize = 12.sp, color = Color.Red)
                            }
                            IconButton(onClick = { viewModel.removeBlockedApp(app.appId, context) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
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
        title = { Text("Thêm khung giờ mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Tên gợi nhớ (VD: Giờ học bài)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Bắt đầu") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Kết thúc") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Định dạng: HH:mm (24h)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                enabled = label.isNotBlank(),
                onClick = { onSave(label, startTime, endTime) }
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}