package com.exemple.blockingapps.model
data class BlockRule(
    val packageName: String,
    val isBlocked: Boolean,
    val limitMinutes: Int?,
    val startTime: String?,
    val endTime: String?,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val radius: Double? = 100.0
)