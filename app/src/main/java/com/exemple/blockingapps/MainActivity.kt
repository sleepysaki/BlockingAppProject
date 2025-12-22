package com.exemple.blockingapps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.repo.UserRepository
import com.exemple.blockingapps.di.LocalUserRepository
import com.exemple.blockingapps.model.network.RetrofitClient // Import của bạn
import com.exemple.blockingapps.navigation.AppNavHost
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.theme.BlockingAppsTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 1. Khai báo trình xử lý xin quyền Vị trí (Giữ logic của bạn vì nó gọi tiếp background permission)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (fineLocationGranted || coarseLocationGranted) {
            Log.d("GEO", "Đã có quyền vị trí, bắt đầu kiểm tra Background Location")
            askBackgroundLocationPermission()
        } else {
            Log.e("GEO", "Người dùng từ chối quyền vị trí. Geofence sẽ không chạy!")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        // --- KHU VỰC XIN QUYỀN (Hợp nhất) ---
        askLocationPermissions()
        askBatteryOptimizationPermission()
        askOverlayPermission()
        askAccessibilityPermission()

        // --- KHU VỰC DỮ LIỆU ---
        val userRepository = UserRepository(FakeLocalDatabase)
        // Lưu ý: Nếu code của bạn kia update FakeLocalDatabase cần context 'this' thì sửa thành loadBlockedPackages(this)
        // Hiện tại giữ nguyên của bạn để đảm bảo chạy được API
        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages(this)

        // --- GỌI API & LOCATION (Của bạn - QUAN TRỌNG) ---
        fetchRulesFromServer()
        startLocationUpdates()

        // --- GIAO DIỆN (Giống nhau cả 2 bên) ---
        setContent {
            BlockingAppsTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = viewModel()

                CompositionLocalProvider(
                    LocalUserRepository provides userRepository
                ) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppNavHost(
                            navController = navController,
                            homeViewModel = homeViewModel
                        )
                    }
                }
            }
        }
    }

    // --- LOGIC API (Của bạn - Đã fix lỗi Oneway) ---
    private fun fetchRulesFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch data from Ktor Server
                val rules = RetrofitClient.api.getBlockRules()

                // 2. Filter blocked apps
                val serverBlockedList = rules.filter { it.isBlocked }.map { it.packageName }.toSet()

                withContext(Dispatchers.Main) {
                    // 3. Update the legacy variable used by your friend's code
                    BlockState.blockedPackages = serverBlockedList

                    // --- 🔥 CRITICAL FIX HERE ---

                    // Option A: If your friend has a reload function in the Service
                    // AppBlockerAccessibilityService.reloadConfig()

                    // Option B: Restart the logic manually (Example)
                    if (serverBlockedList.isNotEmpty()) {
                        Log.d("API_SYNC", "Rules updated. Triggering block check...")
                        // Gọi hàm kiểm tra lại của bạn bác ở đây, ví dụ:
                        // myBackgroundService.updateRules(serverBlockedList)
                    }

                    // Log for debugging
                    Log.i("API_SYNC", "Successfully synced ${serverBlockedList.size} rules from server.")
                }
            } catch (e: Exception) {
                Log.e("API_SYNC", "Failed to fetch rules: ${e.message}")
            }
        }
    }


    // --- LOGIC GEOFENCE (Của bạn - Tự tính khoảng cách) ---
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                if (BlockState.targetLat != 0.0) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        BlockState.targetLat,
                        BlockState.targetLng,
                        results
                    )
                    val distance = results[0]
                    BlockState.isInStudyZone = distance <= 200f

                    // Log để kiểm tra
                    Log.e("GEO_CHECK", "KC: ${distance.toInt()}m | Trong vùng: ${BlockState.isInStudyZone} | Đang chặn: ${BlockState.blockedPackages.size} Apps")
                } else {
                    Log.d("GEO_CHECK", "Chưa chọn vị trí trên Map (targetLat = 0)")
                }
            }
        }, Looper.getMainLooper())
    }

    // --- CÁC HÀM HELPER (Giữ nguyên) ---
    private fun askLocationPermissions() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun askBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundLocation = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasBackgroundLocation) {
                Log.d("GEO", "Cần quyền Background Location. Đang mở cài đặt...")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun askBatteryOptimizationPermission() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try { startActivity(intent) } catch (e: Exception) { Log.e("BLOCKER", "Pin: ${e.message}") }
        }
    }

    private fun askOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(packageName) == true
    }

    private fun askAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}