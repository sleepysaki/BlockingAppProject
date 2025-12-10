package com.usth.blockingappproject.model.realtime

import kotlinx.serialization.Serializable

/**
 * Real-time communication models for device commands and live access checks.
 * 
 * These are used for WebSocket/push notification payloads and
 * immediate parent-to-device commands.
 */

// =============================================================================
// DEVICE COMMANDS (Parent → Child Device)
// =============================================================================

/**
 * Types of commands a parent can send to a child's device.
 */
@Serializable
enum class CommandType {
    /** Immediately lock the device */
    LOCK_DEVICE,
    /** Unlock a previously locked device */
    UNLOCK_DEVICE,
    /** Send a warning notification to the child */
    SEND_WARNING,
    /** Force the device to re-sync policies from server */
    SYNC_POLICIES,
    /** Start a focus/study session */
    START_FOCUS_SESSION,
    /** End an active focus session early */
    END_FOCUS_SESSION
}

/**
 * Command payload sent from parent to child device.
 * 
 * @property commandId Unique ID for tracking/acknowledgment
 * @property targetDeviceId Device to receive this command
 * @property type What action to perform
 * @property message Optional message to display (e.g., "Time for bed!")
 * @property durationMinutes Optional duration (for locks or focus sessions)
 */
@Serializable
data class DeviceCommandDto(
    val commandId: String,
    val targetDeviceId: String,
    val type: CommandType,
    val message: String? = null,
    val durationMinutes: Int? = null
)

/**
 * Acknowledgment from device after receiving a command.
 */
@Serializable
data class CommandAckDto(
    val commandId: String,
    val deviceId: String,
    val success: Boolean,
    val errorMessage: String? = null
)

// =============================================================================
// REAL-TIME ACCESS CHECK (for WebSocket-based instant blocking)
// =============================================================================

/**
 * Real-time access check request with additional context.
 * 
 * This extends the basic AccessCheckRequest with timestamp and profile ID
 * for real-time WebSocket-based access control.
 * 
 * For REST API access checks, use [com.usth.blockingappproject.model.api.AccessCheckRequest]
 */
@Serializable
data class RealTimeAccessCheckRequest(
    val profileId: Int,
    val packageName: String,
    val currentLat: Double? = null,
    val currentLong: Double? = null,
    val timestamp: Long
)