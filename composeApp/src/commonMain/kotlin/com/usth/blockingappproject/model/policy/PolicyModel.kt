package com.usth.blockingappproject.model.policy

import kotlinx.serialization.Serializable

@Serializable
enum class PolicyType { 
    DAILY_LIMIT,      // e.g., "2 hours per day"
    FIXED_SCHEDULE,   // e.g., "Blocked from 9 AM to 5 PM"
    STRICT_BLOCK,     // Always blocked
    FOCUS_SESSION     // Temporary session (e.g., Pomodoro)
}

@Serializable
data class TimeSlotDto(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int> // 1=Mon, 7=Sun
)

@Serializable
data class PolicyDto(
    val id: Int,
    val targetProfileId: Int, // The child/user this applies to
    val appPackageNames: List<String>, // List of apps (e.g., all games)
    val policyType: PolicyType,
    
    // Conditionals
    val limitMinutes: Int? = null,        // For DAILY_LIMIT
    val blockedSlots: List<TimeSlotDto> = emptyList(), // For FIXED_SCHEDULE
    val blockedZoneIds: List<Int> = emptyList(), // For Location-based blocking
    val isActive: Boolean = true
)


// Used when the app starts up 
data class PolicySyncResponse(
    val policies: List<PolicyDto>,
    val serverTimestamp: Long
)