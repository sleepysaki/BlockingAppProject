package com.exemple.blockingapps.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableAppScreen(
    onBack: () -> Unit,
    geoViewModel: GeoBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val apps by geoViewModel.appList.collectAsState()

    LaunchedEffect(Unit) {
        geoViewModel.getInstalledApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Apps") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps) { app ->
                ListItem(
                    leadingContent = {
                        val iconBitmap = remember(app.packageName) {
                            app.icon?.toBitmap()?.asImageBitmap()
                        }
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    headlineContent = {
                        Text(app.name)
                    }
                )
                Divider()
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}