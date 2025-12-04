package com.usth.blockingappproject.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usth.blockingappproject.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen() {
    // Mock Data (Dữ liệu giả để test UI)
    val appList = remember {
        mutableStateListOf(
            AppInfo("1", "Facebook", Icons.Default.PlayArrow, false),
            AppInfo("2", "TikTok", Icons.Default.PlayArrow, true),
            AppInfo("3", "YouTube", Icons.Default.PlayArrow, false),
            AppInfo("4", "Instagram", Icons.Default.PlayArrow, false),
            AppInfo("5", "Game ABC", Icons.Default.PlayArrow, true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Apps", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            items(appList) { app ->
                AppItemRow(app = app, onToggleBlock = { isBlocked ->
                    val index = appList.indexOf(app)
                    if (index != -1) {
                        appList[index] = appList[index].copy(isBlocked = isBlocked)
                    }
                })
            }
        }
    }
}

@Composable
fun AppItemRow(app: AppInfo, onToggleBlock: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    imageVector = if(app.isBlocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if(app.isBlocked) Color.Red else Color.Blue
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (app.isBlocked) Color.Gray else Color.Black
                    )
                    if (app.isBlocked) {
                        Text("Blocked", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }

            Switch(
                checked = app.isBlocked,
                onCheckedChange = { isChecked ->
                    onToggleBlock(isChecked)
                }
            )
        }
    }
}