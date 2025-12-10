package com.usth.blockingappproject.server.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.postgresql.util.PGobject
import java.util.UUID

/**
 * Database table definitions using Exposed ORM.
 * 
 * Schema Overview:
 * - Identity: Users, Groups, GroupMembers, Devices
 * - Face Recognition: FaceEmbeddings (pgvector)
 * - Policy: AppCatalog, UsageLimits, GeoZones, ZoneRules
 * - Telemetry: AppUsageLogs
 * 
 * Requires PostgreSQL with pgvector extension for face embedding storage.
 */

// =============================================================================
// PGVECTOR SUPPORT
// =============================================================================

/**
 * Custom column type for PostgreSQL pgvector extension.
 * Stores face recognition embeddings as vectors for similarity search.
 * 
 * @param dimension Vector dimension (128 for ML Kit face embeddings)
 */
class VectorColumnType(private val dimension: Int) : ColumnType() {
    override fun sqlType(): String = "vector($dimension)"

    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is PGobject -> {
                // Parse "[0.1, 0.2, ...]" string to List<Float>
                value.value?.trim('[', ']')?.split(",")?.map { it.trim().toFloat() } 
                    ?: emptyList<Float>()
            }
            else -> value
        }
    }

    override fun notNullValueToDB(value: Any): Any {
        return when (value) {
            is List<*> -> {
                // Convert List<Float> to Postgres vector syntax "[0.1,0.2]"
                PGobject().apply {
                    type = "vector"
                    this.value = value.joinToString(",", "[", "]")
                }
            }
            else -> super.notNullValueToDB(value)
        }
    }
}

/** Extension to easily add vector columns to tables */
fun Table.vector(name: String, dimension: Int) = 
    registerColumn<List<Float>>(name, VectorColumnType(dimension))


// Identity

object Users : UUIDTable("users", "user_id") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 100)
    val role = varchar("role", 50) // Maps to UserRole enum
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object Groups : UUIDTable("groups", "group_id") {
    val parentGroupId = reference("parent_group_id", Groups).nullable()
    val name = varchar("name", 100)
    val type = varchar("type", 50)
    val createdBy = reference("created_by", Users)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
}

object GroupMembers : Table("group_members") {
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val isGroupAdmin = bool("is_group_admin").default(false)
    val joinedAt = timestamp("joined_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(groupId, userId)
}

object Devices : Table("devices") {
    val deviceId = varchar("device_id", 100)  // Hardware/installation ID
    val userId = reference("user_id", Users).nullable()
    val deviceName = varchar("device_name", 100).nullable()
    val fcmToken = text("fcm_token").nullable()  // Firebase Cloud Messaging
    val isLocked = bool("is_locked").default(false)
    val lastSyncedAt = timestamp("last_synced_at").nullable()
    
    override val primaryKey = PrimaryKey(deviceId)
}


// Face Recognition

object FaceEmbeddings : Table("face_embeddings") {
    val id = integer("embedding_id").autoIncrement()
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val vectorData = vector("vector_data", 128)  // ML Kit produces 128-dim vectors
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    
    override val primaryKey = PrimaryKey(id)
}


// Policy

object AppCatalog : Table("app_catalog") {
    val packageName = varchar("package_name", 255)
    val appName = varchar("app_name", 255).nullable()
    val category = varchar("category", 100).nullable()  // GAME, SOCIAL, EDUCATION, etc.
    
    override val primaryKey = PrimaryKey(packageName)
}

/**
 * Usage limits - per-user, per-app restrictions.
 * 
 * Defines daily time limits, blocked times, and blocked apps.
 */
object UsageLimits : UUIDTable("usage_limits", "limit_id") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val packageName = reference("package_name", AppCatalog.packageName)
    
    val dailyLimitMinutes = integer("daily_limit_minutes").nullable()
    val isBlocked = bool("is_blocked").default(false)
    
    // Schedule-based blocking
    val startTime = time("start_time").nullable()
    val endTime = time("end_time").nullable()
    val dayOfWeek = integer("day_of_week").nullable()  // 1=Mon, 7=Sun
    
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
}

/**
 * Geographic zones for location-based restrictions.
 */
object GeoZones : UUIDTable("geo_zones", "zone_id") {
    val groupId = reference("group_id", Groups).nullable()
    val name = varchar("name", 100)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val radiusMeters = integer("radius_meters").default(100)
}

/**
 * Zone rules - what apps to block/allow in each zone.
 */
object ZoneRules : Table("zone_rules") {
    val id = integer("rule_id").autoIncrement()
    val zoneId = reference("zone_id", GeoZones, onDelete = ReferenceOption.CASCADE)
    val packageName = reference("package_name", AppCatalog.packageName)
    val policyType = varchar("policy_type", 20)  // BLOCK, ALLOW_ONLY
    
    override val primaryKey = PrimaryKey(id)
}


// =============================================================================
// TELEMETRY
// =============================================================================

/**
 * App usage logs - records of app sessions.
 * 
 * Used for daily reports and limit tracking.
 * Duration can be computed: sessionEnd - sessionStart
 */
object AppUsageLogs : Table("app_usage_logs") {
    val id = long("log_id").autoIncrement()
    val userId = reference("user_id", Users)
    val deviceId = reference("device_id", Devices.deviceId)
    val packageName = varchar("package_name", 255)
    val sessionStart = timestamp("session_start")
    val sessionEnd = timestamp("session_end")
    
    override val primaryKey = PrimaryKey(id)
}