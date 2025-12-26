package com.exemple.blockingapps.ui.group

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import java.util.Locale
import com.exemple.blockingapps.ui.appblock.AppBlockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: String,
    // --- KHÔI PHỤC CÁC THAM SỐ CŨ ĐỂ KHỚP VỚI APPNAVHOST ---
    groupName: String,
    joinCode: String,
    onBack: () -> Unit,
    viewModel: GroupViewModel = viewModel() // Giữ tham số này dù không dùng cho việc chặn app
    // --------------------------------------------------------
) {
    // 1. Vẫn khởi tạo AppBlockViewModel để xử lý chặn app
    val appBlockViewModel: AppBlockViewModel = viewModel()

    // 2. Lấy danh sách rules
    val rules by appBlockViewModel.rules.collectAsState()

    val context = LocalContext.current
    val groupIdLong = groupId.toLongOrNull() ?: 0L

    val commonApps = listOf(
        "com.google.android.youtube" to "YouTube",
        "com.facebook.katana" to "Facebook",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.google.android.gm" to "Gmail",
        "com.android.chrome" to "Chrome"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings: $groupName") }, // Có thể hiển thị tên nhóm ở đây
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Hiển thị mã tham gia (Join Code) nếu cần
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Group Info", style = MaterialTheme.typography.titleSmall)
                        Text("Join Code: $joinCode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text(
                    text = "App Blocking Rules",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(commonApps) { (pkg, name) ->
                val existingRule = rules.find { it.packageName == pkg }
                val isBlocked = existingRule?.isBlocked == true
                val startTime = existingRule?.startTime ?: "00:00"
                val endTime = existingRule?.endTime ?: "23:59"

                AppRuleItem(
                    appName = name,
                    packageName = pkg,
                    isBlocked = isBlocked,
                    startTime = startTime,
                    endTime = endTime,
                    onToggle = { newState ->
                        appBlockViewModel.updateRule(
                            groupId = groupIdLong,
                            packageName = pkg,
                            isBlocked = newState,
                            startTime = startTime,
                            endTime = endTime
                        )
                    },
                    onTimeChange = { newStart, newEnd ->
                        appBlockViewModel.updateRule(
                            groupId = groupIdLong,
                            packageName = pkg,
                            isBlocked = isBlocked,
                            startTime = newStart,
                            endTime = newEnd
                        )
                    },
                    context = context
                )
                Divider()
            }
        }
    }
}

// ... (Các hàm AppRuleItem, TimeSelector, showTimePicker giữ nguyên như cũ) ...
@Composable
fun AppRuleItem(
    appName: String,
    packageName: String,
    isBlocked: Boolean,
    startTime: String,
    endTime: String,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (String, String) -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle
            )
        }

        if (isBlocked) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeSelector(
                    label = "From",
                    time = startTime,
                    context = context,
                    onTimeSelected = { newTime -> onTimeChange(newTime, endTime) }
                )

                TimeSelector(
                    label = "To",
                    time = endTime,
                    context = context,
                    onTimeSelected = { newTime -> onTimeChange(startTime, newTime) }
                )
            }
        }
    }
}

@Composable
fun TimeSelector(
    label: String,
    time: String,
    context: Context,
    onTimeSelected: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                showTimePicker(context, time) { selectedTime ->
                    onTimeSelected(selectedTime)
                }
            }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Time",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(text = time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

fun showTimePicker(context: Context, currentTime: String, onTimePicked: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    try {
        val parts = currentTime.split(":")
        if (parts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
        }
    } catch (e: Exception) { e.printStackTrace() }

    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            onTimePicked(formattedTime)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}