package com.exemple.blockingapps.data.model

enum class RequestStatus {
    PENDING,
    APPROVED,
    DECLINED
}

data class ExtraTimeRequest(
    val requestId: String,
    val appPackage: String,
    val appName: String,
    val requestedMinutes: Int,
    val reason: String,
    val childName: String,
    val status: RequestStatus
)
