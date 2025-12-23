package com.exemple.blockingapps.model.network

import BlockRuleDTO
import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.LoginResponse
import com.exemple.blockingapps.data.model.RegisterRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.exemple.blockingapps.model.BlockRule
import com.exemple.blockingapps.model.CreateGroupRequest
import com.exemple.blockingapps.model.CreateGroupResponse
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.model.JoinGroupRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// 1. Định nghĩa các hành động gọi API
interface ApiService {
    @GET("/rules") // Đường dẫn này nối đuôi vào BASE_URL -> http://ip:8080/rules
    suspend fun getBlockRules(): List<BlockRule>

    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/auth/register")
    suspend fun register(@Body request: RegisterRequest): ResponseBody
    @POST("rules")
    suspend fun addBlockRule(@Body rule: BlockRuleDTO): Map<String, String>

    @POST("groups/create")
    suspend fun createGroup(@Body request: CreateGroupRequest): CreateGroupResponse

    @POST("groups/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): Map<String, String>

    @GET("groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") groupId: String): List<GroupMember>
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