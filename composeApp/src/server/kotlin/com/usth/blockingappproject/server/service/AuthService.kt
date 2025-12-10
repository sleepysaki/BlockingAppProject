package com.usth.blockingappproject.server.service

import com.usth.blockingappproject.model.api.*
import com.usth.blockingappproject.server.database.Devices
import com.usth.blockingappproject.server.database.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Service handling user authentication (registration, login).
 * 
 * TODO: Replace plain password storage with proper hashing (bcrypt/argon2)
 * TODO: Replace mock JWT with real JWT generation using a library
 */
class AuthService {

    /**
     * Register a new user account.
     * 
     * @param request Registration details
     * @return AuthResponse on success, null if email already exists
     */
    suspend fun registerUser(request: RegisterRequest): AuthResponse? {
        return transaction {
            // Check if email already registered
            val existingUser = Users.select { Users.email eq request.email }.singleOrNull()
            if (existingUser != null) return@transaction null

            // Create user record
            val newUserId = Users.insertAndGetId {
                it[email] = request.email
                // WARNING: Storing plain password - replace with hash in production!
                it[passwordHash] = request.password
                it[fullName] = request.fullName
                it[role] = request.role.name  // Store enum as string
                it[createdAt] = LocalDateTime.now()
            }

            AuthResponse(
                token = generateToken(newUserId.value.toString()),
                userId = newUserId.value.toString(),
                workspaceId = null,
                role = request.role,
                profiles = emptyList()
            )
        }
    }

    /**
     * Authenticate an existing user.
     * 
     * @param request Login credentials
     * @return AuthResponse on success, null if credentials invalid
     */
    suspend fun loginUser(request: LoginRequest): AuthResponse? {
        return transaction {
            // Find user by email
            val userRow = Users.select { Users.email eq request.email }.singleOrNull()
                ?: return@transaction null

            // Verify password (plain comparison - use hash verification in production!)
            val storedHash = userRow[Users.passwordHash]
            if (storedHash != request.password) return@transaction null
            
            val userIdUuid = userRow[Users.id].value

            // Register/update device if deviceId provided
            request.deviceId?.let { deviceId ->
                val deviceExists = Devices.select { Devices.deviceId eq deviceId }.any()
                
                if (deviceExists) {
                    Devices.update({ Devices.deviceId eq deviceId }) {
                        it[userId] = userIdUuid
                        it[deviceName] = request.deviceName
                        it[lastSyncedAt] = LocalDateTime.now()
                    }
                } else {
                    Devices.insert {
                        it[Devices.deviceId] = deviceId
                        it[userId] = userIdUuid
                        it[deviceName] = request.deviceName
                        it[lastSyncedAt] = LocalDateTime.now()
                    }
                }
            }

            // Parse stored role string back to enum
            val userRole = try {
                UserRole.valueOf(userRow[Users.role])
            } catch (e: IllegalArgumentException) {
                UserRole.CHILD // Default fallback
            }

            AuthResponse(
                token = generateToken(userIdUuid.toString()),
                userId = userIdUuid.toString(),
                workspaceId = null, // TODO: Fetch from GroupMembers
                role = userRole,
                profiles = emptyList() // TODO: Fetch child profiles for parents
            )
        }
    }

    /**
     * Generate authentication token.
     * 
     * TODO: Replace with real JWT generation using io.ktor:ktor-server-auth-jwt
     */
    private fun generateToken(userId: String): String {
        return "mock-jwt-token-for-$userId"
    }
}