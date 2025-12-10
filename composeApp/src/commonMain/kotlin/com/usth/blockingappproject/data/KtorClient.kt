package com.usth.blockingappproject.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Singleton HTTP client configured for the FocusGuard API.
 * 
 * Features:
 * - JSON serialization with kotlinx.serialization
 * - Automatic auth token injection
 * - Request/response logging (debug builds)
 * - Configurable timeouts
 * 
 * Usage:
 * ```
 * // After login, set the token
 * KtorClient.authToken = response.token
 * 
 * // All subsequent requests auto-include Authorization header
 * val data = KtorClient.client.get("...").body<MyData>()
 * ```
 */
object KtorClient {
    
    /**
     * JWT token for authenticated requests.
     * Set this after successful login; cleared on logout.
     * 
     * When set, all requests will include: `Authorization: Bearer <token>`
     */
    var authToken: String? = null

    /**
     * Pre-configured HTTP client instance.
     * Use this for all API calls to ensure consistent configuration.
     */
    val client = HttpClient {
        // JSON serialization/deserialization
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true           // Readable logs
                isLenient = true             // Accept slightly malformed JSON
                ignoreUnknownKeys = true     // Don't crash if server adds new fields
                encodeDefaults = true        // Include default values in requests
            })
        }

        // Network timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000   // Overall request timeout
            connectTimeoutMillis = 10_000   // Time to establish connection
            socketTimeoutMillis = 15_000    // Time between data packets
        }

        // Logging for debugging (disable in production or use BuildConfig flag)
        install(Logging) {
            level = LogLevel.BODY           // Log full request/response bodies
            logger = Logger.SIMPLE          // Use simple console logger
        }

        // Default headers for all requests
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            
            // Auto-inject auth token if available
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    /**
     * Clear auth state (call on logout).
     */
    fun clearAuth() {
        authToken = null
    }
}