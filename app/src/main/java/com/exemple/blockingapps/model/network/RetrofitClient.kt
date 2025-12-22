package com.exemple.blockingapps.model.network

import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.LoginResponse
import com.exemple.blockingapps.data.model.RegisterRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.exemple.blockingapps.model.BlockRule
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

// 1. Định nghĩa các hành động gọi API
interface ApiService {
    @GET("/rules") // Đường dẫn này nối đuôi vào BASE_URL -> http://ip:8080/rules
    suspend fun getBlockRules(): List<BlockRule>

    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/auth/register")
    suspend fun register(@Body request: RegisterRequest): ResponseBody


}

// 2. Tạo cục kết nối (Singleton)
object RetrofitClient {


    private const val BASE_URL = "http://10.0.2.2:8080/"
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}