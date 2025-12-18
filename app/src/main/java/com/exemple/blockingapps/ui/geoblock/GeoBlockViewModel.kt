package com.exemple.blockingapps.ui.geoblock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            val apps = resolveInfos.map { info ->
                AppItem(
                    name = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    icon = info.loadIcon(pm),
                    isSelected = false
                )
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
}