package com.usth.blockingappproject.data.model

import kotlinx.serialization.Serializable

// Auth
@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val role: String? = null // "PARENT" or "CHILD", optional for Login
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val role: String,
    val requiresFaceSetup: Boolean // To trigger Req 5 flow
)

// App rules
@Serializable
data class AppRuleDto(
    val id: Int? = null,
    val packageName: String, // "com.youtube"
    val appName: String,     // "YouTube"
    val dailyLimitMinutes: Int,
    val isBlocked: Boolean,
    val zoneId: Int? = null
)

// Access check
@Serializable
data class AccessCheckRequest(
    val userId: Int,
    val packageName: String,
    val latitude: Double? = null, // Nullable if GPS is off
    val longitude: Double? = null
)

@Serializable
data class AccessCheckResponse(
    val isAllowed: Boolean,
    val remainingTimeMinutes: Int,
    val reason: String? = null // e.g., "Blocked by Location Zone: School"
)

// Face Recognition
@Serializable
data class FaceVerificationRequest(
    val userId: Int,
    val faceEmbedding: List<Float> // The vector data from ML Kit
)