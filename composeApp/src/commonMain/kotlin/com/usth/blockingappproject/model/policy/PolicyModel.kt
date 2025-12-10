package com.usth.blockingappproject.model.policy

import kotlinx.serialization.Serializable

/**
 * Models for app usage policies and restrictions.
 * 
 * Policies define rules like:
 * - Daily time limits per app
 * - Scheduled blocking (e.g., no games during school hours)
 * - Complete app blocking
 * - Focus/study sessions
 */

// =============================================================================
// ENUMS
// =============================================================================

/**
 * Types of policies that can be applied to apps.
 */
@Serializable
enum class PolicyType { 
    /** Limit daily usage (e.g., "2 hours per day") */
    DAILY_LIMIT,
    /** Block during specific time slots (e.g., "9 AM to 5 PM on weekdays") */
    SCHEDULED_BLOCK,
    /** Always blocked, no exceptions */
    ALWAYS_BLOCKED,
    /** Temporary focus session (e.g., 25-minute Pomodoro) */
    FOCUS_SESSION,
    /** Allow only during specific times (inverse of SCHEDULED_BLOCK) */
    ALLOWED_TIMES_ONLY
}

/**
 * Days of the week for scheduling.
 */
@Serializable
enum class DayOfWeek(val value: Int) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7)
}

// =============================================================================
// TIME SCHEDULING
// =============================================================================

/**
 * Defines a time slot when a policy is active.
 * 
 * Example: Block games from 9:00 to 15:00 on Monday through Friday
 * 
 * @property startHour Hour the slot starts (0-23)
 * @property startMinute Minute the slot starts (0-59)
 * @property endHour Hour the slot ends (0-23)
 * @property endMinute Minute the slot ends (0-59)
 * @property daysOfWeek Which days this slot applies (1=Mon, 7=Sun)
 */
@Serializable
data class TimeSlotDto(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // Default: all days
) {
    /**
     * Check if a given time falls within this slot.
     */
    fun containsTime(hour: Int, minute: Int, dayOfWeek: Int): Boolean {
        if (dayOfWeek !in daysOfWeek) return false
        
        val currentMinutes = hour * 60 + minute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        
        return currentMinutes in startMinutes until endMinutes
    }
}

// =============================================================================
// POLICIES
// =============================================================================

/**
 * Complete policy definition applied to one or more apps for a user.
 * 
 * @property id Unique policy identifier
 * @property targetProfileId User/profile this policy applies to
 * @property appPackageNames List of apps affected (e.g., ["com.youtube", "com.tiktok"])
 * @property policyType Type of restriction
 * @property limitMinutes For DAILY_LIMIT: max minutes per day
 * @property blockedSlots For SCHEDULED_BLOCK: when apps are blocked
 * @property allowedSlots For ALLOWED_TIMES_ONLY: when apps are allowed
 * @property blockedZoneIds Zones where this policy is enforced
 * @property isActive Whether policy is currently active
 * @property createdAt When policy was created
 * @property updatedAt When policy was last modified
 */
@Serializable
data class PolicyDto(
    val id: Int,
    val targetProfileId: Int,
    val appPackageNames: List<String>,
    val policyType: PolicyType,
    
    // Type-specific fields
    val limitMinutes: Int? = null,
    val blockedSlots: List<TimeSlotDto> = emptyList(),
    val allowedSlots: List<TimeSlotDto> = emptyList(),
    val blockedZoneIds: List<Int> = emptyList(),
    
    val isActive: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

/**
 * Request to create a new policy.
 */
@Serializable
data class CreatePolicyRequest(
    val targetProfileId: Int,
    val appPackageNames: List<String>,
    val policyType: PolicyType,
    val limitMinutes: Int? = null,
    val blockedSlots: List<TimeSlotDto> = emptyList(),
    val allowedSlots: List<TimeSlotDto> = emptyList(),
    val blockedZoneIds: List<Int> = emptyList()
)

/**
 * Request to update an existing policy.
 */
@Serializable
data class UpdatePolicyRequest(
    val id: Int,
    val appPackageNames: List<String>? = null,
    val policyType: PolicyType? = null,
    val limitMinutes: Int? = null,
    val blockedSlots: List<TimeSlotDto>? = null,
    val allowedSlots: List<TimeSlotDto>? = null,
    val blockedZoneIds: List<Int>? = null,
    val isActive: Boolean? = null
)

// =============================================================================
// SYNC
// =============================================================================

/**
 * Response when device syncs all policies from server.
 * 
 * @property policies All active policies for this user/device
 * @property serverTimestamp Server time for cache invalidation
 */
@Serializable
data class PolicySyncResponse(
    val policies: List<PolicyDto>,
    val serverTimestamp: Long
)