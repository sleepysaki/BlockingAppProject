package com.usth.blockingappproject.model.api

import kotlinx.serialization.Serializable

/**
 * Canonical network DTOs shared between client and server.
 * 
 * This file is the single source of truth for all API request/response models.
 * Both Android client and Ktor server import from here to ensure type consistency.
 */

// =============================================================================
// ENUMS
// =============================================================================

/**
 * User roles in the system.
 * - PARENT: Can manage children, set policies, view reports
 * - CHILD: Subject to policies, usage tracked
 * - ADMIN: System administrator (future use)
 */
@Serializable
enum class UserRole {
    PARENT,
    CHILD,
    ADMIN
}

/**
 * Types of access denial reasons returned by the server.
 */
@Serializable
enum class AccessDenialReason {
    TIME_LIMIT_EXCEEDED,
    BLOCKED_BY_SCHEDULE,
    BLOCKED_BY_ZONE,
    APP_BLOCKED,
    DEVICE_LOCKED
}

// =============================================================================
// AUTHENTICATION
// =============================================================================

/**
 * Request to log in an existing user.
 * 
 * @property email User's email address
 * @property password User's password (sent over HTTPS, hashed server-side)
 * @property deviceId Optional hardware device identifier for device registration
 * @property deviceName Optional human-readable device name (e.g., "John's Phone")
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null
)

/**
 * Request to register a new user account.
 * 
 * @property email User's email address (must be unique)
 * @property password User's chosen password
 * @property fullName User's display name
 * @property role User role: "PARENT" or "CHILD"
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: UserRole
)

/**
 * Response after successful authentication (login or register).
 * 
 * @property token JWT token for subsequent API calls
 * @property userId Unique user identifier
 * @property workspaceId Optional workspace/family group ID
 * @property role User's role in the system
 * @property profiles List of profiles (for parents: their children's profiles)
 */
@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val workspaceId: String? = null,
    val role: UserRole,
    val profiles: List<UserProfileDto> = emptyList()
)

/**
 * Represents a user profile (typically a child managed by a parent).
 * 
 * @property id Unique profile identifier
 * @property name Display name
 * @property isChild Whether this profile is a child (subject to restrictions)
 * @property hasFaceData Whether face recognition data has been set up
 */
@Serializable
data class UserProfileDto(
    val id: String,
    val name: String,
    val isChild: Boolean,
    val hasFaceData: Boolean
)

// =============================================================================
// APP RULES & ACCESS CONTROL
// =============================================================================

/**
 * Represents a rule/policy for a specific app.
 * 
 * @property id Rule ID (null when creating new)
 * @property packageName Android package name (e.g., "com.youtube")
 * @property appName Human-readable app name (e.g., "YouTube")
 * @property dailyLimitMinutes Maximum daily usage in minutes (0 = no limit)
 * @property isBlocked Whether the app is completely blocked
 * @property zoneId Optional: only apply this rule in a specific geo-zone
 */
@Serializable
data class AppRuleDto(
    val id: Int? = null,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 0,
    val isBlocked: Boolean = false,
    val zoneId: Int? = null
)

/**
 * Request to check if a user can access a specific app right now.
 * Called by the client before launching or while using an app.
 * 
 * @property userId User/profile ID making the request
 * @property packageName App package name to check
 * @property latitude Optional current GPS latitude (for zone-based rules)
 * @property longitude Optional current GPS longitude
 */
@Serializable
data class AccessCheckRequest(
    val userId: Int,
    val packageName: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Response indicating whether app access is allowed.
 * 
 * @property isAllowed Whether the user can use the app
 * @property remainingTimeMinutes Minutes left in daily allowance (0 if blocked)
 * @property reason Human-readable reason if denied (e.g., "Daily limit reached")
 * @property denialReason Structured denial reason enum (for programmatic handling)
 */
@Serializable
data class AccessCheckResponse(
    val isAllowed: Boolean,
    val remainingTimeMinutes: Int = 0,
    val reason: String? = null,
    val denialReason: AccessDenialReason? = null
)

// =============================================================================
// FACE RECOGNITION
// =============================================================================

/**
 * Request to verify a user's identity via face recognition.
 * 
 * @property userId User ID attempting verification
 * @property faceEmbedding 128-dimensional face embedding vector from ML Kit
 */
@Serializable
data class FaceVerificationRequest(
    val userId: Int,
    val faceEmbedding: List<Float>
)

/**
 * Response from face verification attempt.
 * 
 * @property isMatch Whether the face matched the stored embedding
 * @property matchedProfileId Profile ID that matched (if successful)
 * @property confidence Match confidence score (0.0 to 1.0)
 * @property authToken Optional temporary auth token for the verified user
 */
@Serializable
data class FaceVerificationResponse(
    val isMatch: Boolean,
    val matchedProfileId: Int? = null,
    val confidence: Float = 0f,
    val authToken: String? = null
)

// =============================================================================
// GENERIC API RESPONSES
// =============================================================================

/**
 * Generic success response for operations that don't return data.
 */
@Serializable
data class ApiSuccessResponse(
    val success: Boolean = true,
    val message: String? = null
)

/**
 * Generic error response structure.
 */
@Serializable
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: String,
    val errorCode: String? = null
)
