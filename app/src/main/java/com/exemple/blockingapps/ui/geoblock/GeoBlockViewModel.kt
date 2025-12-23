package com.exemple.blockingapps.ui.geoblock

import BlockRuleDTO
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Simple data class for UI List
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?,
    var isSelected: Boolean = false
)

class GeoBlockViewModel : ViewModel() {

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    // Load all installed apps (excluding the current app itself)
    fun getInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            // Use GET_META_DATA to retrieve package info
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val apps = packages
                .filter {
                    // Safety check: Filter out nulls and exclude our own app to prevent self-blocking
                    it.applicationInfo != null && it.packageName != context.packageName
                }
                .map {
                    val info = it.applicationInfo!!
                    AppInfo(
                        name = info.loadLabel(pm).toString(),
                        packageName = it.packageName,
                        icon = info.loadIcon(pm),
                        isSelected = false
                    )
                }
                .sortedBy { it.name } // Sort alphabetically

            _appList.value = apps
        }
    }

    // Toggle Checkbox Selection
    fun toggleAppSelection(packageName: String) {
        _appList.value = _appList.value.map {
            if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    // ACTIVATE BLOCKING ON SERVER
    fun activateBlocking(context: Context, latitude: Double, longitude: Double) {
        Log.d("DEBUG_GEO", "ViewModel received: Lat=$latitude, Long=$longitude")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step A: Filter selected apps
                val selectedApps = _appList.value.filter { it.isSelected }

                if (selectedApps.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please select at least one app!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var serverMessage = "Success"

                // Step B: Send each rule to the Backend (Server)
                selectedApps.forEach { app ->
                    val rule = BlockRuleDTO(
                        packageName = app.packageName,
                        isBlocked = true,
                        limitMinutes = 0, // No time limit, just geo-block
                        startTime = null,
                        endTime = null,
                        latitude = latitude,
                        longitude = longitude,
                        radius = 100.0 // Default radius 100m
                    )

                    Log.d("DEBUG_GEO", "Sending Rule: $rule")

                    // 👇 CRITICAL FIX: Receive Map response instead of String
                    // Ensure ApiService returns Map<String, String>
                    val response = RetrofitClient.api.addBlockRule(rule)

                    // Extract message from JSON: {"status": "success", "message": "Saved successfully"}
                    serverMessage = response["message"] ?: "Saved"
                }

                // Step C: Update Local State immediately
                withContext(Dispatchers.Main) {
                    // Update global variables used by Background Service
                    BlockState.targetLatitude = latitude
                    BlockState.targetLongitude = longitude
                    BlockState.targetRadius = 100.0
                    BlockState.blockedPackages = selectedApps.map { it.packageName }.toSet()

                    // Display success message from Server
                    Toast.makeText(context, "Server: $serverMessage", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Handle generic errors (Network, parsing, etc.)
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}