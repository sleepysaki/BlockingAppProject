import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: Int,
    val role: String 
)

@Serializable
data class CheckAccessRequest(
    val packageName: String,
    val userId: Int
)

@Serializable
data class CheckAccessResponse(
    val isAllowed: Boolean,
    val reason: String? = null
)