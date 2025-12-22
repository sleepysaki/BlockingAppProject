package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.utils.LocationPrefs
import com.google.android.gms.location.*

class AppBlockerAccessibilityService : AccessibilityService() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isInsideTargetZone = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startBackgroundLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startBackgroundLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val target = LocationPrefs.getTargetLocation(this@AppBlockerAccessibilityService) ?: return

                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    location.latitude, location.longitude,
                    target.first, target.second, results
                )

                isInsideTargetZone = results[0] <= target.third
                Log.d("GEO_SERVICE", "KC: ${results[0]}m | Trong vùng: $isInsideTargetZone")
            }
        }, Looper.getMainLooper())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName || pkg == "com.android.settings") return

        val blockedApps = FakeLocalDatabase.loadBlockedPackages(this)

        if (pkg in blockedApps && isInsideTargetZone) {
            Log.e("GEO_BLOCK", "CHẶN THÀNH CÔNG APP: $pkg")
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {}
}