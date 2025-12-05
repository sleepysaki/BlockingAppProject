package com.exemple.blockingapps.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val countdownJobs = mutableMapOf<String, Job>()
    private val appEndTimes = mutableMapOf<String, Instant>()

    init {
        startAllCountdowns()
    }

    private fun createInitialState(): HomeUiState {
        val devices = listOf(
            DeviceItem(deviceId = "DEV-001", deviceName = "Kid - Pixel 5", lastActive = "1h ago"),
            DeviceItem(deviceId = "DEV-002", deviceName = "Kid - Galaxy A12", lastActive = "2d ago")
        )

        val blockedApps = listOf(
            BlockedAppItem(
                appId = "com.tiktok",
                appName = "TikTok",
                category = "Social",
                dailyLimitMinutes = 60,
                remainingSeconds = 25 * 60L,
                scheduleFrom = "19:00",
                scheduleTo = "21:00"
            ),
            BlockedAppItem(
                appId = "com.youtube",
                appName = "YouTube",
                category = "Video",
                dailyLimitMinutes = 120,
                remainingSeconds = 0L
            )
        )

        val recommendations = listOf(
            RecommendationItem("Kid watched TikTok 200 mins today — suggest 60 min limit", "com.tiktok", 60),
            RecommendationItem("Kid watched YouTube 140 mins — suggest 90 min limit", "com.youtube", 90)
        )

        val geo = listOf(
            GeoZoneItem(zoneId = "school", name = "School Zone", apps = listOf("com.tiktok", "com.minecraft")),
            GeoZoneItem(zoneId = "home", name = "Home", apps = listOf("com.youtube"))
        )

        return HomeUiState(
            username = "Parent A",
            role = "Parent",
            totalUsageMinutesToday = 320,
            devices = devices,
            blockedApps = blockedApps,
            recommendations = recommendations,
            geozones = geo,
            isLoading = false
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAllCountdowns() {
        val current = _uiState.value
        for (app in current.blockedApps) {
            if (app.remainingSeconds > 0) {
                val end = Instant.now().plusSeconds(app.remainingSeconds)
                appEndTimes[app.appId] = end
                startCountdownFor(app.appId)
            }
        }
    }

    private fun startCountdownFor(appId: String) {
        if (countdownJobs.containsKey(appId)) return

        val job = viewModelScope.launch {
            while (true) {
                val end = appEndTimes[appId] ?: break
                val now = Instant.now()
                val remaining = Duration.between(now, end).seconds.coerceAtLeast(0L)

                _uiState.value = _uiState.value.copy(
                    blockedApps = _uiState.value.blockedApps.map { b ->
                        if (b.appId == appId) b.copy(remainingSeconds = remaining) else b
                    }
                )
                if (remaining <= 0L) break
                delay(1000L)
            }
            countdownJobs.remove(appId)
        }
        countdownJobs[appId] = job
    }



    fun refreshData() {

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            delay(600)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun addDevice(deviceName: String, deviceId: String) {
        val cur = _uiState.value
        val newDevice = DeviceItem(deviceId = deviceId, deviceName = deviceName, lastActive = "just now")
        _uiState.value = cur.copy(devices = cur.devices + newDevice)
    }

    fun removeDevice(deviceId: String) {
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices.filterNot { it.deviceId == deviceId })
    }

    fun addBlockedApp(item: BlockedAppItem) {
        _uiState.value = _uiState.value.copy(blockedApps = _uiState.value.blockedApps + item)
        if (item.remainingSeconds > 0) {
            appEndTimes[item.appId] = Instant.now().plusSeconds(item.remainingSeconds)
            startCountdownFor(item.appId)
        }
    }

    fun removeBlockedApp(appId: String) {
        _uiState.value = _uiState.value.copy(blockedApps = _uiState.value.blockedApps.filterNot { it.appId == appId })
        // cancel job if exists
        countdownJobs[appId]?.cancel()
        countdownJobs.remove(appId)
        appEndTimes.remove(appId)
    }

    fun lockAllNow() {
        val updated = _uiState.value.blockedApps.map { it.copy(remainingSeconds = it.dailyLimitMinutes * 60L) }
        _uiState.value = _uiState.value.copy(blockedApps = updated)
        updated.forEach { if (it.remainingSeconds > 0) {
            appEndTimes[it.appId] = Instant.now().plusSeconds(it.remainingSeconds)
            startCountdownFor(it.appId)
        } }
    }

    fun applyRecommendation(rec: RecommendationItem) {
        rec.appId?.let { appId ->
            val found = _uiState.value.blockedApps.find { it.appId == appId }
            if (found != null && rec.suggestedLimitMinutes != null) {
                val updated = _uiState.value.blockedApps.map {
                    if (it.appId == appId) it.copy(dailyLimitMinutes = rec.suggestedLimitMinutes,
                        remainingSeconds = rec.suggestedLimitMinutes * 60L) else it
                }
                _uiState.value = _uiState.value.copy(blockedApps = updated)
                appEndTimes[appId] = Instant.now().plusSeconds(rec.suggestedLimitMinutes * 60L.toLong())
                startCountdownFor(appId)
            } else {
                val newApp = BlockedAppItem(
                    appId = rec.appId ?: "unknown",
                    appName = "Unknown",
                    category = "Unknown",
                    dailyLimitMinutes = rec.suggestedLimitMinutes ?: 0,
                    remainingSeconds = (rec.suggestedLimitMinutes ?: 0) * 60L
                )
                addBlockedApp(newApp)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJobs.values.forEach { it.cancel() }
        countdownJobs.clear()
        appEndTimes.clear()
    }
}
