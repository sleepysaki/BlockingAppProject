package com.exemple.blockingapps.model.network

import BlockRuleDTO
import com.exemple.blockingapps.data.model.*
import com.exemple.blockingapps.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse


    @GET("rules")
    suspend fun getBlockRules(): List<BlockRuleDTO>


    @POST("api/devices/sync")
    suspend fun syncInstalledApps(@Body request: SyncAppsRequest): Response<Unit>


    @GET("api/users/{id}/groups")
    suspend fun getUserGroups(@Path("id") userId: String): Response<List<GroupDTO>>





    @POST("groups/create")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<CreateGroupResponse>

    @POST("groups/join")
    suspend fun joinGroup(
        @Query("user_id") userId: String,
        @Body inviteCode: String
    ): Response<Map<String, String>>


    @GET("api/groups/{id}/rules")
    suspend fun getGroupRules(@Path("id") groupId: String): Response<List<GroupRuleDTO>>

    @GET("groups/{id}/members")
    suspend fun getGroupMembers(@Path("id") groupId: String): Response<List<GroupMember>>

    @POST("groups/leave")
    suspend fun leaveGroup(@Body request: LeaveGroupRequest): Response<Map<String, String>>

    @POST("groups/remove")
    suspend fun removeMember(@Body request: RemoveMemberRequest): Response<Map<String, String>>

    @POST("groups/rules")
    suspend fun updateGroupRule(@Body rule: GroupRuleDTO): Response<Unit>

    @POST("rules")
    suspend fun addBlockRule(@Body rule: BlockRuleDTO): Response<Map<String, String>>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val api: ApiService get() = apiService
}