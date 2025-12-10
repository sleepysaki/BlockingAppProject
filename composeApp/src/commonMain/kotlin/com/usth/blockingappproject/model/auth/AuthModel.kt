@file:Suppress("unused")
package com.usth.blockingappproject.model.auth

/**
 * DEPRECATED: Backward compatibility aliases.
 * 
 * Use [com.usth.blockingappproject.model.api] directly for new code.
 */

@Deprecated("Use LoginRequest from model.api", ReplaceWith("LoginRequest", "com.usth.blockingappproject.model.api.LoginRequest"))
typealias LoginRequest = com.usth.blockingappproject.model.api.LoginRequest

@Deprecated("Use AuthResponse from model.api", ReplaceWith("AuthResponse", "com.usth.blockingappproject.model.api.AuthResponse"))
typealias LoginResponse = com.usth.blockingappproject.model.api.AuthResponse

@Deprecated("Use AccessCheckRequest from model.api", ReplaceWith("AccessCheckRequest", "com.usth.blockingappproject.model.api.AccessCheckRequest"))
typealias CheckAccessRequest = com.usth.blockingappproject.model.api.AccessCheckRequest

@Deprecated("Use AccessCheckResponse from model.api", ReplaceWith("AccessCheckResponse", "com.usth.blockingappproject.model.api.AccessCheckResponse"))
typealias CheckAccessResponse = com.usth.blockingappproject.model.api.AccessCheckResponse