@file:Suppress("unused")
package com.usth.blockingappproject.data.model

/**
 * DEPRECATED: This file provides backward compatibility aliases.
 * 
 * All DTOs have been consolidated into [com.usth.blockingappproject.model.api.ApiDtos].
 * New code should import directly from `model.api.*` instead.
 * 
 * This file will be removed in a future version.
 */

@Deprecated("Use LoginRequest from model.api", ReplaceWith("LoginRequest", "com.usth.blockingappproject.model.api.LoginRequest"))
typealias AuthRequest = com.usth.blockingappproject.model.api.LoginRequest

@Deprecated("Use AuthResponse from model.api", ReplaceWith("AuthResponse", "com.usth.blockingappproject.model.api.AuthResponse"))
typealias AuthResponse = com.usth.blockingappproject.model.api.AuthResponse

@Deprecated("Use AppRuleDto from model.api", ReplaceWith("AppRuleDto", "com.usth.blockingappproject.model.api.AppRuleDto"))
typealias AppRuleDto = com.usth.blockingappproject.model.api.AppRuleDto

@Deprecated("Use AccessCheckRequest from model.api", ReplaceWith("AccessCheckRequest", "com.usth.blockingappproject.model.api.AccessCheckRequest"))
typealias AccessCheckRequest = com.usth.blockingappproject.model.api.AccessCheckRequest

@Deprecated("Use AccessCheckResponse from model.api", ReplaceWith("AccessCheckResponse", "com.usth.blockingappproject.model.api.AccessCheckResponse"))
typealias AccessCheckResponse = com.usth.blockingappproject.model.api.AccessCheckResponse

@Deprecated("Use FaceVerificationRequest from model.api", ReplaceWith("FaceVerificationRequest", "com.usth.blockingappproject.model.api.FaceVerificationRequest"))
typealias FaceVerificationRequest = com.usth.blockingappproject.model.api.FaceVerificationRequest