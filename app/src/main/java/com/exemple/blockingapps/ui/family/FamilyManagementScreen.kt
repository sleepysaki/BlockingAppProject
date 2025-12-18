package com.exemple.blockingapps.ui.family

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.exemple.blockingapps.ui.home.DeviceItem
import com.exemple.blockingapps.ui.home.HomeViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FamilyManagementScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDeviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quản lý Thiết bị Con") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDeviceDialog = true}) {
                Icon(Icons.Default.Add, contentDescription = "Thêm Thiết bị")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.devices.isEmpty()) {
                item {
                    Text("Chưa có thiết bị con nào được liên kết.", Modifier.padding(top = 16.dp))
                }
            } else {
                items(uiState.devices) { device ->
                    DeviceCard(
                        device = device,
                        onRemoveClicked = { viewModel.removeDevice(device.deviceId) }
                    )
                }
            }
        }

        if (showAddDeviceDialog) {
            AddDeviceDialog(
                onDismiss = { showAddDeviceDialog = false },
                onAddDevice = { deviceName ->
                    val newDeviceId = UUID.randomUUID().toString().substring(0, 8)
                    viewModel.addDevice(deviceName, newDeviceId)
                    showAddDeviceDialog = false
                }
            )
        }
    }
}


@Composable
fun DeviceCard(device: DeviceItem, onRemoveClicked: () -> Unit) {

    val isOnline = device.isConnected

    val statusColor = if (isOnline) Color(0xFF4CAF50) else Color.Gray
    val statusIcon = if (isOnline) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Trạng thái",
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(device.deviceName, style = MaterialTheme.typography.titleMedium)

                    Text(
                        text = if (isOnline) "TRỰC TUYẾN" else "Ngoại tuyến",
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (!isOnline) {
                        Text("Hoạt động cuối: ${device.lastActive}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Kết nối ổn định", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            IconButton(onClick = onRemoveClicked) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa Thiết bị", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onAddDevice: (deviceName: String) -> Unit
) {
    var deviceName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Thiết bị Con Mới") },
        text = {
            Column {
                Text("Nhập tên cho thiết bị con (ví dụ: 'Kid - Tablet')")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Tên Thiết bị") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Lưu ý: Thiết bị con cần phải cài đặt ứng dụng và nhập mã liên kết.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deviceName.isNotBlank()) {
                        onAddDevice(deviceName)
                    }
                },
                enabled = deviceName.isNotBlank()
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}