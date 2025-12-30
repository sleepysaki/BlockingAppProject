package com.exemple.blockingapps.ui.geoblock

import BlockRuleDTO
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.model.AppItem
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

    fun activateBlocking(context: Context, latitude: Double, longitude: Double) {
        Log.d("DEBUG_GEO", "ViewModel received: Lat=$latitude, Long=$longitude")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedApps = _appList.value.filter { it.isSelected }

                if (selectedApps.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please select at least one app!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 1. LƯU TỌA ĐỘ VÀO LOCATION PREFS (Để Service đọc)
                // Mặc định bán kính là 100m
                LocationPrefs.saveTargetLocation(context, latitude, longitude, 100f)
                Log.d("DEBUG_GEO", "Saved location to Prefs")

                // 2. LƯU DANH SÁCH APP BỊ CHẶN VÀO BLOCK MANAGER (Để Service kiểm tra)
                // Chuyển đổi sang GroupRuleDTO để BlockManager hiểu (giả lập dữ liệu như từ Server trả về)
                val localRules = selectedApps.map { app ->
                    GroupRuleDTO(
                        groupId = "LOCAL_GEO",
                        packageName = app.packageName,
                        isBlocked = true,
                        radius = 100.0, // Quan trọng: Phải có radius > 0 để BlockManager nhận diện là Geo Rule
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                // Gọi hàm lưu local
                BlockManager.saveBlockedPackages(context, localRules)


                // 3. GỬI LÊN SERVER (Backup dữ liệu)
                var serverMessage = "Saved Locally"
                selectedApps.forEach { app ->
                    val rule = BlockRuleDTO(
                        packageName = app.packageName,
                        isBlocked = true,
                        limitMinutes = 0,
                        startTime = null,
                        endTime = null,
                        latitude = latitude,
                        longitude = longitude,
                        radius = 100.0
                    )

                    val response = RetrofitClient.apiService.addBlockRule(rule)
                    if (response.isSuccessful) {
                        serverMessage = "Synced to Server"
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "GeoBlock Active! ($serverMessage)", Toast.LENGTH_LONG).show()
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