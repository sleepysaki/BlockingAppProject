package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.utils.BlockManager
import com.exemple.blockingapps.utils.LocationPrefs
import com.exemple.blockingapps.ui.block.BlockPageActivity
import com.google.android.gms.location.*

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isInsideTargetZone = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startBackgroundLocationUpdates()
        Log.d("BlockService", "Service Connected & Location Updates Started")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "com.android.settings" ||
            packageName.contains("launcher") ||
            packageName.contains("inputmethod")
        ) {
            return
        }

        // Log.d("BlockService", "Checking App: $packageName | Zone: $isInsideTargetZone")

        // 👇 CẬP NHẬT QUAN TRỌNG: Lấy lý do chặn thay vì chỉ kiểm tra True/False
        val blockReason = BlockManager.getBlockReason(this, packageName, isInsideTargetZone)

        if (blockReason != null) {
            Log.d("BlockService", "BLOCKED: $packageName. Reason: $blockReason")
            // Truyền lý do vào hàm hiển thị
            showBlockScreen(blockReason)
        }
    }

    // 👇 CẬP NHẬT: Nhận tham số reason
    private fun showBlockScreen(reason: String) {
        val intent = Intent(this, BlockPageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            // 👇 Gửi lý do sang Activity
            putExtra("BLOCK_REASON", reason)
        }
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun startBackgroundLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return

                    val target = LocationPrefs.getTargetLocation(this@AppBlockerAccessibilityService)

                    if (target == null) {
                        isInsideTargetZone = false
                        return
                    }

                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        location.latitude, location.longitude,
                        target.first, target.second, results
                    )

                    val distanceInMeters = results[0]
                    val radius = target.third

                    val wasInside = isInsideTargetZone
                    isInsideTargetZone = distanceInMeters <= radius

                    if (wasInside != isInsideTargetZone) {
                        Log.d("BlockService", "Zone Status Changed: Inside=$isInsideTargetZone (Dist: $distanceInMeters m)")
                    }
                }
            }, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("BlockService", "Location update error: ${e.message}")
        }
    }

    override fun onInterrupt() {}
}