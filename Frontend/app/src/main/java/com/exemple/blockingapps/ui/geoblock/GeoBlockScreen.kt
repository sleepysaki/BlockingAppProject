package com.exemple.blockingapps.ui.geoblock

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.data.local.SessionManager // 👈 Nhớ import SessionManager
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun GeoBlockScreen(viewModel: GeoBlockViewModel = viewModel()) {
    val context = LocalContext.current

    // 1. Collect dữ liệu từ ViewModel
    val apps by viewModel.appList.collectAsState()
    val userGroups by viewModel.userGroups.collectAsState() // 👇 Lấy danh sách nhóm

    // 2. Lấy User ID hiện tại để tải nhóm
    val currentUserId = remember { SessionManager.getUserId(context) }

    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(21.0285, 105.8542), 15f)
    }

    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    val properties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

    // 3. Khởi chạy: Tải App và Tải Nhóm
    LaunchedEffect(Unit) {
        viewModel.getInstalledApps(context)
        if (!currentUserId.isNullOrEmpty()) {
            viewModel.fetchUserGroups(currentUserId) // 👇 Gọi API lấy nhóm
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- MAP SECTION ---
        Box(modifier = Modifier.weight(0.4f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    Toast.makeText(context, "Location Selected: ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
                }
            ) {
                selectedLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = loc),
                        title = "Target Location"
                    )
                }
            }
        }

        // --- CONTROL PANEL SECTION ---
        Card(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Step 1: Select Apps to Block",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // App List
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items = apps, key = { it.packageName }) { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val iconBitmap = remember(app.packageName) {
                                app.icon?.toBitmap()?.asImageBitmap()
                            }
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = app.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                maxLines = 1
                            )

                            Checkbox(
                                checked = app.isSelected,
                                onCheckedChange = { viewModel.toggleAppSelection(app.packageName) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location Info
                Text(
                    text = if (selectedLocation == null)
                        "Step 2: Tap on Map to select location"
                    else
                        "Target: ${"%.4f".format(selectedLocation?.latitude)}, ${"%.4f".format(selectedLocation?.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedLocation == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                // --- UPDATED BUTTON LOGIC ---
                Button(
                    onClick = {
                        // A. Validation cơ bản
                        if (selectedLocation == null) {
                            Toast.makeText(context, "❌ Please select a location on the map!", Toast.LENGTH_SHORT).show()
                        } else if (!apps.any { it.isSelected }) {
                            Toast.makeText(context, "❌ Please select at least one app!", Toast.LENGTH_SHORT).show()
                        } else {
                            // B. Lấy Group ID (Logic: Lấy nhóm đầu tiên tìm thấy)
                            // Nếu bạn muốn user chọn nhóm, bạn cần thêm DropdownMenu ở UI.
                            // Ở đây mình lấy mặc định nhóm đầu tiên cho nhanh.
                            val targetGroup = userGroups.firstOrNull()

                            if (targetGroup != null) {
                                // ✅ GỌI HÀM VỚI ID NHÓM
                                selectedLocation?.let { loc ->
                                    viewModel.activateBlocking(
                                        context,
                                        loc.latitude,
                                        loc.longitude,
                                        targetGroup.groupId // Truyền ID vào đây
                                    )
                                }
                            } else {
                                Toast.makeText(context, "⚠️ No Family Group found! Create a group first.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = true
                ) {
                    Text("ACTIVATE GEO-BLOCK (SERVER)")
                }
            }
        }
    }
}