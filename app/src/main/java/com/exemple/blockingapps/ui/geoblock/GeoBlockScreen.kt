package com.exemple.blockingapps.ui.geoblock

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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.exemple.blockingapps.data.common.BlockState
import com.google.maps.android.compose.*
import android.widget.Toast

@Composable
fun GeoBlockScreen(viewModel: GeoBlockViewModel = viewModel()) {
    val context = LocalContext.current
    val apps by viewModel.appList.collectAsState()

    // Lưu địa điểm bác chọn trên Map
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Thiết lập Camera: Mặc định ban đầu, sau đó sẽ cập nhật theo vị trí máy ảo
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(21.0285, 105.8542), 15f)
    }

    // Hiển thị nút "Vị trí của tôi" và Chấm xanh trên Map
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    val properties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

    LaunchedEffect(Unit) {
        viewModel.getInstalledApps(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // PHẦN 1: BẢN ĐỒ (Chiếm 40% màn hình)
        Box(modifier = Modifier.weight(0.4f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapClick = { selectedLocation = it }
            ) {
                // Nếu bác chấm tay vào map, hiện cái Marker màu đỏ
                selectedLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = loc),
                        title = "Điểm chặn đã chọn"
                    )
                }
            }
        }

        // PHẦN 2: DANH SÁCH APP VÀ NÚT KÍCH HOẠT (60% màn hình)
        Card(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bước 1: Chọn ứng dụng muốn chặn",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

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
                            iconBitmap?.let {
                                Image(bitmap = it, contentDescription = null, modifier = Modifier.size(36.dp))
                            }
                            Text(app.name, modifier = Modifier.weight(1f).padding(start = 12.dp))
                            Checkbox(
                                checked = app.isSelected,
                                onCheckedChange = { viewModel.toggleAppSelection(app.packageName) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (selectedLocation == null) "Bước 2: Chấm 1 điểm trên bản đồ" else "Đã chọn tọa độ: ${"%.4f".format(selectedLocation?.latitude)}, ${"%.4f".format(selectedLocation?.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedLocation == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        selectedLocation?.let { loc ->
                            BlockState.restrictedApps = apps.filter { it.isSelected }.map { it.packageName }.toSet()
                            BlockState.targetLat = loc.latitude
                            BlockState.targetLng = loc.longitude

                            Toast.makeText(context, "KÍCH HOẠT THÀNH CÔNG! Đang canh gác vùng này.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    // Chỉ cho bấm khi đã chọn app VÀ đã chấm điểm trên map
                    enabled = selectedLocation != null && apps.any { it.isSelected }
                ) {
                    Text("KÍCH HOẠT CHẶN")
                }
            }
        }
    }
}