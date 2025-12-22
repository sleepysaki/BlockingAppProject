package com.exemple.blockingapps.model
data class BlockRule(
    val packageName: String,
    val isBlocked: Boolean,
    val limitMinutes: Int?,
    val startTime: String?,
    val endTime: String?
)