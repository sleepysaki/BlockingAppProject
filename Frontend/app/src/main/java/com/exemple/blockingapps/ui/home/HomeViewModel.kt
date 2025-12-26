package com.exemple.blockingapps.ui.home

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.*
import com.exemple.blockingapps.data.network.RetrofitClient
import com.exemple.blockingapps.data.repo.AppRepository
import com.exemple.blockingapps.data.repository.UsageDataProvider
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.utils.BlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val repository = AppRepository(application.applicationContext)

    private val countdownJobs = mutableMapOf<String, Job>()
    private val appEndTimes = mutableMapOf<String, Instant>()

    private var instantLockJob: Job? = null
    private val INSTANT_LOCK_DURATION_SECONDS = 3600L

    init {
        startAllCountdowns()
    }


    fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)


            val userId = "36050457-f112-4762-a7f7-24daab6986ce"
            val deviceId = "DEV-001"

            try {

                Log.d("HomeViewModel", "--> Bắt đầu gửi danh sách App...")
                repository.syncAppsToServer(deviceId)


                Log.d("HomeViewModel", "--> Bắt đầu lấy luật chặn...")
                syncGroupRules(userId)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Lỗi Sync: ${e.message}")
            } finally {
                delay(1000)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun syncGroupRules(userId: String) {
        val context = getApplication<Application>().applicationContext
        withContext(Dispatchers.IO) {
            try {
                val responseGroups = RetrofitClient.apiService.getUserGroups(userId)
                if (responseGroups.isSuccessful) {
                    val groups = responseGroups.body() ?: emptyList()
                    val allRules = mutableListOf<GroupRuleDTO>()

                    groups.forEach { group ->
                        try {
                            val responseRules =
                                RetrofitClient.apiService.getGroupRules(group.groupId)
                            if (responseRules.isSuccessful) {
                                allRules.addAll(responseRules.body() ?: emptyList())
                            }
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Lỗi rules nhóm ${group.groupId}")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        BlockManager.updateRules(context, allRules)
                        updateBlockedAppsUI(allRules)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateBlockedAppsUI(rules: List<GroupRuleDTO>) {
        val blockedItems = rules.filter { it.isBlocked }.map { rule ->
            BlockedAppItem(
                appId = rule.packageName,
                packageName = rule.packageName,
                appName = rule.packageName.substringAfterLast(".")
                    .replaceFirstChar { it.uppercase() },
                category = "Server Blocked",
                dailyLimitMinutes = 0,
                remainingSeconds = 0L,
                scheduleFrom = rule.startTime,
                scheduleTo = rule.endTime
            )
        }
        _uiState.value = _uiState.value.copy(blockedApps = blockedItems)
        syncBlockState()
    }


    fun refreshDataFromDisk(context: Context) {
        val blockedFromDisk = FakeLocalDatabase.loadBlockedPackages(context)
        val currentRecs = _uiState.value.recommendations
        val filteredRecs = currentRecs.filter { it.appId !in blockedFromDisk }
        _uiState.value = _uiState.value.copy(recommendations = filteredRecs)
        syncBlockState(context)
    }

    fun loadPresetsFromDisk(context: Context) {
        val savedPresets = FakeLocalDatabase.loadTimePresets(context)
        _uiState.value = _uiState.value.copy(timePresets = savedPresets)
    }

    fun addTimePreset(context: Context, label: String, start: String, end: String) {
        val newPreset = TimePreset(
            id = UUID.randomUUID().toString(),
            label = label,
            startTime = start,
            endTime = end
        )
        val updatedList = _uiState.value.timePresets + newPreset
        _uiState.value = _uiState.value.copy(timePresets = updatedList)
        FakeLocalDatabase.saveTimePresets(context, updatedList)
    }

    fun deleteTimePreset(context: Context, presetId: String) {
        val currentList = _uiState.value.timePresets.toMutableList()
        currentList.removeAll { it.id == presetId }
        _uiState.value = _uiState.value.copy(timePresets = currentList)
        FakeLocalDatabase.saveTimePresets(context, currentList)
    }

    fun assignAppToPreset(context: Context, app: AppItem, preset: TimePreset) {
        val pkgName = app.packageName
        val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
        currentSet.add(pkgName)
        FakeLocalDatabase.saveBlockedPackages(context, currentSet)

        val prefs = context.getSharedPreferences("TimePresetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("assign_$pkgName", preset.id).apply()

        syncBlockState(context)


        _uiState.value = _uiState.value.copy(
            blockedApps = _uiState.value.blockedApps + BlockedAppItem(
                appId = pkgName,
                packageName = pkgName,
                appName = app.name,
                category = "Lịch trình: ${preset.label}",
                scheduleFrom = preset.startTime,
                scheduleTo = preset.endTime,
                dailyLimitMinutes = 0,
                remainingSeconds = 0L
            )
        )
    }

    fun removeBlockedApp(packageName: String, context: Context) {

        val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
        if (currentSet.contains(packageName)) {
            currentSet.remove(packageName)
            FakeLocalDatabase.saveBlockedPackages(context, currentSet)
        }


        val prefs = context.getSharedPreferences("TimePresetPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("assign_$packageName").apply()

        syncBlockState(context)


        _uiState.value = _uiState.value.copy(
            blockedApps = _uiState.value.blockedApps.filter { it.packageName != packageName }
        )
    }


    fun loadRealUsageAndGenerateRecs(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val autoRecs = UsageDataProvider.getRealDataAndAutoRec(context)
                _uiState.value = _uiState.value.copy(recommendations = autoRecs, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }


    private fun syncBlockState(context: Context? = null) {
        context?.let {
            val diskApps = FakeLocalDatabase.loadBlockedPackages(it)
            BlockState.blockedPackages = diskApps
            BlockState.isBlocking = diskApps.isNotEmpty()
        } ?: run {
            val currentPkgs = _uiState.value.blockedApps.map { it.appId }.toSet()
            BlockState.blockedPackages = currentPkgs
        }
    }

    fun selectDeviceForHistory(deviceId: String) {
        _uiState.value = _uiState.value.copy(selectedDeviceId = deviceId)
    }

    fun clearSelectedDevice() {
        _uiState.value = _uiState.value.copy(selectedDeviceId = null)
    }

    fun addDevice(deviceName: String, deviceId: String) {
        val cur = _uiState.value
        val newDevice = DeviceItem(
            deviceId = deviceId,
            deviceName = deviceName,
            lastActive = "vừa xong",
            isConnected = true
        )
        _uiState.value = cur.copy(devices = cur.devices + newDevice)
    }

    fun removeDevice(deviceId: String) {
        _uiState.value =
            _uiState.value.copy(devices = _uiState.value.devices.filterNot { it.deviceId == deviceId })
    }

    fun updateInstalledApps(apps: List<AppItem>) {
        _uiState.value = _uiState.value.copy(installedApps = apps)
    }

    fun refreshData() {
        syncData()
    }

    fun lockAllNow() {
        instantLockJob?.cancel()
        BlockState.setInstantLockTime(INSTANT_LOCK_DURATION_SECONDS)
        instantLockJob = viewModelScope.launch {
            var remaining = INSTANT_LOCK_DURATION_SECONDS
            while (remaining > 0) {
                delay(1000L)
                remaining--
                BlockState.setInstantLockTime(remaining)
            }
            BlockState.setInstantLockTime(0L)
        }
    }

    fun applyRecommendation(context: Context, rec: RecommendationItem) {
        val pkgName = rec.appId ?: return
        if (pkgName == "com.exemple.blockingapps") return

        val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
        currentSet.add(pkgName)
        FakeLocalDatabase.saveBlockedPackages(context, currentSet)


        val newBlockedApp = BlockedAppItem(
            appId = pkgName,
            packageName = pkgName,
            appName = rec.title.substringBefore("—").trim(),
            category = "Social",
            dailyLimitMinutes = rec.suggestedLimitMinutes ?: 15,
            remainingSeconds = 0L
        )

        _uiState.value = _uiState.value.copy(
            recommendations = _uiState.value.recommendations.filter { it.appId != pkgName },
            blockedApps = _uiState.value.blockedApps + newBlockedApp
        )

        syncBlockState(context)
    }

    fun loadWeeklyData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weeklyData = UsageDataProvider.getRealUsageForLast7Days(context)
                val currentHistory = _uiState.value.usageHistory.toMutableMap()
                currentHistory["DEV-002"] = weeklyData
                _uiState.value =
                    _uiState.value.copy(usageHistory = currentHistory, isLoading = false)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Lỗi đọc dữ liệu tuần")
            }
        }
    }

    private fun startAllCountdowns() {
        for (app in _uiState.value.blockedApps) {
            BlockState.remainingTimeSeconds[app.appId] = app.remainingSeconds
            if (app.remainingSeconds > 0) {
                appEndTimes[app.appId] = Instant.now().plusSeconds(app.remainingSeconds)
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
                BlockState.remainingTimeSeconds[appId] = remaining
                if (remaining <= 0L) break
                delay(1000L)
            }
        }
        countdownJobs[appId] = job
    }

    override fun onCleared() {
        super.onCleared()
        countdownJobs.values.forEach { it.cancel() }
    }

    private fun createInitialState(context: Context? = null): HomeUiState {


        val devices = listOf(
            DeviceItem("DEV-001", "Kid - Pixel 5", "1h trước", false),
            DeviceItem("DEV-002", "Kid - Galaxy A12", "Vừa xong", true)
        )
        return HomeUiState(
            username = "Parent A",
            role = "Parent",
            totalUsageMinutesToday = 0,
            devices = devices,
            blockedApps = emptyList(),
            recommendations = emptyList(),
            geozones = emptyList(),
            isLoading = false,
            error = null,
            usageHistory = emptyMap(),
            selectedDeviceId = null,
            timePresets = emptyList(),
            installedApps = emptyList()
        )
    }
}