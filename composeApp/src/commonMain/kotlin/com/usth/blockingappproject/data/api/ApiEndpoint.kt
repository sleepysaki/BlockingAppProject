package com.usth.blockingappproject.data.api

import com.usth.blockingappproject.config.AppConfig
import com.usth.blockingappproject.model.api.*
import com.usth.blockingappproject.data.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Main API client for the FocusGuard parental control system.
 * 
 * This class provides type-safe access to all backend endpoints.
 * All methods return [Result] to handle errors gracefully.
 * 
 * Usage:
 * ```
 * val api = FocusGuardApi()
 * val result = api.login("user@email.com", "password")
 * result.onSuccess { response -> /* use response.token */ }
 * result.onFailure { error -> /* handle error */ }
 * ```
 */
class FocusGuardApi {
    private val client = KtorClient.client
    private val baseUrl = AppConfig.BASE_URL

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    /**
     * Log in an existing user.
     * 
     * On success, the auth token is automatically stored in [KtorClient]
     * and will be included in subsequent requests.
     * 
     * @param email User's email address
     * @param password User's password
     * @param deviceId Optional device identifier for device registration
     * @param deviceName Optional human-readable device name
     * @return [AuthResponse] with token and user details
     */
    suspend fun login(
        email: String, 
        password: String, 
        deviceId: String? = null, 
        deviceName: String? = null
    ): Result<AuthResponse> = runSafeApi {
        val response = client.post("$baseUrl/auth/login") {
            setBody(LoginRequest(email, password, deviceId, deviceName))
        }
        val data: AuthResponse = response.body()
        // Auto-save token for future authenticated requests
        KtorClient.authToken = data.token 
        data
    }

    /**
     * Register a new user account.
     * 
     * @param email User's email (must be unique)
     * @param password Chosen password
     * @param role User role (use [UserRole.PARENT] or [UserRole.CHILD])
     * @param fullName User's display name
     * @return [AuthResponse] with token and user details
     */
    suspend fun register(
        email: String, 
        password: String, 
        role: UserRole, 
        fullName: String
    ): Result<AuthResponse> = runSafeApi {
        client.post("$baseUrl/auth/register") {
            setBody(RegisterRequest(email, password, fullName, role))
        }.body()
    }

    /**
     * Log out the current user.
     * Clears the stored auth token.
     */
    fun logout() {
        KtorClient.authToken = null
    }

    // =========================================================================
    // ACCESS CONTROL
    // =========================================================================

    /**
     * Check if a user can access a specific app right now.
     * 
     * Call this before launching an app or periodically during app usage.
     * 
     * @param userId Profile/user ID to check
     * @param packageName Android package name (e.g., "com.youtube")
     * @param latitude Optional current GPS latitude
     * @param longitude Optional current GPS longitude
     * @return [AccessCheckResponse] with allowed status and remaining time
     */
    suspend fun checkAccess(
        userId: Int, 
        packageName: String, 
        latitude: Double? = null, 
        longitude: Double? = null
    ): Result<AccessCheckResponse> = runSafeApi {
        client.post("$baseUrl/rules/check") {
            setBody(AccessCheckRequest(userId, packageName, latitude, longitude))
        }.body()
    }

    // Admin management
    suspend fun getAppRules(childUserId: Int): Result<List<AppRuleDto>> = runSafeApi {
        client.get("$baseUrl/rules/$childUserId").body()
    }

    /**
     * Create or update an app rule.
     * 
     * @param rule The rule to save (if id is null, creates new; otherwise updates)
     * @return true if successful
     */
    suspend fun saveAppRule(rule: AppRuleDto): Result<Boolean> = runSafeApi {
        client.put("$baseUrl/rules/update") {
            setBody(rule)
        }.status == HttpStatusCode.OK
    }

    /**
     * Delete an app rule.
     * 
     * @param ruleId ID of the rule to delete
     * @return true if successful
     */
    suspend fun deleteAppRule(ruleId: Int): Result<Boolean> = runSafeApi {
        client.delete("$baseUrl/rules/$ruleId").status == HttpStatusCode.OK
    }

    // =========================================================================
    // FACE RECOGNITION
    // =========================================================================

    /**
     * Verify a user's identity using face recognition.
     * 
     * @param userId User ID attempting verification
     * @param embeddings 128-dimensional face embedding from ML Kit
     * @return [FaceVerificationResponse] with match result and confidence
     */
    suspend fun verifyFace(
        userId: Int, 
        embeddings: List<Float>
    ): Result<FaceVerificationResponse> = runSafeApi {
        client.post("$baseUrl/face/verify") {
            setBody(FaceVerificationRequest(userId, embeddings))
        }.body()
    }

    /**
     * Register face data for a user.
     * 
     * @param userId User ID to register face for
     * @param embeddings 128-dimensional face embedding
     * @return true if successful
     */
    suspend fun registerFace(
        userId: Int, 
        embeddings: List<Float>
    ): Result<Boolean> = runSafeApi {
        client.post("$baseUrl/face/register") {
            setBody(FaceVerificationRequest(userId, embeddings))
        }.status == HttpStatusCode.Created
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    /**
     * Wraps API calls in try-catch and returns [Result].
     * Converts exceptions to [Result.failure] for clean error handling.
     */
    private suspend fun <T> runSafeApi(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.success(apiCall())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}