package com.exemple.blockingapps.ui.home

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.data.repository.UsageDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var timeTickerJob: Job? = null
    private var instantLockJob: Job? = null
    private val INSTANT_LOCK_DURATION_SECONDS = 3600L

    private fun createInitialState(): HomeUiState {
        return HomeUiState(
            username = "Parent A",
            role = "Parent",
            devices = listOf(
                DeviceItem("DEV-001", "Kid - Pixel 5", "1h trước", false),
                DeviceItem("DEV-002", "Kid - Galaxy A12", "Vừa xong", true)
            ),
            isLoading = false
        )
    }

    // --- 1. CORE LOGIC & REFRESH ---
    fun refreshAllData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val blockedIdList = FakeLocalDatabase.loadBlockedPackages(context)
                val limitsPrefs = context.getSharedPreferences("FocusGuardLimits", Context.MODE_PRIVATE)
                val history = UsageDataProvider.getRealUsageForLast7Days(context)
                val todaySummary = history.firstOrNull()

                val updatedApps = blockedIdList.map { id ->
                    val limitMinutes = limitsPrefs.getInt("limit_$id", 0)
                    val usageRecord = todaySummary?.topApps?.find { it.appId == id }
                    val usedMinutes = usageRecord?.totalMinutes ?: 0
                    val remainingSecs = ((limitMinutes - usedMinutes).coerceAtLeast(0) * 60).toLong()

                    BlockedAppItem(
                        appId = id, packageName = id,
                        appName = usageRecord?.appName ?: id.substringAfterLast("."),
                        category = usageRecord?.category ?: "App",
                        dailyLimitMinutes = limitMinutes,
                        remainingSeconds = remainingSecs,
                        scheduleFrom = limitsPrefs.getString("sched_from_$id", null),
                        scheduleTo = limitsPrefs.getString("sched_to_$id", null)
                    )
                }

                withContext(Dispatchers.Main) {
                    val currentHistory = _uiState.value.usageHistory.toMutableMap()
                    currentHistory["DEV-002"] = history
                    _uiState.value = _uiState.value.copy(
                        blockedApps = updatedApps,
                        usageHistory = currentHistory,
                        isLoading = false
                    )

                    // CHỈ CHẶN KHI LIMIT > 0 VÀ HẾT GIỜ
                    val appsToBlockNow = updatedApps.filter {
                        it.dailyLimitMinutes > 0 && it.remainingSeconds <= 0
                    }.map { it.appId }.toSet()
                    BlockState.blockedPackages = appsToBlockNow
                    updatedApps.forEach { BlockState.remainingTimeSeconds[it.appId] = it.remainingSeconds }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = false) }
            }
        }
    }

    fun refreshDataFromDisk(context: Context) = refreshAllData(context)

    // --- 2. PRESETS (Đã thêm lại deleteTimePreset) ---
    fun loadPresetsFromDisk(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val presets = FakeLocalDatabase.loadTimePresets(context)
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(timePresets = presets) }
        }
    }

    fun addTimePreset(context: Context, label: String, start: String, end: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPresets = FakeLocalDatabase.loadTimePresets(context).toMutableList()
            currentPresets.add(TimePreset(label = label, startTime = start, endTime = end))
            FakeLocalDatabase.saveTimePresets(context, currentPresets)
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(timePresets = currentPresets) }
        }
    }

    fun deleteTimePreset(context: Context, presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPresets = FakeLocalDatabase.loadTimePresets(context).toMutableList()
            currentPresets.removeAll { it.id == presetId }
            FakeLocalDatabase.saveTimePresets(context, currentPresets)
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(timePresets = currentPresets) }
        }
    }

    fun assignAppToPreset(context: Context, app: AppItem, preset: TimePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
            currentSet.add(app.packageName)
            FakeLocalDatabase.saveBlockedPackages(context, currentSet)
            context.getSharedPreferences("FocusGuardLimits", Context.MODE_PRIVATE).edit()
                .putString("sched_from_${app.packageName}", preset.startTime)
                .putString("sched_to_${app.packageName}", preset.endTime).apply()
            refreshAllData(context)
        }
    }

    // --- 3. DEVICES & HISTORY ---
    fun selectDeviceForHistory(deviceId: String) { _uiState.value = _uiState.value.copy(selectedDeviceId = deviceId) }
    fun clearSelectedDevice() { _uiState.value = _uiState.value.copy(selectedDeviceId = null) }
    fun addDevice(deviceName: String, deviceId: String) {
        val newDevice = DeviceItem(deviceId, deviceName, "Vừa xong", true)
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices + newDevice)
    }
    fun removeDevice(deviceId: String) {
        _uiState.value = _uiState.value.copy(devices = _uiState.value.devices.filterNot { it.deviceId == deviceId })
    }
    fun loadWeeklyData(context: Context) = refreshAllData(context)

    // --- 4. RECOMMENDATIONS ---
    fun loadRealUsageAndGenerateRecs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val autoRecs = UsageDataProvider.getRealDataAndAutoRec(context)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(recommendations = autoRecs, isLoading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = false) }
            }
        }
    }

    fun applyRecommendation(context: Context, rec: RecommendationItem) {
        val pkgName = rec.appId ?: return
        updateAppLimit(context, pkgName, rec.title, rec.suggestedLimitMinutes ?: 60)
    }

    // --- 5. TIME LIMITS ---
    fun updateAppLimit(context: Context, packageName: String, appName: String, minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            context.getSharedPreferences("FocusGuardLimits", Context.MODE_PRIVATE).edit().putInt("limit_$packageName", minutes).apply()
            val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
            currentSet.add(packageName)
            FakeLocalDatabase.saveBlockedPackages(context, currentSet)
            refreshAllData(context)
        }
    }

    fun removeBlockedApp(packageName: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSet = FakeLocalDatabase.loadBlockedPackages(context).toMutableSet()
            currentSet.remove(packageName)
            FakeLocalDatabase.saveBlockedPackages(context, currentSet)

            context.getSharedPreferences("FocusGuardLimits", Context.MODE_PRIVATE).edit().apply {
                remove("limit_$packageName")      // Xóa giới hạn phút
                remove("sched_from_$packageName") // Xóa giờ bắt đầu
                remove("sched_to_$packageName")   // Xóa giờ kết thúc
                apply()
            }

            refreshAllData(context)
        }
    }

    // --- 6. UTILS ---
    fun startTimeTicker(context: Context) {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshAllData(context)
                delay(30000)
            }
        }
    }

    fun stopTimeTicker() { timeTickerJob?.cancel() }

    fun lockAllNow() {
        instantLockJob?.cancel()
        BlockState.setInstantLockTime(3600L)
        instantLockJob = viewModelScope.launch {
            var remaining = 3600L
            while (remaining > 0) {
                delay(1000L); remaining--
                BlockState.setInstantLockTime(remaining)
            }
        }
    }
}