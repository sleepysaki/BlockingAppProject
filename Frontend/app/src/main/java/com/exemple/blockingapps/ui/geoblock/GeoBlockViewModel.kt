package com.exemple.blockingapps.ui.geoblock

import BlockRuleDTO
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeoBlockViewModel : ViewModel() {
    // Use AppItem model to match frontend structure
    private val _appList = MutableStateFlow<List<AppItem>>(emptyList())
    val appList = _appList.asStateFlow()

    fun getInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val apps = allApps.mapNotNull { info ->
                // Filter using launch intent to exclude system services
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

            Log.d("LIST_APP", "Loaded ${apps.size} launchable apps")
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

    // Logic to send blocking rules to server
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

                var serverMessage = "Success"

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

                    // SỬA LỖI: Truy cập body() của Response trước khi lấy "message"
                    val response = RetrofitClient.apiService.addBlockRule(rule)
                    if (response.isSuccessful) {
                        serverMessage = response.body()?.get("message") ?: "Saved"
                    } else {
                        Log.e("DEBUG_GEO", "Error code: ${response.code()}")
                    }
                }

                // Update local state immediately
                withContext(Dispatchers.Main) {
                    BlockState.targetLatitude = latitude
                    BlockState.targetLongitude = longitude
                    BlockState.targetRadius = 100.0
                    BlockState.blockedPackages = selectedApps.map { it.packageName }.toSet()

                    Toast.makeText(context, "Server: $serverMessage", Toast.LENGTH_LONG).show()
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