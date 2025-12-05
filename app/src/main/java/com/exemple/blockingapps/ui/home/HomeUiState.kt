package com.exemple.blockingapps.ui.home

data class HomeUiState(
    val username: String = "Parent User",
    val role: String = "Parent",
    val totalUsageMinutesToday: Int = 0,
    val devices: List<DeviceItem> = emptyList(),
    val blockedApps: List<BlockedAppItem> = emptyList(),
    val recommendations: List<RecommendationItem> = emptyList(),
    val geozones: List<GeoZoneItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DeviceItem(
    val deviceId: String,
    val deviceName: String,
    val lastActive: String = ""
)

data class BlockedAppItem(
    val appId: String,
    val appName: String,
    val category: String,
    val dailyLimitMinutes: Int = 0,
    val remainingSeconds: Long = 0L,
    val scheduleFrom: String? = null,
    val scheduleTo: String? = null
)

data class RecommendationItem(
    val text: String,
    val appId: String?,
    val suggestedLimitMinutes: Int?
)

data class GeoZoneItem(
    val zoneId: String,
    val name: String,
    val apps: List<String>
)
