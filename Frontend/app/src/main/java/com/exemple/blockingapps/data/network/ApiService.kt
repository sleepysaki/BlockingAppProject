package com.exemple.blockingapps.data.network

import com.exemple.blockingapps.model.GroupDTO
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.SyncAppsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/devices/sync")
    suspend fun syncInstalledApps(@Body request: SyncAppsRequest): Response<Unit>

    @GET("api/groups/{groupId}/rules")
    suspend fun getGroupRules(@Path("groupId") groupId: String): Response<List<GroupRuleDTO>>

    @GET("api/users/{userId}/groups")
    suspend fun getUserGroups(@Path("userId") userId: String): Response<List<GroupDTO>>
}