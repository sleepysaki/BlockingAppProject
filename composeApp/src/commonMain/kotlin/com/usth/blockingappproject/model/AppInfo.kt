package com.usth.blockingappproject.model

import androidx.compose.ui.graphics.vector.ImageVector

data class AppInfo(
    val id: String,
    val name: String,
    val icon: ImageVector?,
    var isBlocked: Boolean
)