package com.exemple.blockingapps.ui.home

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.*
import com.exemple.blockingapps.data.network.RetrofitClient
import com.exemple.blockingapps.data.repo.AppRepository
import com.exemple.blockingapps.data.repository.UsageDataProvider
import com.exemple.blockingapps.model.CreateGroupRequest
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.RemoveMemberRequest
import com.exemple.blockingapps.utils.BlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState

    // 👇 STATE MỚI: Quản lý danh sách thành viên
    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers.asStateFlow()

    private val repository = AppRepository(application.applicationContext)
    private val countdownJobs = mutableMapOf<String, Job>()
    private val appEndTimes = mutableMapOf<String, Instant>()
    private var instantLockJob: Job? = null
    private val INSTANT_LOCK_DURATION_SECONDS = 3600L

    // ID Mặc định (Có thể thay thế bằng logic lấy từ UserPreferences thực tế)
    private val currentUserId = "36050457-f112-4762-a7f7-24daab6986ce"
    private val currentDeviceId = "DEV-001"

    init {
        startAllCountdowns()
        fetchUserGroups(currentUserId)
    }

    // --- GROUP LOGIC ---

    fun fetchUserGroups(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getUserGroups(userId)
                if (response.isSuccessful) {
                    val groupsList = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(groups = groupsList, isLoading = false)
                        Log.d("GROUP_UI", "SUCCESS: State updated with ${groupsList.size} groups")
                    }
                } else {
                    withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = false) }
            }
        }
    }

    // 👇 CẬP NHẬT: Thêm Context và UserId để hiển thị Toast và đúng logic UI
    fun createGroup(context: Context, groupName: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = CreateGroupRequest(
                    name = groupName,
                    adminId = userId
                )
                val response = RetrofitClient.apiService.createGroup(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Group Created!", Toast.LENGTH_SHORT).show()
                        fetchUserGroups(userId) // Reload list
                    } else {
                        Toast.makeText(context, "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 👇 MỚI: Lấy danh sách thành viên của Group
    fun fetchGroupMembers(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getGroupMembers(groupId)
                if (response.isSuccessful) {
                    val members = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        _groupMembers.value = members
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 👇 MỚI: Thăng chức thành viên (Promote to Admin)
    fun promoteMember(context: Context, groupId: String, adminId: String, targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // SỬA Ở ĐÂY: Dùng đúng PromoteMemberRequest
                val req = com.exemple.blockingapps.model.PromoteMemberRequest(groupId, adminId, targetUserId)

                // Bây giờ gọi hàm này sẽ không bị lỗi Type Mismatch nữa
                val res = RetrofitClient.apiService.promoteMember(req)

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && res.body()?.get("status") == "success") {
                        Toast.makeText(context, "Promoted to Admin!", Toast.LENGTH_SHORT).show()
                        fetchGroupMembers(groupId)
                    } else {
                        Toast.makeText(context, "Failed to promote", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 👇 MỚI: Xóa thành viên (Kick Member)
    fun removeMember(context: Context, groupId: String, adminId: String, targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = RemoveMemberRequest(groupId, adminId, targetUserId)
                val res = RetrofitClient.apiService.removeMember(req)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && res.body()?.get("status") == "success") {
                        Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                        fetchGroupMembers(groupId) // Reload danh sách thành viên
                    } else {
                        Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- SYNC & LOGIC CŨ ---

    fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.syncAppsToServer(currentDeviceId)
                fetchUserGroups(currentUserId)
                syncGroupRules(currentUserId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Sync Error: ${e.message}")
            } finally {
                delay(800)
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
                        val responseRules = RetrofitClient.apiService.getGroupRules(group.groupId)
                        if (responseRules.isSuccessful) {
                            val rules = responseRules.body() ?: emptyList()

                            // 👇 ĐOẠN CODE DEBUG MỚI THÊM VÀO ĐÂY
                            // Giúp kiểm tra xem Server trả về tọa độ hay là Null
                            rules.forEach { r ->
                                if (r.latitude != null && (r.radius ?: 0.0) > 0.0) {
                                    Log.d("DEBUG_SYNC", "✅ SERVER CÓ DATA: ${r.packageName} -> Lat: ${r.latitude}, Long: ${r.longitude}, R: ${r.radius}")
                                } else {
                                    Log.d("DEBUG_SYNC", "⚠️ SERVER TRẢ VỀ NULL HOẶC RỖNG: ${r.packageName} -> Lat: ${r.latitude}, R: ${r.radius}")
                                }
                            }
                            // 👆 HẾT ĐOẠN DEBUG

                            allRules.addAll(rules)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        // Cập nhật vào BlockManager (Lúc này nếu allRules có tọa độ thì Geo Block sẽ hoạt động)
                        BlockManager.updateRules(context, allRules)
                        updateBlockedAppsUI(allRules)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "SyncRules Error: ${e.message}")
            }
        }
    }

    private fun updateBlockedAppsUI(rules: List<GroupRuleDTO>) {
        val blockedItems = rules.filter { it.isBlocked }.map { rule ->
            BlockedAppItem(
                appId = rule.packageName,
                packageName = rule.packageName,
                appName = rule.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                category = "Server Blocked",
                scheduleFrom = rule.startTime,
                scheduleTo = rule.endTime
            )
        }
        _uiState.value = _uiState.value.copy(blockedApps = blockedItems)
        syncBlockState()
    }

    fun refreshDataFromDisk(context: Context) {
        val prefs = context.getSharedPreferences("blocked_apps_pref", Context.MODE_PRIVATE)
        val savedRules = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        val loadedItems = savedRules.map { entry ->
            val parts = entry.split("|")
            BlockedAppItem(
                appId = parts[0],
                packageName = parts[0],
                appName = parts[0].substringAfterLast(".").replaceFirstChar { it.uppercase() },
                category = "Active Schedule",
                scheduleFrom = if (parts.size > 1) parts[1] else null,
                scheduleTo = if (parts.size > 2) parts[2] else null
            )
        }
        _uiState.value = _uiState.value.copy(blockedApps = loadedItems)
        com.exemple.blockingapps.data.common.BlockState.blockedPackages = loadedItems.map { it.packageName }.toSet()
    }

    fun loadPresetsFromDisk(context: Context) {
        val savedPresets = FakeLocalDatabase.loadTimePresets(context)
        _uiState.value = _uiState.value.copy(timePresets = savedPresets)
    }

    fun addTimePreset(context: Context, label: String, start: String, end: String) {
        val newPreset = TimePreset(id = UUID.randomUUID().toString(), label = label, startTime = start, endTime = end)
        val updatedList = _uiState.value.timePresets + newPreset
        _uiState.value = _uiState.value.copy(timePresets = updatedList)
        FakeLocalDatabase.saveTimePresets(context, updatedList)
    }

    fun deleteTimePreset(context: Context, presetId: String) {
        val currentList = _uiState.value.timePresets.filterNot { it.id == presetId }
        _uiState.value = _uiState.value.copy(timePresets = currentList)
        FakeLocalDatabase.saveTimePresets(context, currentList)
    }

    fun removeBlockedApp(packageName: String, context: Context) {
        val updatedList = _uiState.value.blockedApps.filterNot { it.packageName == packageName }
        _uiState.value = _uiState.value.copy(blockedApps = updatedList)

        val rulesToSave = updatedList.map { item ->
            com.exemple.blockingapps.model.BlockRule(
                packageName = item.packageName,
                isBlocked = true,
                startTime = item.scheduleFrom,
                endTime = item.scheduleTo,
                limitMinutes = 0
            )
        }
        BlockManager.saveRulesFromUI(context, rulesToSave)
        syncBlockState()
    }

    fun addDevice(deviceName: String, deviceId: String) {
        val newDevice = DeviceItem(deviceId = deviceId, deviceName = deviceName, lastActive = "now", isConnected = true)
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices + newDevice)
    }

    fun removeDevice(deviceId: String) {
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices.filterNot { it.deviceId == deviceId })
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

    fun loadWeeklyData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val weeklyData = UsageDataProvider.getRealUsageForLast7Days(context)
                val currentHistory = _uiState.value.usageHistory.toMutableMap()
                currentHistory["DEV-002"] = weeklyData
                _uiState.value = _uiState.value.copy(usageHistory = currentHistory, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun applyRecommendation(context: Context, rec: RecommendationItem) {
        val pkgName = rec.appId ?: return
        val newBlockedApp = BlockedAppItem(
            appId = pkgName, packageName = pkgName,
            appName = rec.title.substringBefore("—").trim(),
            category = "Social", dailyLimitMinutes = rec.suggestedLimitMinutes ?: 15
        )
        _uiState.value = _uiState.value.copy(
            recommendations = _uiState.value.recommendations.filterNot { it.appId == pkgName },
            blockedApps = _uiState.value.blockedApps + newBlockedApp
        )
        syncBlockState()
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

    fun assignAppToPreset(context: Context, app: AppItem, preset: TimePreset) {
        val newItem = BlockedAppItem(
            appId = app.packageName,
            packageName = app.packageName,
            appName = app.name,
            category = "Preset: ${preset.label}",
            scheduleFrom = preset.startTime,
            scheduleTo = preset.endTime
        )
        val currentList = _uiState.value.blockedApps.filterNot { it.packageName == app.packageName }.toMutableList()
        currentList.add(newItem)
        _uiState.value = _uiState.value.copy(blockedApps = currentList)

        val rulesToSave = currentList.map { item ->
            com.exemple.blockingapps.model.BlockRule(
                packageName = item.packageName,
                isBlocked = true,
                startTime = item.scheduleFrom,
                endTime = item.scheduleTo,
                limitMinutes = 0
            )
        }
        BlockManager.saveRulesFromUI(context, rulesToSave)
        syncBlockState()
    }

    private fun syncBlockState(context: Context? = null) {
        val currentPkgs = _uiState.value.blockedApps.map { it.packageName }.toSet()
        BlockState.blockedPackages = currentPkgs
        BlockState.isBlocking = currentPkgs.isNotEmpty()
    }

    private fun startAllCountdowns() {
        _uiState.value.blockedApps.forEach { app ->
            if (app.remainingSeconds > 0) {
                appEndTimes[app.appId] = Instant.now().plusSeconds(app.remainingSeconds)
                startCountdownFor(app.appId)
            }
        }
    }

    private fun startCountdownFor(appId: String) {
        if (countdownJobs.containsKey(appId)) return
        countdownJobs[appId] = viewModelScope.launch {
            while (true) {
                val end = appEndTimes[appId] ?: break
                val remaining = Duration.between(Instant.now(), end).seconds.coerceAtLeast(0L)
                _uiState.value = _uiState.value.copy(
                    blockedApps = _uiState.value.blockedApps.map { if (it.appId == appId) it.copy(remainingSeconds = remaining) else it }
                )
                if (remaining <= 0L) break
                delay(1000L)
            }
        }
    }

    private fun createInitialState() = HomeUiState(
        devices = listOf(DeviceItem("DEV-001", "Kid - Pixel 5", "1h ago", false), DeviceItem("DEV-002", "Kid - Galaxy A12", "now", true)),
        groups = emptyList(), isLoading = false
    )

    fun selectDeviceForHistory(deviceId: String) { _uiState.value = _uiState.value.copy(selectedDeviceId = deviceId) }
    fun clearSelectedDevice() { _uiState.value = _uiState.value.copy(selectedDeviceId = null) }
    fun updateInstalledApps(apps: List<AppItem>) { _uiState.value = _uiState.value.copy(installedApps = apps) }
    override fun onCleared() { super.onCleared(); countdownJobs.values.forEach { it.cancel() } }
}