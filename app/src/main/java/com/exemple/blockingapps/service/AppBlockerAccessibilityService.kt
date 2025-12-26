package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.utils.LocationPrefs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Calendar

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

        // 0. KHÔNG CHẶN APP HỆ THỐNG
        if (pkg == packageName || pkg == "com.android.settings" || pkg == "com.android.launcher") return

        val sharedPrefs = getSharedPreferences("FocusGuardLimits", Context.MODE_PRIVATE)
        val blockedAppsFromDisk = FakeLocalDatabase.loadBlockedPackages(this)

        // --- CHỨC NĂNG 1: CHẶN THEO VỊ TRÍ (GEOFENCING) ---
        // Nếu app nằm trong danh sách chặn và đang ở trong vùng -> Vả luôn
        if (pkg in blockedAppsFromDisk && isInsideTargetZone) {
            Log.e("BLOCK_LOGIC", "CHẶN THEO VÙNG: $pkg")
            blockApp()
            return
        }

        // --- CHỨC NĂNG 2: CHẶN THEO THỜI GIAN/GỢI Ý (TIME LIMIT) ---
        val limitMinutes = sharedPrefs.getInt("limit_$pkg", 0)
        if (limitMinutes > 0) {
            val usedMinutes = getActualUsageMinutes(pkg)
            if (usedMinutes >= limitMinutes) {
                Log.e("BLOCK_LOGIC", "CHẶN THỜI GIAN: $pkg ($usedMinutes/$limitMinutes min)")
                blockApp()
                return
            }
        }

        // --- CHỨC NĂNG 3: CHẶN THEO KHUNG GIỜ (SCHEDULE/PRESET) ---
        val scheduleFrom = sharedPrefs.getString("sched_from_$pkg", null)
        val scheduleTo = sharedPrefs.getString("sched_to_$pkg", null)
        if (scheduleFrom != null && scheduleTo != null) {
            if (isNowInSchedule(scheduleFrom, scheduleTo)) {
                Log.e("BLOCK_LOGIC", "CHẶN THEO KHUNG GIỜ: $pkg")
                blockApp()
                return
            }
        }
    }

    private fun blockApp() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun getActualUsageMinutes(packageName: String): Int {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, now)
        return (stats.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L).toInt() / 1000 / 60
    }

    private fun isNowInSchedule(from: String, to: String): Boolean {
        try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val f = from.split(":")
            val t = to.split(":")
            val startMinutes = f[0].toInt() * 60 + f[1].toInt()
            val endMinutes = t[0].toInt() * 60 + t[1].toInt()

            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else { // Trường hợp qua đêm (VD: 22h - 6h)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (e: Exception) { return false }
    }

    override fun onInterrupt() {}
}