package com.usth.blockingappproject.model.realtime

import kotlinx.serialization.Serializable

@Serializable
data class AccessCheckRequest(
    val profileId: Int,
    val packageName: String,
    val currentLat: Double? = null,
    val currentLong: Double? = null,
    val timestamp: Long
)

@Serializable
data class AccessCheckResponse(
    val isAllowed: Boolean,
    val reason: String? = null, // "Time Limit Reached", "School Zone"
    val remainingTimeMinutes: Int
)

@Serializable
enum class CommandType { LOCK_DEVICE, UNLOCK_DEVICE, SEND_WARNING, SYNC_POLICIES }

@Serializable
data class DeviceCommandDto(
    val commandId: String,
    val targetDeviceId: String,
    val type: CommandType,
    val message: String? = null, // e.g. "Go to sleep!"
    val durationMinutes: Int? = null // Lock for X minutes
)