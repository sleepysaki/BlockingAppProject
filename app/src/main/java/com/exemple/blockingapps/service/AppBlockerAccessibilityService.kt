package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.ui.overlay.BlockOverlay
import com.exemple.blockingapps.utils.BlockManager
import com.exemple.blockingapps.utils.LocationPrefs
import com.google.android.gms.location.*

class AppBlockerAccessibilityService : AccessibilityService() {

    // --- Overlay Components ---
    private var blockOverlay: BlockOverlay? = null

    // --- Location Components ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isInsideTargetZone = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockService", "Service Connected - Ready to block!")

        blockOverlay = BlockOverlay(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            startBackgroundLocationUpdates()
        } catch (e: Exception) {
            Log.e("BlockService", "Error starting location updates: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        Log.d("BlockService", "User opened: $packageName")
        if (packageName == this.packageName || packageName == "com.android.settings") return

        if (BlockManager.isAppBlocked(this, packageName)) {
            Log.d("BlockService", "Đang chặn theo Group: $packageName")
            blockOverlay?.show()
            return 
        }

        val geoBlockedApps = FakeLocalDatabase.loadBlockedPackages(this)
        if (packageName in geoBlockedApps && isInsideTargetZone) {
            Log.d("BlockService", "Đang chặn theo Geo: $packageName")
            blockOverlay?.show()
            return
        }

        blockOverlay?.hide()
    }

    @SuppressLint("MissingPermission")
    private fun startBackgroundLocationUpdates() {
        try {
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
                    // Log.d("GEO_SERVICE", "KC: ${results[0]}m | Trong vùng: $isInsideTargetZone")
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("BlockService", "Thiếu quyền vị trí!")
        }
    }

    override fun onInterrupt() {
        blockOverlay?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        blockOverlay?.hide()
    }
}