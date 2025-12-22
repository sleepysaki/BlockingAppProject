package vn.edu.usth.blockappserver.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp // Thử giữ dòng này xem hết đỏ không

object UsageLimits : Table("usage_limits") {
    val limitId = uuid("limit_id")
    val userId = uuid("user_id").nullable()
    val packageName = varchar("package_name", 255)
    val dailyLimitMinutes = integer("daily_limit_minutes").nullable()
    val isBlocked = bool("is_blocked").default(false)
    val startTime = time("start_time").nullable()
    val endTime = time("end_time").nullable()
    val dayOfWeek = integer("day_of_week").nullable()

    // CÁCH FIX: Nếu CurrentTimestamp() đỏ, hãy dùng đoạn code dưới đây:
    val updatedAt = timestamp("updated_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)
}

@Serializable
data class BlockRuleDTO(
    val packageName: String,
    val isBlocked: Boolean,
    val limitMinutes: Int?,
    val startTime: String? = null,
    val endTime: String? = null
)