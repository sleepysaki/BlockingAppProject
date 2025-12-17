package com.exemple.blockingapps

import android.Manifest // <-- QUAN TRỌNG
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
import androidx.activity.result.contract.ActivityResultContracts // <-- QUAN TRỌNG
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.repo.UserRepository
import com.exemple.blockingapps.di.LocalUserRepository
import com.exemple.blockingapps.navigation.AppNavHost
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.theme.BlockingAppsTheme
import com.exemple.blockingapps.utils.GeofenceHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // 1. Khai báo trình xử lý xin quyền Vị trí
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

        // GỌI XIN QUYỀN VỊ TRÍ ĐẦU TIÊN
        askLocationPermissions()

        // CÁC QUYỀN CŨ CỦA BÁC
        askBatteryOptimizationPermission()
        askOverlayPermission()
        askAccessibilityPermission()

        val userRepository = UserRepository(FakeLocalDatabase)
        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages()

        startLocationUpdates()
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
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // 1. Giảm thời gian quét xuống 2 giây cho nhanh để test (sau này chỉnh lại 10s sau)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                // Kiểm tra xem bác đã nhấn nút KÍCH HOẠT trên Map chưa
                if (BlockState.targetLat != 0.0) {
                    val results = FloatArray(1)

                    // TỰ TÍNH KHOẢNG CÁCH (Không dùng GeofenceHelper nữa cho chắc)
                    android.location.Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        BlockState.targetLat,
                        BlockState.targetLng,
                        results
                    )

                    val distance = results[0]
                    // Cập nhật trạng thái vùng học tập
                    BlockState.isInStudyZone = distance <= 200f

                    // --- DÒNG LOG QUAN TRỌNG NHẤT ĐỂ KIỂM TRA ---
                    // Bác dùng Log.e để nó hiện màu ĐỎ trong Logcat cho dễ nhìn
                    Log.e("GEO_CHECK", "KC: $distance m | Trong vùng: ${BlockState.isInStudyZone} | App cần chặn: ${BlockState.restrictedApps.size}")
                } else {
                    Log.d("GEO_CHECK", "Chưa chọn vị trí trên Map (targetLat = 0)")
                }
            }
        }, Looper.getMainLooper())
    }
    // --- HÀM XIN QUYỀN VỊ TRÍ (MỚI) ---
    private fun askLocationPermissions() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun askBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Kiểm tra xem đã có quyền chạy ngầm chưa
            val hasBackgroundLocation = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasBackgroundLocation) {
                // Hiển thị thông báo giải thích cho người dùng hoặc mở thẳng cài đặt
                Log.d("GEO", "Cần quyền Background Location. Đang mở cài đặt...")
                // Lưu ý: Android 11+ yêu cầu người dùng tự tay chọn "Allow all the time"
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    // --- CÁC HÀM CŨ CỦA BÁC GIỮ NGUYÊN ---
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