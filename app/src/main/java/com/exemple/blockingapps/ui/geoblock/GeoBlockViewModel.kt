package com.exemple.blockingapps.ui.geoblock

import android.content.Context
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
            val alreadyBlocked = com.exemple.blockingapps.data.local.FakeLocalDatabase.loadBlockedPackages(context)

            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val apps = allApps.mapNotNull { info ->
                val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                if (launchIntent != null) {
                    AppItem(
                        name = info.loadLabel(pm).toString(),
                        packageName = info.packageName,
                        icon = info.loadIcon(pm),
                        isSelected = alreadyBlocked.contains(info.packageName)
                    )
                } else null
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

    fun saveGeoBlocking(context: Context, selectedLocation: com.google.android.gms.maps.model.LatLng) {
        val selectedAppPackages = _appList.value.filter { it.isSelected }.map { it.packageName }.toSet()

        com.exemple.blockingapps.data.local.FakeLocalDatabase.saveBlockedPackages(context, selectedAppPackages)

        com.exemple.blockingapps.data.common.BlockState.blockedPackages = selectedAppPackages

        com.exemple.blockingapps.utils.LocationPrefs.saveTargetLocation(
            context,
            selectedLocation.latitude,
            selectedLocation.longitude,
            100f
        )
    }
}