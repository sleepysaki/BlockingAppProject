package com.exemple.blockingapps.model

data class SyncAppsRequest(
    val deviceId: String,
    val installedApps: List<AppInfoDTO>
)

data class AppInfoDTO(
    val packageName: String,
    val appName: String
)

data class GroupDTO(
    val groupId: String,
    val groupName: String,
    val role: String? = "MEMBER",
    val inviteCode: String? = null,
    val joinCode: String
)