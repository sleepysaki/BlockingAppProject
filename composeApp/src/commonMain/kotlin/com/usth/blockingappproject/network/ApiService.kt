package com.usth.blockingappproject.network

import com.usth.blockingappproject.config.AppConfig
import com.usth.blockingappproject.data.network.KtorClient
import com.usth.blockingappproject.model.api.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * High-level API service for common operations.
 * 
 * This is a simplified alternative to [FocusGuardApi] for basic use cases.
 * Consider using [FocusGuardApi] for more complete API coverage.
 */
class ApiService {
    private val client = KtorClient.client
    private val baseUrl = AppConfig.BASE_URL

    /**
     * Authenticate user and obtain session token.
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = client.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            // Store token for subsequent requests
            KtorClient.authToken = response.token
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun checkAppAccess(packageName: String, userId: Int): Boolean {
        return try {
            val response: AccessCheckResponse = client.post("$baseUrl/rules/check") {
                contentType(ContentType.Application.Json)
                setBody(AccessCheckRequest(userId, packageName))
            }.body()
            
            response.isAllowed
        } catch (e: Exception) {
            true // If API fails, default to allow (fail-safe) or block depending on policy
        }
    }
}