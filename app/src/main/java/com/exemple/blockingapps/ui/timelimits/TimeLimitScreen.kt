package com.exemple.blockingapps.ui.timelimits

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.home.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeLimitScreen(
    viewModel: HomeViewModel,
    geoViewModel: GeoBlockViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allApps by geoViewModel.appList.collectAsState()
    val context = LocalContext.current
    var selectedAppForLimit by remember { mutableStateOf<com.exemple.blockingapps.data.model.AppItem?>(null) }

    // Map dựa trên appId (Vì BlockedAppItem dùng appId làm định danh)
    val limitsMap = remember(uiState.blockedApps) {
        uiState.blockedApps.associateBy { it.appId }
    }

    // Tự động load app và đồng bộ khi mở màn hình
    LaunchedEffect(Unit) {
        geoViewModel.getInstalledApps(context)
        viewModel.refreshAllData(context) // Dùng hàm này để load full từ Disk
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Định mức thời gian", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Tổng quan
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tổng thời gian đã dùng hôm nay", style = MaterialTheme.typography.titleSmall)
                        Text("${uiState.totalUsageMinutesToday} phút", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Danh sách App và Progress
            items(allApps, key = { it.packageName }) { app ->
                // So khớp app cài trên máy với danh sách đang bị chặn/giới hạn
                val limitInfo = limitsMap[app.packageName]

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconBitmap = remember(app.packageName) {
                                app.icon?.toBitmap()?.asImageBitmap()
                            }

                            Box(Modifier.size(44.dp)) {
                                if (iconBitmap != null) {
                                    Image(bitmap = iconBitmap, contentDescription = null)
                                } else {
                                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(30.dp), tint = Color.Gray)
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(app.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                if (limitInfo != null && limitInfo.dailyLimitMinutes > 0) {
                                    val remMinutes = limitInfo.remainingSeconds / 60
                                    val remSeconds = limitInfo.remainingSeconds % 60
                                    Text(
                                        text = "Còn lại: ${remMinutes}m ${remSeconds}s / ${limitInfo.dailyLimitMinutes}p",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (limitInfo.remainingSeconds < 300) Color.Red else Color.Unspecified
                                    )
                                } else {
                                    Text("Chưa đặt giới hạn", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            IconButton(onClick = { selectedAppForLimit = app }) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Hiển thị thanh Progress nếu có đặt limit
                        if (limitInfo != null && limitInfo.dailyLimitMinutes > 0) {
                            Spacer(Modifier.height(10.dp))
                            val dailyLimitSeconds = (limitInfo.dailyLimitMinutes * 60).toFloat()
                            val progress = (1f - (limitInfo.remainingSeconds.toFloat() / dailyLimitSeconds)).coerceIn(0f, 1f)

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (progress > 0.9f) Color.Red else MaterialTheme.colorScheme.primary,
                                strokeCap = StrokeCap.Round,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog chỉnh sửa định mức
    if (selectedAppForLimit != null) {
        val appPkg = selectedAppForLimit!!.packageName
        val currentLimit = limitsMap[appPkg]?.dailyLimitMinutes ?: 60
        var sliderValue by remember(appPkg) { mutableStateOf(currentLimit.toFloat()) }

        AlertDialog(
            onDismissRequest = { selectedAppForLimit = null },
            title = { Text("Đặt giới hạn cho ${selectedAppForLimit!!.name}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${sliderValue.toInt()} phút/ngày", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..180f,
                        steps = 17
                    )
                    Text("0p", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAppLimit(context, appPkg, selectedAppForLimit!!.name, sliderValue.toInt())
                    selectedAppForLimit = null
                }) { Text("Lưu cấu hình") }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForLimit = null }) { Text("Hủy") }
            }
        )
    }
}