package com.exemple.blockingapps.data.repo

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.exemple.blockingapps.data.network.RetrofitClient
import com.exemple.blockingapps.model.AppInfoDTO
import com.exemple.blockingapps.model.BlockRule
import com.exemple.blockingapps.model.SyncAppsRequest
import com.exemple.blockingapps.utils.BlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val api = RetrofitClient.apiService

    // 1. Hàm quét tất cả ứng dụng trong máy
    private fun getInstalledApps(): List<AppInfoDTO> {
        val pm = context.packageManager
        // Lấy danh sách app
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfoDTO>()

        for (packageInfo in packages) {
            // SỬA LỖI Ở ĐÂY: Thêm dấu ? trước các thuộc tính có thể null
            val appInfo = packageInfo.applicationInfo

            // Chỉ xử lý nếu appInfo không null
            if (appInfo != null) {
                val appName = appInfo.loadLabel(pm).toString()
                val packageName = packageInfo.packageName

                // Bạn có thể lọc app hệ thống ở đây nếu muốn
                // if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                appList.add(AppInfoDTO(packageName, appName))
                // }
            }
        }
        return appList
    }

    // ... (Các hàm syncAppsToServer và fetchAndSaveRules giữ nguyên như cũ)
    suspend fun syncAppsToServer(deviceId: String) {
        withContext(Dispatchers.IO) {
            try {
                val apps = getInstalledApps()
                Log.d("AppRepo", "Found ${apps.size} apps. Syncing to server...")

                val request = SyncAppsRequest(deviceId = deviceId, installedApps = apps)
                val response = api.syncInstalledApps(request)

                if (response.isSuccessful) {
                    Log.d("AppRepo", "Sync Success!")
                } else {
                    Log.e("AppRepo", "Sync Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AppRepo", "Sync Error: ${e.message}")
            }
        }
    }

    suspend fun fetchAndSaveRules(groupId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getGroupRules(groupId)
                if (response.isSuccessful) {
                    val serverRules = response.body() ?: emptyList()
                    Log.d("AppRepo", "Fetched ${serverRules.size} rules from server")

                    val localRules = serverRules.map { dto ->
                        BlockRule(
                            packageName = dto.packageName,
                            isBlocked = dto.isBlocked,
                            startTime = dto.startTime,
                            endTime = dto.endTime,
                            limitMinutes = null
                        )
                    }

                    BlockManager.saveRulesFromUI(context, localRules)
                    Log.d("AppRepo", "Rules updated locally!")
                } else {
                    Log.e("AppRepo", "Fetch Rules Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AppRepo", "Fetch Rules Error: ${e.message}")
            }
        }
    }
}