package com.usth.blockingappproject.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    
    // Store this token in a local DB or Settings after login
    var authToken: String? = null

    val client = HttpClient {
        // JSON configuration
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // CRITICAL for production: Prevents crash if backend adds new fields
            })
        }

        // Timeout configuration 
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 15000
        }

        // Network call logging
        install(Logging) {
            level = LogLevel.ALL 
            logger = Logger.SIMPLE
        }

        // Default headers (Auto-inject Token)
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            
            // If we have a logged-in user, send their token
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}