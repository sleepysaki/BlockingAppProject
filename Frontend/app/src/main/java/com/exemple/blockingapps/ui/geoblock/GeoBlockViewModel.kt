package com.exemple.blockingapps.ui.geoblock

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.model.GroupDTO // 👈 Import GroupDTO
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.network.RetrofitClient
import com.exemple.blockingapps.utils.BlockManager
import com.exemple.blockingapps.utils.LocationPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoBlockViewModel : ViewModel() {
    private val _appList = MutableStateFlow<List<AppItem>>(emptyList())
    val appList = _appList.asStateFlow()

    // 👇 1. State lưu danh sách nhóm
    private val _userGroups = MutableStateFlow<List<GroupDTO>>(emptyList())
    val userGroups = _userGroups.asStateFlow()

    // 👇 2. Hàm tải danh sách nhóm (Gọi từ Screen)
    fun fetchUserGroups(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getUserGroups(userId)
                if (response.isSuccessful) {
                    val groups = response.body() ?: emptyList()
                    _userGroups.value = groups
                    Log.d("DEBUG_GEO", "Fetched ${groups.size} groups")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val apps = allApps.mapNotNull { info ->
                val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                if (launchIntent != null) {
                    AppItem(
                        name = info.loadLabel(pm).toString(),
                        packageName = info.packageName,
                        icon = info.loadIcon(pm),
                        isSelected = false
                    )
                } else {
                    null
                }
            }.filter { it.packageName != context.packageName }
                .sortedBy { it.name }

            _appList.value = apps
        }
    }

    fun toggleAppSelection(packageName: String) {
        _appList.update { currentList ->
            currentList.map {
                if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
            }
        }
    }

    // 👇 3. Hàm kích hoạt chặn (Nhận GroupID từ UI)
    fun activateBlocking(context: Context, latitude: Double, longitude: Double, currentGroupId: String) {
        Log.d("DEBUG_GEO", "ViewModel received: Lat=$latitude, Long=$longitude, GroupID=$currentGroupId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedApps = _appList.value.filter { it.isSelected }

                if (selectedApps.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please select at least one app!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // A. LƯU LOCAL (Để chặn ngay lập tức)
                LocationPrefs.saveTargetLocation(context, latitude, longitude, 100.0)

                val localRules = selectedApps.map { app ->
                    GroupRuleDTO(
                        groupId = currentGroupId,
                        packageName = app.packageName,
                        isBlocked = true,
                        radius = 100.0,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                BlockManager.saveBlockedPackages(context, localRules)

                // B. GỬI LÊN SERVER (Vào bảng GroupRules)
                var successCount = 0
                selectedApps.forEach { app ->
                    val groupRule = GroupRuleDTO(
                        groupId = currentGroupId,
                        packageName = app.packageName,
                        isBlocked = true,
                        startTime = null,
                        endTime = null,
                        latitude = latitude,   // ✅ Có tọa độ
                        longitude = longitude, // ✅ Có tọa độ
                        radius = 100.0         // ✅ Có bán kính
                    )

                    val response = RetrofitClient.apiService.saveGroupRule(groupRule)
                    if (response.isSuccessful) {
                        successCount++
                    } else {
                        Log.e("DEBUG_GEO", "Failed to save ${app.name}: ${response.code()}")
                    }
                }

                withContext(Dispatchers.Main) {
                    if (successCount > 0) {
                        Toast.makeText(context, "GeoBlock Active! (Synced $successCount apps)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Saved Locally only (Server Sync Failed)", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}