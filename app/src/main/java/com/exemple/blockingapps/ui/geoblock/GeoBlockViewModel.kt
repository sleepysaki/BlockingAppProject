package com.exemple.blockingapps.ui.geoblock

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

            Log.d("LIST_APP", "Sau khi lọc còn: ${apps.size} apps thực tế")
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
}