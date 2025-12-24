package com.exemple.blockingapps.model

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
data class GroupRuleDTO(
    val groupId: String,
    val packageName: String,
    val isBlocked: Boolean
)