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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Tuyệt đối không chặn chính mình và hệ thống
        if (packageName == "com.exemple.blockingapps" ||
            packageName == "com.android.settings" ||
            packageName == "com.android.systemui" ||
            packageName.contains("launcher")
        ) return

        Log.d("BlockService", "Checking: $packageName")

        // 2. Kiểm tra luật chặn (Lịch trình & Vị trí)
        // Mình gộp logic: Nếu BlockManager bảo chặn app này vào lúc này
        if (BlockManager.isAppBlocked(this, packageName)) {

            // 👉 KIỂM TRA THÊM VỊ TRÍ (Nếu bạn muốn luật này chỉ áp dụng khi ở trong Zone)
            // Nếu bạn muốn chặn bất kể vị trí, chỉ cần gọi showBlockScreen() luôn.
            // Nếu muốn "Chỉ chặn Youtube khi ở trường", hãy dùng:
            // if (packageName == "com.google.android.youtube" && !isInsideTargetZone) return

            showBlockScreen()
        }
    }

    private fun showBlockScreen() {
        val intent = Intent(this, BlockPageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
                    val target = LocationPrefs.getTargetLocation(this@AppBlockerAccessibilityService) ?: return

                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        location.latitude, location.longitude,
                        target.first, target.second, results
                    )
                    isInsideTargetZone = results[0] <= target.third
                }
            }, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("BlockService", "Location update error: ${e.message}")
        }
    }

    override fun onInterrupt() {}
}