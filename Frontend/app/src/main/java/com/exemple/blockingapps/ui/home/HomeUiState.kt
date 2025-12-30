package com.exemple.blockingapps.ui.home

import android.icu.text.CaseMap
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.data.model.DailyUsageSummary
import com.exemple.blockingapps.model.GroupDTO

data class HomeUiState(
    val username: String = "Parent User",
    val role: String = "Parent",
    val totalUsageMinutesToday: Int = 0,
    val devices: List<DeviceItem> = emptyList(),
    val blockedApps: List<BlockedAppItem> = emptyList(),
    val recommendations: List<RecommendationItem> = emptyList(),
    val geozones: List<GeoZoneItem> = emptyList(),
    val timePresets: List<TimePreset> = emptyList(),
    val installedApps: List<AppItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val usageHistory: Map<String, List<DailyUsageSummary>> = emptyMap(),
    val selectedDeviceId: String? = null,
    val groups: List<GroupDTO> = emptyList()
    )

data class DeviceItem(
    val deviceId: String,
    val deviceName: String,
    val lastActive: String = "",
    val isConnected: Boolean = false
)

data class TimePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val startTime: String,
    val endTime: String
)

data class BlockedAppItem(
    val appId: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val dailyLimitMinutes: Int = 0,
    val remainingSeconds: Long = 0L,
    val scheduleFrom: String? = null,
    val scheduleTo: String? = null
)

data class RecommendationItem(
    val title: String,
    val appId: String?,
    val suggestedLimitMinutes: Int?
)

data class GeoZoneItem(
    val zoneId: String,
    val name: String,
    val apps: List<String>
)
