package com.usth.blockingappproject.data.api

import com.usth.blockingappproject.config.AppConfig
import com.usth.blockingappproject.data.model.*
import com.usth.blockingappproject.data.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class FocusGuardApi {
    private val client = KtorClient.client
    private val baseUrl = AppConfig.BASE_URL

    // Auth
    suspend fun login(email: String, pass: String): Result<AuthResponse> = runSafeApi {
        val response = client.post("$baseUrl/auth/login") {
            setBody(AuthRequest(email, pass))
        }
        // Save token globally for future requests
        val data: AuthResponse = response.body()
        KtorClient.authToken = data.token 
        data
    }

    suspend fun register(email: String, pass: String, role: String): Result<AuthResponse> = runSafeApi {
        client.post("$baseUrl/auth/register") {
            setBody(AuthRequest(email, pass, role))
        }.body()
    }

    // Member access check
    suspend fun checkAccess(
        userId: Int, 
        packageName: String, 
        lat: Double?, 
        lng: Double?
    ): Result<AccessCheckResponse> = runSafeApi {
        client.post("$baseUrl/rules/check") {
            setBody(AccessCheckRequest(userId, packageName, lat, lng))
        }.body()
    }

    // Admin management
    suspend fun getAppRules(childUserId: Int): Result<List<AppRuleDto>> = runSafeApi {
        client.get("$baseUrl/rules/$childUserId").body()
    }

    suspend fun updateAppRule(rule: AppRuleDto): Result<Boolean> = runSafeApi {
        client.put("$baseUrl/rules/update") {
            setBody(rule)
        }.status == HttpStatusCode.OK
    }
    
    // Face recognition
    suspend fun verifyFace(userId: Int, embeddings: List<Float>): Result<Boolean> = runSafeApi {
        client.post("$baseUrl/face/verify") {
            setBody(FaceVerificationRequest(userId, embeddings))
        }.body()
    }

    // Wrap crashes (internet loss, downtime) into a clean result object
    private suspend fun <T> runSafeApi(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.success(apiCall())
        } catch (e: Exception) {
            e.printStackTrace()
            // Parse error code here in production
            Result.failure(e)
        }
    }
}