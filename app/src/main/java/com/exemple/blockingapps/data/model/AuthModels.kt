package com.exemple.blockingapps.data.model

import com.google.gson.annotations.SerializedName

// --- REQUESTS (Gửi đi) ---

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String
)

// --- RESPONSES (Nhận về) ---

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserInfo
)

data class UserInfo(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("role") val role: String
)

data class RegisterResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)