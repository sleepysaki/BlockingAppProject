import kotlinx.serialization.Serializable

@Serializable
data class BlockRuleDTO(
    val packageName: String,
    val isBlocked: Boolean,
    val limitMinutes: Int? = 0,
    val startTime: String? = null,
    val endTime: String? = null,
    // Match these with Backend
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 100.0
)