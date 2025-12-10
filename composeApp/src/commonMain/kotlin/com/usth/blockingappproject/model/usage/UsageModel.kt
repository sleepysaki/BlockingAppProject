package com.usth.blockingappproject.model.usage

import kotlinx.serialization.Serializable

/**
 * Models for tracking and reporting app usage and policy violations.
 * 
 * These DTOs are used for:
 * - Uploading usage logs from device to server
 * - Reporting policy violations
 * - Parent dashboard analytics
 */

// =============================================================================
// USAGE LOGGING
// =============================================================================

/**
 * Type of policy violation that occurred.
 */
@Serializable
enum class ViolationType {
    /** User entered a blocked geo-zone while using restricted app */
    ZONE_VIOLATION,
    /** User exceeded daily time limit for an app */
    TIME_LIMIT_EXCEEDED,
    /** User attempted to open a blocked app */
    BLOCKED_APP_ATTEMPT,
    /** User attempted to use app outside allowed schedule */
    SCHEDULE_VIOLATION
}

/**
 * Single app usage session log entry.
 * 
 * @property profileId The user/profile who used the app
 * @property packageName Android package name of the app
 * @property startTime Session start timestamp (epoch millis)
 * @property endTime Session end timestamp (epoch millis)
 * @property durationSeconds Total duration (endTime - startTime) / 1000
 */
@Serializable
data class AppUsageLogDto(
    val profileId: Int,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long
)

/**
 * Record of a policy violation attempt.
 * 
 * @property profileId The user who violated the policy
 * @property packageName App involved in the violation
 * @property attemptedAt Timestamp of the violation attempt
 * @property violationType Type of violation
 * @property locationLat GPS latitude when violation occurred (if available)
 * @property locationLong GPS longitude when violation occurred (if available)
 */
@Serializable
data class ViolationReportDto(
    val profileId: Int,
    val packageName: String,
    val attemptedAt: Long,
    val violationType: ViolationType,
    val locationLat: Double? = null,
    val locationLong: Double? = null
)

// =============================================================================
// BATCH UPLOAD (for offline sync)
// =============================================================================

/**
 * Request to upload a batch of collected logs and violations.
 * 
 * Used when the device comes back online after being offline,
 * or for periodic batch uploads to reduce API calls.
 */
@Serializable
data class BatchLogRequest(
    val deviceId: String,
    val usageLogs: List<AppUsageLogDto>,
    val violations: List<ViolationReportDto>
)

/**
 * Response after batch upload.
 */
@Serializable
data class BatchLogResponse(
    val logsReceived: Int,
    val violationsReceived: Int,
    val serverTimestamp: Long
)

// =============================================================================
// ANALYTICS SUMMARIES (for parent dashboard)
// =============================================================================

/**
 * Daily usage summary for a single app.
 */
@Serializable
data class DailyAppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val limitMinutes: Int?,
    val isOverLimit: Boolean
)

/**
 * Complete daily usage report for a profile.
 */
@Serializable
data class DailyUsageReport(
    val profileId: Int,
    val profileName: String,
    val date: String, // "2025-12-10"
    val totalScreenTimeMinutes: Int,
    val appUsage: List<DailyAppUsageSummary>,
    val violationCount: Int
)