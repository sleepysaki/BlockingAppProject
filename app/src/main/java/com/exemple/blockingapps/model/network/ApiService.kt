package com.exemple.blockingapps.model.network

import BlockRuleDTO
import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.LoginResponse
import com.exemple.blockingapps.data.model.RegisterRequest
import com.exemple.blockingapps.data.model.RegisterResponse
import com.exemple.blockingapps.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ApiService {

    // --- AUTHENTICATION ---

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // --- BLOCKING RULES ---

    @GET("rules")
    suspend fun getBlockRules(): List<BlockRuleDTO>

    @POST("rules")
    suspend fun addBlockRule(@Body rule: BlockRuleDTO): Map<String, String>

    // --- GROUP MANAGEMENT ---

    @POST("groups/create")
    suspend fun createGroup(@Body request: CreateGroupRequest): CreateGroupResponse

    @POST("groups/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): Map<String, String>

    @GET("groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") groupId: String): List<GroupMember>

    @GET("users/{id}/groups")
    suspend fun getUserGroups(@Path("id") userId: String): List<UserGroup>

    @POST("groups/leave")
    suspend fun leaveGroup(@Body request: LeaveGroupRequest): Map<String, String>

    @POST("groups/remove")
    suspend fun removeMember(@Body request: RemoveMemberRequest): Map<String, String>

    @GET("groups/{id}/rules")
    suspend fun getGroupRules(@Path("id") groupId: String): List<GroupRuleDTO>

    @POST("groups/rules")
    suspend fun updateGroupRule(@Body rule: GroupRuleDTO): Map<String, String>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // 👇 CẤU HÌNH LOGGING INTERCEPTOR
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 👈 Thêm dòng này
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // 👈 Nhớ set client vào đây
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}