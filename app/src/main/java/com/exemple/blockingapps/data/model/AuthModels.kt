// AuthModels.kt
package com.exemple.blockingapps.data.model

data class UserDTO(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class LoginResponse(
    val token: String,
    val user: UserDTO
)

// Used to send to the server when logging in
data class LoginRequest(
    val email: String,
    val password: String
)

// Used to send to the server during registration
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "PARENT" // Default is Parent
)