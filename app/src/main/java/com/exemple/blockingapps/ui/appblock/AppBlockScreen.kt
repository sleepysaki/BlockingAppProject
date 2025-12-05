package com.exemple.blockingapps.ui.appblock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.exemple.blockingapps.data.common.BlockState

@Composable
fun AppBlockScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Block ứng dụng ngay", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                BlockState.isBlocking = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BLOCK NGAY")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                BlockState.isBlocking = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("TẮT BLOCK")
        }
    }
}