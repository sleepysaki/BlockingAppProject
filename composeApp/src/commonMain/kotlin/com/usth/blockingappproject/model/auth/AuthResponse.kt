@file:Suppress("unused")
package com.usth.blockingappproject.model.auth

/**
 * DEPRECATED: Backward compatibility aliases.
 * 
 * Use [com.usth.blockingappproject.model.api] directly for new code.
 */

@Deprecated("Use AuthResponse from model.api", ReplaceWith("AuthResponse", "com.usth.blockingappproject.model.api.AuthResponse"))
typealias AuthResponse = com.usth.blockingappproject.model.api.AuthResponse

@Deprecated("Use UserProfileDto from model.api", ReplaceWith("UserProfileDto", "com.usth.blockingappproject.model.api.UserProfileDto"))
typealias UserProfileDto = com.usth.blockingappproject.model.api.UserProfileDto

@Deprecated("Use UserRole from model.api", ReplaceWith("UserRole", "com.usth.blockingappproject.model.api.UserRole"))
typealias UserRole = com.usth.blockingappproject.model.api.UserRole