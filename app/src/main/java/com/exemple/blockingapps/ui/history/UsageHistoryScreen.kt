package com.exemple.blockingapps.ui.history

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exemple.blockingapps.data.model.DailyUsageSummary
import com.exemple.blockingapps.data.model.UsageCategory
import com.exemple.blockingapps.data.model.UsageRecord
import com.exemple.blockingapps.ui.home.*

fun minutesToHours(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}g ${m}ph" else "${m}ph"
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UsageHistoryScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDeviceId = uiState.selectedDeviceId
    val historyData: List<DailyUsageSummary>? = uiState.usageHistory[selectedDeviceId]
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadWeeklyData(context)
    }

    if (selectedDeviceId == null) {
        DeviceSelectionScreen(uiState.devices, viewModel::selectDeviceForHistory)
    } else {
        HistoryDetailScreen(
            devices = uiState.devices,
            selectedDeviceId = selectedDeviceId,
            historyData = historyData,
            onBack = viewModel::clearSelectedDevice
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionScreen(
    devices: List<DeviceItem>,
    onDeviceSelected: (String) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Lịch sử Sử dụng") }) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Chọn thiết bị con để xem lịch sử sử dụng chi tiết theo ngày:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device.deviceId) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(device.deviceName, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${device.deviceId}", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Xem chi tiết")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    devices: List<DeviceItem>,
    selectedDeviceId: String,
    historyData: List<DailyUsageSummary?>?,
    onBack: () -> Unit
) {
    val deviceName = devices.find { it.deviceId == selectedDeviceId }?.deviceName ?: "Thiết bị không xác định"

    var dayIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val currentSummary = historyData?.getOrNull(dayIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử: $deviceName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (currentSummary == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Không có dữ liệu cho ngày này.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (dayIndex < (historyData?.size?.minus(1) ?: 0)) dayIndex++ },
                            enabled = dayIndex < (historyData?.size?.minus(1) ?: 0)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Ngày trước")
                        }

                        Text(
                            text = currentSummary.date,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = { if (dayIndex > 0) dayIndex-- },
                            enabled = dayIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Ngày sau")
                        }
                    }
                }

                item { UsageSummaryCard(currentSummary) }
                item { UsageBarChart(currentSummary.hourlyUsage) }
                item { UsageCategoryBreakdown(currentSummary.categories) }
                item {
                    Text("DÙNG NHIỀU NHẤT", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                }

                items(currentSummary.topApps) { record ->
                    TopAppUsageRow(record, currentSummary.totalScreenTimeMinutes)
                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
@Composable
fun DatePickerHeader(date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* TODO: Implement change date */ }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Ngày trước") }
        Text(date, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { /* TODO: Implement change date */ }) { Icon(Icons.Default.ChevronRight, contentDescription = "Ngày sau") }
    }
}

@Composable
fun UsageSummaryCard(summary: DailyUsageSummary) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = minutesToHours(summary.totalScreenTimeMinutes),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
            fontWeight = FontWeight.ExtraBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isIncrease = summary.comparisonPercent.startsWith("+")
            Icon(
                if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = "Thay đổi",
                modifier = Modifier.size(18.dp),
                tint = if (isIncrease) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(4.dp))
            Text(
                summary.comparisonPercent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Cập nhật: 05:35 Hôm nay",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun UsageBarChart(hourlyUsage: List<Int>) {
    val maxUsage = (hourlyUsage.maxOrNull() ?: 1).coerceAtLeast(60)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyUsage.forEachIndexed { index, minutes ->
                val barHeight = (minutes.toFloat() / maxUsage).coerceIn(0.05f, 1f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barHeight)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (minutes > 0) MaterialTheme.colorScheme.primary
                            else Color.LightGray.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 4, 8, 12, 16, 20, 23).forEach { hour ->
                Text("${hour}h", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}


@Composable
fun UsageCategoryBreakdown(categories: List<UsageCategory>) {
    Divider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        categories.forEach { category ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(category.categoryName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = minutesToHours(category.totalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Divider()
}

@Composable
fun TopAppUsageRow(record: UsageRecord, totalTime: Int) {
    val appIcon: ImageVector = remember(record.appName) {
        when (record.appName) {
            "Facebook" -> Icons.Default.Facebook
            "TikTok" -> Icons.Default.MusicNote
            "YouTube" -> Icons.Default.PlayArrow
            else -> Icons.Default.PhoneAndroid
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                appIcon,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray.copy(0.3f)).padding(6.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(minutesToHours(record.totalMinutes), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(6.dp))

                val progress = (record.totalMinutes.toFloat() / totalTime).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        }
    }
}