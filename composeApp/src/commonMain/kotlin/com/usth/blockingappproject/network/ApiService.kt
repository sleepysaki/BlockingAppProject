import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.usth.blockingappproject.model.* // Import your models

class ApiService {
    // 10.0.2.2 is the localhost for Android Emulator. 
    // If testing on Web, use "http://localhost:8080"
    // Ideally, put this in a config file.
    private val BASE_URL = "http://10.0.2.2:8080" 

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response: LoginResponse = httpClient.post("$BASE_URL/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            Result.success(response)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun checkAppAccess(packageName: String, userId: Int): Boolean {
        return try {
            val response: CheckAccessResponse = httpClient.post("$BASE_URL/rules/check") {
                contentType(ContentType.Application.Json)
                setBody(CheckAccessRequest(packageName, userId))
            }.body()
            
            response.isAllowed
        } catch (e: Exception) {
            true // If API fails, default to allow (fail-safe) or block depending on policy
        }
    }
}