package com.exemple.blockingapps.ui.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.data.model.DailyUsageSummary
import com.exemple.blockingapps.data.model.UsageCategory
import com.exemple.blockingapps.data.model.UsageRecord
import com.exemple.blockingapps.data.repository.UsageDataProvider
import com.exemple.blockingapps.model.network.RetrofitClient // 👈 Import Retrofit
import com.exemple.blockingapps.utils.BlockManager // 👈 Import BlockManager
import kotlinx.coroutines.Dispatchers // 👈 Import Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // 👈 Import withContext
import java.time.Duration
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val countdownJobs = mutableMapOf<String, Job>()
    private val appEndTimes = mutableMapOf<String, Instant>()

    private var instantLockJob: Job? = null
    private val INSTANT_LOCK_DURATION_SECONDS = 3600L

    init {
        // syncBlockState() -> Cái này là của FakeLocalDatabase, có thể giữ lại nếu muốn dùng song song
        startAllCountdowns()
    }

    // 👇👇👇 QUAN TRỌNG: THÊM HÀM NÀY ĐỂ ĐỒNG BỘ GROUP RULES TỪ SERVER 👇👇👇
    fun syncGroupRules(context: Context, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("HomeViewModel", "Starting syncGroupRules for user: $userId")

                // 1. Lấy danh sách nhóm của user
                val groups = RetrofitClient.api.getUserGroups(userId)

                val allRules = mutableListOf<com.exemple.blockingapps.model.GroupRuleDTO>()

                // 2. Lặp qua từng nhóm để lấy Rules
                groups.forEach { group ->
                    try {
                        val rules = RetrofitClient.api.getGroupRules(group.groupId)
                        allRules.addAll(rules)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Lưu tổng hợp tất cả rules vào bộ nhớ máy (BlockManager)
                withContext(Dispatchers.Main) {
                    BlockManager.updateRules(context, allRules)
                    Log.d("BlockManager", "Synced ${allRules.size} rules from Server. Blocked apps updated.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Sync failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    // 👆👆👆 HẾT PHẦN THÊM MỚI 👆👆👆

    private fun createInitialState(context: Context? = null): HomeUiState {
        // ... (Giữ nguyên code cũ của bạn) ...
        val devices = listOf(
            DeviceItem("DEV-001", "Kid - Pixel 5", "1h trước", false),
            DeviceItem("DEV-002", "Kid - Galaxy A12", "Vừa xong", true)
        )

        val blockedFromDisk = context?.let {
            FakeLocalDatabase.loadBlockedPackages(it)
        } ?: emptySet()

        val blockedApps = blockedFromDisk.map { pkg ->
            BlockedAppItem(
                appId = pkg,
                packageName = pkg,
                appName = pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                category = "Restricted",
                dailyLimitMinutes = 0,
                remainingSeconds = 0L
            )
        }

        val mockHourlyData = listOf(0, 0, 0, 0, 0, 0, 10, 15, 5, 0, 0, 0, 30, 45, 10, 0, 0, 0, 20, 15, 12, 0, 0, 0)
        val kid1HistorySummary = DailyUsageSummary(
            date = "Hôm nay, 16 tháng 12",
            totalScreenTimeMinutes = 162,
            comparisonPercent = "-42% so với tuần trước",
            categories = listOf(UsageCategory("Xã hội", 155), UsageCategory("Giải trí", 42), UsageCategory("Khác", 10)),
            topApps = listOf(
                UsageRecord("com.facebook", "Facebook", 155, 30, "Xã hội"),
                UsageRecord("com.tiktok", "TikTok", 87, 20, "Xã hội"),
                UsageRecord("com.youtube", "YouTube", 42, 10, "Giải trí"),
                UsageRecord("com.threads", "Threads", 16, 5, "Xã hội"),
                UsageRecord("com.vnet", "VNetTraffic", 10, 2, "Khác")
            ),
            hourlyUsage = mockHourlyData
        )

        val historyData = mapOf("DEV-001" to listOf(kid1HistorySummary), "DEV-002" to emptyList())

        val allRecommendations = listOf(
            RecommendationItem("Kid watched TikTok 200 mins today — suggest 60 min limit", "com.tiktok", 60),
            RecommendationItem("Kid watched YouTube 140 mins — suggest 90 min limit", "com.youtube", 90)
        )

        val filteredRecommendations = allRecommendations.filter { it.appId !in blockedFromDisk }

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
            recommendations = filteredRecommendations,
            geozones = geo,
            isLoading = false,
            error = null,
            usageHistory = historyData,
            selectedDeviceId = null
        )
    }

    // ... (Các hàm logic cũ giữ nguyên, không cần sửa gì thêm) ...

    fun selectDeviceForHistory(deviceId: String) {
        _uiState.value = _uiState.value.copy(selectedDeviceId = deviceId)
    }

    fun clearSelectedDevice() {
        _uiState.value = _uiState.value.copy(selectedDeviceId = null)
    }

    fun addDevice(deviceName: String, deviceId: String) {
        val cur = _uiState.value
        val newDevice = DeviceItem(deviceId = deviceId, deviceName = deviceName, lastActive = "vừa xong", isConnected = true)
        _uiState.value = cur.copy(devices = cur.devices + newDevice)
    }

    fun removeDevice(deviceId: String) {
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices.filterNot { it.deviceId == deviceId })
    }


    fun loadPresetsFromDisk(context: Context) {
        val savedPresets = FakeLocalDatabase.loadTimePresets(context)
        _uiState.value = _uiState.value.copy(timePresets = savedPresets)
    }

    fun addTimePreset(context: Context, label: String, start: String, end: String) {
        val newPreset = TimePreset(label = label, startTime = start, endTime = end)
        val updatedList = _uiState.value.timePresets + newPreset
        _uiState.value = _uiState.value.copy(timePresets = updatedList)
        FakeLocalDatabase.saveTimePresets(context, updatedList)
    }

    fun updateInstalledApps(apps: List<AppItem>) {
        _uiState.value = _uiState.value.copy(installedApps = apps)
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
                scheduleTo = preset.endTime
            )
        )
    }

    fun deleteTimePreset(context: Context, presetId: String) {
        val currentList = _uiState.value.timePresets.toMutableList()
        currentList.removeAll { it.id == presetId }
        _uiState.value = _uiState.value.copy(timePresets = currentList)
        FakeLocalDatabase.saveTimePresets(context, currentList)
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
            blockedApps = _uiState.value.blockedApps + BlockedAppItem(pkgName, pkgName, "App", "Category", 0, 0)
        )

        syncBlockState(context)
        BlockState.setInstantLockTime(INSTANT_LOCK_DURATION_SECONDS)
    }

    fun refreshDataFromDisk(context: Context) {
        val blockedFromDisk = FakeLocalDatabase.loadBlockedPackages(context)
        val currentRecs = _uiState.value.recommendations
        val filteredRecs = currentRecs.filter { it.appId !in blockedFromDisk }
        _uiState.value = _uiState.value.copy(recommendations = filteredRecs)
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

    fun addBlockedApp(item: BlockedAppItem) {
        _uiState.value = _uiState.value.copy(blockedApps = _uiState.value.blockedApps + item)
        syncBlockState()
        if (item.remainingSeconds > 0) {
            appEndTimes[item.appId] = Instant.now().plusSeconds(item.remainingSeconds)
            startCountdownFor(item.appId)
        }
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

    fun loadWeeklyData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weeklyData = UsageDataProvider.getRealUsageForLast7Days(context)
                val currentHistory = _uiState.value.usageHistory.toMutableMap()
                currentHistory["DEV-002"] = weeklyData
                _uiState.value = _uiState.value.copy(usageHistory = currentHistory, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Lỗi đọc dữ liệu tuần")
            }
        }
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

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(600)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJobs.values.forEach { it.cancel() }
    }
}