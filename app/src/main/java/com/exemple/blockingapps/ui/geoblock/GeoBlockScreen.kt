package com.exemple.blockingapps.ui.geoblock

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun GeoBlockScreen(viewModel: GeoBlockViewModel = viewModel()) {
    val context = LocalContext.current
    val apps by viewModel.appList.collectAsState()

    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(21.0285, 105.8542), 15f)
    }

    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    val properties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

    LaunchedEffect(Unit) {
        viewModel.getInstalledApps(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.4f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                onMapClick = { selectedLocation = it }
            ) {
                selectedLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = loc),
                        title = "Điểm chặn đã chọn"
                    )
                }
            }
        }

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
                            viewModel.saveGeoBlocking(context, loc)
                            Toast.makeText(context, "KÍCH HOẠT THÀNH CÔNG!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = selectedLocation != null && apps.any { it.isSelected }
                ) {
                    Text("KÍCH HOẠT CHẶN")
                }
            }
        }
    }
}