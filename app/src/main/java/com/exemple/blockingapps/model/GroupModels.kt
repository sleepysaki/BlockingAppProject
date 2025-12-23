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
    val isAdmin: Boolean
)