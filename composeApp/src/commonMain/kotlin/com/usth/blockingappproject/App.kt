package com.usth.blockingappproject

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.usth.blockingappproject.ui.AppListScreen // Import màn hình vừa tạo

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppListScreen()
    }
}