package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.ui.overlay.BlockOverlayActivity // Import Activity mới
import com.exemple.blockingapps.utils.BlockManager
import com.exemple.blockingapps.utils.LocationPrefs
import com.google.android.gms.location.*

class AppBlockerAccessibilityService : AccessibilityService() {

    // --- Location Components ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isInsideTargetZone = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockService", "Service Connected - Ready to block!")

        // Khởi tạo Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            startBackgroundLocationUpdates()
        } catch (e: Exception) {
            Log.e("BlockService", "Error starting location updates: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 1. Kiểm tra sự kiện mở cửa sổ
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 2. Bỏ qua chính App mình và Cài đặt (để tránh bị khóa ngoài không tắt được)
        if (packageName == this.packageName || packageName == "com.android.settings") return

        Log.d("BlockService", "Checking package: $packageName")

        // 3. Logic chặn theo Nhóm (Time Schedule) - Code mới thêm
        // Hàm này trong BlockManager đã check giờ rồi
        if (BlockManager.isAppBlocked(this, packageName)) {
            Log.d("BlockService", "BLOCKED by Group/Time: $packageName")
            showBlockScreen() // Gọi hàm hiện màn hình đỏ
            return
        }

        // 4. Logic chặn theo Vị trí (Geo Blocking) - Code cũ giữ nguyên
        val geoBlockedApps = FakeLocalDatabase.loadBlockedPackages(this)
        if (packageName in geoBlockedApps && isInsideTargetZone) {
            Log.d("BlockService", "BLOCKED by Geo: $packageName")
            showBlockScreen() // Gọi hàm hiện màn hình đỏ
            return
        }
    }

    // --- Hàm mới: Khởi động màn hình chặn (BlockOverlayActivity) ---
    private fun showBlockScreen() {
        val intent = Intent(this, BlockOverlayActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        } catch (e: SecurityException) {
            Log.e("BlockService", "Thiếu quyền vị trí!")
        }
    }

    override fun onInterrupt() {
        // Không làm gì
    }
}