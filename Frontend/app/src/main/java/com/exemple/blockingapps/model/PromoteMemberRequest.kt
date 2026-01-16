package com.exemple.blockingapps.model

data class PromoteMemberRequest(
    val groupId: String,
    val adminId: String,
    val targetUserId: String
)