package com.exemple.blockingapps.model

import com.google.gson.annotations.SerializedName

data class CreateGroupRequest(
    val name: String,
    val adminId: String
)

data class JoinGroupRequest(
    val joinCode: String,
    val userId: String
)

data class CreateGroupResponse(
    val status: String,
    val message: String,
    val groupId: String,
    val joinCode: String
)

data class JoinGroupResponse(
    val status: String,
    val message: String
)

data class GroupMember(
    val userId: String,
    val fullName: String,
    val email: String? = "",
    val role: String
)
data class UserGroup(
    val groupId: String,
    val groupName: String,
    val joinCode: String,
    val role: String
)
data class LeaveGroupRequest(
    val groupId: String,
    val userId: String
)

data class RemoveMemberRequest(
    val groupId: String,
    val adminId: String,
    val targetUserId: String
)

// 👇 SỬA LẠI CLASS NÀY
data class GroupRuleDTO(
    val id: String? = null,
    val groupId: String,
    val packageName: String,

    @SerializedName("isBlocked")
    val isBlocked: Boolean,

    val startTime: String? = null,
    val endTime: String? = null,

    // 👇 THÊM SERIALIZED NAME ĐỂ BẮT MỌI TRƯỜNG HỢP TÊN BIẾN TỪ SERVER
    @SerializedName(value = "latitude", alternate = ["lat", "Lat"])
    val latitude: Double? = null,

    @SerializedName(value = "longitude", alternate = ["long", "lng", "Long", "Lng"])
    val longitude: Double? = null,

    @SerializedName(value = "radius", alternate = ["rad"])
    val radius: Double? = null
)