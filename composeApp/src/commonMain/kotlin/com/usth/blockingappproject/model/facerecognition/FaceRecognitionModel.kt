package com.usth.blockingappproject.model.face

import kotlinx.serialization.Serializable

@Serializable
data class FaceVerificationRequest(
    val deviceId: String,
    val faceEmbedding: List<Float> // vector from ML Kit
)

@Serializable
data class FaceVerificationResponse(
    val isMatch: Boolean,
    val matchedProfileId: Int?,
    val confidence: Float, // 0.0 to 1.0
    val authToken: String? // Temp token for the identified user
)