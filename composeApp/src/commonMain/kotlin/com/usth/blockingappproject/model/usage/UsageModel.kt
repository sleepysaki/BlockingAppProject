package com.usth.blockingappproject.model.analytics

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageLogDto(
    val profileId: Int,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
)

@Serializable
data class ViolationReportDto(
    val profileId: Int,
    val packageName: String,
    val attemptedAt: Long,
    val violationType: String, // "ZONE_VIOLATION", "TIME_LIMIT"
    val locationLat: Double? = null,
    val locationLong: Double? = null
)

// For uploading a batch of offline logs
@Serializable
data class BatchLogRequest(
    val usageLogs: List<AppUsageLogDto>,
    val violations: List<ViolationReportDto>
)