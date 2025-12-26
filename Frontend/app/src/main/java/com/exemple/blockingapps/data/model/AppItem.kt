package com.exemple.blockingapps.data.model

import android.graphics.drawable.Drawable

data class AppItem (
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isSelected: Boolean = false
)