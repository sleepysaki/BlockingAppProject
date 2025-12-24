package vn.edu.usth.blockappserver.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp

// TABLE USERS
object Users : Table("users") {
    val userId = uuid("user_id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val role = varchar("role", 50)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(userId)
}

// TABLE GROUPS (Workspace)
object Groups : Table("groups") {
    val groupId = uuid("group_id").autoGenerate()
    val parentGroupId = uuid("parent_group_id").references(groupId).nullable()
    val name = varchar("name", 255)
    val type = varchar("type", 50).default("FAMILY")
    val createdBy = uuid("created_by").references(Users.userId)

    // Added for joining functionality
    val joinCode = varchar("join_code", 10).uniqueIndex()

    override val primaryKey = PrimaryKey(groupId)
}

//  TABLE GROUP MEMBERS (Link Users to Groups)
object GroupMembers : Table("group_members") {
    val groupId = uuid("group_id").references(Groups.groupId)
    val userId = uuid("user_id").references(Users.userId)
    val role = varchar("role", 50).default("MEMBER")
    val isGroupAdmin = bool("is_group_admin").default(false)
    override val primaryKey = PrimaryKey(groupId, userId)
}

// TABLE USAGE LIMITS (Blocking Rules)
object UsageLimits : Table("usage_limits") {
    val limitId = uuid("limit_id").autoGenerate()
    val userId = uuid("user_id").references(Users.userId).nullable()

    // Assuming simple varchar for package name
    val packageName = varchar("package_name", 255)

    val dailyLimitMinutes = integer("daily_limit_minutes").nullable()
    val isBlocked = bool("is_blocked").default(false)
    val startTime = time("start_time").nullable()
    val endTime = time("end_time").nullable()
    val dayOfWeek = integer("day_of_week").nullable()

    // Geo-blocking fields
    val latitude = double("latitude").default(0.0)
    val longitude = double("longitude").default(0.0)
    val radius = double("radius").default(100.0)

    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(limitId)
}
// GROUP RULES
object GroupRules : Table("group_rules") {
    val ruleId = uuid("rule_id").autoGenerate()
    val groupId = uuid("group_id").references(Groups.groupId)
    val packageName = varchar("package_name", 255)
    val isBlocked = bool("is_blocked").default(true)

    init {
        uniqueIndex(groupId, packageName)
    }

    override val primaryKey = PrimaryKey(ruleId)
}

//  DATA TRANSFER OBJECTS (DTO)

@Serializable
data class UserDTO(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDTO
)

@Serializable
data class BlockRuleDTO(
    val packageName: String,
    val isBlocked: Boolean,
    val limitMinutes: Int?,
    val startTime: String? = null,
    val endTime: String? = null,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val radius: Double? = 100.0
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "PARENT"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

// NEW GROUP DTOs

@Serializable
data class CreateGroupRequest(
    val name: String,
    val adminId: String // UUID as String
)

@Serializable
data class JoinGroupRequest(
    val joinCode: String,
    val userId: String // UUID as String
)

@Serializable
data class GroupMemberDTO(
    val userId: String,
    val fullName: String,
    val isAdmin: Boolean
)
@Serializable // Nếu dùng Kotlinx.Serialization
data class GroupMemberResponse(
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String // 👈 THÊM DÒNG NÀY VÀO
)
@Serializable
data class CreateGroupResponse(
    val status: String,
    val message: String,
    val groupId: String,
    val joinCode: String
)
@Serializable
data class LeaveGroupRequest(
    val groupId: String,
    val userId: String
)

@Serializable
data class RemoveMemberRequest(
    val groupId: String,
    val adminId: String,
    val targetUserId: String
)
@Serializable
data class GroupRuleDTO(
    val groupId: String,
    val packageName: String,
    val isBlocked: Boolean
)